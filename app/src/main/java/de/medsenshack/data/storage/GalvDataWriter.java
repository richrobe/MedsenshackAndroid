package de.medsenshack.data.storage;

import java.io.IOException;

import de.fau.lme.sensorlib.dataframe.SensorDataFrame;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackGalvDataFrame;

/**
 * Created by Jigoku969 on 23.04.2016.
 */
public class GalvDataWriter extends DataWriter{
    /**
     * Creates a new DataWriter to write the received ECG data to the external storage
     *
     * @param name
     */
    public GalvDataWriter(String name) {
        super(name);
    }

    @Override
    public void doWriteData(SensorDataFrame data) throws IOException {
        if(data instanceof SimbleeMedhackGalvDataFrame) {
            SimbleeMedhackGalvDataFrame galvData = (SimbleeMedhackGalvDataFrame) data;
            mBufferedWriter.write(String.valueOf(galvData.getTimestamp())+mDelimiter+String.valueOf(galvData.getGalvanicSkinResponse()));
        }
    }
}
