/**
 * Table sound effects, synthesised with the Web Audio API so there are no files to load.
 *
 * Every sound is built from filtered noise — the way paper, felt and clay chips actually sound —
 * rather than from beeping oscillators. Voices run through one bus that feeds a gentle compressor
 * and a short, synthesised room reverb, so they sit together as if heard at the same table.
 */

interface NoiseOpts {
  at?: number;
  dur: number;
  gain: number;
  type: BiquadFilterType;
  freq: number;
  /** Optional end frequency: the filter sweeps from `freq` to here over `dur`. */
  sweepTo?: number;
  q?: number;
  attack?: number;
}

interface ToneOpts {
  at?: number;
  freq: number;
  /** Optional end frequency for a pitch drop (a thump rather than a note). */
  dropTo?: number;
  dur: number;
  gain: number;
  type?: OscillatorType;
  attack?: number;
}

/** How much of each sound goes to the room reverb. */
const ROOM_SEND = 0.14;

class SoundController {
  private ctx: AudioContext | null = null;
  private bus: GainNode | null = null;
  private noise: AudioBuffer | null = null;
  public enabled: boolean = true;
  public volume: number = 0.7;

  /** Creates the context and the shared signal chain on first use (browsers require a gesture first). */
  private ready(): AudioContext | null {
    if (!this.enabled || typeof window === 'undefined') return null;
    if (!this.ctx) {
      const AudioCtx =
        window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return null;
      const ctx = new AudioCtx();

      const compressor = ctx.createDynamicsCompressor();
      compressor.threshold.value = -18;
      compressor.knee.value = 12;
      compressor.ratio.value = 3;
      compressor.attack.value = 0.003;
      compressor.release.value = 0.12;
      compressor.connect(ctx.destination);

      const bus = ctx.createGain();
      bus.connect(compressor);

      const room = ctx.createConvolver();
      room.buffer = this.roomImpulse(ctx);
      const wet = ctx.createGain();
      wet.gain.value = ROOM_SEND;
      bus.connect(room);
      room.connect(wet);
      wet.connect(compressor);

      // One second of white noise, reused by every voice.
      const noise = ctx.createBuffer(1, ctx.sampleRate, ctx.sampleRate);
      const data = noise.getChannelData(0);
      for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;

      this.ctx = ctx;
      this.bus = bus;
      this.noise = noise;
    }
    if (this.ctx.state === 'suspended') this.ctx.resume().catch(() => {});
    return this.ctx;
  }

  /** A small, soft room: 0.4 s of decaying stereo noise, darker as it fades. */
  private roomImpulse(ctx: AudioContext): AudioBuffer {
    const length = Math.floor(ctx.sampleRate * 0.4);
    const impulse = ctx.createBuffer(2, length, ctx.sampleRate);
    for (let ch = 0; ch < 2; ch++) {
      const data = impulse.getChannelData(ch);
      let last = 0;
      for (let i = 0; i < length; i++) {
        const t = i / length;
        // One-pole lowpass whose cutoff falls with time, so the tail loses its highs.
        const smoothing = 0.2 + t * 0.7;
        last = last * smoothing + (Math.random() * 2 - 1) * (1 - smoothing);
        data[i] = last * Math.pow(1 - t, 3);
      }
    }
    return impulse;
  }

  private noiseBurst({ at = 0, dur, gain, type, freq, sweepTo, q = 0.7, attack = 0.002 }: NoiseOpts) {
    const ctx = this.ctx!;
    const t = ctx.currentTime + at;
    const src = ctx.createBufferSource();
    src.buffer = this.noise;
    // Start somewhere random in the buffer so repeated sounds are never identical.
    const offset = Math.random() * (1 - dur);

    const filter = ctx.createBiquadFilter();
    filter.type = type;
    filter.Q.value = q;
    filter.frequency.setValueAtTime(freq, t);
    if (sweepTo) filter.frequency.exponentialRampToValueAtTime(sweepTo, t + dur);

    const env = ctx.createGain();
    env.gain.setValueAtTime(0.0001, t);
    env.gain.exponentialRampToValueAtTime(gain * this.volume, t + attack);
    env.gain.exponentialRampToValueAtTime(0.0001, t + dur);

    src.connect(filter).connect(env).connect(this.bus!);
    src.start(t, Math.max(0, offset), dur + 0.02);
  }

  private tone({ at = 0, freq, dropTo, dur, gain, type = 'sine', attack = 0.003 }: ToneOpts) {
    const ctx = this.ctx!;
    const t = ctx.currentTime + at;
    const osc = ctx.createOscillator();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, t);
    if (dropTo) osc.frequency.exponentialRampToValueAtTime(dropTo, t + dur);

