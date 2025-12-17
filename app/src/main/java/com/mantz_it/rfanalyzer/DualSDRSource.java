package com.mantz_it.rfanalyzer;

import android.content.Context;
import java.util.concurrent.ArrayBlockingQueue;

public class DualSDRSource implements DualIQSourceInterface,
        IQSourceInterface,
        IQSourceInterface.Callback {

    public final IQSourceInterface srcA;
    public final IQSourceInterface srcB;

    private final ArrayBlockingQueue<IQBuffer> queueA = new ArrayBlockingQueue<>(8);
    private final ArrayBlockingQueue<IQBuffer> queueB = new ArrayBlockingQueue<>(8);

    private Callback callback;
    private Context context;

    public DualSDRSource(IQSourceInterface a, IQSourceInterface b) {
        this.srcA = a;
        this.srcB = b;
    }

    // --------------------------------------------------------------------
    // IQSourceInterface
    // --------------------------------------------------------------------

    @Override
    public boolean open(Callback cb, Context ctx) {
        this.callback = cb;
        this.context = ctx;

        boolean okA = srcA.open(this, ctx);
        boolean okB = srcB.open(this, ctx);

        return okA && okB;
    }

    @Override
    public void close() {
        srcA.close();
        srcB.close();
        queueA.clear();
        queueB.clear();
    }

    @Override
    public boolean isOpen() {
        return srcA.isOpen() && srcB.isOpen();
    }

    @Override
    public int getSampleRate() {
        return srcA.getSampleRate();
    }

    @Override
    public int getMaxSampleRate() {
        return srcA.getMaxSampleRate();
    }

    @Override
    public long getFrequency() {
        return srcA.getFrequency();
    }

    @Override
    public long getMaxFrequency() {
        return srcA.getMaxFrequency();
    }

    @Override
    public long getMinFrequency() {
        return srcA.getMinFrequency();
    }

    @Override
    public int getPacketSize() {
        return srcA.getPacketSize();
    }

    @Override
    public void setFrequency(long frequency) {
        srcA.setFrequency(frequency);
        srcB.setFrequency(frequency);
    }

    @Override
    public void setSampleRate(int sampleRate) {
        srcA.setSampleRate(sampleRate);
        srcB.setSampleRate(sampleRate);
    }

    @Override
    public String getName() {
        return srcA.getName() + " + " + srcB.getName();
    }

    // --------------------------------------------------------------------
    // DualIQSourceInterface
    // --------------------------------------------------------------------

    @Override
    public DualIQBuffer getNextBuffer() throws InterruptedException {

        IQBuffer a = queueA.take();
        IQBuffer b = queueB.take();

        // Sanity check: packet sizes MUST match
        if (a.getIq().length != b.getIq().length) {
            throw new IllegalStateException("Packet size mismatch between SDRs");
        }

        return new DualIQBuffer(
                a.getIq(),
                b.getIq(),
                Math.max(a.getTimestamp(), b.getTimestamp())
        );
    }

    // --------------------------------------------------------------------
    // IQSourceInterface.Callback
    // --------------------------------------------------------------------

    @Override
    public void onIQSourceReady(IQSourceInterface source) {
        if (srcA.isOpen() && srcB.isOpen() && callback != null) {
            callback.onIQSourceReady(this);
        }
    }

    @Override
    public void onIQSourceError(IQSourceInterface source, String message) {
        if (callback != null) {
            callback.onIQSourceError(this, message);
        }
    }

    @Override
    public void onIQBufferReady(IQSourceInterface source, IQBuffer buffer) {

        try {
            if (source == srcA) {
                queueA.put(buffer); // BLOCKING — preserves alignment
            } else if (source == srcB) {
                queueB.put(buffer);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
