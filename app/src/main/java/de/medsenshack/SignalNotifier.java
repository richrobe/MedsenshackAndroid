package de.medsenshack;

import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.sensors.DsSensor;
import de.fau.lme.plotview.Plot;

/**
 * Created by Robert on 16.01.16.
 */
public interface SignalNotifier {

    void onNewSignalValue(SensorDataProcessor dataProcessor, DsSensor sensor, int signalId, double value, long time);

    void onNewSignalMarker(SensorDataProcessor dataProcessor, DsSensor sensor, int signalId, int index, Plot.PlotMarker marker);

}
