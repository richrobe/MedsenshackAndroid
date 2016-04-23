/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.sensors;

import android.util.Log;

import java.util.EnumSet;

import de.fau.lme.sensorlib.SensorDataProcessor;
import de.fau.lme.sensorlib.dataframe.AccelDataFrame;
import de.fau.lme.sensorlib.dataframe.AnnotatedDataFrame;
import de.fau.lme.sensorlib.dataframe.EcgDataFrame;
import de.fau.lme.sensorlib.dataframe.EmgDataFrame;
import de.fau.lme.sensorlib.dataframe.GyroDataFrame;
import de.fau.lme.sensorlib.dataframe.RespirationDataFrame;
import de.fau.lme.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by gradl on 08.10.2015.
 */
public class SimulatedSensor extends DsSensor {
    SimulatedDataFrame[] simData;
    boolean liveMode;
    private Thread simThread;

    public SimulatedSensor(String deviceName, SensorDataProcessor dataHandler, SimulatedDataFrame[] simulatedData, double samplingRate, boolean liveMode) {
        super(deviceName, "SensorLib::SimulatedSensor::" + deviceName, dataHandler);
        simData = simulatedData;
        setSamplingRate(samplingRate);
        this.liveMode = liveMode;
        sendSensorCreated();
    }

    public static class SimulatedDataFrame extends SensorDataFrame implements AccelDataFrame, AnnotatedDataFrame, EcgDataFrame, EmgDataFrame, GyroDataFrame, RespirationDataFrame {
        protected SimulatedDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public SimulatedDataFrame() {
            super(null, 0);
        }

        ;

        protected void setSensor(DsSensor fromSensor) {
            originatingSensor = fromSensor;
        }

        protected void setTimestamp(double timestamp) {
            this.timestamp = timestamp;
        }

        public double accelX;
        public double accelY;
        public double accelZ;
        public double gyroX;
        public double gyroY;
        public double gyroZ;
        public double ecg;
        public double ecgLA;
        public double ecgRA;
        public double respiration;
        public double emg;
        public char label;

        @Override
        public double getAccelX() {
            return accelX;
        }

        @Override
        public double getAccelY() {
            return accelY;
        }

        @Override
        public double getAccelZ() {
            return accelZ;
        }

        @Override
        public char getAnnotationChar() {
            return label;
        }

        @Override
        public String getAnnotationString() {
            return null;
        }

        @Override
        public Object getAnnotation() {
            return new Character(label);
        }

        @Override
        public double getEcgSample() {
            return ecg;
        }

        @Override
        public double getSecondaryEcgSample() {
            return ecgLA;
        }

        @Override
        public double getEmgSample() {
            return emg;
        }

        @Override
        public double getGyroX() {
            return gyroX;
        }

        @Override
        public double getGyroY() {
            return gyroY;
        }

        @Override
        public double getGyroZ() {
            return gyroZ;
        }

        @Override
        public double getRespirationSample() {
            return respiration;
        }

        @Override
        public double getRespirationRate() {
            return 0;
        }
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.allOf(HardwareSensor.class);
    }

    @Override
    public boolean connect() throws Exception {
        startStreaming();
        return true;
    }

    @Override
    public void disconnect() {
        stopStreaming();
    }

    @Override
    public void startStreaming() {
        simThread = new Thread(
                new Runnable() {
                    public void run() {
                        transmitData();
                    }
                });
        simThread.start();
    }

    @Override
    public void stopStreaming() {
        simThread.interrupt();
    }

    private void transmitData() {
        //init variables for live mode
        double startTime = System.nanoTime();
        double samplingInterval = 1000d / this.getSamplingRate();
        double samplingIntervalNano = 1.0e9d / mSamplingRate;

        sendStartStreaming();

        //loop over samples
        for (int i = 0; i < simData.length; i++) {
            //get current time
            double curTime = System.nanoTime();
            //transmitting state
            //if (m_state == State.TRANSMITTING)
            {
                //send message (data available)
                simData[i].setSensor(this);
                simData[i].setTimestamp(i * samplingInterval);
                mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, simData[i]).sendToTarget();
            }
            //live mode
            if (liveMode) {
                //sleep until next sampling event
                while ((System.nanoTime() - curTime) < samplingIntervalNano) {
                    Thread.yield();
                }
            }
            if (Thread.interrupted()) {
                Log.e("SimulatedSensor", "Thread interrupted!");
                sendStopStreaming();
                return;
            }
        }

        sendStopStreaming();
    }
}
