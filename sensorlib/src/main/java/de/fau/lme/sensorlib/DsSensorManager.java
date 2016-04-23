/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.fau.lme.sensorlib.sensors.BleEcgSensor;
import de.fau.lme.sensorlib.sensors.BleEcgSimulatedSensor;
import de.fau.lme.sensorlib.sensors.DsSensor;
import de.fau.lme.sensorlib.sensors.EmpaticaSensor;
import de.fau.lme.sensorlib.sensors.FitnessShirtSensor;
import de.fau.lme.sensorlib.sensors.InternalSensor;
import de.fau.lme.sensorlib.sensors.PolarHrSensor;
import de.fau.lme.sensorlib.sensors.ShimmerSensor;
import de.fau.lme.sensorlib.sensors.SimbleeEcgSensor;
import de.fau.lme.sensorlib.sensors.SmartWatch;

public class DsSensorManager {

    private static final String TAG = DsSensorManager.class.getSimpleName();

    public static Class inferSensorClass(String deviceName) {
        if (deviceName.startsWith("RN42")) {
            // Shimmer 2R sensors
            return ShimmerSensor.class;
        } else if (deviceName.startsWith("Shimmer3")) {
            // Shimmer 3 sensors
            return ShimmerSensor.class;
        } else if (deviceName.startsWith("FSv3.1")) {
            // Fraunhofer fitness shirt
            return FitnessShirtSensor.class;
        } else if (deviceName.startsWith("FSv3.UW")) {
            // Fraunhofer fitness shirt
            return FitnessShirtSensor.class;
        } else if (deviceName.startsWith("Moto")) {
            // Moto 360 SmartWatch
            return SmartWatch.class;
        } else if (deviceName.startsWith("Empatica")) {
            // Empatica device
            return EmpaticaSensor.class;
        } else if (deviceName.startsWith("Polar")) {
            // Polar device
            return PolarHrSensor.class;
        } else if (deviceName.equals("Internal")) {
            return InternalSensor.class;
        } else if (deviceName.contains("POSTAGE")) {
            return BleEcgSensor.class;
        } else if (deviceName.contains("BleEcgSimulator")) {
            return BleEcgSimulatedSensor.class;
        } else if (deviceName.contains("Simblee")) {
            return SimbleeEcgSensor.class;
        }

        return null;
    }


    /**
     * Constructor to create sensors with known Bluetooth device.
     *
     * @param device      known Bluetooth device
     * @param dataHandler method to provide unified data handling
     * @return new sensor type or null
     */
    public static DsSensor createSupportedSensor(BluetoothDevice device, SensorDataProcessor dataHandler, Context context) {

        String deviceName = device.getName();
        DsSensor sensor = null;

        if (deviceName == null)
            return null; // this very likely means that the requested device is not paired.

        Class sensorClass = inferSensorClass(deviceName);
        if (sensorClass == null)
            return null;

        if (sensorClass == ShimmerSensor.class) {
            sensor = new ShimmerSensor(device, dataHandler);
        } else if (sensorClass == FitnessShirtSensor.class) {
            sensor = new FitnessShirtSensor(device, dataHandler);
        } else if (sensorClass == SmartWatch.class) {
            sensor = new SmartWatch(device, dataHandler, context);
        } else if (sensorClass == EmpaticaSensor.class) {
            sensor = new EmpaticaSensor(context, deviceName, device.getAddress(), dataHandler);
        } else if (sensorClass == PolarHrSensor.class) {
            sensor = new PolarHrSensor(context, deviceName, device.getAddress(), dataHandler);
        } else if (sensorClass == BleEcgSensor.class) {
            sensor = new BleEcgSensor(context, deviceName, device.getAddress(), dataHandler);
        } else if (sensorClass == SimbleeEcgSensor.class) {
            sensor = new SimbleeEcgSensor(context, deviceName, device.getAddress(), dataHandler);
        }

        if (sensor != null)
            sensor.setContext(context);

        return sensor;
    }

