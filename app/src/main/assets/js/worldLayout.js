import * as THREE from '../vendor/three/three.module.min.js';
import { createRoomPlaqueTexture } from './textures.js';

export const HOUSE_BOUNDS = {
  minX: -27,
  maxX: 27,
  minZ: -22,
  maxZ: 22,
  wallHeight: 5.4,
  atriumHeight: 6.4
};

export const ROOM_ZONES = [
  { id: 'living', name: 'Central Living Atrium', x1: -10, z1: -8, x2: 10, z2: 8, color: '#eab954' },
  { id: 'dining', name: 'Dining Area', x1: -10, z1: -22, x2: 2, z2: -8, color: '#f59e0b' },
  { id: 'kitchen', name: 'Gourmet Kitchen', x1: -20, z1: -22, x2: -10, z2: -8, color: '#fb923c' },
  { id: 'store', name: 'Store Room', x1: -27, z1: -22, x2: -20, z2: -8, color: '#94a3b8' },
  { id: 'bedroom', name: '15-Player Bedroom', x1: -27, z1: -8, x2: -10, z2: 10, color: '#818cf8' },
  { id: 'bathroom', name: 'Spa Bathroom', x1: -27, z1: 10, x2: -10, z2: 22, color: '#38bdf8' },
  { id: 'pool', name: 'Indoor Swimming Pool', x1: -10, z1: 8, x2: 10, z2: 22, color: '#06b6d4' },
  { id: 'garden', name: 'Indoor Botanical Garden', x1: 10, z1: 10, x2: 27, z2: 22, color: '#34d399' },
  { id: 'activity', name: 'Activity & Challenge Room', x1: 10, z1: 0, x2: 27, z2: 10, color: '#f43f5e' },
  { id: 'admin', name: 'Admin Control Room', x1: 14, z1: -8, x2: 27, z2: 0, color: '#22d3ee', adminOnly: true },
  { id: 'confession', name: 'Confession Room', x1: 19, z1: -22, x2: 27, z2: -8, color: '#c084fc' },
  { id: 'captain', name: 'Captain VIP Room', x1: 10, z1: -22, x2: 19, z2: -14, color: '#facc15' },
  { id: 'smoking', name: 'Fictional Smoking Lounge', x1: 2, z1: -22, x2: 10, z2: -14, color: '#fb7185' },
  { id: 'gallery', name: 'VIP Wing Gallery', x1: 2, z1: -14, x2: 19, z2: -8, color: '#cbd5e1' },
  { id: 'east_alcove', name: 'East Wing Foyer', x1: 10, z1: -8, x2: 14, z2: 0, color: '#cbd5e1' }
];

export function getRoomAtPosition(x, z, isAdmin = false) {
  for (const r of ROOM_ZONES) {
    if (r.adminOnly && !isAdmin) continue;
    if (x >= r.x1 && x <= r.x2 && z >= r.z1 && z <= r.z2) {
      return r;
    }
  }
  if (!isAdmin && x >= 14 && x <= 27 && z >= -8 && z <= 0) {
    return ROOM_ZONES.find((r) => r.id === 'east_alcove') || ROOM_ZONES[0];
  }
  return ROOM_ZONES[0];
}

