package de.fau.lme.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import java.util.EnumSet;
import java.util.Iterator;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.AccelDataFrame;
import de.fau.lme.sensorlib.dataframe.BloodVolumePressureDataFrame;
import de.fau.lme.sensorlib.dataframe.HeartRateDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimpleDataFrame;

/**
 * Empatica sensor (https://www.empatica.com/)
 * <p/>
 * This sensor is a little more complex to operate:
 * (1) you need an API key you get after registering a developer account on their site.
 * (2) You dev-account/API-key needs to be assigned/registered to the sensors you want to use (in the dev-area on their dev-site).
 * (3) During the connection process (Android->Empatica sensor) an I-Net connection is required so the driver can fetch the authorization from the empatica-site that you are allowed to use the given sensor.
 * (4) Connections can only be made after the EmpaDeviceManager has been initialized, i.e. after receiving the EmpaStatus.READY status update. Any call to anything in the Empatica-API will fail with an exception before that!!
 * <p/>
 * Our connection scheme works as follows:
 * (1) the sensor is created, sensor state is UNDEFINED
 * (2) status update sends: READY, sensor state is INITIALIZED, or if it was CONNECTING we immediately start search the desired sensor
 * (3) if a connect call comes in and Empatica API is not READY yet, set sensor state to CONNECTING, otherwise, start search for the sensor
 * (4) sensor found notification: connect to the sensor
 */
public class EmpaticaSensor extends DsSensor {
    private static final String API_KEY = "91ccc9f23095423aa29af3802c737e41";
    private EmpaDeviceManager mDeviceManager = null;
    private BluetoothDevice mBtDevice;

    private static final double EMPA_TIMESTAMP_TO_MILLISECONDS = 1000d;

    public static class EmpaticaAccelDataFrame extends SensorDataFrame implements AccelDataFrame {
        double x;
        double y;
        double z;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public EmpaticaAccelDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public double getAccelX() {
            return x;
        }

        @Override
        public double getAccelY() {
            return y;
        }

        @Override
        public double getAccelZ() {
            return z;
        }
    }

    public static class EmpaticaBvpDataFrame extends SensorDataFrame implements BloodVolumePressureDataFrame {
        double mBloodVolumePressure;

        public EmpaticaBvpDataFrame(DsSensor fromSensor, double timestamp, double bvp) {
            super(fromSensor, timestamp);
            mBloodVolumePressure = bvp;
        }

        @Override
        public double getBloodVolumePressure() {
            return mBloodVolumePressure;
        }
    }

    public static class EmpaticaIbiDataFrame extends SensorDataFrame implements HeartRateDataFrame {
        double mIbi;

        public EmpaticaIbiDataFrame(DsSensor fromSensor, double timestamp, double ibi) {
            super(fromSensor, timestamp);
            mIbi = ibi;
        }

        @Override
        public double getHeartRate() {
            return 60d / mIbi;
        }

        @Override
        public double getInterbeatInterval() {
            return mIbi;
        }
    }


    private class EmpaticaInternalHandler extends InternalHandler implements EmpaDataDelegate, EmpaStatusDelegate {

        public EmpaticaInternalHandler(DsSensor sensor) {
            super(sensor);
        }

        @Override
        public void didUpdateStatus(EmpaStatus empaStatus) {
            Log.d(this.getClass().getSimpleName(), "didUpdateStatus: " + empaStatus.name());

            // ready signalized that the Empatica-API can be used
            if (empaStatus == EmpaStatus.READY) {
                sendSensorCreated();

                // if we have a connection request pending, we immediately start searching for the sensor.
                if (getState() == SensorState.CONNECTING) {
                    mDeviceManager.startScanning();
                } else {
                    // if no connection request was made, set sensor to initialized so we wait for a call to connect before searching for the sensor
                    setState(SensorState.INITIALIZED);
                }
            } else if (empaStatus == EmpaStatus.CONNECTED) {
                Log.d(getDeviceName(), "connected.");
                setState(SensorState.CONNECTED);
                sendConnected();
                startStreaming();
            } else if (empaStatus == EmpaStatus.DISCONNECTED) {
                if (getState() == SensorState.CONNECTED) {
                    stopStreaming();
                    sendDisconnected();
                }
            }

            if (empaStatus == EmpaStatus.CONNECTED || empaStatus == EmpaStatus.DISCONNECTED || empaStatus == EmpaStatus.READY)
                sendNotification(getDeviceName() + " status: " + empaStatus.toString());
        }

