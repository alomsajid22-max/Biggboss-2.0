import * as THREE from '../vendor/three/three.module.min.js';
import { GLTFLoader } from '../vendor/three/GLTFLoader.js';
import * as SkeletonUtils from '../vendor/three/SkeletonUtils.js';
import { HOUSE_BOUNDS } from './worldLayout.js';

export const EXPRESSIONS = ['neutral', 'happy', 'angry', 'sad', 'surprised'];

export const HOUSEMATE_PROFILES = [
  {
    id: 'Player 01',
    number: 1,
    name: 'Aria',
    gender: 'female',
    archetype: 'Strategist',
    spawn: { x: 0.0, z: 6.0, rotY: 0, room: 'Living Atrium' },
    appearance: {
      skin: 0xf1c29b, top: 0x1f7a8c, accent: 0xeab954, pants: 0x1e2433, shoes: 0xf8fafc,
      hairColor: 0x261c17, hairStyle: 'ponytail', outfit: 'bomber', accessory: 'earrings', glasses: false
    }
  },
  {
    id: 'Player 02',
    number: 2,
    name: 'Kai',
    gender: 'male',
    archetype: 'Wildcard',
    spawn: { x: -6.2, z: -3.5, rotY: 0.7, room: 'Living Atrium' },
    appearance: {
      skin: 0xdca47f, top: 0xb91c1c, accent: 0xfef08a, pants: 0x18181b, shoes: 0xef4444,
      hairColor: 0x111111, hairStyle: 'spiky', outfit: 'hoodie', accessory: 'headphones', glasses: false
    }
  },
  {
    id: 'Player 03',
    number: 3,
    name: 'Zara',
    gender: 'female',
    archetype: 'Architect',
    spawn: { x: 6.2, z: 3.5, rotY: -2.2, room: 'Living Atrium' },
    appearance: {
      skin: 0x8d5524, top: 0xd97706, accent: 0xffffff, pants: 0x27272a, shoes: 0xfef3c7,
      hairColor: 0x1c130f, hairStyle: 'curlyCrown', outfit: 'blazer', accessory: 'glasses', glasses: true
    }
  },
  {
    id: 'Player 04',
    number: 4,
    name: 'Mateo',
    gender: 'male',
    archetype: 'Chef',
    spawn: { x: -0.6, z: -15.0, rotY: 1.57, room: 'Dining Area' },
    appearance: {
      skin: 0xc68642, top: 0xf8fafc, accent: 0x0f172a, pants: 0x334155, shoes: 0x1e293b,
      hairColor: 0x3b2314, hairStyle: 'sidePart', outfit: 'vest', accessory: 'none', glasses: false
    }
  },
  {
    id: 'Player 05',
    number: 5,
    name: 'Nova',
    gender: 'female',
    archetype: 'Showrunner',
    spawn: { x: -17.2, z: -14.8, rotY: -1.57, room: 'Gourmet Kitchen' },
    appearance: {
      skin: 0xf5cba7, top: 0x7c3aed, accent: 0x67e8f9, pants: 0x1e1b4b, shoes: 0xe2e8f0,
      hairColor: 0xe2e8f0, hairStyle: 'bobCut', outfit: 'bomber', accessory: 'earrings', glasses: false
    }
  },
  {
    id: 'Player 06',
    number: 6,
    name: 'Devon',
    gender: 'male',
    archetype: 'Quartermaster',
    spawn: { x: -23.0, z: -14.8, rotY: 0, room: 'Store Room' },
    appearance: {
      skin: 0x6c3d22, top: 0x059669, accent: 0xa7f3d0, pants: 0x1f2937, shoes: 0xf9fafb,
      hairColor: 0x171717, hairStyle: 'shortCrop', outfit: 'hoodie', accessory: 'none', glasses: false
    }
  },
  {
    id: 'Player 07',
    number: 7,
    name: 'Sienna',
    gender: 'female',
    archetype: 'Analyst',
    spawn: { x: -20.1, z: -2.7, rotY: -1.2, room: '15-Player Bedroom' },
    appearance: {
      skin: 0xe0ac69, top: 0xbe185d, accent: 0xfbcfe8, pants: 0x1f2937, shoes: 0xfdf2f8,
      hairColor: 0x7c2d12, hairStyle: 'topBun', outfit: 'blazer', accessory: 'glasses', glasses: true
    }
  },
  {
    id: 'Player 08',
    number: 8,
    name: 'Rohan',
    gender: 'male',
    archetype: 'Athlete',
    spawn: { x: -14.3, z: 2.7, rotY: 2.4, room: '15-Player Bedroom' },
    appearance: {
      skin: 0xa66e46, top: 0x2563eb, accent: 0x93c5fd, pants: 0x0f172a, shoes: 0x38bdf8,
      hairColor: 0x18181b, hairStyle: 'undercut', outfit: 'bomber', accessory: 'headphones', glasses: false
    }
  },
  {
    id: 'Player 09',
    number: 9,
    name: 'Chloe',
    gender: 'female',
    archetype: 'Socialite',
    spawn: { x: -17.5, z: 15.2, rotY: 0.4, room: 'Spa Bathroom' },
    appearance: {
      skin: 0xf6d5b8, top: 0x0d9488, accent: 0xfde68a, pants: 0xf1f5f9, shoes: 0xeab954,
      hairColor: 0xd97706, hairStyle: 'ponytail', outfit: 'vest', accessory: 'earrings', glasses: false
    }
  },
  {
    id: 'Player 10',
    number: 10,
    name: 'Liam',
    gender: 'male',
    archetype: 'Tactician',
    spawn: { x: 0.0, z: 9.2, rotY: Math.PI, room: 'Swimming Pool' },
    appearance: {
      skin: 0xf1c29b, top: 0x0284c7, accent: 0xffffff, pants: 0x1e3a5f, shoes: 0xf8fafc,
      hairColor: 0x5b3a29, hairStyle: 'sidePart', outfit: 'hoodie', accessory: 'none', glasses: false
    }
  },
  {
    id: 'Player 11',
    number: 11,
    name: 'Maya',
    gender: 'female',
    archetype: 'Swimmer',
    spawn: { x: 8.2, z: 15.2, rotY: 1.57, room: 'Swimming Pool' },
    appearance: {
      skin: 0xc68642, top: 0xe11d48, accent: 0xfde047, pants: 0x1e293b, shoes: 0xf1f5f9,
      hairColor: 0x1c1917, hairStyle: 'topBun', outfit: 'bomber', accessory: 'none', glasses: false
    }
  },
  {
    id: 'Player 12',
    number: 12,
    name: 'Tariq',
    gender: 'male',
    archetype: 'Botanist',
    spawn: { x: 15.5, z: 16.0, rotY: -0.9, room: 'Botanical Garden' },
    appearance: {
      skin: 0x8d5524, top: 0x15803d, accent: 0xd9f99d, pants: 0x292524, shoes: 0xd6d3d1,
      hairColor: 0x171717, hairStyle: 'shortCrop', outfit: 'vest', accessory: 'glasses', glasses: true
    }
  },
  {
    id: 'Player 13',
    number: 13,
    name: 'Elena',
    gender: 'female',
    archetype: 'Challenger',
    spawn: { x: 14.0, z: 5.0, rotY: -1.57, room: 'Activity Room' },
    appearance: {
      skin: 0xeec4a2, top: 0xea580c, accent: 0x1e293b, pants: 0x18181b, shoes: 0xfb923c,
      hairColor: 0x451a03, hairStyle: 'bobCut', outfit: 'bomber', accessory: 'earrings', glasses: false
    }
  },
  {
    id: 'Player 14',
    number: 14,
    name: 'Jude',
    gender: 'male',
    archetype: 'Producer',
    spawn: { x: 11.8, z: -4.5, rotY: -1.57, room: 'East Wing Foyer' },
    appearance: {
      skin: 0xd9a077, top: 0x334155, accent: 0x22d3ee, pants: 0x0f172a, shoes: 0x22d3ee,
      hairColor: 0x27272a, hairStyle: 'undercut', outfit: 'blazer', accessory: 'glasses', glasses: true
    }
  },
  {
    id: 'Player 15',
    number: 15,
    name: 'Priya',
    gender: 'female',
    archetype: 'VIP Contender',
    spawn: { x: 8.0, z: -11.2, rotY: 2.3, room: 'VIP Gallery' },
    appearance: {
      skin: 0xb5764c, top: 0x9333ea, accent: 0xfacc15, pants: 0x1e1b4b, shoes: 0xfef08a,
      hairColor: 0x14110f, hairStyle: 'curlyCrown', outfit: 'blazer', accessory: 'earrings', glasses: false
    }
  }
];

