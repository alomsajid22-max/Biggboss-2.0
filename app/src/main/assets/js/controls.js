import { HOUSE_BOUNDS, ROOM_ZONES } from './worldLayout.js';

export function setupControls({
  canvas,
  joystickZone,
  joystickBase,
  joystickKnob,
  onOrbitCamera,
  onZoomCamera = () => {},
  onToggleInteract,
  onAltInteract = () => {},
  onToggleSprint
}) {
  const joyState = {
    pointerId: null,
    centerX: 0,
    centerY: 0,
    maxRadius: 42,
    x: 0,
    y: 0
  };

  const lookState = {
    pointerId: null,
    lastX: 0,
    lastY: 0
  };

  const keys = new Set();

  function updateJoystickFromPoint(clientX, clientY) {
    const dx = clientX - joyState.centerX;
    const dy = clientY - joyState.centerY;
    const dist = Math.hypot(dx, dy);
    const clamped = Math.min(dist, joyState.maxRadius);
    const angle = Math.atan2(dy, dx);
    const kx = Math.cos(angle) * clamped;
    const ky = Math.sin(angle) * clamped;

    joystickKnob.style.transform = `translate(${kx}px, ${ky}px)`;
    joyState.x = kx / joyState.maxRadius;
    // Screen up (-ky) maps to +1 forward
    joyState.y = -ky / joyState.maxRadius;
  }

  function resetJoystick() {
    joyState.pointerId = null;
    joyState.x = 0;
    joyState.y = 0;
    joystickBase.classList.remove('active');
    joystickKnob.style.transform = 'translate(0px, 0px)';
  }

  joystickZone.addEventListener('pointerdown', (e) => {
    e.preventDefault();
    e.stopPropagation();
    const rect = joystickBase.getBoundingClientRect();
    joyState.pointerId = e.pointerId;
    joyState.centerX = rect.left + rect.width / 2;
    joyState.centerY = rect.top + rect.height / 2;
    joystickBase.classList.add('active');
    try {
      joystickZone.setPointerCapture(e.pointerId);
    } catch (_) {}
    updateJoystickFromPoint(e.clientX, e.clientY);
  });

  joystickZone.addEventListener('pointermove', (e) => {
    if (e.pointerId !== joyState.pointerId) return;
    e.preventDefault();
    updateJoystickFromPoint(e.clientX, e.clientY);
  });

  const endJoy = (e) => {
    if (e.pointerId === joyState.pointerId) {
      resetJoystick();
    }
  };
  joystickZone.addEventListener('pointerup', endJoy);
  joystickZone.addEventListener('pointercancel', endJoy);

  // Canvas drag for 3D camera orbit (multi-touch safe alongside joystick)
  canvas.addEventListener('pointerdown', (e) => {
    if (lookState.pointerId !== null) return;
    lookState.pointerId = e.pointerId;
    lookState.lastX = e.clientX;
    lookState.lastY = e.clientY;
    try {
      canvas.setPointerCapture(e.pointerId);
    } catch (_) {}
  });

  canvas.addEventListener('pointermove', (e) => {
    if (e.pointerId !== lookState.pointerId) return;
    const dx = e.clientX - lookState.lastX;
    const dy = e.clientY - lookState.lastY;
    lookState.lastX = e.clientX;
    lookState.lastY = e.clientY;
    onOrbitCamera(dx * 0.0065, dy * 0.005);
  });

  const endLook = (e) => {
    if (e.pointerId === lookState.pointerId) {
      lookState.pointerId = null;
    }
  };
  canvas.addEventListener('pointerup', endLook);
  canvas.addEventListener('pointercancel', endLook);

  // Wheel & two-finger pinch zoom on 3D canvas
  canvas.addEventListener('wheel', (e) => {
    e.preventDefault();
    onZoomCamera(e.deltaY > 0 ? 0.55 : -0.55);
  }, { passive: false });

  let pinchStartDist = 0;
  canvas.addEventListener('touchstart', (e) => {
    if (e.touches && e.touches.length === 2) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      pinchStartDist = Math.hypot(dx, dy);
    }
  }, { passive: true });
  canvas.addEventListener('touchmove', (e) => {
    if (e.touches && e.touches.length === 2 && pinchStartDist > 0) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      const curDist = Math.hypot(dx, dy);
      const diff = curDist - pinchStartDist;
      if (Math.abs(diff) > 6) {
        onZoomCamera(-diff * 0.015);
        pinchStartDist = curDist;
      }
    }
  }, { passive: true });
  canvas.addEventListener('touchend', () => {
    pinchStartDist = 0;
  }, { passive: true });

  // Keyboard support for emulator/desktop preview testing
  window.addEventListener('keydown', (e) => {
    const code = e.code;
    keys.add(code);
    if (code === 'KeyE') {
      onToggleInteract();
    } else if (code === 'KeyF') {
      onAltInteract();
    } else if (code === 'ShiftLeft' || code === 'ShiftRight') {
      onToggleSprint(true);
    }
  });

  window.addEventListener('keyup', (e) => {
    keys.delete(e.code);
    if (e.code === 'ShiftLeft' || e.code === 'ShiftRight') {
      onToggleSprint(false);
    }
  });

  return {
    getMoveVector() {
      let x = joyState.x;
      let y = joyState.y;

      if (keys.has('KeyA') || keys.has('ArrowLeft')) x -= 1;
      if (keys.has('KeyD') || keys.has('ArrowRight')) x += 1;
      if (keys.has('KeyW') || keys.has('ArrowUp')) y += 1;
      if (keys.has('KeyS') || keys.has('ArrowDown')) y -= 1;

      const len = Math.hypot(x, y);
      if (len > 1) {
        x /= len;
        y /= len;
      }
      return { x, y };
    }
  };
}

