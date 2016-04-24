package de.medsenshack.data.storage;

import java.io.IOException;

import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackGyroDataFrame;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class GyroDataWriter extends DataWriter {
    /**
     * Creates a new DataWriter to write the received ECG data to the external storage
     *
     * @param name
     */
    public GyroDataWriter(String name) {
        super(name);
    }

    @Override
    public void doWriteData(SensorDataFrame data) throws IOException {
        if(data instanceof SimbleeMedhackGyroDataFrame) {
            SimbleeMedhackGyroDataFrame gyroData = (SimbleeMedhackGyroDataFrame) data;
            mBufferedWriter.write(String.valueOf(gyroData.getTimestamp()) + mDelimiter + String.valueOf(gyroData.getSensorPacketTimestamp()) + mDelimiter + String.valueOf(gyroData.getGyroX()) + mDelimiter + String.valueOf(gyroData.getGyroY()) + mDelimiter + String.valueOf(gyroData.getGyroZ()));
        }
    }
}
