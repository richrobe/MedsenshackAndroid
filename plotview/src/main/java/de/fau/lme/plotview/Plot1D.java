/**
 * Plot1D.java
 * Copyright (C) 2012 Pattern Recognition Lab, University Erlangen-Nuremberg.
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE 3 - GPLv3 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the license is attached to this source in the file LICENSE.txt.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 *
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.plotview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import junit.framework.Assert;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import de.fau.lme.plotview.PlotView.PlotScrollPolicy;
import de.fau.lme.plotview.PlotView.PlotSurface;

/**
 * "One"-dimensional line plot
 *
 * Default implementation of a Plot for storing or displaying sampled (signal)
 * data using just the amplitude and a timestamp. Be careful about choosing the
 * sampling rate and maxCachedSeconds in regard to the resulting array-sizes.
 *
 * @author Stefan Gradl
 *
 */
public class Plot1D extends Plot {
    protected LongValueList x;

    public PlotAxis xAxis = new PlotAxis();

    /**
     * @param plotTitle
     * @param paint
     * @param style
     * @param maxCache
     */
    public Plot1D(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        super(plotTitle, paint, style, maxCache);

        // allocate arrays
        m_dataLock.lock();
        x = new LongValueList(m_maxCachedEntries, true);
        m_dataLock.unlock();

    }

    public Plot1D(String plotTitle, Paint paint, PlotStyle style, int maxCache, boolean maintainMinMax) {
        super(plotTitle, paint, style, maxCache, maintainMinMax);

        // allocate arrays
        m_dataLock.lock();
        x = new LongValueList(m_maxCachedEntries, maintainMinMax);
        m_dataLock.unlock();

    }

    /**
     * @param xTitle
     * @param xUnit
     * @param xMultiplier
     * @param valueTitle
     * @param valueUnit
     * @param valueMultiplier
     */
    public void setAxis(String xTitle, String xUnit, float xMultiplier, String valueTitle, String valueUnit,
                        float valueMultiplier) {
        super.setAxis(valueTitle, valueUnit, valueMultiplier);

        xAxis.title = xTitle;
        xAxis.unitName = xUnit;
        xAxis.multiplier = xMultiplier;

        xAxis.paint = Plot.generatePlotPaint(2f, 192, 128, 128, 128);

        xAxis.paintText = Plot.generatePlotPaint(1f, 192, 192, 192, 192);
        xAxis.paintText.setTextAlign(Paint.Align.CENTER);
        xAxis.paintText.setStyle(Style.STROKE);
        // xAxis.paintText.setTypeface( Typeface.MONOSPACE );
        // xAxis.paintText.setShadowLayer( 1.0f, 1, 1, Color.LTGRAY );
        // mXAxisTextPaint.setTypeface( Typeface.DEFAULT_BOLD );
        // mXAxisTextPaint.setTextSize( yunit * 0.14f );
        xAxis.paintText.setTextSize(PlotView.AXIS_PADDING * 0.75f);
    }

