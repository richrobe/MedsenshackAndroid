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
            SimbleeMedhackGyroDataFrame accData = (SimbleeMedhackGyroDataFrame) data;
            mBufferedWriter.write(String.valueOf(accData.getTimestamp())+mDelimiter+String.valueOf(accData.getGyroX())+mDelimiter+String.valueOf(accData.getGyroY())+mDelimiter+String.valueOf(accData.getGyroZ()));
        }
    }
}
