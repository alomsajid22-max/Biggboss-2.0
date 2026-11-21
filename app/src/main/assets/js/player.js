import * as THREE from '../vendor/three/three.module.min.js';
import { HOUSE_BOUNDS } from './worldLayout.js';
import { resolveEnvironmentCollisions } from './characters.js';
import { POOL_BOUNDS } from './furniture.js';

export function createPlayerController(scene, camera, colliders, initialCharacter, furnitureManager = null) {
  let boundChar = initialCharacter;

  const state = {
    camYaw: boundChar ? boundChar.rotY : 0,
    camPitch: 0.34,
    camDist: 4.6,
    camMode: 'THIRD_PERSON', // 'THIRD_PERSON' | 'FIRST_PERSON' | 'FREE'
    camSensitivity: 1.0,
    camSmoothness: 0.72,
    freeCamAllowed: true,
    freeCamPos: new THREE.Vector3(boundChar ? boundChar.x : 0, 2.2, boundChar ? boundChar.z + 3 : 6),
    isRunning: false,
    mode: 'free', // 'free' | 'sit' | 'bed_sit' | 'sleep' | 'swim' | 'dance'
    expression: boundChar ? (boundChar.expression || 'neutral') : 'neutral',
    seatedTarget: null,
    activityMode: null, // 'cook' | 'eat' | 'sit_eat' | 'wash'
    activityTimer: 0,
    speed: 0
  };

  const camTarget = new THREE.Vector3();
  const desiredCamPos = new THREE.Vector3();

  function shortestAngleDiff(a, b) {
    let diff = ((b - a + Math.PI) % (Math.PI * 2)) - Math.PI;
    if (diff < -Math.PI) diff += Math.PI * 2;
    return diff;
  }

  return {
    get position() {
      return { x: boundChar.x, y: boundChar.group ? boundChar.group.position.y : 0, z: boundChar.z };
    },
    get yaw() {
      return boundChar.rotY;
    },
    get mode() {
      return state.mode;
    },
    get expression() {
      return state.expression;
    },
    get isDancing() {
      return state.mode === 'dance';
    },
    get networkPose() {
      if (state.activityMode && state.activityTimer > 0) return state.activityMode;
      if (state.mode === 'swim') return state.speed > 0.3 ? 'swim' : 'swim_idle';
      if (state.mode === 'sit' || state.mode === 'bed_sit' || state.mode === 'sleep' || state.mode === 'dance') {
        return state.mode;
      }
      if (state.speed > 0.25) return state.isRunning ? 'run' : 'walk';
      return 'idle';
    },
    get isSeated() {
      return state.mode === 'sit' || state.mode === 'bed_sit';
    },
    get isSleeping() {
      return state.mode === 'sleep';
    },
    get isSwimming() {
      return state.mode === 'swim';
    },
    get seatedTarget() {
      return state.seatedTarget;
    },
    get activeCharacter() {
      return boundChar;
    },
    bindCharacter(charInstance) {
      if (boundChar && boundChar.bodyPivot) {
        boundChar.bodyPivot.visible = true;
      }
      if (state.seatedTarget) {
        this.standUp();
      }
      if (state.mode === 'swim') {
        this.exitPool();
      }
      boundChar = charInstance;
      state.speed = 0;
      state.mode = 'free';
      state.camYaw = boundChar.rotY;
      state.camPitch = 0.34;
      if (boundChar) {
        boundChar.setExpression(state.expression);
        if (boundChar.bodyPivot) {
          boundChar.bodyPivot.visible = state.camMode !== 'FIRST_PERSON';
        }
      }
    },
    setExpression(expr) {
      state.expression = expr || 'neutral';
      if (boundChar) {
        boundChar.setExpression(state.expression);
      }
    },
    setDancing(enabled) {
      if (state.seatedTarget || state.mode === 'swim') return false;
      state.activityMode = null;
      state.activityTimer = 0;
      state.mode = enabled ? 'dance' : 'free';
      state.speed = 0;
      return state.mode === 'dance';
    },
    startTimedActivity(mode, durationSec = 3.2) {
      if (state.mode === 'swim' || state.mode === 'sleep') return false;
      const valid = ['cook', 'eat', 'sit_eat', 'wash'];
      if (!valid.includes(mode)) return false;
      if (mode === 'sit_eat' && !state.seatedTarget) {
        state.activityMode = 'eat';
      } else {
        if (state.mode === 'dance') state.mode = 'free';
        state.activityMode = mode;
      }
      state.activityTimer = Math.max(0.8, Number(durationSec) || 3.0);
      return true;
    },
    toggleDance() {
      return this.setDancing(state.mode !== 'dance');
    },
    setRunning(val) {
      state.isRunning = Boolean(val);
    },
    isRunning() {
      return state.isRunning;
    },
    orbitCamera(deltaYaw, deltaPitch) {
      const sens = state.camSensitivity;
      state.camYaw -= deltaYaw * sens;
      const minPitch = state.camMode === 'FIRST_PERSON' ? -0.65 : (state.camMode === 'FREE' ? -0.95 : 0.14);
      const maxPitch = state.camMode === 'FIRST_PERSON' ? 0.65 : 0.95;
      state.camPitch = Math.max(minPitch, Math.min(maxPitch, state.camPitch + deltaPitch * sens));
    },
    adjustZoom(delta) {
      state.camDist = Math.max(2.0, Math.min(8.5, state.camDist + delta));
      return this.getCameraPrefs();
    },
    cycleCameraMode() {
      const order = state.freeCamAllowed ? ['THIRD_PERSON', 'FIRST_PERSON', 'FREE'] : ['THIRD_PERSON', 'FIRST_PERSON'];
      const nextIdx = (order.indexOf(state.camMode) + 1) % order.length;
      this.setCameraPrefs({ mode: order[nextIdx] });
      return this.getCameraPrefs();
    },
    setCameraPrefs(prefs = {}) {
      if (!prefs || typeof prefs !== 'object') return this.getCameraPrefs();
      if (typeof prefs.freeCamAllowed === 'boolean') {
        state.freeCamAllowed = prefs.freeCamAllowed;
      }
      if (typeof prefs.mode === 'string') {
        let nextMode = prefs.mode.toUpperCase();
        if (nextMode === 'FREE' && !state.freeCamAllowed) nextMode = 'THIRD_PERSON';
        if (['THIRD_PERSON', 'FIRST_PERSON', 'FREE'].includes(nextMode) && nextMode !== state.camMode) {
          state.camMode = nextMode;
          if (nextMode === 'FREE') {
            state.freeCamPos.set(camera.position.x, Math.max(1.2, Math.min(4.6, camera.position.y)), camera.position.z);
          } else if (nextMode === 'FIRST_PERSON') {
            state.camPitch = 0.0;
          } else {
            state.camPitch = 0.34;
          }
        }
      }
      if (typeof prefs.distance === 'number' && !Number.isNaN(prefs.distance)) {
        state.camDist = Math.max(2.0, Math.min(8.5, prefs.distance));
      }
      if (typeof prefs.sensitivity === 'number' && !Number.isNaN(prefs.sensitivity)) {
        state.camSensitivity = Math.max(0.4, Math.min(2.2, prefs.sensitivity));
      }
      if (typeof prefs.smoothness === 'number' && !Number.isNaN(prefs.smoothness)) {
        state.camSmoothness = Math.max(0.15, Math.min(0.95, prefs.smoothness));
      }
      if (boundChar && boundChar.bodyPivot) {
        boundChar.bodyPivot.visible = state.camMode !== 'FIRST_PERSON';
      }
      return this.getCameraPrefs();
    },
    getCameraPrefs() {
      return {
        mode: state.camMode,
        distance: Number(state.camDist.toFixed(2)),
        sensitivity: Number(state.camSensitivity.toFixed(2)),
        smoothness: Number(state.camSmoothness.toFixed(2)),
        freeCamAllowed: state.freeCamAllowed
      };
    },
    resetCamera() {
      state.camYaw = boundChar.rotY;
      state.camPitch = state.camMode === 'FIRST_PERSON' ? 0.0 : 0.34;
      if (state.camMode === 'FREE') {
        state.freeCamPos.set(boundChar.x, 2.2, boundChar.z + 2.5);
      }
    },
    sitAt(target, sitMode = 'sit') {
      if (!boundChar || !target) return { ok: false, reason: 'Invalid seat target.' };
      if (state.mode === 'swim') {
        this.exitPool();
      }
      if (state.seatedTarget && state.seatedTarget.id !== target.id && furnitureManager) {
        furnitureManager.releaseTarget(state.seatedTarget.id, boundChar.id);
      }
      if (furnitureManager) {
        const claim = furnitureManager.claimTarget(target.id, boundChar.id);
        if (!claim.ok) {
          return { ok: false, reason: claim.reason };
        }
      }
      state.seatedTarget = target;
      state.mode = sitMode === 'sleep' ? 'sleep' : (sitMode === 'bed_sit' ? 'bed_sit' : 'sit');
      state.speed = 0;
      return { ok: true };
    },
    standUp() {
      if (!state.seatedTarget) {
        state.mode = 'free';
        return;
      }
      const t = state.seatedTarget;
      if (furnitureManager) {
        furnitureManager.releaseTarget(t.id, boundChar.id);
      }
      const [rx, rz] = resolveEnvironmentCollisions(t.exitX, t.exitZ, colliders, boundChar.radius);
      boundChar.x = rx;
      boundChar.z = rz;
      boundChar.syncTransform(0);
      state.seatedTarget = null;
      state.mode = 'free';
      boundChar.applyPose('idle', 0.016, 0);
    },
    enterPool(entryInfo) {
      if (!boundChar || !entryInfo) return false;
      if (state.seatedTarget) {
        this.standUp();
      }
      boundChar.x = Math.max(POOL_BOUNDS.swimMinX, Math.min(POOL_BOUNDS.swimMaxX, entryInfo.entryX));
      boundChar.z = Math.max(POOL_BOUNDS.swimMinZ, Math.min(POOL_BOUNDS.swimMaxZ, entryInfo.entryZ));
      boundChar.rotY = entryInfo.faceAngle || 0;
      state.mode = 'swim';
      state.speed = 0;
      boundChar.syncTransform(-0.92);
      boundChar.applyPose('swim_idle', 0.016, 0);
      if (furnitureManager) {
        furnitureManager.setSwimmerRipple(true, boundChar.x, boundChar.z);
      }
      return true;
    },
    exitPool() {
      if (state.mode !== 'swim') return false;
      const exitSpot = furnitureManager
        ? furnitureManager.getNearestPoolExit(boundChar.x, boundChar.z)
        : { x: 0, z: 9.45 };
      const [rx, rz] = resolveEnvironmentCollisions(exitSpot.x, exitSpot.z, colliders, boundChar.radius);
      boundChar.x = rx;
      boundChar.z = rz;
      state.mode = 'free';
      state.speed = 0;
      boundChar.syncTransform(0);
      boundChar.applyPose('idle', 0.016, 0);
      if (furnitureManager) {
        furnitureManager.setSwimmerRipple(false, 0, 0);
      }
      return true;
    },
    update(dt, inputVec) {
      if (!boundChar) return;
      const mag = Math.hypot(inputVec.x, inputVec.y);

      if (state.activityTimer > 0) {
        state.activityTimer = Math.max(0, state.activityTimer - dt);
        if (state.activityTimer <= 0 || (!state.seatedTarget && mag > 0.25)) {
          state.activityMode = null;
          state.activityTimer = 0;
        }
      }

      // Stand up smoothly if player pushes movement strongly while seated or dancing
      if ((state.mode === 'sit' || state.mode === 'bed_sit') && mag > 0.45) {
        this.standUp();
      } else if (state.mode === 'dance' && mag > 0.25) {
        state.mode = 'free';
      }

      if (state.mode === 'sleep' && state.seatedTarget) {
        const t = state.seatedTarget;
        boundChar.x = t.sleepX;
        boundChar.z = t.sleepZ;
        boundChar.rotY = t.sleepRotY;
        boundChar.syncTransform(t.sleepY - 0.45);
        boundChar.applyPose('sleep', dt, 0);
      } else if ((state.mode === 'sit' || state.mode === 'bed_sit') && state.seatedTarget) {
        const t = state.seatedTarget;
        boundChar.x = t.x;
        boundChar.z = t.z;
        boundChar.rotY = t.charRotY;
        boundChar.syncTransform(t.y - 0.44);
        const sitPose = (state.activityMode === 'sit_eat' || state.activityMode === 'eat') && state.activityTimer > 0
          ? 'sit_eat'
          : state.mode;
        boundChar.applyPose(sitPose, dt, 0);
      } else if (state.mode === 'dance') {
        state.speed = 0;
        boundChar.syncTransform(0);
        boundChar.applyPose('dance', dt, 0);
      } else if (state.mode === 'swim') {
        const swimMax = state.isRunning ? 4.35 : 3.05;
        const targetSpeed = mag > 0.08 ? swimMax * Math.min(1, mag) : 0;
        state.speed += (targetSpeed - state.speed) * Math.min(1, dt * 8);

        if (mag > 0.08) {
          const sinY = Math.sin(state.camYaw);
          const cosY = Math.cos(state.camYaw);
          const moveX = inputVec.x * cosY - inputVec.y * sinY;
          const moveZ = -inputVec.x * sinY - inputVec.y * cosY;
          const norm = Math.hypot(moveX, moveZ) || 1;
          const vx = (moveX / norm) * state.speed;
          const vz = (moveZ / norm) * state.speed;

          boundChar.x = Math.max(
            POOL_BOUNDS.swimMinX,
            Math.min(POOL_BOUNDS.swimMaxX, boundChar.x + vx * dt)
          );
          boundChar.z = Math.max(
            POOL_BOUNDS.swimMinZ,
            Math.min(POOL_BOUNDS.swimMaxZ, boundChar.z + vz * dt)
          );

          const desiredRot = Math.atan2(-vx, -vz);
          boundChar.rotY += shortestAngleDiff(boundChar.rotY, desiredRot) * Math.min(1, dt * 9);
        }

        if (state.speed > 0.3) {
          boundChar.syncTransform(-0.82);
          boundChar.applyPose('swim', dt, state.speed);
        } else {
          boundChar.syncTransform(-0.92);
          boundChar.applyPose('swim_idle', dt, 0);
        }

        if (furnitureManager) {
          furnitureManager.setSwimmerRipple(true, boundChar.x, boundChar.z);
        }
      } else {
        const maxSpeed = state.isRunning ? 7.6 : 4.3;
        const targetSpeed = mag > 0.08 ? maxSpeed * Math.min(1, mag) : 0;
        state.speed += (targetSpeed - state.speed) * Math.min(1, dt * 12);

        if (mag > 0.08) {
          const sinY = Math.sin(state.camYaw);
          const cosY = Math.cos(state.camYaw);
          const moveX = inputVec.x * cosY - inputVec.y * sinY;
          const moveZ = -inputVec.x * sinY - inputVec.y * cosY;
          const norm = Math.hypot(moveX, moveZ) || 1;
          const vx = (moveX / norm) * state.speed;
          const vz = (moveZ / norm) * state.speed;

          const [rx, rz] = resolveEnvironmentCollisions(
            boundChar.x + vx * dt,
            boundChar.z + vz * dt,
            colliders,
            boundChar.radius
          );
          boundChar.x = rx;
          boundChar.z = rz;

          const desiredRot = Math.atan2(-vx, -vz);
          boundChar.rotY += shortestAngleDiff(boundChar.rotY, desiredRot) * Math.min(1, dt * 11);
        }

        boundChar.syncTransform(0);
        if (state.speed > 0.25) {
          boundChar.applyPose(state.isRunning ? 'run' : 'walk', dt, state.speed);
        } else if (state.activityMode && state.activityTimer > 0) {
          boundChar.applyPose(state.activityMode, dt, 0);
        } else {
          boundChar.applyPose('idle', dt, 0);
        }
      }

      // Smooth Camera Modes (FIRST_PERSON / FREE / THIRD_PERSON) with indoor perimeter clamp
      if (boundChar.bodyPivot) {
        boundChar.bodyPivot.visible = state.camMode !== 'FIRST_PERSON';
      }
      const lerpAlpha = Math.min(1, dt * (6 + state.camSmoothness * 14));

      if (state.camMode === 'FIRST_PERSON') {
        const eyeY = state.mode === 'swim'
          ? 0.28
          : (state.mode === 'sleep' ? 0.85 : (state.seatedTarget ? 1.15 : 1.62));
        camera.position.set(boundChar.x, eyeY, boundChar.z);
        const lookDist = 4.0;
        camTarget.set(
          boundChar.x - Math.sin(state.camYaw) * lookDist,
          eyeY - Math.sin(state.camPitch) * lookDist,
          boundChar.z - Math.cos(state.camYaw) * lookDist
        );
        camera.lookAt(camTarget);
      } else if (state.camMode === 'FREE') {
        if (mag > 0.08) {
          const sinY = Math.sin(state.camYaw);
          const cosY = Math.cos(state.camYaw);
          const moveX = inputVec.x * cosY - inputVec.y * sinY;
          const moveZ = -inputVec.x * sinY - inputVec.y * cosY;
          const norm = Math.hypot(moveX, moveZ) || 1;
          const camSpd = state.isRunning ? 10.5 : 6.2;
          state.freeCamPos.x = Math.max(HOUSE_BOUNDS.minX + 0.6, Math.min(HOUSE_BOUNDS.maxX - 0.6, state.freeCamPos.x + (moveX / norm) * camSpd * dt));
          state.freeCamPos.z = Math.max(HOUSE_BOUNDS.minZ + 0.6, Math.min(HOUSE_BOUNDS.maxZ - 0.6, state.freeCamPos.z + (moveZ / norm) * camSpd * dt));
        }
        state.freeCamPos.y = Math.max(0.9, Math.min(HOUSE_BOUNDS.wallHeight - 0.6, 2.2 + state.camPitch * 2.2));
        camera.position.lerp(state.freeCamPos, lerpAlpha);
        camTarget.set(
          state.freeCamPos.x - Math.sin(state.camYaw) * 4.0,
          Math.max(0.4, state.freeCamPos.y - Math.sin(state.camPitch) * 3.0),
          state.freeCamPos.z - Math.cos(state.camYaw) * 4.0
        );
        camera.lookAt(camTarget);
      } else {
        const horizDist = Math.cos(state.camPitch) * state.camDist;
        const vertDist = Math.sin(state.camPitch) * state.camDist;
        const rawCamX = boundChar.x + Math.sin(state.camYaw) * horizDist;
        const rawCamZ = boundChar.z + Math.cos(state.camYaw) * horizDist;

        desiredCamPos.set(
          Math.max(HOUSE_BOUNDS.minX + 0.5, Math.min(HOUSE_BOUNDS.maxX - 0.5, rawCamX)),
          Math.min(HOUSE_BOUNDS.wallHeight - 0.55, 1.45 + vertDist),
          Math.max(HOUSE_BOUNDS.minZ + 0.5, Math.min(HOUSE_BOUNDS.maxZ - 0.5, rawCamZ))
        );

        camera.position.lerp(desiredCamPos, lerpAlpha);
        const lookY = state.mode === 'swim'
          ? 0.45
          : (state.mode === 'sleep' ? 0.75 : (state.seatedTarget ? 1.05 : 1.38));
        camTarget.set(boundChar.x, lookY, boundChar.z);
        camera.lookAt(camTarget);
      }
    }
  };
}
