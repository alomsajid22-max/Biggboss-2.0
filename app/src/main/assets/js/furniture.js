import * as THREE from '../vendor/three/three.module.min.js';
import { createBedNumberTexture, createMonitorWallTexture } from './textures.js';

export const POOL_BOUNDS = Object.freeze({
  basinMinX: -6.5,
  basinMaxX: 6.5,
  basinMinZ: 10.5,
  basinMaxZ: 19.5,
  swimMinX: -5.85,
  swimMaxX: 5.85,
  swimMinZ: 11.15,
  swimMaxZ: 18.85,
  waterY: -0.06
});

export function buildAllRoomFurniture(scene, textures, colliders, lighting = null) {
  const root = new THREE.Group();
  const interactables = [];
  const animatedObjects = {
    waterMesh: null,
    waterBasePositions: null,
    waterBaseY: POOL_BOUNDS.waterY,
    rippleRing: null,
    rippleActive: false,
    ripplePos: new THREE.Vector2(0, 15),
    serverLeds: [],
    stoveMats: [],
    cookingPots: [],
    steamRings: [],
    servingPlatters: [],
    isCookingActive: false,
    hasReadyMeals: false,
    aiOperatorRig: null,
    aiLeftArm: null,
    aiRightArm: null,
    aiHead: null,
    aiHaloMat: null,
    aiVisorMat: null,
    aiScreenMat: null,
    aiState: { enabled: true, mode: 'ASSIST', emergencyStopped: false },
    speakerMats: [],
    speakerPulseRemaining: 0,
    finaleGroup: null,
    finaleTrophy: null
  };

  const makeGlowMat = (colorHex, emissiveHex, intensity = 1.3) =>
    new THREE.MeshStandardMaterial({
      color: colorHex,
      emissive: emissiveHex,
      emissiveIntensity: intensity,
      roughness: 0.22
    });

  // Shared PBR Materials (optimized for mobile GPU batching)
  const mats = {
    gold: new THREE.MeshStandardMaterial({ color: 0xe2b14c, roughness: 0.25, metalness: 0.85 }),
    chrome: new THREE.MeshStandardMaterial({ color: 0xd8dee9, roughness: 0.18, metalness: 0.92 }),
    darkMetal: new THREE.MeshStandardMaterial({ color: 0x202531, roughness: 0.4, metalness: 0.72 }),
    oak: new THREE.MeshStandardMaterial({ map: textures.wood, color: 0x9c724c, roughness: 0.52 }),
    walnut: new THREE.MeshStandardMaterial({ map: textures.walnut, color: 0x4b3122, roughness: 0.45 }),
    marble: new THREE.MeshStandardMaterial({ map: textures.marble, color: 0xf6f3ed, roughness: 0.22, metalness: 0.05 }),
    creamVelvet: new THREE.MeshStandardMaterial({ color: 0xe6dfd3, roughness: 0.82 }),
    tealVelvet: new THREE.MeshStandardMaterial({ color: 0x1e6b6b, roughness: 0.8 }),
    crimsonVelvet: new THREE.MeshStandardMaterial({ color: 0x9f1239, roughness: 0.76 }),
    indigoFabric: new THREE.MeshStandardMaterial({ color: 0x3b4978, roughness: 0.82 }),
    cognacLeather: new THREE.MeshStandardMaterial({ color: 0x7c4327, roughness: 0.48, metalness: 0.08 }),
    whiteLinen: new THREE.MeshStandardMaterial({ color: 0xf4f5f7, roughness: 0.88 }),
    ceramic: new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.16, metalness: 0.06 }),
    poolTile: new THREE.MeshStandardMaterial({ map: textures.poolTile, roughness: 0.28 }),
    stone: new THREE.MeshStandardMaterial({ map: textures.stone, roughness: 0.65 }),
    leafDark: new THREE.MeshStandardMaterial({ color: 0x265c3a, roughness: 0.72 }),
    leafBright: new THREE.MeshStandardMaterial({ color: 0x3d8c57, roughness: 0.68 }),
    glassClear: new THREE.MeshStandardMaterial({
      color: 0xbfefff,
      transparent: true,
      opacity: 0.34,
      roughness: 0.08,
      metalness: 0.2,
      depthWrite: false
    }),
    livingGlow: makeGlowMat(0xffe4a0, 0xffc864, 1.35),
    diningGlow: makeGlowMat(0xffe4a0, 0xffbe55, 1.25),
    bedroomGlow: makeGlowMat(0xfff0c2, 0xffcb74, 1.2),
    bathGlow: makeGlowMat(0xe0f7ff, 0x38bdf8, 1.25),
    poolGlow: makeGlowMat(0x67e8f9, 0x06b6d4, 1.35),
    gardenGlow: makeGlowMat(0xd9f99d, 0x34d399, 1.15),
    activityGlow: makeGlowMat(0xfda4af, 0xf43f5e, 1.35),
    confessionGlow: makeGlowMat(0xf5d0fe, 0xd946ef, 1.35),
    captainGlow: makeGlowMat(0xfef08a, 0xfacc15, 1.3),
    smokingGlow: makeGlowMat(0xfed7aa, 0xf97316, 1.25),
    cyanGlow: makeGlowMat(0x67e8f9, 0x06b6d4, 1.35)
  };

  if (lighting) {
    lighting.registerLightFixture('living', mats.livingGlow, 1.35, 0.04);
    lighting.registerLightFixture('dining', mats.diningGlow, 1.25, 0.04);
    lighting.registerLightFixture('bedroom', mats.bedroomGlow, 1.2, 0.04);
    lighting.registerLightFixture('bathroom', mats.bathGlow, 1.25, 0.04);
    lighting.registerLightFixture('pool', mats.poolGlow, 1.35, 0.04);
    lighting.registerLightFixture('garden', mats.gardenGlow, 1.15, 0.04);
    lighting.registerLightFixture('activity', mats.activityGlow, 1.35, 0.04);
    lighting.registerLightFixture('confession', mats.confessionGlow, 1.35, 0.04);
    lighting.registerLightFixture('captain', mats.captainGlow, 1.3, 0.04);
    lighting.registerLightFixture('smoking', mats.smokingGlow, 1.25, 0.04);
  }

  function addBoxCollider(cx, cz, sx, sz, tag = 'furniture') {
    const c = {
      minX: cx - sx / 2,
      maxX: cx + sx / 2,
      minZ: cz - sz / 2,
      maxZ: cz + sz / 2,
      active: true,
      tag
    };
    colliders.push(c);
    return c;
  }

  /**
   * Registers an interactive seat, bed, switch, or console target.
   * charRotY aligns the character's -Z front properly with the furniture.
   */
  function registerInteractable({
    id,
    label,
    x,
    y,
    z,
    charRotY = 0,
    exitX = x,
    exitZ = z,
    sleepX = x,
    sleepY = y,
    sleepZ = z,
    sleepRotY = charRotY,
    type = 'sit',
    roomId = null,
    requiredRole = null,
    requiredVip = false,
    radius = 2.4
  }) {
    interactables.push({
      id,
      label,
      x,
      y,
      z,
      charRotY,
      exitX,
      exitZ,
      sleepX,
      sleepY,
      sleepZ,
      sleepRotY,
      type,
      roomId,
      requiredRole,
      requiredVip: Boolean(requiredVip),
      radius,
      occupiedBy: null
    });
  }

  // Reusable helpers
  function createPottedPlant(x, z, scale = 1) {
    const g = new THREE.Group();
    g.position.set(x, 0, z);
    const pot = new THREE.Mesh(new THREE.CylinderGeometry(0.32 * scale, 0.24 * scale, 0.55 * scale, 12), mats.stone);
    pot.position.y = 0.275 * scale;
    pot.castShadow = true;
    g.add(pot);

    const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.05 * scale, 0.07 * scale, 0.9 * scale, 8), mats.walnut);
    stem.position.y = 0.85 * scale;
    g.add(stem);

    for (let i = 0; i < 6; i++) {
      const a = (i * Math.PI * 2) / 6;
      const leaf = new THREE.Mesh(new THREE.ConeGeometry(0.24 * scale, 0.72 * scale, 6), i % 2 === 0 ? mats.leafDark : mats.leafBright);
      leaf.position.set(Math.cos(a) * 0.28 * scale, 1.25 * scale, Math.sin(a) * 0.28 * scale);
      leaf.rotation.z = -Math.cos(a) * 0.65;
      leaf.rotation.x = Math.sin(a) * 0.65;
      g.add(leaf);
    }
    root.add(g);
    addBoxCollider(x, z, 0.62 * scale, 0.62 * scale);
  }

  function createArmchair(x, z, rotY, fabricMat, label = 'Armchair', idPrefix = 'chair', targetGroup = root, requiredRole = null, customExit = null, registerInteract = true, requiredVip = false) {
    const g = new THREE.Group();
    g.position.set(x, 0, z);
    g.rotation.y = rotY;

    const base = new THREE.Mesh(new THREE.BoxGeometry(0.92, 0.22, 0.88), mats.walnut);
    base.position.y = 0.18;
    base.castShadow = true;
    g.add(base);

    const seat = new THREE.Mesh(new THREE.BoxGeometry(0.84, 0.2, 0.8), fabricMat);
    seat.position.y = 0.36;
    seat.castShadow = true;
    g.add(seat);

    const back = new THREE.Mesh(new THREE.BoxGeometry(0.86, 0.58, 0.2), fabricMat);
    back.position.set(0, 0.68, -0.34);
    back.castShadow = true;
    g.add(back);

    const armGeo = new THREE.BoxGeometry(0.14, 0.38, 0.78);
    const armL = new THREE.Mesh(armGeo, fabricMat);
    armL.position.set(-0.4, 0.52, -0.02);
    g.add(armL);
    const armR = new THREE.Mesh(armGeo, fabricMat);
    armR.position.set(0.4, 0.52, -0.02);
    g.add(armR);

    targetGroup.add(g);
    addBoxCollider(x, z, 0.96, 0.96);

    // Furniture open front is local +Z -> world (+sin(rotY), +cos(rotY))
    // Character -Z front matches +Z when charRotY = rotY + PI
    if (registerInteract) {
      const ex = customExit ? customExit.x : x + Math.sin(rotY) * 1.08;
      const ez = customExit ? customExit.z : z + Math.cos(rotY) * 1.08;

      registerInteractable({
        id: `${idPrefix}_${Math.round(x * 10)}_${Math.round(z * 10)}`,
        label,
        x,
        y: 0.45,
        z,
        charRotY: rotY + Math.PI,
        exitX: ex,
        exitZ: ez,
        type: 'sit',
        requiredRole,
        requiredVip
      });
    }
  }

  function createCeilingFan(x, z, roomId, targetGroup = root) {
    const fan = new THREE.Group();
    fan.position.set(x, 5.25, z);

    const canopy = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.18, 0.14, 12), mats.darkMetal);
    canopy.position.y = 0.08;
    fan.add(canopy);

    const rod = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, 0.36, 8), mats.gold);
    rod.position.y = -0.14;
    fan.add(rod);

    const rotor = new THREE.Group();
    rotor.position.y = -0.32;

    const motor = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.22, 0.16, 16), mats.darkMetal);
    rotor.add(motor);

    const trimRing = new THREE.Mesh(new THREE.CylinderGeometry(0.25, 0.25, 0.04, 16), mats.gold);
    trimRing.position.y = 0.04;
    rotor.add(trimRing);

    const hubLight = new THREE.Mesh(
      new THREE.CylinderGeometry(0.14, 0.1, 0.07, 14),
      mats[`${roomId}Glow`] || mats.livingGlow
    );
    hubLight.position.y = -0.1;
    rotor.add(hubLight);

    const bladeGeo = new THREE.BoxGeometry(1.12, 0.025, 0.18);
    for (let i = 0; i < 4; i++) {
      const bladeWrap = new THREE.Group();
      bladeWrap.rotation.y = (i * Math.PI) / 2;
      const blade = new THREE.Mesh(bladeGeo, mats.walnut);
      blade.position.set(0.68, 0, 0);
      blade.rotation.x = 0.14;
      bladeWrap.add(blade);
      rotor.add(bladeWrap);
    }

    fan.add(rotor);
    targetGroup.add(fan);
    if (lighting) {
      lighting.registerFan(roomId, rotor, 8.8);
    }
    return rotor;
  }

  function createWallSwitch(x, z, rotY, roomId, kind, label, targetGroup = root, requiredRole = null) {
    const sw = new THREE.Group();
    sw.position.set(x, 1.32, z);
    sw.rotation.y = rotY;

    const plate = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.3, 0.04), mats.darkMetal);
    sw.add(plate);

    const bezel = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.24, 0.045), mats.chrome);
    sw.add(bezel);

    const rocker = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.12, 0.055), mats.whiteLinen);
    rocker.position.y = -0.02;
    sw.add(rocker);

    const ledMat = new THREE.MeshStandardMaterial({
      color: kind === 'fan' ? 0x38bdf8 : 0xfacc15,
      emissive: kind === 'fan' ? 0x38bdf8 : 0xfacc15,
      emissiveIntensity: 1.4,
      roughness: 0.25
    });
    const led = new THREE.Mesh(new THREE.BoxGeometry(0.09, 0.026, 0.06), ledMat);
    led.position.y = 0.08;
    sw.add(led);

    targetGroup.add(sw);

    if (lighting) {
      if (kind === 'fan') {
        lighting.registerFanSwitchLed(roomId, ledMat);
      } else {
        lighting.registerLightSwitchLed(roomId, ledMat);
      }
    }

    // Trigger centered slightly in front of the wall plate (+Z in switch local space)
    const triggerX = x + Math.sin(rotY) * 0.65;
    const triggerZ = z + Math.cos(rotY) * 0.65;

    registerInteractable({
      id: `${kind}_switch_${roomId}`,
      label,
      x: triggerX,
      y: 1.32,
      z: triggerZ,
      charRotY: rotY,
      type: kind === 'fan' ? 'fan_switch' : 'light_switch',
      roomId,
      requiredRole,
      radius: 2.3
    });
  }

  // =========================================================
  // 1. CENTRAL LIVING AREA (Curved Sofas, Tables, Rug, Lights)
  // =========================================================
  const rugMesh = new THREE.Mesh(
    new THREE.CylinderGeometry(6.3, 6.3, 0.03, 48),
    new THREE.MeshStandardMaterial({ map: textures.rug, color: 0xe2dbd0, roughness: 0.95 })
  );
  rugMesh.position.set(0, 0.015, 0);
  rugMesh.receiveShadow = true;
  root.add(rugMesh);

  // Two large curved modular sofas orbiting the Center Tree at radius 4.5m!
  // Each curved sofa provides 3 distinct seats so multiple housemates can sit cleanly.
  function buildCurvedSofa(startAngle, endAngle, radius, segments, fabricMat, label, idPrefix) {
    const sofaGroup = new THREE.Group();
    const step = (endAngle - startAngle) / segments;
    for (let i = 0; i < segments; i++) {
      const a = startAngle + step * (i + 0.5);
      const sx = Math.cos(a) * radius;
      const sz = Math.sin(a) * radius;
      const segLen = Math.abs(step) * radius * 1.04;
      const seg = new THREE.Group();
      seg.position.set(sx, 0, sz);
      // Open front (+Z) faces inward toward Center Tree (0,0)
      seg.rotation.y = Math.atan2(-sx, -sz);

      const plinth = new THREE.Mesh(new THREE.BoxGeometry(segLen, 0.18, 1.05), mats.walnut);
      plinth.position.y = 0.09;
      plinth.castShadow = true;
      seg.add(plinth);

      const cushion = new THREE.Mesh(new THREE.BoxGeometry(segLen * 0.98, 0.24, 0.96), fabricMat);
      cushion.position.y = 0.3;
      cushion.castShadow = true;
      seg.add(cushion);

      const backrest = new THREE.Mesh(new THREE.BoxGeometry(segLen, 0.56, 0.24), fabricMat);
      backrest.position.set(0, 0.62, -0.4);
      backrest.castShadow = true;
      seg.add(backrest);

      if (i % 2 === 0) {
        const pillow = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.32, 0.14), i % 4 === 0 ? mats.gold : mats.tealVelvet);
        pillow.position.set(0, 0.54, -0.22);
        pillow.rotation.x = -0.22;
        seg.add(pillow);
      }

      sofaGroup.add(seg);
      addBoxCollider(sx, sz, 1.15, 1.15);
    }
    root.add(sofaGroup);

    const midA = (startAngle + endAngle) / 2;
    const seatAngles = [midA - 0.38, midA, midA + 0.38];
    seatAngles.forEach((sa, sIdx) => {
      const mx = Math.cos(sa) * radius;
      const mz = Math.sin(sa) * radius;
      const sofaRotY = Math.atan2(-mx, -mz);
      // Exit clear of the coffee tables at (±3.05, 0) and curved sofa segments
      let ex = Math.cos(sa) * (radius - 1.22);
      let ez = Math.sin(sa) * (radius - 1.22);
      if (Math.abs(ez) < 1.1) {
        ex = mx > 0 ? 2.5 : -2.5;
        ez = 1.95;
      }
      registerInteractable({
        id: `${idPrefix}_${sIdx + 1}`,
        label,
        x: mx,
        y: 0.43,
        z: mz,
        charRotY: sofaRotY + Math.PI,
        exitX: ex,
        exitZ: ez,
        type: 'sit'
      });
    });
  }

  buildCurvedSofa(Math.PI * 0.66, Math.PI * 1.34, 4.5, 6, mats.creamVelvet, 'West Curved Sofa', 'sofa_west');
  buildCurvedSofa(-Math.PI * 0.34, Math.PI * 0.34, 4.5, 6, mats.creamVelvet, 'East Curved Sofa', 'sofa_east');

  // Curved marble & brass center coffee tables inside sofa arcs
  [[-3.05, 0], [3.05, 0]].forEach(([tx, tz]) => {
    const tbl = new THREE.Group();
    tbl.position.set(tx, 0, tz);
    const top = new THREE.Mesh(new THREE.CylinderGeometry(0.75, 0.75, 0.08, 24), mats.marble);
    top.position.y = 0.42;
    top.castShadow = true;
    tbl.add(top);
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.38, 0.38, 16), mats.gold);
    leg.position.y = 0.19;
    tbl.add(leg);
    const bowl = new THREE.Mesh(new THREE.SphereGeometry(0.16, 12, 8, 0, Math.PI * 2, Math.PI * 0.45, Math.PI * 0.55), mats.gold);
    bowl.position.set(0.1, 0.5, 0);
    tbl.add(bowl);
    root.add(tbl);
    addBoxCollider(tx, tz, 1.5, 1.5);
  });

  // Living Area corner accent armchairs
  createArmchair(-7.5, -5.2, Math.PI * 0.25, mats.tealVelvet, 'Atrium Armchair', 'living_chair_nw');
  createArmchair(7.5, -5.2, -Math.PI * 0.25, mats.tealVelvet, 'Atrium Armchair', 'living_chair_ne');
  createArmchair(-7.5, 5.2, Math.PI * 0.75, mats.tealVelvet, 'Atrium Armchair', 'living_chair_sw');
  createArmchair(7.5, 5.2, -Math.PI * 0.75, mats.tealVelvet, 'Atrium Armchair', 'living_chair_se');

  // Living room ring chandelier above skylight perimeter
  const chandelier = new THREE.Group();
  chandelier.position.set(0, 5.1, 0);
  const chRing = new THREE.Mesh(new THREE.TorusGeometry(3.4, 0.07, 10, 40), mats.gold);
  chRing.rotation.x = Math.PI / 2;
  chandelier.add(chRing);
  const chLightRing = new THREE.Mesh(new THREE.TorusGeometry(3.38, 0.04, 8, 40), mats.livingGlow);
  chLightRing.rotation.x = Math.PI / 2;
  chLightRing.position.y = -0.04;
  chandelier.add(chLightRing);
  root.add(chandelier);

  // Living Atrium Light Switch (West wall beside Bedroom entrance)
  createWallSwitch(-9.8, -3.2, Math.PI / 2, 'living', 'light', 'Living Atrium Lights');

  // =========================================================
  // 2. DINING AREA (X: -10..2, Z: -22..-8)
  // =========================================================
  const diningTable = new THREE.Group();
  diningTable.position.set(-4, 0, -15);
  const dtTop = new THREE.Mesh(new THREE.BoxGeometry(2.0, 0.1, 5.6), mats.marble);
  dtTop.position.y = 0.78;
  dtTop.castShadow = true;
  diningTable.add(dtTop);
  [-1.9, 1.9].forEach((lz) => {
    const ped = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.34, 0.74, 14), mats.gold);
    ped.position.set(0, 0.37, lz);
    diningTable.add(ped);
  });
  const vase = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.14, 0.42, 12), mats.gold);
  vase.position.set(0, 1.04, 0);
  diningTable.add(vase);
  const floral = new THREE.Mesh(new THREE.DodecahedronGeometry(0.28, 0), mats.leafBright);
  floral.position.set(0, 1.32, 0);
  diningTable.add(floral);
  root.add(diningTable);
  addBoxCollider(-4, -15, 2.05, 5.65);

  function createDiningChair(x, z, rotY, idx, exitX, exitZ) {
    const ch = new THREE.Group();
    ch.position.set(x, 0, z);
    ch.rotation.y = rotY;
    const seat = new THREE.Mesh(new THREE.BoxGeometry(0.52, 0.08, 0.52), mats.creamVelvet);
    seat.position.y = 0.46;
    ch.add(seat);
    const back = new THREE.Mesh(new THREE.BoxGeometry(0.52, 0.52, 0.08), mats.creamVelvet);
    back.position.set(0, 0.74, -0.22);
    ch.add(back);
    const legGeo = new THREE.CylinderGeometry(0.025, 0.02, 0.45, 6);
    [[-0.2, -0.2], [0.2, -0.2], [-0.2, 0.2], [0.2, 0.2]].forEach(([lx, lz]) => {
      const l = new THREE.Mesh(legGeo, mats.gold);
      l.position.set(lx, 0.225, lz);
      ch.add(l);
    });
    root.add(ch);
    addBoxCollider(x, z, 0.56, 0.56);
    registerInteractable({
      id: `dining_chair_${idx}`,
      label: 'Dining Chair',
      x,
      y: 0.46,
      z,
      charRotY: rotY + Math.PI,
      exitX,
      exitZ,
      type: 'sit'
    });
  }

  let dIdx = 1;
  [-17.0, -15.7, -14.3, -13.0].forEach((dz) => {
    createDiningChair(-5.45, dz, Math.PI / 2, dIdx++, -6.55, dz);
    createDiningChair(-2.55, dz, -Math.PI / 2, dIdx++, -1.45, dz);
  });
  createDiningChair(-4, -18.25, 0, dIdx++, -4, -19.35);
  createDiningChair(-4, -11.75, Math.PI, dIdx++, -4, -10.65);

  const buffet = new THREE.Mesh(new THREE.BoxGeometry(4.6, 0.9, 0.75), mats.walnut);
  buffet.position.set(-4, 0.45, -21.3);
  buffet.castShadow = true;
  root.add(buffet);
  addBoxCollider(-4, -21.3, 4.6, 0.8);

  // Dining Chandelier Pendant + Ceiling Fan + Light/Fan Switches
  createCeilingFan(-4.0, -15.0, 'dining');
  createWallSwitch(-9.8, -11.6, Math.PI / 2, 'dining', 'light', 'Dining & Kitchen Lights');
  createWallSwitch(-9.8, -17.6, Math.PI / 2, 'dining', 'fan', 'Dining Ceiling Fan');

  // =========================================================
  // 3. GOURMET KITCHEN (X: -20..-10, Z: -22..-8)
  // =========================================================
  const island = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.96, 4.4), mats.marble);
  island.position.set(-14.2, 0.48, -14.8);
  island.castShadow = true;
  root.add(island);
  addBoxCollider(-14.2, -14.8, 1.45, 4.45);

  [-16.4, -15.3, -14.2, -13.1].forEach((sz, idx) => {
    const stool = new THREE.Group();
    stool.position.set(-12.95, 0, sz);
    const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.16, 0.68, 10), mats.darkMetal);
    stem.position.y = 0.34;
    stool.add(stem);
    const pad = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.22, 0.1, 12), mats.cognacLeather);
    pad.position.y = 0.72;
    stool.add(pad);
    root.add(stool);
    addBoxCollider(-12.95, sz, 0.52, 0.52);
    registerInteractable({
      id: `barstool_${idx}`,
      label: 'Kitchen Barstool',
      x: -12.95,
      y: 0.72,
      z: sz,
      charRotY: Math.PI / 2, // Face -X toward the kitchen island
      exitX: -11.95,
      exitZ: sz,
      type: 'sit'
    });
  });

  const northCounter = new THREE.Mesh(new THREE.BoxGeometry(7.2, 0.92, 0.9), mats.walnut);
  northCounter.position.set(-15.5, 0.46, -21.2);
  root.add(northCounter);
  const counterTop = new THREE.Mesh(new THREE.BoxGeometry(7.25, 0.08, 0.95), mats.marble);
  counterTop.position.set(-15.5, 0.94, -21.2);
  root.add(counterTop);
  addBoxCollider(-15.5, -21.2, 7.25, 0.96);

  const hood = new THREE.Mesh(new THREE.ConeGeometry(0.75, 1.1, 4), mats.chrome);
  hood.rotation.y = Math.PI / 4;
  hood.position.set(-15.5, 2.75, -21.15);
  root.add(hood);

  const fridge = new THREE.Mesh(new THREE.BoxGeometry(1.4, 2.4, 0.95), mats.chrome);
  fridge.position.set(-11.2, 1.2, -21.15);
  fridge.castShadow = true;
  root.add(fridge);
  addBoxCollider(-11.2, -21.15, 1.45, 1.0);

  // Fridge LED dispenser accent & chrome handle
  const fridgeLed = new THREE.Mesh(new THREE.BoxGeometry(0.32, 0.42, 0.04), mats.cyanGlow);
  fridgeLed.position.set(-11.45, 1.38, -20.66);
  root.add(fridgeLed);
  const fridgeHandle = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.9, 0.06), mats.darkMetal);
  fridgeHandle.position.set(-10.85, 1.25, -20.64);
  root.add(fridgeHandle);

  // Twin Induction Stoves + Virtual Pots + Steam Rings (northCounter X: -14.6 and -16.4)
  [-14.6, -16.4].forEach((sx, idx) => {
    const cooktop = new THREE.Mesh(new THREE.BoxGeometry(1.05, 0.04, 0.72), mats.darkMetal);
    cooktop.position.set(sx, 0.99, -21.15);
    root.add(cooktop);

    const burnerMat = new THREE.MeshStandardMaterial({
      color: 0xfb923c,
      emissive: 0xea580c,
      emissiveIntensity: 0.15,
      roughness: 0.35
    });
    animatedObjects.stoveMats.push(burnerMat);
    const burner = new THREE.Mesh(new THREE.TorusGeometry(0.18, 0.03, 8, 20), burnerMat);
    burner.rotation.x = Math.PI / 2;
    burner.position.set(sx, 1.015, -21.15);
    root.add(burner);

    const pot = new THREE.Group();
    pot.position.set(sx, 1.12, -21.15);
    const potBody = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.18, 0.18, 16), mats.chrome);
    pot.add(potBody);
    const potLid = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.2, 0.04, 16), mats.gold);
    potLid.position.y = 0.1;
    pot.add(potLid);
    root.add(pot);
    animatedObjects.cookingPots.push({ mesh: pot, baseY: 1.12, phase: idx * 1.4 });

    const steamMat = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      emissive: 0xfef3c7,
      emissiveIntensity: 0.4,
      transparent: true,
      opacity: 0.0
    });
    const steam = new THREE.Mesh(new THREE.TorusGeometry(0.14, 0.03, 6, 16), steamMat);
    steam.rotation.x = Math.PI / 2;
    steam.position.set(sx, 1.38, -21.15);
    steam.visible = false;
    root.add(steam);
    animatedObjects.steamRings.push({ mesh: steam, mat: steamMat, baseY: 1.35, phase: idx * 0.5 });
  });

  // Kitchen Double-Basin Sink & Chrome Faucet (X: -18.2, Z: -21.15)
  const sinkBasin = new THREE.Mesh(new THREE.BoxGeometry(1.1, 0.06, 0.62), mats.darkMetal);
  sinkBasin.position.set(-18.2, 0.97, -21.15);
  root.add(sinkBasin);
  const faucetStem = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.035, 0.34, 10), mats.chrome);
  faucetStem.position.set(-18.2, 1.15, -21.38);
  root.add(faucetStem);
  const faucetSpout = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.04, 0.22), mats.chrome);
  faucetSpout.position.set(-18.2, 1.3, -21.28);
  root.add(faucetSpout);

  // Island Cutting Board & Shared Serving Platter (X: -14.2, Z: -14.8)
  const prepBoard = new THREE.Mesh(new THREE.BoxGeometry(0.75, 0.04, 0.55), mats.oak);
  prepBoard.position.set(-14.2, 0.98, -15.6);
  root.add(prepBoard);
  const islandPlatter = new THREE.Group();
  islandPlatter.position.set(-14.2, 1.0, -14.0);
  islandPlatter.add(new THREE.Mesh(new THREE.CylinderGeometry(0.32, 0.28, 0.04, 18), mats.ceramic));
  const islandFood = new THREE.Mesh(new THREE.SphereGeometry(0.18, 12, 10), mats.gold);
  islandFood.scale.set(1.2, 0.65, 1.0);
  islandFood.position.y = 0.08;
  islandPlatter.add(islandFood);
  islandPlatter.visible = false;
  root.add(islandPlatter);
  animatedObjects.servingPlatters.push(islandPlatter);

  // Dining Table Shared Meal Platter (X: -4.0, Z: -15.0)
  const diningPlatter = new THREE.Group();
  diningPlatter.position.set(-4.0, 0.84, -15.0);
  diningPlatter.add(new THREE.Mesh(new THREE.CylinderGeometry(0.45, 0.38, 0.05, 20), mats.ceramic));
  const diningDish = new THREE.Mesh(new THREE.SphereGeometry(0.24, 14, 12), mats.gold);
  diningDish.scale.set(1.3, 0.65, 1.1);
  diningDish.position.y = 0.1;
  diningPlatter.add(diningDish);
  diningPlatter.visible = false;
  root.add(diningPlatter);
  animatedObjects.servingPlatters.push(diningPlatter);

  // Register Phase 7 Kitchen Interactables
  registerInteractable({
    id: 'kitchen_fridge',
    label: 'Ration Refrigerator',
    kitchenAction: 'pantry',
    x: -11.2,
    y: 0,
    z: -20.1,
    charRotY: 0,
    exitX: -11.2,
    exitZ: -19.8,
    type: 'kitchen',
    radius: 2.35
  });
  registerInteractable({
    id: 'kitchen_stove_1',
    label: 'Cooking Stove #1',
    kitchenAction: 'cook',
    x: -14.6,
    y: 0,
    z: -20.1,
    charRotY: 0,
    exitX: -14.6,
    exitZ: -19.8,
    type: 'kitchen',
    radius: 2.2
  });
  registerInteractable({
    id: 'kitchen_stove_2',
    label: 'Cooking Stove #2',
    kitchenAction: 'cook',
    x: -16.4,
    y: 0,
    z: -20.1,
    charRotY: 0,
    exitX: -16.4,
    exitZ: -19.8,
    type: 'kitchen',
    radius: 2.2
  });
  registerInteractable({
    id: 'kitchen_sink',
    label: 'Kitchen Sink (Wash)',
    kitchenAction: 'wash',
    x: -18.2,
    y: 0,
    z: -20.1,
    charRotY: 0,
    exitX: -18.2,
    exitZ: -19.8,
    type: 'kitchen',
    radius: 2.2
  });
  registerInteractable({
    id: 'kitchen_prep',
    label: 'Kitchen Island Station',
    kitchenAction: 'prep',
    x: -15.4,
    y: 0,
    z: -14.8,
    charRotY: -Math.PI / 2,
    exitX: -15.5,
    exitZ: -14.8,
    type: 'kitchen',
    radius: 2.3
  });
  registerInteractable({
    id: 'kitchen_dining_serve',
    label: 'Dining Table Meals',
    kitchenAction: 'eat',
    x: -4.0,
    y: 0,
    z: -15.0,
    charRotY: 0,
    exitX: -4.0,
    exitZ: -11.0,
    type: 'kitchen',
    radius: 3.35
  });

  // =========================================================
  // 4. STORE ROOM (X: -27..-20, Z: -22..-8)
  // =========================================================
  function createStorageRack(x, z, rotY) {
    const rack = new THREE.Group();
    rack.position.set(x, 0, z);
    rack.rotation.y = rotY;
    const postGeo = new THREE.BoxGeometry(0.08, 2.6, 0.08);
    [[-1.15, -0.35], [1.15, -0.35], [-1.15, 0.35], [1.15, 0.35]].forEach(([px, pz]) => {
      const p = new THREE.Mesh(postGeo, mats.darkMetal);
      p.position.set(px, 1.3, pz);
      rack.add(p);
    });

    const crateMat = new THREE.MeshStandardMaterial({ color: 0xc2935b, roughness: 0.78 });
    [0.45, 1.25, 2.05].forEach((sy, sIdx) => {
      const shelf = new THREE.Mesh(new THREE.BoxGeometry(2.38, 0.06, 0.78), mats.oak);
      shelf.position.y = sy;
      rack.add(shelf);
      for (let c = -1; c <= 1; c++) {
        const crate = new THREE.Mesh(
          new THREE.BoxGeometry(0.58, 0.42, 0.56),
          (c + sIdx) % 2 === 0 ? crateMat : mats.whiteLinen
        );
        crate.position.set(c * 0.72, sy + 0.24, 0);
        rack.add(crate);
      }
    });

    root.add(rack);
    const sx = Math.abs(Math.cos(rotY)) > 0.5 ? 2.4 : 0.82;
    const sz = Math.abs(Math.cos(rotY)) > 0.5 ? 0.82 : 2.4;
    addBoxCollider(x, z, sx, sz);
  }

  createStorageRack(-23.5, -21.2, 0);
  createStorageRack(-26.2, -16.5, Math.PI / 2);
  createStorageRack(-26.2, -12.5, Math.PI / 2);
  createStorageRack(-23.5, -8.8, 0);

  // =========================================================
  // 5. 15-PLAYER BEDROOM (X: -27..-10, Z: -8..10)
  // =========================================================
  const bedHeadGeo = new THREE.BoxGeometry(1.16, 1.05, 0.14);
  const bedFrameGeo = new THREE.BoxGeometry(1.16, 0.26, 2.05);
  const mattressGeo = new THREE.BoxGeometry(1.06, 0.22, 1.92);
  const duvetGeo = new THREE.BoxGeometry(1.08, 0.16, 1.28);
  const pillowGeo = new THREE.BoxGeometry(0.72, 0.14, 0.42);
  const standGeo = new THREE.BoxGeometry(0.42, 0.52, 0.42);

  let bedNumber = 1;
  const bedRowsZ = [-5.4, 0.0, 5.4];
  const bedColsX = [-24.5, -21.6, -18.7, -15.8, -12.9];

  for (let r = 0; r < bedRowsZ.length; r++) {
    for (let c = 0; c < bedColsX.length; c++) {
      const bx = bedColsX[c];
      const bz = bedRowsZ[r];
      const num = bedNumber++;

      const bed = new THREE.Group();
      bed.position.set(bx, 0, bz);

      const frame = new THREE.Mesh(bedFrameGeo, mats.walnut);
      frame.position.y = 0.13;
      frame.castShadow = true;
      bed.add(frame);

      const head = new THREE.Mesh(bedHeadGeo, mats.indigoFabric);
      head.position.set(0, 0.56, -0.96);
      head.castShadow = true;
      bed.add(head);

      const mattress = new THREE.Mesh(mattressGeo, mats.whiteLinen);
      mattress.position.set(0, 0.36, 0.02);
      bed.add(mattress);

      const duvet = new THREE.Mesh(duvetGeo, num % 2 === 0 ? mats.tealVelvet : mats.indigoFabric);
      duvet.position.set(0, 0.42, 0.34);
      bed.add(duvet);

      const pillow = new THREE.Mesh(pillowGeo, mats.whiteLinen);
      pillow.position.set(0, 0.52, -0.66);
      bed.add(pillow);

      const numTex = createBedNumberTexture(num);
      const numPlaque = new THREE.Mesh(
        new THREE.PlaneGeometry(0.32, 0.32),
        new THREE.MeshStandardMaterial({ map: numTex, roughness: 0.3 })
      );
      numPlaque.position.set(0, 0.88, -0.88);
      bed.add(numPlaque);

      const stand = new THREE.Mesh(standGeo, mats.walnut);
      stand.position.set(0.84, 0.26, -0.78);
      bed.add(stand);

      const lampShade = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.15, 0.22, 10), mats.bedroomGlow);
      lampShade.position.set(0.84, 0.68, -0.78);
      bed.add(lampShade);

      root.add(bed);
      addBoxCollider(bx, bz, 1.22, 2.1);
      addBoxCollider(bx + 0.84, bz - 0.78, 0.45, 0.45);

      registerInteractable({
        id: `bed_${num}`,
        label: `Bed #${String(num).padStart(2, '0')}`,
        x: bx,
        y: 0.45,
        z: bz + 0.42,
        charRotY: Math.PI, // Sit facing +Z foot of bed
        sleepX: bx,
        sleepY: 0.54,
        sleepZ: bz - 0.14,
        sleepRotY: Math.PI, // Head on pillow (-Z), face up
        exitX: bx,
        exitZ: bz + 1.52,
        type: 'bed',
        radius: 2.35
      });
    }
  }

  for (let wz = -4; wz <= 6; wz += 3.2) {
    const locker = new THREE.Mesh(new THREE.BoxGeometry(0.75, 2.35, 2.2), mats.walnut);
    locker.position.set(-26.35, 1.175, wz);
    locker.castShadow = true;
    root.add(locker);
    addBoxCollider(-26.35, wz, 0.8, 2.25);
  }

  createCeilingFan(-21.6, -2.7, 'bedroom');
  createCeilingFan(-15.8, 2.7, 'bedroom');
  createWallSwitch(-10.22, -3.2, -Math.PI / 2, 'bedroom', 'light', 'Bedroom Lights');
  createWallSwitch(-10.22, 3.2, -Math.PI / 2, 'bedroom', 'fan', 'Bedroom Ceiling Fans');

  // =========================================================
  // 6. SPA BATHROOM (X: -27..-10, Z: 10..22)
  // =========================================================
  const vanity = new THREE.Mesh(new THREE.BoxGeometry(0.85, 0.88, 4.4), mats.marble);
  vanity.position.set(-26.25, 0.44, 16);
  root.add(vanity);
  addBoxCollider(-26.25, 16, 0.9, 4.45);

  [14.9, 17.1].forEach((mz) => {
    const basin = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.22, 0.16, 14), mats.ceramic);
    basin.position.set(-26.2, 0.95, mz);
    root.add(basin);
    const mirror = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.55, 0.05, 20), mats.chrome);
    mirror.rotation.z = Math.PI / 2;
    mirror.position.set(-26.68, 1.75, mz);
    root.add(mirror);
    const halo = new THREE.Mesh(new THREE.TorusGeometry(0.58, 0.025, 8, 24), mats.bathGlow);
    halo.rotation.y = Math.PI / 2;
    halo.position.set(-26.66, 1.75, mz);
    root.add(halo);
  });

  const tub = new THREE.Mesh(new THREE.CylinderGeometry(0.88, 0.72, 0.68, 20), mats.ceramic);
  tub.scale.set(1.0, 1.0, 1.85);
  tub.position.set(-21.2, 0.34, 19.6);
  tub.castShadow = true;
  root.add(tub);
  addBoxCollider(-21.2, 19.6, 1.8, 3.2);

  [-15.5, -12.5].forEach((sx) => {
    const partition = new THREE.Mesh(new THREE.BoxGeometry(0.06, 2.5, 2.4), mats.glassClear);
    partition.position.set(sx - 1.1, 1.25, 20.5);
    root.add(partition);
    const head = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.22, 0.05, 12), mats.chrome);
    head.position.set(sx, 2.55, 21.1);
    root.add(head);
    addBoxCollider(sx - 1.1, 20.5, 0.16, 2.4);
  });

  createWallSwitch(-15.5, 10.22, 0, 'bathroom', 'light', 'Spa Bathroom Lights');

  // =========================================================
  // 7. INDOOR SWIMMING POOL AREA (X: -10..10, Z: 8..22)
  // =========================================================
  // Deep recessed pool basin + tiled interior walls for realistic swimming depth
  const poolBasin = new THREE.Mesh(new THREE.BoxGeometry(13.0, 0.14, 9.0), mats.poolTile);
  poolBasin.position.set(0, -1.35, 15.0);
  root.add(poolBasin);

  const poolWallN = new THREE.Mesh(new THREE.BoxGeometry(13.0, 1.35, 0.12), mats.poolTile);
  poolWallN.position.set(0, -0.68, 10.5);
  root.add(poolWallN);
  const poolWallS = poolWallN.clone();
  poolWallS.position.z = 19.5;
  root.add(poolWallS);
  const poolWallW = new THREE.Mesh(new THREE.BoxGeometry(0.12, 1.35, 9.0), mats.poolTile);
  poolWallW.position.set(-6.5, -0.68, 15.0);
  root.add(poolWallW);
  const poolWallE = poolWallW.clone();
  poolWallE.position.x = 6.5;
  root.add(poolWallE);

  // Underwater glowing strip beneath pool coping
  const poolStripN = new THREE.Mesh(new THREE.BoxGeometry(12.8, 0.06, 0.05), mats.poolGlow);
  poolStripN.position.set(0, -0.22, 10.58);
  root.add(poolStripN);
  const poolStripS = poolStripN.clone();
  poolStripS.position.z = 19.42;
  root.add(poolStripS);

  // Animated Shimmering Water Plane (vertices modulated gently on update)
  const waterGeo = new THREE.PlaneGeometry(12.9, 8.9, 18, 12);
  waterGeo.rotateX(-Math.PI / 2);
  const waterBasePositions = Float32Array.from(waterGeo.attributes.position.array);
  const waterMat = new THREE.MeshStandardMaterial({
    color: 0x1ec8e6,
    emissive: 0x0a5b70,
    emissiveIntensity: 0.42,
    transparent: true,
    opacity: 0.74,
    roughness: 0.1,
    metalness: 0.25,
    side: THREE.DoubleSide
  });
  const waterMesh = new THREE.Mesh(waterGeo, waterMat);
  waterMesh.position.set(0, POOL_BOUNDS.waterY, 15.0);
  root.add(waterMesh);
  animatedObjects.waterMesh = waterMesh;
  animatedObjects.waterBasePositions = waterBasePositions;

  // Swimmer surface ripple ring
  const rippleGeo = new THREE.RingGeometry(0.32, 0.5, 24);
  rippleGeo.rotateX(-Math.PI / 2);
  const rippleMat = new THREE.MeshBasicMaterial({
    color: 0xe0faff,
    transparent: true,
    opacity: 0.0,
    side: THREE.DoubleSide,
    depthWrite: false
  });
  const rippleRing = new THREE.Mesh(rippleGeo, rippleMat);
  rippleRing.position.set(0, POOL_BOUNDS.waterY + 0.025, 15.0);
  rippleRing.visible = false;
  root.add(rippleRing);
  animatedObjects.rippleRing = rippleRing;

  // Pool Coping Rim & Collider (keeps non-swimming walkers on the travertine deck)
  const copingN = new THREE.Mesh(new THREE.BoxGeometry(13.4, 0.16, 0.35), mats.stone);
  copingN.position.set(0, 0.06, 10.4);
  root.add(copingN);
  const copingS = copingN.clone();
  copingS.position.z = 19.6;
  root.add(copingS);
  const copingW = new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.16, 9.4), mats.stone);
  copingW.position.set(-6.6, 0.06, 15.0);
  root.add(copingW);
  const copingE = copingW.clone();
  copingE.position.x = 6.6;
  root.add(copingE);
  const poolCollider = addBoxCollider(0, 15.0, 13.2, 9.2, 'pool_basin');

  const ladder = new THREE.Mesh(new THREE.TorusGeometry(0.35, 0.035, 8, 16, Math.PI), mats.chrome);
  ladder.position.set(-6.2, 0.35, 12.2);
  ladder.rotation.y = Math.PI / 2;
  root.add(ladder);

  // Poolside Sun Loungers (lounger faces -Z pool -> charRotY = 0; exit into deck gap)
  [-4.2, -1.6, 1.6, 4.2].forEach((lx, idx) => {
    const lounger = new THREE.Group();
    lounger.position.set(lx, 0, 20.8);
    const frame = new THREE.Mesh(new THREE.BoxGeometry(0.88, 0.24, 1.95), mats.oak);
    frame.position.y = 0.16;
    lounger.add(frame);
    const pad = new THREE.Mesh(new THREE.BoxGeometry(0.82, 0.1, 1.35), mats.whiteLinen);
    pad.position.set(0, 0.32, -0.25);
    lounger.add(pad);
    const backrest = new THREE.Mesh(new THREE.BoxGeometry(0.82, 0.1, 0.65), mats.whiteLinen);
    backrest.position.set(0, 0.48, 0.66);
    backrest.rotation.x = 0.55;
    lounger.add(backrest);
    root.add(lounger);
    addBoxCollider(lx, 20.8, 0.92, 2.0);
    registerInteractable({
      id: `lounger_${idx}`,
      label: 'Poolside Lounger',
      x: lx,
      y: 0.42,
      z: 20.65,
      charRotY: 0,
      exitX: lx < 0 ? lx + 1.05 : lx - 1.05,
      exitZ: 20.6,
      type: 'sit'
    });
  });

  createPottedPlant(-8.8, 10.2, 1.25);
  createPottedPlant(8.8, 10.2, 1.25);
  createWallSwitch(-3.6, 8.22, 0, 'pool', 'light', 'Pool & Deck Lights');

  // =========================================================
  // 8. INDOOR BOTANICAL GARDEN (X: 10..27, Z: 10..22)
  // =========================================================
  const pathPts = [
    [11.4, 16.0], [13.5, 16.4], [15.8, 15.8], [18.2, 14.5], [18.5, 12.2]
  ];
  pathPts.forEach(([px, pz]) => {
    const paver = new THREE.Mesh(new THREE.CylinderGeometry(0.65, 0.68, 0.05, 12), mats.stone);
    paver.position.set(px, 0.025, pz);
    paver.receiveShadow = true;
    root.add(paver);
  });

  function createGardenBed(cx, cz, sx, sz) {
    const wall = new THREE.Mesh(new THREE.BoxGeometry(sx, 0.45, sz), mats.stone);
    wall.position.set(cx, 0.225, cz);
    wall.castShadow = true;
    root.add(wall);
    addBoxCollider(cx, cz, sx + 0.1, sz + 0.1);

    const count = Math.floor((sx * sz) / 1.4);
    for (let i = 0; i < count; i++) {
      const ox = ((i * 37) % 100) / 100 - 0.5;
      const oz = ((i * 61) % 100) / 100 - 0.5;
      const bush = new THREE.Mesh(
        new THREE.DodecahedronGeometry(0.45 + (i % 3) * 0.14, 1),
        i % 2 === 0 ? mats.leafDark : mats.leafBright
      );
      bush.position.set(cx + ox * (sx - 0.8), 0.72 + (i % 2) * 0.25, cz + oz * (sz - 0.8));
      bush.scale.y = 1.25;
      root.add(bush);
    }
  }

  createGardenBed(23.2, 18.8, 5.4, 4.4);
  createGardenBed(14.5, 20.0, 6.2, 2.6);
  createGardenBed(24.5, 12.6, 3.2, 3.8);

  // Two Garden Benches with 2 seat slots each!
  function createGardenBench(x, z, rotY, idPrefix) {
    const b = new THREE.Group();
    b.position.set(x, 0, z);
    b.rotation.y = rotY;
    const seat = new THREE.Mesh(new THREE.BoxGeometry(2.1, 0.1, 0.65), mats.oak);
    seat.position.y = 0.45;
    b.add(seat);
    const back = new THREE.Mesh(new THREE.BoxGeometry(2.1, 0.48, 0.08), mats.oak);
    back.position.set(0, 0.72, -0.28);
    b.add(back);
    [-0.88, 0.88].forEach((lx) => {
      const leg = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.45, 0.6), mats.darkMetal);
      leg.position.set(lx, 0.225, 0);
      b.add(leg);
    });
    root.add(b);
    const sx = Math.abs(Math.cos(rotY)) > 0.5 ? 2.15 : 0.7;
    const sz = Math.abs(Math.cos(rotY)) > 0.5 ? 0.7 : 2.15;
    addBoxCollider(x, z, sx, sz);

    [-0.5, 0.5].forEach((offset, slotIdx) => {
      const cosR = Math.cos(rotY);
      const sinR = Math.sin(rotY);
      const seatX = x + offset * cosR;
      const seatZ = z - offset * sinR;
      const ex = seatX + sinR * 1.05;
      const ez = seatZ + cosR * 1.05;
      registerInteractable({
        id: `${idPrefix}_${slotIdx + 1}`,
        label: 'Garden Bench',
        x: seatX,
        y: 0.46,
        z: seatZ,
        charRotY: rotY + Math.PI,
        exitX: ex,
        exitZ: ez,
        type: 'sit'
      });
    });
  }
  createGardenBench(18.5, 18.8, 0, 'garden_bench_1');
  createGardenBench(21.4, 15.0, -Math.PI / 2, 'garden_bench_2');

  // Pathway glowing bollards + light switch
  [[12.2, 14.5], [16.5, 14.5]].forEach(([bx, bz]) => {
    const post = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.1, 0.75, 10), mats.darkMetal);
    post.position.set(bx, 0.375, bz);
    root.add(post);
    const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.09, 0.14, 10), mats.gardenGlow);
    cap.position.set(bx, 0.78, bz);
    root.add(cap);
  });
  createWallSwitch(10.22, 13.4, Math.PI / 2, 'garden', 'light', 'Botanical Garden Lights');

  // =========================================================
  // 9. ACTIVITY & CHALLENGE ROOM (X: 10..27, Z: 0..10)
  // =========================================================
  const gameTable = new THREE.Group();
  gameTable.position.set(17.5, 0, 5.0);
  const gTop = new THREE.Mesh(new THREE.BoxGeometry(3.2, 0.12, 1.7), mats.tealVelvet);
  gTop.position.y = 0.84;
  gTop.castShadow = true;
  gameTable.add(gTop);
  const gTrim = new THREE.Mesh(new THREE.BoxGeometry(3.26, 0.05, 1.76), mats.activityGlow);
  gTrim.position.y = 0.78;
  gameTable.add(gTrim);
  const net = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.2, 1.72), mats.whiteLinen);
  net.position.y = 0.98;
  gameTable.add(net);
  [[-1.3, -0.65], [1.3, -0.65], [-1.3, 0.65], [1.3, 0.65]].forEach(([lx, lz]) => {
    const leg = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.8, 0.12), mats.darkMetal);
    leg.position.set(lx, 0.4, lz);
    gameTable.add(leg);
  });
  root.add(gameTable);
  addBoxCollider(17.5, 5.0, 3.3, 1.8);
  registerInteractable({
    id: 'activity_table',
    label: 'Inspect Challenge Table',
    x: 17.5,
    y: 0.45,
    z: 3.6,
    charRotY: 0,
    type: 'inspect'
  });

  [[24.5, 3.2, 0.5], [24.5, 5.0, 0.85], [24.5, 6.8, 0.35]].forEach(([px, pz, ph], idx) => {
    const pod = new THREE.Mesh(new THREE.BoxGeometry(1.1, ph, 1.2), mats.darkMetal);
    pod.position.set(px, ph / 2, pz);
    root.add(pod);
    const rim = new THREE.Mesh(new THREE.BoxGeometry(1.14, 0.06, 1.24), idx === 1 ? mats.gold : mats.cyanGlow);
    rim.position.set(px, ph, pz);
    root.add(rim);
    addBoxCollider(px, pz, 1.15, 1.25);
  });

  createArmchair(13.2, 1.8, Math.PI / 4, mats.crimsonVelvet, 'Activity Lounge Chair', 'act_chair_1');
  createArmchair(13.2, 8.0, (Math.PI * 3) / 4, mats.crimsonVelvet, 'Activity Lounge Chair', 'act_chair_2');
  createCeilingFan(17.5, 5.0, 'activity');
  createWallSwitch(10.22, 1.4, Math.PI / 2, 'activity', 'light', 'Activity Room Lights');
  createWallSwitch(10.22, 6.6, Math.PI / 2, 'activity', 'fan', 'Activity Ceiling Fan');

  // =========================================================
  // 10. ADMIN CONTROL ROOM (X: 14..27, Z: -8..0) — ADMIN ROLE ONLY
  // =========================================================
  const adminGroup = new THREE.Group();
  adminGroup.visible = false; // Hidden for ordinary PLAYER role!
  root.add(adminGroup);

  const consoleDesk = new THREE.Mesh(new THREE.BoxGeometry(1.18, 0.84, 5.6), mats.darkMetal);
  consoleDesk.position.set(22.2, 0.42, -4.0);
  consoleDesk.castShadow = true;
  adminGroup.add(consoleDesk);
  const consoleTop = new THREE.Mesh(new THREE.BoxGeometry(1.26, 0.06, 5.66), mats.cyanGlow);
  consoleTop.position.set(22.2, 0.85, -4.0);
  adminGroup.add(consoleTop);
  const deskTrimGold = new THREE.Mesh(new THREE.BoxGeometry(1.28, 0.04, 5.68), mats.gold);
  deskTrimGold.position.set(22.2, 0.89, -4.0);
  adminGroup.add(deskTrimGold);
  addBoxCollider(22.2, -4.0, 1.25, 5.65);

  // Curved production desk monitor screens + Admin Voting Phone dock + Broadcast Mic
  [-5.7, -4.0, -2.3].forEach((mz, idx) => {
    const deskScreen = new THREE.Mesh(
      new THREE.BoxGeometry(0.06, 0.48, 1.15),
      idx === 1 ? mats.gold : mats.cyanGlow
    );
    deskScreen.position.set(22.42, 1.24, mz);
    deskScreen.rotation.z = 0.22;
    adminGroup.add(deskScreen);
  });

  // Broadcast Mic Stand
  const micStand = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.06, 0.42, 8), mats.chrome);
  micStand.position.set(21.85, 1.12, -5.0);
  adminGroup.add(micStand);
  const micHead = new THREE.Mesh(new THREE.SphereGeometry(0.055, 10, 8), mats.gold);
  micHead.position.set(21.85, 1.35, -5.0);
  adminGroup.add(micHead);

  // Admin Voting Smartphone Dock on Console
  const phoneDock = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.06, 0.28), mats.darkMetal);
  phoneDock.position.set(21.85, 0.94, -3.1);
  adminGroup.add(phoneDock);
  const phoneScreen = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.24, 0.03), mats.cyanGlow);
  phoneScreen.position.set(21.85, 1.06, -3.1);
  phoneScreen.rotation.y = -Math.PI / 2;
  phoneScreen.rotation.x = -0.35;
  adminGroup.add(phoneScreen);

  // Overhead Studio Gantry & Production Sign
  const studioRing = new THREE.Mesh(new THREE.TorusGeometry(2.4, 0.05, 8, 36), mats.cyanGlow);
  studioRing.rotation.x = Math.PI / 2;
  studioRing.position.set(22.2, 4.45, -4.0);
  adminGroup.add(studioRing);

  const monTex0 = createMonitorWallTexture(0);
  const monTex1 = createMonitorWallTexture(1);
  [-5.6, -2.4].forEach((mz, idx) => {
    const mon = new THREE.Mesh(
      new THREE.BoxGeometry(0.12, 1.65, 3.0),
      new THREE.MeshStandardMaterial({
        map: idx === 0 ? monTex0 : monTex1,
        emissive: 0xffffff,
        emissiveMap: idx === 0 ? monTex0 : monTex1,
        emissiveIntensity: 0.85,
        roughness: 0.2
      })
    );
    mon.position.set(26.55, 2.08, mz);
    adminGroup.add(mon);
  });

  [-7.0, -1.0].forEach((sz) => {
    const rack = new THREE.Mesh(new THREE.BoxGeometry(0.85, 2.5, 0.95), mats.darkMetal);
    rack.position.set(25.8, 1.25, sz);
    adminGroup.add(rack);
    addBoxCollider(25.8, sz, 0.9, 1.0);
    for (let l = 0; l < 4; l++) {
      const ledMat = mats.cyanGlow.clone();
      const led = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.06, 0.35), ledMat);
      led.position.set(25.35, 0.7 + l * 0.45, sz);
      adminGroup.add(led);
      animatedObjects.serverLeds.push(ledMat);
    }
  });

  // CHAIR 1: CHATGPT AI OPERATOR SEAT + BESPOKE 3D AI OPERATOR AVATAR + AI TERMINAL CONSOLE
  createArmchair(20.6, -5.2, Math.PI / 2, mats.tealVelvet, 'ChatGPT AI Operator Chair', 'admin_chair_1_mesh', adminGroup, 'ADMIN', { x: 19.4, z: -5.2 }, false);

  const aiScreenMat = makeGlowMat(0x22d3ee, 0x06b6d4, 1.35);
  const aiTerminal = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.52, 1.05), aiScreenMat);
  aiTerminal.position.set(22.15, 1.24, -5.2);
  aiTerminal.rotation.z = 0.2;
  adminGroup.add(aiTerminal);

  const aiOperatorRig = new THREE.Group();
  aiOperatorRig.position.set(20.66, 0.44, -5.2);
  aiOperatorRig.rotation.y = Math.PI / 2; // Facing +X console desk
  const aiBodyMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, roughness: 0.35, metalness: 0.72 });
  const aiSuitTrim = new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.45, metalness: 0.45 });
  const aiVisorMat = makeGlowMat(0x38bdf8, 0x0ea5e9, 1.4);
  const aiHaloMat = makeGlowMat(0x10b981, 0x10b981, 1.2);

  const aiPelvis = new THREE.Mesh(new THREE.BoxGeometry(0.42, 0.18, 0.28), aiSuitTrim);
  aiPelvis.position.set(0, 0.08, 0);
  aiOperatorRig.add(aiPelvis);
  [-0.12, 0.12].forEach((lx) => {
    const thigh = new THREE.Mesh(new THREE.CylinderGeometry(0.075, 0.065, 0.38, 10), aiBodyMat);
    thigh.rotation.x = Math.PI / 2;
    thigh.position.set(lx, 0.1, 0.22);
    aiOperatorRig.add(thigh);
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.062, 0.052, 0.42, 10), aiSuitTrim);
    shin.position.set(lx, -0.1, 0.4);
    aiOperatorRig.add(shin);
  });
  const aiTorso = new THREE.Mesh(new THREE.CylinderGeometry(0.21, 0.17, 0.56, 12), aiBodyMat);
  aiTorso.position.set(0, 0.46, 0);
  aiOperatorRig.add(aiTorso);
  const aiCoreBadge = new THREE.Mesh(new THREE.OctahedronGeometry(0.08, 0), aiVisorMat);
  aiCoreBadge.position.set(0, 0.5, 0.18);
  aiOperatorRig.add(aiCoreBadge);

  const aiHead = new THREE.Group();
  aiHead.position.set(0, 0.88, 0.02);
  const aiSkull = new THREE.Mesh(new THREE.SphereGeometry(0.165, 14, 12), aiSuitTrim);
  aiHead.add(aiSkull);
  const aiVisor = new THREE.Mesh(new THREE.BoxGeometry(0.24, 0.09, 0.12), aiVisorMat);
  aiVisor.position.set(0, 0.01, 0.12);
  aiHead.add(aiVisor);
  const aiHalo = new THREE.Mesh(new THREE.TorusGeometry(0.24, 0.018, 8, 28), aiHaloMat);
  aiHalo.rotation.x = Math.PI / 2;
  aiHalo.position.set(0, 0.22, 0);
  aiHead.add(aiHalo);
  aiOperatorRig.add(aiHead);

  const makeAiArm = (sideSign) => {
    const armGroup = new THREE.Group();
    armGroup.position.set(sideSign * 0.25, 0.66, 0.03);
    const upper = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.045, 0.3, 8), aiBodyMat);
    upper.rotation.x = -0.75;
    upper.position.set(0, -0.08, 0.12);
    armGroup.add(upper);
    const fore = new THREE.Mesh(new THREE.CylinderGeometry(0.042, 0.036, 0.28, 8), aiSuitTrim);
    fore.rotation.x = -Math.PI / 2.2;
    fore.position.set(0, -0.18, 0.33);
    armGroup.add(fore);
    const handGlow = new THREE.Mesh(new THREE.SphereGeometry(0.042, 8, 8), aiVisorMat);
    handGlow.position.set(0, -0.19, 0.48);
    armGroup.add(handGlow);
    aiOperatorRig.add(armGroup);
    return armGroup;
  };
  animatedObjects.aiOperatorRig = aiOperatorRig;
  animatedObjects.aiLeftArm = makeAiArm(-1);
  animatedObjects.aiRightArm = makeAiArm(1);
  animatedObjects.aiHead = aiHead;
  animatedObjects.aiHaloMat = aiHaloMat;
  animatedObjects.aiVisorMat = aiVisorMat;
  animatedObjects.aiScreenMat = aiScreenMat;
  adminGroup.add(aiOperatorRig);

  registerInteractable({
    id: 'ai_operator_console',
    label: 'ChatGPT AI Operator Console',
    x: 21.3,
    y: 0.45,
    z: -5.2,
    charRotY: -Math.PI / 2,
    type: 'ai_operator_console',
    requiredRole: 'ADMIN'
  });

  // CHAIR 2: MAIN ADMIN (GAME OWNER) CHAIR + OWNER DASHBOARD CONSOLE
  createArmchair(20.6, -2.8, Math.PI / 2, mats.indigoFabric, 'Main Admin (Owner) Chair', 'admin_chair_2', adminGroup, 'ADMIN', { x: 19.4, z: -2.8 });
  registerInteractable({
    id: 'admin_dashboard_console',
    label: 'Open Admin Control Suite',
    x: 21.3,
    y: 0.45,
    z: -2.8,
    charRotY: -Math.PI / 2,
    type: 'admin_dashboard',
    requiredRole: 'ADMIN'
  });
  createWallSwitch(14.22, -1.6, Math.PI / 2, 'admin', 'light', 'Admin Control Lights', adminGroup, 'ADMIN');

  // Control Room Interactive CCTV Wall & Exclusive Main Admin Personal Phone Prop
  registerInteractable({
    id: 'control_cctv_tv',
    label: 'Live 24-Cam CCTV Wall',
    x: 24.6,
    y: 1.5,
    z: -4.0,
    charRotY: -Math.PI / 2,
    type: 'cctv_wall',
    requiredRole: 'ADMIN',
    radius: 2.8
  });

  const adminPhoneBody = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.28, 0.03), mats.darkMetal);
  adminPhoneBody.position.set(22.0, 0.96, -1.9);
  adminPhoneBody.rotation.y = -Math.PI / 2;
  adminPhoneBody.rotation.x = -0.35;
  adminGroup.add(adminPhoneBody);
  const adminPhoneGlow = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.22, 0.035), mats.cyanGlow);
  adminPhoneGlow.position.copy(adminPhoneBody.position);
  adminPhoneGlow.rotation.copy(adminPhoneBody.rotation);
  adminGroup.add(adminPhoneGlow);

  registerInteractable({
    id: 'admin_personal_phone',
    label: 'Main Admin Personal Phone',
    x: 21.85,
    y: 1.0,
    z: -1.9,
    charRotY: -Math.PI / 2,
    type: 'admin_phone',
    requiredRole: 'ADMIN',
    radius: 2.4
  });

  // 5 Physical 3D House Speakers (mounted across common areas, pulsing on House Announcements)
  const speakerPlacements = [
    { x: 0.0, y: 4.85, z: -7.65, rotY: 0 },
    { x: -15.0, y: 4.75, z: -8.35, rotY: Math.PI },
    { x: -4.0, y: 4.75, z: -21.55, rotY: 0 },
    { x: 10.5, y: 4.75, z: -8.35, rotY: Math.PI },
    { x: 18.5, y: 4.75, z: 10.35, rotY: 0 }
  ];
  speakerPlacements.forEach((sp) => {
    const spGrp = new THREE.Group();
    spGrp.position.set(sp.x, sp.y, sp.z);
    spGrp.rotation.y = sp.rotY;
    const spBox = new THREE.Mesh(new THREE.BoxGeometry(0.62, 0.38, 0.28), mats.darkMetal);
    spGrp.add(spBox);
    const haloMat = makeGlowMat(0xfacc15, 0xeab308, 0.45);
    const halo = new THREE.Mesh(new THREE.TorusGeometry(0.14, 0.025, 8, 20), haloMat);
    halo.position.z = 0.15;
    spGrp.add(halo);
    root.add(spGrp);
    animatedObjects.speakerMats.push(haloMat);
  });

  // Living Atrium Grand Finale Trophy Podium (toggled by Finale Ceremony State)
  const finaleGroup = new THREE.Group();
  finaleGroup.position.set(0.0, 0.0, -4.6);
  finaleGroup.visible = false;
  const podiumStep1 = new THREE.Mesh(new THREE.CylinderGeometry(1.55, 1.75, 0.24, 24), mats.marble);
  podiumStep1.position.y = 0.12;
  finaleGroup.add(podiumStep1);
  const podiumStep2 = new THREE.Mesh(new THREE.CylinderGeometry(0.95, 1.1, 0.42, 24), mats.gold);
  podiumStep2.position.y = 0.45;
  finaleGroup.add(podiumStep2);
  const finaleRing = new THREE.Mesh(new THREE.TorusGeometry(1.48, 0.04, 8, 32), mats.captainGlow);
  finaleRing.rotation.x = Math.PI / 2;
  finaleRing.position.y = 0.25;
  finaleGroup.add(finaleRing);
  const finaleTrophy = new THREE.Group();
  finaleTrophy.position.y = 1.05;
  const cupBase = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.22, 0.24, 16), mats.gold);
  finaleTrophy.add(cupBase);
  const cupBowl = new THREE.Mesh(new THREE.ConeGeometry(0.28, 0.45, 16, 1, false), mats.gold);
  cupBowl.rotation.x = Math.PI;
  cupBowl.position.y = 0.36;
  finaleTrophy.add(cupBowl);
  const cupGem = new THREE.Mesh(new THREE.OctahedronGeometry(0.14, 0), mats.cyanGlow);
  cupGem.position.y = 0.68;
  finaleTrophy.add(cupGem);
  finaleGroup.add(finaleTrophy);
  root.add(finaleGroup);
  animatedObjects.finaleGroup = finaleGroup;
  animatedObjects.finaleTrophy = finaleTrophy;

  // =========================================================
  // 11. CONFESSION ROOM (X: 19..27, Z: -22..-8)
  // =========================================================
  const dais = new THREE.Mesh(new THREE.CylinderGeometry(2.0, 2.15, 0.22, 32), mats.walnut);
  dais.position.set(23.5, 0.11, -17.5);
  root.add(dais);
  const daisGlow = new THREE.Mesh(new THREE.TorusGeometry(2.05, 0.04, 8, 36), mats.confessionGlow);
  daisGlow.rotation.x = Math.PI / 2;
  daisGlow.position.set(23.5, 0.22, -17.5);
  root.add(daisGlow);

  const throne = new THREE.Group();
  throne.position.set(23.5, 0.22, -17.8);
  const tSeat = new THREE.Mesh(new THREE.BoxGeometry(1.15, 0.32, 0.95), mats.crimsonVelvet);
  tSeat.position.y = 0.28;
  throne.add(tSeat);
  const tBack = new THREE.Mesh(new THREE.BoxGeometry(1.22, 1.45, 0.24), mats.crimsonVelvet);
  tBack.position.set(0, 1.0, -0.4);
  throne.add(tBack);
  const tCrown = new THREE.Mesh(new THREE.TorusGeometry(1.35, 0.05, 8, 32), mats.confessionGlow);
  tCrown.position.set(0, 1.45, -0.58);
  throne.add(tCrown);
  root.add(throne);
  addBoxCollider(23.5, -17.8, 1.35, 1.15);
  registerInteractable({
    id: 'confession_throne',
    label: 'Confession Throne',
    x: 23.5,
    y: 0.65,
    z: -17.65,
    charRotY: Math.PI, // Faces +Z toward studio camera
    exitX: 23.5,
    exitZ: -16.4,
    type: 'sit'
  });

  const tripod = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.35, 1.3, 6), mats.darkMetal);
  tripod.position.set(23.5, 0.65, -13.6);
  root.add(tripod);
  const camBody = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.32, 0.55), mats.darkMetal);
  camBody.position.set(23.5, 1.4, -13.6);
  root.add(camBody);
  const camLens = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.13, 0.28, 12), mats.chrome);
  camLens.rotation.x = Math.PI / 2;
  camLens.position.set(23.5, 1.4, -13.95);
  root.add(camLens);
  addBoxCollider(23.5, -13.6, 0.7, 0.7);
  createWallSwitch(19.22, -9.0, Math.PI / 2, 'confession', 'light', 'Confession Room Lights');

  // =========================================================
  // 12. CAPTAIN VIP ROOM (X: 10..19, Z: -22..-14)
  // =========================================================
  const kingBed = new THREE.Group();
  kingBed.position.set(14.5, 0, -20.2);
  const kbFrame = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.3, 2.3), mats.walnut);
  kbFrame.position.y = 0.15;
  kingBed.add(kbFrame);
  const kbHead = new THREE.Mesh(new THREE.BoxGeometry(2.5, 1.3, 0.18), mats.gold);
  kbHead.position.set(0, 0.7, -1.12);
  kingBed.add(kbHead);
  const kbMat = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.25, 2.1), mats.whiteLinen);
  kbMat.position.set(0, 0.42, 0.02);
  kingBed.add(kbMat);
  const kbDuvet = new THREE.Mesh(new THREE.BoxGeometry(2.24, 0.18, 1.35), mats.gold);
  kbDuvet.position.set(0, 0.48, 0.38);
  kingBed.add(kbDuvet);
  const kbPillow = new THREE.Mesh(new THREE.BoxGeometry(1.3, 0.15, 0.45), mats.whiteLinen);
  kbPillow.position.set(0, 0.56, -0.78);
  kingBed.add(kbPillow);
  root.add(kingBed);
  addBoxCollider(14.5, -20.2, 2.45, 2.35);
  registerInteractable({
    id: 'captain_bed',
    label: 'Captain King Bed',
    x: 14.5,
    y: 0.52,
    z: -19.55,
    charRotY: Math.PI,
    sleepX: 14.5,
    sleepY: 0.60,
    sleepZ: -20.25,
    sleepRotY: Math.PI,
    exitX: 14.5,
    exitZ: -18.4,
    type: 'bed',
    requiredVip: true,
    radius: 2.5
  });

  const capDesk = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.78, 0.85), mats.walnut);
  capDesk.position.set(17.5, 0.39, -18.5);
  root.add(capDesk);
  addBoxCollider(17.5, -18.5, 1.85, 0.9);
  createArmchair(17.5, -17.3, Math.PI, mats.cognacLeather, 'Captain Desk Chair', 'captain_chair', root, null, { x: 17.5, z: -16.15 }, true, true);

  const ped = new THREE.Mesh(new THREE.CylinderGeometry(0.32, 0.38, 1.1, 16), mats.marble);
  ped.position.set(11.4, 0.55, -20.6);
  root.add(ped);
  const trophy = new THREE.Mesh(new THREE.OctahedronGeometry(0.28, 0), mats.gold);
  trophy.position.set(11.4, 1.38, -20.6);
  root.add(trophy);
  addBoxCollider(11.4, -20.6, 0.75, 0.75);

  createCeilingFan(14.5, -17.6, 'captain');
  createWallSwitch(12.2, -14.22, Math.PI, 'captain', 'light', 'Captain Room Lights');
  createWallSwitch(16.8, -14.22, Math.PI, 'captain', 'fan', 'Captain Ceiling Fan');

  // =========================================================
  // 13. DESIGNATED FICTIONAL SMOKING ROOM (X: 2..10, Z: -22..-14)
  // =========================================================
  const exhaustHood = new THREE.Mesh(new THREE.CylinderGeometry(1.1, 1.65, 0.75, 16), mats.darkMetal);
  exhaustHood.position.set(6.0, 4.75, -18.0);
  root.add(exhaustHood);
  const exhaustRing = new THREE.Mesh(new THREE.TorusGeometry(1.58, 0.04, 8, 28), mats.smokingGlow);
  exhaustRing.rotation.x = Math.PI / 2;
  exhaustRing.position.set(6.0, 4.38, -18.0);
  root.add(exhaustRing);

  // Exhaust ventilation fan blades mounted under the smoking canopy hood
  const exhaustRotor = new THREE.Group();
  exhaustRotor.position.set(6.0, 4.48, -18.0);
  const exHub = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.22, 0.1, 12), mats.chrome);
  exhaustRotor.add(exHub);
  for (let i = 0; i < 6; i++) {
    const bWrap = new THREE.Group();
    bWrap.rotation.y = (i * Math.PI) / 3;
    const blade = new THREE.Mesh(new THREE.BoxGeometry(0.76, 0.02, 0.16), mats.chrome);
    blade.position.set(0.55, 0, 0);
    blade.rotation.x = 0.24;
    bWrap.add(blade);
    exhaustRotor.add(bWrap);
  }
  root.add(exhaustRotor);
  if (lighting) {
    lighting.registerFan('smoking', exhaustRotor, 10.5);
  }

  const smokeTable = new THREE.Mesh(new THREE.CylinderGeometry(0.85, 0.65, 0.48, 18), mats.darkMetal);
  smokeTable.position.set(6.0, 0.24, -18.2);
  root.add(smokeTable);
  const smokeGlass = new THREE.Mesh(new THREE.CylinderGeometry(0.9, 0.9, 0.05, 18), mats.glassClear);
  smokeGlass.position.set(6.0, 0.5, -18.2);
  root.add(smokeGlass);
  const ashtray = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.14, 0.06, 12), mats.chrome);
  ashtray.position.set(6.0, 0.56, -18.2);
  root.add(ashtray);
  addBoxCollider(6.0, -18.2, 1.75, 1.75);

  createArmchair(4.1, -18.2, Math.PI / 2, mats.cognacLeather, 'Smoking Lounge Chair', 'smoke_chair_w', root, null, { x: 3.1, z: -18.2 });
  createArmchair(7.9, -18.2, -Math.PI / 2, mats.cognacLeather, 'Smoking Lounge Chair', 'smoke_chair_e', root, null, { x: 8.9, z: -18.2 });
  createArmchair(6.0, -20.2, 0, mats.cognacLeather, 'Smoking Lounge Chair', 'smoke_chair_n', root, null, { x: 4.9, z: -20.2 });

  createWallSwitch(3.8, -14.22, Math.PI, 'smoking', 'light', 'Smoking Lounge Lights');
  createWallSwitch(8.2, -14.22, Math.PI, 'smoking', 'fan', 'Ventilation Exhaust Fan');

  // VIP Gallery Decorative Sculpture Pedestal
  const artPed = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.4, 1.05, 16), mats.marble);
  artPed.position.set(3.2, 0.525, -11.0);
  root.add(artPed);
  const artOrb = new THREE.Mesh(new THREE.TorusKnotGeometry(0.24, 0.07, 48, 8), mats.gold);
  artOrb.position.set(3.2, 1.45, -11.0);
  root.add(artOrb);
  addBoxCollider(3.2, -11.0, 0.85, 0.85);

  scene.add(root);

  return {
    root,
    interactables,
    poolCollider,
    syncRoleVisibility(authSession) {
      adminGroup.visible = Boolean(authSession && authSession.isAdmin());
    },
    claimTarget(itemId, actorId) {
      const item = interactables.find((it) => it.id === itemId);
      if (!item) return { ok: false, reason: 'Interactable not found.' };
      if (item.occupiedBy && item.occupiedBy !== actorId) {
        return { ok: false, reason: `Already occupied by ${item.occupiedBy}.` };
      }
      item.occupiedBy = actorId;
      return { ok: true, item };
    },
    releaseTarget(itemId, actorId = null) {
      const item = interactables.find((it) => it.id === itemId);
      if (!item) return;
      if (!actorId || item.occupiedBy === actorId) {
        item.occupiedBy = null;
      }
    },
    releaseAllForActor(actorId) {
      if (!actorId) return;
      for (const item of interactables) {
        if (item.occupiedBy === actorId) {
          item.occupiedBy = null;
        }
      }
    },
    applyRemoteSeatStates(remoteSeatsMap, localUid) {
      const activeRemoteSeats = new Set();
      if (remoteSeatsMap && typeof remoteSeatsMap === 'object') {
        for (const [seatId, info] of Object.entries(remoteSeatsMap)) {
          if (!info || !info.uid || info.uid === localUid) continue;
          const item = interactables.find((it) => it.id === seatId);
          if (item && item.requiredRole !== 'ADMIN') {
            item.occupiedBy = info.name || 'Remote Player';
            item.remoteUid = info.uid;
            activeRemoteSeats.add(seatId);
          }
        }
      }
      for (const item of interactables) {
        if (item.remoteUid && !activeRemoteSeats.has(item.id)) {
          item.occupiedBy = null;
          item.remoteUid = null;
        }
      }
    },
    getAvailableNpcSeatsAndBeds() {
      return interactables.filter(
        (it) =>
          (it.type === 'sit' || it.type === 'bed') &&
          it.requiredRole !== 'ADMIN' &&
          !it.requiredVip &&
          !it.occupiedBy
      );
    },
    isNearPoolEdge(px, pz) {
      // Check if player is on the travertine deck within 1.55m of the pool rim
      const { basinMinX, basinMaxX, basinMinZ, basinMaxZ, swimMinX, swimMaxX, swimMinZ, swimMaxZ } = POOL_BOUNDS;
      const inRoom = px >= -9.8 && px <= 9.8 && pz >= 8.2 && pz <= 21.8;
      if (!inRoom) return { near: false };
      const insideBasin = px > basinMinX && px < basinMaxX && pz > basinMinZ && pz < basinMaxZ;
      if (insideBasin) return { near: false };

      const nearX = Math.max(basinMinX, Math.min(px, basinMaxX));
      const nearZ = Math.max(basinMinZ, Math.min(pz, basinMaxZ));
      const dist = Math.hypot(px - nearX, pz - nearZ);
      if (dist > 1.55) return { near: false };

      const entryX = Math.max(swimMinX, Math.min(swimMaxX, nearX + (px === nearX ? 0 : Math.sign(nearX - px) * 0.75)));
      const entryZ = Math.max(swimMinZ, Math.min(swimMaxZ, nearZ + (pz === nearZ ? 0 : Math.sign(nearZ - pz) * 0.75)));
      const faceAngle = Math.atan2(-(entryX - px), -(entryZ - pz));
      return { near: true, entryX, entryZ, faceAngle };
    },
    getNearestPoolExit(px, pz) {
      const candidates = [
        { x: Math.max(-5.2, Math.min(5.2, px)), z: 9.45 }, // North pool deck
        { x: -7.55, z: Math.max(11.4, Math.min(18.6, pz)) }, // West pool deck
        { x: 7.55, z: Math.max(11.4, Math.min(18.6, pz)) }, // East pool deck
        { x: 0.0, z: 20.4 }, // South deck center gap between loungers
        { x: -2.9, z: 20.4 },
        { x: 2.9, z: 20.4 }
      ];
      let best = candidates[0];
      let bestDist = Infinity;
      for (const c of candidates) {
        const d = Math.hypot(px - c.x, pz - c.z);
        if (d < bestDist) {
          bestDist = d;
          best = c;
        }
      }
      return best;
    },
    setPoolColliderActive(active) {
      poolCollider.active = Boolean(active);
    },
    setSwimmerRipple(active, x, z) {
      animatedObjects.rippleActive = Boolean(active);
      animatedObjects.ripplePos.set(x, z);
    },
    attemptInteract(itemId, authSession, actorId = null) {
      const item = interactables.find((it) => it.id === itemId);
      if (!item) return { ok: false, denied: false, reason: 'Interactable not found.' };
      if (item.requiredRole === 'ADMIN') {
        const perm = authSession && typeof authSession.assertAdminPermission === 'function'
          ? authSession.assertAdminPermission(`INTERACT_${item.id}`)
          : { allowed: false, code: 'ACCESS_DENIED', error: 'Access denied: ADMIN role required.' };
        if (!perm.allowed) {
          return { ok: false, denied: true, reason: perm.error };
        }
      }
      if (item.requiredVip) {
        const perm = authSession && typeof authSession.assertVipPermission === 'function'
          ? authSession.assertVipPermission(item.label)
          : { allowed: false, code: 'VIP_ACCESS_DENIED', error: 'VIP Room furniture reserved for Captain & VIPs.' };
        if (!perm.allowed) {
          return { ok: false, denied: true, reason: perm.error };
        }
      }
      if ((item.type === 'sit' || item.type === 'bed') && item.occupiedBy && item.occupiedBy !== actorId) {
        return { ok: false, denied: false, occupied: true, reason: `${item.label} is occupied by ${item.occupiedBy}.` };
      }
      return { ok: true, denied: false, item };
    },
    getNearestInteractable(px, pz, maxDist = 2.45, authSession = null, options = {}) {
      const isAdmin = Boolean(authSession && authSession.isAdmin());
      const skipOccupied = options.ignoreOccupied !== false;
      let best = null;
      let bestDist = maxDist;
      for (const item of interactables) {
        if (item.requiredRole === 'ADMIN' && !isAdmin) continue;
        if (skipOccupied && item.occupiedBy) continue;
        const limit = Math.min(maxDist, item.radius || maxDist);
        const d = Math.hypot(px - item.x, pz - item.z);
        if (d < limit && d < bestDist) {
          bestDist = d;
          best = item;
        }
      }
      return best;
    },
    setKitchenVisualState(kitchenState) {
      if (!kitchenState || typeof kitchenState !== 'object') return;
      animatedObjects.isCookingActive = Number(kitchenState.cookingCount || 0) > 0;
      animatedObjects.hasReadyMeals = Number(kitchenState.readyMealsCount || 0) > 0;
      animatedObjects.servingPlatters.forEach((platter) => {
        platter.visible = animatedObjects.hasReadyMeals;
      });
    },
    setAiOperatorVisualState(aiState) {
      if (!aiState || typeof aiState !== 'object') return;
      animatedObjects.aiState = {
        enabled: Boolean(aiState.enabled),
        mode: String(aiState.mode || 'MANUAL').toUpperCase(),
        emergencyStopped: Boolean(aiState.emergencyStopped)
      };
      if (animatedObjects.aiHaloMat && animatedObjects.aiVisorMat && animatedObjects.aiScreenMat) {
        let hex = 0xf59e0b; // MANUAL amber
        if (animatedObjects.aiState.emergencyStopped || !animatedObjects.aiState.enabled) {
          hex = 0xef4444; // OFF / EMERGENCY STOP crimson
        } else if (animatedObjects.aiState.mode === 'AUTO') {
          hex = 0x10b981; // AUTO emerald
        } else if (animatedObjects.aiState.mode === 'ASSIST') {
          hex = 0x38bdf8; // ASSIST cyan
        }
        animatedObjects.aiHaloMat.color.setHex(hex);
        animatedObjects.aiHaloMat.emissive.setHex(hex);
        animatedObjects.aiVisorMat.color.setHex(hex);
        animatedObjects.aiVisorMat.emissive.setHex(hex);
        animatedObjects.aiScreenMat.emissive.setHex(hex);
      }
    },
    update(time) {
      // Gentle multi-wave pool shimmer
      if (animatedObjects.waterMesh && animatedObjects.waterBasePositions) {
        const posAttr = animatedObjects.waterMesh.geometry.attributes.position;
        const base = animatedObjects.waterBasePositions;
        for (let i = 0; i < posAttr.count; i++) {
          const bx = base[i * 3];
          const bz = base[i * 3 + 2];
          const wave = Math.sin(bx * 0.85 + time * 2.1) * 0.018 + Math.cos(bz * 1.1 + time * 1.7) * 0.014;
          posAttr.setY(i, wave);
        }
        posAttr.needsUpdate = true;
        animatedObjects.waterMesh.material.emissiveIntensity = 0.36 + Math.sin(time * 2.8) * 0.08;
      }

      // Swimmer water ripple effect
      if (animatedObjects.rippleRing) {
        if (animatedObjects.rippleActive) {
          animatedObjects.rippleRing.visible = true;
          const phase = (time * 0.9) % 1;
          const scale = 0.85 + phase * 1.6;
          animatedObjects.rippleRing.position.set(
            animatedObjects.ripplePos.x,
            POOL_BOUNDS.waterY + 0.03,
            animatedObjects.ripplePos.y
          );
          animatedObjects.rippleRing.scale.set(scale, 1, scale);
          animatedObjects.rippleRing.material.opacity = (1 - phase) * 0.55;
        } else {
          animatedObjects.rippleRing.visible = false;
        }
      }

      // Stove burners, simmering pots & rising steam when cooking is active
      const cookingOn = animatedObjects.isCookingActive;
      animatedObjects.stoveMats.forEach((mat, idx) => {
        mat.emissiveIntensity = cookingOn ? (1.05 + Math.sin(time * 6.0 + idx) * 0.35) : 0.14;
      });
      animatedObjects.cookingPots.forEach((entry) => {
        entry.mesh.position.y = cookingOn
          ? entry.baseY + Math.abs(Math.sin(time * 7.5 + entry.phase)) * 0.025
          : entry.baseY;
      });
      animatedObjects.steamRings.forEach((entry) => {
        entry.mesh.visible = cookingOn;
        if (cookingOn) {
          const p = (time * 0.85 + entry.phase) % 1;
          entry.mesh.position.y = entry.baseY + p * 0.48;
          const s = 0.7 + p * 0.9;
          entry.mesh.scale.set(s, s, 1);
          entry.mat.opacity = (1 - p) * 0.52;
        }
      });

      // Server rack blinking LEDs + ChatGPT AI Operator animation inside Control Room
      if (adminGroup.visible) {
        animatedObjects.serverLeds.forEach((ledMat, idx) => {
          ledMat.emissiveIntensity = 0.4 + Math.max(0, Math.sin(time * (3.5 + idx * 0.6))) * 1.1;
        });
        const aiCfg = animatedObjects.aiState || { enabled: true, mode: 'ASSIST', emergencyStopped: false };
        const aiActive = aiCfg.enabled && !aiCfg.emergencyStopped;
        if (animatedObjects.aiHead) {
          animatedObjects.aiHead.rotation.y = aiActive ? Math.sin(time * 1.4) * 0.16 : 0;
          animatedObjects.aiHead.rotation.x = aiActive ? Math.sin(time * 2.1) * 0.06 : 0.28;
        }
        if (animatedObjects.aiLeftArm && animatedObjects.aiRightArm) {
          const speed = aiCfg.mode === 'AUTO' ? 8.5 : 4.2;
          const amp = aiActive ? 0.12 : 0.0;
          animatedObjects.aiLeftArm.rotation.x = Math.sin(time * speed) * amp;
          animatedObjects.aiRightArm.rotation.x = Math.cos(time * speed * 1.1) * amp;
        }
        if (animatedObjects.aiHaloMat) {
          animatedObjects.aiHaloMat.emissiveIntensity = aiActive
            ? 0.9 + Math.sin(time * 3.8) * 0.45
            : 0.35 + Math.abs(Math.sin(time * 5.0)) * 0.9;
        }
      }

      // House Speakers announcement halo pulse + Finale Trophy spin
      if (animatedObjects.speakerPulseRemaining > 0) {
        animatedObjects.speakerPulseRemaining = Math.max(0, animatedObjects.speakerPulseRemaining - 0.016);
      }
      const spActive = animatedObjects.speakerPulseRemaining > 0;
      animatedObjects.speakerMats.forEach((mat, idx) => {
        if (spActive) {
          mat.emissive.setHex(0x22d3ee);
          mat.emissiveIntensity = 1.1 + Math.abs(Math.sin(time * 9.0 + idx)) * 1.4;
        } else {
          mat.emissive.setHex(0xeab308);
          mat.emissiveIntensity = 0.32;
        }
      });
      if (animatedObjects.finaleGroup && animatedObjects.finaleGroup.visible && animatedObjects.finaleTrophy) {
        animatedObjects.finaleTrophy.rotation.y = time * 1.15;
      }
    },
    triggerSpeakerPulse(durationSec = 4.5) {
      animatedObjects.speakerPulseRemaining = Math.max(animatedObjects.speakerPulseRemaining, durationSec);
    },
    setFinaleActive(active) {
      if (animatedObjects.finaleGroup) {
        animatedObjects.finaleGroup.visible = Boolean(active);
      }
    }
  };
}
