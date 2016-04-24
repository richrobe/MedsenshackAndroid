package de.fau.lme.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackAccDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackEcgDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackGalvDataFrame;

/**
 * Created by Robert on 03.01.16.
 */
public class SimbleeMedhackSensor extends DsSensor {

    private static final String TAG = SimbleeMedhackSensor.class.getSimpleName();

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


    public static final int ACC_SAMPLING_RATE = 10;
    public static final int ECG_SAMPLING_RATE = 250;
    public static final int EDA_SAMPLING_RATE = 10;

    private Context mContext;

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
                }
            }
        }

        private long timeStamp;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getName().equals(mName)) {
                long time = System.currentTimeMillis();
                byte[] values = characteristic.getValue();
                //Log.d(TAG, "values: " + Arrays.toString(values));
                int id = (values[1] & 0b0000000011110000) >>> 4;
                int tmp1 = ((int) values[1]) & 0b0000000000001111;
                int tmp2 = ((int) values[0]) & 0b0000000011111111;
                int timestamp = (tmp1 << 8) | tmp2;

                int[] data = new int[(values.length - 2) / 2];
                String vals = "id: " + id + ", timestamp: " + timestamp;
                vals += ", vals: ";
                int cou = 0;
                for (int i = 2; i < values.length - 1; i += 2) {
                    tmp1 = ((int) values[i + 1]) & 0b000000011111111;
                    tmp2 = ((int) values[i]) & 0b0000000011111111;
                    vals += (((tmp1 << 8) | tmp2) + ", ");
                    data[cou++] = ((tmp1 << 8) | tmp2);
                }

                Log.d(TAG, "bytes: " + Arrays.toString(values));
                Log.d(TAG, vals);
                switch (id) {
                    case 1: {
                        for (int i = 0; i < data.length / 3; i++) {
                            sendNewData(new SimbleeMedhackAccDataFrame(data[i * 3], data[i * 3 + 1], data[i * 3 + 2], time - (data.length / 3 - 1 - i) * (1000 / ACC_SAMPLING_RATE), timestamp));
                        }
                        break;
                    }
                    case 2: {
                        for (int i = 0; i < data.length; i++) {
                            sendNewData(new SimbleeMedhackGalvDataFrame(data[i], time - (data.length - 1 - i) * (1000 / EDA_SAMPLING_RATE), timestamp));
                        }
                        break;
                    }
                    case 3: {
                        for (int i = 0; i < data.length; i++) {
                            sendNewData(new SimbleeMedhackEcgDataFrame(data[i], time - (data.length - 1 - i) * (1000 / ECG_SAMPLING_RATE), timestamp));
                        }
                        break;
                    }
                    default: {
                        Log.e(TAG, "data package with unknown sensor id!");
                    }
                }
            }
        }
    };

    public SimbleeMedhackSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
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
            //mWriter = new BleEcgDataWriter();
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
        //if (mWriter != null) {
        //mWriter.completeWriter();
        //}
        mBluetoothGatt.disconnect();
        sendStopStreaming();
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ECG, HardwareSensor.ACCELEROMETER, HardwareSensor.MAGNETOMETER);
    }
}
