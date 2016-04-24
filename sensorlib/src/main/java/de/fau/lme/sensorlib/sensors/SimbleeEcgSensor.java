package de.fau.lme.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.EcgDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by Robert on 03.01.16.
 */
public class SimbleeEcgSensor extends DsSensor {

    private static final String TAG = SimbleeEcgSensor.class.getSimpleName();

    public final static String ACTION_CONNECTED =
            "com.simblee.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.simblee.ACTION_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.simblee.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.simblee.EXTRA_DATA";


    // ECG SERVICE
    public final static UUID UUID_SERVICE = UUID.fromString("00000FE84-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_RECEIVE = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_SEND = UUID.fromString("2d30c083-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_DISCONNECT = UUID.fromString("2d30c084-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("000002902-0000-1000-8000-00805F9B34FB");


    private Context mContext;
    private BleEcgDataWriter mWriter;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;

    public boolean connectionLost;

    /**
     * GATT callback for the communication with the Bluetooth remote device
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();

            Log.e(TAG, "error: " + status + ", " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED) {
                 /*
                 * Once successfully connected, we must next discover all the
				 * services on the device before we can read and write their
				 * characteristics.
				 */
                sendConnected();
                sendNotification("Discovering services...");

            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {

                 /*
                 * If at any point we disconnect, send a message to clear the
				 * values out of the UI
				 */
                if (name.equals(mName)) {
                    connectionLost = false;
                    sendDisconnected();
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * if there is a failure at any stage, simply disconnect
                 */
                if (name.equals(mName)) {
                    connectionLost = true;
                    sendDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Log.e(TAG, "Simblee GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Log.e(TAG, "Simblee receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "Simblee receive characteristic not found!");
                }

                setState(SensorState.STREAMING);
                sendStartStreaming();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mName.equals(gatt.getDevice().getName()) && UUID_RECEIVE.equals(characteristic.getUuid())) {
                    byte[] values = characteristic.getValue();
                    for (byte value : values) {
                        sendNewData(new SimbleeDataFrame(value, timeStamp++));
                    }
                }
            }
        }

        private long timeStamp;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getName().equals(mName)) {
                byte[] values = characteristic.getValue();
                //Log.d(TAG, "values: " + Arrays.toString(values));
                for (byte value : values) {
                    sendNewData(new SimbleeDataFrame(value, timeStamp++));
                }
            }
        }
    };

    public SimbleeEcgSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress, dataHandler);
        mContext = context;
        sendSensorCreated();
    }

    @Override
    public boolean connect() throws Exception {
        // Previously connected device.  Try to reconnect.
        if (mDeviceAddress != null && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        BluetoothDevice device = DsSensorManager.findBtDevice(mDeviceAddress);
        if (device != null) {
            mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
            mWriter = new BleEcgDataWriter();
            sendNotification("Connecting to... " + device.getName());
            return true;
        }

        return false;
    }

    public boolean send(byte[] data) {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                mBluetoothGattService.getCharacteristic(UUID_SEND);

        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public void startStreaming() {
        Log.d(TAG, "start streaming");
        mBluetoothGatt.discoverServices();
    }

    @Override
    public void stopStreaming() {
        Log.d(TAG, "stop streaming");
        if (mWriter != null) {
            mWriter.completeWriter();
        }
        mBluetoothGatt.disconnect();
        sendStopStreaming();
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ECG, HardwareSensor.ACCELEROMETER, HardwareSensor.MAGNETOMETER);
    }

    /**
     * Schedules the writing of the device's raw ECG data
     *
     * @param ecgData The raw ECG data from the Bluetooth device
     */
    private void scheduleWriting(final SimbleeDataFrame ecgData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mWriter != null) {
                    mWriter.writeData(ecgData);
                }
            }
        }).start();
    }

    public static class SimbleeDataFrame extends SensorDataFrame implements EcgDataFrame {

        public double ecgRaw;
        public char label;
        public long timeStamp;

        public SimbleeDataFrame(double ecg, long timestamp) {
            super(null, timestamp);
            this.ecgRaw = ecg;
            this.timeStamp = timestamp;
        }

        public SimbleeDataFrame(double ecg, long timestamp, char label) {
            super(null, timestamp);
            this.ecgRaw = ecg;
            this.timeStamp = timestamp;
        }

        @Override
        public double getEcgSample() {
            return ecgRaw;
        }

        @Override
        public double getSecondaryEcgSample() {
            return 0;
        }
    }


    /**
     * Original version by Tim Maiwald, Max Schaldach endowed professorship of Biomedical Engineering.
     * <p/>
     * Modified by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
     * <p/>
     * FAU Erlangen-Nürnberg
     * <p/>
     * (c) 2014
     *
     * @author Tim Maiwald
     * @author Robert Richer
     */
    public class BleEcgDataWriter {

        private static final char mSeparator = '\n';
        private static final String mHeader = "samplingrate";
        private String mName;
        private BufferedWriter mBufferedWriter;
        private File mECGFileHandler;
        private boolean mStorageWritable;
        private boolean mECGFileCreated;

        /**
         * Creates a new DataWriter to write the received ECG data to the external storage
         */
        public BleEcgDataWriter() {
            String[] parts = mDeviceAddress.split(":");
            String name = parts[parts.length - 2] + parts[parts.length - 1];
            if (parts.length > 1) {
                mName = name;
            } else {
                mName = "XXXX";
            }
            // create file
            createFile();
        }

        private void createFile() {
            String state;
            File root = null;
            File path;
            String currentTimeString;

            // set current time
            currentTimeString = new SimpleDateFormat("dd.MM.yy_HH.mm_", Locale.getDefault()).format(new Date());

            // try to write on SD card
            state = Environment.getExternalStorageState();
            switch (state) {
                case Environment.MEDIA_MOUNTED:
                    // media readable and writable
                    root = Environment.getExternalStorageDirectory();
                    mStorageWritable = true;
                    break;
                case Environment.MEDIA_MOUNTED_READ_ONLY:
                    // media only readable
                    mStorageWritable = false;
                    Log.e(TAG, "SD card only readable!");
                    break;
                default:
                    // not readable or writable
                    mStorageWritable = false;
                    Log.e(TAG, "SD card not readable and writable!");
                    break;
            }

            if (!mStorageWritable) {
                // try to write on external storage
                root = Environment.getDataDirectory();
                if (root.canWrite()) {
                    mStorageWritable = true;
                } else {
                    Log.e(TAG, "External storage not readable and writable!");
                }
            }

            if (mStorageWritable) {
                try {
                    // create directory
                    path = new File(root, "DailyHeartRecordings");
                    mECGFileCreated = path.mkdir();
                    if (!mECGFileCreated) {
                        mECGFileCreated = path.exists();
                        if (!mECGFileCreated) {
                            Log.e(TAG, "File could not be created!");
                            return;
                        } else {
                            Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                        }
                    }
                    // create files
                    mECGFileHandler = new File(path + "/dailyheart_" + currentTimeString + mName + ".csv");
                    mECGFileCreated = mECGFileHandler.createNewFile();
                    if (!mECGFileCreated) {
                        mECGFileCreated = mECGFileHandler.exists();
                        if (!mECGFileCreated) {
                            Log.e(TAG, "File could not be created!");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mECGFileCreated = false;
                }
            }
        }

        /**
         * Prepares the {@link BleEcgDataWriter}
         *
         * @param samplingRate The sensor's sampling rate
         */
        public void prepareWriter(double samplingRate) {
            FileWriter fw;
            if (mStorageWritable && mECGFileCreated) {
                try {
                    // open buffered writer and write header line
                    fw = new FileWriter(mECGFileHandler);
                    mBufferedWriter = new BufferedWriter(fw);
                    mBufferedWriter.write(mHeader);
                    mBufferedWriter.write(String.valueOf(samplingRate));
                    mBufferedWriter.write(mSeparator);
                } catch (Exception e) {
                    Log.e(TAG, "Exception on dir and file create!", e);
                    mECGFileCreated = false;
                }
            }
        }

        /**
         * Adds the received ECG data into the internal {@link BufferedWriter}.
         *
         * @param ecgData An array of incoming ECG data
         */
        public void writeData(SimbleeEcgSensor.SimbleeDataFrame ecgData) {
            if (isWritable()) {
                try {
                    // writes the raw value into the BufferedWriter
                    mBufferedWriter.write(String.valueOf(ecgData.ecgRaw));
                    mBufferedWriter.write(mSeparator);
                } catch (Exception ignored) {
                }
            }
        }

        /**
         * Flushes and closes the internal {@link BufferedWriter}
         */
        public void completeWriter() {
            if (isWritable()) {
                try {
                    // flush and close writer
                    mBufferedWriter.flush();
                    mBufferedWriter.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error on completing writer!");
                }
            }
        }

        private boolean isWritable() {
            return (mStorageWritable && mECGFileCreated && (mBufferedWriter != null));
        }

    }
}
