/**
 * Plot3D.java
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

import android.graphics.Paint;

/**
 * Three-dimensional (space) plot
 *
 * @author Stefan Gradl
 */
public class Plot3D extends Plot2D {
    protected LongValueList z;

    /**
     * @param plotTitle
     * @param xUnitName
     * @param yUnitName
     * @param m_paint
     * @param style
     * @param maxCache
     */
    public Plot3D(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        super(plotTitle, paint, style, maxCache);
        // TODO Auto-generated constructor stub
    }

}