const ROOM_WAYPOINTS = {
  living: [
    [-5.8, -5.0], [5.8, -5.0], [-5.8, 5.0], [5.8, 5.0],
    [-2.5, 6.2], [2.5, 6.2], [0.0, -6.4], [-7.2, 0.0], [7.2, 0.0]
  ],
  dining: [
    [-7.8, -15.0], [-0.8, -15.0], [-4.0, -10.2], [-4.0, -19.8]
  ],
  kitchen: [
    [-17.2, -14.8], [-17.2, -11.2], [-17.2, -18.2], [-12.2, -11.0]
  ],
  store: [
    [-23.2, -15.0], [-23.2, -12.0], [-23.2, -18.0]
  ],
  bedroom: [
    [-20.1, -3.8], [-14.3, -3.8], [-20.1, 1.6], [-14.3, 1.6], [-11.5, 0.0]
  ],
  bathroom: [
    [-17.5, 13.0], [-17.5, 16.5], [-23.0, 14.2], [-12.5, 15.8]
  ],
  pool: [
    [-5.0, 9.2], [0.0, 9.2], [5.0, 9.2], [-8.2, 15.0], [8.2, 15.0], [0.0, 20.1]
  ],
  garden: [
    [12.5, 15.8], [15.8, 15.8], [18.2, 13.5], [14.5, 13.0]
  ],
  activity: [
    [14.0, 5.0], [17.5, 2.2], [17.5, 7.8], [21.5, 5.0]
  ],
  gallery: [
    [6.0, -11.2], [11.5, -11.2], [15.8, -11.2], [11.8, -4.5]
  ]
};

function createSharedHairAndAccessAssets() {
  return {
    skullCapGeo: new THREE.SphereGeometry(0.125, 16, 10, 0, Math.PI * 2, 0, Math.PI * 0.56),
    ponytailCapGeo: new THREE.SphereGeometry(0.127, 16, 10, 0, Math.PI * 2, 0, Math.PI * 0.62),
    ponytailTailGeo: new THREE.CapsuleGeometry(0.042, 0.14, 6, 10),
    bunGeo: new THREE.SphereGeometry(0.068, 12, 10),
    bobCrownGeo: new THREE.SphereGeometry(0.132, 16, 12, 0, Math.PI * 2, 0, Math.PI * 0.68),
    bobSideGeo: new THREE.CapsuleGeometry(0.038, 0.11, 5, 8),
    curlPuffGeo: new THREE.SphereGeometry(0.064, 10, 8),
    spikeGeo: new THREE.ConeGeometry(0.036, 0.11, 8),
    sideWaveGeo: new THREE.CapsuleGeometry(0.045, 0.13, 5, 10),
    lensGeo: new THREE.TorusGeometry(0.025, 0.0045, 8, 16),
    bridgeGeo: new THREE.BoxGeometry(0.026, 0.005, 0.005),
    templeArmGeo: new THREE.BoxGeometry(0.005, 0.005, 0.11),
    earringGeo: new THREE.TorusGeometry(0.017, 0.0035, 6, 14),
    headbandGeo: new THREE.TorusGeometry(0.124, 0.014, 8, 18, Math.PI),
    earCupGeo: new THREE.CylinderGeometry(0.038, 0.038, 0.026, 12),
    ringGeo: new THREE.RingGeometry(0.28, 0.36, 28),
    glassesMat: new THREE.MeshStandardMaterial({ color: 0x0f172a, roughness: 0.22, metalness: 0.85 }),
    goldAccessoryMat: new THREE.MeshStandardMaterial({ color: 0xfacc15, roughness: 0.24, metalness: 0.88 }),
    headphoneMat: new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.35, metalness: 0.65 })
  };
}