    /**
     * Fast addValue that skips the lock, marker and plotChange listener. Used
     * for loading or QoS operation. If both arrays don't maintain min/max the
     * method will return in predictable, constant time.
     *
     * @param value
     * @param x
     */
    public void addValueFast(long value, long x) {
        // add x coordinate
        this.x.add(x);

        // add value
        values.add(value);

        // invalidate marker
        inspectValues.add(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see Plot1D#addValueFast(long, long)
     */
    public void addValueFast(float value, long x) {
        // add x coordinate
        this.x.add(x);

        // add value
        values.add(value);

        // invalidate marker
        inspectValues.add(false);
    }

    /**
     * Adds a single new value to this plot using the given x coordinate.
     *
     * @param value
     *            Sample value.
     * @param x
     *            X coordinate
     */
    public void addValue(long value, long x) {
        m_dataLock.lock();

        addValueFast(value, x);

        // make sure any potential marker on the old position is removed
        setMarker(values.head, null);

        m_dataLock.unlock();

        plotChanged();
    }

    /*
     * (non-Javadoc)
     * 
     * @see Plot1D#addValue(long, long)
     */
    public void addValue(float value, long x) {
        m_dataLock.lock();

        addValueFast(value, x);

        // make sure any potential marker on the old position is removed
        setMarker(values.head, null);

        m_dataLock.unlock();

        plotChanged();
    }

    /**
     * Adds a single new sample using the timestamp and sets a new PlotMarker to
     * this entry.
     *
     * @param value
     * @param x
     * @param marker
     */
    public void addValue(long value, long x, PlotMarker marker) {
        m_dataLock.lock();

        // add x coordinate
        this.x.add(x);

        // add value
        values.add(value);

        if (marker != null)
            inspectValues.add(true);
        else
            inspectValues.add(false);

        setMarker(this.x.head, marker);

        m_dataLock.unlock();

        plotChanged();
    }

    public void clear() {
        super.clear();
        x.clear();
    }

    private transient int tIdx = 0, tPixelIdx = 0, tRealIdx = 0;
    private transient float tppValue = 0f, tppValueMax = 0f, tppValueMin = 0f;
    private transient PlotMarker tMarker = null;
    private transient Path tPath = new Path();
    private transient int tppIdxMin, tppIdxMax;

    // private transient long tTimer0, tTimer1, tTimer2, tTimer3, tTimer4,
    // tTimer5;

    /**
     * Hairy Plotter for Plot1D
     *
     * Specialized drawing method for sampled data.
     *
     * In addition to the normal scaling and translation provided by the
     * PlotView's PlotSurface this method also considers a specified time window
     * to draw as many samples as given by mSamplingRate * mTimeWindow.
     */
    protected void draw(Canvas can, PlotSurface surface) {
        // tTimer0 = System.nanoTime();

        getViewport(surface);

        m_dataLock.lock();

        // check if there is anything to draw
        if (m_idxNum < 1 || m_numIdxPerPixel == 0) {
            m_dataLock.unlock();
            return;
        }

        // if this isn't the masterplot, use its scale values
        // TODO: do that only if a flag is set
        if (surface.masterPlot != this) {
            m_yPxScale = surface.masterPlot.m_yPxScale;
            m_yPxTrans = surface.masterPlot.m_yPxTrans;
        }

        // invalidate overlay marker
        m_markerOverlay = null;
        m_markerLast = 0f;
        m_markerInvalid = false;

        // save canvas before operations
        can.save();

        try {
            // tTimer1 = System.nanoTime();

            tPath.reset();

            // reserve the maximal number of possible lines
            // tPath.incReserve( surface.width << 1 );

            // move to first element
            tppValue = (float) ((values.getIndirect(m_idxStart) + m_yPxTrans) * m_yPxScale);
            tPath.moveTo(0, tppValue);

            // tTimer2 = System.nanoTime();

            // ==============================================
            // == LOOP ALL POINTS
            // ====>
            for (tIdx = 0, tPixelIdx = 1; tIdx < m_idxNum; ++tPixelIdx) {
                tppIdxMin = -1;

                // ======================= LOOP ALL PROJECTED POINTS
                // =======================
                for (; tIdx < (tPixelIdx * m_numIdxPerPixel) && tIdx < m_idxNum; ++tIdx) {
                    // we get the real idx to save one lookup for marker
                    // processing later
                    tRealIdx = values.normIdx(m_idxStart + tIdx);

                    // ===== Y translation & Y scaling is done here only!
                    tppValue = (float) ((values.values[tRealIdx] + m_yPxTrans) * m_yPxScale);

                    if (m_numIdxPerPixel > 1) {
                        if (tppIdxMin == -1) {
                            // first value: min = max = value
                            tppValueMin = tppValueMax = tppValue;
                            tppIdxMin = tppIdxMax = tIdx;
                        } else {
                            if (tppValue < tppValueMin) {
                                // new min
                                tppValueMin = tppValue;
                                tppIdxMin = tIdx;
                            } else if (tppValue > tppValueMax) {
                                // new max
                                tppValueMax = tppValue;
                                tppIdxMax = tIdx;
                            }
                        }
                    } else
                        tppIdxMin = tRealIdx;

                    // ==============> MARKERS
                    // check for marker on this index
                    if (inspectValues.values[tRealIdx]) {
                        tMarker = getMarker(tRealIdx);
                        if (tMarker != null)
                            tMarker.onDraw(can, surface, tPixelIdx, tppValue);
                    }
                    // <============= markers
                }
                // =========================================================================
                // projected loop

                // any point at all to draw?
                if (tppIdxMin != -1) {
                    // ==============> projected points
                    if (m_numIdxPerPixel > 1) {
                        // line from first min/max to second min/max
                        if (tppIdxMin <= tppIdxMax) {
                            tPath.lineTo(tPixelIdx, tppValueMin);
                            tPath.lineTo(tPixelIdx, tppValueMax);
                        } else {
                            tPath.lineTo(tPixelIdx, tppValueMax);
                            tPath.lineTo(tPixelIdx, tppValueMin);
                        }
                    }
                    // <=============
                    else
                    // ==============> single point
                    {
                        tPath.lineTo(tPixelIdx, tppValue);
                    }
                    // <=============
                }
            }
            // <====
            // ==============================================

            m_dataLock.unlock();

            // tTimer3 = System.nanoTime();

            // ==============> draw path
            can.drawPath(tPath, m_paint);
            // <=============

            // tTimer4 = System.nanoTime();
        } catch (Exception e) {
            m_dataLock.unlock();
            e.printStackTrace();
        }

        // can.save();
        // can.scale( 1f, -1f );
        // m_paint.setStyle( Paint.Style.STROKE );
        // can.drawText( Integer.toString( surface.drawIdxNum ), 70, (float)
        // m_paint.descent(), m_paint );
        // can.restore();

        // restore original canvas
        can.restore();

        // tTimer5 = System.nanoTime();
        //
        // Log.d( PlotView.TAG,
        // new StringBuilder( 128 ).append( "Timing: " ).append( tTimer1 -
        // tTimer0 ).append( "  " )
        // .append( tTimer2 - tTimer1 ).append( "  " ).append( tTimer3 - tTimer2
        // ).append( "  " )
        // .append( tTimer4 - tTimer3 ).append( "  " ).append( tTimer5 - tTimer4
        // ).toString() );
    }

    @Override
    public void getViewport(PlotSurface surface) {
        m_dataLock.lock();

        // ==============> Process desired Viewport specified by the
        // constructing code
        if (values.num < m_desiredViewportIdxNum || m_desiredViewportIdxNum <= 0)
            m_idxNum = values.num;
        else
            m_idxNum = m_desiredViewportIdxNum;
        // <=============

        // ==============> Process user scale
        m_idxNum = (int) (m_idxNum * surface.xScale);
        // <=============

        // We now have the final number of indices to draw.
        // ==============> Check index bounds
        if (m_idxNum <= 0) {
            m_dataLock.unlock();
            return;
        } else if (m_idxNum > values.num) {
            m_idxNum = values.num;

            // restore last valid xScale
            surface.xScale = (float) m_xIdxScale;

            // make sure no translation is applied anymore, since we already
            // display max values
            surface.xTrans = 0;
        } else {
            // save xScale since it's valid at this point
            m_xIdxScale = surface.xScale;
        }
        // <=============

        // ==============> Pixel Projection
        if (m_idxNum <= surface.width) {
            // no pixel projection, every point can have its own pixel
            m_numIdxPerPixel = 1;

            // make sure the entire width is used if applicable
            if (values.num > surface.width) {
                m_idxNum = surface.width;
                if (values.num != 0)
                    surface.xScale = (float) (surface.width / (double) values.num);
            } else
                m_idxNum = values.num;
        } else {
            // NumPerPixel > 1.0
            m_numIdxPerPixel = (double) m_idxNum / (surface.width + 2);
        }
        // <=============

        // calculate start idx & end idx to draw based on scroll policy and user
        // translation
        if (scrollHow == PlotScrollPolicy.DEFAULT) {
            m_xIdxTrans = (int) (surface.xTrans);
            // ==============> cap xTrans at number of existing points
            if (m_xIdxTrans <= m_idxNum - values.num) {
                m_xIdxTrans = m_idxNum - values.num;
                surface.xTrans = m_xIdxTrans;
            }
            // else if (surface.xIdxTrans >= surface.idxNum)
            // {
            // surface.xIdxTrans = surface.idxNum - 1;
            // }
            // don't scroll into the future
            if (m_xIdxTrans > 0) {
                m_xIdxTrans = 0;
                surface.xTrans = 0;
            }
            // <=============
            m_idxStart = values.normIdx(values.head - m_idxNum + 1 + m_xIdxTrans);

            // only translate end if m_xIdxTrans is negative. Otherwise we would
            // be in the future.
            m_idxEnd = values.normIdx((int) (values.head + (m_xIdxTrans < 0 ? m_xIdxTrans : 0)));

        } else if (scrollHow == PlotScrollPolicy.OVERRUN) {
            m_idxEnd = values.normIdx(m_idxNum - 1);
            m_idxStart = 0;
        }

        // ==============> calculate m_yPxScale
        if (values.rangeMinMax != 0)
            m_yPxScale = (surface.height - PlotView.AXIS_PADDING * 2) / values.rangeMinMax;
        else
            m_yPxScale = 1d;

        // apply user scale
        m_yPxScale *= surface.yScale;
        if (m_yPxScale == 0)
            m_yPxScale = 1f;
        // <=============

        // ==============> calculate m_yPxTrans
        if (surface.yTrans == 0) {
            m_yPxTrans = (float) (-values.minValue + (PlotView.AXIS_PADDING + 2) / m_yPxScale);
        } else
            m_yPxTrans = surface.yTrans;
        // <=============

        m_xAxisMax = x.getIndirect(m_idxEnd);
        m_xAxisMin = x.getIndirect(m_idxStart);

        m_yAxisMax = values.maxValue;
        m_yAxisMin = values.minValue;

        m_dataLock.unlock();
    }

    private transient Time tTime = new Time();

    @Override
    protected String formatAxisText(PlotAxis axis, int pt) {
        if (axis == xAxis) {
            if (m_idxStart + m_numIdxPerPixel * pt >= x.num)
                return "n/a";

            //return String.format( "%d", x.get( (int) (m_idxStart + m_numIdxPerPixel * pt) ) );
            return String.format("%d", x.get((int) (m_idxStart + m_numIdxPerPixel * pt)));
            //tTime.set( x.getIndirect( (int) (m_idxStart + m_numIdxPerPixel * pt) ) );
            //return tTime.format( "%H:%M:%S" );
        } else if (axis == valueAxis && m_yPxScale != 0) {
            if (pt / m_yPxScale > 10)
                return String.format("%.0f", ((m_yAxisMin + (long) (pt / m_yPxScale)) * axis.multiplier));
            else
                return String.format("%.1f", ((m_yAxisMin + (long) (pt / m_yPxScale)) * axis.multiplier));
        }

        return "n/a";
    }

    private transient RectF tRect = new RectF();

    @Override
    protected void drawAxis(Canvas can, PlotSurface surface, boolean drawGrid, boolean drawMap, AxisPaint domainAxisPaint, AxisPaint valueAxisPaint) {
        // ==============> indicator rect where we are on the x-axis right now
        if (drawMap && values.num > 0) {
            // assign borders
            tRect.left = x.tailDistance(m_idxStart) * ((float) surface.viewWidth / values.num);
            tRect.right = (float) (tRect.left + (m_idxNum / (double) x.num) * surface.viewWidth);
            tRect.bottom = surface.viewHeight - surface.plotView.getAxisHeight();
            tRect.top = surface.viewHeight;
            // check bounds
            tRect.sort();
            can.drawRect(tRect, PlotView.s_mapPaint);
        }
        // <=============

        if (!m_hideAxis) {
            drawXAxis(can, surface, xAxis, drawGrid, domainAxisPaint);
            drawYAxis(can, surface, valueAxis, drawGrid, valueAxisPaint);
        }
    }

    /**
     * Save to filename as text m_file. XVALUE VALUE \n
     *
     * If the m_file already exists, the data is appended.
     *
     * It is important to know that the values are streamed from index 0 to
     * PlotValueList.num into the m_file, regardless of the actual head and tail
     * positions! This is done to avoid two lookups for each value since we
     * assume that this method will only be called on occasion. If you want to
     * continuously stream data to the m_file, call this method every time head
     * reaches PlotValueList.EOR.
     *
     * @param con
     * @param filePath
     *            The absolute path of the m_file.
     * @param header
     *            [optional] Header to write at the beginning of the m_file.
     *            (incl. '\n' !)
     */
    public boolean saveToFile(Context con, String filePath, String header) {
        File f = null;

        try {
            // is a valid dir given?
            if (filePath == null) {
                if (m_file.charAt(0) == File.separatorChar)
                    f = new File(m_file);
                else
                    f = new File(con.getExternalFilesDir(null), m_file);
            } else {
                f = new File(filePath);
            }

            FileWriter fw = new FileWriter(f, true);

            // new m_file? append header?
            if (header != null && f.length() < 5) {
                fw.write(header);
            }

            final StringBuilder sb = new StringBuilder(128);

            // write all data values
            for (tIdx = 0; tIdx < values.num; tIdx++) {
                sb.setLength(0);
                sb.append(x.values[tIdx]).append(" ").append(values.values[tIdx]).append(PlotView.NEWLINE);
                fw.write(sb.toString());
            }

            fw.close();

            return true;
        } catch (IOException e) {
            if (f != null)
                Log.w(PlotView.TAG, "Error writing " + f.getAbsolutePath(), e);
            else
                Log.w(PlotView.TAG, "Error writing " + filePath, e);
        }

        return false;
    }

    /**
     * Creates a Plot1D by loading the given text (gzipped) m_file, separating
     * at delimiter and using the given columns.
     *
     * @param filePath
     *            File to load. Must be a textfile but can be gzipped.
     * @param delimiter
     *            Character at which to separate the columns.
     * @param firstColumn
     *            Index of the first column to use for the x values (starting
     *            from 1)
     * @param secondColumn
     *            Index of the second column to use for the Plot values
     *            (starting from 2, must be > firstColumn)
     * @param numHeaderLines
     *            Number of header lines to skip.
     * @param progressListener
     *            (AsyncTask) object listener the progress is updated to
     * @return A Plot1D object ready to use.
     */
    public static Plot1D create(String filePath, char delimiter, int firstColumn, int secondColumn, int numHeaderLines,
                                PlotView.PlotProgressListener progressListener) {
        Plot1D plot = null;

        Assert.assertTrue(firstColumn < secondColumn);

        try {
            File f = new File(filePath);

            long val1;
            float val2;
            int currentColumn = 0;
            int counter = 0;
            ArrayList<Long> vals1;
            ArrayList<Float> vals2;
            BufferedReader reader;
            long size;

            // is gzip m_file?
            if (filePath.endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new BufferedInputStream(
                        new FileInputStream(f)))));
                // estimate the sizes
                size = f.length() >> 1;
            } else {
                reader = new BufferedReader(new FileReader(f));
                // estimate the sizes
                size = f.length() >> 3;
            }

            // no m_file size
            if (size <= 0) {
                return null;
            }

            // preallocate value arrays
            vals1 = new ArrayList<Long>((int) size);
            vals2 = new ArrayList<Float>((int) size);

            // initialize string (line) splitter
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(delimiter);
            String line = null;

            // skip header lines
            while (numHeaderLines > 0) {
                reader.readLine();
                --numHeaderLines;
            }

            if (progressListener != null)
                progressListener.onSetMaxProgress((int) size + 10);

            // ==============> read lines
            while ((line = reader.readLine()) != null) {
                // split
                splitter.setString(line);

                try {
                    // ==============> iterate columns
                    currentColumn = 1;
                    for (String split : splitter) {
                        if (currentColumn == firstColumn) {
                            val1 = Long.parseLong(split);
                            vals1.add(val1);
                        }

                        if (currentColumn == secondColumn) {
                            val2 = Float.valueOf(split);
                            vals2.add(val2);
                            // we have both values, break column iterator
                            break;
                        }

                        ++currentColumn;
                    }
                    // <=============
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                ++counter;

                if (progressListener != null && counter % 256 == 0) {
                    if (progressListener.isCancelled()) {
                        reader.close();
                        return null;
                    }

                    progressListener.onUpdateProgress(counter);
                }
            }
            // <=============

            // Log.d( PlotView.TAG,
            // String.format( "Plot1D.create loaded %d and %d values.",
            // vals1.size(),
            // vals2.size() ) );
            if (progressListener != null)
                progressListener.onUpdateProgress((int) (size + 3));

            // create plot
            plot = new Plot1D(f.getName(), null, PlotStyle.LINE, vals1.size());
            plot.setAxis("t", "s", 1f, "a", "g", 1f);

            plot.x.copy(vals1);

            if (progressListener != null)
                progressListener.onUpdateProgress((int) (size + 7));

            plot.values.copy(vals2);

            if (progressListener != null)
                progressListener.onUpdateProgress((int) (size + 10));

            reader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            return null;
        }

        return plot;
    }

