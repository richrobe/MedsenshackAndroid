/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.EnumSet;

import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.AccelDataFrame;
import de.fau.lme.sensorlib.dataframe.AmbientDataFrame;
import de.fau.lme.sensorlib.dataframe.GyroDataFrame;
import de.fau.lme.sensorlib.dataframe.MagDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;

public class InternalSensor extends DsSensor implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private Sensor mSensorGyro;
    private Sensor mSensorMag;
    private Sensor mSensorLight;
    private Sensor mSensorPress;
    private Sensor mSensorTemp;


    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalSensorDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame, MagDataFrame, AmbientDataFrame {

        public double ax, ay, az;
        public double gx, gy, gz;
        public double mx, my, mz;
        public double l, p, t;

        public InternalSensorDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getAccelX() {
            return ax;
        }

        @Override
        public double getAccelY() {
            return ay;
        }

        @Override
        public double getAccelZ() {
            return az;
        }

        @Override
        public double getGyroX() {
            return gx;
        }

        @Override
        public double getGyroY() {
            return gy;
        }

        @Override
        public double getGyroZ() {
            return gz;
        }

        @Override
        public double getLight() {
            return l;
        }

        @Override
        public double getPressure() {
            return p;
        }

        @Override
        public double getTemperature() {
            return t;
        }

        @Override
        public double getMagX() {
            return mx;
        }

        @Override
        public double getMagY() {
            return my;
        }

        @Override
        public double getMagZ() {
            return mz;
        }
    }


    /**
     * Constructor for standard internal sensor devices.
     *
     * @param dataHandler method to provide unified data handling
     */
    public InternalSensor(SensorDataProcessor dataHandler) {
        super("Internal", "n/a", dataHandler);
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ACCELEROMETER,
                HardwareSensor.GYROSCOPE,
                HardwareSensor.MAGNETOMETER,
                HardwareSensor.LIGHT,
                HardwareSensor.PRESSURE,
                HardwareSensor.TEMPERATURE);
    }

    @Override
    public boolean connect() throws Exception {

        int samplingRate = (int) (1000000 / (int) getSamplingRate());

        mSensorManager = (SensorManager) super.mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSelectedHwSensors.contains(HardwareSensor.ACCELEROMETER)) {
            mSensorAcc = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mSensorAcc, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.GYROSCOPE)) {
            mSensorGyro = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, mSensorGyro, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.MAGNETOMETER)) {
            mSensorMag = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, mSensorMag, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.LIGHT)) {
            mSensorLight = this.mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener(this, mSensorLight, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.PRESSURE)) {
            mSensorPress = this.mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mSensorManager.registerListener(this, mSensorPress, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.TEMPERATURE)) {
            mSensorTemp = this.mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            mSensorManager.registerListener(this, mSensorTemp, samplingRate);
        }

        return true;
    }

    @Override
    public void disconnect() {
        mSensorManager.unregisterListener(this);
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
    public void onSensorChanged(SensorEvent event) {
        InternalSensorDataFrame frame = new InternalSensorDataFrame(this, System.currentTimeMillis());
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            frame.ax = event.values[0];
            frame.ay = event.values[1];
            frame.az = event.values[2];
            //Log.i("Internal Accelerometer", Double.toString(frame.ax) + "   " + Double.toString(frame.ay) + "   " + Double.toString(frame.az));
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            frame.gx = event.values[0];
            frame.gy = event.values[1];
            frame.gz = event.values[2];
            //Log.i("Internal Gyroscope", Double.toString(frame.gx) + "   " + Double.toString(frame.gy) + "   " + Double.toString(frame.gz));
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            frame.gx = event.values[0];
            frame.gy = event.values[1];
            frame.gz = event.values[2];
            //Log.i("Internal Magnetometer", Double.toString(frame.mx) + "   " + Double.toString(frame.my) + "   " + Double.toString(frame.mz));
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            frame.gx = event.values[0];
            //Log.i("Internal Lightsensor", Double.toString(frame.l));
        }
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            frame.gx = event.values[0];
            //Log.i("Internal Pressuresensor", Double.toString(frame.p));
        }
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            frame.gx = event.values[0];
            //Log.i("Internal Tempsensor", Double.toString(frame.t));
        }
        mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, frame).sendToTarget();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
