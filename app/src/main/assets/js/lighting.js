import * as THREE from '../vendor/three/three.module.min.js';

export const TIME_MODES = [
  { id: 'day', label: 'Day', icon: '☀️' },
  { id: 'dusk', label: 'Dusk', icon: '🌇' },
  { id: 'night', label: 'Night', icon: '🌙' }
];

export const ROOM_LABELS = {
  living: 'Living Atrium',
  dining: 'Dining & Kitchen',
  bedroom: '15-Player Bedroom',
  bathroom: 'Spa Bathroom',
  pool: 'Swimming Pool',
  garden: 'Botanical Garden',
  activity: 'Activity Room',
  confession: 'Confession Room',
  captain: 'Captain Room',
  smoking: 'Smoking Lounge',
  admin: 'Admin Control Room'
};

export function buildLightingSystem(scene, skylightMats = []) {
  // 1. Hemisphere Ambient Fill (Indoor ceiling bounce + warm floor bounce)
  const hemi = new THREE.HemisphereLight(0xf6f9ff, 0x5c4f42, 1.1);
  scene.add(hemi);

  // 2. Primary Skylight Directional Sunbeam (Single Shadow Caster for Mobile 60fps)
  const sunLight = new THREE.DirectionalLight(0xfff5e0, 1.55);
  sunLight.position.set(12, 24, 10);
  sunLight.castShadow = true;
  sunLight.shadow.mapSize.set(512, 512);
  sunLight.shadow.camera.near = 2;
  sunLight.shadow.camera.far = 55;
  sunLight.shadow.camera.left = -28;
  sunLight.shadow.camera.right = 28;
  sunLight.shadow.camera.top = 24;
  sunLight.shadow.camera.bottom = -24;
  sunLight.shadow.bias = -0.0012;
  scene.add(sunLight);

  // 3. Key Zone Indoor Accent Lights (Non-shadow-casting for Android GPU speed)
  const zoneLights = {
    living: new THREE.PointLight(0xffe1a8, 1.1, 22, 1.4),
    dining: new THREE.PointLight(0xffd28a, 1.0, 18, 1.5),
    bedroom: new THREE.PointLight(0xdbeafe, 0.95, 22, 1.4),
    bathroom: new THREE.PointLight(0xe0f2fe, 0.9, 16, 1.5),
    pool: new THREE.PointLight(0x22d3ee, 1.35, 18, 1.4),
    garden: new THREE.PointLight(0x6ee7b7, 1.0, 16, 1.5),
    activity: new THREE.PointLight(0xfda4af, 1.05, 18, 1.5),
    confession: new THREE.PointLight(0xe879f9, 1.3, 14, 1.5),
    admin: new THREE.PointLight(0x38bdf8, 1.1, 14, 1.5),
    captain: new THREE.PointLight(0xfde047, 1.0, 14, 1.5),
    smoking: new THREE.PointLight(0xfb923c, 0.85, 13, 1.5)
  };

  zoneLights.living.position.set(0, 4.9, 0);
  zoneLights.dining.position.set(-8, 4.4, -15);
  zoneLights.bedroom.position.set(-18.5, 4.4, 0);
  zoneLights.bathroom.position.set(-18.5, 4.2, 16);
  zoneLights.pool.position.set(0, 2.2, 15);
  zoneLights.garden.position.set(18.5, 4.2, 16);
  zoneLights.activity.position.set(17.5, 4.3, 5);
  zoneLights.confession.position.set(23.5, 3.8, -16);
  zoneLights.admin.position.set(21, 3.8, -4);
  zoneLights.captain.position.set(14.5, 4.2, -18);
  zoneLights.smoking.position.set(6.0, 4.1, -18);

  let adminAccessAllowed = false;
  zoneLights.admin.visible = false;
  const zoneEntries = Object.entries(zoneLights);
  zoneEntries.forEach(([, l]) => scene.add(l));

  // Per-room interactive light & fan states
  const roomLightsOn = {
    living: true,
    dining: true,
    bedroom: true,
    bathroom: true,
    pool: true,
    garden: true,
    activity: true,
    confession: true,
    captain: true,
    smoking: true,
    admin: true
  };

  const roomFansOn = {
    bedroom: true,
    dining: true,
    captain: true,
    activity: true,
    smoking: true
  };

  // Registered emissive fixture groups & fan rotors
  const fixtureRegistry = []; // { roomId, mat, onIntensity, offIntensity }
  const lightLedRegistry = []; // { roomId, mat }
  const fanRegistry = []; // { roomId, rotor, axis, currentSpeed, targetMaxSpeed }
  const fanLedRegistry = []; // { roomId, mat }

  let modeIndex = 0;

  const presets = {
    day: {
      bg: new THREE.Color(0x131a29),
      hemiSky: new THREE.Color(0xf8fbff),
      hemiGround: new THREE.Color(0x6e5f50),
      hemiInt: 1.25,
      sunColor: new THREE.Color(0xfff7e6),
      sunInt: 1.65,
      sunPos: new THREE.Vector3(12, 24, 10),
      indoorInt: 0.78,
      poolInt: 0.95,
      skylightColor: new THREE.Color(0x9bd7ff),
      skylightInt: 0.5
    },
    dusk: {
      bg: new THREE.Color(0x141122),
      hemiSky: new THREE.Color(0xffbfa0),
      hemiGround: new THREE.Color(0x4a3848),
      hemiInt: 0.95,
      sunColor: new THREE.Color(0xff9b54),
      sunInt: 1.15,
      sunPos: new THREE.Vector3(-16, 15, 14),
      indoorInt: 1.48,
      poolInt: 1.85,
      skylightColor: new THREE.Color(0xf49d6e),
      skylightInt: 0.42
    },
    night: {
      bg: new THREE.Color(0x090d18),
      hemiSky: new THREE.Color(0x476696),
      hemiGround: new THREE.Color(0x282032),
      hemiInt: 0.72,
      sunColor: new THREE.Color(0x7aa2f7),
      sunInt: 0.52,
      sunPos: new THREE.Vector3(-8, 22, -12),
      indoorInt: 2.1,
      poolInt: 2.45,
      skylightColor: new THREE.Color(0x1d2b4f),
      skylightInt: 0.22
    }
  };

  scene.background = presets.day.bg.clone();

  function syncSwitchLed(mat, isOn, onColorHex = 0x34d399) {
    mat.color.setHex(isOn ? onColorHex : 0x475569);
    mat.emissive.setHex(isOn ? onColorHex : 0x0f172a);
    mat.emissiveIntensity = isOn ? 1.4 : 0.15;
  }

  // Lightweight pooled rain streaks for RAIN weather mode (low GPU cost)
  const rainCount = 160;
  const rainPositions = new Float32Array(rainCount * 6);
  for (let i = 0; i < rainCount; i++) {
    const rx = (Math.random() - 0.5) * 48;
    const ry = 0.5 + Math.random() * 5.2;
    const rz = (Math.random() - 0.5) * 40;
    const idx = i * 6;
    rainPositions[idx] = rx;
    rainPositions[idx + 1] = ry;
    rainPositions[idx + 2] = rz;
    rainPositions[idx + 3] = rx - 0.04;
    rainPositions[idx + 4] = ry - 0.42;
    rainPositions[idx + 5] = rz - 0.02;
  }
  const rainGeo = new THREE.BufferGeometry();
  rainGeo.setAttribute('position', new THREE.BufferAttribute(rainPositions, 3));
  const rainMat = new THREE.LineBasicMaterial({ color: 0x93c5fd, transparent: true, opacity: 0.45 });
  const rainMesh = new THREE.LineSegments(rainGeo, rainMat);
  rainMesh.visible = false;
  scene.add(rainMesh);

  let weatherMode = 'CLEAR';
  let partyMode = false;
  let emergencySirenActive = false;
  let fxClock = 0;

  return {
    getCurrentMode() {
      return TIME_MODES[modeIndex];
    },
    cycleMode() {
      modeIndex = (modeIndex + 1) % TIME_MODES.length;
      return TIME_MODES[modeIndex];
    },
    setModeById(modeId) {
      const idx = TIME_MODES.findIndex((m) => m.id === modeId);
      if (idx >= 0) {
        modeIndex = idx;
      }
      return TIME_MODES[modeIndex];
    },
    setWeather(nextWeather) {
      const w = String(nextWeather || 'CLEAR').toUpperCase();
      weatherMode = ['CLEAR', 'RAIN', 'GOLDEN', 'NIGHT_MIST'].includes(w) ? w : 'CLEAR';
      rainMesh.visible = weatherMode === 'RAIN';
    },
    setPartyMode(enabled) {
      partyMode = Boolean(enabled);
    },
    setEmergencySiren(active) {
      emergencySirenActive = Boolean(active);
    },
    setShadowQuality(high) {
      sunLight.castShadow = high;
    },
    setAdminAccessVisible(isAdmin) {
      adminAccessAllowed = Boolean(isAdmin);
      zoneLights.admin.visible = adminAccessAllowed;
    },
    updateZoneLightBudget(camX, camZ, cctvActive = false) {
      if (cctvActive) {
        const cctvKeep = new Set(['living', 'pool', 'dining', adminAccessAllowed ? 'admin' : 'bedroom']);
        for (const [id, light] of zoneEntries) {
          light.visible = cctvKeep.has(id);
        }
        return;
      }
      const candidates = [];
      for (const [id, light] of zoneEntries) {
        if (id === 'admin' && !adminAccessAllowed) {
          light.visible = false;
          continue;
        }
        const dx = light.position.x - camX;
        const dz = light.position.z - camZ;
        candidates.push({ light, distSq: dx * dx + dz * dz });
      }
      candidates.sort((a, b) => a.distSq - b.distSq);
      for (let i = 0; i < candidates.length; i++) {
        candidates[i].light.visible = i < 4;
      }
    },
    registerLightFixture(roomId, mat, onIntensity = 1.3, offIntensity = 0.04) {
      fixtureRegistry.push({ roomId, mat, onIntensity, offIntensity });
    },
    registerLightSwitchLed(roomId, mat) {
      lightLedRegistry.push({ roomId, mat });
      syncSwitchLed(mat, Boolean(roomLightsOn[roomId]), 0xfacc15);
    },
    registerFan(roomId, rotor, targetMaxSpeed = 8.5) {
      fanRegistry.push({ roomId, rotor, currentSpeed: roomFansOn[roomId] ? targetMaxSpeed : 0, targetMaxSpeed });
    },
    registerFanSwitchLed(roomId, mat) {
      fanLedRegistry.push({ roomId, mat });
      syncSwitchLed(mat, Boolean(roomFansOn[roomId]), 0x38bdf8);
    },
    isRoomLightOn(roomId) {
      return roomLightsOn[roomId] !== false;
    },
    toggleRoomLight(roomId) {
      if (!(roomId in roomLightsOn)) return false;
      roomLightsOn[roomId] = !roomLightsOn[roomId];
      const on = roomLightsOn[roomId];
      for (const entry of lightLedRegistry) {
        if (entry.roomId === roomId) {
          syncSwitchLed(entry.mat, on, 0xfacc15);
        }
      }
      return on;
    },
    isFanOn(roomId) {
      return roomFansOn[roomId] !== false;
    },
    toggleFan(roomId) {
      if (!(roomId in roomFansOn)) return false;
      roomFansOn[roomId] = !roomFansOn[roomId];
      const on = roomFansOn[roomId];
      for (const entry of fanLedRegistry) {
        if (entry.roomId === roomId) {
          syncSwitchLed(entry.mat, on, 0x38bdf8);
        }
      }
      return on;
    },
    getRoomLabel(roomId) {
      return ROOM_LABELS[roomId] || roomId;
    },
    update(dt) {
      fxClock += dt;
      const target = presets[TIME_MODES[modeIndex].id];
      const k = Math.min(1, dt * 3.8);

      scene.background.lerp(target.bg, k);
      hemi.color.lerp(target.hemiSky, k);
      hemi.groundColor.lerp(target.hemiGround, k);
      hemi.intensity += (target.hemiInt - hemi.intensity) * k;

      sunLight.color.lerp(target.sunColor, k);
      if (weatherMode === 'GOLDEN') {
        sunLight.color.lerp(new THREE.Color(0xffb347), k * 0.8);
      }
      sunLight.intensity += (target.sunInt - sunLight.intensity) * k;
      sunLight.position.lerp(target.sunPos, k);

      if (scene.fog) {
        scene.fog.density = weatherMode === 'NIGHT_MIST' ? 0.018 : (weatherMode === 'RAIN' ? 0.012 : 0.008);
      }

      if (rainMesh.visible) {
        const attr = rainGeo.attributes.position;
        const arr = attr.array;
        for (let i = 0; i < rainCount; i++) {
          const idx = i * 6;
          arr[idx + 1] -= 9.5 * dt;
          arr[idx + 4] -= 9.5 * dt;
          if (arr[idx + 1] < 0.2) {
            const ry = 5.4 + Math.random() * 0.8;
            arr[idx + 1] = ry;
            arr[idx + 4] = ry - 0.42;
          }
        }
        attr.needsUpdate = true;
      }

      const mult = (id) => (roomLightsOn[id] ? 1 : 0);

      zoneLights.living.intensity += (target.indoorInt * 0.92 * mult('living') - zoneLights.living.intensity) * k;
      zoneLights.dining.intensity += (target.indoorInt * 0.88 * mult('dining') - zoneLights.dining.intensity) * k;
      zoneLights.bedroom.intensity += (target.indoorInt * 0.85 * mult('bedroom') - zoneLights.bedroom.intensity) * k;
      zoneLights.bathroom.intensity += (target.indoorInt * 0.85 * mult('bathroom') - zoneLights.bathroom.intensity) * k;
      zoneLights.captain.intensity += (target.indoorInt * 0.95 * mult('captain') - zoneLights.captain.intensity) * k;
      zoneLights.garden.intensity += (target.indoorInt * 0.85 * mult('garden') - zoneLights.garden.intensity) * k;
      zoneLights.pool.intensity += (target.poolInt * mult('pool') - zoneLights.pool.intensity) * k;
      zoneLights.activity.intensity += (target.indoorInt * 0.95 * mult('activity') - zoneLights.activity.intensity) * k;
      zoneLights.confession.intensity += (target.indoorInt * 1.15 * mult('confession') - zoneLights.confession.intensity) * k;
      zoneLights.smoking.intensity += (target.indoorInt * 0.85 * mult('smoking') - zoneLights.smoking.intensity) * k;
      zoneLights.admin.intensity += (target.indoorInt * 0.9 * mult('admin') - zoneLights.admin.intensity) * k;

      if (emergencySirenActive) {
        const pulse = 0.5 + 0.5 * Math.sin(fxClock * 10.5);
        zoneLights.living.color.setRGB(1.0, 0.18 + 0.35 * (1 - pulse), 0.18 + 0.35 * (1 - pulse));
        zoneLights.living.intensity = 1.6 + pulse * 1.2;
      } else if (partyMode) {
        zoneLights.living.color.setHSL((fxClock * 0.25) % 1, 0.85, 0.62);
        zoneLights.pool.color.setHSL(((fxClock * 0.25) + 0.35) % 1, 0.9, 0.58);
      } else {
        zoneLights.living.color.setHex(0xffe1a8);
        zoneLights.pool.color.setHex(0x22d3ee);
      }

      for (const fix of fixtureRegistry) {
        const goal = roomLightsOn[fix.roomId] ? fix.onIntensity : fix.offIntensity;
        fix.mat.emissiveIntensity += (goal - fix.mat.emissiveIntensity) * k;
      }

      for (const fan of fanRegistry) {
        const goalSpeed = roomFansOn[fan.roomId] ? fan.targetMaxSpeed : 0;
        fan.currentSpeed += (goalSpeed - fan.currentSpeed) * Math.min(1, dt * 2.6);
        if (fan.currentSpeed > 0.01) {
          fan.rotor.rotation.y += fan.currentSpeed * dt;
        }
      }

      for (const sm of skylightMats) {
        sm.emissive.lerp(target.skylightColor, k);
        sm.emissiveIntensity += (target.skylightInt - sm.emissiveIntensity) * k;
      }
    }
  };
}
