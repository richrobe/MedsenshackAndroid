/**
 * PlotView.java
 * Copyright (C) 2012 Pattern Recognition Lab, University Erlangen-Nuremberg.
 * <p/>
 * Licensed under the GNU GENERAL PUBLIC LICENSE 3 - GPLv3 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the license is attached to this source in the file LICENSE.txt.
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * <p/>
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * <p/>
 * <p/>
 * <p/>
 * PlotView classes Attention: most drawing methods use strictly optimized code to avoid frequent (re)allocations and
 * subsequent repeated garbage collections. Always consider the scope of all transient variables when modifying those
 * methods!!!
 */
package de.lme.plotview;

import java.util.ArrayList;
import java.util.EnumSet;

import junit.framework.Assert;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Shader;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Implementation of a View to draw multiple plots.
 * <p/>
 * Each plot is represented by the class Plot and has to be attached to this
 * view by calling attachPlot.
 * <p/>
 * Each Plot can have a PlotChangedListener whose onPlotChanged method is called
 * each time the Plot changes. The default implementation is used to call the
 * PlotView's requestRedraw method to redraw the View. If you're implementing
 * your own listener you should consider calling through to the superclass or
 * call requestRedraw yourself.
 * <p/>
 * Surviving activity restarts: If you want the PlotView to survive activity
 * restarts (e.g. due to screen orientation change), you either have to manage
 * saving the PlotView itself, or you save each Plot as static attribute and
 * reattach them in the onCreate call.
 *
 * @author Stefan Gradl
 */
public class PlotView extends View {
    /**
     * PlotView TAG for logging
     */
    protected static final String TAG = "PlotView";
    protected static final String NEWLINE = System.getProperty("line.separator");

    /**
     * space between outer border and axis
     */
    protected static final int AXIS_PADDING = 16;
    /**
     * length of "pins" on the axis, indicating value text
     */
    protected static final int AXIS_PIN_LENGTH = 4;
    /**
     * number of "pins" on each axis
     */
    protected static final int AXIS_PIN_COUNT = 5;
    /**
     * default (axis) text size
     */
    protected static final float DEFAULT_TEXT_SIZE = 12f;

    /**
     * All plots attached to this PlotView.
     */
    private ArrayList<Plot> m_plots = new ArrayList<Plot>();

    /**
     * The maximum redraw rate in milliseconds. The view will try not to redraw
     * at a faster rate. Calling invalidate() directly will always force a
     * redraw. However it is recommended to only call invalidate directly if it
     * is vital to UI survival. To smooth UI display you generally just want to
     * suggest a redraw via requestRedraw()! If m_maxRedrawRate <= 0 then the
     * view will redraw as soon as it returns from the previous onDraw() call.
     */
    private long m_maxRedrawRate = 25;

    /**
     * The timestamp at which the last redraw was carried out completely.
     */
    private volatile long m_lastRedrawMillis = -1;

    /**
     * Option-flags
     */
    private EnumSet<Flags> m_plotFlags = Flags.DEFAULT.clone();

    /**
     * default fallback m_paint
     */
    private Paint m_defaultPaint = null;

    /**
     * the default fill-shader
     */
    private Paint m_shader = null;
    protected static Paint s_mapPaint = null;

    /**
     * The m_paint to draw overlay marker regions.
     */
    protected static Paint s_overlayPaint = null;
    protected static Paint s_overlayInvPaint = null;
    protected static Paint s_markerPaint = null;
    protected static Paint s_marker2Paint = null;
    protected static Paint s_markerTextPaint = null;

    /**
     * Gesture and scale detector implementations
     */
    private GestureDetector m_gestureDetector;
    private ScaleGestureDetector m_scaleGeDet;
    private boolean m_isYScale = false;

    /**
     * Draw surface information container
     */
    protected PlotSurface m_surface = null;

    /**
     * The group this PlotView is linked into.
     */
    protected PlotViewGroup m_group = null;

