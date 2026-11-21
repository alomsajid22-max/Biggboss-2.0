import * as THREE from '../vendor/three/three.module.min.js';

function makeCanvas(w, h) {
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  return canvas;
}

function finalizeTexture(canvas, repeatX = 1, repeatY = 1, isColor = true) {
  const tex = new THREE.CanvasTexture(canvas);
  tex.wrapS = THREE.RepeatWrapping;
  tex.wrapT = THREE.RepeatWrapping;
  tex.repeat.set(repeatX, repeatY);
  if (isColor) {
    tex.colorSpace = THREE.SRGBColorSpace;
  }
  tex.anisotropy = 1;
  return tex;
}

export function createTextureLibrary() {
  // 1. Luxury Calacatta Marble Floor
  const marbleCanvas = makeCanvas(512, 512);
  const mctx = marbleCanvas.getContext('2d');
  mctx.fillStyle = '#ebe8e1';
  mctx.fillRect(0, 0, 512, 512);
  for (let i = 0; i < 24; i++) {
    mctx.strokeStyle = i % 3 === 0 ? 'rgba(182, 160, 122, 0.25)' : 'rgba(130, 136, 148, 0.2)';
    mctx.lineWidth = 1 + (i % 3);
    mctx.beginPath();
    let x = (i * 53) % 512;
    let y = 0;
    mctx.moveTo(x, y);
    for (let s = 1; s <= 8; s++) {
      x += Math.sin(i + s * 1.3) * 36;
      y = s * 64;
      mctx.lineTo(x, y);
    }
    mctx.stroke();
  }
  mctx.strokeStyle = 'rgba(165, 155, 140, 0.35)';
  mctx.lineWidth = 2;
  for (let g = 0; g <= 512; g += 128) {
    mctx.beginPath();
    mctx.moveTo(g, 0);
    mctx.lineTo(g, 512);
    mctx.stroke();
    mctx.beginPath();
    mctx.moveTo(0, g);
    mctx.lineTo(512, g);
    mctx.stroke();
  }

  // 2. Warm Herringbone / Plank Oak Wood
  const woodCanvas = makeCanvas(512, 512);
  const wctx = woodCanvas.getContext('2d');
  wctx.fillStyle = '#6f4b2f';
  wctx.fillRect(0, 0, 512, 512);
  const plankH = 64;
  for (let row = 0; row < 8; row++) {
    const shade = (row % 3) * 10 - 10;
    wctx.fillStyle = `rgb(${112 + shade}, ${76 + Math.floor(shade * 0.7)}, ${48 + Math.floor(shade * 0.4)})`;
    wctx.fillRect(0, row * plankH, 512, plankH - 2);
    wctx.strokeStyle = 'rgba(42, 24, 12, 0.45)';
    wctx.lineWidth = 2;
    wctx.strokeRect(0, row * plankH, 512, plankH);
    const offset = (row % 2) * 128;
    for (let col = 0; col < 2; col++) {
      const jx = (col * 256 + offset) % 512;
      wctx.beginPath();
      wctx.moveTo(jx, row * plankH);
      wctx.lineTo(jx, (row + 1) * plankH);
      wctx.stroke();
    }
    wctx.strokeStyle = 'rgba(45, 25, 12, 0.16)';
    wctx.lineWidth = 1;
    for (let g = 0; g < 6; g++) {
      const gy = row * plankH + 8 + g * 9;
      wctx.beginPath();
      wctx.moveTo(0, gy);
      wctx.bezierCurveTo(170, gy + 3, 340, gy - 3, 512, gy);
      wctx.stroke();
    }
  }

  // 3. Dark Walnut Wood (Smoking / Confession / Captain accents)
  const walnutCanvas = makeCanvas(256, 256);
  const wlctx = walnutCanvas.getContext('2d');
  wlctx.fillStyle = '#3b261b';
  wlctx.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 40; i++) {
    wlctx.strokeStyle = i % 2 === 0 ? 'rgba(22, 12, 7, 0.32)' : 'rgba(92, 62, 42, 0.2)';
    wlctx.beginPath();
    wlctx.moveTo(0, i * 6.5);
    wlctx.lineTo(256, i * 6.5 + Math.sin(i) * 4);
    wlctx.stroke();
  }

  // 4. Pool Mosaic Tile
  const poolCanvas = makeCanvas(256, 256);
  const pctx = poolCanvas.getContext('2d');
  pctx.fillStyle = '#1896b3';
  pctx.fillRect(0, 0, 256, 256);
  const tile = 32;
  for (let y = 0; y < 256; y += tile) {
    for (let x = 0; x < 256; x += tile) {
      const v = ((x * 7 + y * 13) % 28) - 14;
      pctx.fillStyle = `rgb(${22 + v}, ${152 + v}, ${182 + v})`;
      pctx.fillRect(x + 2, y + 2, tile - 3, tile - 3);
    }
  }

  // 5. Travertine / Stone Deck Tile
  const stoneCanvas = makeCanvas(256, 256);
  const sctx = stoneCanvas.getContext('2d');
  sctx.fillStyle = '#cfc7ba';
  sctx.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 320; i++) {
    sctx.fillStyle = i % 2 === 0 ? 'rgba(255,255,255,0.08)' : 'rgba(90,82,70,0.08)';
    sctx.fillRect((i * 37) % 256, (i * 71) % 256, 18, 5);
  }
  sctx.strokeStyle = 'rgba(110, 102, 92, 0.35)';
  sctx.lineWidth = 2;
  sctx.strokeRect(1, 1, 126, 126);
  sctx.strokeRect(129, 1, 126, 126);
  sctx.strokeRect(1, 129, 126, 126);
  sctx.strokeRect(129, 129, 126, 126);

  // 6. Dark Slate / Tech Floor (Admin Control & Activity)
  const slateCanvas = makeCanvas(256, 256);
  const slctx = slateCanvas.getContext('2d');
  slctx.fillStyle = '#242936';
  slctx.fillRect(0, 0, 256, 256);
  for (let y = 0; y < 256; y += 64) {
    for (let x = 0; x < 256; x += 64) {
      slctx.fillStyle = (x + y) % 128 === 0 ? '#293040' : '#212633';
      slctx.fillRect(x + 1, y + 1, 62, 62);
    }
  }

  // 7. Garden Botanical Soil / Turf
  const turfCanvas = makeCanvas(256, 256);
  const tctx = turfCanvas.getContext('2d');
  tctx.fillStyle = '#2e5c35';
  tctx.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 650; i++) {
    tctx.fillStyle = i % 3 === 0 ? '#3b7444' : '#234a29';
    tctx.fillRect((i * 43) % 256, (i * 89) % 256, 4, 4);
  }

  // 8. Wall Venetian Plaster
  const plasterCanvas = makeCanvas(256, 256);
  const plctx = plasterCanvas.getContext('2d');
  plctx.fillStyle = '#e7e2d9';
  plctx.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 400; i++) {
    plctx.fillStyle = i % 2 === 0 ? 'rgba(255,255,255,0.06)' : 'rgba(160,150,138,0.06)';
    plctx.beginPath();
    plctx.arc((i * 59) % 256, (i * 97) % 256, 16, 0, Math.PI * 2);
    plctx.fill();
  }

  // 9. Acoustic Ribbed Panel (Confession / Media wall)
  const ribCanvas = makeCanvas(256, 256);
  const rctx = ribCanvas.getContext('2d');
  rctx.fillStyle = '#1c1824';
  rctx.fillRect(0, 0, 256, 256);
  for (let x = 0; x < 256; x += 16) {
    rctx.fillStyle = '#2b2438';
    rctx.fillRect(x + 2, 0, 10, 256);
    rctx.fillStyle = '#110e16';
    rctx.fillRect(x + 12, 0, 4, 256);
  }

  // 10. Woven Bouclé / Carpet Rug
  const rugCanvas = makeCanvas(256, 256);
  const rgctx = rugCanvas.getContext('2d');
  rgctx.fillStyle = '#d7cfc3';
  rgctx.fillRect(0, 0, 256, 256);
  for (let y = 0; y < 256; y += 4) {
    for (let x = 0; x < 256; x += 4) {
      rgctx.fillStyle = (x + y) % 8 === 0 ? 'rgba(255,255,255,0.14)' : 'rgba(130,115,96,0.12)';
      rgctx.fillRect(x, y, 2, 2);
    }
  }

  return {
    marble: finalizeTexture(marbleCanvas, 5, 5, true),
    wood: finalizeTexture(woodCanvas, 4, 4, true),
    walnut: finalizeTexture(walnutCanvas, 2, 2, true),
    poolTile: finalizeTexture(poolCanvas, 6, 4, true),
    stone: finalizeTexture(stoneCanvas, 4, 4, true),
    slate: finalizeTexture(slateCanvas, 4, 3, true),
    turf: finalizeTexture(turfCanvas, 5, 5, true),
    plaster: finalizeTexture(plasterCanvas, 4, 2, true),
    acousticRib: finalizeTexture(ribCanvas, 6, 1, true),
    rug: finalizeTexture(rugCanvas, 4, 4, true)
  };
}

