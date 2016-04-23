/**
 * FloatValueList.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Assert;

import android.util.FloatMath;

/**
 * @author Stefan Gradl
 */
public class FloatValueList extends CircularValueList implements List<Float> {
    /**
     * The raw values
     */
    public float[] values = null;

    public float minValue = Float.MAX_VALUE;
    public float maxValue = Float.MIN_VALUE;

    /**
     * the distance between the min and max value
     */
    public float rangeMinMax = 1;

    /**
     * sum of all values in this list. is maintained if maintainSum is true.
     * allows for fast statistics calculations
     */
    public double sum = 0;

    /**
     * TRANSIENT variables
     */
    private transient int tIter = 0;

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public FloatValueList(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        super(cacheSize, maintainMinMax, maintainSum);
        values = new float[sizeMax];
    }

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public FloatValueList(int cacheSize, boolean maintainMinMax) {
        super(cacheSize, maintainMinMax);
        this.maintainSum = false;
        values = new float[sizeMax];
    }

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize Number of preallocated entries.
     */
    public FloatValueList(int cacheSize) {
        super(cacheSize);
        values = new float[sizeMax];
    }

    /**
     * Adds a new entry to the ring, possibly overwriting the eldest entry.
     *
     * @param newValue The value to add to the list.
     * @return new head position
     */
    public int add(float newValue) {
        ++head;
        if (head == sizeMax)
            head = 0;

        if (maintainSum) {
            // update sum value, subtracting the old value that gets overwritten
            // and adding the new one
            sum = sum - values[head] + newValue;
        }

        values[head] = newValue;

        if (num < sizeMax)
            ++num;
        else {
            // if buffer is entirely filled, tail increases with head
            ++tail;
            if (tail == sizeMax)
                tail = 0;
        }

        if (maintainMinMax) {
            // check min/max
            if (newValue <= minValue) {
                // new MIN
                minValue = newValue;
                minIdx = head;
                rangeMinMax = maxValue - minValue;
            } else if (newValue >= maxValue) {
                // new MAX
                maxValue = newValue;
                maxIdx = head;
                rangeMinMax = maxValue - minValue;
            } else {
                // the new value is not a new min or max
                // check if we are going to overwrite the old min/max
                if (head == minIdx) {
                    // search for new minimum
                    findMin();
                } else if (head == maxIdx) {
                    // search for new maximum
                    findMax();
                }
            }
        }

        return head;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#clear()
     */
    public void clear() {
        super.clear();
        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;
        rangeMinMax = 1;
        sum = 0;
        Arrays.fill(values, 0f);
    }

    /**
     * Copies the contents of sourceList into this ring buffer.
     *
     * @param sourceList
     */
    public void copy(ArrayList<? extends Number> sourceList) {
        num = sourceList.size();

        Assert.assertTrue(num <= sizeMax);

        head = -1;
        for (Number l : sourceList) {
            values[++head] = l.floatValue();
        }

        tail = 0;

        if (maintainMinMax) {
            findMinMax();
        }
    }

    /**
     * Copies the contents of sourceList into this ring buffer.
     *
     * @param sourceList
     */
    public void copy(FloatValueList sourceList) {
        num = sourceList.size();

        Assert.assertTrue(num <= sizeMax);

        head = -1;

        if (maintainSum) {
            // only iterate if we should maintain the sum
            for (int i = 0; i < sourceList.num; ++i) {
                values[++head] = sourceList.values[i];
                sum += sourceList.values[i];
            }
        } else {
            // otherwise just copy
            System.arraycopy(sourceList, 0, values, 0, sourceList.num);
            head = sourceList.head;
        }

        tail = 0;

        if (maintainMinMax) {
            findMinMax();
        }
    }

    /**
     * Iterates over all valid elements and fills the max value.
     */
    public void findMax() {
        maxValue = minValue;
        for (tIter = 0; tIter < num; tIter++) {
            // new max?
            if (values[tIter] > maxValue) {
                maxValue = values[tIter];
                maxIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Iterates over all valid elements and fills the min value.
     */
    public void findMin() {
        minValue = maxValue;
        for (tIter = 0; tIter < num; tIter++) {
            // new min?
            if (values[tIter] < minValue) {
                minValue = values[tIter];
                minIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Iterates over all valid elements and fills the min & max value.
     */
    public void findMinMax() {
        minValue = Float.MAX_VALUE;
        minIdx = -1;
        maxValue = Float.MIN_VALUE;
        maxIdx = -1;
        for (tIter = 0; tIter < num; tIter++) {
            // new max?
            if (values[tIter] > maxValue) {
                maxValue = values[tIter];
                maxIdx = tIter;
            }
            // new min?
            else if (values[tIter] < minValue) {
                minValue = values[tIter];
                minIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Returns the mean of all values in this list.
     *
     * @return
     */
    public float getMean() {
        if (num > 0)
            return (float) (sum / num);
        return 0f;
    }

    /**
     * Returns the value at the current head position.
     */
    public float getHeadValue() {
        if (head < 0)
            return 0f;
        return values[head];
    }

    /**
     * Returns the value that lies idxPast entries before the current head.
     *
     * @param idxPast Positive number indicating how many values to go into the
     *                past.
     * @return the value at the given index in the past.
     */
    public float getPastValue(int idxPast) {
        return getIndirect(head - idxPast);
    }

    /**
     * Returns the value at position rIdx. It has the same effect as calling
     * this.value[normIdx(rIdx)].
     *
     * @param rIdx negative or positive index in the ring
     * @return the value at rIdx or -1 if the List doesn't contain any elements
     */
    public float getIndirect(int rIdx) {
        // no elements
        if (num == 0)
            return -1f;

        else if (rIdx < -num)
            return values[(num << 1) + rIdx];

        else if (rIdx < 0)
            return values[num + rIdx];

        else if (rIdx >= num)
            return values[rIdx - num];

        return values[rIdx];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Float object) {
        return add(object.floatValue()) != -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int location, Float object) {
        values[normIdx(location)] = object.floatValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends Float> arg0) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int arg0, Collection<? extends Float> arg1) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object object) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> arg0) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#get(int)
     */
    public Float get(int location) {
        return getIndirect(normIdx(location));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#iterator()
     */
    public Iterator<Float> iterator() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator()
     */
    public ListIterator<Float> listIterator() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<Float> listIterator(int location) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(int)
     */
    public Float remove(int location) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Float set(int location, Float object) {
        int idx = normIdx(location);
        float prev = values[idx];
        values[idx] = object;
        return prev;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#subList(int, int)
     */
    public List<Float> subList(int start, int end) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] array) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#add(long)
     */
    @Override
    public int add(long newValue) {
        return add((float) newValue);
    }

    public String toString(String elementFormatter) {
        StringBuilder str = new StringBuilder(num * 16);
        for (tIter = 0; tIter < num; ++tIter) {
            str.append(String.format(elementFormatter, values[tIter])).append("; ");
        }
        return str.toString();
    }

    /**
     * Calculate statistical values in the given range, including start and end.
     *
     * @param start
     * @param end
     * @param stats receives the computed statistics. must not be null.
     */
    public void calculateStats(int start, int end, Statistics stats) {
        stats.sum = stats.rms = 0f;
        stats.num = end - start + 1;

        // calculate sum & rms
        for (tIter = start; tIter <= end; ++tIter) {
            stats.sum += values[tIter];
            stats.rms += values[tIter] * values[tIter];
        }

        // average
        stats.average = stats.sum / stats.num;

        // we don't calculate the root for the RMS immediately since we need it
        // squared for the variance
        stats.rms /= stats.num;

        // calculate variance, exploiting RMS == avg + variance
        stats.variance = stats.rms - stats.average * stats.average;

        // no we calculate the root for the RMS
        stats.rms = (float) Math.sqrt(stats.rms);

        // standard deviation
        stats.stdDeviation = (float) Math.sqrt(stats.variance);
    }
}
