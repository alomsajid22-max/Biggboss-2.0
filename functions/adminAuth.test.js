'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const Module = require('node:module');
const path = require('node:path');

function createHarness() {
  const store = new Map(); // path -> object
  const users = new Map(); // uid -> user object
  const tokens = new Map(); // bearerToken -> decodedClaims
  const rtdb = new Map();
  let currentSecretValue = '';
  let autoIdCounter = 0;

  class DocSnap {
    constructor(ref, data) {
      this.ref = ref;
      this.id = ref.id;
      this.exists = data !== undefined;
      this._data = data ? structuredClone(data) : undefined;
    }
    data() {
      return this._data ? structuredClone(this._data) : undefined;
    }
  }

  class QuerySnap {
    constructor(docs) {
      this.docs = docs;
      this.size = docs.length;
    }
    forEach(cb) {
      this.docs.forEach(cb);
    }
  }

  class DocRef {
    constructor(docPath) {
      this.path = docPath;
      const parts = docPath.split('/');
      this.id = parts[parts.length - 1];
    }
    collection(name) {
      return new CollectionRef(`${this.path}/${name}`);
    }
    async get() {
      return new DocSnap(this, store.get(this.path));
    }
    async set(data, opts = {}) {
      const normalized = stripSentinels(data);
      if (opts && opts.merge && store.has(this.path)) {
        store.set(this.path, { ...store.get(this.path), ...normalized });
      } else {
        store.set(this.path, normalized);
      }
    }
    async update(data) {
      if (!store.has(this.path)) {
        const err = new Error(`Document ${this.path} not found`);
        err.code = 'not-found';
        throw err;
      }
      store.set(this.path, { ...store.get(this.path), ...stripSentinels(data) });
    }
    async delete() {
      store.delete(this.path);
    }
  }

  class QueryRef {
    constructor(colPath, filters = []) {
      this.colPath = colPath;
      this.filters = filters;
    }
    where(field, op, value) {
      return new QueryRef(this.colPath, [...this.filters, { field, op, value }]);
    }
    async get() {
      const prefix = `${this.colPath}/`;
      const depth = this.colPath.split('/').length + 1;
      const docs = [];
      for (const [docPath, data] of store.entries()) {
        if (!docPath.startsWith(prefix)) continue;
        if (docPath.split('/').length !== depth) continue;
        const matches = this.filters.every((f) => {
          if (f.op === '==') return data[f.field] === f.value;
          return true;
        });
        if (matches) {
          docs.push(new DocSnap(new DocRef(docPath), data));
        }
      }
      return new QuerySnap(docs);
    }
  }

  class CollectionRef extends QueryRef {
    constructor(colPath) {
      super(colPath, []);
      this.path = colPath;
    }
    doc(id) {
      const resolved = id || `auto_${++autoIdCounter}`;
      return new DocRef(`${this.path}/${resolved}`);
    }
  }

  function stripSentinels(obj) {
    if (obj === null || typeof obj !== 'object') return obj;
    if (Array.isArray(obj)) return obj.map(stripSentinels);
    const out = {};
    for (const [k, v] of Object.entries(obj)) {
      if (v && v.__serverTimestamp) {
        out[k] = Date.now();
      } else {
        out[k] = stripSentinels(v);
      }
    }
    return out;
  }

  const fakeFirestore = {
    collection(name) {
      return new CollectionRef(name);
    },
    async runTransaction(fn) {
      const tx = {
        get: (ref) => ref.get(),
        set: (ref, data, opts) => ref.set(data, opts),
        update: (ref, data) => ref.update(data),
        delete: (ref) => ref.delete(),
      };
      return fn(tx);
    },
    batch() {
      const ops = [];
      return {
        set: (ref, data, opts) => ops.push(() => ref.set(data, opts)),
        update: (ref, data) => ops.push(() => ref.update(data)),
        delete: (ref) => ops.push(() => ref.delete()),
        commit: async () => {
          for (const op of ops) await op();
        },
      };
    },
  };

  class MockHttpsError extends Error {
    constructor(code, message) {
      super(message);
      this.code = code;
    }
  }

  const origLoad = Module._load;
  Module._load = function patchedLoad(request, parent, isMain) {
    if (request === 'firebase-functions/v2') {
      return { setGlobalOptions: () => {} };
    }
    if (request === 'firebase-functions/params') {
      return {
        defineSecret: (name) => ({
          name,
          value: () => currentSecretValue,
        }),
      };
    }
    if (request === 'firebase-functions/v2/https') {
      return {
        HttpsError: MockHttpsError,
        onCall: (optsOrFn, maybeFn) => (maybeFn ? maybeFn : optsOrFn),
        onRequest: (optsOrFn, maybeFn) => {
          const hasOpts = typeof optsOrFn === 'object' && typeof maybeFn === 'function';
          const handler = hasOpts ? maybeFn : optsOrFn;
          handler.__options = hasOpts ? optsOrFn : {};
          return handler;
        },
      };
    }
    if (request === 'firebase-functions/v2/scheduler') {
      return {
        onSchedule: (optsOrSchedule, fn) => fn || optsOrSchedule,
      };
    }
    if (request === 'firebase-admin/app') {
      return { initializeApp: () => ({}) };
    }
    if (request === 'firebase-admin/firestore') {
      return {
        getFirestore: () => fakeFirestore,
        FieldValue: {
          serverTimestamp: () => ({ __serverTimestamp: true }),
          increment: (n) => n,
        },
      };
    }
    if (request === 'firebase-admin/auth') {
      return {
        getAuth: () => ({
          async getUser(uid) {
            if (!users.has(uid)) throw new Error('auth/user-not-found');
            return users.get(uid);
          },
          async createUser({ uid, displayName }) {
            const u = { uid, displayName, claims: {} };
            users.set(uid, u);
            return u;
          },
          async setCustomUserClaims(uid, claims) {
            const u = users.get(uid) || { uid };
            u.claims = claims;
            users.set(uid, u);
          },
          async revokeRefreshTokens() {},
          async createCustomToken(uid, claims) {
            const tok = `custom_${uid}_${claims.adminSessionId || 'nosess'}`;
            // Simulate the resulting signed-in ID token bearing custom claims for verifyIdToken.
            tokens.set(`id_${tok}`, { uid, ...claims });
            return tok;
          },
          async verifyIdToken(token) {
            if (!tokens.has(token)) {
              throw new Error('Invalid or expired ID token');
            }
            return tokens.get(token);
          },
        }),
      };
    }
    if (request === 'firebase-admin/database') {
      return {
        getDatabase: () => ({
          ref: (p) => ({
            set: async (v) => rtdb.set(p, v),
            update: async (v) => rtdb.set(p, { ...(rtdb.get(p) || {}), ...v }),
            remove: async () => rtdb.delete(p),
          }),
        }),
      };
    }
    return origLoad.apply(this, arguments);
  };

  const indexPath = path.resolve(__dirname, 'index.js');
  delete require.cache[indexPath];
  const fnModule = require(indexPath);
  Module._load = origLoad;

  async function invokeHttp(handler, { method = 'POST', body = {}, headers = {} } = {}) {
    let statusCode = 200;
    let payload = null;
    const req = { method, body, headers };
    const res = {
      status(code) {
        statusCode = code;
        return this;
      },
      json(obj) {
        payload = obj;
        return this;
      },
    };
    await handler(req, res);
    return { statusCode, body: payload };
  }

  return {
    fnModule,
    store,
    tokens,
    setSecret: (val) => {
      currentSecretValue = val;
    },
    invokeHttp,
  };
}

