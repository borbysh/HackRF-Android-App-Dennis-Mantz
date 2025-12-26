package com.mantz_it.rfanalyzer;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that reads byte packets from an IQ source and converts them
 * into SamplePacket objects that are pushed into a provided queue for further processing.
 *
 * This class is lightweight and expects the source to be started (startSampling()) before run().
 */
public class SecondarySampler extends Thread {
    private static final String LOGTAG = "SecondarySampler";
    private volatile boolean stopRequested = false;
    private final IQSourceInterface source;
    private final ArrayBlockingQueue<SamplePacket> outputQueue;

    public SecondarySampler(IQSourceInterface source, ArrayBlockingQueue<SamplePacket> outputQueue) {
        super("SecondarySampler");
        this.source = source;
        this.outputQueue = outputQueue;
    }

    public boolean isRunning() {
        return !stopRequested;
    }

    public void requestStop() {
        this.stopRequested = true;
        this.interrupt();
    }

    @Override
    public void run() {
        Log.i(LOGTAG, "SecondarySampler started");
        // Allocate a SamplePacket sized to hold the typical number of complex samples per packet.
        // Many sources produce PACKET_SIZE bytes; each complex sample is I+Q (2 bytes for 8bit IQ),
        // so capacity ~ packetSize/2 complex samples.
        int packetSize = source.getPacketSize();
        int sampleCapacity = Math.max(256, packetSize / 2); // defensive fallback
        while (!stopRequested) {
            try {
                byte[] packet = source.getPacket(1000);
                if (packet == null) {
                    // timeout or end of stream; continue (or break if source closed)
                    if (!source.isOpen()) {
                        Log.i(LOGTAG, "Source closed or not open; stopping secondary sampler");
                        break;
                    }
                    continue;
                }

                // Convert into SamplePacket
                SamplePacket sp = new SamplePacket(sampleCapacity);
                sp.setSize(0);
                int samplesAdded = source.fillPacketIntoSamplePacket(packet, sp);
                if (samplesAdded > 0) {
                    sp.setSampleRate(source.getSampleRate());
                    sp.setFrequency(source.getFrequency());
                    // Offer to queue (non-blocking with small timeout to avoid blocking indefinitely)
                    boolean offered = outputQueue.offer(sp, 200, TimeUnit.MILLISECONDS);
                    if (!offered) {
                        // queue full: drop oldest? For now log and drop this packet.
                        Log.w(LOGTAG, "Output queue full; dropping secondary packet");
                    }
                }

                // return packet to source buffer pool
                source.returnPacket(packet);
            } catch (InterruptedException e) {
                // interrupted -> check if stopRequested
                if (stopRequested) break;
            } catch (Exception e) {
                Log.e(LOGTAG, "Error in SecondarySampler: " + e.getMessage(), e);
                // On error, continue unless the source is closed
                if (!source.isOpen()) break;
            }
        }
        Log.i(LOGTAG, "SecondarySampler stopped");
    }
}