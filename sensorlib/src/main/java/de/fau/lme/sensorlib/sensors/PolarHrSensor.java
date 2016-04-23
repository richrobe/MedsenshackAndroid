package de.fau.lme.sensorlib.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.SimpleDataFrame;

/**
 * Created by gradl on 07.12.2015.
 * <p/>
 * http://developer.polar.com/wiki/H6_and_H7_Heart_rate_sensors
 * https://code.google.com/p/mytracks/source/browse/MyTracks/src/com/google/android/apps/mytracks/services/sensors/PolarMessageParser.java
 */
public class PolarHrSensor extends DsSensor {
    private static final String TAG = PolarHrSensor.class.getSimpleName();

    private static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static final UUID UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(HEART_RATE_MEASUREMENT);

    Context mContext;
    BluetoothDevice mBtDevice;
    BluetoothGatt mBluetoothGatt;

    public PolarHrSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress, dataHandler);
        mContext = context;
    }

    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.d(this.getClass().getSimpleName(), "onServicesDiscovered: " + status + "; " + gatt);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // find the HR Service
                BluetoothGattService hrService = null;
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService s : services) {
                    if (s.getUuid().equals(UUID_HEART_RATE_SERVICE)) {
                        hrService = s;
                        break;
                    }
                }

                // find the HR Characteristic
                BluetoothGattCharacteristic hrCharacteristic = null;
                if (hrService != null) {
                    List<BluetoothGattCharacteristic> cs = hrService.getCharacteristics();
                    for (BluetoothGattCharacteristic c : cs) {
                        if (c.getUuid().equals(UUID_HEART_RATE_MEASUREMENT)) {
                            hrCharacteristic = c;
                            break;
                        }
                    }
                }

                // set the notification value
                if (hrCharacteristic != null) {
                    BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID_HEART_RATE_MEASUREMENT, 0, 0);
                    mBluetoothGatt.setCharacteristicNotification(hrCharacteristic, true);

                    BluetoothGattDescriptor descriptor = hrCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d(this.getClass().getSimpleName(), "onCharacteristicRead: " + status + "; " + characteristic + "; " + gatt);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "Heart rate format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "Heart rate format UINT8.");
                    }
                    final int heartRate = characteristic.getIntValue(format, 1);
                    Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                    sendNewData(new SimpleDataFrame(null, SystemClock.uptimeMillis(), 0, heartRate));
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(this.getClass().getSimpleName(), "onConnectionStateChange: " + status + "; " + newState + "; " + gatt);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(this.getClass().getSimpleName(), "onDescriptorRead: " + status + "; " + descriptor + "; " + gatt);
        }
    };

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.HEART_RATE);
    }

    @Override
    public boolean connect() throws Exception {
        mBtDevice = DsSensorManager.findBtDevice(mDeviceAddress);
        if (mBtDevice == null)
            return false;

        BluetoothAdapter.getDefaultAdapter().startLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("LeScan", "Device: " + device + "; " + scanRecord);
            }
        });

        mBluetoothGatt = mBtDevice.connectGatt(mContext, false, mGattCallback);

        return false;
    }

    @Override
    public void disconnect() {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public void startStreaming() {

    }

    @Override
    public void stopStreaming() {

    }
}
