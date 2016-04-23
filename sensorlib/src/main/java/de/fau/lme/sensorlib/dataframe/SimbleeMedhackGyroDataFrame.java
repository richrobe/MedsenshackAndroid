package de.fau.lme.sensorlib.dataframe;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class SimbleeMedhackGyroDataFrame extends SimbleeMedhackDataFrame implements GyroDataFrame {

    public double gyroX;
    public double gyroY;
    public double gyroZ;
    public char label;
    public long timeStamp;

    public SimbleeMedhackGyroDataFrame(double gyroX, double gyroY, double gyroZ, long timestamp) {
        super(null, timestamp);
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.timeStamp = timestamp;
    }

    public SimbleeMedhackGyroDataFrame(double gyroX, double gyroY, double gyroZ, long timestamp, char label) {
        super(null, timestamp);
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.timeStamp = timestamp;
    }

    @Override
    public double getGyroX() {
        return 0;
    }

    @Override
    public double getGyroY() {
        return 0;
    }

    @Override
    public double getGyroZ() {
        return 0;
    }
}