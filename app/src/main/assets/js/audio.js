/**
 * Lightweight Web Audio synthesizer for Phase 3 house interactions.
 * Runs purely client-side with zero external audio asset dependencies.
 */
export function createSoundSystem() {
  let ctx = null;
  let noiseBuffer = null;

  function ensureContext() {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return null;
    if (!ctx) {
      ctx = new AudioCtx();
    }
    if (ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }
    if (!noiseBuffer && ctx) {
      const length = Math.floor(ctx.sampleRate * 0.45);
      noiseBuffer = ctx.createBuffer(1, length, ctx.sampleRate);
      const data = noiseBuffer.getChannelData(0);
      for (let i = 0; i < length; i++) {
        data[i] = Math.random() * 2 - 1;
      }
    }
    return ctx;
  }

  function playTone(freqStart, freqEnd, duration = 0.16, gainPeak = 0.045, type = 'sine') {
    try {
      const c = ensureContext();
      if (!c) return;
      const now = c.currentTime;
      const osc = c.createOscillator();
      const gain = c.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freqStart, now);
      osc.frequency.exponentialRampToValueAtTime(Math.max(40, freqEnd), now + duration);
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.linearRampToValueAtTime(gainPeak, now + 0.018);
      gain.gain.exponentialRampToValueAtTime(0.001, now + duration);
      osc.connect(gain);
      gain.connect(c.destination);
      osc.start(now);
      osc.stop(now + duration);
    } catch (_) {}
  }

  function playFilteredNoise(filterFreqStart, filterFreqEnd, duration = 0.28, gainPeak = 0.06, q = 1.8) {
    try {
      const c = ensureContext();
      if (!c || !noiseBuffer) return;
      const now = c.currentTime;
      const src = c.createBufferSource();
      src.buffer = noiseBuffer;

      const filter = c.createBiquadFilter();
      filter.type = 'bandpass';
      filter.Q.setValueAtTime(q, now);
      filter.frequency.setValueAtTime(filterFreqStart, now);
      filter.frequency.exponentialRampToValueAtTime(Math.max(80, filterFreqEnd), now + duration);

      const gain = c.createGain();
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.linearRampToValueAtTime(gainPeak, now + 0.04);
      gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      src.connect(filter);
      filter.connect(gain);
      gain.connect(c.destination);
      src.start(now);
      src.stop(now + duration);
    } catch (_) {}
  }

  return {
    playUiTap() {
      playTone(420, 560, 0.08, 0.03, 'sine');
    },
    playDoorSlide(isOpen) {
      // Smooth aluminum rail glide + soft magnetic frame click
      if (isOpen) {
        playFilteredNoise(420, 1150, 0.32, 0.055, 2.2);
        playTone(230, 480, 0.16, 0.035, 'triangle');
      } else {
        playFilteredNoise(980, 340, 0.32, 0.055, 2.2);
        playTone(380, 170, 0.16, 0.035, 'triangle');
        setTimeout(() => playTone(160, 95, 0.07, 0.04, 'sine'), 230);
      }
    },
    playSwitchClick(isOn) {
      playTone(isOn ? 520 : 390, isOn ? 740 : 260, 0.065, 0.045, 'triangle');
    },
    playFanToggle(isOn) {
      if (isOn) {
        playTone(300, 540, 0.12, 0.04, 'triangle');
        playFilteredNoise(240, 520, 0.25, 0.035, 1.4);
      } else {
        playTone(440, 210, 0.14, 0.035, 'triangle');
      }
    },
    playPoolSplash(entering) {
      if (entering) {
        playFilteredNoise(1400, 320, 0.38, 0.09, 1.1);
        playTone(340, 150, 0.22, 0.04, 'sine');
      } else {
        playFilteredNoise(900, 480, 0.22, 0.05, 1.5);
        playTone(260, 440, 0.14, 0.035, 'sine');
      }
    },
    playSit(isBed = false) {
      playTone(isBed ? 280 : 340, isBed ? 380 : 470, 0.13, 0.035, 'sine');
    },
    playStand() {
      playTone(320, 450, 0.11, 0.035, 'sine');
    },
    playDenied() {
      playTone(220, 145, 0.2, 0.05, 'sawtooth');
    },
    playCookStart() {
      playFilteredNoise(600, 1300, 0.26, 0.05, 1.4);
      playTone(330, 520, 0.14, 0.04, 'triangle');
    },
    playCookReady() {
      playTone(523, 784, 0.16, 0.045, 'sine');
      setTimeout(() => playTone(784, 1046, 0.18, 0.04, 'sine'), 110);
    },
    playEat() {
      playTone(300, 420, 0.09, 0.04, 'triangle');
      setTimeout(() => playTone(380, 520, 0.1, 0.035, 'sine'), 95);
    },
    playCheckpointClear() {
      playTone(440, 660, 0.12, 0.045, 'triangle');
      setTimeout(() => playTone(660, 880, 0.14, 0.045, 'sine'), 85);
    },
    playEmergencySiren() {
      playTone(540, 880, 0.24, 0.055, 'sawtooth');
      setTimeout(() => playTone(880, 520, 0.24, 0.055, 'sawtooth'), 250);
    },
    playCelebrationChime() {
      playTone(523, 659, 0.14, 0.04, 'sine');
      setTimeout(() => playTone(659, 784, 0.14, 0.04, 'sine'), 120);
      setTimeout(() => playTone(784, 1046, 0.22, 0.045, 'sine'), 240);
    }
  };
}
