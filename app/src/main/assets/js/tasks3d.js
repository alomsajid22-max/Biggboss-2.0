import * as THREE from '../vendor/three/three.module.min.js';

const TREASURE_NODES = [
  { id: 'relic_atrium', altId: 'relic_living', label: 'Golden Eye Clue #1 (Atrium)', x: 5.6, z: -5.2 },
  { id: 'relic_kitchen', label: 'Golden Eye Clue #2 (Kitchen)', x: -17.2, z: -14.8 },
  { id: 'relic_garden', label: 'Golden Eye Clue #3 (Garden)', x: 15.5, z: 14.5 }
];

const OBSTACLE_RINGS = [
  { id: 'ring_activity', label: 'Checkpoint Ring #1 (Activity)', x: 17.5, z: 5.0 },
  { id: 'ring_garden', label: 'Checkpoint Ring #2 (Garden)', x: 14.8, z: 13.4 },
  { id: 'ring_pool', label: 'Checkpoint Ring #3 (Pool Deck)', x: 5.2, z: 9.5 }
];

const CHECKPOINT_ZONES = [
  { id: 'zone_bedroom', label: 'Zone #1: 15-Player Bedroom', x: -18.5, z: 0.0 },
  { id: 'zone_kitchen', label: 'Zone #2: Gourmet Kitchen', x: -14.2, z: -18.2 },
  { id: 'zone_garden', label: 'Zone #3: Indoor Garden', x: 15.2, z: 14.2 },
  { id: 'zone_lounge', label: 'Zone #4: Living Atrium', x: 0.0, z: -4.2 }
];

const COOKING_BEACONS = [
  { id: 'kitchen_collect', label: 'Step 1: Collect Pantry Rations', x: -11.2, z: -19.8 },
  { id: 'kitchen_cook', label: 'Step 2: Cook Virtual Meal', x: -15.5, z: -19.8 },
  { id: 'kitchen_serve', label: 'Step 3: Serve & Share Meal', x: -4.0, z: -15.0 }
];

const PLAYABLE_STATUSES = new Set(['ACTIVE', 'IN_PROGRESS', 'ASSIGNED', 'AVAILABLE']);

