import * as THREE from '../vendor/three/three.module.min.js';
import { getRoomAtPosition } from './worldLayout.js';

export const FORBIDDEN_CAMERA_ZONES = ['bathroom', 'washroom', 'changing', 'private', 'smoking'];

// 24 Common-Area CCTV Cameras strictly excluding bathrooms, washrooms, changing areas, and smoking lounge.
export const CCTV_CAMERA_SPECS = [
  // Living Atrium (4)
  { id: 'cam_living_1', code: 'CAM 01', filterGroup: 'living', label: 'Living Atrium NW', roomIds: ['living'], pos: [-8.8, 4.45, -6.8], lookAt: [0.0, 1.35, 0.4] },
  { id: 'cam_living_2', code: 'CAM 02', filterGroup: 'living', label: 'Living Atrium NE', roomIds: ['living'], pos: [8.8, 4.45, -6.8], lookAt: [0.0, 1.35, 0.4] },
  { id: 'cam_living_3', code: 'CAM 03', filterGroup: 'living', label: 'Living Lounge SW', roomIds: ['living'], pos: [-8.8, 4.45, 6.8], lookAt: [0.0, 1.2, -1.5] },
  { id: 'cam_living_4', code: 'CAM 04', filterGroup: 'living', label: 'Living Center Stage', roomIds: ['living'], pos: [8.6, 4.45, 6.8], lookAt: [0.0, 1.25, -3.0] },

  // Gourmet Kitchen (4)
  { id: 'cam_kitchen_1', code: 'CAM 05', filterGroup: 'kitchen', label: 'Kitchen Stove Line', roomIds: ['kitchen'], pos: [-10.8, 4.15, -9.0], lookAt: [-16.2, 1.0, -16.8] },
  { id: 'cam_kitchen_2', code: 'CAM 06', filterGroup: 'kitchen', label: 'Kitchen Pantry Wall', roomIds: ['kitchen'], pos: [-19.2, 4.15, -9.2], lookAt: [-13.5, 1.1, -16.5] },
  { id: 'cam_kitchen_3', code: 'CAM 07', filterGroup: 'kitchen', label: 'Kitchen Prep Island', roomIds: ['kitchen'], pos: [-11.0, 4.15, -21.0], lookAt: [-15.8, 0.95, -14.2] },
  { id: 'cam_kitchen_4', code: 'CAM 08', filterGroup: 'kitchen', label: 'Kitchen Sink & Pass', roomIds: ['kitchen'], pos: [-19.0, 4.15, -21.0], lookAt: [-14.0, 1.0, -13.0] },

  // Dining Area (3)
  { id: 'cam_dining_1', code: 'CAM 09', filterGroup: 'dining', label: 'Dining Table North', roomIds: ['dining'], pos: [-9.2, 4.15, -21.2], lookAt: [-4.0, 1.0, -14.5] },
  { id: 'cam_dining_2', code: 'CAM 10', filterGroup: 'dining', label: 'Dining Table South', roomIds: ['dining'], pos: [1.2, 4.15, -8.8], lookAt: [-5.0, 1.0, -15.5] },
  { id: 'cam_dining_3', code: 'CAM 11', filterGroup: 'dining', label: 'Dining Buffet Side', roomIds: ['dining'], pos: [-9.0, 4.15, -9.2], lookAt: [-3.5, 1.05, -16.2] },

  // Common Bedroom (3)
  { id: 'cam_bedroom_1', code: 'CAM 12', filterGroup: 'bedroom', label: 'Bedroom Dorm West', roomIds: ['bedroom'], pos: [-11.0, 4.2, -6.8], lookAt: [-20.0, 0.9, 1.2] },
  { id: 'cam_bedroom_2', code: 'CAM 13', filterGroup: 'bedroom', label: 'Bedroom Dorm Center', roomIds: ['bedroom'], pos: [-26.0, 4.2, -6.8], lookAt: [-17.5, 0.95, 1.5] },
  { id: 'cam_bedroom_3', code: 'CAM 14', filterGroup: 'bedroom', label: 'Bedroom Dorm East', roomIds: ['bedroom'], pos: [-11.2, 4.2, 8.8], lookAt: [-19.5, 0.95, 0.2] },

  // Garden & Poolside (4 — excludes Spa Bathroom & Smoking Lounge)
  { id: 'cam_garden_1', code: 'CAM 15', filterGroup: 'garden', label: 'Botanical Garden NW', roomIds: ['garden'], pos: [11.2, 4.2, 11.0], lookAt: [19.4, 1.0, 16.4] },
  { id: 'cam_garden_2', code: 'CAM 16', filterGroup: 'garden', label: 'Botanical Garden SE', roomIds: ['garden'], pos: [25.8, 4.2, 20.8], lookAt: [16.5, 1.0, 15.0] },
  { id: 'cam_garden_3', code: 'CAM 17', filterGroup: 'garden', label: 'Indoor Pool Deck N', roomIds: ['pool'], pos: [-8.6, 4.35, 9.0], lookAt: [0.0, 0.5, 15.2] },
  { id: 'cam_garden_4', code: 'CAM 18', filterGroup: 'garden', label: 'Indoor Pool Deck S', roomIds: ['pool'], pos: [8.6, 4.35, 21.0], lookAt: [0.0, 0.6, 14.5] },

  // Activity & Challenge Room (3)
  { id: 'cam_activity_1', code: 'CAM 19', filterGroup: 'activity', label: 'Activity Arena NW', roomIds: ['activity'], pos: [11.0, 4.2, 8.8], lookAt: [18.5, 1.15, 5.0] },
  { id: 'cam_activity_2', code: 'CAM 20', filterGroup: 'activity', label: 'Activity Arena NE', roomIds: ['activity'], pos: [25.8, 4.2, 1.2], lookAt: [17.0, 1.1, 5.2] },
  { id: 'cam_activity_3', code: 'CAM 21', filterGroup: 'activity', label: 'Activity Challenge Ring', roomIds: ['activity'], pos: [11.2, 4.2, 1.2], lookAt: [19.5, 1.1, 6.2] },

  // Main Hall & VIP Corridor (3)
  { id: 'cam_hall_1', code: 'CAM 22', filterGroup: 'hall', label: 'VIP Wing Gallery W', roomIds: ['gallery', 'east_alcove'], pos: [2.8, 4.25, -13.0], lookAt: [11.5, 1.2, -10.2] },
  { id: 'cam_hall_2', code: 'CAM 23', filterGroup: 'hall', label: 'VIP Wing Gallery E', roomIds: ['gallery', 'east_alcove'], pos: [18.2, 4.25, -8.8], lookAt: [8.5, 1.2, -11.2] },
  { id: 'cam_hall_3', code: 'CAM 24', filterGroup: 'hall', label: 'East Wing Foyer', roomIds: ['gallery', 'east_alcove'], pos: [10.6, 4.25, -7.2], lookAt: [13.2, 1.2, -2.5] }
];

