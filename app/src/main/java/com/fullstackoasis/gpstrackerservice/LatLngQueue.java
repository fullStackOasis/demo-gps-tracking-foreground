/**
 * Copyright 2020 Marya Doery
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.fullstackoasis.gpstrackerservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LatLngQueue<T> implements Queue<T> {
    // Limit number of points in Queue
    private int MAX_XPOINTS;
    LatLngQueue(int MAX_XPOINTS) {
        this.MAX_XPOINTS = MAX_XPOINTS;
    }

    private ArrayList<T> backingArray = new ArrayList<T>(MAX_XPOINTS);

    @Override
    public int size() {
        return backingArray.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return false;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return backingArray.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        if (this.size() >= MAX_XPOINTS) {
            backingArray.remove(0);
        }
        backingArray.add(t);
        return true;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        backingArray.remove(0);
        return true;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean offer(T t) {
        return false;
    }

    @Override
    public T remove() {
        return null;
    }

    @Nullable
    @Override
    public T poll() {
        return null;
    }

    @Override
    public T element() {
        return null;
    }

    @Nullable
    @Override
    public T peek() {
        return null;
    }
}

