/*
 * This file is part of JLine Console Log4j2 Appender, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Wolf480pl <wolf480@interia.pl/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.wolf480pl.jline_log4j2_appender;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;

public class HookableOutputStreamManager extends OutputStreamManager {
    private final Set<Listener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<Listener, Boolean>());

    public HookableOutputStreamManager(OutputStream os, String streamName, Layout<?> layout) {
        super(os, streamName, layout);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    protected void write(final byte[] bytes, final int offset, final int length) {
        for (Listener listener : this.listeners) {
            listener.onWrite();
        }
        super.write(bytes, offset, length);
    }

    @Override
    public void flush() {
        super.flush();
        for (Listener listener : this.listeners) {
            listener.onFlush();
        }
    }

    @Override
    public void releaseSub() {
        super.releaseSub();
        this.listeners.clear();
    }

    public static interface Listener {
        void onWrite();

        void onFlush();
    }

    public static <T> HookableOutputStreamManager getHookableManager(final String name, final T data, final ManagerFactory<? extends HookableOutputStreamManager, T> factory) {
        return AbstractManager.getManager(name, factory, data);
    }
}