test('adminVerifyLogin binds BIGGBOSS_ADMIN_INITIAL_PASSWORD via defineSecret', () => {
  const h = createHarness();
  const opts = h.fnModule.adminVerifyLogin.__options || {};
  assert.ok(Array.isArray(opts.secrets), 'onRequest options must declare secrets array');
  assert.equal(opts.secrets[0].name, 'BIGGBOSS_ADMIN_INITIAL_PASSWORD');
});

test('adminVerifyLogin returns 503 when BIGGBOSS_ADMIN_INITIAL_PASSWORD secret is unset', async () => {
  const h = createHarness();
  h.setSecret('');
  delete process.env.BIGGBOSS_ADMIN_INITIAL_PASSWORD;

  const res = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: 'SomeTestPassword1!', seasonId: 'season_01' },
  });
  assert.equal(res.statusCode, 503);
  assert.equal(res.body.ok, false);
  assert.match(res.body.error, /BIGGBOSS_ADMIN_INITIAL_PASSWORD/);
});

test('adminVerifyLogin authenticates, returns expiring session + customToken, and enforces rate limit', async () => {
  const h = createHarness();
  const testSecret = 'InitialTestSecret99!';
  h.setSecret(testSecret);

  const okRes = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: testSecret, seasonId: 'season_01' },
  });
  assert.equal(okRes.statusCode, 200);
  assert.equal(okRes.body.ok, true);
  assert.equal(okRes.body.uid, 'biggboss_admin');
  assert.equal(okRes.body.username, 'BIGGBOSS');
  assert.equal(okRes.body.role, 'ADMIN');
  assert.equal(okRes.body.mustChangeInitialPassword, true);
  assert.ok(okRes.body.customToken.startsWith('custom_biggboss_admin_'));
  assert.ok(okRes.body.sessionId.length >= 16);
  assert.ok(okRes.body.expiresAtMillis > Date.now());

  // Updating Secret Manager secret before in-app rotation re-syncs v1 credentials cleanly
  const rotatedSecret = 'UpdatedSecretBeforeRotate88!';
  h.setSecret(rotatedSecret);
  const resyncRes = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: rotatedSecret, seasonId: 'season_01' },
  });
  assert.equal(resyncRes.statusCode, 200);
  assert.equal(resyncRes.body.mustChangeInitialPassword, true);

  // Wrong credentials reject with 401 and trigger 429 lockout after 5 attempts
  for (let i = 1; i <= 5; i += 1) {
    const bad = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
      body: { username: 'BIGGBOSS', password: `wrong-${i}`, seasonId: 'season_01' },
    });
    assert.equal(bad.statusCode, 401);
  }
  const locked = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: rotatedSecret, seasonId: 'season_01' },
  });
  assert.equal(locked.statusCode, 429);
});

