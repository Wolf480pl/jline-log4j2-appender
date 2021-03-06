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

import jline.console.ConsoleReader;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.fusesource.jansi.AnsiConsole;

import com.github.wolf480pl.jline_log4j2_appender.HookableOutputStreamManager.Listener;

@Plugin(name = "JLineConsole", category = "Core", elementType = "appender", printObject = true)
public class JLineConsoleAppender extends AbstractOutputStreamAppender {
    private final HookableOutputStreamManager manager;
    private OutputStreamManager held; // Make sure they don't close System.out or System.err when we're still using it.

    protected JLineConsoleAppender(String name, Layout<? extends Serializable> layout, Filter filter, HookableOutputStreamManager manager, OutputStreamManager held, boolean ignoreExceptions) {
        super(name, layout, filter, ignoreExceptions, true, manager);
        this.manager = manager;
        this.held = held;
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

    @Override
    public void stop() {
        super.stop();
        this.held.release();
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
            layout = PatternLayout.newBuilder().build();
        }
        final boolean isFollow = Boolean.parseBoolean(follow);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t);
        FactoryData data = new FactoryData(getStream(isFollow, target), layout);
        return new JLineConsoleAppender(name, layout, filter, getManager(isFollow, target, data), getHeldManager(isFollow, target, data), ignoreExceptions);
    }

    protected static HookableOutputStreamManager getManager(boolean follow, Target target, FactoryData data) {
        return HookableOutputStreamManager.getHookableManager(target.name() + ".jline." + follow, data, FACTORY);
    }

    protected static OutputStreamManager getHeldManager(boolean follow, Target target, FactoryData data) {
        return OutputStreamManager.getManager(target.name() + "." + follow, data, FACTORY);
    }

    protected static HookableOutputStreamManager getManager(boolean follow, Target target, Layout<? extends Serializable> layout) {
        OutputStream stream;
        stream = getStream(follow, target);
        return HookableOutputStreamManager.getHookableManager(target.name() + ".jline." + follow, new FactoryData(stream, layout), FACTORY);
    }

    protected static OutputStream getStream(boolean follow, Target target) {
        OutputStream os;
        if (target == Target.SYSTEM_ERR) {
            os = follow ? new SystemErrStream() : new NeverClosingOutputStream(System.err);
        } else {
            os = follow ? new SystemOutStream() : new NeverClosingOutputStream(System.out);
        }
        return AnsiConsole.wrapOutputStream(os);
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
