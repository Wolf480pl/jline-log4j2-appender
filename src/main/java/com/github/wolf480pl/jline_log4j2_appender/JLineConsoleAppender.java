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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jline.console.ConsoleReader;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import com.github.wolf480pl.jline_log4j2_appender.HookableOutputStreamManager.Listener;

@Plugin(name = "JLineConsole", category = "Core", elementType = "appender", printObject = true)
public class JLineConsoleAppender extends AbstractOutputStreamAppender {
    private static Method getOutputStream = null;
    static {
        try {
            getOutputStream = ConsoleAppender.class.getDeclaredMethod("getOutputStream", Boolean.TYPE, Target.class);
            getOutputStream.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.fatal(e);
        }
    }

    private final HookableOutputStreamManager manager;

    protected JLineConsoleAppender(String name, Layout<? extends Serializable> layout, Filter filter, HookableOutputStreamManager manager, boolean ignoreExceptions) {
        super(name, layout, filter, ignoreExceptions, true, manager);
        this.manager = manager;
    }

    @Override
    public void append(LogEvent event) {
        Message msg = event.getMessage();
        if (msg instanceof ConsoleSetupMessage) {
            ConsoleReader reader = ((ConsoleSetupMessage) msg).getReader();
            Listener listener = new ConsoleReaderListener(reader);
            switch (((ConsoleSetupMessage) msg).getAction()) {
            case ADD:
                this.manager.addListener(listener);
                break;
            case REMOVE:
                this.manager.removeListener(listener);
            }
        }
        super.append(event);
    }

    @PluginFactory
    public static JLineConsoleAppender createAppender(@PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filters") final Filter filter,
            @PluginAttribute("target") final String t, @PluginAttribute("name") final String name, @PluginAttribute("follow") final String follow,
            @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createLayout(null, null, null, null, null);
        }
        final boolean isFollow = Boolean.parseBoolean(follow);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t);
        return new JLineConsoleAppender(name, layout, filter, getManager(isFollow, target, layout), ignoreExceptions);
    }

    protected static HookableOutputStreamManager getManager(boolean follow, Target target, Layout<? extends Serializable> layout) {
        OutputStream stream;
        try {
            stream = (OutputStream) getOutputStream.invoke(null, follow, target);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return HookableOutputStreamManager.getHookableManager(target.name() + ".jline." + follow, new FactoryData(stream, layout), FACTORY);
    }

    public static class FactoryData {
        private final OutputStream os;
        private final Layout<? extends Serializable> layout;

        public FactoryData(final OutputStream os, final Layout<? extends Serializable> layout) {
            this.os = os;
            this.layout = layout;
        }
    }

    public static ConsoleManagerFactory FACTORY = new ConsoleManagerFactory();

    public static class ConsoleManagerFactory implements ManagerFactory<HookableOutputStreamManager, FactoryData> {
        @Override
        public HookableOutputStreamManager createManager(String name, FactoryData data) {
            return new HookableOutputStreamManager(data.os, name, data.layout);
        }
    }

    public static class ConsoleReaderListener implements Listener {
        private final ConsoleReader reader;
        private boolean writing = false;

        public ConsoleReaderListener(ConsoleReader reader) {
            this.reader = reader;
        }

        @Override
        public void onWrite() {
            if (!this.writing) {
                this.writing = true;
                try {
                    this.reader.print(String.valueOf(ConsoleReader.RESET_LINE));
                    this.reader.flush();
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }

        @Override
        public void onFlush() {
            this.writing = false;
            try {
                this.reader.drawLine();
            } catch (IOException e) {
                this.reader.getCursorBuffer().clear();
                LOGGER.error(e);
            }
            try {
                this.reader.flush();
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof ConsoleReaderListener) {
                return this.reader == ((ConsoleReaderListener) obj).reader;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.reader.hashCode();
        }
    }
}