export function renderMinimap(canvas, playerPos, playerYaw, doors, housematesList = [], isAdmin = false, remotePlayers = []) {
  const ctx = canvas.getContext('2d');
  const W = canvas.width;
  const H = canvas.height;
  ctx.clearRect(0, 0, W, H);

  ctx.fillStyle = '#0b101d';
  ctx.fillRect(0, 0, W, H);

  const pad = 26;
  const houseW = HOUSE_BOUNDS.maxX - HOUSE_BOUNDS.minX;
  const houseD = HOUSE_BOUNDS.maxZ - HOUSE_BOUNDS.minZ;
  const scale = Math.min((W - pad * 2) / houseW, (H - pad * 2) / houseD);
  const offX = (W - houseW * scale) / 2;
  const offY = (H - houseD * scale) / 2;

  const toCanvasX = (wx) => offX + (wx - HOUSE_BOUNDS.minX) * scale;
  const toCanvasY = (wz) => offY + (wz - HOUSE_BOUNDS.minZ) * scale;

  // Draw rooms (Admin Control Room is hidden for PLAYER role)
  for (const r of ROOM_ZONES) {
    const rx = toCanvasX(r.x1);
    const ry = toCanvasY(r.z1);
    const rw = (r.x2 - r.x1) * scale;
    const rh = (r.z2 - r.z1) * scale;

    if (r.adminOnly && !isAdmin) {
      ctx.fillStyle = '#070a13';
      ctx.fillRect(rx, ry, rw, rh);
      ctx.strokeStyle = 'rgba(255,255,255,0.08)';
      ctx.lineWidth = 1.5;
      ctx.strokeRect(rx, ry, rw, rh);
      continue;
    }

    ctx.fillStyle = `${r.color}24`;
    ctx.fillRect(rx, ry, rw, rh);
    ctx.strokeStyle = 'rgba(255,255,255,0.22)';
    ctx.lineWidth = 1.5;
    ctx.strokeRect(rx, ry, rw, rh);

    ctx.fillStyle = '#e2e8f0';
    ctx.font = '700 10px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const shortName = r.name
      .replace('Central Living Atrium', 'Living Atrium')
      .replace('Indoor Botanical Garden', 'Garden')
      .replace('Indoor Swimming Pool', 'Pool')
      .replace('Activity & Challenge Room', 'Activity')
      .replace('Fictional Smoking Lounge', 'Smoking')
      .replace('15-Player Bedroom', '15-Bed Dorm');
    ctx.fillText(shortName, rx + rw / 2, ry + rh / 2);
  }

  // Draw Pool Basin
  ctx.fillStyle = 'rgba(6, 182, 212, 0.42)';
  ctx.fillRect(toCanvasX(-6.5), toCanvasY(10.5), 13 * scale, 9 * scale);

  // Draw Center Tree
  ctx.fillStyle = '#34d399';
  ctx.beginPath();
  ctx.arc(toCanvasX(0), toCanvasY(0), 1.9 * scale, 0, Math.PI * 2);
  ctx.fill();

  // Draw Glass Sliding Doors (hide Admin door from ordinary players)
  for (const d of doors) {
    if (d.requiredRole === 'ADMIN' && !isAdmin) continue;
    ctx.fillStyle = d.isOpen ? '#22c55e' : '#eab954';
    ctx.beginPath();
    ctx.arc(toCanvasX(d.x), toCanvasY(d.z), 4.5, 0, Math.PI * 2);
    ctx.fill();
  }

  // Draw NPC Housemates (only visible NPCs)
  for (const h of housematesList) {
    if (h.isUser || h.visible === false) continue;
    ctx.fillStyle = 'rgba(226, 232, 240, 0.55)';
    ctx.beginPath();
    ctx.arc(toCanvasX(h.x), toCanvasY(h.z), 3.1, 0, Math.PI * 2);
    ctx.fill();
  }

  // Draw Real Remote Human Players
  for (const rp of remotePlayers) {
    const rx = toCanvasX(rp.x);
    const ry = toCanvasY(rp.z);
    ctx.fillStyle = '#34d399';
    ctx.beginPath();
    ctx.arc(rx, ry, 5.2, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1.4;
    ctx.stroke();
  }

  // Draw Player Position + Facing Arrow
  const px = toCanvasX(playerPos.x);
  const py = toCanvasY(playerPos.z);
  ctx.save();
  ctx.translate(px, py);
  ctx.rotate(-playerYaw);
  ctx.fillStyle = '#38bdf8';
  ctx.beginPath();
  ctx.moveTo(0, -10);
  ctx.lineTo(7, 8);
  ctx.lineTo(0, 4);
  ctx.lineTo(-7, 8);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}
