/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.sensors;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.EnumSet;

import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by gradl on 29.09.2015.
 */
public abstract class DsSensor {

    protected static final int MESSAGE_NEW_DATA = 1010;
    protected static final int MESSAGE_NOTIFICATION = 1011;
    protected static final int MESSAGE_SENSOR_CREATED = 1012;
    protected static final int MESSAGE_CONNECTED = 1013;
    protected static final int MESSAGE_DISCONNECTED = 1014;
    protected static final int MESSAGE_START_STREAMING = 1015;
    protected static final int MESSAGE_STOP_STREAMING = 1016;
    protected static final int MESSAGE_SAMPLING_RATE_CHANGED = 1017;

    /**
     * The address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    protected String mDeviceAddress = "n/a";
    protected String mName = "Unknown";
    protected double mSamplingRate;
    protected Context mContext;
    protected InternalHandler mInternalHandler;
    protected ArrayList<SensorDataProcessor> mExternalHandlers = new ArrayList<>(2);
    protected EnumSet<HardwareSensor> mHardwareSensors = EnumSet.noneOf(HardwareSensor.class);
    protected EnumSet<HardwareSensor> mSelectedHwSensors = EnumSet.noneOf(HardwareSensor.class);
    private SensorState mSensorState = SensorState.UNDEFINED;

    /**
     * Possible supported hardware sensors.
     */
    public enum HardwareSensor {
        ACCELEROMETER,
        ECG,
        EMG,
        GYROSCOPE,
        MAGNETOMETER,
        LIGHT,
        PRESSURE,
        TEMPERATURE,
        RESPIRATION,
        HEART_RATE,
        BLOOD_VOLUME_PRESSURE,
        GALVANIC_SKIN_RESPONSE
    }

    public enum Simulator {
        DAILYHEART,
        MIT_BIH
    }

    /**
     * The states a DsSensor can be in.
     */
    public enum SensorState {
        UNDEFINED,
        DISCONNECTED,
        INITIALIZED,
        CONNECTING,
        CONNECTED,
        STREAMING
    }

    /**
     * The default internal handler class used if no custom class is implemented.
     */
    protected static class InternalHandler extends Handler {
        private DsSensor mSensor;

        public InternalHandler(DsSensor sensor) {
            mSensor = sensor;
        }