    /**
     * Loads from streamIn.
     *
     * @param con
     * @param streamIn
     */
    public int loadFromFile(Context con, InputStream streamIn) {
        int count = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(streamIn));

        try {
            long rval, rx;
            final long curtime = System.currentTimeMillis();

            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(' ');
            String line = null;

            // read lines
            while ((line = reader.readLine()) != null) {
                // split
                splitter.setString(line);

                if (splitter.hasNext()) {
                    // first value
                    rval = (long) Long.parseLong(splitter.next());
                    if (splitter.hasNext()) {
                        rx = (long) Long.parseLong(splitter.next());
                        addValueFast(rval, rx);
                    } else {
                        // FIXME: debug markers
                        // if (count == 200)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.OVERLAY_BEGIN );
                        // else if (count == 900)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.OVERLAY_END );
                        // else if (count == 1400)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.LINE_VERTICAL );
                        // else if (count == 2400)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.LINE_VERTICAL );
                        // else if (count == 2800)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.LINE_HORIZONTAL );
                        // else if (count == 3200)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.CIRCLE );
                        // else if (count == 3500)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.STAR );
                        // else if (count == 3700)
                        // addValue( rval, System.currentTimeMillis() + 2000 *
                        // count++, PlotMarker.ARROW );
                        // else

                        // just one value in the line
                        addValueFast(rval, curtime + 5 * count++);
                    }
                }
            }

            reader.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }

        return count;
    }

}
