package de.fau.lme.sensorlib.dataframe;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class SimbleeMedhackAccDataFrame extends SimbleeMedhackDataFrame implements AccelDataFrame {

    public double accX;
    public double accY;
    public double accZ;
    public char label;
    public long timeStamp;

    public SimbleeMedhackAccDataFrame(double accX, double accY, double accZ, long timestamp) {
        super(null, timestamp);
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.timeStamp = timestamp;
    }

    public SimbleeMedhackAccDataFrame(double ecg, long timestamp, char label) {
        super(null, timestamp);
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.timeStamp = timestamp;
    }

    @Override
    public double getAccelX() {
        return accX;
    }

    @Override
    public double getAccelY() {
        return accY;
    }

    @Override
    public double getAccelZ() {
        return accZ;
    }
}