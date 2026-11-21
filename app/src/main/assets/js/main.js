import * as THREE from '../vendor/three/three.module.min.js';
import { createAuthSession, ROLES } from './auth.js';
import { createSoundSystem } from './audio.js';
import { createTextureLibrary } from './textures.js';
import { buildHouseArchitecture, getRoomAtPosition } from './worldLayout.js';
import { buildCenterTree } from './centerTree.js';
import { buildGlassSlidingDoors } from './doors.js';
import { buildAllRoomFurniture } from './furniture.js';
import { buildLightingSystem } from './lighting.js';
import { createHousematesManager, HOUSEMATE_PROFILES } from './characters.js';
import { createPlayerController } from './player.js';
import { setupControls, renderMinimap } from './controls.js';
import { createCctvSystem } from './cctv.js';
import { createTasks3dManager } from './tasks3d.js';
import { buildBiggBossEyes } from './biggBossEyes.js';

const ui = {
  canvas: document.getElementById('game-canvas'),
  hud: document.getElementById('hud'),
  loadingScreen: document.getElementById('loading-screen'),
  progressFill: document.getElementById('progress-fill'),
  loaderStatus: document.getElementById('loader-status'),
  authGateScreen: document.getElementById('auth-gate-screen'),
  errorScreen: document.getElementById('error-screen'),
  errorMessage: document.getElementById('error-message'),
  btnRetry: document.getElementById('btn-retry'),
  activePlayerTag: document.getElementById('active-player-tag'),
  roomName: document.getElementById('room-name'),
  roomDot: document.querySelector('.room-dot'),
  btnPlayers: document.getElementById('btn-players'),
  onlineCountLabel: document.getElementById('online-count-label'),
  btnLobby: document.getElementById('btn-lobby'),
  btnCast: document.getElementById('btn-cast'),
  btnWardrobe: document.getElementById('btn-wardrobe'),
  btnChat: document.getElementById('btn-chat'),
  chatUnreadDot: document.getElementById('chat-unread-dot'),
  btnProfile: document.getElementById('btn-profile'),
  btnHouseHub: document.getElementById('btn-house-hub'),
  btnKitchen: document.getElementById('btn-kitchen'),
  kitchenStatusPill: document.getElementById('kitchen-status-pill'),
  hudEnergyVal: document.getElementById('hud-energy-val'),
  hudHungerVal: document.getElementById('hud-hunger-val'),
  hudMealsBadge: document.getElementById('hud-meals-badge'),
  clockPill: document.getElementById('clock-pill'),
  hudClockVal: document.getElementById('hud-clock-val'),
  hudWeatherVal: document.getElementById('hud-weather-val'),
  celebrationBanner: document.getElementById('celebration-banner'),
  activeTaskHud: document.getElementById('active-task-hud'),
  taskHudBadge: document.getElementById('task-hud-badge'),
  taskHudTitle: document.getElementById('task-hud-title'),
  taskHudProgress: document.getElementById('task-hud-progress'),
  taskHudTimer: document.getElementById('task-hud-timer'),
  btnAdminPhone: document.getElementById('btn-admin-phone'),
  btnAdminDash: document.getElementById('btn-admin-dash'),
  adminModal: document.getElementById('admin-dashboard-modal'),
  btnCloseAdmin: document.getElementById('btn-close-admin'),
  adminDoorStatus: document.getElementById('admin-door-status'),
  adminOccupancyVal: document.getElementById('admin-occupancy-val'),
  btnAdminCctv: document.getElementById('btn-admin-cctv'),
  btnAdminDoorToggle: document.getElementById('btn-admin-door-toggle'),
  btnAdminBroadcast: document.getElementById('btn-admin-broadcast'),
  cctvOverlay: document.getElementById('cctv-overlay'),
  cctvHeaderTitle: document.getElementById('cctv-header-title'),
  cctvRoomFilter: document.getElementById('cctv-room-filter'),
  btnCctvPagePrev: document.getElementById('btn-cctv-page-prev'),
  cctvPageLabel: document.getElementById('cctv-page-label'),
  btnCctvPageNext: document.getElementById('btn-cctv-page-next'),
  btnCctvPrev: document.getElementById('btn-cctv-prev'),
  btnCctvNext: document.getElementById('btn-cctv-next'),
  btnCctvGrid: document.getElementById('btn-cctv-grid'),
  btnCctvControlRoom: document.getElementById('btn-cctv-control-room'),
  btnCctvRecord: document.getElementById('btn-cctv-record'),
  cctvRecTimer: document.getElementById('cctv-rec-timer'),
  btnCloseCctv: document.getElementById('btn-close-cctv'),
  cctvGridLayer: document.getElementById('cctv-grid-layer'),
  cctvFullscreenHud: document.getElementById('cctv-fullscreen-hud'),
  cctvFsBadge: document.getElementById('cctv-fs-badge'),
  cctvFsPlayers: document.getElementById('cctv-fs-players'),
  btnTime: document.getElementById('btn-time'),
  timeIcon: document.getElementById('time-icon'),
  timeLabel: document.getElementById('time-label'),
  btnMap: document.getElementById('btn-map'),
  btnCloseMap: document.getElementById('btn-close-map'),
  mapModal: document.getElementById('map-modal'),
  minimapCanvas: document.getElementById('minimap-canvas'),
  btnQuality: document.getElementById('btn-quality'),
  qualityLabel: document.getElementById('quality-label'),
  btnCamMode: document.getElementById('btn-cam-mode'),
  camModeLabel: document.getElementById('cam-mode-label'),
  btnZoomOut: document.getElementById('btn-zoom-out'),
  btnZoomIn: document.getElementById('btn-zoom-in'),
  btnCamReset: document.getElementById('btn-cam-reset'),
  expressionBar: document.getElementById('expression-bar'),
  btnDance: document.getElementById('btn-dance'),
  danceLabel: document.getElementById('dance-label'),
  btnSprint: document.getElementById('btn-sprint'),
  sprintIcon: document.querySelector('.sprint-icon'),
  sprintLabel: document.getElementById('sprint-label'),
  btnInteract: document.getElementById('btn-interact'),
  interactBadge: document.getElementById('interact-badge'),
  interactLabel: document.getElementById('interact-label'),
  btnInteractAlt: document.getElementById('btn-interact-alt'),
  interactAltBadge: document.getElementById('interact-alt-badge'),
  interactAltLabel: document.getElementById('interact-alt-label'),
  btnStand: document.getElementById('btn-stand'),
  standIcon: document.getElementById('stand-icon'),
  standLabel: document.getElementById('stand-label'),
  statusBanner: document.getElementById('status-banner'),
  joystickZone: document.getElementById('joystick-zone'),
  joystickBase: document.getElementById('joystick-base'),
  joystickKnob: document.getElementById('joystick-knob'),
  playersModal: document.getElementById('players-modal'),
  btnClosePlayers: document.getElementById('btn-close-players'),
  playersList: document.getElementById('players-list'),
  playersSyncBadge: document.getElementById('players-sync-badge'),
  btnToggleNpcs: document.getElementById('btn-toggle-npcs'),
  btnReturnLobby: document.getElementById('btn-return-lobby'),
  btnHouseLogout: document.getElementById('btn-house-logout'),
  charSelectModal: document.getElementById('char-select-modal'),
  charGrid: document.getElementById('char-grid'),
  charSelectedSummary: document.getElementById('char-selected-summary'),
  btnConfirmChar: document.getElementById('btn-confirm-char'),
  btnCloseCast: document.getElementById('btn-close-cast')
};

const pendingBridgePayloads = {
  session: null,
  remotePlayers: null,
  worldState: null,
  phase6: null,
  phase7And8: null,
  phase9Ai: null,
  phase12To20: null,
  cctvRequest: null,
  paused: false
};

// Pre-register bridge immediately so Android never drops an early state push.
window.MultiplayerBridge = {
  setAuthenticatedSession(payload) {
    pendingBridgePayloads.session = payload;
  },
  onRemotePlayersUpdate(playersList) {
    pendingBridgePayloads.remotePlayers = playersList;
  },
  onWorldStateUpdate(worldObj) {
    pendingBridgePayloads.worldState = worldObj;
  },
  onPhase6StateUpdate(phase6Obj) {
    pendingBridgePayloads.phase6 = phase6Obj;
  },
  onPhase7And8StateUpdate(payload) {
    pendingBridgePayloads.phase7And8 = payload;
  },
  onPhase9AiStateUpdate(payload) {
    pendingBridgePayloads.phase9Ai = payload;
  },
  onPhase12To20StateUpdate(payload) {
    pendingBridgePayloads.phase12To20 = payload;
  },
  setCctvMode(enabled, focusCamId = null) {
    pendingBridgePayloads.cctvRequest = { enabled: Boolean(enabled), focusCamId };
  },
  setPaused(paused) {
    pendingBridgePayloads.paused = Boolean(paused);
  }
};

function bridgeCall(fnName, ...args) {
  try {
    if (window.AndroidBridge && typeof window.AndroidBridge[fnName] === 'function') {
      window.AndroidBridge[fnName](...args);
      return true;
    }
  } catch (err) {
    console.warn(`AndroidBridge.${fnName} failed:`, err);
  }
  return false;
}

function setProgress(pct, text) {
  ui.progressFill.style.width = `${pct}%`;
  ui.loaderStatus.textContent = text;
}

function showError(msg) {
  ui.loadingScreen.classList.add('hidden');
  ui.hud.classList.add('hidden');
  ui.charSelectModal.classList.add('hidden');
  ui.playersModal.classList.add('hidden');
  ui.adminModal.classList.add('hidden');
  ui.cctvOverlay.classList.add('hidden');
  ui.authGateScreen.classList.add('hidden');
  ui.errorMessage.textContent = msg;
  ui.errorScreen.classList.remove('hidden');
  bridgeCall('onSceneError', msg);
}

ui.btnRetry.addEventListener('click', () => {
  window.location.reload();
});