export function createCctvSystem(scene) {
  const rigGroup = new THREE.Group();
  const housingMat = new THREE.MeshStandardMaterial({
    color: 0x1e293b,
    roughness: 0.35,
    metalness: 0.78
  });
  const lensMat = new THREE.MeshStandardMaterial({
    color: 0x0f172a,
    roughness: 0.12,
    metalness: 0.92
  });

  const housingGeo = new THREE.CylinderGeometry(0.11, 0.14, 0.24, 10);
  const lensGeo = new THREE.SphereGeometry(0.085, 8, 8);
  const ledGeo = new THREE.SphereGeometry(0.028, 6, 6);

  const feeds = [];

  for (const spec of CCTV_CAMERA_SPECS) {
    // Hard privacy assertion: never mount CCTV inside bathrooms, washrooms, changing areas, or smoking lounge.
    if (spec.roomIds.some((r) => FORBIDDEN_CAMERA_ZONES.includes(r))) {
      throw new Error(`Privacy violation: CCTV camera ${spec.id} cannot target restricted zones.`);
    }

    const cam = new THREE.PerspectiveCamera(62, 16 / 10, 0.2, 62);
    cam.position.set(spec.pos[0], spec.pos[1], spec.pos[2]);
    cam.lookAt(spec.lookAt[0], spec.lookAt[1], spec.lookAt[2]);

    const baseEuler = new THREE.Euler().setFromQuaternion(cam.quaternion, 'YXZ');

    const ledMat = new THREE.MeshBasicMaterial({ color: 0x34d399 });
    const prop = new THREE.Group();
    prop.position.set(spec.pos[0], spec.pos[1] + 0.18, spec.pos[2]);
    prop.lookAt(spec.lookAt[0], spec.lookAt[1], spec.lookAt[2]);

    const body = new THREE.Mesh(housingGeo, housingMat);
    body.rotation.x = Math.PI / 2;
    prop.add(body);

    const dome = new THREE.Mesh(lensGeo, lensMat);
    dome.position.z = 0.12;
    prop.add(dome);

    const led = new THREE.Mesh(ledGeo, ledMat);
    led.position.set(0.06, 0.06, 0.13);
    prop.add(led);

    rigGroup.add(prop);

    feeds.push({
      id: spec.id,
      code: spec.code,
      filterGroup: spec.filterGroup,
      label: spec.label,
      roomIds: spec.roomIds,
      camera: cam,
      baseYaw: baseEuler.y,
      basePitch: baseEuler.x,
      ledMat,
      online: true
    });
  }

  scene.add(rigGroup);

  function getFilteredFeeds(roomFilter = 'all') {
    const f = String(roomFilter || 'all').toLowerCase();
    if (f === 'all' || !f) return feeds;
    const matched = feeds.filter((feed) => feed.filterGroup === f || feed.roomIds.includes(f));
    return matched.length > 0 ? matched : feeds;
  }

  function getPageFeeds(roomFilter = 'all', pageIndex = 0) {
    const list = getFilteredFeeds(roomFilter);
    const pageSize = 4; // Max 4 WebGL viewports per frame to keep WebView GPU safe
    const pageCount = Math.max(1, Math.ceil(list.length / pageSize));
    const safePage = ((pageIndex % pageCount) + pageCount) % pageCount;
    const slice = list.slice(safePage * pageSize, safePage * pageSize + pageSize);
    return { feeds: slice, pageIndex: safePage, pageCount, totalFiltered: list.length };
  }

  function computeGridLayout(canvasW, canvasH, roomFilter = 'all', pageIndex = 0, topPadPx = 88, bottomPadPx = 18, sidePadPx = 14) {
    const pageInfo = getPageFeeds(roomFilter, pageIndex);
    const cols = 2;
    const rows = 2;
    const gap = 10;
    const availW = Math.max(120, canvasW - sidePadPx * 2);
    const availH = Math.max(120, canvasH - topPadPx - bottomPadPx);
    const tileW = Math.floor((availW - gap * (cols - 1)) / cols);
    const tileH = Math.floor((availH - gap * (rows - 1)) / rows);

    return {
      ...pageInfo,
      tiles: pageInfo.feeds.map((feed, idx) => {
        const col = idx % cols;
        const row = Math.floor(idx / cols);
        const cssLeft = sidePadPx + col * (tileW + gap);
        const cssTop = topPadPx + row * (tileH + gap);
        const glBottom = Math.max(0, canvasH - (cssTop + tileH));
        return {
          feed,
          cssLeft,
          cssTop,
          width: tileW,
          height: tileH,
          glBottom
        };
      })
    };
  }

  return {
    feeds,
    getFeedById(id) {
      return feeds.find((f) => f.id === id) || feeds[0];
    },
    getFilteredFeeds,
    getPageFeeds,
    setCameraOnlineMap(onlineMap) {
      if (!onlineMap || typeof onlineMap !== 'object') return;
      for (const feed of feeds) {
        const isOn = onlineMap[feed.id] !== false;
        feed.online = isOn;
        feed.ledMat.color.setHex(isOn ? 0x34d399 : 0xef4444);
      }
    },
    computeGridLayout,
    renderCctvView(renderer, canvasW, canvasH, focusedCamId, elapsed, roomFilter = 'all', pageIndex = 0) {
      const safeW = Math.max(1, canvasW);
      const safeH = Math.max(1, canvasH);

      if (focusedCamId) {
        const feed = this.getFeedById(focusedCamId);
        if (!feed.online) {
          renderer.setScissorTest(true);
          renderer.setViewport(0, 0, safeW, safeH);
          renderer.setScissor(0, 0, safeW, safeH);
          renderer.clear();
          renderer.setScissorTest(false);
          return null;
        }
        const aspect = safeW / safeH;
        if (Math.abs(feed.camera.aspect - aspect) > 0.01) {
          feed.camera.aspect = aspect;
          feed.camera.updateProjectionMatrix();
        }
        const pan = Math.sin(elapsed * 0.35) * 0.15;
        feed.camera.rotation.order = 'YXZ';
        feed.camera.rotation.y = feed.baseYaw + pan;
        feed.camera.rotation.x = feed.basePitch;
        feed.camera.rotation.z = 0;

        renderer.setScissorTest(false);
        renderer.setViewport(0, 0, safeW, safeH);
        renderer.render(scene, feed.camera);
        return null;
      }

      // Max 4-Up Live Quad Split Synchronized CCTV Grid via WebGL Viewport + Scissor
      renderer.setScissorTest(true);
      renderer.setViewport(0, 0, safeW, safeH);
      renderer.setScissor(0, 0, safeW, safeH);
      renderer.clear();

      const layout = computeGridLayout(safeW, safeH, roomFilter, pageIndex);
      layout.tiles.forEach((tile, idx) => {
        const { feed, cssLeft, glBottom, width, height } = tile;
        if (!feed.online || width <= 4 || height <= 4) return;
        const aspect = width / height;
        if (Math.abs(feed.camera.aspect - aspect) > 0.01) {
          feed.camera.aspect = aspect;
          feed.camera.updateProjectionMatrix();
        }
        const pan = Math.sin(elapsed * 0.3 + idx * 1.1) * 0.12;
        feed.camera.rotation.order = 'YXZ';
        feed.camera.rotation.y = feed.baseYaw + pan;
        feed.camera.rotation.x = feed.basePitch;
        feed.camera.rotation.z = 0;

        renderer.setViewport(cssLeft, glBottom, width, height);
        renderer.setScissor(cssLeft, glBottom, width, height);
        renderer.render(scene, feed.camera);
      });
      renderer.setScissorTest(false);
      return layout.tiles;
    },
    getPlayersInFeed(feed, localInfo, remotePositions = [], npcPositions = []) {
      const occupants = [];
      if (localInfo && localInfo.active) {
        const r = getRoomAtPosition(localInfo.x, localInfo.z, true);
        if (feed.roomIds.includes(r.id)) {
          occupants.push({
            label: `${localInfo.displayName} (You)`,
            online: true,
            isNpc: false
          });
        }
      }
      for (const rp of remotePositions) {
        const r = getRoomAtPosition(rp.x, rp.z, true);
        if (feed.roomIds.includes(r.id)) {
          occupants.push({
            label: rp.displayName || rp.playerId || 'Housemate',
            online: true,
            isNpc: false
          });
        }
      }
      for (const npc of npcPositions) {
        if (npc.isUser || npc.visible === false) continue;
        const r = getRoomAtPosition(npc.x, npc.z, true);
        if (feed.roomIds.includes(r.id)) {
          occupants.push({
            label: `${npc.name} (NPC)`,
            online: false,
            isNpc: true
          });
        }
      }
      return occupants;
    }
  };
}
