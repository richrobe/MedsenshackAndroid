/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib;

import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.sensors.DsSensor;

/**
 * Created by gradl on 07.10.2015.
 */
public abstract class SensorDataProcessor {
    /**
     * This is called every time a new data frame (sample) is received from the sensor.
     *
     * @param data
     */
    public abstract void onNewData(SensorDataFrame data);

    /**
     * This is called after the sensor has internally been initialized. This depends on the sensor. However it is guaranteed that it will always be called before the
     * first connection attempt to the sensor is made.
     *
     * @param sensor
     */
    public void onSensorCreated(DsSensor sensor) {
    }

    /**
     * This is called after the sensor has been (successfully) connected.
     *
     * @param sensor
     */
    public void onConnected(DsSensor sensor) {
    }

    /**
     * This is called after the sensor has been disconnected.
     *
     * @param sensor
     */
    public void onDisconnected(DsSensor sensor) {
    }

    /**
     * This is called when the sensor is connected and starts streaming data.
     *
     * @param sensor
     */
    public void onStartStreaming(DsSensor sensor) {
    }

    /**
     * This is called when the sensor stopped streaming data but is still connected.
     *
     * @param sensor
     */
    public void onStopStreaming(DsSensor sensor) {
    }

    /**
     * Is sent when the sampling rate changed for the given sensor. This can happen e.g. when a sampling rate of 200 Hz is requested, but the sensor only supports 204 Hz,
     * then this will be sent as soon as the sensor reported its actually used sampling rate. This will ONLY be called if the new sampling rate is different from the old
     * one.
     *
     * @param sensor
     */
    public void onSamplingRateChanged(DsSensor sensor, double newSamplingRate) {
    }

    /**
     * Is sent when the sensor provides any informative notifications.
     *
     * @param sensor
     * @param notification
     */
    public void onNotify(DsSensor sensor, Object notification) {
    }
}