function buildHeadAttachment(app, hairAssets) {
  const headAttach = new THREE.Group();
  const hairMat = new THREE.MeshStandardMaterial({ color: app.hairColor, roughness: 0.68, metalness: 0.05 });

  // Head bone is at world Y=1.58; cranium ellipsoid center is +0.11 above Head bone
  const skullY = 0.112;
  const style = app.hairStyle || 'shortCrop';

  if (style === 'ponytail') {
    const cap = new THREE.Mesh(hairAssets.ponytailCapGeo, hairMat);
    cap.position.set(0, skullY + 0.008, 0.004);
    cap.scale.set(0.98, 1.15, 1.06);
    cap.rotation.x = 0.16;
    headAttach.add(cap);

    const tie = new THREE.Mesh(hairAssets.earringGeo, hairAssets.goldAccessoryMat);
    tie.position.set(0, skullY - 0.01, 0.135);
    tie.rotation.x = Math.PI / 2;
    headAttach.add(tie);

    const tail = new THREE.Mesh(hairAssets.ponytailTailGeo, hairMat);
    tail.position.set(0, skullY - 0.08, 0.155);
    tail.rotation.x = 0.28;
    headAttach.add(tail);
  } else if (style === 'topBun') {
    const cap = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    cap.position.set(0, skullY + 0.01, 0.002);
    cap.scale.set(0.98, 1.16, 1.06);
    cap.rotation.x = 0.15;
    headAttach.add(cap);

    const bun = new THREE.Mesh(hairAssets.bunGeo, hairMat);
    bun.position.set(0, skullY + 0.155, 0.025);
    headAttach.add(bun);
  } else if (style === 'bobCut') {
    const crown = new THREE.Mesh(hairAssets.bobCrownGeo, hairMat);
    crown.position.set(0, skullY + 0.005, 0.014);
    crown.scale.set(1.02, 1.18, 1.08);
    crown.rotation.x = 0.18;
    headAttach.add(crown);

    [-1, 1].forEach((side) => {
      const lock = new THREE.Mesh(hairAssets.bobSideGeo, hairMat);
      lock.position.set(side * 0.105, skullY - 0.075, -0.015);
      lock.rotation.z = side * -0.1;
      headAttach.add(lock);
    });
  } else if (style === 'curlyCrown') {
    const baseCap = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    baseCap.position.set(0, skullY + 0.01, 0.005);
    baseCap.scale.set(1.02, 1.15, 1.08);
    headAttach.add(baseCap);

    const curlOffsets = [
      [0, 0.13, -0.02], [-0.075, 0.11, -0.01], [0.075, 0.11, -0.01],
      [-0.105, 0.06, 0.02], [0.105, 0.06, 0.02], [0, 0.10, 0.08],
      [-0.07, 0.08, 0.08], [0.07, 0.08, 0.08], [-0.05, 0.125, -0.06], [0.05, 0.125, -0.06]
    ];
    for (const [cx, cy, cz] of curlOffsets) {
      const puff = new THREE.Mesh(hairAssets.curlPuffGeo, hairMat);
      puff.position.set(cx, skullY + cy, cz);
      headAttach.add(puff);
    }
  } else if (style === 'spiky') {
    const cap = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    cap.position.set(0, skullY + 0.008, 0.005);
    cap.scale.set(0.98, 1.14, 1.06);
    headAttach.add(cap);

    const spikes = [
      [0, 0.155, -0.04, -0.25, 0],
      [-0.045, 0.145, -0.02, -0.15, 0.25],
      [0.045, 0.145, -0.02, -0.15, -0.25],
      [0, 0.15, 0.03, 0.2, 0],
      [-0.05, 0.135, 0.04, 0.2, 0.3],
      [0.05, 0.135, 0.04, 0.2, -0.3]
    ];
    for (const [sx, sy, sz, rx, rz] of spikes) {
      const sp = new THREE.Mesh(hairAssets.spikeGeo, hairMat);
      sp.position.set(sx, skullY + sy, sz);
      sp.rotation.set(rx, 0, rz);
      headAttach.add(sp);
    }
  } else if (style === 'sidePart') {
    const cap = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    cap.position.set(0, skullY + 0.008, 0.004);
    cap.scale.set(0.99, 1.15, 1.07);
    headAttach.add(cap);

    const wave = new THREE.Mesh(hairAssets.sideWaveGeo, hairMat);
    wave.position.set(0.028, skullY + 0.115, -0.035);
    wave.rotation.z = Math.PI / 2 - 0.12;
    wave.rotation.y = 0.24;
    headAttach.add(wave);
  } else if (style === 'undercut') {
    const fadeCap = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    fadeCap.position.set(0, skullY + 0.018, 0.004);
    fadeCap.scale.set(0.96, 1.12, 1.05);
    headAttach.add(fadeCap);

    const quiff = new THREE.Mesh(hairAssets.sideWaveGeo, hairMat);
    quiff.position.set(0, skullY + 0.125, -0.025);
    quiff.rotation.x = Math.PI / 2 + 0.22;
    quiff.scale.set(1.25, 1.0, 0.9);
    headAttach.add(quiff);
  } else {
    // Clean tailored shortCrop
    const crop = new THREE.Mesh(hairAssets.skullCapGeo, hairMat);
    crop.position.set(0, skullY + 0.008, 0.004);
    crop.scale.set(0.99, 1.16, 1.07);
    crop.rotation.x = 0.16;
    headAttach.add(crop);
  }

  // Sculpted Eyewear
  if (app.glasses || app.accessory === 'glasses') {
    const gGrp = new THREE.Group();
    gGrp.position.set(0, 0.112, -0.128);
    const lensL = new THREE.Mesh(hairAssets.lensGeo, hairAssets.glassesMat);
    lensL.position.x = -0.045;
    gGrp.add(lensL);
    const lensR = new THREE.Mesh(hairAssets.lensGeo, hairAssets.glassesMat);
    lensR.position.x = 0.045;
    gGrp.add(lensR);
    const bridge = new THREE.Mesh(hairAssets.bridgeGeo, hairAssets.glassesMat);
    gGrp.add(bridge);
    [-1, 1].forEach((side) => {
      const arm = new THREE.Mesh(hairAssets.templeArmGeo, hairAssets.glassesMat);
      arm.position.set(side * 0.072, 0.004, 0.055);
      gGrp.add(arm);
    });
    headAttach.add(gGrp);
  }

  // Hoop Earrings
  if (app.accessory === 'earrings') {
    [-1, 1].forEach((side) => {
      const ring = new THREE.Mesh(hairAssets.earringGeo, hairAssets.goldAccessoryMat);
      ring.position.set(side * 0.122, 0.068, 0.005);
      ring.rotation.y = Math.PI / 2;
      headAttach.add(ring);
    });
  }

  // Reality-Show Studio Headphones
  if (app.accessory === 'headphones') {
    const band = new THREE.Mesh(hairAssets.headbandGeo, hairAssets.headphoneMat);
    band.position.set(0, skullY + 0.02, 0.005);
    headAttach.add(band);
    [-1, 1].forEach((side) => {
      const cup = new THREE.Mesh(hairAssets.earCupGeo, hairAssets.headphoneMat);
      cup.rotation.z = Math.PI / 2;
      cup.position.set(side * 0.128, 0.095, 0.005);
      headAttach.add(cup);
    });
  }

  return headAttach;
}

