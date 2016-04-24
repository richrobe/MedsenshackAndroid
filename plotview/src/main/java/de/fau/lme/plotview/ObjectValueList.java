/**
 * ObjectValueList.java
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Stefan Gradl
 *
 */
public class ObjectValueList extends CircularValueList implements List<Object> {
    /**
     * The raw values
     */
    public Object[] values = null;

    private ObjectValueList(int cacheSize, boolean maintainMinMax) {
        super(cacheSize, false);
    }

    public ObjectValueList(int cacheSize) {
        super(cacheSize, false );
        values = new Object[sizeMax];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Object object )
    {
        ++head;
        if (head == sizeMax)
            head = 0;

        values[head] = object;

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

        }

        return true;
    }

    /**
     * Acts like add(Object) but without assigning and just returns the pointer
     * to the next object.
     *
     * @return
     */
    public Object next()
    {
        ++head;
        if (head == sizeMax)
            head = 0;

        if (num < sizeMax)
            ++num;
        else
        {
            // if buffer is entirely filled, tail increases with head
            ++tail;
            if (tail == sizeMax)
                tail = 0;
        }

        return values[head];
    }

    /**
     * Returns the object that lies idxPast entries before the current head.
     *
     * @param idxPast
     *            Positive number indicating how many values to go into the
     *            past.
     * @return the object at the given index in the past.
     */
    public Object getPastValue(int idxPast) {
        return get(head - idxPast );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int location, Object object )
    {
        // === TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends Object> arg0 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int arg0, Collection<? extends Object> arg1 )
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
    public Object get(int rIdx )
    {
        // no elements
        if (num == 0)
            return null;

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
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object object )
    {
        // === TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#iterator()
     */
    public Iterator<Object> iterator()
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object object )
    {
        // === TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator()
     */
    public ListIterator<Object> listIterator()
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<Object> listIterator(int location )
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(int)
     */
    public Object remove(int location )
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object object )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> arg0 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> arg0 )
    {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int location, Object object )
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#subList(int, int)
     */
    public List<Object> subList(int start, int end )
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray()
     */
    public Object[] toArray()
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] array )
    {
        // === TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#add(float)
     */
    @Override
    public int add(float newValue )
    {
        // === TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#add(long)
     */
    @Override
    public int add(long newValue )
    {
        // === TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#findMax()
     */
    @Override
    public void findMax()
    {
        // === TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#findMin()
     */
    @Override
    public void findMin()
    {
        // === TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see de.lme.plotview.CircularValueList#findMinMax()
     */
    @Override
    public void findMinMax()
    {
        // === TODO Auto-generated method stub

    }

}
