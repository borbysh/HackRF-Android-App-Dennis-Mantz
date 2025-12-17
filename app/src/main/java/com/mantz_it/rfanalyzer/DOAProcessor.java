package com.mantz_it.rfanalyzer;

public class DOAProcessor {

    private final double antennaSpacingMeters;
    private final double wavelengthMeters;

    public DOAProcessor(double antennaSpacingMeters, double frequencyHz) {
        this.antennaSpacingMeters = antennaSpacingMeters;
        this.wavelengthMeters = 3e8 / frequencyHz;
    }

    public void process(short[] iqA, short[] iqB) {
        double phaseA = estimatePhase(iqA);
        double phaseB = estimatePhase(iqB);

        double delta = phaseB - phaseA;
        double doa = Math.asin((delta * wavelengthMeters) /
                (2 * Math.PI * antennaSpacingMeters));

        // TODO: publish or log DOA
    }

    private double estimatePhase(short[] iq) {
        double sumI = 0, sumQ = 0;
        for (int i = 0; i < iq.length; i += 2) {
            sumI += iq[i];
            sumQ += iq[i + 1];
        }
        return Math.atan2(sumQ, sumI);
    }
}
