export const ROLES = Object.freeze({
  PLAYER: 'PLAYER',
  ADMIN: 'ADMIN'
});

/**
 * Role & Authorization Session Manager.
 *
 * Security model:
 * - Every housemate defaults to ROLES.PLAYER.
 * - Unauthenticated users cannot enter the multiplayer house.
 * - No hardcoded client-side password or hidden button can grant ROLES.ADMIN.
 * - ROLES.ADMIN is ONLY granted when a server-verified Firebase Auth + Firestore
 *   /admins/{uid} payload is supplied through AndroidBridge.
 */
export function createAuthSession(options = {}) {
  const backendAuthVerifier = typeof options.backendAuthVerifier === 'function'
    ? options.backendAuthVerifier
    : null;

  const state = {
    authenticated: false,
    role: ROLES.PLAYER,
    accountId: null,
    playerId: 'Player 01',
    displayName: 'Contestant',
    characterId: 'Player 01',
    verifiedByServer: false,
    captainUid: '',
    captainVipAccess: true,
    vipUids: new Set()
  };

  function isAdmin() {
    return (
      state.authenticated === true &&
      state.verifiedByServer === true &&
      state.role === ROLES.ADMIN
    );
  }

  function isCaptain() {
    return Boolean(state.accountId && state.captainUid && state.accountId === state.captainUid);
  }

  function isVip() {
    return Boolean(state.accountId && state.vipUids.has(state.accountId));
  }

  function canAccessVipRoom() {
    if (isAdmin()) return true;
    if (isVip()) return true;
    if (isCaptain() && state.captainVipAccess !== false) return true;
    return false;
  }

  function assertVipPermission(actionName = 'VIP_ROOM') {
    if (!canAccessVipRoom()) {
      return {
        allowed: false,
        code: 'VIP_ACCESS_DENIED',
        error: `VIP Room Locked (${actionName}): House Captain or VIP Pass required.`
      };
    }
    return { allowed: true, code: 'OK', error: null };
  }

  function setVipPrivileges(payload = {}) {
    if (!payload || typeof payload !== 'object') return;
    if (typeof payload.captainUid === 'string') {
      state.captainUid = payload.captainUid.trim();
    }
    if (typeof payload.captainVipAccess === 'boolean') {
      state.captainVipAccess = payload.captainVipAccess;
    }
    if (Array.isArray(payload.vipUids)) {
      state.vipUids = new Set(payload.vipUids.map((u) => String(u || '').trim()).filter(Boolean));
    }
  }

  function assertAdminPermission(actionName = 'ADMIN_ACTION') {
    if (!isAdmin()) {
      return {
        allowed: false,
        code: 'ACCESS_DENIED',
        error: `Access denied: ADMIN role required for ${actionName}.`
      };
    }
    return { allowed: true, code: 'OK', error: null };
  }

  function setActiveHousemate(characterId) {
    // Switching housemates (including Player 01) NEVER elevates role to ADMIN.
    state.characterId = characterId || 'Player 01';
    if (!state.verifiedByServer) {
      state.role = ROLES.PLAYER;
    }
  }

  function applyVerifiedSession(payload) {
    if (!payload || payload.verifiedByServer !== true || !payload.uid) {
      signOut();
      return false;
    }
    state.authenticated = true;
    state.verifiedByServer = true;
    state.accountId = String(payload.uid);
    state.playerId = String(payload.playerId || 'Player 01');
    state.displayName = String(payload.displayName || 'Housemate');
    state.characterId = String(payload.characterId || 'Player 01');
    state.role = payload.isAdminVerified === true && payload.role === ROLES.ADMIN
      ? ROLES.ADMIN
      : ROLES.PLAYER;
    return true;
  }

  async function authenticateWithBackend(credentials) {
    if (!backendAuthVerifier) {
      signOut();
      return {
        ok: false,
        role: ROLES.PLAYER,
        error: 'Server authentication is required.'
      };
    }

    const result = await backendAuthVerifier(credentials);
    if (!result || result.verified !== true) {
      signOut();
      return {
        ok: false,
        role: ROLES.PLAYER,
        error: result?.error || 'Authentication failed.'
      };
    }

    state.authenticated = true;
    state.verifiedByServer = true;
    state.accountId = result.accountId || null;
    state.role = result.role === ROLES.ADMIN ? ROLES.ADMIN : ROLES.PLAYER;

    return {
      ok: true,
      role: state.role,
      accountId: state.accountId
    };
  }

  function signOut() {
    state.authenticated = false;
    state.verifiedByServer = false;
    state.accountId = null;
    state.role = ROLES.PLAYER;
    state.captainUid = '';
    state.captainVipAccess = true;
    state.vipUids = new Set();
  }

  return {
    get role() {
      return isAdmin() ? ROLES.ADMIN : ROLES.PLAYER;
    },
    get uid() {
      return state.accountId;
    },
    get playerId() {
      return state.playerId;
    },
    get displayName() {
      return state.displayName;
    },
    get characterId() {
      return state.characterId;
    },
    get isAuthenticated() {
      return state.authenticated;
    },
    isAdmin,
    isCaptain,
    isVip,
    canAccessVipRoom,
    canAccessAdminRoom: isAdmin,
    setVipPrivileges,
    assertVipPermission,
    assertAdminPermission,
    setActiveHousemate,
    applyVerifiedSession,
    authenticateWithBackend,
    signOut
  };
}
