/**
 * CircularValueList.java
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

/**
 * High performance ring buffer implementation. All members are public so they
 * may be accessed directly, however the caller has to make sure not to crash
 * the list then.
 * <p/>
 * It is an enforced policy of this implementation to never do any
 * reallocations. The number of items given in the constructor will be the
 * absolute maximum of items the list may hold during its entire lifetime.
 *
 * @author Stefan Gradl
 */
public abstract class CircularValueList {
    // @formatter:off
    /**
     * <p/>
     * <p/>
     * <pre>
     *
     *                 head
     *                  |
     *        +---+---+---+---+
     *        | 0 | 1 | 2 | 3 |
     *        +---+---+---+---+
     *          |           |
     *         tail        EOR
     *
     *
     *  num = 4
     *
     * 	rIdx >= num
     *  	rIdx = 5	= 5 - 4		= 1
     *      rIdx = 4	= 4 - 4		= 0
     *
     *
     * 	rIdx < 0
     * 		rIdx = -2	= 4 - 2		= 2
     * 		rIdx = -4	= 4 - 4		= 0
     *
     *  rIdx < -num
     *   	rIdx = -5	= 8 - 5		= 3
     *   	rIdx = -8	= 8 - 8		= 0
     *
     *
     *
     * </pre>
     * <p/>
     * <p/>
     */
    // @formatter:on

    public int sizeMax = 0;
    /**
     * End Of Ring - largest valid index, not necessarily used! i.e. EOR may or
     * may not be the same as head or num-1 but always is equal to (sizeMax-1).
     */
    public int EOR = -1;

    /**
     * index of the entry most recently added
     */
    public int head = -1;
    /**
     * index of the oldest entry, when the ring is filled this will always be
     * head + 1
     */
    public int tail = 0;
    /**
     * count of currently used indices
     */
    public int num = 0;

    public int minIdx = -1;
    public int maxIdx = -1;

    /**
     * Indicates whether the minimum and maximum values&indices are updated on
     * every add(), remove() or set() call
     */
    public boolean maintainMinMax = false;
    /**
     * Indicates whether the sum of all valid values is updated on every add(),
     * remove() or set() call
     */
    public boolean maintainSum = false;

    @SuppressWarnings("unused")
    private CircularValueList() {
    }

    /**
     * Private initializer. Only called by the constructors.
     *
     * @param cacheSize
     * @param maintainMinMax
     * @param maintainSum
     */
    private void init(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        if (cacheSize <= 0)
            sizeMax = 1022;
        else
            sizeMax = cacheSize;

        EOR = sizeMax - 1;

        this.maintainMinMax = maintainMinMax;
        this.maintainSum = maintainSum;
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public CircularValueList(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        init(cacheSize, maintainMinMax, maintainSum);
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public CircularValueList(int cacheSize, boolean maintainMinMax) {
        init(cacheSize, maintainMinMax, false);
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize Number of preallocated entries.
     */
    public CircularValueList(int cacheSize) {
        init(cacheSize, false, false);
    }

    public void clear() {
        head = -1;
        tail = 0;
        num = 0;
        minIdx = -1;
        maxIdx = -1;
    }

    /**
     * Normalizes the given negative or positive index. Valid index range: [ -2
     * * num <= rIdx < 2 * num [
     *
     * @param rIdx negative or positive index to normalize
     * @return The normalized index that is within valid array bounds (if rIdx
     * was in the valid range!). Or 0 if this list doesn't contain any
     * elements
     */
    public int normIdx(int rIdx) {
        if (num == 0)
            return 0;

        else if (rIdx < -num)
            return (num << 1) + rIdx;

        else if (rIdx < 0)
            return num + rIdx;

        else if (rIdx >= num)
            return rIdx - num;

        return rIdx;
    }

    /**
     * @param rIdx
     * @return The distance to tail.
     */
    public int tailDistance(int rIdx) {
        if (rIdx >= tail)
            return rIdx - tail;

        return num - tail + rIdx;
    }

    /**
     * @return Count of used entries.
     */
    public int size() {
        return num;
    }

    public boolean isEmpty() {
        return (num == 0);
    }

    public abstract int add(float newValue);

    public abstract int add(long newValue);

    public abstract void findMax();

    public abstract void findMin();

    public abstract void findMinMax();

    /**
     * Statistical information about a certain range of values.
     *
     * @author sistgrad
     */
    public static class Statistics {
        public int idxStart = 0;
        public int idxEnd = 0;
        public int num = 0;

        public float sum = 0f;
        public float average = 0f;
        public float variance = 0f;
        public float stdDeviation = 0f;
        public float rms = 0f;
    }
}
