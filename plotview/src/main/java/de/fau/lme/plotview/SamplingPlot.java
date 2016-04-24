/**
 * SamplingPlot.java
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

import android.graphics.Paint;
import android.util.Log;

/**
 * SamplingPlot for displaying (real time) sampling data. IMPORTANT: the Android
 * (software) drawing operations require about 10-50 ms, depending on the number
 * of samples displayed in the viewport. So remember to change the maxRedrawRate
 * in the PlotView accordingly to avoid lags!
 *
 * @author Stefan Gradl
 *
 */
public class SamplingPlot extends Plot1D {
    /**
     * @param plotTitle
     * @param paint
     * @param style
     * @param maxCache
     */
    public SamplingPlot(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        super(plotTitle, paint, style, maxCache);
        // TODO Auto-generated constructor stub
    }

    public SamplingPlot(String plotTitle, Paint paint, PlotStyle style, int maxCache, boolean maintainMinMax) {
        super(plotTitle, paint, style, maxCache, maintainMinMax);
        // TODO Auto-generated constructor stub
    }

    /**
     * Adds a single sample to this plot. The current system time is used as
     * timestamp. You should only use this method if you don't have a
     * millisecond timestamp at hand.
     *
     * @param value
     *            sample-value
     */
    public void addValue(long value) {
        // TODO: implement the add directly
        addValue(value, System.currentTimeMillis());
    }

    /**
     * Specify a time window of how many samples should be drawn in the view. If
     * the number of seconds is larger than the maximal number of seconds
     * specified on construction, that value is used.
     *
     * @param timeInSeconds
     *            The number of seconds of the time window to display.
     */
    public void setViewport(int samplingRateInHz, int timeInSeconds) {
        m_desiredViewportIdxNum = timeInSeconds * samplingRateInHz;
    }

    @Override
    protected String formatAxisText(PlotAxis axis, int pt) {
        if (axis == xAxis) {
            if (m_idxStart + m_numIdxPerPixel * pt >= x.num)
                return "n/a";

            long timeInMillis = x.get((int) (m_idxStart + m_numIdxPerPixel * pt));
            return String.format("%.2f", (double) timeInMillis / 1000d);
        } else if (axis == valueAxis && m_yPxScale != 0) {
            if (m_yPxScale > 10)
                return String.format("%.2f", ((-m_yPxTrans + pt / m_yPxScale) * axis.multiplier));
            else
                return String.format("%.0f", ((-m_yPxTrans + pt / m_yPxScale) * axis.multiplier));
        }

        return "n/a";
    }
}
