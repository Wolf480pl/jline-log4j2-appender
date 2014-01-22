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
