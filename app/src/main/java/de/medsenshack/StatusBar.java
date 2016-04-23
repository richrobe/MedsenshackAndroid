package de.medsenshack;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * Original version by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * A StatusBar
 *
 * @author Robert Richer
 */
public class StatusBar extends LinearLayout {

    /**
     * ECG-BLE device disconnected / no device available
     */
    public static final int STATUS_DISCONNECTED = 0;
    /**
     * Connecting to ECG-BLE device
     */
    public static final int STATUS_CONNECTING = 1;
    /**
     * Connected to ECG-BLE device
     */
    public static final int STATUS_CONNECTED = 2;
    /**
     * Simulator connected
     */
    public static final int STATUS_SIMULATING = 3;

    private int mState;
    private Context mContext;
    private TextView mTextViewStatus;

    public StatusBar(Context context) {
        this(context, null, -1);
    }

    public StatusBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_status_bar, this);

        mContext = context;
        mTextViewStatus = (TextView) findViewById(R.id.tv_status);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.StatusBar);
        if (attributes != null) {
            try {
                setStatus(attributes.getInteger(R.styleable.StatusBar_status, 0));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }

    }

    /**
     * Returns the current connection state.
     *
     * @return The current connection state
     */
    public int getState() {
        return mState;
    }

    /**
     * Sets the current connections state of the Status Bar.
     *
     * @param state The current connection state
     */
    public void setStatus(int state) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_STATUS);
        switch (state) {
            case STATUS_DISCONNECTED:
                setBackgroundColor(getResources().getColor(R.color.status_bar_disconnected));
                mTextViewStatus.setText(R.string.status_bar_disconnected);
                intent.putExtra(Constants.BROADCAST_KEY_STATUS, Constants.RQS_STATUS_DISCONNECTED);
                break;
            case STATUS_CONNECTING:
                setBackgroundColor(getResources().getColor(R.color.status_bar_connecting));
                mTextViewStatus.setText(R.string.status_bar_connecting);
                intent.putExtra(Constants.BROADCAST_KEY_STATUS, Constants.RQS_STATUS_CONNECTING);
                break;
            case STATUS_CONNECTED:
                setBackgroundColor(getResources().getColor(R.color.status_bar_connected));
                mTextViewStatus.setText(R.string.status_bar_connected);
                intent.putExtra(Constants.BROADCAST_KEY_STATUS, Constants.RQS_STATUS_CONNECTED);
                break;
            case STATUS_SIMULATING:
                setBackgroundColor(getResources().getColor(R.color.status_bar_simulating));
                mTextViewStatus.setText(R.string.status_bar_simulating);
                intent.putExtra(Constants.BROADCAST_KEY_STATUS, Constants.RQS_STATUS_SIMULATING);
                break;
        }
        mContext.sendBroadcast(intent);
        mState = state;
    }
}
