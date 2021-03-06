package de.medsenshack.data.storage;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.medsenshack.data.ActivityClass;

/**
 * Created by Jigoku969 on 24.04.2016.
 */
public class AnnotationWriter {


    private static final String TAG = AnnotationWriter.class.getSimpleName();
    protected static final char mSeparator = '\n';
    protected static final char mDelimiter = ',';
    protected String mName;
    protected BufferedWriter mBufferedWriter;
    protected File mECGFileHandler;
    protected boolean mStorageWritable;
    protected boolean mECGFileCreated;

    /**
     * Creates a new DataWriter to write the received ECG data to the external storage
     */
    public AnnotationWriter(String name) {
        mName = name;
        // create file
        createFile();
    }

    private void createFile() {
        String state;
        File root = null;
        File path;
        String currentTimeString;

        // set current time
        currentTimeString = new SimpleDateFormat("dd.MM.yy_HH.mm_", Locale.getDefault()).format(new Date());

        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                root = Environment.getExternalStorageDirectory();
                mStorageWritable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                mStorageWritable = false;
                Log.e(TAG, "SD card only readable!");
                break;
            default:
                // not readable or writable
                mStorageWritable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!mStorageWritable) {
            // try to write on external storage
            root = Environment.getDataDirectory();
            if (root.canWrite()) {
                mStorageWritable = true;
            } else {
                Log.e(TAG, "External storage not readable and writable!");
            }
        }

        if (mStorageWritable) {
            try {
                // create directory
                path = new File(root, "MedHackathonData");
                mECGFileCreated = path.mkdir();
                if (!mECGFileCreated) {
                    mECGFileCreated = path.exists();
                    if (!mECGFileCreated) {
                        Log.e(TAG, "File could not be created!");
                        return;
                    } else {
                        Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                    }
                }
                // create files
                mECGFileHandler = new File(path + "/" + mName + "_" + currentTimeString + ".csv");
                mECGFileCreated = mECGFileHandler.createNewFile();
                if (!mECGFileCreated) {
                    mECGFileCreated = mECGFileHandler.exists();
                    if (!mECGFileCreated) {
                        Log.e(TAG, "File could not be created!");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                mECGFileCreated = false;
            }
        }
    }

    /**
     * Prepares the {@link DataWriter}
     */
    public void prepareWriter() {
        FileWriter fw;
        if (mStorageWritable && mECGFileCreated) {
            try {
                // open buffered writer and write header line
                fw = new FileWriter(mECGFileHandler);
                mBufferedWriter = new BufferedWriter(fw);
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                mECGFileCreated = false;
            }
        }
    }

    /**
     * Adds the received ECG data into the internal {@link BufferedWriter}.
     *
     * @param activity An array of incoming ECG data
     */
    public void writeData(ActivityClass activity) {
        if (isWritable()) {
            try {
                // writes the raw value into the BufferedWriter
                mBufferedWriter.write(String.valueOf(System.currentTimeMillis())+mDelimiter+activity.toString());
                mBufferedWriter.write(mSeparator);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Flushes and closes the internal {@link BufferedWriter}
     */
    public void completeWriter() {
        if (isWritable()) {
            try {
                // flush and close writer
                mBufferedWriter.flush();
                mBufferedWriter.close();
            } catch (Exception e) {
                Log.e(TAG, "Error on completing writer!");
            }
        }
    }

    private boolean isWritable() {
        return (mStorageWritable && mECGFileCreated && (mBufferedWriter != null));
    }

}
