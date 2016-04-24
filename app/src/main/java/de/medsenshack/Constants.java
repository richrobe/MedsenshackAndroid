package de.medsenshack;

import java.util.UUID;

/**
 * <p/>
 * Original version by Tim Maiwald, Max Schaldach endowed professorship of Biomedical Engineering.
 * <p/>
 * Modified by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * Interface containing the Constants of this Application
 *
 * @author Tim Maiwald
 * @author Robert Richer
 */
public interface Constants {

    int REQUEST_ENABLE_BT = 34;
    int REQUEST_ACCESS_LOCATION = 56;

    ////// BLUETOOTH CONSTANTS ///////
    // ECG SERVICE
    UUID ECG_SERVICE = UUID.fromString("00002d0d-0000-1000-8000-00805f9b34fb");
    // CHARACTERS1
    UUID ECG_MEASURE_CHAR = UUID.fromString("00002d37-0000-1000-8000-00805f9b34fb");
    // DESCRIPTORS
    UUID GATT_CLIENT_CFG_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // DEVICE NAME
    String SIMBLEE = "SimbleeMed";


    ////// HANDLER MESSAGES //////

    /**
     * Message indicating that the Service is about to be killed
     */
    int MSG_SERVICE_DEAD = 201;
    /**
     * Message indicating a new BLE-ECG device has been found
     */
    int MSG_ECG_DEVICE_FOUND = 202;
    /**
     * Message indicating a successful connection to the sensor
     */
    int MSG_DEVICE_CONNECTED = 204;
    /**
     * Message indicating a connection loss to the sensor
     */
    int MSG_CONNECTION_LOST = 205;
    /**
     * Message indicating the connection to the sensor has ended
     */
    int MSG_CONNECTION_ENDED = 211;
    /**
     * Message indicating that the BLE scan has returned no result
     */
    int MSG_NO_DEVICE_AVAILABLE = 212;
    /**
     * Message updating the progress of the connection attempt to the BLE-ECG Stamp
     */
    int MSG_PROGRESS = 301;
    /**
     * Message to dismiss the progress dialog, either because of a connection loss
     * or a successful connection to the sensor
     */
    int MSG_DISMISS = 302;

    // UI <-> BLE_SERVICE
    /**
     * Message to start scanning for available BLE devices
     */
    int MSG_START_SCAN = 408;

    // PROCESSING MESSAGES
    /**
     * Message containing new processed ECG data
     */
    int MSG_PROCESSED_DATA_AVAILABLE = 605;

    /**
     * Message indicating the current heart beat has been segmented
     */
    int MSG_SEGMENTATION_FINISHED = 606;

    // HEARTY STUFF
    /**
     * Message containing new DailyHeart live data
     */
    int MSG_DAILYHEART_LIVE_DATA = 997;

    // BROADCASTS
    String ACTION_CURRENTHR = "de.lme.dailyheart.ACTION_CURRENTHR";
    String ACTION_ANAYALYZEECG = "de.lme.dailyheart.ACTION_ANAlYZEECG";
    String ACTION_DAILYMONITOR = "de.lme.dailyheart.ACTION_DAILYMONITOR";
    String ACTION_DAILYTRAIN = "de.lme.dailyheart.ACTION_DAILYTRAIN";
    String ACTION_START = "de.lme.dailyheart.ACTION_START";
    String ACTION_PAUSE = "de.lme.dailyheart.ACTION_PAUSE";
    String ACTION_STOP = "de.lme.dailyheart.ACTION_STOP";
    String ACTION_STATUS = "de.lme.dailyheart.ACTION_STATUS";
    String ACTION_SIM = "de.lme.dailyheart.ACTION_SIMULATION";
    String BROADCAST_KEY_SERVICE = "ServiceBroadcastKey";
    String ACTIVITY_BROADCAST_KEY = "ActivityBroadcastKey";
    String BROADCAST_KEY_STATUS = "StatusBroadcastKey";
    int RQS_STOP_SERVICE = 1;
    int RQS_PAUSE_SERVICE = 11;
    int RQS_START_SERVICE = 2;
    int RQS_STATUS_DISCONNECTED = 3;
    int RQS_STATUS_CONNECTED = 4;
    int RQS_STATUS_CONNECTING = 5;
    int RQS_STATUS_SIMULATING = 6;
    int RQS_START_CURRENTHR = 7;
    int RQS_START_ANALYZEECG = 8;
    int RQS_START_DAILYMONITOR = 9;
    int RQS_START_DAILYTRAIN = 10;

