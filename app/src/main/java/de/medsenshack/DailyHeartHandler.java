package de.medsenshack;

import de.fau.lme.sensorlib.dataframe.SimbleeMedhackDataFrame;
import de.fau.lme.sensorlib.sensors.BleEcgSensor;
import de.fau.lme.plotview.Plot;

/**
 * Original version by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * Interface that all classes receiving ECG data (live or simulated) have to implement.
 *
 * @author Robert Richer
 */
public interface DailyHeartHandler {

    /**
     * ANDROID WEAR
     * <p/>
     * String path for sending a message from Handheld to Wear when the ECG device
     * started streaming to the handheld.
     */
    String PATH_START_STREAMING = "/handheld/start-streaming";

    /**
     * ANDROID WEAR
     * <p/>
     * String path for sending a message from Handheld to Wear when a heart beat was detected.
     */
    String PATH_HEART_BEAT = "/handheld/heart-beat";
    /**
     * ANDROID WEAR
     * <p/>
     * String path for sending a message from Handheld to Wear when new heart rate
     * is available.
     */
    String PATH_HEART_RATE = "/handheld/heart-rate";
    /**
     * ANDROID WEAR
     * <p/>
     * String path for sending a message from Handheld to Wear with average heart date
     * (at the end of measurement).
     */
    String PATH_HEART_RATE_AVG = "/handheld/heart-avg";

    void onScanResult(boolean sensorFound);

    /**
     * Is called when the {@link BleEcgSensor} sends a message that the device
     * is starting to stream.
     */
    void onStartStreaming();

    void onStopStreaming();

    void onMessageReceived(Object... message);

    void onSensorConnected();

    void onSensorDisconnected();

    void onSensorConnectionLost();

    /**
     * Is called when the {@link BleService} sends a message that a QRS
     * complex has been segmented.
     */
    void onSegmentationFinished();

    /**
     * Handles the new incoming ECG data. Is called when the {@link BleService} sends a message with new
     * available ECG data.
     *
     * @param data the data received from the {@link BleService}
     */
    void onDataReceived(SimbleeMedhackDataFrame data);

    void onPlotMarker(Plot.PlotMarker marker, int signalId, int index);

}
