/**
 * Plot2D.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;

import de.fau.lme.plotview.PlotView.PlotSurface;

/**
 * Two-dimensional (plane) plot
 *
 * @author Stefan Gradl
 *
 */
public class Plot2D extends Plot1D {
    protected FloatValueList y;

    public PlotAxis yAxis = new PlotAxis();

    protected long m_xRangeStart = -1;
    protected long m_xRangeEnd = -1;
    protected float m_yRangeStart = -1;
    protected float m_yRangeEnd = -1;

    /**
     * Constructor.
     *
     * @param plotTitle
     *            title for the plot
     * @param m_paint
     *            Paint to use for this plot. Can be null (default m_paint). Use
     *            PlotView.generatePlotPaint to create various randomized paints
     *            easily.
     * @param style
     *            type of the plot.
     * @param maxCache
     *            maximum number of entries to store. 0 means no limit. due to
     *            performance issues it is HIGHLY recommended that you set this
     *            value! you have to set this to 0 though if you manage the
     *            arrays yourself!
     */
    public Plot2D(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        super(plotTitle, paint, style, maxCache);

        if (style == PlotStyle.RECT_VALUE_FILLED)
            paint.setStyle(Style.FILL);
        else if (style == PlotStyle.RECT)
            paint.setStyle(Style.STROKE);

        // allocate arrays
        m_dataLock.lock();
        y = new FloatValueList(m_maxCachedEntries, true);
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
    public void setAxis(String xTitle, String xUnit, float xMultiplier, String yTitle, String yUnit, float yMultiplier) {
        super.setAxis(xTitle, xUnit, xMultiplier, yTitle, yUnit, yMultiplier);

        yAxis.title = yTitle;
        yAxis.unitName = yUnit;
        yAxis.multiplier = yMultiplier;
    }

    /**
     * Sets the desired viewport in the 2D-plane.
     *
     * @param xPivot
     *            Pivot point for xRange. Displayed values will be: [xPivot -
     *            xRange/2; xPivot + xRange/2]
     * @param yPivot
     *            Pivot point for yRange.
     * @param xRange
     *            Range of values to display in x-direction.
     * @param yRange
     *            Range of values to display in y-direction.
     */
    public void setViewport(long xPivot, long yPivot, long xRange, long yRange) {
        this.m_xRangeStart = (long) (xPivot - xRange / 2);
        this.m_xRangeEnd = (long) (xPivot + xRange / 2);

        this.m_yRangeStart = (yPivot - yRange / 2);
        this.m_yRangeEnd = (yPivot + yRange / 2);
    }

    /**
     * Adds a single new value to this plot using the given x and y coordinate.
     *
     * @param value
     *            Sample value.
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     */
    public void addValue(float value, long x, float y) {
        m_dataLock.lock();

        // add x coordinate
        this.x.add(x);

        // add y coordinate
        this.y.add(y);

        // add value
        values.add(value);

        inspectValues.add(false);

        // make sure any potential marker on the old position is invalidated
        setMarker(values.head, null);

        m_dataLock.unlock();

        plotChanged();
    }

    public void clear() {
        super.clear();
        y.clear();
    }

    /**
     * temporary values to avoid unnecessary GCs - BE careful using/changing
     * these values!!!
     */
    private transient float tX, tY, tVal;
    private transient Path tPath;

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
        getViewport(surface);

        m_dataLock.lock();

        // check if there is anything to draw
        if (m_idxNum < 1 || m_numIdxPerPixel == 0) {
            m_dataLock.unlock();
            return;
        }

        // save canvas before operations
        can.save();

        try {
            if (this.style == PlotStyle.LINE) {
                tPath = new Path();
                tPath.incReserve(m_idxNum + 4);
            }

            for (tIdx = 0; tIdx < m_idxNum; tIdx++) {
                // Log.d( PlotView.TAG,
                // "x " + (x.get( tInt ) - m_xRangeStart) * surface.xScale +
                // "  y "
                // + ((float) ( (y.get( tInt ) - m_yRangeStart) *
                // surface.yPxScale)) );

                tX = (float) ((x.getIndirect(tIdx) - m_xRangeStart) * m_xIdxScale);
                tY = (float) ((y.getIndirect(tIdx) - m_yRangeStart) * m_yPxScale);
                tVal = values.getIndirect(tIdx);

                switch (this.style) {
                    case RECT_VALUE_FILLED:
                        m_paint.setColor((int) tVal);
                        can.drawRect(tX - 5, tY + 5, tX + 5, tY - 5, m_paint);
                        break;

                    case RECT:
                        can.drawRect(tX - tVal, tY + tVal, tX + tVal, tY - tVal, m_paint);
                        break;

                    case CROSS:
                        can.save();
                        can.scale(1f, -1f);
                        m_paint.setStrokeWidth(1f);
                        m_paint.setStyle(Paint.Style.STROKE);
                        m_paint.setTextAlign(Align.CENTER);
                        m_paint.setTextSize(10);
                        can.drawText("x", tX, (float) (-tY + m_paint.descent() * m_yPxScale), m_paint);
                        can.restore();
                        break;

                    case LINE:
                        if (tIdx == 0) {
                            tPath.moveTo(tX, tY);
                            if (tVal > 1f)
                                m_paint.setPathEffect(new CornerPathEffect(tVal));
                        }

                        tPath.lineTo(tX, tY);
                        break;

                    case CIRCLE:
                        can.drawCircle(tX, tY, tVal, m_paint);
                        break;

                    case TEXT:
                        can.save();
                        can.scale(1f, -1f);
                        m_paint.setStrokeWidth(1f);
                        m_paint.setStyle(Paint.Style.STROKE);
                        m_paint.setTextAlign(Align.CENTER);
                        m_paint.setTextSize(10);
                        can.drawText(Long.toString((long) tVal), tX, -tY, m_paint);
                        can.restore();
                        break;

                    default:
                    case POINT:
                        can.drawPoint(tX, tY, m_paint);
                        break;
                }
            }

            m_dataLock.unlock();

            if (this.style == PlotStyle.LINE) {
                can.drawPath(tPath, m_paint);
            }
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
    }

    @Override
    public void getViewport(PlotSurface surface) {
        m_dataLock.lock();

        m_idxNum = values.num;

        // ==============> Process desired Viewport specified by the
        // constructing code
        // if (values.num < m_desiredViewportIdxNum || m_desiredViewportIdxNum
        // <= 0)
        // surface.idxNum = values.num;
        // else
        // surface.idxNum = m_desiredViewportIdxNum;
        // <=============

        // ==============> Process user scale
        // surface.idxNum = (int) (surface.idxNum * surface.xScale);
        // <=============

        // We now have the final number of indices to draw.
        // ==============> Check index bounds
        if (m_idxNum <= 0) {
            m_dataLock.unlock();
            return;
        } else if (m_idxNum > values.num) {
            // surface.idxNum = values.num;
            //
            // // restore last valid xScale
            // surface.xScale = (float) surface.xIdxScale;
            //
            // // make sure no translation is applied anymore, since we already
            // display max values
            // surface.xTrans = 0;
        } else {
            // save xScale since it's valid at this point
            m_xIdxScale = surface.xScale;
        }
        // <=============

        // ==============> Pixel Projection
        // if (surface.idxNum <= surface.width)
        {
            // no pixel projection, every point can have its own pixel
            m_numIdxPerPixel = 1;

            // make sure the entire width is used if applicable
            // if (values.num > surface.width)
            // surface.idxNum = surface.width;
            // else
            // surface.idxNum = values.num;
        }
        // else
        // {
        // // NumPerPixel > 1.0
        // surface.numIdxPerPixel = (double) surface.idxNum / (surface.width +
        // 2);
        // }
        // <=============

        if (m_xRangeStart < 0 && m_xRangeEnd < 0) {
            m_xRangeStart = x.minValue;
            m_xRangeEnd = x.maxValue;
        }

        if (m_yRangeStart < 0 || m_yRangeEnd < 0) {
            m_yRangeStart = y.minValue;
            m_yRangeEnd = y.maxValue;
        }

        // TODO: check auto scale flag
        // if (m_xRangeEnd - m_xRangeStart >= surface.width && m_xRangeEnd !=
        // m_xRangeStart)
        {
            m_xIdxScale = surface.width / (double) (m_xRangeEnd - m_xRangeStart);
        }

        // if (m_yRangeEnd - m_yRangeStart >= surface.height && m_yRangeEnd !=
        // m_yRangeStart)
        {
            m_yPxScale = surface.height / (double) (m_yRangeEnd - m_yRangeStart);
        }

        // check if we should keep both scales linked
        if (flags.contains(PlotFlag.LINK_ASPECT)) {
            if (m_xIdxScale < m_yPxScale) {
                m_yPxScale = m_xIdxScale;
            } else if (m_xIdxScale > m_yPxScale)
                m_xIdxScale = m_yPxScale;
        }

        // calculate start idx & end idx to draw based on scroll policy and user
        // translation
        // if (scrollHow == PlotScrollPolicy.DEFAULT)
        // {
        // surface.xIdxTrans = (int) (surface.xTrans);
        // // ==============> cap xTrans at number of existing points
        // if (surface.xIdxTrans <= surface.idxNum - values.num)
        // {
        // surface.xIdxTrans = surface.idxNum - values.num;
        // surface.xTrans = surface.xIdxTrans;
        // }
        // // else if (surface.xIdxTrans >= surface.idxNum)
        // // {
        // // surface.xIdxTrans = surface.idxNum - 1;
        // // }
        // // don't scroll into the future
        // if (surface.xIdxTrans > 0)
        // {
        // surface.xIdxTrans = 0;
        // surface.xTrans = 0;
        // }
        // // <=============
        // surface.idxStart = values.normIdx( values.head - surface.idxNum +
        // surface.xIdxTrans );
        //
        // // only translate end if m_xIdxTrans is negative. Otherwise we would
        // be in the future.
        // surface.idxEnd = values.normIdx( (int) (values.head +
        // (surface.xIdxTrans < 0 ? surface.xIdxTrans : 0)) );
        //
        //
        // }
        // else if (scrollHow == PlotScrollPolicy.OVERRUN)
        // {
        // surface.idxEnd = values.normIdx( surface.idxNum - 1 );
        // surface.idxStart = 0;
        // }

        // ==============> calculate m_yPxScale
        // if (values.rangeMinMax != 0)
        // surface.yPxScale = (surface.height - PlotView.AXIS_PADDING * 2) /
        // (double) values.rangeMinMax;
        // else
        // surface.yPxScale = 1d;

        // apply user scale
        // surface.yPxScale *= surface.yScale;
        // if (surface.yPxScale == 0)
        // surface.yPxScale = 1f;
        // <=============

        // ==============> calculate m_yPxTrans
        // if (surface.yTrans == 0)
        // surface.yPxTrans = (long) (-values.minValue + (PlotView.AXIS_PADDING
        // + 4) / surface.yPxScale);
        // else
        // surface.yPxTrans = (long) surface.yTrans;
        // <=============

        m_xAxisMax = m_xRangeEnd;
        m_xAxisMin = m_xRangeStart;

        m_yAxisMax = (long) m_yRangeEnd;
        m_yAxisMin = (long) m_yRangeStart;

        m_dataLock.unlock();
    }

    @Override
    protected String formatAxisText(PlotAxis axis, int pt) {
        if (axis == xAxis && m_xIdxScale != 0) {
            return Long.toString((long) ((m_xAxisMin + pt / m_xIdxScale) * axis.multiplier));
        } else if (axis == yAxis && m_yPxScale != 0) {
            return Float.toString((long) ((m_yAxisMin + (long) (pt / m_yPxScale)) * axis.multiplier));
        }

        return "n/a";
    }

    private transient RectF tRect = new RectF();
    private transient int tIdx;

    @Override
    protected void drawAxis(Canvas can, PlotSurface surface, boolean drawGrid, boolean drawMap, AxisPaint domainAxisPaint, AxisPaint valueAxisPaint) {
        // ==============> indicator rect where we are on the x-axis right now
        if (drawMap && values.num > 0) {
            // assign borders
            tRect.left = x.tailDistance(m_idxStart) * ((float) surface.viewWidth / values.num);
            tRect.right = (float) (tRect.left + (m_idxNum / (double) x.num) * surface.viewWidth);
            tRect.bottom = surface.viewHeight - PlotView.AXIS_PADDING;
            tRect.top = surface.viewHeight;
            // check bounds
            // if (tRect.left > tRect.right)
            // tRect.right = tRect.left + 1;
            tRect.sort();
            can.drawRect(tRect, PlotView.s_mapPaint);
        }
        // <=============

        drawXAxis(can, surface, xAxis, drawGrid, domainAxisPaint);

        drawYAxis(can, surface, yAxis, drawGrid, valueAxisPaint);
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
            long rval, rx, ry;

            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(' ');
            String line = null;

            // read lines
            while ((line = reader.readLine()) != null) {
                try {
                    // split
                    splitter.setString(line);

                    if (splitter.hasNext()) {
                        // first value
                        rval = Long.parseLong(splitter.next());
                        if (splitter.hasNext()) {
                            rx = Long.parseLong(splitter.next());
                            if (splitter.hasNext()) {
                                ry = Long.parseLong(splitter.next());
                                // add value
                                addValue(rval, rx, ry);
                            } else {
                                // only two values present, assume value = 1
                                addValue(1, rval, rx);
                            }
                            ++count;
                        }
                    }
                } catch (NumberFormatException e) {
                    // it's probably a text/comment line
                    continue;
                }
            }

            reader.close();
        } catch (IOException e) {
        }

        return count;
    }
}
