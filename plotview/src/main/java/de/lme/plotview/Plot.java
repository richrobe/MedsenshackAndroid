/**
 * Plot.java
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
 */
package de.lme.plotview;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.format.Time;

import de.lme.plotview.PlotView.PlotScrollPolicy;
import de.lme.plotview.PlotView.PlotSurface;

/**
 * Represents a single plot in the view.
 * <p/>
 * Every plot added to a PlotView has to be derived from this class. This class
 * only contains an Array of the Plots' Values (PlotValueList values). The
 * implementing class has to contain and manage further array(s) where the
 * "coordinates" can be looked up for the value at the same index in
 * "this.values". For example Plot2D realizes a two-dimensional coordinate
 * system: it extends Plot1D, which in turn extends Plot. Plot1D has an Array
 * "x" that maps x coordinates to the values. Plot2D has a further array "y".
 * Now every valid index i in "this.values" represents the value in the
 * two-dimensional space at the coordinates Plot1D.x[i] and Plot2D.y[i].
 * <p/>
 * Currently there are four default Plot-implementations:
 * <p/>
 * - Plot1D - default x-coordinate based plot
 * <p/>
 * - Plot2D - extends Plot1D and implements the additional coordinate y
 * <p/>
 * - Plot3D - extends Plot2D and implements the additional coordinate z
 * <p/>
 * - SamplingPlot - extends Plot1D, x coordinates represent timestamps, contains
 * further convenience methods for handling tasks related to sampled data
 * acquisition with very high sampling rates and managing the viewport. However
 * all basic Plot-management is done through Plot1D.
 * <p/>
 * Every plot value can have a PlotMarker assigned to it that will result in
 * additional drawing on this point depending on the chosen marker.
 *
 * @author Stefan Gradl
 */
public abstract class Plot {
    protected ReentrantLock m_dataLock = new ReentrantLock();

    /**
     * This array represents the actual values. The classes that implement Plot
     * manage the coordinate arrays.
     */
    public FloatValueList values;
    /**
     * List that maps a boolean value to every value in the values list to
     * indicate whether the drawing function has to inspect the mapped value
     * further for e.g. markers at that specific index. If the inspectValues
     * list contains a <code>true</code> a the given index, that value will be
     * inspected.
     */
    public BooleanValueList inspectValues;

    /**
     * Default number of preallocated markers.
     */
    private static final int DEFAULT_NUM_MARKERS = 128;

    /**
     * List of all active markers.
     */
    // TODO: change to hashtable?!
    protected ArrayList<PlotMarker> m_markers = new ArrayList<PlotMarker>(DEFAULT_NUM_MARKERS);

    /**
     * the plot's title
     */
    public String plotTitle = "Untitled";

    public PlotAxis valueAxis = new PlotAxis();

    /**
     * whether the plot is drawn or not.
     */
    public boolean isVisible = true;

    /**
     * the m_paint to drawn with.
     */
    protected Paint m_paint = null;

    /**
     * how the plot entries are drawn/connected. LINE is the default
     */
    public PlotStyle style = PlotStyle.LINE;

    /**
     * flags for various (drawing) behaviors
     */
    public EnumSet<PlotFlag> flags = EnumSet.of(PlotFlag.LINK_ASPECT);

    /**
     * Threshold value for the data fields. Any additional entry added will
     * cause the first one to be dropped. 0 means no threshold.
     */
    protected int m_maxCachedEntries = 0;

    /**
     * Specifies the number of indices that should be drawn. How many are
     * exactly drawn is calculated in the getViewport method.
     */
    protected int m_desiredViewportIdxNum = -1;

    public PlotScrollPolicy scrollHow = PlotScrollPolicy.DEFAULT;

    /**
     * the plotchange listener. Its onPlotChanged callback will be called each
     * time a point or marker is added/removed/changed.
     */
    protected PlotChangedListener m_plotChangeListener = null;

    /**
     * Filename / path assigned to this plot upon creation that can be used to
     * stream the contents to the same m_file
     */
    protected String m_file = null;

    /**
     * Internal structure for overlay drawing
     */
    protected RectF m_markerOverlay = null;
    protected float m_markerLast = 0;
    protected boolean m_markerInvalid = false;

    // ==============================================
    // == Values calculated by Plot's getViewport() and used for drawing.
    // ====>
    /**
     * Values to display on the x/y-axis after trans&scale have been applied.
     */
    protected long m_xAxisMin;
    protected long m_xAxisMax;
    protected float m_yAxisMin;
    protected float m_yAxisMax;

