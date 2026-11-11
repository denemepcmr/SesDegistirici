package com.yusuf.sesdegistirici;

/** Düşük gecikmeli, iki kafalı gecikme hattı tabanlı perde değiştirici. */
public class PitchShifter {
    private static final int SIZE = 8192;
    private static final float WIN = 1536f;
    private final float[] buf = new float[SIZE];
    private int w = 0;
    private float phase = 0f;

    public float process(float x, float rate) {
        buf[w] = x;
        float p1 = phase;
        float p2 = phase + 0.5f;
        if (p2 >= 1f) p2 -= 1f;
        float out = read(p1) * tri(p1) + read(p2) * tri(p2);
        phase += (1f - rate) / WIN;
        if (phase >= 1f) phase -= 1f;
        if (phase < 0f) phase += 1f;
        w = (w + 1) % SIZE;
        return out;
    }

    private float tri(float p) {
        return 1f - Math.abs(2f * p - 1f);
    }

    private float read(float p) {
        float pos = w - p * WIN;
        int i0 = (int) Math.floor(pos);
        float f = pos - i0;
        float a = buf[mod(i0)];
        float b = buf[mod(i0 + 1)];
        return a * (1f - f) + b * f;
    }

    private int mod(int i) {
        return ((i % SIZE) + SIZE) % SIZE;
    }
}
