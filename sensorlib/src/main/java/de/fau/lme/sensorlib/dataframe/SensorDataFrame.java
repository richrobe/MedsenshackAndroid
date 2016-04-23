/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.dataframe;

import de.fau.lme.sensorlib.sensors.DsSensor;

/**
 * Base class for all data frames coming from a sensor.
 *
 * @author gradl
 */
public class SensorDataFrame {
    /**
     * The sensor on which this data frame was generated.
     */
    protected DsSensor originatingSensor;

    /**
     * Timestamp in milliseconds when this data frame was generated on the sensor.
     */
    protected double timestamp;

    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SensorDataFrame(DsSensor fromSensor, double timestamp) {
        originatingSensor = fromSensor;
        this.timestamp = timestamp;
    }

    public DsSensor getOriginatingSensor() {
        return originatingSensor;
    }

    /**
     * @return the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public double getTimestamp() {
        return timestamp;
    }
}