    /**
     * All available general option-flags.
     *
     * @author sistgrad
     */
    public enum Flags {
        /**
         * Draw x/y axes.
         */
        DRAW_AXES,

        /**
         * Draw a grid in the background.
         */
        DRAW_GRID,

        /**
         * Draw orientation guidelines, depending on Plot-type
         */
        DRAW_MAP,

        /**
         * Enable gesture processing.
         */
        ENABLE_GESTURES,

        /**
         * User can't change the yTrans
         */
        DISABLE_Y_USERSCROLL,

        /**
         * The plot-scroll follows new values.
         */
        ENABLE_AUTO_SCROLL,

        /**
         * The plot-zoom automatically adapts to display the entire y-range in
         * the current x-range.
         */
        ENABLE_AUTO_ZOOM_Y,

        /**
         * The plot-zoom automatically adapts to display the entire x-range in
         * the current y-range. Use carefully. In most cases you probably only
         * need ZOOM_Y.
         */
        ENABLE_AUTO_ZOOM_X,

        /**
         * If the user scrolled & zoomed and then doesn't touch it for 30s, the
         * autoscroll/zoom reactivates automatically.
         */
        ENABLE_AUTO_RESET;

        public static final EnumSet<Flags> DEFAULT = EnumSet.of(DRAW_AXES,
                // DRAW_GRID,
                DRAW_MAP, ENABLE_GESTURES, ENABLE_AUTO_SCROLL, ENABLE_AUTO_ZOOM_Y);
    }

    /**
     * Links multiple PlotViews together so scrolling, etc is propagated to
     * every View in the group.
     *
     * @author sistgrad
     */
    public static class PlotViewGroup {
        private ArrayList<PlotView> m_links = new ArrayList<PlotView>(8);
        private boolean m_linkY = true;

        /**
         * Adds the given PlotView to the group.
         *
         * @param view
         */
        public void addView(PlotView view, boolean linkY) {
            m_linkY = linkY;
            if (!m_links.contains(view)) {
                m_links.add(view);
                view.m_group = this;
                // m_linkY.add( new Boolean( linkY ) );
            }
        }

        public void propagateScale(PlotView propagator, float scale, boolean isY) {
            for (PlotView v : m_links) {
                if (isY)
                    v.m_surface.yScale *= scale;
                else
                    v.m_surface.xScale /= scale;
                v.invalidate();
            }
        }

        public void propagateTrans(PlotView propagator, float xTrans, float yTrans) {
            for (PlotView v : m_links) {
                v.m_surface.xTrans = xTrans;

                if (m_linkY)
                    v.m_surface.yTrans = yTrans;

                v.invalidate();
            }
        }
    }

    /**
     * ScaleListener implementation to process scale gestures (zoom).
     *
     * @author sistgrad
     */
    final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float m_lastScaleFactor = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            m_lastScaleFactor = detector.getScaleFactor();

            if (m_lastScaleFactor != 0) {
                if (m_isYScale) {
                    m_surface.yScale *= m_lastScaleFactor;

                    if (m_group != null)
                        m_group.propagateScale(PlotView.this, m_lastScaleFactor, true);
                } else {
                    m_surface.xScale /= m_lastScaleFactor;

                    if (m_surface.xScale > 10000f)
                        m_surface.xScale = 10000f;
                    else if (m_surface.xScale < 0.0001f)
                        m_surface.xScale = 0.0001f;

                    if (m_group != null)
                        m_group.propagateScale(PlotView.this, m_lastScaleFactor, false);
                }

                invalidate();
            }

