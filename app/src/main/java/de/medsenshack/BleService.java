package de.medsenshack;

import android.app.Service;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackAccDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackEcgDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackGalvDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackGyroDataFrame;
import de.fau.lme.sensorlib.sensors.DsSensor;
import de.fau.lme.sensorlib.sensors.SimbleeEcgSensor;
import de.fau.lme.plotview.Plot;
import de.medsenshack.data.storage.AccDataWriter;
import de.medsenshack.data.storage.EcgDataWriter;
import de.medsenshack.data.storage.GalvDataWriter;
import de.medsenshack.data.storage.GyroDataWriter;


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
    private long mStartTime;
    private AccDataWriter accWriter;
    private EcgDataWriter ecgWriter;
    private GalvDataWriter galvWriter;
    private GyroDataWriter gyroWriter;

    //////////// NEW ECG LIB FEATURES ////////
    //public DailyHeartDataProcessor mProcessor;
    //////////////////////////////////////////

    private DsSensor mSimbleeSensor;
    private SensorDataProcessor mSensorDataProcessor = new SensorDataProcessor() {
        @Override
        public void onNewData(SensorDataFrame data) {
            if (data instanceof SimbleeMedhackDataFrame) {
                onSimbleeEvent((SimbleeMedhackDataFrame) data);
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
            if (((SimbleeEcgSensor) mSimbleeSensor).connectionLost) {
                mDailyHeartHandler.onSensorConnectionLost();
            }
            mSimbleeSensor = null;
        }

        @Override
        public void onStartStreaming(DsSensor sensor) {
            Log.d(TAG, "onStartStreaming");
            // initialize Pants with sampling rate
            //mPants = new PanTompkins((int) mSamplingRate);
            // Set start time
            mStartTime = System.currentTimeMillis();
            accWriter = new AccDataWriter("acc");
            ecgWriter = new EcgDataWriter("ecg");
            galvWriter = new GalvDataWriter("galv");
            gyroWriter = new GyroDataWriter("gyro");
            accWriter.prepareWriter(10);
            ecgWriter.prepareWriter(250);
            galvWriter.prepareWriter(10);
            gyroWriter.prepareWriter(10);
            mDailyHeartHandler.onStartStreaming();
        }

        @Override
        public void onStopStreaming(DsSensor sensor) {
            Log.d(TAG, "onStopStreaming");
            sensor.disconnect();
            accWriter.completeWriter();
            ecgWriter.completeWriter();
            galvWriter.completeWriter();
            gyroWriter.completeWriter();
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
                mSimbleeSensor = DsSensorManager.createSupportedSensor(result.getDevice(), mSensorDataProcessor, BleService.this);
                if (mSimbleeSensor != null) {
                    mSimbleeSensor.requestSamplingRateChange(250);
                }
            }
        }
    };


    public void startSimblee() {
        DsSensorManager.searchBleDevice(mScanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DsSensorManager.cancelBleSearch(mScanCallback);
                if (mSimbleeSensor == null) {
                    mDailyHeartHandler.onScanResult(false);
                }
            }
        }, 5000);
    }

    public void stopBle() {
        if (mSimbleeSensor == null) {
            return;
        }
        mSimbleeSensor.stopStreaming();
        //mPants = null;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bond!");
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

    private void onSimbleeEvent(SimbleeMedhackDataFrame data) {
        mDailyHeartHandler.onDataReceived(data);
        if(data instanceof SimbleeMedhackAccDataFrame){
            accWriter.writeData(data);
        } else if (data instanceof SimbleeMedhackEcgDataFrame) {
            ecgWriter.writeData(data);
        } else if (data instanceof SimbleeMedhackGalvDataFrame) {
            galvWriter.writeData(data);
        } else if (data instanceof SimbleeMedhackGyroDataFrame){
            gyroWriter.writeData(data);
        }
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
