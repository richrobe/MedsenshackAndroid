/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.UUID;

import de.fau.lme.sensorlib.DsSensorManager;
import de.fau.lme.sensorlib.dataframe.AccelDataFrame;
import de.fau.lme.sensorlib.dataframe.EcgDataFrame;
import de.fau.lme.sensorlib.dataframe.HeartRateDataFrame;
import de.fau.lme.sensorlib.dataframe.RespirationDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.SensorDataProcessor;

/**
 * Created by gradl on 29.09.2015.
 */
public class FitnessShirtSensor extends DsSensor {
    //generic UUID for serial port protocol
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final double ADC_BASELINE_IN_V = 1.65;
    private static final double ADC_TO_V_MULTIPLIER = 3.3 / 4095d;

    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private InputStream btInputStream;
    private DataInputStream mBtDataInStream;
    private ConnectedThread commThread;
    private double samplingIntervalMillis = 0;
    private long startStreamingTimestamp = 0;


    public static class FitnessShirtDataFrame extends SensorDataFrame implements EcgDataFrame, AccelDataFrame, RespirationDataFrame, HeartRateDataFrame {
        public double[] ecgSamples = new double[16];
        public double ecgSample;
        public long respiration;
        public long respirationRate;
        public long heartRate;
        public int accX, accY, accZ;

        public FitnessShirtDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        protected void setTimestamp(double timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public double getEcgSample() {
            return ecgSample;
        }

        @Override
        public double getSecondaryEcgSample() {
            return 0;
        }

        @Override
        public double getHeartRate() {
            return heartRate;
        }

        @Override
        public double getInterbeatInterval() {
            return 60d / heartRate;
        }

        @Override
        public double getRespirationSample() {
            return respiration;
        }

        @Override
        public double getRespirationRate() {
            return respirationRate;
        }

        @Override
        public double getAccelX() {
            return accX;
        }

        @Override
        public double getAccelY() {
            return accY;
        }

        @Override
        public double getAccelZ() {
            return accZ;
        }
    }

    @Override
    protected void dispatchNewData(SensorDataFrame data) {
        if (data instanceof FitnessShirtSensor.FitnessShirtDataFrame == false)
            return;

        FitnessShirtSensor.FitnessShirtDataFrame fsdf = (FitnessShirtSensor.FitnessShirtDataFrame) data;

        // each data frame contains 16 consecutive ecg samples, we reuse the same dataframe to send them separately
        for (int i = 0; i < fsdf.ecgSamples.length; i++) {
            FitnessShirtDataFrame fdf = new FitnessShirtDataFrame(this, fsdf.getTimestamp() + i * samplingIntervalMillis);
            fdf.ecgSample = fsdf.ecgSamples[i];
            fdf.respiration = fsdf.respiration;

            super.dispatchNewData(fdf);

            //fsdf.ecgSample = fsdf.ecgSamples[i];
            //fsdf.setTimestamp( fsdf.getTimestamp() + samplingIntervalMillis );
            //mExternalHandler.onNewData( fsdf );
        }
    }

    public FitnessShirtSensor(BluetoothDevice btDevice, SensorDataProcessor dataHandler) {
        super(btDevice.getName(), btDevice.getAddress(), dataHandler);
        setSamplingRate(256);
    }