        public DsSensor getSensor() {
            return mSensor;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                //handlers have a what identifier which is used to identify the type of msg
                switch (msg.what) {
                    case DsSensor.MESSAGE_NEW_DATA:
                        //Log.e( this.getClass().getSimpleName(), "Received new data " + getSensor().getDeviceName() + " :: " + msg.obj );
                        getSensor().dispatchNewData((SensorDataFrame) msg.obj);
                        break;

                    case DsSensor.MESSAGE_NOTIFICATION:
                        getSensor().dispatchNotification(msg.obj);
                        break;

                    case DsSensor.MESSAGE_CONNECTED:
                        getSensor().dispatchConnected();
                        break;

                    case DsSensor.MESSAGE_SENSOR_CREATED:
                        getSensor().dispatchSensorCreated();
                        break;

                    case DsSensor.MESSAGE_START_STREAMING:
                        getSensor().dispatchStartStreaming();
                        break;

                    case DsSensor.MESSAGE_STOP_STREAMING:
                        getSensor().dispatchStopStreaming();
                        break;

                    case DsSensor.MESSAGE_DISCONNECTED:
                        getSensor().dispatchDisconnected();
                        break;

                    case DsSensor.MESSAGE_SAMPLING_RATE_CHANGED:
                        getSensor().dispatchSamplingRateChanged();
                        break;

                    default:
                        Log.e(this.getClass().getSimpleName(), "Unknown message received.");
                        break;
                }
            } catch (Exception e) {
                //some unknown error occurred
                Log.e(this.getClass().getSimpleName(), "An error occured on sensor data processing!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Default constructor. IMPORTANT: do not initialize any static or sensor-side-specific components here. DsSensor classes are constructed very often any only temporarily.
     * If you need to initialize something on the sensor-side etc do this in the connect method.
     *
     * @param deviceName    the name of the device/sensor.
     * @param deviceAddress the addess (bluetooth, IP, etc.) that is used to contact the sensor.
     * @param dataHandler   a default/initial data handler for sensor data and notifications.
     */
    public DsSensor(String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        this.mName = deviceName;
        mDeviceAddress = deviceAddress;
        mHardwareSensors = providedSensors();
        mInternalHandler = new InternalHandler(this);
        addDataHandler(dataHandler);
    }

    /**
     * Optional additional constructor. IMPORTANT: read info on the default constructor!
     *
     * @param deviceName
     * @param deviceAddress
     * @param dataHandler
     * @param desiredSamplingRate
     */
    public DsSensor(String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        this(deviceName, deviceAddress, dataHandler);
        mSamplingRate = desiredSamplingRate;
    }

    /**
     * Opens a connection to the sensor.
     *
     * @return true if the connection has been established successfully. False otherwise.
     * @throws Exception
     */
    public abstract boolean connect() throws Exception;

    /**
     * Closes the connection to the sensor.
     */
    public abstract void disconnect();

    /**
     * Requests the data streaming to begin. Depending on the sensor, it might not be necessary to call this, since streaming begins automatically after a connection has been established.
     */
    public abstract void startStreaming();

    /**
     * Requests the data streaming to stop. Depending on the sensor, this might induce a disconnect.
     */
    public abstract void stopStreaming();

    /**
     * @return the address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    /**
     * @return a not necessarily unique, human readable name for this sensor.
     */
    public String getDeviceName() {
        return mName;
    }

    /**
     * @return the sampling rate for the current sensor connection in Hz.
     */
    public double getSamplingRate() {
        return mSamplingRate;
    }

    /**
     * Sets the sampling rate for this sensor.
     *
     * @param samplingRate the sampling rate to set.
     */
    protected void setSamplingRate(double samplingRate) {
        if (samplingRate != this.mSamplingRate) {
            this.mSamplingRate = samplingRate;
            sendSamplingRateChanged();
        }
        this.mSamplingRate = samplingRate;
    }

    /**
     * Requests a change of the sampling rate for this sensor. This may or may not succeed, depending on the sensor and its internal state. If successful, the new sampling rate will be reported via the SensorDataProcessor.onSamplingRateChanged notification.
     *
     * @param toSamplingRate the sampling rate to which the sensor should switch.
     * @return true if the sampling rate can change, false if it is definitely not possible
     */
    public boolean requestSamplingRateChange(double toSamplingRate) {
        setSamplingRate(toSamplingRate);
        return true;
    }

    protected void setState(SensorState newState) {
        mSensorState = newState;
    }

    public SensorState getState() {
        return mSensorState;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    /**
     * @param handler
     * @param context
     * @deprecated Use {@link DsSensor#addDataHandler} instead.
     */
    public void setHandler(SensorDataProcessor handler, Context context) {
        addDataHandler(handler);
        this.mContext = context;
    }

    /**
     * Adds an additional SensorDataProcessor to this sensor.
     *
     * @param handler
     */
    public void addDataHandler(SensorDataProcessor handler) {
        if (handler == null) {
            return;
        }

        for (SensorDataProcessor sdp : mExternalHandlers) {
            if (sdp == handler)
                return;
        }

        mExternalHandlers.add(handler);
    }

    /**
     * Selects the specified hardware sensors to be used and data from them reported back for this DsSensor. If the selected hardware sensors are not available the returned values are undefined for these types.
     *
     * @param hwSensors the requested (internal) hardware sensors from which data is needed.
     */
    public void selectHardwareSensors(EnumSet<HardwareSensor> hwSensors) {
        mSelectedHwSensors = hwSensors;
    }

    /**
     * Requests the specified hardware sensor to be used and data from it reported back for this DsSensor. If the requested hardware is not available the returned values are undefined. This is a convenience method for selectHardwareSensors.
     *
     * @param sensor the requested (internal) hardware sensor from which data is needed.
     */
    public void requestHardwareSensor(HardwareSensor sensor) {
        mSelectedHwSensors.add(sensor);
    }

    /**
     * @return an enum containing all the available/provided hardware sensors.
     */
    public EnumSet<HardwareSensor> getHardwareSensors() {
        return mHardwareSensors;
    }

    /**
     * Checks for availability of a (hardware) sensor unit.
     *
     * @param hwSensor the sensor/capability to check for.
     * @return true if the sensor/capability is present, false if not.
     */
    public boolean hasHardwareSensor(HardwareSensor hwSensor) {
        return mHardwareSensors.contains(hwSensor);
    }

    /**
     * Informs about all (internal) existing/provided hardware sensors for this DsSensor. This has to be implemented by all sensor implementations.
     *
     * @return an enum containing all the available/provided hardware sensors.
     */
    protected abstract EnumSet<HardwareSensor> providedSensors();

    /**
     * Sends a onNotify to all external handlers.
     *
     * @param notification
     */
    protected void sendNotification(Object notification) {
        mInternalHandler.obtainMessage(MESSAGE_NOTIFICATION, notification).sendToTarget();
    }

    protected void dispatchNotification(Object notification) {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onNotify(this, notification);
        }
    }

    protected void sendNewData(SensorDataFrame data) {
        //Log.d( this.getClass().getSimpleName(), "new data " + data.getOriginatingSensor() );
        mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, data).sendToTarget();
    }

    protected void dispatchNewData(SensorDataFrame data) {
        //Log.d( this.getClass().getSimpleName(), "dispatch " + mExternalHandlers.size() );
        for (SensorDataProcessor sdp : mExternalHandlers) {
            //Log.d( this.getClass().getSimpleName(), "dispatch to " + sdp.getClass().getSimpleName() );
            sdp.onNewData(data);
        }
    }

    protected void sendSensorCreated() {
        mInternalHandler.obtainMessage(MESSAGE_SENSOR_CREATED).sendToTarget();
    }

    protected void dispatchSensorCreated() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSensorCreated(this);
        }
    }

    protected void sendConnected() {
        mInternalHandler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
    }

    protected void dispatchConnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnected(this);
        }
    }

    protected void sendDisconnected() {
        mInternalHandler.obtainMessage(MESSAGE_DISCONNECTED).sendToTarget();
    }

    protected void dispatchDisconnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onDisconnected(this);
        }
    }

    protected void sendStartStreaming() {
        mInternalHandler.obtainMessage(MESSAGE_START_STREAMING).sendToTarget();
    }

    protected void dispatchStartStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStartStreaming(this);
        }
    }

    protected void sendStopStreaming() {
        mInternalHandler.obtainMessage(MESSAGE_STOP_STREAMING).sendToTarget();
    }

    protected void dispatchStopStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStopStreaming(this);
        }
    }

    protected void sendSamplingRateChanged() {
        mInternalHandler.obtainMessage(MESSAGE_SAMPLING_RATE_CHANGED).sendToTarget();
    }

    protected void dispatchSamplingRateChanged() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSamplingRateChanged(this, getSamplingRate());
        }
    }
}
