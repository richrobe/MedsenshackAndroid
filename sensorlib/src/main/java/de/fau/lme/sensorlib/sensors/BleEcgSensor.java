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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.EcgDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by Robert on 03.01.16.
 */
public class BleEcgSensor extends DsSensor {

    private static final String TAG = BleEcgSensor.class.getSimpleName();

    // ECG SERVICE
    private static final UUID ECG_SERVICE = UUID.fromString("00002d0d-0000-1000-8000-00805f9b34fb");
    // CHARACTERS1
    private static final UUID ECG_MEASURE_CHAR = UUID.fromString("00002d37-0000-1000-8000-00805f9b34fb");
    // DESCRIPTORS
    private static final UUID GATT_CLIENT_CFG_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context mContext;
    private BleEcgDataWriter mWriter;

    private BluetoothGatt mBluetoothGatt;

    /**
     * GATT callback for the communication with the Bluetooth remote device
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();

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
                    sendDisconnected();
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * if there is a failure at any stage, simply disconnect
                 */
                if (name.equals(mName)) {
                    sendDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services discovered: " + status);
            /*
             * With services discovered, we are going to reset our state machine
             * and start working through the sensors we need to enable
             */
            if (gatt.getDevice().getName().equals(mName)) {
                sendNotification("Enabling Sensors...");
                enableEcgSensor(gatt);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Problem writing characteristics!");
            } else {
                Log.d(TAG, "Gatt success!");
            }
            if (gatt.getDevice().getName().equals(mName)) {
                readEcgSensor(gatt, null, characteristic);
            }
        }

        private long timeStamp;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getName().equals(mName)) {
                byte[] values = characteristic.getValue();
                double[] data = new double[values.length / 2];
                int i = 0;
                while (i != values.length) {
                    int tmp1 = ((values[i] & 0xFF) << 6) - 128;
                    int tmp2 = (int) values[i + 1] & 0xFF;
                    int tmp = tmp1 + tmp2;
                    data[i / 2] = tmp;
                    BleEcgDataFrame ecgData = new BleEcgDataFrame(data[i / 2], timeStamp++);
                    sendNewData(ecgData);
                    scheduleWriting(ecgData);
                    i += 2;
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {

            if (gatt.getDevice().getName().equals(mName)) {
                Log.d(TAG, "Descriptor wrote");
                readEcgSensor(gatt, descriptor, null);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }
    };

    public BleEcgSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress, dataHandler);
        mContext = context;
        Log.e(TAG, "KONSTRUKTOR");
        sendSensorCreated();
    }

    @Override
    public boolean connect() throws Exception {
        BluetoothDevice device = DsSensorManager.findBtDevice(mDeviceAddress);
        if (device != null) {
            mBluetoothGatt = device.connectGatt(mContext, true, mGattCallback);
            mWriter = new BleEcgDataWriter();
            sendNotification("Connecting to... " + device.getName());
            return true;
        }
        return false;
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
                HardwareSensor.ECG);
    }

    /**
     * Enables the specific ECG device
     *
     * @param gatt GATT client invoked
     */
    private void enableEcgSensor(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        for (int p = 0; p < services.size(); p++) {
            Log.d("SERVICE" + Integer.toString(p), services.get(p)
                    .getUuid().toString());
            List<BluetoothGattCharacteristic> characs = services.get(p)
                    .getCharacteristics();
            for (int i = 0; i < characs.size(); i++) {
                Log.d("CHARACS" + Integer.toString(i), characs.get(i)
                        .getUuid().toString());
                List<BluetoothGattDescriptor> descri = characs.get(i)
                        .getDescriptors();
                if (!descri.isEmpty()) {
                    for (int n = 0; n < descri.size(); n++) {
                        Log.d("DESCRIPS", descri.get(n).getUuid()
                                .toString());
                    }
                }
                Log.d("*******", "*******************************");
            }
            Log.d("--------", "-----------------------------");
        }

        Log.d(TAG, "Enabling ECG Service");

        BluetoothGattService service = gatt.getService(ECG_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(ECG_MEASURE_CHAR);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GATT_CLIENT_CFG_DESC);
        descriptor.setValue(new byte[]{0x1, 0x0});

        Log.d(TAG, "Trying to write characteristics");
        if (!gatt.writeDescriptor(descriptor)) {
            Log.e(TAG, "Error enabling ECG");
        }
        gatt.setCharacteristicNotification(characteristic, true);

        setState(SensorState.STREAMING);
        sendStartStreaming();
        if (mWriter != null) {
            mWriter.prepareWriter(mSamplingRate);
        }
    }

    /**
     * Reads the characteristics of the specific ECG device
     *
     * @param gatt           GATT client invoked
     * @param descriptor
     * @param characteristic Characteristic that was written to the associated remote device
     */
    private void readEcgSensor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                               BluetoothGattCharacteristic characteristic) {


        if (characteristic != null) {
            gatt.readCharacteristic(characteristic);
            return;
        }

        characteristic = descriptor.getCharacteristic();

        if (!characteristic.getUuid().equals(ECG_MEASURE_CHAR)) {
            gatt.readCharacteristic(characteristic);
        }
    }

    /**
     * Schedules the writing of the device's raw ECG data
     *
     * @param ecgData The raw ECG data from the Bluetooth device
     */
    private void scheduleWriting(final BleEcgDataFrame ecgData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mWriter != null) {
                    mWriter.writeData(ecgData);
                }
            }
        }).start();
    }

    public static class BleEcgDataFrame extends SensorDataFrame implements EcgDataFrame {

        public double ecgRaw;
        public char label;
        public long timeStamp;

        public BleEcgDataFrame(double ecg, long timestamp) {
            super(null, timestamp);
            this.ecgRaw = ecg;
            this.timeStamp = timestamp;
        }

        public BleEcgDataFrame(double ecg, long timestamp, char label) {
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
        public void writeData(BleEcgSensor.BleEcgDataFrame ecgData) {
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