    public FitnessShirtSensor(String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress, dataHandler, 256);
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ACCELEROMETER,
                HardwareSensor.ECG,
                HardwareSensor.RESPIRATION,
                HardwareSensor.HEART_RATE);
    }

    @Override
    public boolean connect() {
        btDevice = DsSensorManager.findBtDevice(mDeviceAddress);
        if (btDevice == null)
            return false;

        sendSensorCreated();

        // try various possible ways to connect to the FS via BT
        if (connectNormally()) {
            Log.i(this.getClass().getSimpleName(), "Connected normally.");
        } else {
            if (connectInsecurely()) {
                Log.i(this.getClass().getSimpleName(), "Connected insecurely.");
            } else {
                if (!connectUsingReflection())
                    return false;   // Everything failed. Connection is not possible.
            }
        }

        samplingIntervalMillis = 1000d / getSamplingRate();
        sendConnected();
        // immediately try to start streaming after successful connection
        startStreaming();
        return true;
    }

    private boolean connectNormally() {
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            btSocket.connect();
        } catch (IOException e) {
            try {
                UUID uuid = btDevice.getUuids()[0].getUuid();
                btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
            } catch (IOException e1) {
                return false;
            }
        }
        return true;
    }

    private boolean connectInsecurely() {
        try {
            btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
            btSocket.connect();
        } catch (IOException e) {
            try {
                UUID uuid = btDevice.getUuids()[0].getUuid();
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
            } catch (IOException e1) {
                return false;
            }
        }
        return true;
    }

    private boolean connectUsingReflection() {
        Method m = null;
        try {
            m = btDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            btSocket = (BluetoothSocket) m.invoke(btDevice, 1);
            btSocket.connect();
        } catch (NoSuchMethodException e1) {
            return false;
        } catch (IllegalAccessException e1) {
            return false;
        } catch (InvocationTargetException e1) {
            return false;
        } catch (IOException e1) {
            return false;
        }
        return true;
    }

    @Override
    public void disconnect() {
        try {
            stopStreaming();
            btSocket.close();
            sendDisconnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startStreaming() {
        try {
            if (btSocket == null)
                return;

            if (commThread != null) {
                commThread.cancel();
                commThread = null;
            }

            btInputStream = btSocket.getInputStream();
            mBtDataInStream = new DataInputStream(btInputStream);

            startStreamingTimestamp = System.nanoTime();

            sendStartStreaming();
            commThread = new ConnectedThread();
            commThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopStreaming() {
        try {
            if (mBtDataInStream != null)
                mBtDataInStream.close();
            if (btInputStream != null)
                btInputStream.close();
            sendStopStreaming();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean requestSamplingRateChange(double toSamplingRate) {
        // FS works at fixed sampling rate
        return false;
    }

    private class ConnectedThread extends Thread {
        public synchronized void run() {
            byte[] buffer = new byte[46];  // buffer store for the stream

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // read 46 byte data frames
                    //bytes = btInputStream.read( buffer );
                    mBtDataInStream.readFully(buffer, 0, 46);

                    //Log.d( this.getClass().getSimpleName(), "@" + System.currentTimeMillis() + " --> BT read: " + numBytesRead );

                    FitnessShirtDataFrame df = extractDataFrame(buffer);
                    if (df == null)
                        continue;

                    sendNewData(df);

/*
                    // dispatch message with data to UI
                    // each data frame contains 16 consecutive ecg samples, we reuse the same dataframe to send them separately
                    for (int i = 0; i < df.ecgSamples.length; i++)
                    {
                        FitnessShirtDataFrame fdf = new FitnessShirtDataFrame( getThis(), df.getTimestamp() + i * samplingIntervalMillis );
                        fdf.ecgSample = df.ecgSamples[i];
                        fdf.respiration = df.respiration;
                        //fdf.setTimestamp( df.getTimestamp() + samplingIntervalMillis );
                        //mInternalHandler.obtainMessage( MESSAGE_NEW_DATA, df ).sendToTarget();
                        mExternalHandler.onNewData( fdf );
                    }*/

                } catch (IOException e) {
                    Log.i(this.getClass().getSimpleName(), "Datastream read failed, probably BT connection terminated.");
                    //e.printStackTrace();
                    //notifyExternalHandler( "Connection closed by remote sensor." );
                    disconnect();
                    break;
                }
            }
        }

        public void cancel() {
            // TODO: cleanup
        }
    }


    /**
     * @param lowByte
     * @param highByte
     * @return
     */
    private static long mergeLowHigh16BitUnsigned(byte lowByte, byte highByte) {
        return (lowByte & 0xFF) | (highByte & 0xFF) << 8;
    }

    /**
     * @param lowByte
     * @param highByte
     * @return
     */
    private static int mergeLowHigh12BitSigned(byte lowByte, byte highByte) {
        // since the 12 bit "shorts" are actually signed, we need to check for that negative sign bit and fill
        // the 4 high bits in the 16-bit Java short to correctly compensate.
        if ((highByte & 0x8) == 0)
            // positive value
            return (lowByte & 0xFF) | (highByte & 0xFF) << 8;
        // negative 12-bit value, shift fill the highest 4 bits in the 16 bit short
        return (lowByte & 0xFF) | (highByte & 0xF) << 8 | (0xF << 12);
    }

    /**
     * Extracts the data from the dataframe byte buffer.
     *
     * @param buffer
     * @return
     */
    private FitnessShirtDataFrame extractDataFrame(byte[] buffer) {
        if (buffer[0] != -1 || buffer[1] != -1) {
            // invalid data frame
            Log.d(this.getClass().getSimpleName(), "Invalid data frame (" + buffer[0] + " " + buffer[1] + ")");
            return null;
        }

        FitnessShirtDataFrame df = new FitnessShirtDataFrame(this, (System.nanoTime() - startStreamingTimestamp) / 1.0e6);

        // each value is encoded in a low and following high-byte.

        // 16 ECG samples are 16-bit unsigned
        for (int i = 0; i < 16; i++) {
            df.ecgSamples[i] = mergeLowHigh16BitUnsigned(buffer[2 + i * 2], buffer[3 + i * 2]);
            // convert from ADC units to Volt and subtract baseline
            df.ecgSamples[i] = df.ecgSamples[i] * ADC_TO_V_MULTIPLIER - ADC_BASELINE_IN_V;
        }

        // respiration, respRate and heartRate are all 16-bit unsigned
        df.respiration = mergeLowHigh16BitUnsigned(buffer[34], buffer[35]);
        df.respirationRate = mergeLowHigh16BitUnsigned(buffer[36], buffer[37]);
        df.heartRate = mergeLowHigh16BitUnsigned(buffer[38], buffer[39]);

        // acceleration is 12-bit signed
        df.accX = mergeLowHigh12BitSigned(buffer[40], buffer[41]);
        df.accY = mergeLowHigh12BitSigned(buffer[42], buffer[43]);
        df.accZ = mergeLowHigh12BitSigned(buffer[44], buffer[45]);

        return df;
    }
}
