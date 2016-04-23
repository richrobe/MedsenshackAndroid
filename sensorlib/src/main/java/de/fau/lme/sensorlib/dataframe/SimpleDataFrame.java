package de.fau.lme.sensorlib.dataframe;


import de.fau.lme.sensorlib.sensors.DsSensor;

/**
 * Created by gradl on 02.12.2015.
 */
public class SimpleDataFrame extends SensorDataFrame {
    int mIdentifier;
    double mValue;

    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SimpleDataFrame(DsSensor fromSensor, double timestamp, int identifier, double value) {
        super(fromSensor, timestamp);
        mIdentifier = identifier;
        mValue = value;
    }

    public int getIdentifier() {
        return mIdentifier;
    }

    public double getValue() {
        return mValue;
    }
}