    /**
     * First index to be drawn
     */
    protected int m_idxStart;
    /**
     * Last index to be drawn
     */
    protected int m_idxEnd;
    /**
     * Number of indices to draw (= m_idxEnd - m_idxStart + 1)
     */
    protected int m_idxNum;
    /**
     * Number of indices to draw on every pixel
     */
    protected double m_numIdxPerPixel;

    /**
     * x axis translation & scale in indices
     */
    protected int m_xIdxTrans = 0;
    protected double m_xIdxScale = 1d;
    /**
     * y axis translation & scale in pixels
     */
    protected float m_yPxTrans = 0;
    protected double m_yPxScale = 1d;

    // <====
    // ==============================================

    /**
     * Flags to control plot drawing behavior.
     *
     * @author sistgrad
     */
    public enum PlotFlag {
        /**
         * Keeps x & y scale the same = argmin(x,y)
         */
        LINK_ASPECT
    }

    /**
     * Possible plot drawing styles.
     *
     * @author sistgrad
     */
    public enum PlotStyle {
        /**
         * Each value is represented by a circular point
         */
        POINT, CIRCLE,
        /**
         * Each value is represented by a rectangle of size "value"
         */
        RECT,
        /**
         * Same as RECT but size is fixed and color is determined by "value"
         */
        RECT_VALUE_FILLED,
        /**
         * A line plot...
         */
        LINE,
        /**
         * Each value is connected to the next by two rectangular lines
         */
        STAIR,
        /**
         * Each value is represented by a (thick) vertical bar
         */
        BAR,
        /**
         * Each value is represented by a vertical line and an X at the top
         */
        STEM,
        /**
         * Each value is represented by a cross or X
         */
        CROSS,
        /**
         * Each value is represented by a star
         */
        STAR,
        /**
         * The most recent y-value of the plot is represented by a text
         */
        TEXT
    }

    /**
     * Class to mark entries in the plot. You may extend the class using your
     * own marking style, or use PlotMarkerDefault.
     *
     * @author sistgrad
     */
    public static abstract class PlotMarker {
        /**
         * index this marker is drawn at
         */
        protected int m_index = -1;
        protected Paint m_paint = null;
        protected Plot m_plot = null;

        /**
         * Called every time this PlotMarker is registered or collected for a
         * bundled drawing operation.
         */
        public void onAttach(Plot plot) {
            m_plot = plot;
            if (m_paint == null)
                m_paint = Plot.generatePlotPaint(1f, 164, 128, 128, 128);
        }

        public abstract void onDraw(Canvas can, PlotSurface surface, float x, float y);
    }

    /**
     * Default implementation of the PlotMarker.
     *
     * @author sistgrad
     */
    public static class PlotMarkerDefault extends PlotMarker {
        public enum DefaultMark {
            NONE, POINT, STAR, ARROW,
            /**
             * param defines the radius of the circle
             */
            CIRCLE,
            /**
             * Starts an overlay area. This will always supersede all other
             * markers if multiple markers have to be projected on one pixel.
             */
            OVERLAY_BEGIN,
            /**
             * Ends an overlay area.
             */
            OVERLAY_END, LINE_VERTICAL, LINE_HORIZONTAL
        }

        /**
         * Default mark used
         */
        protected DefaultMark m_mark = DefaultMark.POINT;
        /**
         * Size/location used to draw the DefaultMark.
         */
        protected float m_param = 1f;

        /**
         * Creates a new default PlotMarker.
         *
         * @param mark  One of the DefaultMarks to use as marker.
         * @param paint can be null
         * @param param A param used for this mark. The meaning depends on the
         *              DefaultMark used.
         */
        public PlotMarkerDefault(DefaultMark mark, Paint paint, float param) {
            m_mark = mark;
            m_paint = paint;
            if (m_paint == null)
                m_paint = PlotView.s_markerPaint;
            // m_paint = Plot.generatePlotPaint( 1f, 192, 192, 232, 192 );
            m_param = param;
        }

