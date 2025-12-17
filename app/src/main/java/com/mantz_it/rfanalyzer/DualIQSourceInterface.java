package com.mantz_it.rfanalyzer;

public interface DualIQSourceInterface {

    interface Callback {
        void onDualIQReady();
        void onDualIQError(String message);
    }

    boolean open(Callback callback);
    void close();

    boolean isOpen();

    int getSampleRate();
    int getPacketSize();

    void setFrequency(long frequency);
    void setSampleRate(int sampleRate);

    /**
     * Blocking call that returns time-aligned IQ buffers
     */
    DualIQBuffer getNextBuffer() throws InterruptedException;
}