export function buildHouseArchitecture(scene, textures, colliders) {
  const group = new THREE.Group();
  const H = HOUSE_BOUNDS.wallHeight;
  const WT = 0.34; // Wall thickness

  const plasterMat = new THREE.MeshStandardMaterial({
    map: textures.plaster,
    color: 0xf4efe6,
    roughness: 0.78,
    metalness: 0.04
  });

  const darkWallMat = new THREE.MeshStandardMaterial({
    map: textures.plaster,
    color: 0x262d3d,
    roughness: 0.7,
    metalness: 0.1
  });

  const confessionWallMat = new THREE.MeshStandardMaterial({
    map: textures.acousticRib,
    color: 0x2a1f36,
    roughness: 0.85,
    metalness: 0.08
  });

  const trimGoldMat = new THREE.MeshStandardMaterial({
    color: 0xd4a94d,
    roughness: 0.28,
    metalness: 0.85
  });

  const baseboardMat = new THREE.MeshStandardMaterial({
    color: 0x222630,
    roughness: 0.55,
    metalness: 0.2
  });

  const windowGlassMat = new THREE.MeshStandardMaterial({
    color: 0x9bd7ea,
    transparent: true,
    opacity: 0.28,
    roughness: 0.08,
    metalness: 0.2,
    depthWrite: false
  });

  const ceilingMat = new THREE.MeshStandardMaterial({
    color: 0xefeae0,
    roughness: 0.85,
    metalness: 0.02
  });

  const skylightGlassMat = new THREE.MeshStandardMaterial({
    color: 0xdcf4ff,
    emissive: 0x7cb8e8,
    emissiveIntensity: 0.35,
    transparent: true,
    opacity: 0.55,
    roughness: 0.12
  });

  // Helper to add floor zone
  function addFloor(x1, z1, x2, z2, mat, y = 0) {
    const w = x2 - x1;
    const d = z2 - z1;
    const mesh = new THREE.Mesh(new THREE.PlaneGeometry(w, d), mat);
    mesh.rotation.x = -Math.PI / 2;
    mesh.position.set((x1 + x2) / 2, y, (z1 + z2) / 2);
    mesh.receiveShadow = true;
    group.add(mesh);
    return mesh;
  }

  // Distinct realistic materials per zone
  const marbleFloorMat = new THREE.MeshStandardMaterial({ map: textures.marble, roughness: 0.24, metalness: 0.08 });
  const oakFloorMat = new THREE.MeshStandardMaterial({ map: textures.wood, roughness: 0.48, metalness: 0.05 });
  const walnutFloorMat = new THREE.MeshStandardMaterial({ map: textures.walnut, roughness: 0.42, metalness: 0.08 });
  const stoneFloorMat = new THREE.MeshStandardMaterial({ map: textures.stone, roughness: 0.6, metalness: 0.04 });
  const slateFloorMat = new THREE.MeshStandardMaterial({ map: textures.slate, roughness: 0.52, metalness: 0.16 });
  const turfFloorMat = new THREE.MeshStandardMaterial({ map: textures.turf, roughness: 0.92, metalness: 0.0 });

  // Living + Gallery + Dining + East Alcove (Calacatta Marble)
  addFloor(-10, -8, 10, 8, marbleFloorMat);
  addFloor(-10, -22, 2, -8, marbleFloorMat);
  addFloor(2, -14, 19, -8, marbleFloorMat);
  addFloor(10, -8, 14, 0, marbleFloorMat);

  // Kitchen + Bedroom + Captain (Warm Oak Wood)
  addFloor(-20, -22, -10, -8, stoneFloorMat);
  addFloor(-27, -22, -20, -8, slateFloorMat); // Store room
  addFloor(-27, -8, -10, 10, oakFloorMat); // 15-player bedroom
  addFloor(10, -22, 19, -14, oakFloorMat); // Captain room

  // Smoking Lounge + Confession Room (Rich Dark Walnut)
  addFloor(2, -22, 10, -14, walnutFloorMat);
  addFloor(19, -22, 27, -8, walnutFloorMat);

  // Bathroom + Pool Deck (Travertine Stone)
  addFloor(-27, 10, -10, 22, stoneFloorMat);
  // Pool Deck surrounds recessed pool basin (pool basin is X: -6.5..6.5, Z: 10.5..19.5)
  addFloor(-10, 8, 10, 10.5, stoneFloorMat); // North deck
  addFloor(-10, 19.5, 10, 22, stoneFloorMat); // South deck
  addFloor(-10, 10.5, -6.5, 19.5, stoneFloorMat); // West deck
  addFloor(6.5, 10.5, 10, 19.5, stoneFloorMat); // East deck

  // Activity Room + Admin Control (Slate)
  addFloor(10, 0, 27, 10, slateFloorMat);
  addFloor(14, -8, 27, 0, slateFloorMat);

  // Indoor Garden (Botanical Turf + Stone border)
  addFloor(10, 10, 27, 22, turfFloorMat);

  // Helper to build wall segment + collider
  function addWall(x1, z1, x2, z2, height = H, mat = plasterMat, yBase = 0, addCollider = true) {
    const dx = x2 - x1;
    const dz = z2 - z1;
    const len = Math.hypot(dx, dz);
    if (len < 0.05) return;
    const cx = (x1 + x2) / 2;
    const cz = (z1 + z2) / 2;
    const horizontal = Math.abs(dx) >= Math.abs(dz);
    const sx = horizontal ? len : WT;
    const sz = horizontal ? WT : len;

    const wall = new THREE.Mesh(new THREE.BoxGeometry(sx, height, sz), mat);
    wall.position.set(cx, yBase + height / 2, cz);
    wall.castShadow = true;
    wall.receiveShadow = true;
    group.add(wall);

    if (yBase === 0) {
      const base = new THREE.Mesh(new THREE.BoxGeometry(sx + 0.03, 0.14, sz + 0.03), baseboardMat);
      base.position.set(cx, 0.07, cz);
      group.add(base);
    }

    if (addCollider) {
      colliders.push({
        minX: cx - sx / 2,
        maxX: cx + sx / 2,
        minZ: cz - sz / 2,
        maxZ: cz + sz / 2,
        tag: 'wall'
      });
    }
  }

  // Helper: Wall with centered or offset doorway cutout + transom header + optional plaque
  function addWallWithDoorway(x1, z1, x2, z2, doorCenter, doorWidth, doorHeight = 3.2, mat = plasterMat, plaqueTitle = '', plaqueAccent = '#eab954') {
    const horizontal = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
    const halfD = doorWidth / 2;

    if (horizontal) {
      const minX = Math.min(x1, x2);
      const maxX = Math.max(x1, x2);
      const z = z1;
      if (doorCenter - halfD > minX + 0.05) {
        addWall(minX, z, doorCenter - halfD, z, H, mat);
      }
      if (maxX > doorCenter + halfD + 0.05) {
        addWall(doorCenter + halfD, z, maxX, z, H, mat);
      }
      // Header above door
      addWall(doorCenter - halfD, z, doorCenter + halfD, z, H - doorHeight, mat, doorHeight, false);
      // Frame trim
      const frameTop = new THREE.Mesh(new THREE.BoxGeometry(doorWidth + 0.16, 0.12, WT + 0.08), trimGoldMat);
      frameTop.position.set(doorCenter, doorHeight + 0.04, z);
      group.add(frameTop);

      if (plaqueTitle) {
        const plaqueTex = createRoomPlaqueTexture(plaqueTitle, plaqueAccent);
        const plaqueMat = new THREE.MeshStandardMaterial({ map: plaqueTex, roughness: 0.35, metalness: 0.25 });
        const plaque = new THREE.Mesh(new THREE.BoxGeometry(1.75, 0.44, 0.06), plaqueMat);
        plaque.position.set(doorCenter, doorHeight + 0.55, z + 0.2);
        group.add(plaque);
        const plaqueBack = plaque.clone();
        plaqueBack.position.set(doorCenter, doorHeight + 0.55, z - 0.2);
        plaqueBack.rotation.y = Math.PI;
        group.add(plaqueBack);
      }
    } else {
      const minZ = Math.min(z1, z2);
      const maxZ = Math.max(z1, z2);
      const x = x1;
      if (doorCenter - halfD > minZ + 0.05) {
        addWall(x, minZ, x, doorCenter - halfD, H, mat);
      }
      if (maxZ > doorCenter + halfD + 0.05) {
        addWall(x, doorCenter + halfD, x, maxZ, H, mat);
      }
      addWall(x, doorCenter - halfD, x, doorCenter + halfD, H - doorHeight, mat, doorHeight, false);
      const frameTop = new THREE.Mesh(new THREE.BoxGeometry(WT + 0.08, 0.12, doorWidth + 0.16), trimGoldMat);
      frameTop.position.set(x, doorHeight + 0.04, doorCenter);
      group.add(frameTop);

      if (plaqueTitle) {
        const plaqueTex = createRoomPlaqueTexture(plaqueTitle, plaqueAccent);
        const plaqueMat = new THREE.MeshStandardMaterial({ map: plaqueTex, roughness: 0.35, metalness: 0.25 });
        const plaque = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.44, 1.75), plaqueMat);
        plaque.position.set(x + 0.2, doorHeight + 0.55, doorCenter);
        const plaqueBack = plaque.clone();
        plaqueBack.position.set(x - 0.2, doorHeight + 0.55, doorCenter);
        const holder = new THREE.Group();
        holder.add(plaque);
        holder.add(plaqueBack);
        group.add(holder);
        return holder;
      }
    }
    return null;
  }

  // Helper: Glass partition wall with doorway (used for Pool panoramic wall & Smoking lounge)
  function addGlassPartitionWithDoor(x1, z1, x2, z2, doorCenter, doorWidth, doorHeight = 3.2, plaqueTitle = '', plaqueAccent = '#06b6d4') {
    const horizontal = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
    const halfD = doorWidth / 2;
    const kneeH = 0.65;

    if (horizontal) {
      const minX = Math.min(x1, x2);
      const maxX = Math.max(x1, x2);
      const z = z1;
      const addGlassSide = (a, b) => {
        if (b - a < 0.1) return;
        addWall(a, z, b, z, kneeH, plasterMat, 0, true);
        const glass = new THREE.Mesh(new THREE.BoxGeometry(b - a, doorHeight - kneeH, 0.08), windowGlassMat);
        glass.position.set((a + b) / 2, kneeH + (doorHeight - kneeH) / 2, z);
        group.add(glass);
        addWall(a, z, b, z, H - doorHeight, plasterMat, doorHeight, false);
      };
      addGlassSide(minX, doorCenter - halfD);
      addGlassSide(doorCenter + halfD, maxX);
      addWall(doorCenter - halfD, z, doorCenter + halfD, z, H - doorHeight, plasterMat, doorHeight, false);
      if (plaqueTitle) {
        const plaqueTex = createRoomPlaqueTexture(plaqueTitle, plaqueAccent);
        const pMat = new THREE.MeshStandardMaterial({ map: plaqueTex, roughness: 0.35 });
        const plaque = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.46, 0.06), pMat);
        plaque.position.set(doorCenter, doorHeight + 0.55, z - 0.2);
        plaque.rotation.y = Math.PI;
        group.add(plaque);
        const plaqueF = plaque.clone();
        plaqueF.position.set(doorCenter, doorHeight + 0.55, z + 0.2);
        plaqueF.rotation.y = 0;
        group.add(plaqueF);
      }
    }
  }

  // ===== 1. SEALED OUTER PERIMETER WALLS (No outside world!) =====
  addWall(-27, -22, 27, -22, H, plasterMat);
  addWall(-27, 22, 27, 22, H, plasterMat);
  addWall(-27, -22, -27, 22, H, plasterMat);
  addWall(27, -22, 27, 22, H, plasterMat);

  // ===== 2. INTERIOR WALLS & DOORWAYS =====
  // Store Room <-> Kitchen (X = -20, Z: -22..-8)
  addWallWithDoorway(-20, -22, -20, -8, -15, 2.8, 3.0, plasterMat, 'Store Room', '#94a3b8');

  // Kitchen <-> Dining (X = -10, Z: -22..-8)
  addWallWithDoorway(-10, -22, -10, -8, -14.5, 3.4, 3.1, plasterMat, 'Kitchen', '#fb923c');

  // Kitchen/Store South Wall (Z = -8, X: -27..-10)
  addWall(-27, -8, -10, -8, H, plasterMat);

  // 15-Player Bedroom <-> Living Area (X = -10, Z: -8..8)
  addWallWithDoorway(-10, -8, -10, 8, 0, 4.0, 3.2, plasterMat, '15-Player Bedroom', '#818cf8');

  // Bedroom South Wall <-> Bathroom (Z = 10, X: -27..-10)
  addWallWithDoorway(-27, 10, -10, 10, -18, 2.8, 3.0, plasterMat, 'Spa Bathroom', '#38bdf8');

  // Corner connector X = -10, Z: 8..10
  addWall(-10, 8, -10, 10, H, plasterMat);

  // Bathroom <-> Pool Area (X = -10, Z: 10..22)
  addWallWithDoorway(-10, 10, -10, 22, 16, 2.8, 3.0, plasterMat, 'Pool Access', '#06b6d4');

  // Living Area <-> Dining Area (Z = -8, X: -10..2) -> Grand 6m open archway
  addWallWithDoorway(-10, -8, 2, -8, -4, 6.2, 3.8, plasterMat, 'Dining Hall', '#f59e0b');

  // Dining Area <-> Smoking Lounge (X = 2, Z: -22..-14)
  addWall(2, -22, 2, -14, H, darkWallMat);

  // Dining Area <-> VIP Gallery (X = 2, Z: -14..-8) -> Open walkthrough
  addWallWithDoorway(2, -14, 2, -8, -11, 4.2, 3.6, plasterMat, '', '#eab954');

  // Smoking Lounge <-> VIP Gallery (Z = -14, X: 2..10) -> Glass partition + sliding door
  addGlassPartitionWithDoor(2, -14, 10, -14, 6, 2.6, 3.1, 'Smoking Lounge', '#fb7185');

  // Smoking Lounge <-> Captain Room (X = 10, Z: -22..-14)
  addWall(10, -22, 10, -14, H, darkWallMat);

  // Captain Room <-> VIP Gallery (Z = -14, X: 10..19)
  addWallWithDoorway(10, -14, 19, -14, 14.5, 2.8, 3.1, plasterMat, 'Captain Room', '#facc15');

  // Confession Room <-> Captain/VIP Gallery (X = 19, Z: -22..-8) -> Doorway at Z = -11
  addWall(19, -22, 19, -14, H, confessionWallMat);
  addWallWithDoorway(19, -14, 19, -8, -11, 2.6, 3.0, confessionWallMat, 'Confession Room', '#c084fc');

  // Admin Control Room North Wall (Z = -8, X: 14..27):
  // Opaque privacy wall for PLAYER role; clear observation glass for authenticated ADMIN only.
  addWall(14, -8, 27, -8, 0.65, darkWallMat, 0, true);
  addWall(14, -8, 27, -8, H - 3.2, darkWallMat, 3.2, false);
  const adminObsGlass = new THREE.Mesh(new THREE.BoxGeometry(13, 2.55, 0.08), windowGlassMat);
  adminObsGlass.position.set(20.5, 0.65 + 2.55 / 2, -8);
  adminObsGlass.visible = false;
  group.add(adminObsGlass);
  const adminObsOpaque = new THREE.Mesh(new THREE.BoxGeometry(13, 2.55, WT), darkWallMat);
  adminObsOpaque.position.set(20.5, 0.65 + 2.55 / 2, -8);
  adminObsOpaque.castShadow = true;
  adminObsOpaque.receiveShadow = true;
  group.add(adminObsOpaque);

  // Admin Control Room <-> East Alcove (X = 14, Z: -8..0) — plaque hidden for normal players
  const adminPlaqueGroup = addWallWithDoorway(14, -8, 14, 0, -4, 2.8, 3.1, darkWallMat, 'Admin Control', '#22d3ee');
  if (adminPlaqueGroup) {
    adminPlaqueGroup.visible = false;
  }

  // Hard perimeter exclusion collider for Admin Control Room (active when role !== ADMIN)
  const adminLockCollider = {
    minX: 13.7,
    maxX: 27.0,
    minZ: -8.2,
    maxZ: 0.2,
    active: true,
    tag: 'admin_locked_zone'
  };
  colliders.push(adminLockCollider);

  // Hard perimeter exclusion collider for EXISTING Captain VIP Room (X: 10..19, Z: -22..-14)
  const vipLockCollider = {
    minX: 10.1,
    maxX: 18.9,
    minZ: -21.9,
    maxZ: -14.1,
    active: true,
    tag: 'vip_locked_zone'
  };
  colliders.push(vipLockCollider);

  // Activity Room North Wall (Z = 0, X: 10..27)
  addWall(10, 0, 27, 0, H, darkWallMat);

  // Living Area <-> Activity Room (X = 10, Z: 0..8)
  addWallWithDoorway(10, 0, 10, 8, 4, 3.6, 3.2, plasterMat, 'Activity Room', '#f43f5e');

  // Corner connector X = 10, Z: 8..10
  addWall(10, 8, 10, 10, H, plasterMat);

  // Living Area <-> Pool Area (Z = 8, X: -10..10) -> Panoramic glass wall + wide double sliding door!
  addGlassPartitionWithDoor(-10, 8, 10, 8, 0, 4.8, 3.4, 'Swimming Pool', '#06b6d4');

  // Pool Area <-> Indoor Garden (X = 10, Z: 10..22)
  addWallWithDoorway(10, 10, 10, 22, 16, 3.2, 3.2, plasterMat, 'Indoor Garden', '#34d399');

  // Activity Room <-> Indoor Garden (Z = 10, X: 10..27)
  addWallWithDoorway(10, 10, 27, 10, 18.5, 3.2, 3.2, plasterMat, 'Botanical Garden', '#34d399');

  // Decorative architectural columns around Living Area & Archways
  const colPositions = [
    [-9.4, -7.4], [9.4, -7.4], [-9.4, 7.4], [9.4, 7.4],
    [1.9, -7.4], [10.0, -7.8], [10.0, 0.0]
  ];
  for (const [cx, cz] of colPositions) {
    const col = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.3, H, 16), plasterMat);
    col.position.set(cx, H / 2, cz);
    col.castShadow = true;
    group.add(col);
    const ring = new THREE.Mesh(new THREE.CylinderGeometry(0.32, 0.32, 0.14, 16), trimGoldMat);
    ring.position.set(cx, 0.07, cz);
    group.add(ring);
    const topRing = ring.clone();
    topRing.position.y = H - 0.07;
    group.add(topRing);
  }

  // ===== 3. INDOOR CEILINGS & LUMINOUS SKYLIGHT ATRIUM =====
  // Raised coffered ceiling over entire 54x44 interior
  const outerCeiling = new THREE.Mesh(new THREE.PlaneGeometry(54, 44), ceilingMat);
  outerCeiling.rotation.x = Math.PI / 2;
  outerCeiling.position.set(0, H, 0);
  group.add(outerCeiling);

  // Atrium recessed glowing skylight frame over Center Tree (X: -6..6, Z: -5..5)
  const skylightFrame = new THREE.Mesh(new THREE.BoxGeometry(12.4, 0.35, 10.4), trimGoldMat);
  skylightFrame.position.set(0, H - 0.08, 0);
  group.add(skylightFrame);

  const livingSkylight = new THREE.Mesh(new THREE.PlaneGeometry(11.6, 9.6), skylightGlassMat);
  livingSkylight.rotation.x = Math.PI / 2;
  livingSkylight.position.set(0, H - 0.27, 0);
  group.add(livingSkylight);

  // Pool skylight strip (X: -7..7, Z: 11..19)
  const poolSkylight = new THREE.Mesh(new THREE.PlaneGeometry(13.5, 7.5), skylightGlassMat);
  poolSkylight.rotation.x = Math.PI / 2;
  poolSkylight.position.set(0, H - 0.04, 15);
  group.add(poolSkylight);

  // Garden skylight strip (X: 12..25, Z: 12..20)
  const gardenSkylight = new THREE.Mesh(new THREE.PlaneGeometry(12, 7.5), skylightGlassMat);
  gardenSkylight.rotation.x = Math.PI / 2;
  gardenSkylight.position.set(18.5, H - 0.04, 16);
  group.add(gardenSkylight);

  scene.add(group);

  // Glass sliding door specifications (wired by doors.js)
  const doorSpecs = [
    { id: 'door_store', label: 'Store Room Door', x: -20, z: -15, axis: 'z', width: 2.8, height: 3.0, doubleLeaf: false, tint: 0xaad4e6 },
    { id: 'door_kitchen', label: 'Kitchen Sliding Door', x: -10, z: -14.5, axis: 'z', width: 3.4, height: 3.1, doubleLeaf: true, tint: 0xbde4f4 },
    { id: 'door_bedroom', label: '15-Player Bedroom Door', x: -10, z: 0, axis: 'z', width: 4.0, height: 3.2, doubleLeaf: true, tint: 0xc7d2fe },
    { id: 'door_bath_bed', label: 'Spa Bathroom Door', x: -18, z: 10, axis: 'x', width: 2.8, height: 3.0, doubleLeaf: false, tint: 0xdbeafe, frosted: true },
    { id: 'door_bath_pool', label: 'Pool Access Door', x: -10, z: 16, axis: 'z', width: 2.8, height: 3.0, doubleLeaf: false, tint: 0xcffafe, frosted: true },
    { id: 'door_smoking', label: 'Smoking Lounge Door', x: 6, z: -14, axis: 'x', width: 2.6, height: 3.1, doubleLeaf: false, tint: 0x52363b },
    { id: 'door_captain', label: 'Captain VIP Door', x: 14.5, z: -14, axis: 'x', width: 2.8, height: 3.1, doubleLeaf: true, tint: 0xfde68a, requiredVip: true },
    { id: 'door_confession', label: 'Confession Room Door', x: 19, z: -11, axis: 'z', width: 2.6, height: 3.0, doubleLeaf: false, tint: 0x5b3d7a },
    { id: 'door_admin', label: 'Admin Control Door', x: 14, z: -4, axis: 'z', width: 2.8, height: 3.1, doubleLeaf: false, tint: 0x7dd3fc, requiredRole: 'ADMIN' },
    { id: 'door_activity', label: 'Activity Room Door', x: 10, z: 4, axis: 'z', width: 3.6, height: 3.2, doubleLeaf: true, tint: 0xfecdd3 },
    { id: 'door_pool', label: 'Swimming Pool Doors', x: 0, z: 8, axis: 'x', width: 4.8, height: 3.4, doubleLeaf: true, tint: 0xa5f3fc },
    { id: 'door_garden_pool', label: 'Garden Pool Door', x: 10, z: 16, axis: 'z', width: 3.2, height: 3.2, doubleLeaf: true, tint: 0xa7f3d0 },
    { id: 'door_garden_act', label: 'Garden Activity Door', x: 18.5, z: 10, axis: 'x', width: 3.2, height: 3.2, doubleLeaf: true, tint: 0xa7f3d0 }
  ];

  return {
    group,
    doorSpecs,
    skylightMats: [skylightGlassMat],
    setAdminAccessVisible(isAdmin) {
      const allowed = Boolean(isAdmin);
      adminObsGlass.visible = allowed;
      adminObsOpaque.visible = !allowed;
      if (adminPlaqueGroup) adminPlaqueGroup.visible = allowed;
      adminLockCollider.active = !allowed;
    },
    setVipAccessVisible(canAccessVip) {
      vipLockCollider.active = !Boolean(canAccessVip);
    }
  };
}
