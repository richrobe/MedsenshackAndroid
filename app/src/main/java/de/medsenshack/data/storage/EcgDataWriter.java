package de.medsenshack.data.storage;

import java.io.IOException;

import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackEcgDataFrame;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class EcgDataWriter extends DataWriter {
    /**
     * Creates a new DataWriter to write the received ECG data to the external storage
     *
     * @param name
     */
    public EcgDataWriter(String name) {
        super(name);
    }

    @Override
    public void doWriteData(SensorDataFrame data) throws IOException {
        if(data instanceof SimbleeMedhackEcgDataFrame) {
            SimbleeMedhackEcgDataFrame ecgData = (SimbleeMedhackEcgDataFrame) data;
            mBufferedWriter.write(String.valueOf(ecgData.getTimestamp()) + mDelimiter + String.valueOf(ecgData.getSensorPacketTimestamp()) + mDelimiter + String.valueOf(ecgData.getEcgSample()));
        }
    }
}
