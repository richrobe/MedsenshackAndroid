package de.fau.lme.sensorlib.dataframe;

import de.fau.lme.sensorlib.sensors.DsSensor;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public  abstract class SimbleeMedhackDataFrame extends SensorDataFrame {
    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SimbleeMedhackDataFrame(DsSensor fromSensor, double timestamp) {
        super(fromSensor, timestamp);
    }
}
