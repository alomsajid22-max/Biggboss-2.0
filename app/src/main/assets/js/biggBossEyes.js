import * as THREE from '../vendor/three/three.module.min.js';

function createIrisTexture() {
  const canvas = document.createElement('canvas');
  canvas.width = 512;
  canvas.height = 512;
  const ctx = canvas.getContext('2d');
  const cx = 256;
  const cy = 256;

  // Deep obsidian sclera-to-iris rim shadow
  ctx.fillStyle = '#070b14';
  ctx.fillRect(0, 0, 512, 512);

  // Radial reality-show amber/cyan luminous iris gradient
  const grad = ctx.createRadialGradient(cx, cy, 42, cx, cy, 240);
  grad.addColorStop(0.0, '#020409');
  grad.addColorStop(0.26, '#050811');
  grad.addColorStop(0.30, '#facc15');
  grad.addColorStop(0.54, '#06b6d4');
  grad.addColorStop(0.82, '#0f3f60');
  grad.addColorStop(0.96, '#071422');
  grad.addColorStop(1.0, '#02060d');
  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(cx, cy, 244, 0, Math.PI * 2);
  ctx.fill();

  // Radial futuristic iris filaments
  for (let i = 0; i < 96; i++) {
    const a = (i / 96) * Math.PI * 2;
    const r0 = 78 + (i % 3) * 10;
    const r1 = 215 - (i % 5) * 12;
    ctx.strokeStyle = i % 4 === 0
      ? 'rgba(250, 204, 21, 0.58)'
      : 'rgba(103, 232, 249, 0.44)';
    ctx.lineWidth = i % 3 === 0 ? 2.5 : 1.3;
    ctx.beginPath();
    ctx.moveTo(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0);
    ctx.lineTo(cx + Math.cos(a + 0.03) * r1, cy + Math.sin(a + 0.03) * r1);
    ctx.stroke();
  }

  // Concentric HUD reticle ring
  ctx.strokeStyle = 'rgba(234, 185, 84, 0.72)';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.arc(cx, cy, 132, 0, Math.PI * 2);
  ctx.stroke();

  // Crisp glossy pupil highlight
  ctx.fillStyle = '#03050a';
  ctx.beginPath();
  ctx.arc(cx, cy, 66, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = 'rgba(255, 255, 255, 0.78)';
  ctx.beginPath();
  ctx.ellipse(cx - 28, cy - 30, 18, 12, -0.5, 0, Math.PI * 2);
  ctx.fill();

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

/**
 * Creates a sculpted, futuristic almond-framed Bigg Boss 3D Eye installation
 * with animated curved eyelids (natural blinking) and saccadic iris gaze scanning.
 */
export function buildBiggBossEyes(scene, lighting = null) {
  const root = new THREE.Group();
  const irisTex = createIrisTexture();

  // Shared lightweight materials for Android mobile GPUs
  const frameGoldMat = new THREE.MeshStandardMaterial({
    color: 0xe2b14c,
    roughness: 0.24,
    metalness: 0.86
  });
  const frameDarkMat = new THREE.MeshStandardMaterial({
    color: 0x151c2c,
    roughness: 0.42,
    metalness: 0.65
  });
  const scleraMat = new THREE.MeshStandardMaterial({
    color: 0xe6f6ff,
    emissive: 0x38bdf8,
    emissiveIntensity: 0.35,
    roughness: 0.22
  });
  const irisMat = new THREE.MeshStandardMaterial({
    map: irisTex,
    emissive: 0xffffff,
    emissiveMap: irisTex,
    emissiveIntensity: 0.95,
    roughness: 0.16,
    metalness: 0.18
  });
  const neonRimMat = new THREE.MeshStandardMaterial({
    color: 0x67e8f9,
    emissive: 0x06b6d4,
    emissiveIntensity: 1.15,
    roughness: 0.2
  });
  const eyelidMat = new THREE.MeshStandardMaterial({
    color: 0x1c253b,
    roughness: 0.38,
    metalness: 0.58
  });
  const stonePedestalMat = new THREE.MeshStandardMaterial({
    color: 0x334155,
    roughness: 0.78,
    metalness: 0.08
  });
  const vineMat = new THREE.MeshStandardMaterial({
    color: 0x2e6f40,
    roughness: 0.72
  });
  const leafMat = new THREE.MeshStandardMaterial({
    color: 0x4ade80,
    emissive: 0x15803d,
    emissiveIntensity: 0.25,
    roughness: 0.65
  });

  // Shared geometries
  const eyeballGeo = new THREE.SphereGeometry(0.62, 24, 18);
  const irisDiscGeo = new THREE.CircleGeometry(0.41, 28);
  const upperLidGeo = new THREE.SphereGeometry(0.655, 22, 12, 0, Math.PI * 2, 0, Math.PI * 0.48);
  const lowerLidGeo = new THREE.SphereGeometry(0.655, 22, 12, 0, Math.PI * 2, Math.PI * 0.52, Math.PI * 0.48);

  function createEyeSculpture(scale = 1.0, phaseOffset = 0, withAccentLight = true) {
    const eyeGroup = new THREE.Group();
    eyeGroup.scale.setScalar(scale);

    // Sculpted backplate
    const backPlate = new THREE.Mesh(new THREE.CylinderGeometry(1.18, 1.24, 0.12, 24), frameDarkMat);
    backPlate.rotation.x = Math.PI / 2;
    backPlate.scale.set(1.38, 1.0, 0.84);
    backPlate.position.z = -0.14;
    eyeGroup.add(backPlate);

    // Almond architectural gold outer ring (stretched horizontally into iconic eye silhouette)
    const outerRim = new THREE.Mesh(new THREE.TorusGeometry(0.95, 0.075, 12, 36), frameGoldMat);
    outerRim.scale.set(1.42, 0.84, 0.85);
    outerRim.position.z = 0.06;
    eyeGroup.add(outerRim);

    // Inner glowing cyan telemetry ring
    const glowRim = new THREE.Mesh(new THREE.TorusGeometry(0.84, 0.032, 10, 36), neonRimMat);
    glowRim.scale.set(1.38, 0.80, 0.85);
    glowRim.position.z = 0.10;
    eyeGroup.add(glowRim);

    // Side wing filigree accents
    [-1, 1].forEach((side) => {
      const fin = new THREE.Mesh(new THREE.ConeGeometry(0.11, 0.46, 6), frameGoldMat);
      fin.rotation.z = side * Math.PI * 0.5;
      fin.position.set(side * 1.48, 0, 0.04);
      eyeGroup.add(fin);
    });

    // Sclera sphere (clipped cleanly inside backplate)
    const sclera = new THREE.Mesh(eyeballGeo, scleraMat);
    sclera.scale.set(1.12, 0.94, 0.58);
    sclera.position.z = 0.05;
    eyeGroup.add(sclera);

    // Pivoting gaze core (smoothly tracks + scans around the room)
    const gazePivot = new THREE.Group();
    gazePivot.position.z = 0.05;
    const irisMesh = new THREE.Mesh(irisDiscGeo, irisMat);
    irisMesh.position.z = 0.37;
    gazePivot.add(irisMesh);
    eyeGroup.add(gazePivot);

    // Upper & lower curved eyelids pivoting on X axis
    const upperLidPivot = new THREE.Group();
    upperLidPivot.position.z = 0.05;
    const upperLid = new THREE.Mesh(upperLidGeo, eyelidMat);
    upperLid.scale.set(1.14, 0.96, 0.62);
    upperLidPivot.add(upperLid);
    const upperTrim = new THREE.Mesh(new THREE.TorusGeometry(0.64, 0.024, 8, 24, Math.PI), frameGoldMat);
    upperTrim.scale.set(1.14, 0.62, 0.62);
    upperTrim.rotation.x = Math.PI / 2;
    upperLidPivot.add(upperTrim);
    eyeGroup.add(upperLidPivot);

    const lowerLidPivot = new THREE.Group();
    lowerLidPivot.position.z = 0.05;
    const lowerLid = new THREE.Mesh(lowerLidGeo, eyelidMat);
    lowerLid.scale.set(1.14, 0.96, 0.62);
    lowerLidPivot.add(lowerLid);
    eyeGroup.add(lowerLidPivot);

    // Open resting angles: upper rotated up (-0.98 rad), lower rotated down (+0.98 rad)
    upperLidPivot.rotation.x = -0.98;
    lowerLidPivot.rotation.x = 0.98;

    let accentLight = null;
    if (withAccentLight) {
      accentLight = new THREE.PointLight(0x38bdf8, 0.5, 9.5, 1.8);
      accentLight.position.set(0, 0, 0.9);
      eyeGroup.add(accentLight);
    }

    return {
      group: eyeGroup,
      gazePivot,
      upperLidPivot,
      lowerLidPivot,
      accentLight,
      phaseOffset,
      blinkTimer: 2.4 + phaseOffset,
      blinkProgress: 0,
      isBlinking: false,
      gazeYaw: 0,
      gazePitch: 0,
      targetGazeYaw: 0,
      targetGazePitch: 0,
      gazeSwitchTimer: 1.5 + phaseOffset
    };
  }

  // =========================================================
  // EYE 1: CENTRAL LIVING ATRIUM SOLID WEST WALL (X = -9.78, Y = 3.25, Z = -5.2)
  // Positioned cleanly on the 6m solid wall section away from Bedroom doorway & light switch.
  // =========================================================
  const livingEye = createEyeSculpture(1.08, 0.0, true);
  livingEye.group.position.set(-9.76, 3.30, -5.20);
  livingEye.group.rotation.y = Math.PI / 2; // Faces +X into Living Atrium
  root.add(livingEye.group);

  // =========================================================
  // EYE 2: INDOOR BOTANICAL GARDEN MONOLITH (Inside southeast lush flowerbed collider)
  // Integrated organically with stone pedestal, twisting vines & foliage facing Garden path.
  // =========================================================
  const gardenPedestal = new THREE.Group();
  gardenPedestal.position.set(23.6, 0.45, 19.1);
  // Rotate northwest toward Garden walkway & Pool/Activity doors
  gardenPedestal.rotation.y = Math.atan2(-1, -1);

  const plinth = new THREE.Mesh(new THREE.CylinderGeometry(0.76, 0.96, 1.45, 12), stonePedestalMat);
  plinth.position.y = 0.725;
  plinth.castShadow = true;
  gardenPedestal.add(plinth);

  const collar = new THREE.Mesh(new THREE.CylinderGeometry(0.84, 0.78, 0.16, 12), frameGoldMat);
  collar.position.y = 1.48;
  gardenPedestal.add(collar);

  // Twisting organic vines embracing the Eye totem
  for (let v = 0; v < 3; v++) {
    const pts = [];
    const baseAngle = (v * Math.PI * 2) / 3;
    for (let s = 0; s <= 12; s++) {
      const t = s / 12;
      const ang = baseAngle + t * Math.PI * 1.65;
      const rad = 0.86 - t * 0.12;
      pts.push(new THREE.Vector3(Math.cos(ang) * rad, t * 2.15, Math.sin(ang) * rad));
    }
    const vineCurve = new THREE.CatmullRomCurve3(pts);
    const vineGeo = new THREE.TubeGeometry(vineCurve, 16, 0.045, 6, false);
    const vineMesh = new THREE.Mesh(vineGeo, vineMat);
    gardenPedestal.add(vineMesh);

    const leafTip = new THREE.Mesh(new THREE.DodecahedronGeometry(0.14, 0), leafMat);
    const endPt = pts[pts.length - 1];
    leafTip.position.copy(endPt);
    gardenPedestal.add(leafTip);
  }

  const gardenEye = createEyeSculpture(0.95, 1.7, true);
  gardenEye.group.position.set(0, 2.38, 0.08);
  gardenPedestal.add(gardenEye.group);
  root.add(gardenPedestal);

  scene.add(root);

  const eyes = [livingEye, gardenEye];

  function updateEye(eye, dt, elapsed, modeId) {
    // 1. Saccadic smooth gaze scanning + subtle curiosity tilt
    eye.gazeSwitchTimer -= dt;
    if (eye.gazeSwitchTimer <= 0) {
      eye.gazeSwitchTimer = 1.6 + Math.random() * 2.6;
      eye.targetGazeYaw = (Math.random() - 0.5) * 0.56;
      eye.targetGazePitch = (Math.random() - 0.5) * 0.24 - 0.06;
    }
    const followSpeed = Math.min(1, dt * 5.2);
    eye.gazeYaw += (eye.targetGazeYaw - eye.gazeYaw) * followSpeed;
    eye.gazePitch += (eye.targetGazePitch - eye.gazePitch) * followSpeed;

    eye.gazePivot.rotation.y = eye.gazeYaw;
    eye.gazePivot.rotation.x = eye.gazePitch;

    // 2. Smooth natural blinking
    if (!eye.isBlinking) {
      eye.blinkTimer -= dt;
      if (eye.blinkTimer <= 0) {
        eye.isBlinking = true;
        eye.blinkProgress = 0;
      }
    }
    let blinkAmt = 0;
    if (eye.isBlinking) {
      eye.blinkProgress += dt * 4.4;
      if (eye.blinkProgress >= 1) {
        eye.isBlinking = false;
        eye.blinkTimer = 2.8 + Math.random() * 3.4;
        blinkAmt = 0;
      } else {
        blinkAmt = Math.sin(eye.blinkProgress * Math.PI);
      }
    }
    const openAngle = 0.98 * (1 - blinkAmt * 0.96);
    eye.upperLidPivot.rotation.x = -openAngle;
    eye.lowerLidPivot.rotation.x = openAngle;

    // 3. Respond to Day / Dusk / Night lighting state
    const targetIrisEmissive = modeId === 'night' ? 1.85 : (modeId === 'dusk' ? 1.32 : 0.85);
    const targetRimEmissive = modeId === 'night' ? 1.95 : (modeId === 'dusk' ? 1.40 : 0.90);
    const targetLightInt = modeId === 'night' ? 1.35 : (modeId === 'dusk' ? 0.85 : 0.35);
    const pulse = 1 + Math.sin(elapsed * 2.2 + eye.phaseOffset) * 0.08;

    irisMat.emissiveIntensity = targetIrisEmissive * pulse;
    neonRimMat.emissiveIntensity = targetRimEmissive * pulse;
    if (eye.accentLight) {
      eye.accentLight.intensity = targetLightInt * pulse;
    }
  }

  return {
    root,
    eyes,
    update(dt, elapsed) {
      const modeId = lighting && typeof lighting.getCurrentMode === 'function'
        ? lighting.getCurrentMode().id
        : 'day';
      for (const eye of eyes) {
        updateEye(eye, dt, elapsed, modeId);
      }
    }
  };
}