        /*
         * (non-Javadoc)
         *
         * @see de.lme.plotview.Plot.PlotMarker#onDraw(android.graphics.Canvas,
         * de.lme.plotview.PlotView.PlotSurface, float, float)
         */
        @Override
        public void onDraw(Canvas can, PlotSurface surface, float x, float y) {
            // check if this is a global mark
            if (m_index == -1) {
                x = m_param;
                y = m_param;
            }

            // Log.d( "s", "xy " + x + "  " + y );

            switch (m_mark) {
                case OVERLAY_BEGIN:
                    if (m_plot.m_markerOverlay != null) {
                        // previous overlay wasn't finished, end it here
                        m_plot.m_markerOverlay.top = surface.height;
                        m_plot.m_markerOverlay.bottom = 0;
                        m_plot.m_markerOverlay.right = x;
                        // draw overlay
                        m_plot.m_markerLast = x;
                        can.drawRect(m_plot.m_markerOverlay, m_paint);
                        m_plot.m_markerInvalid = true;
                    }

                    // create new overlay area
                    m_plot.m_markerOverlay = new RectF();
                    m_plot.m_markerOverlay.left = x;
                    m_plot.m_markerOverlay.bottom = y;
                    m_plot.m_markerOverlay.top = m_plot.m_markerOverlay.bottom;

                    break;

                case OVERLAY_END:
                    if (m_plot.m_markerOverlay == null) {
                        m_plot.m_markerOverlay = new RectF();
                        m_plot.m_markerOverlay.left = m_plot.m_markerLast;
                        m_plot.m_markerOverlay.top = surface.height;
                        m_plot.m_markerOverlay.bottom = 0;
                        m_plot.m_markerOverlay.right = x;
                        m_plot.m_markerLast = x;
                        can.drawRect(m_plot.m_markerOverlay, m_paint);
                        m_plot.m_markerOverlay = null;
                    } else {
                        if (y > m_plot.m_markerOverlay.top)
                            m_plot.m_markerOverlay.top = y;
                        else if (y < m_plot.m_markerOverlay.bottom)
                            m_plot.m_markerOverlay.bottom = y;
                        else {
                            m_plot.m_markerOverlay.bottom -= 10;
                        }

                        m_plot.m_markerOverlay.top = surface.height;
                        m_plot.m_markerOverlay.bottom = 0;

                        m_plot.m_markerOverlay.right = x;
                        m_plot.m_markerLast = x;

                        // draw overlay
                        if (m_plot.m_markerInvalid) {
                            can.drawRect(m_plot.m_markerOverlay, PlotView.s_overlayInvPaint);
                            m_plot.m_markerInvalid = false;
                        } else
                            can.drawRect(m_plot.m_markerOverlay, m_paint);

                        // invalidate
                        m_plot.m_markerOverlay = null;
                    }
                    break;

                case LINE_VERTICAL:
                    can.drawLine(x, 0, x, surface.height, m_paint);
                    break;

                case LINE_HORIZONTAL:
                    can.drawLine(0, y, surface.width, y, m_paint);
                    break;

                case CIRCLE:
                    can.drawCircle(x, y, m_param, m_paint);
                    break;

                case ARROW:
                    can.drawText("<=", x, y, PlotView.s_markerTextPaint);
                    break;

                case STAR:
                    can.drawText("*", x, y, PlotView.s_markerTextPaint);
                    break;

                default:
                    break;
            } // switch
        }
    }

    /**
     * Specifies an axis of this Plot.
     *
     * @author sistgrad
     */
    protected static class PlotAxis {
        /**
         * Axis title (e.g. "x", "H(a)", etc)
         */
        public String title = "n/a";
        /**
         * The name of the units used on this axis (e.g. "cm", "dB", etc)
         */
        public String unitName = "n/a";
        /**
         * Multiplier each value is multiplied with before displaying it as unit
         * on the axis
         */
        public float multiplier = 1f;

        /**
         * Axis m_paint
         */
        public Paint paint = null;
        /**
         * Text m_paint
         */
        public Paint paintText = null;

        public PlotAxis() {
            paint = Plot.generatePlotPaint(2f, 192, 128, 128, 128);

            paintText = Plot.generatePlotPaint(1f, 192, 192, 192, 192);
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setStyle(Style.STROKE);
            paintText.setTextSize(PlotView.DEFAULT_TEXT_SIZE);
        }
    }

    /**
     * Listener to listen for changes in the plot data or properties. It has the
     * default behavior implemented.
     *
     * @author sistgrad
     */
    public static class PlotChangedListener {
        private PlotView mAttachedPlotView = null;

        /**
         * @param v the PlotView this Plot & Listener is attached to. If not
         *          null, its requestRedraw() will be called each time the
         *          plot changed.
         */
        PlotChangedListener(PlotView v) {
            mAttachedPlotView = v;
        }