    const env = ctx.createGain();
    env.gain.setValueAtTime(0.0001, t);
    env.gain.exponentialRampToValueAtTime(gain * this.volume, t + attack);
    env.gain.exponentialRampToValueAtTime(0.0001, t + dur);

    osc.connect(env).connect(this.bus!);
    osc.start(t);
    osc.stop(t + dur + 0.02);
  }

  private play(build: () => void) {
    if (!this.ready()) return;
    try {
      build();
    } catch {
      // Sound is decoration; never let it break a click.
    }
  }

  /** A card sliding off the deck across felt: an airy swish that brightens, then a light landing. */
  playCardDeal() {
    this.play(() => {
      const pitch = 0.9 + Math.random() * 0.2;
      this.noiseBurst({ dur: 0.11, gain: 0.32, type: 'bandpass', freq: 700 * pitch, sweepTo: 3200 * pitch, q: 0.9, attack: 0.018 });
      this.noiseBurst({ at: 0.085, dur: 0.035, gain: 0.22, type: 'lowpass', freq: 1600 * pitch, q: 0.5 });
    });
  }

  /** A card placed firmly on the table: a papery snap over a soft thump of felt. */
  playCardPlay() {
    this.play(() => {
      this.noiseBurst({ dur: 0.022, gain: 0.42, type: 'highpass', freq: 1800, q: 0.6, attack: 0.001 });
      this.noiseBurst({ dur: 0.06, gain: 0.24, type: 'lowpass', freq: 650, q: 0.4, attack: 0.002 });
      this.tone({ freq: 150, dropTo: 70, dur: 0.07, gain: 0.22 });
    });
  }

  /** The trick swept in: three quick, overlapping card brushes falling slightly in pitch. */
  playTrickWon() {
    this.play(() => {
      [0, 0.045, 0.09].forEach((at, i) => {
        const f = 2600 - i * 450;
        this.noiseBurst({ at, dur: 0.075, gain: 0.2, type: 'bandpass', freq: f * 0.6, sweepTo: f, q: 1.1, attack: 0.012 });
      });
      this.noiseBurst({ at: 0.15, dur: 0.04, gain: 0.16, type: 'lowpass', freq: 1200, q: 0.5 });
    });
  }

  /** A quiet mechanical tick for buttons — felt more than heard. */
  playClick() {
    this.play(() => {
      this.noiseBurst({ dur: 0.012, gain: 0.12, type: 'bandpass', freq: 3800, q: 2.2, attack: 0.0008 });
    });
  }

  /** Two knuckle taps on the table — a check. */
  playKnock() {
    this.play(() => {
      [0, 0.11].forEach((at) => {
        this.tone({ at, freq: 190, dropTo: 95, dur: 0.06, gain: 0.3 });
        this.noiseBurst({ at, dur: 0.03, gain: 0.18, type: 'lowpass', freq: 900, q: 0.5, attack: 0.001 });
      });
    });
  }

  /** A bet: two or three clay chips set down on the felt. */
  playChips() {
    this.play(() => {
      let at = 0;
      const count = 2 + Math.round(Math.random());
      for (let i = 0; i < count; i++) {
        const ring = 3000 + Math.random() * 700;
        this.noiseBurst({ at, dur: 0.028, gain: 0.24, type: 'bandpass', freq: ring, q: 7, attack: 0.001 });
        this.tone({ at, freq: ring * 1.02, dur: 0.04, gain: 0.03 });
        at += 0.04 + Math.random() * 0.03;
      }
      this.noiseBurst({ at, dur: 0.05, gain: 0.12, type: 'lowpass', freq: 700, q: 0.4 });
    });
  }

  /** A stack of clay chips pushed across the table, settled by one warm, low chord. */
  playWinFanfare() {
    this.play(() => {
      let at = 0;
      for (let i = 0; i < 7; i++) {
        const ring = 2800 + Math.random() * 900;
        this.noiseBurst({ at, dur: 0.03, gain: 0.26, type: 'bandpass', freq: ring, q: 7, attack: 0.001 });
        this.tone({ at, freq: ring * 1.02, dur: 0.045, gain: 0.035 });
        at += 0.035 + Math.random() * 0.035;
      }
      // A major sixth voicing, soft attack and long release, sitting under the chips.
      [220, 277.18, 329.63, 369.99].forEach((freq, i) => {
        this.tone({ at: at + i * 0.012, freq, dur: 1.4, gain: 0.05, attack: 0.04 });
        this.tone({ at: at + i * 0.012, freq: freq * 1.003, dur: 1.2, gain: 0.025, type: 'triangle', attack: 0.05 });
      });
    });
  }
}

export const soundFx = new SoundController();
