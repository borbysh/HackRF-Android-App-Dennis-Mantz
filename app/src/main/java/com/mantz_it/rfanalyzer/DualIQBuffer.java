package com.mantz_it.rfanalyzer;

public class DualIQBuffer {
    public final short[] iqA;
    public final short[] iqB;
    public final long timestampNanos;

    public DualIQBuffer(short[] iqA, short[] iqB, long timestampNanos) {
        this.iqA = iqA;
        this.iqB = iqB;
        this.timestampNanos = timestampNanos;
    }
}
