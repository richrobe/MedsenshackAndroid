package de.fau.lme.sensorlib.dataframe;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class SimbleeMedhackEcgDataFrame extends SimbleeMedhackDataFrame implements EcgDataFrame {

    public double ecgRaw;
    public char label;
    public long timeStamp;

    public SimbleeMedhackEcgDataFrame(double ecg, long timestamp) {
        super(null, timestamp);
        this.ecgRaw = ecg;
        this.timeStamp = timestamp;
    }

    public SimbleeMedhackEcgDataFrame(double ecg, long timestamp, char label) {
        super(null, timestamp);
        this.ecgRaw = ecg;
        this.timeStamp = timestamp;
    }

    @Override
    public double getEcgSample() {
        return ecgRaw;
    }

    @Override
    public double getSecondaryEcgSample() {
        return 0;
    }
}