function createNameplateTexture(topLabel, bottomLabel, variant = 'npc', isCaptain = false, isVip = false) {
  const canvas = document.createElement('canvas');
  canvas.width = 296;
  canvas.height = 98;
  const ctx = canvas.getContext('2d');

  const isUser = variant === 'user';
  const isRemote = variant === 'remote';

  ctx.fillStyle = isCaptain
    ? 'rgba(42, 28, 8, 0.94)'
    : (isVip
      ? 'rgba(35, 16, 52, 0.94)'
      : (isUser
        ? 'rgba(17, 24, 39, 0.92)'
        : (isRemote ? 'rgba(6, 40, 32, 0.90)' : 'rgba(15, 23, 42, 0.72)')));
  ctx.strokeStyle = isCaptain
    ? '#facc15'
    : (isVip
      ? '#c084fc'
      : (isUser
        ? '#eab954'
        : (isRemote ? '#34d399' : 'rgba(148, 163, 184, 0.55)')));
  ctx.lineWidth = isCaptain || isVip || isUser || isRemote ? 4 : 2.5;
  ctx.beginPath();
  ctx.roundRect(10, 10, 276, 78, 20);
  ctx.fill();
  ctx.stroke();

  ctx.fillStyle = isCaptain ? '#facc15' : (isVip ? '#d8b4fe' : (isUser ? '#eab954' : (isRemote ? '#34d399' : '#94a3b8')));
  ctx.font = '800 18px sans-serif';
  ctx.textAlign = 'center';
  const badgePrefix = (isCaptain && isVip)
    ? '👑💎 CAPTAIN VIP • '
    : (isCaptain
      ? '👑 CAPTAIN • '
      : (isVip
        ? '💎 VIP • '
        : (isUser ? '★ YOU • ' : (isRemote ? '● LIVE • ' : '🤖 NPC • '))));
  ctx.fillText(`${badgePrefix}${String(topLabel).toUpperCase()}`.slice(0, 26), 148, 37);

  ctx.fillStyle = '#f8fafc';
  ctx.font = '700 24px sans-serif';
  ctx.fillText(String(bottomLabel).slice(0, 22), 148, 71);

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

export function resolveEnvironmentCollisions(nx, nz, colliders, radius = 0.36) {
  let px = Math.max(HOUSE_BOUNDS.minX + 0.55, Math.min(HOUSE_BOUNDS.maxX - 0.55, nx));
  let pz = Math.max(HOUSE_BOUNDS.minZ + 0.55, Math.min(HOUSE_BOUNDS.maxZ - 0.55, nz));

  for (let pass = 0; pass < 2; pass++) {
    for (const c of colliders) {
      if (c.active === false) continue;

      if (c.isCircle) {
        const dx = px - c.cx;
        const dz = pz - c.cz;
        const minD = c.radius + radius;
        const distSq = dx * dx + dz * dz;
        if (distSq < minD * minD && distSq > 1e-6) {
          const dist = Math.sqrt(distSq);
          const push = minD - dist;
          px += (dx / dist) * push;
          pz += (dz / dist) * push;
        }
      } else {
        const closestX = Math.max(c.minX, Math.min(px, c.maxX));
        const closestZ = Math.max(c.minZ, Math.min(pz, c.maxZ));
        const dx = px - closestX;
        const dz = pz - closestZ;
        const distSq = dx * dx + dz * dz;

        if (distSq < radius * radius) {
          if (distSq > 1e-6) {
            const dist = Math.sqrt(distSq);
            const push = radius - dist;
            px += (dx / dist) * push;
            pz += (dz / dist) * push;
          } else {
            const dLeft = Math.abs(px - c.minX);
            const dRight = Math.abs(c.maxX - px);
            const dTop = Math.abs(pz - c.minZ);
            const dBot = Math.abs(c.maxZ - pz);
            const m = Math.min(dLeft, dRight, dTop, dBot);
            if (m === dLeft) px = c.minX - radius;
            else if (m === dRight) px = c.maxX + radius;
            else if (m === dTop) pz = c.minZ - radius;
            else pz = c.maxZ + radius;
          }
        }
      }
    }
  }
  return [px, pz];
}

function shortestAngleDiff(a, b) {
  let diff = ((b - a + Math.PI) % (Math.PI * 2)) - Math.PI;
  if (diff < -Math.PI) diff += Math.PI * 2;
  return diff;
}

class HousemateCharacter {
  constructor(profile, sharedAssets, templates, scene) {
    this.profile = profile;
    this.id = profile.id;
    this.name = profile.name;
    this.gender = profile.gender || 'female';
    this.x = profile.spawn.x;
    this.z = profile.spawn.z;
    this.rotY = profile.spawn.rotY;
    this.radius = 0.36;

    this.isUserControlled = false;
    this.isCaptain = false;
    this.isVip = false;
    this.variant = 'npc';
    this.expression = 'neutral';

    // NPC AI state
    this.npcState = 'idle';
    this.npcTimer = 1.5 + Math.random() * 4.0;
    this.npcTarget = null;
    this.occupiedTarget = null;
    this.blockedTimer = 0;

    this.group = new THREE.Group();
    this.group.position.set(this.x, 0, this.z);
    this.group.rotation.y = this.rotY;

    this.bodyPivot = new THREE.Group();
    this.group.add(this.bodyPivot);

    this.glbMixer = null;
    this.glbActions = {};
    this.currentAction = null;
    this.currentMode = null;
    this.morphMeshes = [];
    this.shadowMeshes = [];
    this.animBones = {};
    this.customMats = null;
    this.headBones = [];
    this.headAttachment = null;
    this.sharedAssets = sharedAssets;
    this.activityAnimTime = 0;

    this._buildIdentityRing(sharedAssets);
    this._buildNameplate('npc');

    const tpl = templates ? (templates[this.gender] || templates.female || templates.male) : null;
    if (tpl) {
      this.applyHumanoidTemplate(tpl, sharedAssets);
    }

    scene.add(this.group);
  }

  _buildIdentityRing(sharedAssets) {
    this.ringMat = new THREE.MeshBasicMaterial({
      color: 0x64748b,
      transparent: true,
      opacity: 0.34,
      side: THREE.DoubleSide,
      depthWrite: false
    });
    this.identityRing = new THREE.Mesh(sharedAssets.ringGeo, this.ringMat);
    this.identityRing.rotation.x = -Math.PI / 2;
    this.identityRing.position.set(0, 0.025, 0);
    this.group.add(this.identityRing);
  }

  _updateRingStyle() {
    if (!this.ringMat) return;
    if (this.isCaptain) {
      this.ringMat.color.setHex(0xfacc15);
      this.ringMat.opacity = 0.92;
    } else if (this.isVip) {
      this.ringMat.color.setHex(0xc084fc);
      this.ringMat.opacity = 0.90;
    } else if (this.isUserControlled || this.variant === 'user') {
      this.ringMat.color.setHex(0xeab954);
      this.ringMat.opacity = 0.88;
    } else if (this.variant === 'remote') {
      this.ringMat.color.setHex(0x34d399);
      this.ringMat.opacity = 0.82;
    } else {
      // NPCs clearly distinguishable from real players
      this.ringMat.color.setHex(0x64748b);
      this.ringMat.opacity = 0.32;
    }
  }

  applyHumanoidTemplate(template, sharedAssets) {
    if (!template || !template.scene) return;

    this.bodyPivot.clear();
    const clonedScene = SkeletonUtils.clone(template.scene);
    const app = this.profile.appearance;

    const customMats = {
      mat_skin: new THREE.MeshStandardMaterial({ color: app.skin, roughness: 0.62, metalness: 0.02 }),
      mat_top: new THREE.MeshStandardMaterial({ color: app.top, roughness: 0.52, metalness: 0.08 }),
      mat_accent: new THREE.MeshStandardMaterial({ color: app.accent, roughness: 0.36, metalness: 0.42 }),
      mat_bottom: new THREE.MeshStandardMaterial({ color: app.pants, roughness: 0.72, metalness: 0.04 }),
      mat_shoes: new THREE.MeshStandardMaterial({ color: app.shoes, roughness: 0.42, metalness: 0.06 })
    };

    const headBones = [];
    this.morphMeshes = [];
    this.shadowMeshes = [];
    this.animBones = {};

    clonedScene.traverse((obj) => {
      if (obj.isBone) {
        const bn = (obj.name || '').toLowerCase();
        if (obj.name === 'Head' || bn.includes('head')) {
          if (obj.name === 'Head') headBones.push(obj);
          this.animBones.head = obj;
        } else if (bn.includes('spine') || bn.includes('chest')) {
          this.animBones.spine = this.animBones.spine || obj;
        } else if (bn.includes('rightupperarm') || bn.includes('rightarm') || bn.endsWith('arm_r')) {
          this.animBones.rightArm = obj;
        } else if (bn.includes('rightforearm') || bn.endsWith('forearm_r')) {
          this.animBones.rightForeArm = obj;
        } else if (bn.includes('leftupperarm') || bn.includes('leftarm') || bn.endsWith('arm_l')) {
          this.animBones.leftArm = obj;
        } else if (bn.includes('leftforearm') || bn.endsWith('forearm_l')) {
          this.animBones.leftForeArm = obj;
        }
      }
      if (obj.isMesh || obj.isSkinnedMesh) {
        const matName = obj.material && obj.material.name;
        if (matName && customMats[matName]) {
          obj.material = customMats[matName];
        }
        obj.castShadow = this.isUserControlled;
        obj.receiveShadow = true;
        obj.frustumCulled = false; // Prevent skin bounding-sphere pop when sitting/swimming/dancing
        this.shadowMeshes.push(obj);

        if (obj.morphTargetDictionary && obj.morphTargetInfluences) {
          this.morphMeshes.push(obj);
        }
      }
    });

    this.customMats = customMats;
    this.headBones = headBones;
    this.sharedAssets = sharedAssets || this.sharedAssets;

    if (headBones.length > 0 && this.sharedAssets) {
      const attachment = buildHeadAttachment(app, this.sharedAssets);
      headBones[0].add(attachment);
      this.headAttachment = attachment;
    }

    this.bodyPivot.add(clonedScene);
    this.glbMixer = new THREE.AnimationMixer(clonedScene);
    this.glbActions = {};

    for (const clip of template.animations || []) {
      const action = this.glbMixer.clipAction(clip);
      this.glbActions[clip.name] = action;
    }

    this.currentAction = null;
    this.currentMode = null;
    this._playClip(this.npcState && !this.isUserControlled ? this.npcState : 'idle', 0);
  }

  _playClip(mode, fadeDuration = 0.22) {
    if (!this.glbMixer || !this.glbActions) return;
    let resolved = mode;
    if (mode === 'bed') resolved = 'sleep';
    else if (mode === 'sit_eat') resolved = 'sit';
    else if (mode === 'cook' || mode === 'eat' || mode === 'wash') resolved = 'idle';
    const mappedName = this.glbActions[resolved] ? resolved : 'idle';
    const nextAction = this.glbActions[mappedName] || this.glbActions.idle;
    if (!nextAction) return;

    if (this.currentAction === nextAction) {
      if (!nextAction.isRunning()) nextAction.play();
      return;
    }

    nextAction.reset();
    nextAction.setEffectiveWeight(1);
    nextAction.play();

    if (this.currentAction) {
      this.currentAction.crossFadeTo(nextAction, fadeDuration, true);
    } else if (fadeDuration > 0) {
      nextAction.fadeIn(fadeDuration);
    }
    this.currentAction = nextAction;
    this.currentMode = mappedName;
  }

  applyAppearanceOverrides(overrides = {}) {
    if (!overrides || typeof overrides !== 'object') return;
    const parseColor = (v, fallback) => {
      if (typeof v === 'number' && !Number.isNaN(v)) return v;
      if (typeof v === 'string' && v.trim()) {
        const cleaned = v.trim().replace('#', '');
        const parsed = parseInt(cleaned, 16);
        if (!Number.isNaN(parsed)) return parsed;
      }
      return fallback;
    };
    const app = this.profile.appearance;
    const merged = {
      skin: parseColor(overrides.skin, app.skin),
      top: parseColor(overrides.top, app.top),
      accent: parseColor(overrides.accent, app.accent),
      pants: parseColor(overrides.pants, app.pants),
      shoes: parseColor(overrides.shoes, app.shoes),
      hairColor: parseColor(overrides.hairColor, app.hairColor),
      hairStyle: overrides.hairStyle || app.hairStyle,
      outfit: overrides.outfit || app.outfit,
      accessory: overrides.accessory || app.accessory,
      glasses: typeof overrides.glasses === 'boolean' ? overrides.glasses : app.glasses
    };
    this.profile.appearance = merged;

    if (this.customMats) {
      this.customMats.mat_skin.color.setHex(merged.skin);
      this.customMats.mat_top.color.setHex(merged.top);
      this.customMats.mat_accent.color.setHex(merged.accent);
      this.customMats.mat_bottom.color.setHex(merged.pants);
      this.customMats.mat_shoes.color.setHex(merged.shoes);
    }

    if (this.headBones && this.headBones.length > 0 && this.sharedAssets) {
      if (this.headAttachment && this.headAttachment.parent) {
        this.headAttachment.parent.remove(this.headAttachment);
      }
      const nextAttachment = buildHeadAttachment(merged, this.sharedAssets);
      this.headBones[0].add(nextAttachment);
      this.headAttachment = nextAttachment;
    }
  }

  setExpression(expr = 'neutral') {
    const clean = EXPRESSIONS.includes(expr) ? expr : 'neutral';
    this.expression = clean;
  }

  _updateMorphExpressions(dt) {
    if (!this.morphMeshes || this.morphMeshes.length === 0) return;
    const speed = Math.min(1, dt * 11);
    for (const mesh of this.morphMeshes) {
      const dict = mesh.morphTargetDictionary;
      const infl = mesh.morphTargetInfluences;
      if (!dict || !infl) continue;
      for (const exprName of ['happy', 'angry', 'sad', 'surprised']) {
        const idx = dict[exprName];
        if (typeof idx === 'number') {
          const target = this.expression === exprName ? 1.0 : 0.0;
          infl[idx] += (target - infl[idx]) * speed;
        }
      }
    }
  }

  _buildNameplate(variant = 'npc') {
    this.variant = variant;
    this.npcTex = createNameplateTexture(this.id, this.name, variant);
    this.userTex = null;
    const mat = new THREE.SpriteMaterial({
      map: this.npcTex,
      transparent: true,
      depthTest: true,
      depthWrite: false
    });
    this.nameplate = new THREE.Sprite(mat);
    this.nameplate.position.set(0, 2.16, 0);
    this.nameplate.scale.set(1.48, 0.50, 1);
    this.group.add(this.nameplate);
    this._updateRingStyle();
  }

  setCustomLabel(topLabel, bottomLabel, variant = 'user', isCaptain = false, isVip = false) {
    this.variant = variant;
    this.isCaptain = Boolean(isCaptain);
    this.isVip = Boolean(isVip);
    const tex = createNameplateTexture(topLabel, bottomLabel, variant, isCaptain, isVip);
    if (variant === 'user') {
      if (this.userTex) this.userTex.dispose();
      this.userTex = tex;
      if (this.isUserControlled) {
        this.nameplate.material.map = tex;
        this.nameplate.material.needsUpdate = true;
      }
    } else {
      if (this.npcTex) this.npcTex.dispose();
      this.npcTex = tex;
      if (!this.isUserControlled) {
        this.nameplate.material.map = tex;
        this.nameplate.material.needsUpdate = true;
      }
    }
    this._updateRingStyle();
  }

  setUserControlled(isUser) {
    this.isUserControlled = isUser;
    this.variant = isUser ? 'user' : 'npc';
    if (isUser && !this.userTex) {
      this.userTex = createNameplateTexture(this.id, this.name, 'user', this.isCaptain, this.isVip);
    } else if (!isUser && !this.npcTex) {
      this.npcTex = createNameplateTexture(this.id, this.name, 'npc', this.isCaptain, this.isVip);
    }
    this.nameplate.material.map = isUser ? this.userTex : this.npcTex;
    this.nameplate.material.needsUpdate = true;
    for (const m of this.shadowMeshes) {
      m.castShadow = isUser;
    }
    this._updateRingStyle();
    if (!isUser) {
      this.npcState = 'idle';
      this.npcTimer = 1.0 + Math.random() * 2.5;
      this.npcTarget = null;
      this._playClip('idle', 0.22);
    }
  }

  applyPose(mode, dt, speed = 0) {
    if (!this.glbMixer) return;

    this._playClip(mode, 0.20);
    if (this.currentAction) {
      if (mode === 'run') {
        this.currentAction.setEffectiveTimeScale(Math.max(0.85, speed / 7.2));
      } else if (mode === 'walk') {
        this.currentAction.setEffectiveTimeScale(Math.max(0.75, speed / 3.8));
      } else {
        this.currentAction.setEffectiveTimeScale(1.0);
      }
    }

    this.glbMixer.update(dt);
    this._applyKitchenActivityBones(mode, dt);
    this._updateMorphExpressions(dt);
  }

  _applyKitchenActivityBones(mode, dt) {
    if (mode !== 'cook' && mode !== 'eat' && mode !== 'sit_eat' && mode !== 'wash') {
      this.activityAnimTime = 0;
      return;
    }
    this.activityAnimTime += dt;
    const t = this.activityAnimTime;
    const { rightArm, rightForeArm, leftArm, leftForeArm, spine } = this.animBones;

    if (mode === 'cook') {
      if (spine) spine.rotation.x += 0.12;
      if (rightArm) {
        rightArm.rotation.x -= 0.72 + Math.sin(t * 5.2) * 0.12;
        rightArm.rotation.z -= 0.22 - Math.cos(t * 5.2) * 0.16;
      }
      if (rightForeArm) rightForeArm.rotation.x -= 0.65;
      if (leftArm) leftArm.rotation.x -= 0.42;
      if (leftForeArm) leftForeArm.rotation.x -= 0.45;
    } else if (mode === 'eat' || mode === 'sit_eat') {
      const lift = (Math.sin(t * 3.8) + 1) * 0.5;
      if (rightArm) rightArm.rotation.x -= 0.52 + lift * 0.48;
      if (rightForeArm) rightForeArm.rotation.x -= 0.58 + lift * 0.55;
      if (leftArm) leftArm.rotation.x -= 0.28;
    } else if (mode === 'wash') {
      if (spine) spine.rotation.x += 0.14;
      const rub = Math.sin(t * 8.0) * 0.15;
      if (rightArm) rightArm.rotation.x -= 0.68 + rub;
      if (leftArm) leftArm.rotation.x -= 0.68 - rub;
      if (rightForeArm) rightForeArm.rotation.x -= 0.55;
      if (leftForeArm) leftForeArm.rotation.x -= 0.55;
    }
  }

  syncTransform(yOffset = 0) {
    this.group.position.set(this.x, yOffset, this.z);
    this.group.rotation.y = this.rotY;
    if (this.identityRing) {
      // Hide underwater floor ring while sleeping on bed or keep pinned to yOffset floor
      this.identityRing.visible = Math.abs(yOffset) < 0.25;
      this.identityRing.position.y = 0.025 - yOffset;
    }
  }
}

export function createHousematesManager(scene, colliders) {
  const sharedAssets = createSharedHairAndAccessAssets();
  const glbTemplates = { male: null, female: null };
  let modelsLoaded = false;

  const characters = HOUSEMATE_PROFILES.map((p) => new HousemateCharacter(p, sharedAssets, null, scene));

  const allWaypoints = [];
  Object.values(ROOM_WAYPOINTS).forEach((pts) => {
    pts.forEach(([x, z]) => allWaypoints.push({ x, z }));
  });

  function pickNearbyWaypoint(cx, cz) {
    const candidates = allWaypoints.filter((w) => {
      const d = Math.hypot(w.x - cx, w.z - cz);
      return d > 1.8 && d < 12.5;
    });
    if (candidates.length === 0) {
      return ROOM_WAYPOINTS.living[Math.floor(Math.random() * ROOM_WAYPOINTS.living.length)];
    }
    const chosen = candidates[Math.floor(Math.random() * candidates.length)];
    return [chosen.x, chosen.z];
  }

  function releaseNpcOccupancy(c, furnitureManager) {
    if (c.occupiedTarget && furnitureManager) {
      furnitureManager.releaseTarget(c.occupiedTarget.id, c.id);
      const [ex, ez] = resolveEnvironmentCollisions(
        c.occupiedTarget.exitX,
        c.occupiedTarget.exitZ,
        colliders,
        c.radius
      );
      c.x = ex;
      c.z = ez;
      c.occupiedTarget = null;
      c.syncTransform(0);
    }
  }

  let activeCharacter = characters[0];
  activeCharacter.setUserControlled(true);

  let npcsEnabled = false;
  const remoteAvatars = new Map();
  let captainUid = '';
  let vipUidsSet = new Set();
  let activeIdentity = { uid: '', playerId: '', displayName: '' };

  function applyNpcVisibility() {
    for (const c of characters) {
      if (c.isUserControlled) {
        c.group.visible = true;
      } else {
        c.group.visible = npcsEnabled;
      }
    }
  }
  applyNpcVisibility();

  async function loadModels() {
    if (modelsLoaded) return true;
    const loader = new GLTFLoader();
    const loadOne = (url) =>
      new Promise((resolve, reject) => {
        loader.load(url, resolve, undefined, reject);
      });

    const [maleGltf, femaleGltf] = await Promise.all([
      loadOne('./models/human_male.glb'),
      loadOne('./models/human_female.glb')
    ]);

    glbTemplates.male = maleGltf;
    glbTemplates.female = femaleGltf;
    modelsLoaded = true;

    for (const c of characters) {
      const tpl = glbTemplates[c.gender] || glbTemplates.female;
      c.applyHumanoidTemplate(tpl, sharedAssets);
    }
    for (const entry of remoteAvatars.values()) {
      const tpl = glbTemplates[entry.avatar.gender] || glbTemplates.female;
      entry.avatar.applyHumanoidTemplate(tpl, sharedAssets);
    }
    return true;
  }

  return {
    characters,
    loadModels,
    getActiveCharacter() {
      return activeCharacter;
    },
    getById(id) {
      return characters.find((c) => c.id === id) || characters[0];
    },
    selectCharacter(id, furnitureManager = null, customLabel = null) {
      const next = this.getById(id);
      const prevExpr = activeCharacter ? activeCharacter.expression : 'neutral';
      if (activeCharacter && activeCharacter !== next) {
        activeCharacter.setUserControlled(false);
      }
      if (next.occupiedTarget) {
        releaseNpcOccupancy(next, furnitureManager);
      }
      activeCharacter = next;
      activeCharacter.setUserControlled(true);
      activeCharacter.setExpression(prevExpr);
      if (customLabel && customLabel.displayName) {
        activeIdentity = {
          uid: customLabel.uid || activeIdentity.uid,
          playerId: customLabel.playerId || activeCharacter.id,
          displayName: customLabel.displayName
        };
        const isCap = Boolean(captainUid && activeIdentity.uid && captainUid === activeIdentity.uid);
        const isVip = Boolean(activeIdentity.uid && vipUidsSet.has(activeIdentity.uid));
        activeCharacter.setCustomLabel(activeIdentity.playerId, activeIdentity.displayName, 'user', isCap, isVip);
      }
      applyNpcVisibility();
      return activeCharacter;
    },
    setActiveUserIdentity(playerId, displayName, uid = '') {
      if (displayName) {
        activeIdentity = {
          uid: uid || activeIdentity.uid,
          playerId: playerId || activeCharacter.id,
          displayName
        };
        const isCap = Boolean(captainUid && activeIdentity.uid && captainUid === activeIdentity.uid);
        const isVip = Boolean(activeIdentity.uid && vipUidsSet.has(activeIdentity.uid));
        if (activeCharacter) {
          activeCharacter.setCustomLabel(activeIdentity.playerId, activeIdentity.displayName, 'user', isCap, isVip);
        }
      }
    },
    setActiveExpression(expr) {
      if (activeCharacter) {
        activeCharacter.setExpression(expr);
      }
    },
    setActiveAppearance(appearanceObj) {
      if (activeCharacter && appearanceObj) {
        activeCharacter.applyAppearanceOverrides(appearanceObj);
      }
    },
    refreshBadges() {
      if (activeCharacter && activeIdentity.displayName) {
        const isCap = Boolean(captainUid && activeIdentity.uid && captainUid === activeIdentity.uid);
        const isVip = Boolean(activeIdentity.uid && vipUidsSet.has(activeIdentity.uid));
        activeCharacter.setCustomLabel(activeIdentity.playerId, activeIdentity.displayName, 'user', isCap, isVip);
      }
      for (const entry of remoteAvatars.values()) {
        const isCap = Boolean(captainUid && entry.uid === captainUid);
        const isVip = vipUidsSet.has(entry.uid);
        entry.isCaptain = isCap;
        entry.isVip = isVip;
        entry.avatar.setCustomLabel(entry.playerId, entry.displayName, 'remote', isCap, isVip);
      }
    },
    setCaptainUid(nextCaptainUid = '') {
      const clean = String(nextCaptainUid || '');
      if (clean === captainUid) return;
      captainUid = clean;
      this.refreshBadges();
    },
    setVipUids(nextVipUids = []) {
      const nextSet = new Set((Array.isArray(nextVipUids) ? nextVipUids : []).map((u) => String(u || '')).filter(Boolean));
      vipUidsSet = nextSet;
      this.refreshBadges();
    },
    setNpcsEnabled(enabled, furnitureManager = null) {
      npcsEnabled = Boolean(enabled);
      if (!npcsEnabled && furnitureManager) {
        for (const c of characters) {
          if (!c.isUserControlled && c.occupiedTarget) {
            releaseNpcOccupancy(c, furnitureManager);
          }
        }
      }
      applyNpcVisibility();
    },
    areNpcsEnabled() {
      return npcsEnabled;
    },
    syncRemotePlayers(remoteList = []) {
      const seenUids = new Set();
      for (const rp of remoteList) {
        if (!rp || !rp.uid || !rp.online || rp.state === 'offline') continue;
        seenUids.add(rp.uid);
        const existing = remoteAvatars.get(rp.uid);
        const targetProfile = HOUSEMATE_PROFILES.find((p) => p.id === rp.characterId) || HOUSEMATE_PROFILES[0];

        const isCap = Boolean(captainUid && rp.uid === captainUid);
        const isVip = vipUidsSet.has(rp.uid);
        if (!existing || existing.characterId !== targetProfile.id) {
          if (existing) {
            scene.remove(existing.avatar.group);
          }
          const avatar = new HousemateCharacter(targetProfile, sharedAssets, modelsLoaded ? glbTemplates : null, scene);
          avatar.x = Number(rp.x) || targetProfile.spawn.x;
          avatar.z = Number(rp.z) || targetProfile.spawn.z;
          avatar.rotY = Number(rp.rotY) || 0;
          avatar.syncTransform(Number(rp.y) || 0);
          avatar.setExpression(rp.expression || 'neutral');
          avatar.setCustomLabel(rp.playerId || targetProfile.id, rp.displayName || targetProfile.name, 'remote', isCap, isVip);
          remoteAvatars.set(rp.uid, {
            uid: rp.uid,
            characterId: targetProfile.id,
            displayName: rp.displayName || targetProfile.name,
            playerId: rp.playerId || targetProfile.id,
            avatar,
            targetX: avatar.x,
            targetY: Number(rp.y) || 0,
            targetZ: avatar.z,
            targetRotY: avatar.rotY,
            curY: Number(rp.y) || 0,
            state: rp.state || 'idle',
            expression: rp.expression || 'neutral',
            isCaptain: isCap,
            isVip
          });
        } else {
          if (existing.displayName !== rp.displayName || existing.playerId !== rp.playerId || existing.isCaptain !== isCap || existing.isVip !== isVip) {
            existing.displayName = rp.displayName || existing.displayName;
            existing.playerId = rp.playerId || existing.playerId;
            existing.isCaptain = isCap;
            existing.isVip = isVip;
            existing.avatar.setCustomLabel(existing.playerId, existing.displayName, 'remote', isCap, isVip);
          }
          existing.targetX = Number(rp.x) || 0;
          existing.targetY = Number(rp.y) || 0;
          existing.targetZ = Number(rp.z) || 0;
          existing.targetRotY = Number(rp.rotY) || 0;
          existing.state = rp.state || 'idle';
          const nextExpr = rp.expression || 'neutral';
          if (existing.expression !== nextExpr) {
            existing.expression = nextExpr;
            existing.avatar.setExpression(nextExpr);
          }
        }
      }

      for (const [uid, entry] of remoteAvatars.entries()) {
        if (!seenUids.has(uid)) {
          scene.remove(entry.avatar.group);
          remoteAvatars.delete(uid);
        }
      }
    },
    updateRemotePlayers(dt) {
      const lerpFactor = Math.min(1, dt * 12);
      for (const entry of remoteAvatars.values()) {
        const av = entry.avatar;
        av.x += (entry.targetX - av.x) * lerpFactor;
        av.z += (entry.targetZ - av.z) * lerpFactor;
        entry.curY += (entry.targetY - entry.curY) * lerpFactor;
        av.rotY += shortestAngleDiff(av.rotY, entry.targetRotY) * lerpFactor;
        av.syncTransform(entry.curY);
        const speed = entry.state === 'run' ? 6.8 : (entry.state === 'walk' ? 3.5 : 0);
        av.applyPose(entry.state, dt, speed);
      }
    },
    getRemotePositions() {
      const out = [];
      for (const entry of remoteAvatars.values()) {
        out.push({
          uid: entry.uid,
          playerId: entry.playerId,
          displayName: entry.displayName,
          characterId: entry.characterId,
          x: entry.avatar.x,
          z: entry.avatar.z,
          rotY: entry.avatar.rotY,
          state: entry.state,
          expression: entry.expression
        });
      }
      return out;
    },
    getNpcPositions() {
      return characters.map((c) => ({
        id: c.id,
        name: c.name,
        x: c.x,
        z: c.z,
        rotY: c.rotY,
        isUser: c.isUserControlled,
        visible: c.group.visible
      }));
    },
    updateNPCs(dt, furnitureManager = null) {
      if (!npcsEnabled) return;
      const isSeatedState = (c) =>
        c.npcState === 'sit' || c.npcState === 'bed_sit' || c.npcState === 'sleep';

      for (const c of characters) {
        if (c.isUserControlled) continue;

        c.npcTimer -= dt;

        if (isSeatedState(c)) {
          const t = c.occupiedTarget;
          if (!t || t.occupiedBy !== c.id || c.npcTimer <= 0) {
            releaseNpcOccupancy(c, furnitureManager);
            c.npcState = 'idle';
            c.setExpression('neutral');
            c.npcTimer = 1.8 + Math.random() * 3.0;
            c.npcTarget = null;
          } else {
            if (c.npcState === 'sleep') {
              c.x = t.sleepX;
              c.z = t.sleepZ;
              c.rotY = t.sleepRotY;
              c.syncTransform(t.sleepY - 0.45);
              c.applyPose('sleep', dt, 0);
            } else {
              c.x = t.x;
              c.z = t.z;
              c.rotY = t.charRotY;
              c.syncTransform(t.y - 0.44);
              c.applyPose(c.npcState, dt, 0);
            }
            continue;
          }
        }

        if (c.npcState === 'idle' || c.npcState === 'dance') {
          c.applyPose(c.npcState, dt, 0);
          c.syncTransform(0);
          if (c.npcTimer <= 0) {
            let pickedSeat = null;
            if (furnitureManager && Math.random() < 0.34) {
              const seats = furnitureManager.getAvailableNpcSeatsAndBeds();
              const nearbySeats = seats.filter((s) => {
                const d = Math.hypot(s.exitX - c.x, s.exitZ - c.z);
                return d < 14.5;
              });
              if (nearbySeats.length > 0) {
                pickedSeat = nearbySeats[Math.floor(Math.random() * nearbySeats.length)];
              }
            }

            if (pickedSeat) {
              c.setExpression('neutral');
              c.npcTarget = { x: pickedSeat.exitX, z: pickedSeat.exitZ, seatTarget: pickedSeat };
              c.npcState = 'walk_to_seat';
              c.npcTimer = 6.5;
              c.blockedTimer = 0;
            } else if (c.npcState === 'idle' && Math.random() < 0.18) {
              c.npcState = 'dance';
              c.setExpression('happy');
              c.npcTimer = 3.2 + Math.random() * 2.5;
              c.npcTarget = null;
            } else {
              c.setExpression(Math.random() < 0.25 ? 'happy' : 'neutral');
              const [tx, tz] = pickNearbyWaypoint(c.x, c.z);
              c.npcTarget = { x: tx, z: tz };
              c.npcState = Math.random() < 0.2 ? 'run' : 'walk';
              c.npcTimer = 4.0 + Math.random() * 5.0;
              c.blockedTimer = 0;
            }
          }
        } else {
          if (!c.npcTarget || c.npcTimer <= 0) {
            c.npcState = 'idle';
            c.npcTimer = 2.0 + Math.random() * 4.0;
            c.npcTarget = null;
            c.syncTransform(0);
          } else {
            const dx = c.npcTarget.x - c.x;
            const dz = c.npcTarget.z - c.z;
            const dist = Math.hypot(dx, dz);

            if (dist < (c.npcState === 'walk_to_seat' ? 0.72 : 0.45)) {
              if (c.npcState === 'walk_to_seat' && c.npcTarget.seatTarget && furnitureManager) {
                const target = c.npcTarget.seatTarget;
                const claim = furnitureManager.claimTarget(target.id, c.id);
                if (claim.ok) {
                  c.occupiedTarget = target;
                  c.npcTarget = null;
                  if (target.type === 'bed') {
                    c.npcState = Math.random() < 0.38 ? 'sleep' : 'bed_sit';
                  } else {
                    c.npcState = 'sit';
                  }
                  c.npcTimer = 6.0 + Math.random() * 8.0;
                  continue;
                }
              }
              c.npcState = 'idle';
              c.npcTimer = 2.5 + Math.random() * 4.5;
              c.npcTarget = null;
              c.syncTransform(0);
            } else {
              const speed = c.npcState === 'run' ? 3.8 : 1.75;
              const vx = (dx / dist) * speed;
              const vz = (dz / dist) * speed;
              const prevX = c.x;
              const prevZ = c.z;

              const [rx, rz] = resolveEnvironmentCollisions(c.x + vx * dt, c.z + vz * dt, colliders, c.radius);
              c.x = rx;
              c.z = rz;

              const moved = Math.hypot(rx - prevX, rz - prevZ);
              if (moved < speed * dt * 0.22) {
                c.blockedTimer += dt;
                if (c.blockedTimer > 0.9) {
                  c.npcState = 'idle';
                  c.npcTimer = 1.5 + Math.random() * 2.5;
                  c.npcTarget = null;
                  c.blockedTimer = 0;
                }
              } else {
                c.blockedTimer = 0;
              }

              const desiredRot = Math.atan2(-vx, -vz);
              c.rotY += shortestAngleDiff(c.rotY, desiredRot) * Math.min(1, dt * 8);
              c.applyPose(c.npcState === 'run' ? 'run' : 'walk', dt, speed);
              c.syncTransform(0);
            }
          }
        }
      }

      const minSep = 0.74;
      for (let i = 0; i < characters.length; i++) {
        const a = characters[i];
        const aFixed = a.isUserControlled || isSeatedState(a);
        for (let j = i + 1; j < characters.length; j++) {
          const b = characters[j];
          const bFixed = b.isUserControlled || isSeatedState(b);
          if (aFixed && bFixed) continue;

          const dx = b.x - a.x;
          const dz = b.z - a.z;
          const distSq = dx * dx + dz * dz;
          if (distSq < minSep * minSep && distSq > 1e-6) {
            const dist = Math.sqrt(distSq);
            const overlap = minSep - dist;
            const nx = dx / dist;
            const nz = dz / dist;

            const shareA = aFixed ? 0.0 : (bFixed ? 1.0 : 0.5);
            const shareB = 1.0 - shareA;

            if (shareA > 0) {
              const [ax, az] = resolveEnvironmentCollisions(a.x - nx * overlap * shareA, a.z - nz * overlap * shareA, colliders, a.radius);
              a.x = ax; a.z = az;
              a.syncTransform(0);
            }
            if (shareB > 0) {
              const [bx, bz] = resolveEnvironmentCollisions(b.x + nx * overlap * shareB, b.z + nz * overlap * shareB, colliders, b.radius);
              b.x = bx; b.z = bz;
              b.syncTransform(0);
            }
          }
        }
      }
    }
  };
}