test('normal player is rejected from adminCheckSession, adminChangePassword, adminLogout, and calculateAndCloseVoting', async () => {
  const h = createHarness();
  h.tokens.set('player_token', { uid: 'player_uid_01', role: 'PLAYER' });

  for (const endpoint of [
    h.fnModule.adminCheckSession,
    h.fnModule.adminChangePassword,
    h.fnModule.adminLogout,
    h.fnModule.calculateAndCloseVoting,
  ]) {
    const res = await h.invokeHttp(endpoint, {
      headers: { authorization: 'Bearer player_token' },
      body: { currentPassword: 'x', newPassword: 'NewPassword123', cycleId: 'cycle_1' },
    });
    assert.equal(res.statusCode, 403);
    assert.equal(res.body.ok, false);
  }
});

test('adminCheckSession, adminChangePassword, adminLogout, and calculateAndCloseVoting lifecycle works end-to-end', async () => {
  const h = createHarness();
  const initialSecret = 'BootAdminPass2026!';
  const newPassword = 'RotatedStrongPassword#2026';
  h.setSecret(initialSecret);

  const loginRes = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: initialSecret, seasonId: 'season_01' },
  });
  assert.equal(loginRes.statusCode, 200);
  const firstIdToken = `id_${loginRes.body.customToken}`;

  const checkRes = await h.invokeHttp(h.fnModule.adminCheckSession, {
    headers: { authorization: `Bearer ${firstIdToken}` },
  });
  assert.equal(checkRes.statusCode, 200);
  assert.equal(checkRes.body.ok, true);
  assert.equal(checkRes.body.active, true);
  assert.equal(checkRes.body.sessionId, loginRes.body.sessionId);

  // Password change requires correct current password
  const badChange = await h.invokeHttp(h.fnModule.adminChangePassword, {
    headers: { authorization: `Bearer ${firstIdToken}` },
    body: { currentPassword: 'WrongCurrentPassword', newPassword, seasonId: 'season_01' },
  });
  assert.equal(badChange.statusCode, 401);

  const changeRes = await h.invokeHttp(h.fnModule.adminChangePassword, {
    headers: { authorization: `Bearer ${firstIdToken}` },
    body: { currentPassword: initialSecret, newPassword, seasonId: 'season_01' },
  });
  assert.equal(changeRes.statusCode, 200);
  assert.equal(changeRes.body.mustChangeInitialPassword, false);
  const rotatedIdToken = `id_${changeRes.body.customToken}`;

  // Old secret is now rejected (and no longer re-synced because rotatedInApp === true)
  const oldLogin = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: initialSecret, seasonId: 'season_01' },
  });
  assert.equal(oldLogin.statusCode, 401);

  // New rotated password works and reports mustChangeInitialPassword: false
  const newLogin = await h.invokeHttp(h.fnModule.adminVerifyLogin, {
    body: { username: 'BIGGBOSS', password: newPassword, seasonId: 'season_01' },
  });
  assert.equal(newLogin.statusCode, 200);
  assert.equal(newLogin.body.mustChangeInitialPassword, false);

  // Seed a voting cycle & vote so calculateAndCloseVoting can compute tally
  const activeIdToken = `id_${newLogin.body.customToken}`;
  await h.store.set('seasons/season_01', { captainUid: '', captainImmunity: false, captainDoubleVote: false });
  await h.store.set('seasons/season_01/votingCycles/cycle_1', {
    title: 'Week 1 Eviction',
    status: 'OPEN',
    voteType: 'EVICT',
    npcVotesEnabled: false,
    nomineeSummaries: [{ uid: 'p1', displayName: 'Aarav' }, { uid: 'p2', displayName: 'Zoya' }],
  });
  await h.store.set('seasons/season_01/votingCycles/cycle_1/votes/v1', {
    seasonId: 'season_01',
    cycleId: 'cycle_1',
    voterUid: 'p3',
    targetUid: 'p1',
    sourceType: 'PLAYER',
  });

  const tallyRes = await h.invokeHttp(h.fnModule.calculateAndCloseVoting, {
    headers: { authorization: `Bearer ${activeIdToken}` },
    body: { seasonId: 'season_01', cycleId: 'cycle_1', closeVoting: true },
  });
  assert.equal(tallyRes.statusCode, 200);
  assert.equal(tallyRes.body.ok, true);
  assert.equal(tallyRes.body.closed, true);
  assert.deepEqual(tallyRes.body.leadingEvictionUids, ['p1']);

  // Logout revokes active session immediately
  const logoutRes = await h.invokeHttp(h.fnModule.adminLogout, {
    headers: { authorization: `Bearer ${activeIdToken}` },
    body: { seasonId: 'season_01' },
  });
  assert.equal(logoutRes.statusCode, 200);

  const postLogoutCheck = await h.invokeHttp(h.fnModule.adminCheckSession, {
    headers: { authorization: `Bearer ${activeIdToken}` },
  });
  assert.equal(postLogoutCheck.statusCode, 403);
  assert.equal(postLogoutCheck.body.active, false);

  // Stale rotated token from before relogin is also rejected
  const staleCheck = await h.invokeHttp(h.fnModule.adminCheckSession, {
    headers: { authorization: `Bearer ${rotatedIdToken}` },
  });
  assert.equal(staleCheck.statusCode, 403);
});