let toastTimer = null;
function showToast(text) {
  ui.statusBanner.textContent = text;
  ui.statusBanner.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    ui.statusBanner.classList.add('hidden');
  }, 2400);
}

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const toHex = (num) => `#${num.toString(16).padStart(6, '0')}`;
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function bootstrap() {
  try {
    // Role session starts strictly unauthenticated & ROLES.PLAYER; ADMIN requires server-verified Firebase Auth + /admins/{uid}.
    const auth = createAuthSession();
    const sound = createSoundSystem();

    setProgress(15, 'Initializing WebGL hardware renderer...');
    await sleep(30);

    const renderer = new THREE.WebGLRenderer({
      canvas: ui.canvas,
      antialias: false,
      powerPreference: 'high-performance'
    });

    let isSoftwareGpu = false;
    try {
      const gl = renderer.getContext();
      const dbg = gl && gl.getExtension('WEBGL_DEBUG_RENDERER_INFO');
      const gpuStr = String(
        (dbg && gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL)) ||
          (gl && gl.getParameter(gl.RENDERER)) ||
          ''
      );
      isSoftwareGpu = /swiftshader|llvmpipe|softpipe|emulator|subzero/i.test(gpuStr);
    } catch (e) {
      isSoftwareGpu = false;
    }

    let contextLost = false;
    let isHostPaused = Boolean(pendingBridgePayloads.paused);
    ui.canvas.addEventListener('webglcontextlost', (e) => {
      e.preventDefault();
      contextLost = true;
    }, false);
    ui.canvas.addEventListener('webglcontextrestored', () => {
      contextLost = false;
    }, false);

    const urlEcoMode = new URLSearchParams(window.location.search).get('eco') === '1';
    let highQuality = !isSoftwareGpu && !urlEcoMode;
    const applyRendererQuality = () => {
      const cap = (isSoftwareGpu || urlEcoMode) ? 0.75 : (highQuality ? 1.0 : 0.8);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, cap));
      const shadowsOn = !isSoftwareGpu && !urlEcoMode && highQuality;
      renderer.shadowMap.enabled = shadowsOn;
      renderer.shadowMap.type = THREE.PCFShadowMap;
      renderer.toneMapping = THREE.ACESFilmicToneMapping;
      renderer.toneMappingExposure = 1.08;
    };
    applyRendererQuality();
    if (ui.qualityLabel) {
      ui.qualityLabel.textContent = highQuality ? 'High' : 'Eco';
    }
    renderer.setSize(window.innerWidth, window.innerHeight, false);

    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x131a29, 0.008);

    const initAspect = window.innerWidth / Math.max(1, window.innerHeight);
    const camera = new THREE.PerspectiveCamera(
      initAspect < 1 ? 66 : 56,
      initAspect,
      0.15,
      95
    );

    const colliders = [];

    setProgress(32, 'Generating PBR marble, wood & mosaic textures...');
    await sleep(25);
    const textures = createTextureLibrary();

    setProgress(48, 'Constructing 13-room indoor mansion...');
    await sleep(25);
    const architecture = buildHouseArchitecture(scene, textures, colliders);
    const lighting = buildLightingSystem(scene, architecture.skylightMats);
    lighting.setShadowQuality(!isSoftwareGpu && highQuality);

    setProgress(62, 'Planting centerpiece tree & twisted vines...');
    await sleep(25);
    const centerTree = buildCenterTree(scene, colliders);

    setProgress(74, 'Installing doors, pool, switches, fans & furniture...');
    await sleep(25);
    const doorManager = buildGlassSlidingDoors(scene, architecture.doorSpecs, colliders);
    const furnitureManager = buildAllRoomFurniture(scene, textures, colliders, lighting);

    setProgress(82, 'Mounting glowing Bigg Boss Eyes & CCTV array...');
    await sleep(20);
    const biggBossEyes = buildBiggBossEyes(scene, lighting);
    const cctvManager = createCctvSystem(scene);
    const tasks3d = createTasks3dManager(scene);

    setProgress(90, 'Loading rigged 3D human character models & animations...');
    const housemates = createHousematesManager(scene, colliders);
    await housemates.loadModels();

    const player = createPlayerController(
      scene,
      camera,
      colliders,
      housemates.getActiveCharacter(),
      furnitureManager
    );

    let hasEnteredHouse = false;
    let isRtdbConfigured = false;
    let seasonMembers = [];
    let remotePlayersSnapshot = [];
    let pendingCharId = housemates.getActiveCharacter().id;
    let lastLocalSeatId = null;
    let cctvActive = false;
    let cctvFocusCamId = null;
    let cctvRoomFilter = 'all';
    let cctvPageIndex = 0;
    let currentPhase6Task = null;
    let kitchenHudState = { energy: 75, hunger: 30, cookingCount: 0, readyMealsCount: 0, taskProgressPercent: 0, lastAnimNonce: 0 };
    let lastSpeakerPulseNonce = 0;
    let lastCelebrationKey = '';
    let lastSirenState = false;
    const cctvTileEls = new Map();

    function computeOnlineCount() {
      const onlineUids = new Set();
      if (auth.isAuthenticated && auth.uid && !auth.isAdmin()) {
        onlineUids.add(auth.uid);
      }
      for (const m of seasonMembers) {
        if (m && m.online && m.userId && m.role !== ROLES.ADMIN) onlineUids.add(m.userId);
      }
      for (const rp of remotePlayersSnapshot) {
        if (rp && rp.online && rp.uid && rp.role !== ROLES.ADMIN) onlineUids.add(rp.uid);
      }
      return Math.min(15, onlineUids.size);
    }

    function updatePlayerCountsUi() {
      const activeCount = computeOnlineCount();
      ui.onlineCountLabel.textContent = `${activeCount}/15`;
      ui.adminOccupancyVal.textContent = `${activeCount} / 15 Active`;
      ui.playersSyncBadge.textContent = isRtdbConfigured
        ? `LIVE REALTIME SYNC • ${activeCount}/15 ONLINE`
        : `LOBBY SYNC MODE • ENABLE RTDB FOR LIVE TRANSFORMS`;
    }

    function renderPlayersModalList() {
      const mergedByUid = new Map();
      for (const m of seasonMembers) {
        if (!m || !m.userId || m.role === ROLES.ADMIN) continue;
        mergedByUid.set(m.userId, {
          uid: m.userId,
          playerId: m.playerId || 'Player',
          displayName: m.displayName || 'Housemate',
          characterId: m.characterId || 'Player 01',
          slotNumber: m.slotNumber || 0,
          role: ROLES.PLAYER,
          online: Boolean(m.online),
          pose: m.userId === auth.uid ? player.networkPose : 'idle'
        });
      }
      for (const rp of remotePlayersSnapshot) {
        if (!rp || !rp.uid || rp.role === ROLES.ADMIN) continue;
        const prev = mergedByUid.get(rp.uid) || {};
        mergedByUid.set(rp.uid, {
          ...prev,
          uid: rp.uid,
          playerId: rp.playerId || prev.playerId || 'Player',
          displayName: rp.displayName || prev.displayName || 'Housemate',
          characterId: rp.characterId || prev.characterId || 'Player 01',
          online: Boolean(rp.online && rp.state !== 'offline'),
          pose: rp.state || 'idle'
        });
      }
      // Secret Admin Identity: NEVER add Admin owner into normal housemate lists
      if (auth.isAuthenticated && auth.uid && !auth.isAdmin() && !mergedByUid.has(auth.uid)) {
        mergedByUid.set(auth.uid, {
          uid: auth.uid,
          playerId: auth.playerId,
          displayName: auth.displayName,
          characterId: auth.characterId,
          role: ROLES.PLAYER,
          online: true,
          pose: player.networkPose
        });
      }

      const entries = Array.from(mergedByUid.values()).sort((a, b) => {
        if (a.uid === auth.uid) return -1;
        if (b.uid === auth.uid) return 1;
        if (a.online !== b.online) return a.online ? -1 : 1;
        return (a.slotNumber || 99) - (b.slotNumber || 99);
      });

      if (entries.length === 0) {
        ui.playersList.innerHTML = '<div class="player-row"><span class="player-row-sub">No authenticated housemates in this season yet.</span></div>';
        return;
      }

      ui.playersList.innerHTML = entries.map((entry) => {
        const isSelf = entry.uid === auth.uid;
        const badgeText = isSelf
          ? 'YOU • LIVE'
          : (entry.online ? `ONLINE • ${String(entry.pose || 'idle').toUpperCase()}` : 'OFFLINE');
        const dotClass = entry.online ? 'online' : 'offline';
        const badgeClass = isSelf ? 'self-badge' : (entry.online ? 'live-badge' : 'offline-badge');
        return `
          <div class="player-row">
            <div class="player-row-left">
              <span class="presence-dot ${dotClass}"></span>
              <div>
                <div class="player-row-name">${escapeHtml(entry.displayName)}</div>
                <div class="player-row-sub">${escapeHtml(entry.playerId)} • ${escapeHtml(entry.characterId)}</div>
              </div>
            </div>
            <span class="player-row-badge ${badgeClass}">${escapeHtml(badgeText)}</span>
          </div>
        `;
      }).join('');
    }

    let activeRecorder = null;
    let recChunks = [];
    let recStartMs = 0;
    let recCamId = 'all';
    let recCamLabel = '7-Camera Matrix';
    let recTimerInterval = null;
    let recAutoStopTimer = null;

    function stopCctvRecording(reason = 'stopped') {
      if (!activeRecorder) return;
      clearInterval(recTimerInterval);
      clearTimeout(recAutoStopTimer);
      try {
        if (activeRecorder.state === 'recording') {
          activeRecorder.stop();
        }
      } catch (err) {
        console.warn('Recorder stop error:', err);
      }
      if (ui.btnCctvRecord) {
        ui.btnCctvRecord.classList.remove('recording');
        ui.btnCctvRecord.textContent = '⏺ Record Feed';
      }
      if (ui.cctvRecTimer) {
        ui.cctvRecTimer.classList.add('hidden');
      }
      if (reason === 'auto_limit') {
        showToast('45s max clip reached — saving CCTV recording...');
      }
    }

    function startCctvRecording() {
      const perm = auth.assertAdminPermission('RECORD_CCTV');
      if (!perm.allowed) {
        sound.playDenied();
        showToast(perm.error);
        return;
      }
      if (activeRecorder && activeRecorder.state === 'recording') {
        stopCctvRecording('manual');
        return;
      }
      if (typeof ui.canvas.captureStream !== 'function' || typeof MediaRecorder === 'undefined') {
        const errMsg = 'MediaRecorder/Canvas stream capture is unavailable on this WebView.';
        showToast(errMsg);
        bridgeCall('onCctvRecordingError', errMsg);
        return;
      }

      try {
        const stream = ui.canvas.captureStream(12);
        const mimeTypes = [
          'video/webm;codecs=vp9',
          'video/webm;codecs=vp8',
          'video/webm'
        ];
        const chosenMime = mimeTypes.find((m) => MediaRecorder.isTypeSupported(m)) || '';
        const options = chosenMime
          ? { mimeType: chosenMime, videoBitsPerSecond: 400000 }
          : { videoBitsPerSecond: 400000 };

        recChunks = [];
        recStartMs = Date.now();
        const focusedFeed = cctvFocusCamId ? cctvManager.getFeedById(cctvFocusCamId) : null;
        recCamId = focusedFeed ? focusedFeed.id : 'cam_matrix';
        recCamLabel = focusedFeed ? `${focusedFeed.code} • ${focusedFeed.label}` : '24-Camera Common Quad Matrix';

        const recorder = new MediaRecorder(stream, options);
        activeRecorder = recorder;

        recorder.ondataavailable = (ev) => {
          if (ev.data && ev.data.size > 0) recChunks.push(ev.data);
        };

        recorder.onerror = (ev) => {
          const msg = ev?.error?.message || 'CCTV video encoding failed.';
          stopCctvRecording('error');
          activeRecorder = null;
          showToast(msg);
          bridgeCall('onCctvRecordingError', msg);
        };

        recorder.onstop = () => {
          const durationMs = Math.max(500, Date.now() - recStartMs);
          const finalMime = recorder.mimeType || chosenMime || 'video/webm';
          const chunksCopy = recChunks.slice();
          recChunks = [];
          activeRecorder = null;
          stream.getTracks().forEach((t) => t.stop());

          if (chunksCopy.length === 0) {
            bridgeCall('onCctvRecordingError', 'No video frames captured.');
            return;
          }
          const blob = new Blob(chunksCopy, { type: finalMime });
          if (blob.size > 14 * 1024 * 1024) {
            bridgeCall('onCctvRecordingError', 'Clip exceeded 14 MB limit; record a shorter segment.');
            return;
          }
          const reader = new FileReader();
          reader.onloadend = () => {
            const dataUrl = String(reader.result || '');
            const commaIdx = dataUrl.indexOf(',');
            const b64 = commaIdx >= 0 ? dataUrl.slice(commaIdx + 1) : '';
            if (!b64) {
              bridgeCall('onCctvRecordingError', 'Failed to encode recorded CCTV clip.');
              return;
            }
            bridgeCall('onCctvRecordingCaptured', recCamId, recCamLabel, recStartMs, durationMs, finalMime, b64);
            showToast(`Uploading ${recCamLabel} (${Math.round(durationMs / 1000)}s)...`);
          };
          reader.onerror = () => {
            bridgeCall('onCctvRecordingError', 'Failed to read captured CCTV video blob.');
          };
          reader.readAsDataURL(blob);
        };

        recorder.start(800);
        ui.btnCctvRecord.classList.add('recording');
        ui.btnCctvRecord.textContent = '⏹ Stop & Save';
        ui.cctvRecTimer.textContent = '00:00';
        ui.cctvRecTimer.classList.remove('hidden');

        recTimerInterval = setInterval(() => {
          const sec = Math.floor((Date.now() - recStartMs) / 1000);
          const mm = String(Math.floor(sec / 60)).padStart(2, '0');
          const ss = String(sec % 60).padStart(2, '0');
          ui.cctvRecTimer.textContent = `${mm}:${ss}`;
        }, 500);
        recAutoStopTimer = setTimeout(() => stopCctvRecording('auto_limit'), 45000);
        showToast(`Recording LIVE: ${recCamLabel}`);
      } catch (err) {
        const msg = err?.message || 'Unable to start live CCTV capture.';
        activeRecorder = null;
        showToast(msg);
        bridgeCall('onCctvRecordingError', msg);
      }
    }

    function closeCctvMode() {
      stopCctvRecording('exit');
      cctvActive = false;
      cctvFocusCamId = null;
      renderer.setScissorTest(false);
      renderer.setViewport(0, 0, window.innerWidth, Math.max(1, window.innerHeight));
      ui.cctvOverlay.classList.add('hidden');
    }

    function buildCctvGridTiles() {
      ui.cctvGridLayer.innerHTML = '';
      cctvTileEls.clear();
      const pageInfo = cctvManager.getPageFeeds(cctvRoomFilter, cctvPageIndex);
      cctvPageIndex = pageInfo.pageIndex;
      if (ui.cctvPageLabel) {
        ui.cctvPageLabel.textContent = `${pageInfo.pageIndex + 1} / ${pageInfo.pageCount}`;
      }
      pageInfo.feeds.forEach((feed) => {
        const tile = document.createElement('button');
        tile.type = 'button';
        tile.className = 'cctv-tile';
        tile.dataset.camId = feed.id;
        tile.setAttribute('aria-label', `Focus ${feed.label}`);
        tile.innerHTML = `
          <div class="cctv-tile-top">
            <span class="cctv-cam-label">${escapeHtml(feed.code)} • ${escapeHtml(feed.label)}</span>
            <span class="cctv-status-badge ${feed.online ? '' : 'offline'}">${feed.online ? 'LIVE' : 'OFFLINE'}</span>
          </div>
          <div class="cctv-tile-players"></div>
        `;
        tile.addEventListener('click', () => {
          sound.playUiTap();
          enterCctvMode(feed.id);
        });
        ui.cctvGridLayer.appendChild(tile);
        cctvTileEls.set(feed.id, {
          el: tile,
          playersBox: tile.querySelector('.cctv-tile-players')
        });
      });
    }

    function syncCctvChrome() {
      const isFocused = Boolean(cctvFocusCamId);
      ui.cctvGridLayer.classList.toggle('hidden', isFocused);
      ui.cctvFullscreenHud.classList.toggle('hidden', !isFocused);
      ui.btnCctvPrev.classList.toggle('hidden', !isFocused);
      ui.btnCctvNext.classList.toggle('hidden', !isFocused);
      ui.btnCctvGrid.classList.toggle('hidden', !isFocused);
      if (ui.btnCctvPagePrev) ui.btnCctvPagePrev.classList.toggle('hidden', isFocused);
      if (ui.btnCctvPageNext) ui.btnCctvPageNext.classList.toggle('hidden', isFocused);
      if (ui.cctvPageLabel) ui.cctvPageLabel.classList.toggle('hidden', isFocused);

      if (isFocused) {
        const feed = cctvManager.getFeedById(cctvFocusCamId);
        ui.cctvHeaderTitle.textContent = `${feed.code} • ${feed.label} (${feed.online ? 'LIVE' : 'OFFLINE'})`;
        ui.cctvFsBadge.textContent = `${feed.code} • ${feed.label.toUpperCase()} • PRIVACY SAFE`;
      } else {
        buildCctvGridTiles();
        const layout = cctvManager.computeGridLayout(window.innerWidth, Math.max(1, window.innerHeight), cctvRoomFilter, cctvPageIndex);
        ui.cctvHeaderTitle.textContent = `24-Cam Matrix • ${cctvRoomFilter === 'all' ? 'All Common Rooms' : cctvRoomFilter.toUpperCase()} (${layout.totalFiltered})`;
        layout.tiles.forEach((tile) => {
          const entry = cctvTileEls.get(tile.feed.id);
          if (!entry) return;
          entry.el.style.left = `${tile.cssLeft}px`;
          entry.el.style.top = `${tile.cssTop}px`;
          entry.el.style.width = `${tile.width}px`;
          entry.el.style.height = `${tile.height}px`;
        });
      }
    }

    function updateCctvOccupantBadges() {
      if (!cctvActive || !auth.isAdmin()) return;
      const localPos = player.position;
      const localInfo = {
        active: hasEnteredHouse && auth.isAuthenticated,
        x: localPos.x,
        z: localPos.z,
        displayName: auth.displayName || 'Admin'
      };
      const remotePos = housemates.getRemotePositions();
      const npcPos = housemates.getNpcPositions();

      const renderChips = (occupants) => {
        if (occupants.length === 0) {
          return '<span class="cctv-player-chip offline">Zone Empty</span>';
        }
        return occupants.map((o) =>
          `<span class="cctv-player-chip ${o.online ? '' : 'offline'}">${o.online ? '● ' : '○ '}${escapeHtml(o.label)}</span>`
        ).join('');
      };

      if (cctvFocusCamId) {
        const feed = cctvManager.getFeedById(cctvFocusCamId);
        ui.cctvFsPlayers.innerHTML = renderChips(
          cctvManager.getPlayersInFeed(feed, localInfo, remotePos, npcPos)
        );
      } else {
        const pageInfo = cctvManager.getPageFeeds(cctvRoomFilter, cctvPageIndex);
        pageInfo.feeds.forEach((feed) => {
          const entry = cctvTileEls.get(feed.id);
          if (!entry) return;
          entry.playersBox.innerHTML = renderChips(
            cctvManager.getPlayersInFeed(feed, localInfo, remotePos, npcPos)
          );
        });
      }
    }

    function enterCctvMode(focusCamId = null) {
      const perm = auth.assertAdminPermission('CCTV_FEED');
      if (!perm.allowed) {
        closeCctvMode();
        sound.playDenied();
        showToast(perm.error);
        return false;
      }
      ui.adminModal.classList.add('hidden');
      cctvActive = true;
      cctvFocusCamId = focusCamId;
      ui.cctvOverlay.classList.remove('hidden');
      syncCctvChrome();
      updateCctvOccupantBadges();
      bridgeCall('onAdminControlAction', 'CCTV_SWITCH', focusCamId ? `Viewing ${focusCamId}` : 'Viewing 24-Cam Quad Matrix');
      return true;
    }

    function syncRoleAccess() {
      const isAdmin = auth.isAdmin();
      const canAccessVip = typeof auth.canAccessVipRoom === 'function' ? auth.canAccessVipRoom() : isAdmin;
      architecture.setAdminAccessVisible(isAdmin);
      if (typeof architecture.setVipAccessVisible === 'function') {
        architecture.setVipAccessVisible(canAccessVip);
      }
      doorManager.syncRoleVisibility(auth);
      furnitureManager.syncRoleVisibility(auth);
      lighting.setAdminAccessVisible(isAdmin);
      ui.btnAdminDash.classList.toggle('hidden', !isAdmin);
      if (ui.btnAdminPhone) {
        ui.btnAdminPhone.classList.toggle('hidden', !isAdmin);
      }

      if (!isAdmin) {
        ui.adminModal.classList.add('hidden');
        closeCctvMode();
        // Safety clamp: ensure non-admin character can never remain inside Admin Control Room
        const active = housemates.getActiveCharacter();
        if (active.x >= 13.7 && active.x <= 27 && active.z >= -8.2 && active.z <= 0.2) {
          if (player.isSeated || player.isSleeping) player.standUp();
          active.x = 11.8;
          active.z = -4.5;
          active.syncTransform(0);
        }
      }

      if (!canAccessVip) {
        const active = housemates.getActiveCharacter();
        if (active.x >= 10.1 && active.x <= 18.9 && active.z >= -21.9 && active.z <= -14.1) {
          if (player.isSeated || player.isSleeping) player.standUp();
          active.x = 14.5;
          active.z = -12.2;
          active.syncTransform(0);
          showToast('VIP Room access revoked. Moved to hallway.');
        }
      }

      const activeChar = housemates.getActiveCharacter();
      const labelName = auth.isAuthenticated ? auth.displayName : activeChar.name;
      const labelId = auth.isAuthenticated ? auth.playerId : activeChar.id;
      const statusBadge = auth.isCaptain() ? ' • CAPTAIN' : (auth.isVip() ? ' • VIP' : '');
      ui.activePlayerTag.textContent = `${String(labelId).toUpperCase()} • ${String(labelName).toUpperCase()} • ${auth.role}${statusBadge}`;
    }

    syncRoleAccess();

    // Build 15-Avatar Selection Grid (all housemates remain ROLES.PLAYER unless server /admins/{uid} verifies ADMIN)
    function refreshCharacterGrid() {
      const cards = ui.charGrid.querySelectorAll('.char-card');
      cards.forEach((card) => {
        card.classList.toggle('selected', card.dataset.charId === pendingCharId);
      });
      const prof = HOUSEMATE_PROFILES.find((p) => p.id === pendingCharId) || HOUSEMATE_PROFILES[0];
      ui.charSelectedSummary.textContent = `${prof.id} • ${prof.name} (${auth.role}) — ${prof.spawn.room}`;
      ui.btnConfirmChar.textContent = `Switch Avatar to ${prof.name}`;
    }

    HOUSEMATE_PROFILES.forEach((prof) => {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = 'char-card';
      card.dataset.charId = prof.id;
      const gLabel = prof.gender === 'female' ? 'Female' : 'Male';
      card.innerHTML = `
        <div class="char-card-top">
          <div class="char-swatch" style="background:${toHex(prof.appearance.top)}">
            <div class="char-swatch-hair" style="background:${toHex(prof.appearance.hairColor)}"></div>
            <div class="char-swatch-accent" style="background:${toHex(prof.appearance.accent)}"></div>
          </div>
          <div>
            <div class="char-id">${prof.id.toUpperCase()}</div>
            <div class="char-name">${prof.name}</div>
          </div>
        </div>
        <div class="char-meta">${gLabel} • ${prof.archetype} • ${prof.spawn.room}</div>
      `;
      card.addEventListener('click', () => {
        pendingCharId = prof.id;
        sound.playUiTap();
        refreshCharacterGrid();
      });
      ui.charGrid.appendChild(card);
    });
    refreshCharacterGrid();

    let lastNetSendTime = 0;
    let lastNetState = { x: NaN, y: NaN, z: NaN, rotY: NaN, pose: '', seatId: '', expr: 'neutral' };

    function publishTransform(force = false) {
      if (!hasEnteredHouse || !auth.isAuthenticated) return;
      const now = performance.now();
      const pos = player.position;
      const yaw = player.yaw;
      const pose = player.networkPose;
      const seatId = player.seatedTarget ? player.seatedTarget.id : '';
      const expr = player.expression || 'neutral';

      const poseChanged = pose !== lastNetState.pose || seatId !== lastNetState.seatId || expr !== lastNetState.expr;
      const moved =
        Number.isNaN(lastNetState.x) ||
        Math.hypot(pos.x - lastNetState.x, pos.z - lastNetState.z) > 0.025 ||
        Math.abs(pos.y - lastNetState.y) > 0.02 ||
        Math.abs(yaw - lastNetState.rotY) > 0.04;

      if (!force && !poseChanged && (!moved || now - lastNetSendTime < 82)) {
        return;
      }

      lastNetSendTime = now;
      lastNetState = { x: pos.x, y: pos.y, z: pos.z, rotY: yaw, pose, seatId, expr };
      bridgeCall('onPlayerTransform', pos.x, pos.y, pos.z, yaw, pose, seatId, expr);
    }

    function applySelectedCharacter() {
      if (!auth.isAuthenticated) {
        sound.playDenied();
        showToast('Sign in required to enter the house.');
        return;
      }
      if (lastLocalSeatId) {
        bridgeCall('onSeatOccupancyChanged', lastLocalSeatId, false);
        lastLocalSeatId = null;
      }
      const selected = housemates.selectCharacter(
        pendingCharId,
        furnitureManager,
        { uid: auth.uid, playerId: auth.playerId, displayName: auth.displayName }
      );
      auth.setActiveHousemate(selected.id);
      player.bindCharacter(selected);
      syncRoleAccess();
      ui.btnCloseCast.classList.remove('hidden');
      ui.charSelectModal.classList.add('hidden');
      ui.hud.classList.remove('hidden');
      sound.playUiTap();
      bridgeCall('onCharacterChanged', selected.id);
      publishTransform(true);
      showToast(`Avatar switched to ${selected.name} (${auth.playerId})`);
    }

    ui.btnConfirmChar.addEventListener('click', applySelectedCharacter);
    ui.btnHouseHub.addEventListener('click', () => {
      sound.playUiTap();
      bridgeCall('onOpenHouseHub');
    });
    if (ui.btnKitchen) {
      ui.btnKitchen.addEventListener('click', () => {
        sound.playUiTap();
        if (!bridgeCall('onOpenKitchenDialog', 0, '')) {
          showToast('Kitchen Pantry & Cooking Studio');
        }
      });
    }
    if (ui.kitchenStatusPill) {
      ui.kitchenStatusPill.addEventListener('click', () => {
        sound.playUiTap();
        const tab = kitchenHudState.readyMealsCount > 0 ? 2 : 0;
        bridgeCall('onOpenKitchenDialog', tab, '');
      });
    }
    ui.activeTaskHud.addEventListener('click', () => {
      sound.playUiTap();
      bridgeCall('onOpenHouseHub');
    });
    ui.btnCast.addEventListener('click', () => {
      if (!auth.isAuthenticated) return;
      pendingCharId = housemates.getActiveCharacter().id;
      refreshCharacterGrid();
      ui.charSelectModal.classList.remove('hidden');
    });
    ui.btnCloseCast.addEventListener('click', () => {
      ui.charSelectModal.classList.add('hidden');
    });

    function openAdminDashboard() {
      const perm = auth.assertAdminPermission('OPEN_ADMIN_DASHBOARD');
      if (!perm.allowed) {
        sound.playDenied();
        showToast(perm.error);
        return false;
      }
      // If hosted inside Android, prefer the full native 5-tab Admin Control Suite
      if (bridgeCall('onOpenAdminConsole')) {
        return true;
      }
      const adminDoor = doorManager.doors.find((d) => d.id === 'door_admin');
      ui.adminDoorStatus.textContent = adminDoor && adminDoor.isOpen ? 'Door Open (ADMIN)' : 'Door Closed (ADMIN)';
      updatePlayerCountsUi();
      ui.adminModal.classList.remove('hidden');
      return true;
    }

    function closeAdminDashboard() {
      ui.adminModal.classList.add('hidden');
    }

    ui.btnAdminDash.addEventListener('click', () => {
      sound.playUiTap();
      const perm = auth.assertAdminPermission('OPEN_ADMIN_DASHBOARD');
      if (!perm.allowed) {
        sound.playDenied();
        showToast(perm.error);
        return;
      }
      if (!bridgeCall('onOpenAdminConsole')) {
        openAdminDashboard();
      }
    });
    ui.btnCloseAdmin.addEventListener('click', closeAdminDashboard);
    ui.adminModal.addEventListener('click', (e) => {
      if (e.target === ui.adminModal) closeAdminDashboard();
    });

    ui.btnAdminCctv.addEventListener('click', () => {
      sound.playUiTap();
      enterCctvMode(null);
    });
    ui.btnCloseCctv.addEventListener('click', () => {
      sound.playUiTap();
      closeCctvMode();
    });
    if (ui.btnCctvControlRoom) {
      ui.btnCctvControlRoom.addEventListener('click', () => {
        sound.playUiTap();
        closeCctvMode();
        if (auth.isAdmin()) {
          const active = housemates.getActiveCharacter();
          if (player.isSeated || player.isSleeping) player.standUp();
          active.x = 19.4;
          active.z = -2.8;
          active.rotY = -Math.PI / 2;
          active.syncTransform(0);
          player.resetCamera();
          showToast('Returned to Private Control Room');
        }
      });
    }
    if (ui.cctvRoomFilter) {
      ui.cctvRoomFilter.addEventListener('change', () => {
        cctvRoomFilter = ui.cctvRoomFilter.value || 'all';
        cctvPageIndex = 0;
        cctvFocusCamId = null;
        syncCctvChrome();
        updateCctvOccupantBadges();
      });
    }
    if (ui.btnCctvPagePrev) {
      ui.btnCctvPagePrev.addEventListener('click', () => {
        sound.playUiTap();
        cctvPageIndex -= 1;
        syncCctvChrome();
        updateCctvOccupantBadges();
      });
    }
    if (ui.btnCctvPageNext) {
      ui.btnCctvPageNext.addEventListener('click', () => {
        sound.playUiTap();
        cctvPageIndex += 1;
        syncCctvChrome();
        updateCctvOccupantBadges();
      });
    }
    ui.btnCctvGrid.addEventListener('click', () => {
      sound.playUiTap();
      enterCctvMode(null);
    });
    if (ui.btnCctvRecord) {
      ui.btnCctvRecord.addEventListener('click', () => {
        sound.playUiTap();
        startCctvRecording();
      });
    }
    ui.btnCctvPrev.addEventListener('click', () => {
      sound.playUiTap();
      const feeds = cctvManager.getFilteredFeeds(cctvRoomFilter);
      const idx = feeds.findIndex((f) => f.id === cctvFocusCamId);
      const prevIdx = (idx - 1 + feeds.length) % feeds.length;
      enterCctvMode(feeds[prevIdx].id);
    });
    ui.btnCctvNext.addEventListener('click', () => {
      sound.playUiTap();
      const feeds = cctvManager.getFilteredFeeds(cctvRoomFilter);
      const idx = feeds.findIndex((f) => f.id === cctvFocusCamId);
      const nextIdx = (idx + 1) % feeds.length;
      enterCctvMode(feeds[nextIdx].id);
    });

    ui.btnAdminDoorToggle.addEventListener('click', () => {
      const perm = auth.assertAdminPermission('DOOR_OVERRIDE');
      if (!perm.allowed) {
        sound.playDenied();
        showToast(perm.error);
        return;
      }
      const res = doorManager.attemptToggleDoor('door_admin', auth);
      if (res.ok) {
        sound.playDoorSlide(res.isOpen);
        ui.adminDoorStatus.textContent = res.isOpen ? 'Door Open (ADMIN)' : 'Door Closed (ADMIN)';
        bridgeCall('onDoorToggled', 'door_admin', res.isOpen);
        showToast(`Admin Control Door: ${res.isOpen ? 'Opened' : 'Closed'}`);
      }
    });

    ui.btnAdminBroadcast.addEventListener('click', () => {
      const perm = auth.assertAdminPermission('HOUSE_BROADCAST');
      if (!perm.allowed) {
        sound.playDenied();
        showToast(perm.error);
        return;
      }
      sound.playUiTap();
      bridgeCall('onAdminControlAction', 'HOUSE_BROADCAST', 'Admin broadcast logged from Control Room');
      showToast('Admin Control Broadcast Recorded');
    });

    let activePrompt = null; // { kind: 'door' | 'pool' | 'furniture', target, poolInfo }

    function handleExitMode() {
      if (!hasEnteredHouse) return false;
      if (player.isSwimming) {
        player.exitPool();
        sound.playPoolSplash(false);
        publishTransform(true);
        showToast('Exited Indoor Pool');
        return true;
      }
      if (player.isSleeping) {
        const prevSeat = player.seatedTarget ? player.seatedTarget.id : null;
        player.standUp();
        if (prevSeat) {
          bridgeCall('onSeatOccupancyChanged', prevSeat, false);
          lastLocalSeatId = null;
        }
        sound.playStand();
        publishTransform(true);
        showToast('Woke up from bed');
        return true;
      }
      if (player.isSeated) {
        const prevSeat = player.seatedTarget ? player.seatedTarget.id : null;
        player.standUp();
        if (prevSeat) {
          bridgeCall('onSeatOccupancyChanged', prevSeat, false);
          lastLocalSeatId = null;
        }
        sound.playStand();
        publishTransform(true);
        showToast('Stood up');
        return true;
      }
      return false;
    }

    function handlePrimaryInteract() {
      if (!hasEnteredHouse) return;
      if (handleExitMode()) return;
      if (!activePrompt) return;

      const actorId = housemates.getActiveCharacter().id;

      if (activePrompt.kind === 'task_relic') {
        const collected = tasks3d.collectRelic(activePrompt.target.id);
        if (collected) {
          sound.playUiTap();
          bridgeCall('onTaskObjectCollected', collected.taskId, collected.node.id);
          const prog = tasks3d.getProgress();
          showToast(`Collected ${collected.node.label} (${prog.collected}/${prog.total})!`);
        }
      } else if (activePrompt.kind === 'door') {
        const res = doorManager.attemptToggleDoor(activePrompt.target.id, auth);
        if (!res.ok) {
          sound.playDenied();
          showToast(res.reason || 'Access denied.');
          return;
        }
        sound.playDoorSlide(res.isOpen);
        bridgeCall('onDoorToggled', activePrompt.target.id, res.isOpen);
        showToast(`${activePrompt.target.label}: ${res.isOpen ? 'Opened' : 'Closed'}`);
      } else if (activePrompt.kind === 'pool') {
        player.enterPool(activePrompt.poolInfo);
        sound.playPoolSplash(true);
        publishTransform(true);
        showToast('Swimming in Indoor Pool');
      } else if (activePrompt.kind === 'furniture') {
        const res = furnitureManager.attemptInteract(activePrompt.target.id, auth, actorId);
        if (!res.ok) {
          sound.playDenied();
          showToast(res.reason || 'Action unavailable.');
          return;
        }
        const item = res.item;
        if (item.type === 'ai_operator_console') {
          sound.playUiTap();
          if (!bridgeCall('onOpenAiOperatorConsole')) {
            showToast('ChatGPT AI Operator Console available to Admin');
          }
        } else if (item.type === 'admin_dashboard') {
          openAdminDashboard();
        } else if (item.type === 'cctv_wall') {
          sound.playUiTap();
          enterCctvMode(null);
        } else if (item.type === 'admin_phone') {
          sound.playUiTap();
          if (!bridgeCall('onOpenAdminPhone')) {
            showToast('Main Admin Personal Phone available in APK');
          }
        } else if (item.type === 'kitchen') {
          if (item.kitchenAction === 'wash') {
            sound.playPoolSplash(false);
            player.startTimedActivity('wash', 3.0);
            publishTransform(true);
            showToast('Washed hands & refreshed at Kitchen Sink!');
          } else {
            const targetTab = item.kitchenAction === 'cook' || item.kitchenAction === 'prep'
              ? 1
              : (item.kitchenAction === 'eat' ? 2 : 0);
            sound.playUiTap();
            if (!bridgeCall('onOpenKitchenDialog', targetTab, item.id)) {
              showToast(`${item.label} ready`);
            }
          }
        } else if (item.type === 'light_switch') {
          const isOn = lighting.toggleRoomLight(item.roomId);
          sound.playSwitchClick(isOn);
          showToast(`${item.label}: ${isOn ? 'ON' : 'OFF'}`);
        } else if (item.type === 'fan_switch') {
          const isOn = lighting.toggleFan(item.roomId);
          sound.playFanToggle(isOn);
          showToast(`${item.label}: ${isOn ? 'ON' : 'OFF'}`);
        } else if (item.type === 'inspect') {
          sound.playUiTap();
          if (!bridgeCall('onOpenHouseHub')) {
            showToast('Challenge Table ready for house tasks!');
          }
        } else if (item.type === 'bed') {
          const sitRes = player.sitAt(item, 'sleep');
          if (!sitRes.ok) {
            sound.playDenied();
            showToast(sitRes.reason || 'Bed is occupied.');
            return;
          }
          lastLocalSeatId = item.id;
          bridgeCall('onSeatOccupancyChanged', item.id, true);
          sound.playSit(true);
          publishTransform(true);
          showToast(`Sleeping in ${item.label}`);
        } else {
          const sitRes = player.sitAt(item, 'sit');
          if (!sitRes.ok) {
            sound.playDenied();
            showToast(sitRes.reason || 'Seat is occupied.');
            return;
          }
          lastLocalSeatId = item.id;
          bridgeCall('onSeatOccupancyChanged', item.id, true);
          sound.playSit(false);
          publishTransform(true);
          showToast(`Sitting on ${item.label}`);
        }
      }
    }

    function handleSecondaryInteract() {
      if (!hasEnteredHouse) return;
      if (player.isSeated) {
        const seatId = player.seatedTarget ? player.seatedTarget.id : '';
        if (seatId.startsWith('dining_chair_') || seatId.startsWith('barstool_')) {
          sound.playUiTap();
          bridgeCall('onOpenKitchenDialog', 2, 'seated_dining');
        }
        return;
      }
      if (player.isSleeping || player.isSwimming) return;
      if (!activePrompt || activePrompt.kind !== 'furniture' || activePrompt.target.type !== 'bed') return;

      const actorId = housemates.getActiveCharacter().id;
      const res = furnitureManager.attemptInteract(activePrompt.target.id, auth, actorId);
      if (!res.ok) {
        sound.playDenied();
        showToast(res.reason || 'Bed is occupied.');
        return;
      }
      const sitRes = player.sitAt(res.item, 'bed_sit');
      if (!sitRes.ok) {
        sound.playDenied();
        showToast(sitRes.reason || 'Bed is occupied.');
        return;
      }
      lastLocalSeatId = res.item.id;
      bridgeCall('onSeatOccupancyChanged', res.item.id, true);
      sound.playSit(true);
      publishTransform(true);
      showToast(`Sitting on ${res.item.label}`);
    }

    const controls = setupControls({
      canvas: ui.canvas,
      joystickZone: ui.joystickZone,
      joystickBase: ui.joystickBase,
      joystickKnob: ui.joystickKnob,
      onOrbitCamera: (dy, dp) => player.orbitCamera(dy, dp),
      onZoomCamera: (delta) => {
        const prefs = player.adjustZoom(delta);
        bridgeCall('onCameraPrefsChanged', prefs.mode, prefs.distance, prefs.sensitivity);
      },
      onToggleInteract: handlePrimaryInteract,
      onAltInteract: handleSecondaryInteract,
      onToggleSprint: (active) => {
        player.setRunning(active);
        ui.btnSprint.classList.toggle('sprinting', active);
        ui.btnSprint.setAttribute('aria-pressed', String(active));
      }
    });

    const syncCamModeLabel = (mode) => {
      if (ui.camModeLabel) {
        ui.camModeLabel.textContent = mode === 'FIRST_PERSON' ? '1P' : (mode === 'FREE' ? 'Free' : '3P');
      }
    };

    if (ui.btnWardrobe) {
      ui.btnWardrobe.addEventListener('click', () => {
        sound.playUiTap();
        bridgeCall('onOpenWardrobe');
      });
    }
    if (ui.btnChat) {
      ui.btnChat.addEventListener('click', () => {
        sound.playUiTap();
        if (ui.chatUnreadDot) ui.chatUnreadDot.classList.add('hidden');
        bridgeCall('onOpenChat');
      });
    }
    if (ui.btnProfile) {
      ui.btnProfile.addEventListener('click', () => {
        sound.playUiTap();
        bridgeCall('onOpenProfile');
      });
    }
    if (ui.btnAdminPhone) {
      ui.btnAdminPhone.addEventListener('click', () => {
        sound.playUiTap();
        if (auth.isAdmin()) {
          bridgeCall('onOpenAdminPhone');
        } else {
          sound.playDenied();
        }
      });
    }
    if (ui.btnCamMode) {
      ui.btnCamMode.addEventListener('click', () => {
        sound.playUiTap();
        const prefs = player.cycleCameraMode();
        syncCamModeLabel(prefs.mode);
        bridgeCall('onCameraPrefsChanged', prefs.mode, prefs.distance, prefs.sensitivity);
        showToast(`Camera: ${prefs.mode.replace('_', ' ')}`);
      });
    }
    if (ui.btnZoomIn) {
      ui.btnZoomIn.addEventListener('click', () => {
        sound.playUiTap();
        const prefs = player.adjustZoom(-0.7);
        bridgeCall('onCameraPrefsChanged', prefs.mode, prefs.distance, prefs.sensitivity);
      });
    }
    if (ui.btnZoomOut) {
      ui.btnZoomOut.addEventListener('click', () => {
        sound.playUiTap();
        const prefs = player.adjustZoom(0.7);
        bridgeCall('onCameraPrefsChanged', prefs.mode, prefs.distance, prefs.sensitivity);
      });
    }

    ui.btnSprint.addEventListener('click', () => {
      const next = !player.isRunning();
      player.setRunning(next);
      ui.btnSprint.classList.toggle('sprinting', next);
      ui.btnSprint.setAttribute('aria-pressed', String(next));
    });

    if (ui.btnDance) {
      ui.btnDance.addEventListener('click', () => {
        if (!hasEnteredHouse) return;
        const active = player.toggleDance();
        ui.btnDance.classList.toggle('dancing', active);
        ui.btnDance.setAttribute('aria-pressed', String(active));
        sound.playUiTap();
        publishTransform(true);
        if (!active && (player.isSeated || player.isSwimming)) {
          showToast('Exit seat or pool first to dance.');
        } else {
          showToast(active ? 'Dancing!' : 'Stopped dancing');
        }
      });
    }

    if (ui.expressionBar) {
      ui.expressionBar.querySelectorAll('.expr-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
          const expr = btn.dataset.expr || 'neutral';
          player.setExpression(expr);
          ui.expressionBar.querySelectorAll('.expr-btn').forEach((b) => {
            b.classList.toggle('active', b === btn);
          });
          sound.playUiTap();
          publishTransform(true);
        });
      });
    }

    ui.btnInteract.addEventListener('click', handlePrimaryInteract);
    ui.btnInteractAlt.addEventListener('click', handleSecondaryInteract);
    ui.btnStand.addEventListener('click', handleExitMode);

    ui.btnTime.addEventListener('click', () => {
      const mode = lighting.cycleMode();
      ui.timeIcon.textContent = mode.icon;
      ui.timeLabel.textContent = mode.label;
      sound.playUiTap();
      bridgeCall('onLightingModeChanged', mode.id);
      showToast(`Lighting Mode: ${mode.label}`);
    });

    ui.btnQuality.addEventListener('click', () => {
      highQuality = !highQuality;
      applyRendererQuality();
      lighting.setShadowQuality(!isSoftwareGpu && !urlEcoMode && highQuality);
      ui.qualityLabel.textContent = highQuality ? 'High' : 'Eco';
      showToast(highQuality ? 'Graphics: High Detail' : 'Graphics: Eco Battery Saver');
    });

    ui.btnCamReset.addEventListener('click', () => {
      player.resetCamera();
      showToast('Camera Centered');
    });

    // Live Players Drawer & NPC Toggle & Lobby / Logout
    ui.btnPlayers.addEventListener('click', () => {
      sound.playUiTap();
      updatePlayerCountsUi();
      renderPlayersModalList();
      ui.btnToggleNpcs.textContent = housemates.areNpcsEnabled() ? 'NPCs: On' : 'NPCs: Off';
      ui.playersModal.classList.remove('hidden');
    });
    ui.btnClosePlayers.addEventListener('click', () => {
      ui.playersModal.classList.add('hidden');
    });
    ui.playersModal.addEventListener('click', (e) => {
      if (e.target === ui.playersModal) ui.playersModal.classList.add('hidden');
    });

    ui.btnToggleNpcs.addEventListener('click', () => {
      const next = !housemates.areNpcsEnabled();
      housemates.setNpcsEnabled(next, furnitureManager);
      ui.btnToggleNpcs.textContent = next ? 'NPCs: On' : 'NPCs: Off';
      sound.playUiTap();
      showToast(next ? 'Optional Ambient NPCs Enabled (Not Real Players)' : 'Optional Ambient NPCs Hidden');
    });

    const handleReturnLobby = () => {
      sound.playUiTap();
      if (!bridgeCall('onRequestReturnToLobby')) {
        showToast('Lobby screen is managed by the Android host.');
      }
    };
    ui.btnLobby.addEventListener('click', handleReturnLobby);
    ui.btnReturnLobby.addEventListener('click', handleReturnLobby);

    ui.btnHouseLogout.addEventListener('click', () => {
      sound.playUiTap();
      if (lastLocalSeatId) {
        bridgeCall('onSeatOccupancyChanged', lastLocalSeatId, false);
        lastLocalSeatId = null;
      }
      auth.signOut();
      hasEnteredHouse = false;
      ui.playersModal.classList.add('hidden');
      ui.hud.classList.add('hidden');
      ui.authGateScreen.classList.remove('hidden');
      bridgeCall('onRequestLogout');
    });

    let mapOpen = false;
    ui.btnMap.addEventListener('click', () => {
      mapOpen = true;
      ui.mapModal.classList.remove('hidden');
      renderMinimap(
        ui.minimapCanvas,
        player.position,
        player.yaw,
        doorManager.doors,
        housemates.getNpcPositions(),
        auth.isAdmin(),
        housemates.getRemotePositions()
      );
    });
    ui.btnCloseMap.addEventListener('click', () => {
      mapOpen = false;
      ui.mapModal.classList.add('hidden');
    });
    ui.mapModal.addEventListener('click', (e) => {
      if (e.target === ui.mapModal) {
        mapOpen = false;
        ui.mapModal.classList.add('hidden');
      }
    });

    window.closeGameOverlays = () => {
      if (cctvActive) {
        closeCctvMode();
        return true;
      }
      if (!ui.adminModal.classList.contains('hidden')) {
        closeAdminDashboard();
        return true;
      }
      if (!ui.playersModal.classList.contains('hidden')) {
        ui.playersModal.classList.add('hidden');
        return true;
      }
      if (mapOpen) {
        mapOpen = false;
        ui.mapModal.classList.add('hidden');
        return true;
      }
      if (!ui.charSelectModal.classList.contains('hidden')) {
        ui.charSelectModal.classList.add('hidden');
        return true;
      }
      return false;
    };

    // Wire live AndroidBridge -> Three.js Multiplayer Handlers
    function applySessionFromHost(payload) {
      const ok = auth.applyVerifiedSession(payload);
      if (!ok) {
        hasEnteredHouse = false;
        ui.hud.classList.add('hidden');
        ui.charSelectModal.classList.add('hidden');
        ui.loadingScreen.classList.add('hidden');
        ui.authGateScreen.classList.remove('hidden');
        return false;
      }

      isRtdbConfigured = Boolean(payload.isRtdbConfigured);
      const rawMembers = Array.isArray(payload.members) ? payload.members : [];
      seasonMembers = rawMembers.filter((m) => m && m.role !== ROLES.ADMIN);

      const desiredCharId = payload.characterId || auth.characterId || 'Player 01';
      const currentCharId = housemates.getActiveCharacter().id;
      const shouldBindChar = !hasEnteredHouse || currentCharId !== desiredCharId;

      if (shouldBindChar) {
        const selected = housemates.selectCharacter(
          desiredCharId,
          furnitureManager,
          { uid: auth.uid, playerId: auth.playerId, displayName: auth.displayName }
        );
        player.bindCharacter(selected);
        pendingCharId = selected.id;
      } else {
        housemates.setActiveUserIdentity(auth.playerId, auth.displayName, auth.uid);
      }

      syncRoleAccess();
      updatePlayerCountsUi();
      if (!ui.playersModal.classList.contains('hidden')) {
        renderPlayersModalList();
      }

      const firstEntry = !hasEnteredHouse;
      if (firstEntry && auth.isAdmin()) {
        // Secret Admin Identity: Spawn owner directly inside the private Control Room
        const activeChar = housemates.getActiveCharacter();
        activeChar.x = 19.4;
        activeChar.z = -2.8;
        activeChar.rotY = -Math.PI / 2;
        activeChar.syncTransform(0);
        player.resetCamera();
      }

      hasEnteredHouse = true;
      ui.btnCloseCast.classList.remove('hidden');
      ui.loadingScreen.classList.add('hidden');
      ui.authGateScreen.classList.add('hidden');
      ui.hud.classList.remove('hidden');

      if (payload.sessionSuperseded) {
        showToast('Session conflict: signed in on another device');
      } else if (firstEntry) {
        showToast(auth.isAdmin()
          ? 'Main Admin Owner verified in Private Control Room'
          : 'Common areas are under 24/7 CCTV monitoring. Bathrooms & Smoking Lounge remain private.');
      }
      publishTransform(true);
      return true;
    }

    function applyRemotePlayersFromHost(playersList) {
      const list = (Array.isArray(playersList) ? playersList : []).filter((p) => p && p.role !== ROLES.ADMIN);
      remotePlayersSnapshot = list;
      const others = list.filter((p) => p && p.uid && p.uid !== auth.uid);
      housemates.syncRemotePlayers(others);
      updatePlayerCountsUi();
      if (!ui.playersModal.classList.contains('hidden')) {
        renderPlayersModalList();
      }
    }

    function applyWorldStateFromHost(worldObj) {
      if (!worldObj || typeof worldObj !== 'object') return;
      if (worldObj.doors) {
        doorManager.applyRemoteDoorStates(worldObj.doors, auth);
        if (!ui.adminModal.classList.contains('hidden')) {
          const adminDoor = doorManager.doors.find((d) => d.id === 'door_admin');
          ui.adminDoorStatus.textContent = adminDoor && adminDoor.isOpen ? 'Door Open (ADMIN)' : 'Door Closed (ADMIN)';
        }
      }
      if (worldObj.seats) {
        furnitureManager.applyRemoteSeatStates(worldObj.seats, auth.uid);
      }
      if (worldObj.lightingMode) {
        const mode = lighting.setModeById(worldObj.lightingMode);
        if (mode) {
          ui.timeIcon.textContent = mode.icon;
          ui.timeLabel.textContent = mode.label;
        }
      }
    }

    function updateActiveTaskHudPill() {
      const playableStatuses = new Set(['ACTIVE', 'IN_PROGRESS', 'ASSIGNED', 'AVAILABLE']);
      if (!currentPhase6Task || !playableStatuses.has(currentPhase6Task.status)) {
        ui.activeTaskHud.classList.add('hidden');
        return;
      }
      ui.activeTaskHud.classList.remove('hidden');
      const catLabel = currentPhase6Task.category ? String(currentPhase6Task.category) : '';
      ui.taskHudBadge.textContent = currentPhase6Task.isCaptaincy
        ? 'CAPTAINCY'
        : (catLabel || (currentPhase6Task.assignmentMode === 'TEAM' ? 'TEAM TASK' : 'DAILY TASK'));
      ui.taskHudTitle.textContent = currentPhase6Task.title || 'Active House Task';
      if (ui.taskHudProgress) {
        const pct = Math.max(0, Math.min(100, Number(kitchenHudState.taskProgressPercent || 0)));
        ui.taskHudProgress.textContent = `${pct}%`;
        ui.taskHudProgress.classList.remove('hidden');
      }
      const remSec = Math.max(0, Math.floor(((Number(currentPhase6Task.endTimeMillis) || 0) - Date.now()) / 1000));
      const mm = String(Math.floor(remSec / 60)).padStart(2, '0');
      const ss = String(remSec % 60).padStart(2, '0');
      ui.taskHudTimer.textContent = `${mm}:${ss}`;
    }

    function applyPhase6FromHost(phase6Obj) {
      if (!phase6Obj || typeof phase6Obj !== 'object') return;
      if (typeof phase6Obj.captainUid === 'string') {
        housemates.setCaptainUid(phase6Obj.captainUid);
      }
      if (typeof housemates.setVipUids === 'function' && Array.isArray(phase6Obj.vipUids)) {
        housemates.setVipUids(phase6Obj.vipUids);
      }
      if (typeof auth.setVipPrivileges === 'function') {
        auth.setVipPrivileges({
          captainUid: typeof phase6Obj.captainUid === 'string' ? phase6Obj.captainUid : '',
          captainVipAccess: phase6Obj.captainVipAccess !== false,
          vipUids: Array.isArray(phase6Obj.vipUids) ? phase6Obj.vipUids : []
        });
      }
      syncRoleAccess();
      currentPhase6Task = phase6Obj.activeTask || null;
      const collected = Array.isArray(phase6Obj.collectedNodeIds) ? phase6Obj.collectedNodeIds : [];
      tasks3d.syncTaskState(currentPhase6Task, collected);
      updateActiveTaskHudPill();
    }

    function applyPhase7And8FromHost(payload) {
      if (!payload || typeof payload !== 'object') return;
      const energy = Math.max(0, Math.min(100, Number(payload.energy ?? 75)));
      const hunger = Math.max(0, Math.min(100, Number(payload.hunger ?? 30)));
      const cookingCount = Math.max(0, Number(payload.cookingCount || 0));
      const readyMealsCount = Math.max(0, Number(payload.readyMealsCount || 0));
      const taskProgressPercent = Math.max(0, Math.min(100, Number(payload.taskProgressPercent || 0)));

      kitchenHudState.energy = energy;
      kitchenHudState.hunger = hunger;
      kitchenHudState.cookingCount = cookingCount;
      kitchenHudState.readyMealsCount = readyMealsCount;
      kitchenHudState.taskProgressPercent = taskProgressPercent;

      if (ui.hudEnergyVal) ui.hudEnergyVal.textContent = `⚡ ${energy}%`;
      if (ui.hudHungerVal) ui.hudHungerVal.textContent = `🍽️ ${hunger}%`;
      if (ui.hudMealsBadge) {
        ui.hudMealsBadge.textContent = `🍲 ${readyMealsCount} Ready`;
        ui.hudMealsBadge.classList.toggle('hidden', readyMealsCount <= 0);
      }

      if (typeof furnitureManager.setKitchenVisualState === 'function') {
        furnitureManager.setKitchenVisualState({ cookingCount, readyMealsCount });
      }
      updateActiveTaskHudPill();

      const animNonce = Number(payload.animNonce || 0);
      if (animNonce > 0 && animNonce !== kitchenHudState.lastAnimNonce) {
        kitchenHudState.lastAnimNonce = animNonce;
        const animPose = String(payload.animPose || '');
        if (animPose) {
          player.startTimedActivity(animPose, 3.4);
          if (animPose === 'cook') sound.playCookStart();
          else if (animPose === 'eat' || animPose === 'sit_eat') sound.playEat();
          publishTransform(true);
        }
      }
    }

    function applyPhase9AiFromHost(payload) {
      if (!payload || typeof payload !== 'object') return;
      if (typeof furnitureManager.setAiOperatorVisualState === 'function') {
        furnitureManager.setAiOperatorVisualState(payload);
      }
    }

    function applyPhase12To20FromHost(payload) {
      if (!payload || typeof payload !== 'object') return;
      if (payload.wardrobe && typeof housemates.setActiveAppearance === 'function') {
        housemates.setActiveAppearance(payload.wardrobe);
      }
      if (payload.cameraPrefs && typeof player.setCameraPrefs === 'function') {
        const synced = player.setCameraPrefs(payload.cameraPrefs);
        syncCamModeLabel(synced.mode);
      }
      if (payload.clock && typeof payload.clock === 'object') {
        const c = payload.clock;
        if (ui.hudClockVal && c.formatted) ui.hudClockVal.textContent = c.formatted;
        if (ui.hudWeatherVal && c.weather) {
          const wIcon = c.weather === 'RAIN' ? '🌧️' : (c.weather === 'GOLDEN' ? '🌅' : (c.weather === 'NIGHT_MIST' ? '🌫️' : '☀️'));
          ui.hudWeatherVal.textContent = `${wIcon} ${c.weather}`;
        }
        if (c.timeMode) {
          const mode = lighting.setModeById(c.timeMode);
          if (mode) {
            ui.timeIcon.textContent = mode.icon;
            ui.timeLabel.textContent = mode.label;
          }
        }
        if (typeof lighting.setWeather === 'function' && c.weather) {
          lighting.setWeather(c.weather);
        }
        if (typeof lighting.setPartyMode === 'function') {
          lighting.setPartyMode(Boolean(c.partyMode));
        }
        if (typeof lighting.setEmergencySiren === 'function') {
          const siren = Boolean(c.emergencySirenActive);
          lighting.setEmergencySiren(siren);
          if (siren && !lastSirenState) {
            sound.playEmergencySiren();
          }
          lastSirenState = siren;
        }
      }

      const fin = payload.finale || {};
      if (typeof furnitureManager.setFinaleActive === 'function') {
        furnitureManager.setFinaleActive(Boolean(fin.active));
      }
      if (ui.celebrationBanner) {
        const sirenActive = Boolean(payload.clock && payload.clock.emergencySirenActive);
        if (sirenActive) {
          ui.celebrationBanner.textContent = '🚨 EMERGENCY HOUSE SIREN ACTIVE';
          ui.celebrationBanner.classList.add('siren');
          ui.celebrationBanner.classList.remove('hidden');
        } else if (fin.active) {
          ui.celebrationBanner.textContent = `🏆 GRAND FINALE • ${fin.winnerName ? `${fin.winnerName} WINS ${fin.trophyTitle || 'TROPHY'}` : 'CEREMONY LIVE'}`;
          ui.celebrationBanner.classList.remove('siren');
          ui.celebrationBanner.classList.remove('hidden');
        } else if (payload.activeCelebration) {
          const key = String(payload.activeCelebration);
          ui.celebrationBanner.textContent = `🎉 ${key}`;
          ui.celebrationBanner.classList.remove('siren');
          ui.celebrationBanner.classList.remove('hidden');
          if (key !== lastCelebrationKey) {
            sound.playCelebrationChime();
            lastCelebrationKey = key;
          }
        } else {
          ui.celebrationBanner.classList.add('hidden');
          ui.celebrationBanner.classList.remove('siren');
        }
      }

      const pulseNonce = Number(payload.speakerPulseNonce || 0);
      if (pulseNonce > lastSpeakerPulseNonce) {
        lastSpeakerPulseNonce = pulseNonce;
        if (typeof furnitureManager.triggerSpeakerPulse === 'function') {
          furnitureManager.triggerSpeakerPulse(5.0);
        }
      }

      if (payload.cctvOnlineMap && typeof cctvManager.setCameraOnlineMap === 'function') {
        cctvManager.setCameraOnlineMap(payload.cctvOnlineMap);
        if (cctvActive) syncCctvChrome();
      }

      if (ui.chatUnreadDot) {
        ui.chatUnreadDot.classList.toggle('hidden', Number(payload.unreadNotificationCount || 0) <= 0);
      }
    }

    window.MultiplayerBridge = {
      setAuthenticatedSession: applySessionFromHost,
      onRemotePlayersUpdate: applyRemotePlayersFromHost,
      onWorldStateUpdate: applyWorldStateFromHost,
      onPhase6StateUpdate: applyPhase6FromHost,
      onPhase7And8StateUpdate: applyPhase7And8FromHost,
      onPhase9AiStateUpdate: applyPhase9AiFromHost,
      onPhase12To20StateUpdate: applyPhase12To20FromHost,
      setCctvMode: (enabled, focusCamId = null) => {
        if (enabled) enterCctvMode(focusCamId);
        else closeCctvMode();
      },
      setPaused: (paused) => {
        isHostPaused = Boolean(paused);
      }
    };

    window.addEventListener('resize', () => {
      const w = window.innerWidth;
      const h = Math.max(1, window.innerHeight);
      const aspect = w / h;
      camera.aspect = aspect;
      camera.fov = aspect < 1 ? 66 : 56;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h, false);
      if (cctvActive) syncCctvChrome();
    });

    setProgress(100, 'Authenticating multiplayer house session...');
    await sleep(120);

    // Notify Android host that WebGL scene is ready for verified session injection.
    const hasNativeHost = bridgeCall('onSceneReady');

    if (pendingBridgePayloads.session) {
      applySessionFromHost(pendingBridgePayloads.session);
      pendingBridgePayloads.session = null;
    } else if (!hasNativeHost) {
      // Unauthenticated browser visitors cannot bypass into the house.
      ui.loadingScreen.classList.add('hidden');
      ui.authGateScreen.classList.remove('hidden');
    }

    if (pendingBridgePayloads.remotePlayers) {
      applyRemotePlayersFromHost(pendingBridgePayloads.remotePlayers);
      pendingBridgePayloads.remotePlayers = null;
    }
    if (pendingBridgePayloads.worldState) {
      applyWorldStateFromHost(pendingBridgePayloads.worldState);
      pendingBridgePayloads.worldState = null;
    }
    if (pendingBridgePayloads.phase6) {
      applyPhase6FromHost(pendingBridgePayloads.phase6);
      pendingBridgePayloads.phase6 = null;
    }
    if (pendingBridgePayloads.phase7And8) {
      applyPhase7And8FromHost(pendingBridgePayloads.phase7And8);
      pendingBridgePayloads.phase7And8 = null;
    }
    if (pendingBridgePayloads.phase9Ai) {
      applyPhase9AiFromHost(pendingBridgePayloads.phase9Ai);
      pendingBridgePayloads.phase9Ai = null;
    }
    if (pendingBridgePayloads.phase12To20) {
      applyPhase12To20FromHost(pendingBridgePayloads.phase12To20);
      pendingBridgePayloads.phase12To20 = null;
    }
    if (pendingBridgePayloads.cctvRequest) {
      if (pendingBridgePayloads.cctvRequest.enabled) {
        enterCctvMode(pendingBridgePayloads.cctvRequest.focusCamId);
      }
      pendingBridgePayloads.cctvRequest = null;
    }

    function clearInteractClasses() {
      ui.btnInteract.classList.remove('door-mode', 'pool-mode', 'switch-mode', 'bed-mode', 'kitchen-mode');
    }

    // Single Shared Render Loop + WebGL Lifecycle Disposal
    let lastTime = performance.now();
    let lastRoomId = '';
    let frameCount = 0;
    let rafId = null;
    let sceneDisposed = false;

    window.disposeThreeScene = () => {
      sceneDisposed = true;
      if (rafId !== null) {
        cancelAnimationFrame(rafId);
        rafId = null;
      }
      stopCctvRecording('dispose');
      try {
        scene.traverse((obj) => {
          if (obj.geometry && typeof obj.geometry.dispose === 'function') {
            obj.geometry.dispose();
          }
          if (obj.material) {
            const matsList = Array.isArray(obj.material) ? obj.material : [obj.material];
            matsList.forEach((m) => {
              if (!m) return;
              if (m.map && typeof m.map.dispose === 'function') m.map.dispose();
              if (m.emissiveMap && typeof m.emissiveMap.dispose === 'function') m.emissiveMap.dispose();
              if (typeof m.dispose === 'function') m.dispose();
            });
          }
        });
        renderer.dispose();
      } catch (e) {
        console.warn('disposeThreeScene error:', e);
      }
    };

    function animate(now) {
      if (sceneDisposed) return;
      rafId = requestAnimationFrame(animate);
      if (document.hidden || contextLost || isHostPaused) {
        lastTime = now;
        return;
      }
      const dt = Math.min(0.05, (now - lastTime) / 1000);
      lastTime = now;
      const elapsed = now * 0.001;

      const moveVec = hasEnteredHouse && auth.isAuthenticated ? controls.getMoveVector() : { x: 0, y: 0 };
      player.update(dt, moveVec);

      // If player walked out of a chair automatically inside player.update(), release remote seat lock.
      const currentSeatId = player.seatedTarget ? player.seatedTarget.id : null;
      if (lastLocalSeatId && currentSeatId !== lastLocalSeatId) {
        bridgeCall('onSeatOccupancyChanged', lastLocalSeatId, false);
        lastLocalSeatId = currentSeatId;
      }

      housemates.updateRemotePlayers(dt);
      housemates.updateNPCs(dt, furnitureManager);
      doorManager.update(dt);
      centerTree.update(elapsed);
      furnitureManager.update(elapsed);
      lighting.update(dt);
      if (frameCount % 6 === 0) {
        lighting.updateZoneLightBudget(camera.position.x, camera.position.z, cctvActive && auth.isAdmin());
      }
      biggBossEyes.update(dt, elapsed);

      if (ui.btnDance) {
        const dancing = player.isDancing;
        ui.btnDance.classList.toggle('dancing', dancing);
        ui.btnDance.setAttribute('aria-pressed', String(dancing));
      }

      const pos = player.position;
      tasks3d.update(elapsed, pos.x, pos.z, (taskId, ringNode) => {
        if (currentPhase6Task && currentPhase6Task.activityType === 'CHECKPOINT') {
          sound.playCheckpointClear();
        } else {
          sound.playUiTap();
        }
        bridgeCall('onTaskObjectCollected', taskId, ringNode.id);
        const prog = tasks3d.getProgress();
        showToast(`Cleared ${ringNode.label} (${prog.collected}/${prog.total})!`);
      });
      const isAdmin = auth.isAdmin();
      const room = getRoomAtPosition(pos.x, pos.z, isAdmin);
      if (room.id !== lastRoomId) {
        lastRoomId = room.id;
        ui.roomName.textContent = room.name;
        ui.roomDot.style.background = room.color;
        ui.roomDot.style.boxShadow = `0 0 12px ${room.color}`;
        bridgeCall('onCurrentRoomChanged', room.name);
      }

      if (hasEnteredHouse && auth.isAuthenticated) {
        publishTransform(false);
        if (frameCount % 25 === 0) {
          updateActiveTaskHudPill();
        }

        const running = player.isRunning();
        if (player.isSwimming) {
          ui.sprintIcon.textContent = '🏊';
          ui.sprintLabel.textContent = running ? 'Fast' : 'Swim';
        } else {
          ui.sprintIcon.textContent = '🏃';
          ui.sprintLabel.textContent = running ? 'Run' : 'Walk';
        }

        if (player.isSwimming) {
          ui.btnStand.hidden = false;
          ui.standIcon.textContent = '🏊';
          ui.standLabel.textContent = 'Exit Pool';
          ui.btnInteract.classList.add('hidden');
          ui.btnInteractAlt.classList.add('hidden');
          activePrompt = null;
        } else if (player.isSleeping) {
          ui.btnStand.hidden = false;
          ui.standIcon.textContent = '☀️';
          ui.standLabel.textContent = 'Wake Up';
          ui.btnInteract.classList.add('hidden');
          ui.btnInteractAlt.classList.add('hidden');
          activePrompt = null;
        } else if (player.isSeated) {
          ui.btnStand.hidden = false;
          ui.standIcon.textContent = '🧍';
          ui.standLabel.textContent = 'Stand Up';
          ui.btnInteract.classList.add('hidden');
          const seatId = player.seatedTarget ? player.seatedTarget.id : '';
          const canEatHere = (seatId.startsWith('dining_chair_') || seatId.startsWith('barstool_'))
            && kitchenHudState.readyMealsCount > 0;
          if (canEatHere) {
            ui.btnInteractAlt.classList.remove('hidden');
            ui.interactAltBadge.textContent = 'DINING MEAL';
            ui.interactAltLabel.textContent = 'Eat Meal';
          } else {
            ui.btnInteractAlt.classList.add('hidden');
          }
          activePrompt = null;
        } else {
          ui.btnStand.hidden = true;

          const nearRelic = tasks3d.getNearestInteractable(pos.x, pos.z, 2.5);
          const nearDoor = doorManager.getNearestDoor(pos.x, pos.z, 3.05, auth);
          const nearProp = furnitureManager.getNearestInteractable(pos.x, pos.z, 2.45, auth);
          const poolEdge = furnitureManager.isNearPoolEdge(pos.x, pos.z);

          const doorDist = nearDoor ? Math.hypot(pos.x - nearDoor.x, pos.z - nearDoor.z) : Infinity;
          const propDist = nearProp ? Math.hypot(pos.x - nearProp.x, pos.z - nearProp.z) : Infinity;

          clearInteractClasses();

          if (nearRelic) {
            activePrompt = { kind: 'task_relic', target: nearRelic };
            ui.btnInteract.classList.remove('hidden');
            ui.btnInteract.classList.add('door-mode');
            ui.btnInteractAlt.classList.add('hidden');
            ui.interactBadge.textContent = 'TREASURE HUNT';
            ui.interactLabel.textContent = `Collect ${nearRelic.label.split(' (')[0]}`;
          } else if (nearDoor && doorDist <= propDist + 0.35) {
            activePrompt = { kind: 'door', target: nearDoor };
            ui.btnInteract.classList.remove('hidden');
            ui.btnInteract.classList.add('door-mode');
            ui.btnInteractAlt.classList.add('hidden');
            ui.interactBadge.textContent = 'SLIDING GLASS DOOR';
            ui.interactLabel.textContent = `${nearDoor.isOpen ? 'Close' : 'Open'} ${nearDoor.label.replace(' Door', '')}`;
          } else if (poolEdge.near && propDist > 1.35) {
            activePrompt = { kind: 'pool', poolInfo: poolEdge };
            ui.btnInteract.classList.remove('hidden');
            ui.btnInteract.classList.add('pool-mode');
            ui.btnInteractAlt.classList.add('hidden');
            ui.interactBadge.textContent = 'SWIMMING POOL';
            ui.interactLabel.textContent = 'Enter Pool';
          } else if (nearProp) {
            activePrompt = { kind: 'furniture', target: nearProp };
            ui.btnInteract.classList.remove('hidden');

            if (nearProp.type === 'bed') {
              ui.btnInteract.classList.add('bed-mode');
              ui.interactBadge.textContent = nearProp.label.toUpperCase();
              ui.interactLabel.textContent = 'Sleep';

              ui.btnInteractAlt.classList.remove('hidden');
              ui.interactAltBadge.textContent = 'BED';
              ui.interactAltLabel.textContent = 'Sit';
            } else {
              ui.btnInteractAlt.classList.add('hidden');
              if (nearProp.type === 'kitchen') {
                ui.btnInteract.classList.add('kitchen-mode');
                if (nearProp.kitchenAction === 'pantry') {
                  ui.interactBadge.textContent = 'PANTRY';
                  ui.interactLabel.textContent = 'Open Pantry';
                } else if (nearProp.kitchenAction === 'cook' || nearProp.kitchenAction === 'prep') {
                  ui.interactBadge.textContent = 'COOKING STATION';
                  ui.interactLabel.textContent = 'Cook Meal';
                } else if (nearProp.kitchenAction === 'wash') {
                  ui.interactBadge.textContent = 'KITCHEN SINK';
                  ui.interactLabel.textContent = 'Wash & Clean';
                } else {
                  ui.interactBadge.textContent = 'DINING TABLE';
                  ui.interactLabel.textContent = kitchenHudState.readyMealsCount > 0 ? 'Claim & Eat' : 'View Meals';
                }
              } else if (nearProp.type === 'light_switch') {
                const isOn = lighting.isRoomLightOn(nearProp.roomId);
                ui.btnInteract.classList.add('switch-mode');
                ui.interactBadge.textContent = 'LIGHT SWITCH';
                ui.interactLabel.textContent = isOn ? 'Turn Light Off' : 'Turn Light On';
              } else if (nearProp.type === 'fan_switch') {
                const isOn = lighting.isFanOn(nearProp.roomId);
                ui.btnInteract.classList.add('switch-mode');
                ui.interactBadge.textContent = 'CEILING FAN';
                ui.interactLabel.textContent = isOn ? 'Turn Fan Off' : 'Turn Fan On';
              } else if (nearProp.type === 'sit') {
                ui.interactBadge.textContent = nearProp.label.toUpperCase();
                ui.interactLabel.textContent = 'Sit';
              } else if (nearProp.type === 'ai_operator_console') {
                ui.btnInteract.classList.add('switch-mode');
                ui.interactBadge.textContent = 'CHAIR 1 • AI OPERATOR';
                ui.interactLabel.textContent = 'Open ChatGPT AI';
              } else if (nearProp.type === 'admin_dashboard') {
                ui.btnInteract.classList.add('door-mode');
                ui.interactBadge.textContent = 'CHAIR 2 • OWNER';
                ui.interactLabel.textContent = 'Open Control Suite';
              } else if (nearProp.type === 'cctv_wall') {
                ui.btnInteract.classList.add('door-mode');
                ui.interactBadge.textContent = 'SURVEILLANCE TV';
                ui.interactLabel.textContent = 'Open 24-Cam CCTV';
              } else if (nearProp.type === 'admin_phone') {
                ui.btnInteract.classList.add('switch-mode');
                ui.interactBadge.textContent = 'OWNER DEVICE';
                ui.interactLabel.textContent = 'Open Admin Phone';
              } else {
                ui.interactBadge.textContent = 'CHALLENGE ZONE';
                ui.interactLabel.textContent = nearProp.label;
              }
            }
          } else {
            activePrompt = null;
            ui.btnInteract.classList.add('hidden');
            ui.btnInteractAlt.classList.add('hidden');
          }
        }
      }

      if (mapOpen && frameCount % 4 === 0) {
        renderMinimap(
          ui.minimapCanvas,
          pos,
          player.yaw,
          doorManager.doors,
          housemates.getNpcPositions(),
          isAdmin,
          housemates.getRemotePositions()
        );
      }
      frameCount++;

      if (cctvActive && isAdmin) {
        if (frameCount % 10 === 0) {
          updateCctvOccupantBadges();
        }
        const cctvCadence = (urlEcoMode || isSoftwareGpu || !highQuality) ? 4 : 2;
        if (cctvFocusCamId || frameCount % cctvCadence === 0) {
          cctvManager.renderCctvView(
            renderer,
            window.innerWidth,
            Math.max(1, window.innerHeight),
            cctvFocusCamId,
            elapsed,
            cctvRoomFilter,
            cctvPageIndex
          );
        }
      } else {
        renderer.setScissorTest(false);
        renderer.setViewport(0, 0, window.innerWidth, Math.max(1, window.innerHeight));
        renderer.render(scene, camera);
      }
    }

    rafId = requestAnimationFrame(animate);
  } catch (err) {
    console.error('BIGG BOSS – THE HOUSE startup error:', err);
    showError(err && err.message ? err.message : 'Failed to initialize Three.js 3D scene.');
  }
}

bootstrap();
