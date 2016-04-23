package de.fau.lme.sensorlib.dataframe;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class SimbleeMedhackGalvDataFrame extends SimbleeMedhackDataFrame implements GalvanicSkinResponseDataFrame {

    public double galv;
    public char label;
    public long timeStamp;

    public SimbleeMedhackGalvDataFrame(double galv, long timestamp) {
        super(null, timestamp);
        this.galv = galv;
        this.timeStamp = timestamp;
    }

    public SimbleeMedhackGalvDataFrame(double ecg, long timestamp, char label) {
        super(null, timestamp);
        this.galv = galv;
        this.timeStamp = timestamp;
    }

    @Override
    public double getGalvanicSkinResponse() {
        return 0;
    }
}
