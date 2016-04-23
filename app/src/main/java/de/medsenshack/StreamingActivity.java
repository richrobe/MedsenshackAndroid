package de.medsenshack;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.transition.Explode;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by Robert on 16.12.15.
 */
public abstract class StreamingActivity extends Activity implements View.OnClickListener, DailyHeartHandler, ServiceConnection {

    //////// UI elements, same for all Activities /////////

    private static String TAG;


    /**
     * Boolean when activity is finishing (hack to prevent double Bluetooth request dialog)
     */
    protected boolean mFinishing = false;

    /**
     * {@link StatusBar} for displaying connection state
     */
    protected StatusBar mStatusBar;
    /**
     * Floating Action Button (FAB) according to new Material Design
     */
    protected ImageButton mFAButton;
    protected Button mPauseButton;
    protected Button mStopButton;
    protected Animation mAnimLeftClose;
    protected Animation mAnimRightClose;
    protected Animation mAnimFABPressed;
    /**
     * {@link ToneGenerator} for playing a heart beat tone after successful heart
     * beat segmentation
     */
    protected ToneGenerator mToneGenerator;

    /**
     * {@link ProgressDialog} for showing the connection status to the ECG-BLE device
     */
    protected ProgressDialog mProgressDialog;
    /**
     * Boolean for the pressed state of the Floating Action Button
     */
    protected boolean mFABToggle;
    /**
     * Boolean for the pressed state of the Paused Button
     */
    protected boolean mPauseButtonPressed;
    protected Handler mRunnableHandler = new Handler();
    /**
     * Boolean indicating if a ECG-BLE device is streaming and if the Pause Button is clicked.
     */
    protected boolean mStreaming;
    /**
     * Boolean indicating if a ECG-BLE device is connected.
     */
    protected boolean mConnected;
    protected long mStartTime;
    protected BleService mService;
    protected Intent mServiceIntent;
    protected IntentFilter mIntentFilter;
    protected ActionBar mActionBar;

    // Broadcast Receiver
    private boolean mIsLiveActivity;
    private Class mClass;
    private Context mContext;

    protected void setActivity(Activity activity, boolean isLive) {
        mClass = activity.getClass();
        mContext = activity;
        mIsLiveActivity = isLive;
        TAG = mClass.getSimpleName();
        initializeComponents();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        getWindow().setExitTransition(new Explode());
        setProgressBarIndeterminate(true);

        mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);


