#!/usr/bin/env python3
"""
Generates the VLK Battery Doctor connection chime.

Struck glass is not a harmonic sound. A plate or a wine glass rings with
partials at roughly 1, 2.76, 5.40 and 8.93 times the fundamental, and the
higher ones die away fastest — that uneven decay is what makes the ear hear
"glass" rather than "bell" or "tone".

Two strikes a perfect fifth apart, the second landing 125 ms after the first,
so the pair resolves upward and the moment reads as a latch closing.

Run:  python3 make_chime.py
Out:  ../app/src/main/res/raw/connect_chime.wav
"""
import math, os, struct, wave

SR = 44100
DURATION = 1.25

# ratio, amplitude, decay rate (per second) — higher partials fade first
PARTIALS = [
    (1.00, 1.00, 2.3),
    (2.76, 0.46, 3.9),
    (5.40, 0.22, 6.2),
    (8.93, 0.11, 9.0),
    (13.3, 0.05, 13.0),
]

# (fundamental Hz, start time s, gain)  — C6 then G6, a perfect fifth
STRIKES = [
    (1046.50, 0.000, 1.00),
    (1567.98, 0.125, 0.82),
]

ATTACK = 0.0018   # seconds; fast enough to sound struck, slow enough not to click


def strike(t, f0, gain):
    """One struck-glass note at time t seconds after its own onset."""
    if t < 0:
        return 0.0
    env_attack = 1.0 - math.exp(-t / ATTACK)
    total = 0.0
    for ratio, amp, decay in PARTIALS:
        # A little detune on the upper partials stops it sounding synthetic.
        f = f0 * ratio * (1.0 + 0.0009 * ratio)
        total += amp * math.exp(-decay * t) * math.sin(2 * math.pi * f * t)
    return gain * env_attack * total


def transient(t):
    """The tick of the strike itself: a very short bright click."""
    if t < 0 or t > 0.004:
        return 0.0
    # deterministic pseudo-noise so the file is reproducible
    x = math.sin(t * 91711.0) + math.sin(t * 54319.0) + math.sin(t * 33767.0)
    return 0.22 * (x / 3.0) * math.exp(-t / 0.0012)


def main():
    n = int(SR * DURATION)
    samples = []
    for i in range(n):
        t = i / SR
        v = 0.0
        for f0, start, gain in STRIKES:
            v += strike(t - start, f0, gain)
            v += transient(t - start) * gain
        samples.append(v)

    # Fade the last 60 ms to silence so the file ends cleanly.
    fade = int(SR * 0.06)
    for i in range(fade):
        samples[n - fade + i] *= (fade - i) / fade

    peak = max(abs(s) for s in samples) or 1.0
    scale = 0.86 / peak
    frames = b"".join(
        struct.pack("<h", max(-32768, min(32767, int(s * scale * 32767))))
        for s in samples
    )

    out = os.path.join(
        os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw",
        "connect_chime.wav"
    )
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with wave.open(out, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(frames)

    rms = math.sqrt(sum((s * scale) ** 2 for s in samples) / n)
    print(f"wrote {os.path.normpath(out)}")
    print(f"  {DURATION:.2f} s, {SR} Hz, mono 16-bit, {len(frames)/1024:.0f} KB")
    print(f"  peak {0.86:.2f}, rms {rms:.3f}")


if __name__ == "__main__":
    main()
