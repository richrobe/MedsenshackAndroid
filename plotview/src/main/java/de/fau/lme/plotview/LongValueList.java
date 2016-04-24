/**
 * LongValueList.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Assert;
import android.util.Log;

/**
 *
 * @author Stefan Gradl
 *
 */
public class LongValueList extends CircularValueList implements List<Long> {
    /**
     * The raw values
     */
    public long[] values = null;

    public long minValue = Long.MAX_VALUE;
    public long maxValue = Long.MIN_VALUE;

    /** the distance between the min and max value */
    public long rangeMinMax = 1;

    /** TRANSIENT variables */
    private transient int tIter = 0;

    /**
     * Constructs a new PlotValueList with the given preallocated entries.
     *
     * @param cacheSize
     *            Number of preallocated entries.
     * @param maintainMinMax
     *            Whether to maintain the min and max values.
     */
    public LongValueList(int cacheSize, boolean maintainMinMax) {
        super(cacheSize, maintainMinMax );
        values = new long[sizeMax];
    }

    /**
     * Adds a new entry to the ring, possibly overwriting the eldest entry.
     *
     * @param newValue
     *            The value to add to the list.
     * @return new head position
     */
    public int add(long newValue )
    {
        ++head;
        if (head == sizeMax)
            head = 0;

        values[head] = newValue;

        if (num < sizeMax)
            ++num;
        else
        {
            // if buffer is entirely filled, tail increases with head
            ++tail;
            if (tail == sizeMax)
                tail = 0;
        }

        if (maintainMinMax)
        {
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
    public void clear()
    {
        super.clear();
        minValue = Long.MAX_VALUE;
        maxValue = Long.MIN_VALUE;
    }

    /**
     * Copies the contents of sourceList into this ring buffer.
     *
     * @param sourceList
     */
    public void copy(ArrayList<Long> sourceList )
    {
        num = sourceList.size();

        Assert.assertTrue(num <= sizeMax );

        head = -1;
        for (Long l : sourceList)
        {
            values[++head] = l;
        }

        tail = 0;

        if (maintainMinMax)
        {
            findMinMax();
        }
    }

    /**
     * Iterates over all valid elements and fills the max value.
     */
    public void findMax()
    {
        maxValue = minValue;
        for (tIter = 0; tIter < num; tIter++)
        {
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
    public void findMin()
    {
        minValue = maxValue;
        for (tIter = 0; tIter < num; tIter++)
        {
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
    public void findMinMax()
    {
        minValue = Long.MAX_VALUE;
        minIdx = -1;
        maxValue = Long.MIN_VALUE;
        maxIdx = -1;
        for (tIter = 0; tIter < num; tIter++)
        {
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
     * Returns the value at the current head position.
     */
    public long getHeadValue()
    {
        if (head < 0)
            return 0;
        return values[head];
    }

    /**
     * Returns the value at position rIdx. It has the same effect as calling
     * this.value[normIdx(rIdx)].
     *
     * @param rIdx
     *            negative or positive index in the ring
     * @return the value at rIdx or -1 if the List doesn't contain any elements
     */
    public long getIndirect(int rIdx )
    {
        // no elements
        if (num == 0)
            return -1;

        return values[normIdx( rIdx )];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int location, Long object) {
        values[normIdx(location)] = object.longValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends Long> arg0 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int arg0, Collection<? extends Long> arg1 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object object )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> arg0 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#get(int)
     */
    public Long get(int location) {
        return getIndirect( location );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object object )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#iterator()
     */
    public Iterator<Long> iterator()
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object object )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator()
     */
    public ListIterator<Long> listIterator()
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<Long> listIterator(int location )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(int)
     */
    public Long remove(int location )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object object )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> arg0 )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> arg0 )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Long set(int location, Long object) {
        int idx = normIdx( location );
        long prev = values[idx];
        values[idx] = object;
        return prev;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#subList(int, int)
     */
    public List<Long> subList(int start, int end )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray()
     */
    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] array )
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Long object) {
        return add(object.longValue() ) != -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#add(float)
     */
    @Override
    public int add(float newValue) {
        return add((long) newValue );
    }
}