        /**
         * Called when the Plot is attached to a PlotView.
         *
         * @param v
         */
        protected void onAttach(PlotView v) {
            mAttachedPlotView = v;
        }

        /**
         * Called every time there is a change in the Plot.
         */
        public void onPlotChanged() {
            if (mAttachedPlotView != null) {
                mAttachedPlotView.requestRedraw(false);
            }
        }
    }

    /**
     * Default constructor.
     */
    public Plot() {

    }

    /**
     * Constructor.
     *
     * @param plotTitle title for the plot
     * @param m_paint   Paint to use for this plot. Can be null (default m_paint). Use
     *                  PlotView.generatePlotPaint to create various randomized paints
     *                  easily.
     * @param style     type of the plot.
     * @param maxCache  maximum number of entries to store. 0 means no limit. due to
     *                  performance issues it is HIGHLY recommended that you set this
     *                  value! you have to set this to 0 though if you manage the
     *                  arrays yourself!
     */
    public Plot(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        this.plotTitle = plotTitle;

        // make sure we have a valid m_paint
        if (paint != null)
            this.m_paint = paint;
        else
            this.m_paint = Plot.generatePlotPaint();

        this.style = style;
        m_maxCachedEntries = maxCache;

        values = new FloatValueList(m_maxCachedEntries, true);
        inspectValues = new BooleanValueList(m_maxCachedEntries, false);

        Time tt = new Time();
        tt.setToNow();
        m_file = "plot_" + tt.format2445() + "_" + this.plotTitle + ".ssd";
    }

    /**
     * Overloaded constructor to specify maintainMinMax.
     *
     * @param plotTitle
     * @param m_paint
     * @param style
     * @param maxCache
     * @param maintainMinMax
     */
    public Plot(String plotTitle, Paint paint, PlotStyle style, int maxCache, boolean maintainMinMax) {
        this.plotTitle = plotTitle;

        // make sure we have a valid m_paint
        if (paint != null)
            this.m_paint = paint;
        else
            this.m_paint = Plot.generatePlotPaint();

        this.style = style;
        m_maxCachedEntries = maxCache;

        values = new FloatValueList(m_maxCachedEntries, maintainMinMax);
        inspectValues = new BooleanValueList(m_maxCachedEntries, false);

        Time tt = new Time();
        tt.setToNow();
        m_file = "plot_" + tt.format2445() + "_" + this.plotTitle + ".ssd";
    }

    /**
     * @param axisTitle
     * @param unitName
     * @param multiplier
     */
    public void setAxis(String axisTitle, String unitName, float multiplier) {
        valueAxis.title = axisTitle;
        valueAxis.unitName = unitName;
        valueAxis.multiplier = multiplier;

        valueAxis.paint = Plot.generatePlotPaint(2f, 192, 128, 128, 128);

        valueAxis.paintText = Plot.generatePlotPaint(1f, 192, 192, 192, 192);
        valueAxis.paintText.setTextAlign(Paint.Align.LEFT);
        valueAxis.paintText.setStyle(Style.STROKE);
        // valueAxis.paintText.setShadowLayer( 1.0f, 1, 1, Color.LTGRAY );
        // mYAxisTextPaint.setTypeface( Typeface.DEFAULT_BOLD );
        // mYAxisTextPaint.setTextSize( yunit * 0.14f );
        valueAxis.paintText.setTextSize(PlotView.AXIS_PADDING * 0.75f);
    }

    /**
     * Sets the m_paint that will be used to draw the plot (lines).
     *
     * @param p
     */
    public void setPaint(Paint p) {
        if (p != null)
            m_paint = p;
    }

    /**
     * Sets a filename/path that will be used to save this Plot.
     *
     * @param newFile
     */
    public void setFile(String newFile) {
        if (newFile != null)
            this.m_file = newFile;
    }

    /**
     * Generates a randomized (plot) Paint. TODO: random? or specific? what's
     * better?
     *
     * @return a new random Paint
     */
    public static Paint generatePlotPaint() {
        Paint p = new Paint();

        Random rand = new Random();

        p.setARGB(254, 24 + rand.nextInt(230), 24 + rand.nextInt(230), 24 + rand.nextInt(230));
        p.setAntiAlias(true);
        p.setStrokeWidth(1.0f);
        p.setTextAlign(Align.CENTER);
        p.setStyle(Paint.Style.STROKE);

        return p;
    }

