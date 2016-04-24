package de.medsenshack.data.storage;

import java.io.IOException;

import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackAccDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackEcgDataFrame;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class AccDataWriter extends DataWriter {
    /**
     * Creates a new DataWriter to write the received ECG data to the external storage
     *
     * @param name
     */
    public AccDataWriter(String name) {
        super(name);
    }

    @Override
    public void doWriteData(SensorDataFrame data) throws IOException {
        if(data instanceof SimbleeMedhackAccDataFrame) {
            SimbleeMedhackAccDataFrame accData = (SimbleeMedhackAccDataFrame) data;
            mBufferedWriter.write(String.valueOf(accData.getTimestamp()) + mDelimiter + String.valueOf(accData.getSensorPacketTimestamp()) + mDelimiter + String.valueOf(accData.getAccelX()) + mDelimiter + String.valueOf(accData.getAccelY()) + mDelimiter + String.valueOf(accData.getAccelZ()));
        }
    }
}
