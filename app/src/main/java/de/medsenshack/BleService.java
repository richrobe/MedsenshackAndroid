package de.medsenshack;

import android.app.Service;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.Serializable;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.sensors.BleEcgSensor;
import de.fau.lme.sensorlib.sensors.DsSensor;
import de.fau.lme.sensorlib.sensors.SimbleeEcgSensor;
import de.lme.plotview.Plot;


/**
 * Original version by Tim Maiwald, Max Schaldach endowed professorship of Biomedical Engineering.
 * <p/>
 * Modified by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * This class represents the Service for the BLE-ECG device. It receives all new data, handles the
 * data processing and provides new information to the Activities.
 *
 * @author Tim Maiwald
 * @author Robert Richer
 */
public class BleService extends Service implements SignalNotifier {

    private static final String TAG = BleService.class.getSimpleName();
    /**
     * Static member variable containing all algorithms for the ECG processing according to
     * the algorithm provided by Pan and Tompkins.
     */
    //public static PanTompkins mPants;
    /**
     * Static member variable for the QRS detection validation.
     */
    private IBinder mBinder = new BleServiceBinder();
    private DailyHeartHandler mDailyHeartHandler;
    private double mSamplingRate;
    private Class mClass;
    private long mStartTime;

    //////////// NEW ECG LIB FEATURES ////////
    //public DailyHeartDataProcessor mProcessor;
    //////////////////////////////////////////

    private DsSensor mEcgSensor;
    private SensorDataProcessor mSensorDataProcessor = new SensorDataProcessor() {
        @Override
        public void onNewData(SensorDataFrame data) {
            if (data instanceof SimbleeEcgSensor.SimbleeDataFrame) {
                onSimbleeEvent((SimbleeEcgSensor.SimbleeDataFrame) data);
            }
        }

        @Override
        public void onSensorCreated(DsSensor sensor) {
            Log.d(TAG, "onSensorCreated");
            mDailyHeartHandler.onScanResult(true);
            try {
                sensor.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnected(DsSensor sensor) {
            Log.d(TAG, "onConnected");
            mDailyHeartHandler.onSensorConnected();
            try {
                sensor.startStreaming();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(DsSensor sensor) {
            Log.d(TAG, "onDisconnected");
            mDailyHeartHandler.onSensorDisconnected();
            if (((SimbleeEcgSensor) mEcgSensor).connectionLost) {
                mDailyHeartHandler.onSensorConnectionLost();
            }
            mEcgSensor = null;
        }

        @Override
        public void onStartStreaming(DsSensor sensor) {
            Log.d(TAG, "onStartStreaming");
            // initialize Pants with sampling rate
            //mPants = new PanTompkins((int) mSamplingRate);
            // Set start time
            mStartTime = System.currentTimeMillis();
            mDailyHeartHandler.onStartStreaming();
        }

        @Override
        public void onStopStreaming(DsSensor sensor) {
            Log.d(TAG, "onStopStreaming");
            sensor.disconnect();
            mDailyHeartHandler.onStopStreaming();
        }

        @Override
        public void onSamplingRateChanged(DsSensor sensor, double newSamplingRate) {
            Log.d(TAG, "onSamplingRateChanged");
            mSamplingRate = newSamplingRate;
        }

        @Override
        public void onNotify(DsSensor sensor, Object notification) {
            // TODO: add notifications
            if (notification instanceof String) {
                String message = (String) notification;
                Log.e(TAG, message);
                if (message.contains("Record empty")) {
                    mDailyHeartHandler.onMessageReceived(message);
                } else {
                    mDailyHeartHandler.onMessageReceived(Constants.MSG_PROGRESS, message);
                }
            }
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "New BLE device: " + result.getDevice().getName() + "@" + result.getRssi());
            if (result.getDevice() != null && Constants.SIMBLEE.equals(result.getDevice().getName())) {
                mEcgSensor = DsSensorManager.createSupportedSensor(result.getDevice(), mSensorDataProcessor, BleService.this);
                if (mEcgSensor != null) {
                    mEcgSensor.requestSamplingRateChange(250);
                }
            }
        }
    };

    public void sendSimblee(String msg) {
        ((SimbleeEcgSensor) mEcgSensor).send(msg.getBytes());
    }


    public void startSimblee() {
        DsSensorManager.searchBleDevice(mScanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DsSensorManager.cancelBleSearch(mScanCallback);
                if (mEcgSensor == null) {
                    mDailyHeartHandler.onScanResult(false);
                }
            }
        }, 5000);
    }

    public void stopBle() {
        if (mEcgSensor == null) {
            return;
        }
        mEcgSensor.stopStreaming();
        //mPants = null;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bond!");
        // IntentFilter and BroadcastReceiver for starting and stopping the service
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_STOP);
        intentFilter.addAction(Constants.ACTION_START);
        // Extract the name of the Activity that has started the service for the NotificationManager
        Serializable aClass = intent.getSerializableExtra(Constants.EXTRA_ACTIVITY_NAME);
        if (aClass instanceof Class<?>) {
            mClass = (Class) aClass;
        }
        //mDataProcessor = new DataProcessor(mServiceHandler);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DsSensorManager.cancelBleSearch(mScanCallback);
        // Destroy all running Notifications

        mBinder = null;
        mScanCallback = null;
        return false;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void onSimbleeEvent(SimbleeEcgSensor.SimbleeDataFrame data) {
        mDailyHeartHandler.onDataReceived(new BleEcgSensor.BleEcgDataFrame(data.ecgRaw, data.timeStamp));
    }

    /**
     * Handles all new incoming live and simulation data.
     *
     * @param data New data from a BLE_ECG device or from the Simulator
     */
    private void onBleEcgDataReceivedEvent(BleEcgSensor.BleEcgDataFrame data) {

        // send the raw data to the UI for plotting
        mDailyHeartHandler.onDataReceived(data);
    }

    public void setDailyHeartHandler(DailyHeartHandler handler) {
        mDailyHeartHandler = handler;
    }

    @Override
    public void onNewSignalValue(SensorDataProcessor dataProcessor, DsSensor sensor, int signalId, double value, long time) {

    }

    @Override
    public void onNewSignalMarker(SensorDataProcessor dataProcessor, DsSensor sensor, int signalId, int index, Plot.PlotMarker marker) {

    }

    /**
     * Inner class representing the Binder between {@link BleService}
     * and {@link android.app.Activity}
     */
    public class BleServiceBinder extends Binder {

        public BleService getService() {
            return BleService.this;
        }
    }
}
