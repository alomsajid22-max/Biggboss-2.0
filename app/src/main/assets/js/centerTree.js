import * as THREE from '../vendor/three/three.module.min.js';

export function buildCenterTree(scene, colliders) {
  const group = new THREE.Group();
  group.position.set(0, 0, 0);

  // 1. Raised Luxury Circular Travertine & Brass Planter
  const planterMat = new THREE.MeshStandardMaterial({ color: 0xd8cfc2, roughness: 0.45, metalness: 0.08 });
  const brassMat = new THREE.MeshStandardMaterial({ color: 0xe2b14c, roughness: 0.28, metalness: 0.85 });
  const soilMat = new THREE.MeshStandardMaterial({ color: 0x3b2a1e, roughness: 0.95 });

  const outerRim = new THREE.Mesh(new THREE.CylinderGeometry(1.95, 2.05, 0.56, 32), planterMat);
  outerRim.position.y = 0.28;
  outerRim.castShadow = true;
  outerRim.receiveShadow = true;
  group.add(outerRim);

  const brassTrim = new THREE.Mesh(new THREE.TorusGeometry(1.96, 0.05, 10, 40), brassMat);
  brassTrim.rotation.x = Math.PI / 2;
  brassTrim.position.y = 0.56;
  group.add(brassTrim);

  const soilDisc = new THREE.Mesh(new THREE.CylinderGeometry(1.78, 1.78, 0.5, 28), soilMat);
  soilDisc.position.y = 0.29;
  group.add(soilDisc);

  // Glowing LED Uplight Ring inside Planter
  const glowRingMat = new THREE.MeshStandardMaterial({
    color: 0xffdf8e,
    emissive: 0xffc65c,
    emissiveIntensity: 1.4,
    roughness: 0.2
  });
  const glowRing = new THREE.Mesh(new THREE.TorusGeometry(1.66, 0.04, 8, 36), glowRingMat);
  glowRing.rotation.x = Math.PI / 2;
  glowRing.position.y = 0.56;
  group.add(glowRing);

  // Collider for planter
  colliders.push({
    minX: -1.98,
    maxX: 1.98,
    minZ: -1.98,
    maxZ: 1.98,
    isCircle: true,
    cx: 0,
    cz: 0,
    radius: 2.02,
    tag: 'center_tree'
  });

  // 2. Sculpted Gnarled Trunk & Sweeping Branches
  const barkMat = new THREE.MeshStandardMaterial({
    color: 0x5c3e2a,
    roughness: 0.9,
    metalness: 0.02
  });

  const trunkCurve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0.4, 0),
    new THREE.Vector3(0.14, 1.4, -0.08),
    new THREE.Vector3(-0.12, 2.5, 0.1),
    new THREE.Vector3(0.05, 3.6, 0)
  ]);
  const trunkGeo = new THREE.TubeGeometry(trunkCurve, 20, 0.42, 12, false);
  // Taper trunk vertices smoothly
  const posAttr = trunkGeo.attributes.position;
  for (let i = 0; i < posAttr.count; i++) {
    const y = posAttr.getY(i);
    const t = Math.max(0, Math.min(1, (y - 0.4) / 3.2));
    const scale = 1.15 - t * 0.48;
    const cx = trunkCurve.getPoint(t).x;
    const cz = trunkCurve.getPoint(t).z;
    posAttr.setX(i, cx + (posAttr.getX(i) - cx) * scale);
    posAttr.setZ(i, cz + (posAttr.getZ(i) - cz) * scale);
  }
  trunkGeo.computeVertexNormals();
  const trunkMesh = new THREE.Mesh(trunkGeo, barkMat);
  trunkMesh.castShadow = true;
  trunkMesh.receiveShadow = true;
  group.add(trunkMesh);

  // 5 Main Branches radiating into the Atrium Canopy
  const branchSpecs = [
    [new THREE.Vector3(0.02, 3.2, 0), new THREE.Vector3(1.1, 4.1, 0.7), new THREE.Vector3(2.3, 4.85, 1.2)],
    [new THREE.Vector3(0.02, 3.3, 0), new THREE.Vector3(-1.2, 4.2, 0.8), new THREE.Vector3(-2.4, 4.9, 1.3)],
    [new THREE.Vector3(0.02, 3.4, 0), new THREE.Vector3(1.0, 4.3, -1.0), new THREE.Vector3(2.1, 5.0, -1.7)],
    [new THREE.Vector3(0.02, 3.25, 0), new THREE.Vector3(-1.1, 4.1, -1.0), new THREE.Vector3(-2.2, 4.8, -1.8)],
    [new THREE.Vector3(0.02, 3.5, 0), new THREE.Vector3(0.1, 4.5, 0.1), new THREE.Vector3(0.0, 5.3, -0.1)]
  ];

  const branchCurves = [];
  branchSpecs.forEach((pts) => {
    const curve = new THREE.CatmullRomCurve3(pts);
    branchCurves.push(curve);
    const bGeo = new THREE.TubeGeometry(curve, 12, 0.14, 8, false);
    const bMesh = new THREE.Mesh(bGeo, barkMat);
    bMesh.castShadow = true;
    group.add(bMesh);
  });

  // 3. Twisted Snake-Like Decorative Vines Coiling Around Trunk & Branches
  const vineMat = new THREE.MeshStandardMaterial({
    color: 0x2d6a4f,
    emissive: 0x0b2e1d,
    emissiveIntensity: 0.45,
    roughness: 0.45,
    metalness: 0.18
  });

  const goldVineMat = new THREE.MeshStandardMaterial({
    color: 0x52b788,
    emissive: 0x1b4332,
    emissiveIntensity: 0.55,
    roughness: 0.35,
    metalness: 0.25
  });

  const blossomMat = new THREE.MeshStandardMaterial({
    color: 0xffe082,
    emissive: 0xffb703,
    emissiveIntensity: 1.5,
    roughness: 0.2
  });

  const blossomGeo = new THREE.SphereGeometry(0.065, 8, 8);

  // 3 helical serpentine vines wrapping around the trunk and climbing onto branches
  for (let v = 0; v < 3; v++) {
    const vinePts = [];
    const phase = (v * Math.PI * 2) / 3;
    const steps = 36;
    for (let s = 0; s <= steps; s++) {
      const t = s / steps;
      const trunkPt = trunkCurve.getPoint(Math.min(t * 1.05, 1));
      const angle = phase + t * Math.PI * 5.2; // multiple tight snake-like coils!
      const coilRadius = (0.48 - t * 0.2) + Math.sin(t * 14 + v) * 0.04;
      const vx = trunkPt.x + Math.cos(angle) * coilRadius;
      const vz = trunkPt.z + Math.sin(angle) * coilRadius;
      const vy = 0.52 + t * 3.15;
      vinePts.push(new THREE.Vector3(vx, vy, vz));
    }

    // Extend each snake-like vine along one of the main branches
    const targetBranch = branchCurves[v];
    for (let b = 1; b <= 10; b++) {
      const bt = b / 10;
      const bp = targetBranch.getPoint(bt);
      const bAngle = phase + bt * Math.PI * 3.0;
      vinePts.push(new THREE.Vector3(
        bp.x + Math.cos(bAngle) * 0.19,
        bp.y + Math.sin(bAngle) * 0.16,
        bp.z + Math.sin(bAngle) * 0.19
      ));
    }

    const vineCurve = new THREE.CatmullRomCurve3(vinePts);
    const vineGeo = new THREE.TubeGeometry(vineCurve, 44, 0.055, 7, false);
    const vineMesh = new THREE.Mesh(vineGeo, v === 1 ? goldVineMat : vineMat);
    group.add(vineMesh);

    // Decorative glowing lantern blossoms along the snake-like vine
    for (let k = 0.18; k <= 0.94; k += 0.15) {
      const pt = vineCurve.getPoint(k);
      const node = new THREE.Mesh(blossomGeo, blossomMat);
      node.position.copy(pt);
      group.add(node);
    }
  }

  // 4. Lush Layered Canopy Clusters
  const leafMats = [
    new THREE.MeshStandardMaterial({ color: 0x2d6a4f, roughness: 0.72 }),
    new THREE.MeshStandardMaterial({ color: 0x40916c, roughness: 0.68 }),
    new THREE.MeshStandardMaterial({ color: 0x52b788, roughness: 0.65 })
  ];
  const puffGeo = new THREE.DodecahedronGeometry(0.85, 1);

  const canopyNodes = [
    [2.3, 4.9, 1.2, 1.25], [1.5, 5.1, 0.5, 1.05], [2.8, 4.5, 1.6, 0.9],
    [-2.4, 4.95, 1.3, 1.25], [-1.6, 5.15, 0.7, 1.0], [-2.9, 4.55, 1.6, 0.88],
    [2.1, 5.05, -1.7, 1.2], [1.3, 5.2, -0.9, 1.0], [2.6, 4.6, -2.1, 0.85],
    [-2.2, 4.85, -1.8, 1.22], [-1.4, 5.1, -1.0, 1.0], [-2.7, 4.5, -2.1, 0.88],
    [0.0, 5.45, 0.0, 1.45], [0.8, 5.3, 0.0, 1.1], [-0.8, 5.3, 0.1, 1.1],
    [0.0, 5.25, 0.9, 1.15], [0.0, 5.25, -0.9, 1.15]
  ];

  canopyNodes.forEach(([x, y, z, s], idx) => {
    const puff = new THREE.Mesh(puffGeo, leafMats[idx % leafMats.length]);
    puff.position.set(x, y, z);
    puff.scale.set(s * 1.15, s * 0.78, s * 1.15);
    puff.rotation.set(idx * 0.4, idx * 0.9, 0);
    puff.castShadow = true;
    puff.receiveShadow = true;
    group.add(puff);
  });

  // 5. Attractive Accent Lights around the Tree
  const treeWarmLight = new THREE.PointLight(0xffcf70, 3.2, 11, 1.5);
  treeWarmLight.position.set(0, 1.35, 0);
  group.add(treeWarmLight);

  const treeEmeraldLight = new THREE.PointLight(0x4ade80, 1.8, 9, 1.6);
  treeEmeraldLight.position.set(0, 3.6, 0);
  group.add(treeEmeraldLight);

  // Four brass uplight bollards tucked cleanly inside planter rim
  const bollardGeo = new THREE.CylinderGeometry(0.06, 0.08, 0.18, 10);
  for (let i = 0; i < 4; i++) {
    const a = (i * Math.PI) / 2 + Math.PI / 4;
    const bollard = new THREE.Mesh(bollardGeo, brassMat);
    bollard.position.set(Math.cos(a) * 1.42, 0.62, Math.sin(a) * 1.42);
    group.add(bollard);
    const lens = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 0.03, 10), blossomMat);
    lens.position.set(Math.cos(a) * 1.42, 0.72, Math.sin(a) * 1.42);
    group.add(lens);
  }

  scene.add(group);

  return {
    group,
    glowRingMat,
    blossomMat,
    treeWarmLight,
    treeEmeraldLight,
    update(time) {
      // Gentle magical pulse on tree lights & vine blossoms
      const pulse = 0.88 + Math.sin(time * 2.1) * 0.12;
      blossomMat.emissiveIntensity = 1.25 * pulse;
      glowRingMat.emissiveIntensity = 1.2 * pulse;
    }
  };
}
