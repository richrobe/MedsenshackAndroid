package de.medsenshack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.fau.lme.sensorlib.sensors.BleEcgSensor;
import de.lme.plotview.Plot;

public class SimbleeActivity extends StreamingActivity {

    private static final String TAG = SimbleeActivity.class.getSimpleName();

    private TextView mReceiveTextView;
    private Button mSendButton;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simblee);
        setActivity(this, true);

        createUiElements();

    }

    @Override
    protected void createUiElements() {
        mReceiveTextView = (TextView) findViewById(R.id.tv_receive);
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(this);
        mSendButton.setEnabled(false);
        mEditText = (EditText) findViewById(R.id.edit_text);

        mFABToggle = false;
        mPauseButtonPressed = false;
    }

    @Override
    protected void clearUi() {

    }

    @Override
    public void onSensorConnected() {
        super.onSensorConnected();
        mSendButton.setEnabled(true);
    }

    @Override
    public void onSensorDisconnected() {
        super.onSensorDisconnected();
        mSendButton.setEnabled(false);
    }

    @Override
    public void onSegmentationFinished() {

    }

    @Override
    public void onDataReceived(BleEcgSensor.BleEcgDataFrame data) {

        Log.e(TAG, "data");
        mReceiveTextView.setText("Data: " + data.getEcgSample());
    }

    @Override
    public void onPlotMarker(Plot.PlotMarker marker, int signalId, int index) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_send:
                mService.sendSimblee(mEditText.getText().toString());
                break;
            case R.id.fab_button:
                if (!mFABToggle && !mStreaming) {
                    startDailyHeart();
                }
                animateFAB();
                break;
            case R.id.button_stop:
                onStopButtonClick();
                break;
            case R.id.button_pause:
                onPauseButtonClick();
                break;
        }
    }
}