    /**
     * Convenience method for generating plot paints.
     *
     * @param width Width of the strokes (default: 1f)
     * @param a     Alpha value
     * @param r     Red
     * @param g     Green
     * @param b     Blue
     * @return A new Paint
     */
    public static Paint generatePlotPaint(float width, int a, int r, int g, int b) {
        Paint p = new Paint();
        p.setARGB(a, r, g, b);
        p.setAntiAlias(true);
        p.setStrokeWidth(width);
        p.setStyle(Paint.Style.STROKE);
        p.setTextAlign(Align.CENTER);

        return p;
    }

    /**
     * Convenience method for generating plot paints.
     *
     * @param style paint style
     * @param a     Alpha value
     * @param r     Red
     * @param g     Green
     * @param b     Blue
     * @return A new Paint
     */
    public static Paint generatePlotPaint(Style style, int a, int r, int g, int b) {
        Paint p = new Paint();
        p.setARGB(a, r, g, b);
        // p.setAntiAlias( true );
        // p.setStrokeWidth( width );
        p.setStyle(style);
        // p.setTextAlign( Align.CENTER );

        return p;
    }

    /**
     * Internal wrapper method to call the plotChangedListener.
     */
    protected void plotChanged() {
        if (m_plotChangeListener != null) {
            m_plotChangeListener.onPlotChanged();
        }
    }

    /**
     * Sets a marker for the current head value. @see setMarker(int, PlotMarker)
     *
     * @param marker
     */
    public void setMarker(PlotMarker marker) {
        setMarker(getValueHead(), marker);
    }

    /**
     * Sets a PlotMarker to the given entry. The entryIdx can be interpreted
     * differently, depending on the derived implementation.
     *
     * @param entryIdx If -1 the Marker is always drawn, independently of any actual
     *                 points in this Plot.
     * @param marker   The PlotMarker used.
     */
    public void setMarker(int entryIdx, PlotMarker marker) {
        m_dataLock.lock();
        if (marker == null) {
            // delete marker
            tNum = m_markers.size();
            for (tIdx = 0; tIdx < tNum; ++tIdx) {
                tMark = m_markers.get(tIdx);
                if (tMark.m_index == entryIdx) {
                    m_markers.remove(tIdx);
                    break;
                }
            }
            inspectValues.set(entryIdx, false);
        } else {
            inspectValues.set(entryIdx, true);
            marker.m_index = entryIdx;
            marker.m_plot = this;
            m_markers.add(marker);
        }
        m_dataLock.unlock();
    }

    private transient int tNum;
    private transient PlotMarker tMark = null;
    private transient int tIdx;

    /**
     * Returns the PlotMarker for the given entryIdx, if there exists one.
     * Otherwise null is returned.
     *
     * @param entryIdx
     * @return
     */
    public PlotMarker getMarker(int entryIdx) {
        tNum = m_markers.size();
        for (tIdx = 0; tIdx < tNum; ++tIdx) {
            tMark = m_markers.get(tIdx);
            if (tMark.m_index == entryIdx)
                return tMark;
        }

        return null;
    }

    /**
     * Sets the number of value indices to display
     *
     * @param numIdx
     */
    public void setViewport(int numIdx) {
        m_desiredViewportIdxNum = numIdx;
    }

    private transient RectF tRect = new RectF();
    private transient int tIdxAxis;

    /**
     * Draws the given axis as X-Axis.
     *
     * @param can
     * @param surface
     * @param axis
     * @param drawGrid
     */
    protected void drawXAxis(Canvas can, PlotSurface surface, PlotAxis axis, boolean drawGrid) {
        can.drawLine(0, surface.viewHeight - PlotView.AXIS_PADDING, surface.viewWidth, surface.viewHeight
                - PlotView.AXIS_PADDING, axis.paint);

        // center pin text
        axis.paintText.setTextAlign(Align.CENTER);

        // Log.d( PlotView.TAG, "x " + m_xIdxScale );

        // draw 4 pins
        for (tIdxAxis = 1; tIdxAxis < PlotView.AXIS_PIN_COUNT; tIdxAxis++) {
            tRect.left = PlotView.AXIS_PADDING + tIdxAxis * surface.viewWidth / PlotView.AXIS_PIN_COUNT;
            // x axis only needs one x-coord
            tRect.right = tRect.left;

            tRect.bottom = surface.viewHeight - PlotView.AXIS_PADDING + PlotView.AXIS_PIN_LENGTH;
            tRect.top = surface.viewHeight - PlotView.AXIS_PADDING - PlotView.AXIS_PIN_LENGTH;

            // draw grid line?
            if (drawGrid) {
                can.drawLine(tRect.left, tRect.bottom, tRect.right, 0, axis.paint);
            }

            // axis pin
            can.drawLine(tRect.left, tRect.bottom, tRect.right, tRect.top, axis.paint);

            // pin text
            can.drawText(formatAxisText(axis, tIdxAxis * surface.viewWidth / PlotView.AXIS_PIN_COUNT), tRect.left,
                    surface.viewHeight - axis.paintText.descent(), axis.paintText);
        }

        // axis name
        axis.paintText.setTextAlign(Align.RIGHT);
        can.drawText(axis.title, surface.viewWidth - 4, surface.viewHeight - axis.paintText.descent(), axis.paintText);
    }