        // initialize IntentFilter for receiving Broadcasts that control the application
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Constants.ACTION_STOP);
        mIntentFilter.addAction(Constants.ACTION_START);
        mIntentFilter.addAction(Constants.ACTION_PAUSE);
        mIntentFilter.addAction(Constants.ACTION_STATUS);

    }

    private void initializeComponents() {

        // initialize ProgressDialog
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);


        // initialize Intent to bind BLEService
        mServiceIntent = new Intent(this, BleService.class);
        mServiceIntent.putExtra(Constants.EXTRA_ACTIVITY_NAME, mClass);

        // Make sure that the phone supports BLE
        if (mIsLiveActivity && !getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported!", Toast.LENGTH_SHORT).show();
            finishAfterTransition();
        }

        mStatusBar = (StatusBar) findViewById(R.id.status_bar);
        mFAButton = (ImageButton) findViewById(R.id.fab_button);
        mFAButton.setOnClickListener(this);
        mPauseButton = (Button) findViewById(R.id.button_pause);
        mPauseButton.setOnClickListener(this);
        mPauseButton.getBackground().setColorFilter(getResources().getColor(R.color.button_pause_color), PorterDuff.Mode.MULTIPLY);
        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.getBackground().setColorFilter(getResources().getColor(R.color.button_stop_color), PorterDuff.Mode.MULTIPLY);
        mStopButton.setOnClickListener(this);

        // load animations for closing Pause and Stop Buttons and animate FAB
        mAnimLeftClose = AnimationUtils.loadAnimation(this, R.anim.view_pause_close);
        mAnimRightClose = AnimationUtils.loadAnimation(this, R.anim.view_stop_close);
        mAnimFABPressed = AnimationUtils.loadAnimation(this, R.anim.fab_pressed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mToneGenerator == null) {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);
        }

        // if Bluetooth is not enabled, open dialog to ask for enabling Bluetooth
        if (mIsLiveActivity && !BluetoothAdapter.getDefaultAdapter().isEnabled() && !mFinishing) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
        if (!checkSelfPermission()) {
            requestPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            switch (resultCode) {
                // if Bluetooth was enabled, no further action
                case RESULT_OK:
                    break;
                // if Bluetooth wasn't enabled, quit Activity
                case RESULT_CANCELED:
                    Toast.makeText(mContext,
                            "Please enable Bluetooth for measuring the heart rate!", Toast.LENGTH_SHORT).show();
                    mFinishing = true;
                    finishAfterTransition();
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mToneGenerator != null) {
            mToneGenerator.release();
            mToneGenerator = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected abstract void createUiElements();

    protected abstract void clearUi();

    protected void onPauseButtonClick() {
        if (!mPauseButton.isEnabled()) {
            return;
        }

        if (!mPauseButtonPressed) {
            // Pause DailyHeart
            mPauseButton.setText(R.string.button_resume);
        } else {
            // Resume DailyHeart
            mPauseButton.setText(R.string.button_pause);
        }
        mPauseButtonPressed = !mPauseButtonPressed;
    }

    protected void onStopButtonClick() {
        if (!mStopButton.isEnabled()) {
            return;
        }
        // start animations
        mStreaming = false;
        mPauseButton.startAnimation(mAnimLeftClose);
        mStopButton.startAnimation(mAnimRightClose);
        mFAButton.performClick();
        mFAButton.setImageResource(R.mipmap.ic_play);
        stopDailyHeart();
    }

    protected void animateFAB() {
        if (!mFABToggle) {
            Animation animLeft = AnimationUtils.loadAnimation(this, R.anim.view_pause_open);
            Animation animRight = AnimationUtils.loadAnimation(this, R.anim.view_stop_open);
            Animation animFAB = AnimationUtils.loadAnimation(this, R.anim.fab_not_pressed);
            mPauseButton.setVisibility(View.VISIBLE);
            mPauseButton.setEnabled(true);
            mStopButton.setVisibility(View.VISIBLE);
            mStopButton.setEnabled(true);
            mPauseButton.startAnimation(animLeft);
            mStopButton.startAnimation(animRight);
            mFAButton.startAnimation(animFAB);
            mFAButton.setImageResource(R.mipmap.ic_stop);
        } else {
            mAnimLeftClose.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPauseButton.setVisibility(View.INVISIBLE);
                    mPauseButton.setEnabled(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mAnimRightClose.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mStopButton.setVisibility(View.INVISIBLE);
                    mStopButton.setEnabled(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            mPauseButton.startAnimation(mAnimLeftClose);
            mStopButton.startAnimation(mAnimRightClose);
            mFAButton.startAnimation(mAnimFABPressed);
        }
        mFABToggle = !mFABToggle;
    }

    @Override
    public void onMessageReceived(Object... message) {

        if (message[0] instanceof String) {
            Toast.makeText(this, (String) message[0], Toast.LENGTH_SHORT).show();
        } else if (message[0] instanceof Integer) {
            switch ((Integer) message[0]) {
                case Constants.MSG_PROGRESS:
                    // Message received from the BLEDeviceManager updating the progress
                    // of the connection attempt to a ECG-BLE device
                    // => Show ProgressDialog and update message
                    Log.d(TAG, "Showing Progress Dialog...");
                    if (message.length > 1 && message[1] instanceof String) {
                        mProgressDialog.setMessage((String) message[1]);
                        if (!mProgressDialog.isShowing()) {
                            mProgressDialog.show();
                        }
                    }
                    break;
                case Constants.MSG_CONNECTION_LOST:
                    mConnected = false;
                    if (mStopButton.isClickable()) {
                        mStopButton.performClick();
                    }
                    mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
                    break;
            }
        }
    }

    @Override
    public void onScanResult(boolean sensorFound) {
        if (sensorFound) {
            mStatusBar.setStatus(StatusBar.STATUS_CONNECTING);
        } else {
            Toast.makeText(this, "No device available...", Toast.LENGTH_SHORT).show();
            mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
            mStopButton.performClick();
        }
    }

    @Override
    public void onSensorConnected() {
        Log.d(TAG, "Sensor connected!");
        mProgressDialog.cancel();
        mConnected = true;
        mStatusBar.setStatus(StatusBar.STATUS_CONNECTED);
    }

    @Override
    public void onSensorDisconnected() {
        Log.d(TAG, "Sensor disconnected!");
        mProgressDialog.cancel();
        mConnected = false;
        if (mStreaming) {
            // There is something wrong with the sensor
            mStopButton.performClick();
        }
        mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
    }

    @Override
    public void onSensorConnectionLost() {
        mFAButton.performClick();
        mStopButton.performClick();
        Toast.makeText(this, "Connection to sensor lost.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStartStreaming() {
        mStartTime = System.currentTimeMillis();
        // let Pause/Stop Buttons disappear
        mFAButton.performClick();
        mStreaming = true;
        mProgressDialog.cancel();
        clearUi();
    }

    @Override
    public void onStopStreaming() {
        mStreaming = false;
        mProgressDialog.cancel();
    }

    protected void startDailyHeart() {
        Log.d(TAG, "startDailyHeart");
        // bind Service
        bindService(mServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    protected void stopDailyHeart() {
        Log.d(TAG, "stopDailyHeart");

        if (mIsLiveActivity) {
            mService.stopBle();
        }

        // unbind Service
        unbindService(this);
        mService = null;
    }

    private boolean checkSelfPermission() {
        boolean coarseGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fineGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return coarseGranted || fineGranted;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                Constants.REQUEST_ACCESS_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_ACCESS_LOCATION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Android versions > 6.0 need location access for scanning for BLE devices!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        // get reference to BLEService instance
        mService = ((BleService.BleServiceBinder) iBinder).getService();
        mService.setDailyHeartHandler(this);
        // start with BLE scan
        if (mIsLiveActivity) {
            //mService.startBLE();
            // TODO: new sensorlib approach
            //mService.startBle();
            mService.startSimblee();
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;
    }

}