    /**
     * Constructor to create sensors with known Bluetooth address string.
     *
     * @param deviceAddress known device address string
     * @param dataHandler   method to provide unified data handling
     * @return new sensor type or null
     */
    public static DsSensor createSupportedSensor(String deviceAddress, SensorDataProcessor dataHandler, Context context) {

        if (deviceAddress == null || deviceAddress.equals("n/a")) {
            DsSensor sensor = new InternalSensor(dataHandler);
            sensor.setContext(context);
            return sensor;
        }

        BluetoothDevice dev = findBtDevice(deviceAddress);
        if (dev == null) {
            return null;
        }

        return createSupportedSensor(dev, dataHandler, context);
    }

    public static DsSensor createSimulationSensor(String deviceName, SensorDataProcessor dataHandler, Context context,
                                                  DsSensor.Simulator type, double samplingRate, String fileName) {
        if (deviceName != null) {
            DsSensor sensor = new BleEcgSimulatedSensor(deviceName, dataHandler, samplingRate, fileName, type);
            sensor.setContext(context);
            return sensor;
        }
        return null;
    }

    /**
     * method to list all available and connectable sensor within this framework.
     *
     * @return list of available sensor or null
     */
    public static List<DsSensor> getConnectableSensors(Context context) {
        // Create sensor list
        ArrayList<DsSensor> sensorList = new ArrayList<>();

        // Add internal sensor
        DsSensor sensor = createSupportedSensor("n/a", null, context);
        if (sensor != null) {
            sensorList.add(sensor);
        }

        // Search for Bluetooth sensors
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            return sensorList;
        }
        Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
        // Get paired devices iterator
        // Loop over all paired devices
        for (BluetoothDevice device : pairedDevices) {
            // Get next device
            sensor = createSupportedSensor(device, null, context);
            if (sensor != null) {
                sensorList.add(sensor);
            }
        }


        // Return list of available sensor
        return sensorList;
    }


    /**
     * @param context a context.
     * @param address the address to look for.
     * @return a human readable name (if available) for a given device address or the address string if a name is not available.
     */
    public static String getNameForDeviceAddress(Context context, String address) {
        List<DsSensor> sensors = getConnectableSensors(context);
        for (DsSensor s : sensors) {
            if (s.getDeviceAddress().equals(address))
                return s.getDeviceName();
        }
        return address;
    }

    /**
     * @param context a context.
     * @param name    name of the sensor to look for.
     * @return the address of the first sensor where the given name matches the sensor name exactly. Returns <code>null</code> if no matching sensor was found.
     */
    public static String findFirstMatchingAddressForName(Context context, String name) {
        List<DsSensor> sensors = getConnectableSensors(context);
        for (DsSensor s : sensors) {
            if (s.getDeviceName().equals(name))
                return s.getDeviceAddress();
        }
        return null;
    }


    /**
     * Finds a BluetoothDevice based on an given address.
     *
     * @param deviceAddress the address for the device for which a BluetoothDevice should be returned.
     * @return the found BluetoothDevice, or null on error.
     */
    public static BluetoothDevice findBtDevice(String deviceAddress) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if (ba == null) {
            Log.d(DsSensorManager.class.getSimpleName(), "Failed to get default BT adapter.");
            return null;
        }

        if (!ba.isEnabled()) {
            Log.d(DsSensorManager.class.getSimpleName(), "Can't find device. Bluetooth is not enabled.");
            return null; // TODO: maybe we should throw some sensible exceptions here?
        }

        ba.cancelDiscovery();

        return ba.getRemoteDevice(deviceAddress);
    }

    public static void searchBleDevice(ScanCallback callback) {
        Log.e(TAG, "Searching for BLE device...");
        BluetoothLeScanner bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        bleScanner.startScan(callback);
    }

    public static void cancelBleSearch(ScanCallback callback) {
        BluetoothLeScanner bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        bleScanner.stopScan(callback);
    }
}