    /**
     * Draws the given axis as Y-Axis.
     *
     * @param can
     * @param surface
     * @param axis
     * @param drawGrid
     */
    protected void drawYAxis(Canvas can, PlotSurface surface, PlotAxis axis, boolean drawGrid) {
        can.drawLine(PlotView.AXIS_PADDING, surface.viewHeight, PlotView.AXIS_PADDING, 0, axis.paint);

        for (tIdxAxis = 1; tIdxAxis < PlotView.AXIS_PIN_COUNT; tIdxAxis++) {
            tRect.left = PlotView.AXIS_PADDING - PlotView.AXIS_PIN_LENGTH;
            tRect.right = PlotView.AXIS_PADDING + PlotView.AXIS_PIN_LENGTH;

            tRect.bottom = surface.viewHeight - PlotView.AXIS_PADDING - tIdxAxis * surface.viewHeight / PlotView.AXIS_PIN_COUNT;
            tRect.top = tRect.bottom;

            if (drawGrid) {
                can.drawLine(tRect.left, tRect.bottom, surface.viewWidth, tRect.top, axis.paint);
            }

            // y axis PIN
            can.drawLine(tRect.left, tRect.bottom, tRect.right, tRect.top, axis.paint);

            // y axis pin TEXT
            can.drawText(formatAxisText(axis, tIdxAxis * surface.viewHeight / PlotView.AXIS_PIN_COUNT), 1, tRect.bottom,
                    axis.paintText);

        }

        // y axis TITLE
        can.drawText(axis.title, 1, 0 - axis.paintText.ascent(), axis.paintText);
    }

    /**
     * Draws global PlotMarkers, if there are any.
     *
     * @param can
     * @param surface
     */
    public void drawGlobalMarks(Canvas can, PlotSurface surface) {
        m_dataLock.lock();

        tNum = m_markers.size();

        for (tIdx = 0; tIdx < tNum; ++tIdx) {
            tMark = m_markers.get(tIdx);
            if (tMark.m_index == -1) {
                // TODO this might be wrong - better call draw()
                tMark.onDraw(can, surface, 0, 0);
            }
        }

        m_dataLock.unlock();
    }

    /**
     * Calculates certain viewport-related values.
     *
     * @param surface Structure has to be filled with missing values.
     */
    protected abstract void getViewport(PlotSurface surface);

    /**
     * Draws the entire Plot onto Canvas can.
     *
     * @param can
     * @param surface
     */
    protected abstract void draw(Canvas can, PlotSurface surface);

    /**
     * Returns the String to draw to the axis pin at the given screen-point.
     *
     * @param axis The PlotAxis for which the text is requested.
     * @param pt   The point (pixel) on the screen in axis-direction where the
     *             pin will be located.
     * @return A new String containing the text.
     */
    protected abstract String formatAxisText(PlotAxis axis, int pt);

    /**
     * Draws the axis on the Canvas.
     *
     * @param can
     * @param surface
     */
    protected abstract void drawAxis(Canvas can, PlotSurface surface, boolean drawGrid, boolean drawMap);

    /**
     * @return The head position in the values ring.
     */
    public int getValueHead() {
        return values.head;
    }

    public void clear() {
        values.clear();
    }

    /**
     * Saves the contents of this Plot to a m_file.
     *
     * @param con      Context
     * @param filename
     * @return True on success, false on failure
     */
    public abstract boolean saveToFile(Context con, String filePath, String header);

    /**
     * Load m_file
     *
     * @param con
     * @param streamIn
     */
    public abstract int loadFromFile(Context con, InputStream streamIn);
}