            return true;
        }
    }

    /**
     * @author sistgrad
     */
    final class ScaleGestDetector extends ScaleGestureDetector {
        private float m_xRange, m_yRange;

        /**
         * @param context
         * @param listener
         */
        public ScaleGestDetector(Context context, OnScaleGestureListener listener) {
            super(context, listener);
            // TODO Auto-generated constructor stub
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.view.ScaleGestureDetector#onTouchEvent(android.view.MotionEvent
         * )
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() > 1) {
                m_xRange = event.getX(event.getPointerId(0)) - event.getX(event.getPointerId(1));
                m_yRange = event.getY(event.getPointerId(0)) - event.getY(event.getPointerId(1));

                if (Math.abs(m_xRange) > Math.abs(m_yRange))
                    m_isYScale = false;
                else
                    m_isYScale = true;
            }
            return super.onTouchEvent(event);
        }

    }

    /**
     * GestureListener implementation to receive and process press and scroll
     * gestures.
     *
     * @author sistgrad
     */
    final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTapEvent(MotionEvent ev) {
            // events during double taps
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(
         * android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            // reset
            m_surface.reset(false);
            invalidate();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // don't accept the fling if it's too short as it may conflict with
            // a button push

            // x fling
            if (Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(e2.getX() - e1.getX()) > 70) {
                m_surface.animateXTrans(m_surface.xTrans, (float) (m_surface.xTrans - velocityX * m_surface.xScrollAmp), 5000);
                invalidate();
            }

            // y fling
            else if (Math.abs(velocityY) > Math.abs(velocityX) && Math.abs(e2.getY() - e1.getY()) > 35) {
                if (e2.getY() > e1.getY()) {
                    // down fling, scale out
                    m_surface.animateXScale(m_surface.xScale, m_surface.xScale * 2, 1500);
                } else {
                    // up fling, scale in
                    m_surface.animateXScale(m_surface.xScale, m_surface.xScale / 2, 1500);
                }

                invalidate();
            }

            // Log.d( TAG, "fling " + " " + velocityX + " " + velocityY );

            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            PlotView.this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            Toast.makeText(getContext(), "Double-tap to reset view. Fling up/down to scale.", Toast.LENGTH_SHORT).show();

            // only show warp dialog if there is a plot in the view
            if (m_surface.masterPlot != null) {
                // build a text input dialog
                final AlertDialog.Builder intBuilder = new AlertDialog.Builder(getContext());
                intBuilder.setTitle("Warp Me");
                intBuilder.setMessage("Enter the index to jump to.");
                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                intBuilder.setView(input);
                input.setHint(Integer.toString(m_surface.masterPlot.values.num + m_surface.masterPlot.m_xIdxTrans));

                intBuilder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int whichButton) {
                        m_surface.xTrans = -m_surface.masterPlot.values.num + Float.parseFloat(input.getText().toString());
                        PlotView.this.invalidate();
                    }
                });
                intBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int whichButton) {
                    }
                });
                intBuilder.show();
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Log.d( TAG, "scroll " + distanceX + " " + distanceY );

            // check if two pointers are present to avoid scrolling & scaling at
            // the same time
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1)
                return false;

            if (m_surface.xFlinger != null && !m_surface.xFlinger.hasEnded()) {
                // stop the fling animation if a scroll is initiated
                m_surface.xFlinger.cancel();
                return true;
            }

            if (m_surface.masterPlot != null)
                m_surface.yTrans = m_surface.masterPlot.m_yPxTrans;

            m_surface.xTrans += distanceX * m_surface.xScrollAmp;
            m_surface.yTrans += distanceY * m_surface.yScrollAmp;

            // Log.d( TAG, "scroll " + m_surface.xTrans + " " + m_surface.yTrans
            // );

            if (m_group != null)
                m_group.propagateTrans(PlotView.this, m_surface.xTrans, m_surface.yTrans);

            invalidate();

            return true;
        }

        @Override
        public void onShowPress(MotionEvent ev) {
            if (m_surface.xFlinger != null && !m_surface.xFlinger.hasEnded()) {
                // stop the fling animation
                m_surface.xFlinger.cancel();
                invalidate();
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            // Log.d( TAG, "single " + ev.getDownTime() + " " +
            // ev.getEventTime() );
            return true;
        }
    }

    /**
     * Progress Listener Interface to use with progress dialogs and e.g.
     * AsyncTask to publish progress from various time-consuming methods to the
     * UI.
     *
     * @author sistgrad
     */
    public interface PlotProgressListener {
        /**
         * Called every time progress is made.
         *
         * @param progress
         */
        public void onUpdateProgress(int progress);

        /**
         * Called at the beginning of a lengthy task with the maximum reachable
         * progress count.
         *
         * @param maxProgress
         */
        public void onSetMaxProgress(int maxProgress);

        /**
         * Should be checked regularly and should always return whether the
         * operation should be cancelled. (e.g. for AsyncTask this should return
         * AsyncTask.isCancelled())
         *
         * @return True if process is cancelled.
         */
        public boolean isCancelled();
    }

    /**
     * Possible x-axis scrolling strategies that can be used. Their exact
     * behavior may vary depending on the Plot implementation.
     *
     * @author sistgrad
     */
    enum PlotScrollPolicy {
        /**
         * Default scrolling. The scrolling smoothly follows new additions to
         * the right.
         */
        DEFAULT,
        /**
         * New values beyond view width overrun the oldest values on the left
         * side.
         */
        OVERRUN,
        /**
         * The first new value beyond view width will drop all visible values
         * and start again on the left side.
         */
        DROP,
        /**
         * Like default scrolling, but only in discrete intervals
         */
        GAP
    }

    /**
     * Holds surface information passed to every Plot's draw() method. Updated
     * on every onDraw() call.
     *
     * @author sistgrad
     */
    protected class PlotSurface {
        // ==============================================
        // == Values set by PlotView or User (input).
        // ====>
        // TODO: it should be enforced that no plot can change any of those
        // values, without having a getter for
        // everything
        /**
         * Width of the View [px]
         */
        public int viewWidth;
        /**
         * Height of the View [px]
         */
        public int viewHeight;

        /**
         * These values represent the actual view size minus axis and padding
         */
        public int width;
        public int height;

        /**
         * User desired viewport translation & scale (gestures)
         */
        public float xTrans = 0;
        public float yTrans = 0;
        public float xScale = 1f;
        public float yScale = 1f;
        /**
         * Scalar all user movement is amplified with
         */
        public double xScrollAmp = 1d;
        public double yScrollAmp = 1d;
        /**
         * Flinger animation
         */
        public TranslateAnimation xFlinger = null;
        public ScaleAnimation xAnimScale = null;

        /**
         * First visible Plot in this PlotView.
         */
        public Plot masterPlot = null;
        // <====
        // ==============================================

        /**
         * Interpolator used for fling animations
         */
        private DecelerateInterpolator m_decInterpolator = new DecelerateInterpolator(2f);
        private LinearInterpolator m_linInterpolator = new LinearInterpolator();

        /**
         * Resets user/view specific values.
         *
         * @param noFancyStuff Don't you dare playing games with me, boy!
         */
        public void reset(boolean noFancyStuff) {
            yTrans = 0;
            yScale = 1f;
            xScrollAmp = 1d;
            yScrollAmp = 1d;

            if (xFlinger != null)
                xFlinger.cancel();

            if (xAnimScale != null)
                xAnimScale.cancel();

            if (noFancyStuff) {
                xTrans = 0;
                xScale = 1f;
                xFlinger = null;
                xAnimScale = null;
            } else {
                animateXTrans(m_surface.xTrans, 0, 2500);
                animateXScale(m_surface.xScale, 1f, 1500);
            }

            PlotView.this.invalidate();
        }

        /**
         * Initializes and starts an x scroll animation.
         *
         * @param fromXDelta
         * @param toXDelta
         * @param duration   Duration of the animation in milliseconds.
         */
        public void animateXTrans(float fromXDelta, float toXDelta, long duration) {
            xFlinger = new TranslateAnimation(fromXDelta, toXDelta, 0, 0);
            xFlinger.initialize(1, 1, 20000, 10);
            xFlinger.setFillEnabled(false);
            xFlinger.setDuration(duration);
            xFlinger.setInterpolator(m_decInterpolator);
            xFlinger.setRepeatCount(0);
            xFlinger.startNow();
        }

        /**
         * Initializes and starts an x scale animation.
         *
         * @param fromX
         * @param toX
         * @param duration Duration of the animation in milliseconds.
         */
        public void animateXScale(float fromX, float toX, long duration) {
            xAnimScale = new ScaleAnimation(fromX, toX, 0, 0);
            xAnimScale.initialize(1000, 1000, 100000, 100000);
            xAnimScale.setFillEnabled(false);
            xAnimScale.setDuration(duration);
            xAnimScale.setInterpolator(m_linInterpolator);
            xAnimScale.setRepeatCount(0);
            xAnimScale.startNow();
        }
    }

    /**
     * @param context
     */
    public PlotView(Context context) {
        super(context);
        initPlotView(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPlotView(context);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public PlotView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPlotView(context);
    }

    /**
     * Initializes the PlotView. This is called by all three constructors.
     *
     * @param context
     */
    private void initPlotView(Context context) {
        m_lastRedrawMillis = -1;

        m_gestureDetector = new GestureDetector(new GestureListener());
        m_scaleGeDet = new ScaleGestDetector(context, new ScaleListener());

        // init surface with maximum possible number of pixels on this device
        // DisplayMetrics metrics = new DisplayMetrics();
        // ((WindowManager) context.getSystemService( Context.WINDOW_SERVICE
        // )).getDefaultDisplay().getMetrics( metrics
        // );
        // m_surface = new PlotSurface( Math.max( metrics.widthPixels,
        // metrics.heightPixels ) );
        m_surface = new PlotSurface();
    }

    /**
     * Adds an options-flag.
     *
     * @param flag
     */
    public void addFlag(Flags flag) {
        m_plotFlags.add(flag);
    }

    /**
     * Adds an additional plot to the view and returns its id.
     *
     * @param plot
     * @return the idx of the plot that was added.
     */
    public int attachPlot(Plot plot) {
        if (plot == null)
            return -1;

        if (plot.m_plotChangeListener == null) {
            // if the caller didn't create an own Listener, we create one with
            // default behavior.
            plot.m_plotChangeListener = new Plot.PlotChangedListener(this);

        } else {
            // call onAttach
            plot.m_plotChangeListener.onAttach(this);
        }

        m_plots.add(plot);
        requestRedraw(false);
        return m_plots.size() - 1;
    }

    /**
     * Adds an additional plot to the view and returns its id.
     *
     * @param plot
     * @param changeListener
     * @return the idx of the plot that was added.
     */
    public int attachPlot(Plot plot, Plot.PlotChangedListener changeListener) {
        if (plot == null)
            return -1;

        if (changeListener != null) {
            plot.m_plotChangeListener = changeListener;
            plot.m_plotChangeListener.onAttach(this);
        } else {
            plot.m_plotChangeListener = null;
        }

        m_plots.add(plot);
        requestRedraw(false);
        return m_plots.size() - 1;
    }

    private transient int t_idx = 0, t_size = 0;

    /**
     * @return the first plot that is visible, or null if none is visible
     */
    public Plot getFirstVisiblePlot() {
        t_size = m_plots.size();
        for (t_idx = 0; t_idx < t_size; t_idx++) {
            if (m_plots.get(t_idx).isVisible)
                return m_plots.get(t_idx);
        }
        return null;
    }

    /**
     * @return the m_maxRedrawRate
     */
    public long getMaxRedrawRate() {
        return m_maxRedrawRate;
    }

    /**
     * Returns the number of plots currently drawn in this view.
     *
     * @return
     */
    public int getNumPlots() {
        return m_plots.size();
    }

    /**
     * Returns the plot with index idx
     *
     * @param idx
     * @return
     */
    public Plot getPlot(int idx) {
        Assert.assertTrue(idx >= 0 && idx < m_plots.size());
        return m_plots.get(idx);
    }

    /**
     * Checks for the specified flag.
     *
     * @param flag
     * @return
     */
    public boolean hasFlag(Flags flag) {
        if (m_plotFlags.contains(flag))
            return true;
        return false;
    }

    /**
     * Should be called when plot changes occurred and a redraw is required. Set
     * usePost = true when calling from outside the UI thread.
     *
     * @param usePost If true then postInvalidate() will be used instead of
     *                invalidate()
     */
    public void requestRedraw(boolean usePost) {
        if (m_maxRedrawRate <= 0) {
            if (usePost)
                postInvalidate();
            else
                invalidate();
        } else {
            Plot firstVisPlot = getFirstVisiblePlot();

            // is any plot visible? is the view enabled?
            if (firstVisPlot != null && this.isEnabled() == true) {
                // check for redraw rate
                if (m_lastRedrawMillis + m_maxRedrawRate <= System.currentTimeMillis()) {
                    if (usePost)
                        postInvalidate();
                    else
                        invalidate();
                }
            }
        }
    }

    /**
     * Makes sure that all available Paint is initialized.
     */
    private void checkPaint() {
        if (m_defaultPaint == null) {
            m_defaultPaint = new Paint();
            m_defaultPaint.setARGB(192, 128, 128, 138);
            m_defaultPaint.setAntiAlias(true);
            m_defaultPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            m_defaultPaint.setStrokeWidth(1.0f);
            // mXAxisTextPaint.setTextAlign( Paint.Align.RIGHT );
            // m_defaultPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
            // m_defaultPaint.setPathEffect( new CornerPathEffect( 10.0f ) );
        }

        // initialize the first time or after size changes
        if (m_shader == null) {
            int[] col = new int[2];
            col[0] = Color.argb(64, 128, 164, 128);
            col[1] = Color.argb(128, 64, 92, 64);
            m_shader = new Paint();
            m_shader.setShader(new LinearGradient(0, 0, 0, getHeight(), col, null, Shader.TileMode.MIRROR));
        }

        if (s_mapPaint == null) {
            // int[] col = new int[ 2 ];
            // col[ 0 ] = Color.argb( 162, 128, 128, 164 );
            // col[ 1 ] = Color.argb( 225, 64, 64, 128 );
            s_mapPaint = new Paint();
            s_mapPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            s_mapPaint.setColor(0x2FB0C4DE);
            // s_mapPaint.setARGB( 64, 204, 153, 102 );
            s_mapPaint.setAntiAlias(true);
        }

        if (s_overlayPaint == null) {
            // int[] col = new int[ 2 ];
            // col[ 0 ] = Color.argb( 162, 128, 128, 164 );
            // col[ 1 ] = Color.argb( 225, 64, 64, 128 );
            s_overlayPaint = new Paint();
            s_overlayPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            s_overlayPaint.setARGB(128, 239, 238, 220);
            // s_overlayPaint.setShader( new LinearGradient( 0, 0, 0,
            // getHeight(), col, null,
            // Shader.TileMode.MIRROR ) );
        }

        if (s_overlayInvPaint == null) {
            // int[] col = new int[ 2 ];
            // col[ 0 ] = Color.argb( 162, 128, 128, 164 );
            // col[ 1 ] = Color.argb( 225, 64, 64, 128 );
            s_overlayInvPaint = new Paint();
            s_overlayInvPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            s_overlayInvPaint.setARGB(128, 128, 128, 128);
            // s_overlayPaint.setShader( new LinearGradient( 0, 0, 0,
            // getHeight(), col, null,
            // Shader.TileMode.MIRROR ) );
        }

        if (s_markerPaint == null) {
            s_markerPaint = new Paint();
            s_markerPaint.setARGB(192, 143, 188, 143);
            s_markerPaint.setAntiAlias(true);
            s_markerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            s_markerPaint.setStrokeWidth(3.0f);
            // mXAxisTextPaint.setTextAlign( Paint.Align.RIGHT );
            // m_defaultPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
            float intervals[] = {8.0f, 2.0f};
            s_markerPaint.setPathEffect(new DashPathEffect(intervals, 0.0f));
        }

        if (s_marker2Paint == null) {
            s_marker2Paint = new Paint();
            s_marker2Paint.setARGB(192, 143, 188, 143);
            s_marker2Paint.setAntiAlias(true);
            s_marker2Paint.setStyle(Paint.Style.STROKE);
            s_marker2Paint.setStrokeWidth(2.0f);
            // mXAxisTextPaint.setTextAlign( Paint.Align.RIGHT );
            // m_defaultPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
            float intervals[] = {8.0f, 2.0f};
            s_marker2Paint.setPathEffect(new DashPathEffect(intervals, 0.0f));
        }

        if (s_markerTextPaint == null) {
            s_markerTextPaint = new Paint();
            s_markerTextPaint.setARGB(192, 143, 188, 143);
            s_markerTextPaint.setAntiAlias(true);
            s_markerTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            s_markerTextPaint.setStrokeWidth(2.0f);
            s_markerTextPaint.setTextSize(32f);
            s_markerTextPaint.setTextAlign(Align.LEFT);
            // m_defaultPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
        }
    }

    /**
     * temporary values to avoid unnecessary GCs
     */
    private transient int t_iter = 0, t_drawSize = 0;

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (this.isEnabled() == false || this.getVisibility() != View.VISIBLE)
            return;

        m_lastRedrawMillis = System.currentTimeMillis();

        // this is called every redraw in case the size of the view changed and
        // we have to update
        // the gradients
        checkPaint();

        m_surface.viewHeight = getHeight();
        m_surface.viewWidth = getWidth();

        // fill plot viewport rect
        m_surface.width = m_surface.viewWidth - AXIS_PADDING - this.getPaddingRight() - this.getPaddingLeft();
        m_surface.height = m_surface.viewHeight - AXIS_PADDING - this.getPaddingTop() - this.getPaddingBottom();

        // get first visible plot / see if we have one at all
        m_surface.masterPlot = getFirstVisiblePlot();
        if (m_surface.masterPlot == null) {
            // there are no plots to draw, or none is visible, we just draw a
            // dummy view
            canvas.drawText("n/a", 30, m_surface.viewHeight - 30, m_defaultPaint);
            canvas.drawLine(-1000f, m_surface.viewHeight - 5f, 1000f, m_surface.viewHeight - 5f, m_defaultPaint);
            canvas.drawLine(5f, -1000f, 5f, 1000f, m_defaultPaint);
            return;
        }

        m_surface.masterPlot.getViewport(m_surface);
        m_surface.xScrollAmp = m_surface.masterPlot.m_numIdxPerPixel;

        if (hasFlag(Flags.DISABLE_Y_USERSCROLL)) {
            m_surface.yTrans = 0f;
            m_surface.yScrollAmp = 1d;
        } else {
            m_surface.yScrollAmp = 1 / m_surface.masterPlot.m_yPxScale;
        }

        // enforce autoscroll TODO: implement 30s timeout
        // if (m_plotFlags.contains( Flags.ENABLE_AUTO_SCROLL ))
        // mDesiredIdx = -1;

        // TODO: draw some kind of plot title color assignment

        // we always draw the axes of the first visible plot
        // X-AXIS
        if (m_plotFlags.contains(Flags.DRAW_AXES)) {
            // draw the title and current scroll position
            canvas.drawText(m_surface.masterPlot.plotTitle, 80, 30, m_defaultPaint);

            // canvas.drawText( String.format(
            // "[#:%d; xT:%d; xS:%.2f; xAmp:%.2f]",
            // m_surface.masterPlot.m_idxNum,
            // m_surface.masterPlot.m_xIdxTrans,
            // m_surface.xScale,
            // m_surface.xScrollAmp ), 120, 30, m_defaultPaint );

            m_surface.masterPlot.drawAxis(canvas, m_surface, m_plotFlags.contains(Flags.DRAW_GRID),
                    m_plotFlags.contains(Flags.DRAW_MAP));
        }

        canvas.save();

        // correct origin from left->right/top->down to left->right/bottom->up
        canvas.translate(0, m_surface.viewHeight);
        canvas.scale(1f, -1f);

        canvas.translate(AXIS_PADDING + this.getPaddingLeft(), AXIS_PADDING + this.getPaddingBottom());

        // draw all visible plots minus masterPlot, which is drawn last on top
        // of the others
        t_drawSize = m_plots.size();
        for (t_iter = 0; t_iter < t_drawSize; t_iter++) {
            if (m_plots.get(t_iter) == m_surface.masterPlot)
                continue;

            if (m_plots.get(t_iter).isVisible) {
                m_plots.get(t_iter).draw(canvas, m_surface);
                m_plots.get(t_iter).drawGlobalMarks(canvas, m_surface);
            }
        }

        // draw masterPlot last and on top
        m_surface.masterPlot.draw(canvas, m_surface);

        // draw masterPlot marks
        m_surface.masterPlot.drawGlobalMarks(canvas, m_surface);

        canvas.restore();

        // ==============> Fling transformation animation handling
        if (m_surface.xFlinger != null && !m_surface.xFlinger.hasEnded()) {
            m_surface.xFlinger.getTransformation(AnimationUtils.currentAnimationTimeMillis(), tAnimTrafo);
            tAnimTrafo.getMatrix().getValues(tTrafoMat);

            // x trans is represented by a13 of the transformation matrix
            m_surface.xTrans = tTrafoMat[2];

            // reinvalidate immediately to continue animation
            invalidate();
        }
        // <=============

        // ==============> Scale animation handling
        if (m_surface.xAnimScale != null && !m_surface.xAnimScale.hasEnded()) {
            m_surface.xAnimScale.getTransformation(AnimationUtils.currentAnimationTimeMillis(), tAnimTrafo);
            tAnimTrafo.getMatrix().getValues(tTrafoMat);

            // x scale is represented by a11 of the transformation matrix
            m_surface.xScale = tTrafoMat[0];
            // Log.d( TAG, " " + tAnimTrafo.toShortString() );

            // reinvalidate immediately to continue animation
            invalidate();
        }
        // <=============

        // Immediately reinvalidate if requested
        if (m_maxRedrawRate <= 0)
            invalidate();
    }

    private transient Transformation tAnimTrafo = new Transformation();
    private transient float tTrafoMat[] = new float[9];

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onSizeChanged(int, int, int, int)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // on sizechange, kill the shader since it depends on coordinates
        m_shader = null;
        s_overlayPaint = null;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (m_gestureDetector.onTouchEvent(event))
            return true;
        else if (m_scaleGeDet.onTouchEvent(event))
            return true;
        else
            return false;
    }

    /**
     * Removes an options-flag.
     *
     * @param flag
     */
    public void removeFlag(Flags flag) {
        m_plotFlags.remove(flag);
        requestRedraw(false);
    }

    /**
     * Removes the given plot.
     *
     * @param idx
     */
    public void removePlot(int idx) {
        if (idx < 0) {
            m_plots.clear();
        } else
            m_plots.remove(idx);
        requestRedraw(false);
    }

    /**
     * @param m_maxRedrawRate the mRedrawFrequency to set
     */
    public void setMaxRedrawRate(long maxRedrawRate) {
        this.m_maxRedrawRate = maxRedrawRate;
        requestRedraw(false);
    }

    /**
     * Helper method for debug output.
     *
     * @param str
     */
    protected static void log(String str) {
        Log.d(TAG, str);
    }
}