export function createRoomPlaqueTexture(title, accentHex = '#eab954') {
  const canvas = makeCanvas(512, 128);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#121827';
  ctx.fillRect(0, 0, 512, 128);

  ctx.strokeStyle = accentHex;
  ctx.lineWidth = 6;
  ctx.strokeRect(8, 8, 496, 112);

  ctx.fillStyle = accentHex;
  ctx.font = '700 18px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('BIGG BOSS ZONE', 256, 42);

  ctx.fillStyle = '#f8fafc';
  ctx.font = '800 34px sans-serif';
  ctx.fillText(title.toUpperCase(), 256, 88);

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

export function createMonitorWallTexture(index = 0) {
  const canvas = makeCanvas(512, 256);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#070b14';
  ctx.fillRect(0, 0, 512, 256);

  const labels = [
    ['CAM 01 • ATRIUM', 'CAM 02 • POOL DECK', 'CAM 03 • 15-BED DORM', 'CAM 04 • DINING'],
    ['CAM 05 • CONFESSION', 'CAM 06 • KITCHEN', 'CAM 07 • GARDEN', 'CAM 08 • ACTIVITY']
  ][index % 2];

  const colors = ['#1e3a5f', '#134e4a', '#3b2f63', '#4a2535'];

  for (let i = 0; i < 4; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 12 + col * 248;
    const y = 12 + row * 120;
    const w = 236;
    const h = 108;

    const grad = ctx.createLinearGradient(x, y, x + w, y + h);
    grad.addColorStop(0, colors[(i + index) % colors.length]);
    grad.addColorStop(1, '#0d1424');
    ctx.fillStyle = grad;
    ctx.fillRect(x, y, w, h);

    // Simulated camera architectural grid lines
    ctx.strokeStyle = 'rgba(255,255,255,0.14)';
    ctx.lineWidth = 1;
    ctx.strokeRect(x + 18, y + 26, w - 36, h - 40);

    ctx.fillStyle = '#ef4444';
    ctx.beginPath();
    ctx.arc(x + 16, y + 16, 4, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#e2e8f0';
    ctx.font = '700 13px monospace';
    ctx.fillText(labels[i], x + 28, y + 20);

    ctx.fillStyle = '#34d399';
    ctx.font = '600 11px monospace';
    ctx.fillText('LIVE FEED • HD', x + 16, y + h - 10);
  }

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

export function createBedNumberTexture(num) {
  const canvas = makeCanvas(128, 128);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#182034';
  ctx.fillRect(0, 0, 128, 128);
  ctx.strokeStyle = '#eab954';
  ctx.lineWidth = 8;
  ctx.strokeRect(6, 6, 116, 116);
  ctx.fillStyle = '#eab954';
  ctx.font = '800 52px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(String(num).padStart(2, '0'), 64, 68);
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}