        @Override
        public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus, EmpaSensorType empaSensorType) {
            sendNotification(getDeviceName() + " status: " + empaSensorStatus.toString());
        }

        @Override
        public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
            Log.d(getDeviceName(), "didDiscoverDevice " + bluetoothDevice.getName() + "; " + allowed);

            if (allowed && bluetoothDevice.getAddress().equals(getSensor().getDeviceAddress())) {
                // Stop scanning. We found the right device.
                mDeviceManager.stopScanning();
                try {
                    // Connect to the device
                    Log.d(this.getClass().getSimpleName(), "connecting to  " + bluetoothDevice.getName());
                    mDeviceManager.connectDevice(bluetoothDevice);
                } catch (ConnectionNotAllowedException e) {
                    // This should happen only if you try to connect when allowed == false.
                    //Toast.makeText(this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            sendNotification("Connecting: " + deviceName);
        }

        @Override
        public void didRequestEnableBluetooth() {
            sendNotification("Bt enable requested.");
        }

        @Override
        public void didReceiveGSR(float gsr, double timestamp) {
            // galvanic skin response
            sendNewData(new SimpleDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, 0, gsr));
        }

        @Override
        public void didReceiveBVP(float bvp, double timestamp) {
            // blood volume pressure
            sendNewData(new EmpaticaBvpDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, bvp));
        }

        @Override
        public void didReceiveIBI(float ibi, double timestamp) {
            // Inter beat interval
            sendNewData(new EmpaticaIbiDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, ibi));
        }

        @Override
        public void didReceiveTemperature(float temp, double timestamp) {
            //Log.d( this.getClass().getSimpleName(), "Temp: " + temp );
            sendNewData(new SimpleDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, 3, temp));
        }

        @Override
        public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
            //Log.d( this.getClass().getSimpleName(), "Accel: " + x + " " + y + " " + z + ";  " + mExternalHandlers.size() );
            sendNewData(new EmpaticaAccelDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, x, y, z));
        }

        @Override
        public void didReceiveBatteryLevel(float level, double timestamp) {
            sendNewData(new SimpleDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, 4, level));
        }
    }


    public EmpaticaSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress, dataHandler);
        mContext = context;
        //init( context );
    }

    public EmpaticaSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        super(deviceName, deviceAddress, dataHandler, desiredSamplingRate);
        mContext = context;
        //init( context );
    }

    private void init(Context context) {
        mInternalHandler = new EmpaticaInternalHandler(this);
        mDeviceManager = new EmpaDeviceManager(context, (EmpaDataDelegate) mInternalHandler, (EmpaStatusDelegate) mInternalHandler);
        mDeviceManager.authenticateWithAPIKey(API_KEY);
        //sendSensorCreated();
    }

    @Override
    public boolean connect() throws Exception {
        //if (getState() == SensorState.INITIALIZED)
        {
            //  mDeviceManager.startScanning();
        }
        init(mContext);

        setState(SensorState.CONNECTING);

        return true;
    }

    @Override
    public void disconnect() {
        mDeviceManager.disconnect();
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ACCELEROMETER,
                HardwareSensor.BLOOD_VOLUME_PRESSURE,
                HardwareSensor.GALVANIC_SKIN_RESPONSE,
                HardwareSensor.HEART_RATE);
    }


}