export function createTasks3dManager(scene) {
  const rootGroup = new THREE.Group();
  scene.add(rootGroup);

  const relicGeo = new THREE.OctahedronGeometry(0.32, 0);
  const relicRingGeo = new THREE.TorusGeometry(0.46, 0.04, 10, 24);
  const goldMat = new THREE.MeshStandardMaterial({
    color: 0xfacc15,
    emissive: 0xd97706,
    emissiveIntensity: 0.7,
    roughness: 0.25,
    metalness: 0.85
  });

  const ringGeo = new THREE.TorusGeometry(0.95, 0.085, 12, 32);
  const ringMat = new THREE.MeshStandardMaterial({
    color: 0x22d3ee,
    emissive: 0x06b6d4,
    emissiveIntensity: 0.85,
    roughness: 0.2,
    metalness: 0.65
  });

  const zoneRingGeo = new THREE.TorusGeometry(1.15, 0.06, 10, 32);
  const zonePillarGeo = new THREE.CylinderGeometry(0.28, 0.42, 2.4, 16, 1, true);
  const zoneMat = new THREE.MeshStandardMaterial({
    color: 0x34d399,
    emissive: 0x10b981,
    emissiveIntensity: 0.75,
    transparent: true,
    opacity: 0.42,
    side: THREE.DoubleSide
  });

  const cookRingGeo = new THREE.TorusGeometry(0.85, 0.055, 10, 28);
  const cookMat = new THREE.MeshStandardMaterial({
    color: 0xfb923c,
    emissive: 0xea580c,
    emissiveIntensity: 0.85,
    roughness: 0.25
  });

  const relicMeshes = TREASURE_NODES.map((node) => {
    const group = new THREE.Group();
    group.position.set(node.x, 1.15, node.z);
    const gem = new THREE.Mesh(relicGeo, goldMat);
    const halo = new THREE.Mesh(relicRingGeo, goldMat);
    halo.rotation.x = Math.PI * 0.5;
    group.add(gem, halo);
    group.visible = false;
    rootGroup.add(group);
    return { ...node, group, gem, halo, collected: false };
  });

  const obstacleMeshes = OBSTACLE_RINGS.map((node) => {
    const group = new THREE.Group();
    group.position.set(node.x, 1.25, node.z);
    const torus = new THREE.Mesh(ringGeo, ringMat);
    group.add(torus);
    group.visible = false;
    rootGroup.add(group);
    return { ...node, group, torus, collected: false };
  });

  const checkpointMeshes = CHECKPOINT_ZONES.map((node) => {
    const group = new THREE.Group();
    group.position.set(node.x, 0.05, node.z);
    const ring = new THREE.Mesh(zoneRingGeo, zoneMat);
    ring.rotation.x = Math.PI / 2;
    const pillar = new THREE.Mesh(zonePillarGeo, zoneMat);
    pillar.position.y = 1.2;
    group.add(ring, pillar);
    group.visible = false;
    rootGroup.add(group);
    return { ...node, group, ring, pillar, collected: false };
  });

  const cookingBeacons = COOKING_BEACONS.map((node) => {
    const group = new THREE.Group();
    group.position.set(node.x, 0.08, node.z);
    const ring = new THREE.Mesh(cookRingGeo, cookMat);
    ring.rotation.x = Math.PI / 2;
    group.add(ring);
    group.visible = false;
    rootGroup.add(group);
    return { ...node, group, ring, collected: false };
  });

  let activeTaskId = '';
  let activeType = '';
  let isTaskActive = false;

  function applyVisibility() {
    const showRelics = isTaskActive && activeType === 'TREASURE_HUNT';
    const showRings = isTaskActive && activeType === 'OBSTACLE';
    const showCheckpoints = isTaskActive && activeType === 'CHECKPOINT';
    const showCooking = isTaskActive && activeType === 'COOKING';

    relicMeshes.forEach((m) => {
      m.group.visible = showRelics && !m.collected;
    });
    obstacleMeshes.forEach((m) => {
      m.group.visible = showRings && !m.collected;
    });
    checkpointMeshes.forEach((m) => {
      m.group.visible = showCheckpoints && !m.collected;
    });
    cookingBeacons.forEach((m) => {
      m.group.visible = showCooking && !m.collected;
    });
  }

  return {
    syncTaskState(taskObj, collectedIds = []) {
      const nextId = taskObj && taskObj.taskId ? String(taskObj.taskId) : '';
      const nextType = taskObj && taskObj.activityType ? String(taskObj.activityType) : '';
      const nextActive = Boolean(taskObj && PLAYABLE_STATUSES.has(String(taskObj.status || '')));
      const collectedSet = new Set(Array.isArray(collectedIds) ? collectedIds : []);

      activeTaskId = nextId;
      activeType = nextType;
      isTaskActive = nextActive;

      relicMeshes.forEach((m) => {
        m.collected = collectedSet.has(m.id) || (m.altId ? collectedSet.has(m.altId) : false);
      });
      obstacleMeshes.forEach((m) => {
        m.collected = collectedSet.has(m.id);
      });
      checkpointMeshes.forEach((m) => {
        m.collected = collectedSet.has(m.id);
      });
      cookingBeacons.forEach((m) => {
        m.collected = collectedSet.has(m.id);
      });
      applyVisibility();
    },

    update(elapsed, px, pz, onNodePassed) {
      if (!isTaskActive) return;

      if (activeType === 'TREASURE_HUNT') {
        relicMeshes.forEach((m, idx) => {
          if (!m.group.visible) return;
          m.gem.rotation.y = elapsed * 2.2 + idx;
          m.halo.rotation.z = elapsed * 1.4;
          m.group.position.y = 1.15 + Math.sin(elapsed * 2.6 + idx) * 0.14;
        });
      }

      if (activeType === 'OBSTACLE') {
        obstacleMeshes.forEach((m, idx) => {
          if (!m.group.visible) return;
          m.torus.rotation.y = elapsed * 1.6 + idx * 0.8;
          m.group.position.y = 1.2 + Math.sin(elapsed * 2.2 + idx) * 0.08;
          const dist = Math.hypot(px - m.x, pz - m.z);
          if (dist < 1.35 && !m.collected) {
            m.collected = true;
            m.group.visible = false;
            if (typeof onNodePassed === 'function') {
              onNodePassed(activeTaskId, m);
            }
          }
        });
      }

      if (activeType === 'CHECKPOINT') {
        checkpointMeshes.forEach((m, idx) => {
          if (!m.group.visible) return;
          m.pillar.rotation.y = elapsed * 1.5 + idx;
          const pulse = 1.0 + Math.sin(elapsed * 3.0 + idx) * 0.09;
          m.ring.scale.set(pulse, pulse, 1);
          const dist = Math.hypot(px - m.x, pz - m.z);
          if (dist < 1.65 && !m.collected) {
            m.collected = true;
            m.group.visible = false;
            if (typeof onNodePassed === 'function') {
              onNodePassed(activeTaskId, m);
            }
          }
        });
      }

      if (activeType === 'COOKING') {
        cookingBeacons.forEach((m, idx) => {
          if (!m.group.visible) return;
          const pulse = 1.0 + Math.sin(elapsed * 3.4 + idx) * 0.1;
          m.ring.scale.set(pulse, pulse, 1);
        });
      }
    },

    getNearestInteractable(px, pz, maxDist = 2.45) {
      if (!isTaskActive || activeType !== 'TREASURE_HUNT') return null;
      let best = null;
      let bestDist = maxDist;
      for (const m of relicMeshes) {
        if (!m.group.visible || m.collected) continue;
        const d = Math.hypot(px - m.x, pz - m.z);
        if (d < bestDist) {
          bestDist = d;
          best = m;
        }
      }
      return best;
    },

    collectRelic(nodeId) {
      const m = relicMeshes.find((item) => item.id === nodeId || item.altId === nodeId);
      if (!m || m.collected) return null;
      m.collected = true;
      m.group.visible = false;
      return { taskId: activeTaskId, node: m };
    },

    getProgress() {
      if (!isTaskActive) return { collected: 0, total: 0 };
      if (activeType === 'TREASURE_HUNT') {
        return {
          collected: relicMeshes.filter((m) => m.collected).length,
          total: relicMeshes.length
        };
      }
      if (activeType === 'OBSTACLE') {
        return {
          collected: obstacleMeshes.filter((m) => m.collected).length,
          total: obstacleMeshes.length
        };
      }
      if (activeType === 'CHECKPOINT') {
        return {
          collected: checkpointMeshes.filter((m) => m.collected).length,
          total: checkpointMeshes.length
        };
      }
      if (activeType === 'COOKING') {
        return {
          collected: cookingBeacons.filter((m) => m.collected).length,
          total: cookingBeacons.length
        };
      }
      return { collected: 0, total: 0 };
    }
  };
}
