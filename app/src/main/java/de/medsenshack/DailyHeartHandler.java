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