    ////// INTENT EXTRAS //////
    String EXTRA_ACTIVITY_NAME = "EXTRA_ACTIVITY_NAME";
    String EXTRA_ACTION_START = "EXTRA_ACTION_START";
    String EXTRA_START_TIME = "EXTRA_START_TIME";
    String EXTRA_CHRONOMETER = "EXTRA_CHRONOMETER";
    String EXTRA_DURATION = "EXTRA_DURATION";
    String EXTRA_HEART_STATUS = "EXTRA_HEART_STATUS";
    String EXTRA_HEART_RATE_CURR = "EXTRA_HEART_RATE_CURR";
    String EXTRA_HEART_RATE_AVG = "EXTRA_HEART_RATE_AVG";
    String EXTRA_HEART_RR = "EXTRA_HEART_RR";
    String EXTRA_HEART_QRSTA = "EXTRA_HEART_QRSTA";
    String EXTRA_HEART_NUM_TOTAL_BEATS = "EXTRA_HEART_NUM_TOTAL_BEATS";
    String EXTRA_HEART_NUM_NORMAL_BEATS = "EXTRA_HEART_NUM_NORMAL_BEATS";
    String EXTRA_HEART_NUM_ABERRANT_BEATS = "EXTRA_HEART_NUM_ABERRANT_BEATS";
    String EXTRA_HEART_NUM_PVC_BEATS = "EXTRA_HEART_NUM_PVC_BEATS";
    String EXTRA_HEART_RATE_MIN = "EXTRA_HEART_RATE_MIN";
    String EXTRA_HEART_RATE_MAX = "EXTRA_HEART_RATE_MAX";
    String EXTRA_ECG_GSON = "EXTRA_ECG_GSON";
    String EXTRA_HR_GSON = "EXTRA_HR_GSON";
    String EXTRA_SAMPLING_RATE = "EXTRA_SAMPLING_RATE";
    String EXTRA_STAND_UP_TIME = "EXTRA_STAND_UP_TIME";
    String EXTRA_HRV_RR_AVG_BEFORE = "EXTRA_HRV_RR_AVG_BEFORE";
    String EXTRA_HRV_RR_AVG_AFTER = "EXTRA_HRV_RR_AVG_AFTER";
    String EXTRA_HRV_NUM_TOTAL_BEATS_BEFORE = "EXTRA_HRV_NUM_TOTAL_BEATS_BEFORE";
    String EXTRA_HRV_NUM_TOTAL_BEATS_AFTER = "EXTRA_HRV_NUM_TOTAL_BEATS_AFTER";
    String EXTRA_HRV_NUM_RR_20_BEFORE = "EXTRA_HRV_NUM_RR_20_BEFORE";
    String EXTRA_HRV_NUM_RR_20_AFTER = "EXTRA_HRV_NUM_RR_20_AFTER";
    String EXTRA_HRV_NUM_RR_50_BEFORE = "EXTRA_HRV_NUM_RR_50_BEFORE";
    String EXTRA_HRV_NUM_RR_50_AFTER = "EXTRA_HRV_NUM_RR_50_AFTER";
    String EXTRA_HRV_STD_RR_BEFORE = "EXTRA_HRV_STD_RR_BEFORE";
    String EXTRA_HRV_STD_RR_AFTER = "EXTRA_HRV_STD_RR_AFTER";
    String EXTRA_HRV_SD_RR_AVG_BEFORE = "EXTRA_HRV_SD_RR_AVG_BEFORE";
    String EXTRA_HRV_SD_RR_AVG_AFTER = "EXTRA_HRV_SD_RR_AVG_AFTER";
    String EXTRA_HRV_SD_RR_STD_BEFORE = "EXTRA_HRV_SD_RR_STD_BEFORE";
    String EXTRA_HRV_SD_RR_STD_AFTER = "EXTRA_HRV_SD_RR_STD_AFTER";
    String EXTRA_HRV_SD_RR_RMS_BEFORE = "EXTRA_HRV_SD_RR_RMS_BEFORE";
    String EXTRA_HRV_SD_RR_RMS_AFTER = "EXTRA_HRV_SD_RR_RMS_AFTER";


    // Android Wear Constants
    String PATH_START_CURRENT_HR = "/wear/start-current-hr";
    String PATH_START_ANALYZE_ECG = "/wear/start-analyze-ecg";
    String PATH_START_DAILY_MONITOR = "/wear/start-daily-monitor";
    String PATH_START_DAILY_TRAIN = "/wear/start-daily-train";
}
