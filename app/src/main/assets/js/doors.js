import * as THREE from '../vendor/three/three.module.min.js';

export function buildGlassSlidingDoors(scene, doorSpecs, colliders) {
  const doors = [];

  const frameMat = new THREE.MeshStandardMaterial({
    color: 0x242a38,
    roughness: 0.32,
    metalness: 0.8
  });

  const handleMat = new THREE.MeshStandardMaterial({
    color: 0xeab954,
    roughness: 0.24,
    metalness: 0.88
  });

  const safetyBandMat = new THREE.MeshStandardMaterial({
    color: 0xffffff,
    transparent: true,
    opacity: 0.35,
    roughness: 0.4
  });

  for (const spec of doorSpecs) {
    const doorGroup = new THREE.Group();
    doorGroup.position.set(spec.x, 0, spec.z);
    if (spec.axis === 'z') {
      doorGroup.rotation.y = Math.PI / 2;
    }

    const w = spec.width;
    const h = spec.height;
    const isRestrictedAdmin = spec.requiredRole === 'ADMIN';

    // Top sliding track rail
    const trackLen = spec.doubleLeaf ? w * 1.95 : w * 1.9;
    const track = new THREE.Mesh(new THREE.BoxGeometry(trackLen, 0.1, 0.18), frameMat);
    track.position.set(0, h + 0.02, 0);
    doorGroup.add(track);

    // Restricted Admin door starts as completely opaque privacy glass so players cannot see inside
    const glassMat = new THREE.MeshStandardMaterial({
      color: isRestrictedAdmin ? 0x1b2232 : (spec.tint || 0xb6e3f4),
      transparent: !isRestrictedAdmin,
      opacity: isRestrictedAdmin ? 1.0 : (spec.frosted ? 0.56 : 0.34),
      roughness: isRestrictedAdmin ? 0.42 : (spec.frosted ? 0.45 : 0.08),
      metalness: isRestrictedAdmin ? 0.35 : 0.22,
      depthWrite: isRestrictedAdmin
    });

    const leaves = [];
    const stripes = [];
    const handles = [];

    const createLeaf = (leafW, offsetX, slideDir) => {
      const leaf = new THREE.Group();
      leaf.position.set(offsetX, 0, 0);

      // Glass panel
      const pane = new THREE.Mesh(new THREE.BoxGeometry(leafW - 0.12, h - 0.12, 0.05), glassMat);
      pane.position.y = h / 2;
      leaf.add(pane);

      // Frosted privacy/safety stripe
      const stripe = new THREE.Mesh(new THREE.BoxGeometry(leafW - 0.14, 0.18, 0.055), safetyBandMat);
      stripe.position.y = h * 0.44;
      stripe.visible = !isRestrictedAdmin;
      leaf.add(stripe);
      stripes.push(stripe);

      // Slim aluminum perimeter rails
      const vBarGeo = new THREE.BoxGeometry(0.07, h, 0.08);
      const leftBar = new THREE.Mesh(vBarGeo, frameMat);
      leftBar.position.set(-leafW / 2 + 0.035, h / 2, 0);
      leaf.add(leftBar);

      const rightBar = new THREE.Mesh(vBarGeo, frameMat);
      rightBar.position.set(leafW / 2 - 0.035, h / 2, 0);
      leaf.add(rightBar);

      const hBarGeo = new THREE.BoxGeometry(leafW, 0.08, 0.08);
      const topBar = new THREE.Mesh(hBarGeo, frameMat);
      topBar.position.y = h - 0.04;
      leaf.add(topBar);

      const botBar = new THREE.Mesh(hBarGeo, frameMat);
      botBar.position.y = 0.04;
      leaf.add(botBar);

      // Vertical gold pull handle on both faces (hidden on locked privacy panel for normal players)
      const handleX = slideDir < 0 ? leafW / 2 - 0.2 : -leafW / 2 + 0.2;
      const handle = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.022, 0.72, 8), handleMat);
      handle.position.set(handleX, 1.18, 0.065);
      handle.visible = !isRestrictedAdmin;
      leaf.add(handle);
      const handleBack = handle.clone();
      handleBack.position.z = -0.065;
      handleBack.visible = !isRestrictedAdmin;
      leaf.add(handleBack);
      handles.push(handle, handleBack);

      doorGroup.add(leaf);
      leaves.push({
        mesh: leaf,
        closedX: offsetX,
        openX: offsetX + slideDir * (leafW * 0.88)
      });
    };

    if (spec.doubleLeaf) {
      const leafW = w / 2;
      createLeaf(leafW, -leafW / 2, -1);
      createLeaf(leafW, leafW / 2, 1);
    } else {
      createLeaf(w, 0, 1);
    }

    scene.add(doorGroup);

    // Dynamic collider that disables when door is open > 0.65 (and stays locked when unauthorized)
    const halfW = w / 2;
    const thickness = 0.28;
    const collider = spec.axis === 'x'
      ? {
          minX: spec.x - halfW,
          maxX: spec.x + halfW,
          minZ: spec.z - thickness,
          maxZ: spec.z + thickness,
          active: true,
          tag: 'door'
        }
      : {
          minX: spec.x - thickness,
          maxX: spec.x + thickness,
          minZ: spec.z - halfW,
          maxZ: spec.z + halfW,
          active: true,
          tag: 'door'
        };
    colliders.push(collider);

    const doorObj = {
      id: spec.id,
      label: spec.label,
      requiredRole: spec.requiredRole || null,
      requiredVip: Boolean(spec.requiredVip),
      x: spec.x,
      z: spec.z,
      isOpen: false,
      progress: 0,
      leaves,
      collider,
      setAdminUnlocked(isAdmin) {
        if (!isRestrictedAdmin) return;
        const unlocked = Boolean(isAdmin);
        glassMat.color.setHex(unlocked ? (spec.tint || 0x7dd3fc) : 0x1b2232);
        glassMat.transparent = unlocked;
        glassMat.opacity = unlocked ? 0.34 : 1.0;
        glassMat.roughness = unlocked ? 0.08 : 0.42;
        glassMat.metalness = unlocked ? 0.22 : 0.35;
        glassMat.depthWrite = !unlocked;
        glassMat.needsUpdate = true;
        stripes.forEach((s) => { s.visible = unlocked; });
        handles.forEach((hnd) => { hnd.visible = unlocked; });
        if (!unlocked) {
          this.isOpen = false;
          this.progress = 0;
          this.collider.active = true;
          for (const leaf of this.leaves) {
            leaf.mesh.position.x = leaf.closedX;
          }
        }
      },
      setVipUnlocked(canAccess) {
        if (!this.requiredVip) return;
        const unlocked = Boolean(canAccess);
        glassMat.color.setHex(unlocked ? (spec.tint || 0xfde68a) : 0x453414);
        glassMat.opacity = unlocked ? 0.34 : 0.72;
        glassMat.needsUpdate = true;
        if (!unlocked) {
          this.isOpen = false;
          this.progress = 0;
          this.collider.active = true;
          for (const leaf of this.leaves) {
            leaf.mesh.position.x = leaf.closedX;
          }
        }
      },
      toggle(authSession) {
        if (this.requiredRole === 'ADMIN') {
          const perm = authSession && typeof authSession.assertAdminPermission === 'function'
            ? authSession.assertAdminPermission(`DOOR_${this.id}`)
            : { allowed: false, code: 'ACCESS_DENIED', error: 'Access denied: ADMIN role required.' };
          if (!perm.allowed) {
            this.isOpen = false;
            return { ok: false, denied: true, isOpen: false, reason: perm.error };
          }
        }
        if (this.requiredVip) {
          const perm = authSession && typeof authSession.assertVipPermission === 'function'
            ? authSession.assertVipPermission(this.label)
            : { allowed: false, code: 'VIP_ACCESS_DENIED', error: 'VIP Room Locked: House Captain or VIP Pass required.' };
          if (!perm.allowed) {
            this.isOpen = false;
            return { ok: false, denied: true, isOpen: false, reason: perm.error };
          }
        }
        this.isOpen = !this.isOpen;
        return { ok: true, denied: false, isOpen: this.isOpen };
      }
    };

    doors.push(doorObj);
  }

  return {
    doors,
    syncRoleVisibility(authSession) {
      const isAdmin = Boolean(authSession && authSession.isAdmin());
      const canAccessVip = Boolean(authSession && typeof authSession.canAccessVipRoom === 'function' && authSession.canAccessVipRoom());
      for (const d of doors) {
        if (d.requiredRole === 'ADMIN') {
          d.setAdminUnlocked(isAdmin);
        }
        if (d.requiredVip) {
          d.setVipUnlocked(canAccessVip);
        }
      }
    },
    attemptToggleDoor(doorId, authSession) {
      const door = doors.find((d) => d.id === doorId);
      if (!door) return { ok: false, denied: false, reason: 'Door not found.' };
      return door.toggle(authSession);
    },
    applyRemoteDoorStates(remoteDoorsMap, authSession) {
      if (!remoteDoorsMap || typeof remoteDoorsMap !== 'object') return;
      const isAdmin = Boolean(authSession && authSession.isAdmin());
      const canAccessVip = Boolean(authSession && typeof authSession.canAccessVipRoom === 'function' && authSession.canAccessVipRoom());
      for (const d of doors) {
        if (d.requiredRole === 'ADMIN' && !isAdmin) {
          d.isOpen = false;
          continue;
        }
        if (d.requiredVip && !canAccessVip) {
          d.isOpen = false;
          continue;
        }
        if (Object.prototype.hasOwnProperty.call(remoteDoorsMap, d.id)) {
          d.isOpen = Boolean(remoteDoorsMap[d.id]);
        }
      }
    },
    update(dt) {
      const speed = 3.6;
      for (const d of doors) {
        const target = d.isOpen ? 1 : 0;
        if (Math.abs(d.progress - target) > 0.001) {
          d.progress += (target - d.progress) * Math.min(1, dt * speed * 2.2);
          if (Math.abs(d.progress - target) < 0.005) d.progress = target;
          const t = d.progress * d.progress * (3 - 2 * d.progress);
          for (const leaf of d.leaves) {
            leaf.mesh.position.x = leaf.closedX + (leaf.openX - leaf.closedX) * t;
          }
        }
        d.collider.active = d.progress < 0.65;
      }
    },
    getNearestDoor(px, pz, maxDist = 3.1, authSession = null) {
      const isAdmin = Boolean(authSession && authSession.isAdmin());
      let best = null;
      let bestDist = maxDist;
      for (const d of doors) {
        if (d.requiredRole === 'ADMIN' && !isAdmin) continue;
        const dist = Math.hypot(px - d.x, pz - d.z);
        if (dist < bestDist) {
          bestDist = dist;
          best = d;
        }
      }
      return best;
    }
  };
}
