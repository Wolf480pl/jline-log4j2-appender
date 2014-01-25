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

import jline.console.ConsoleReader;

import org.apache.logging.log4j.message.SimpleMessage;

public class ConsoleSetupMessage extends SimpleMessage {
    private final ConsoleReader reader;
    private final Action action;

    public ConsoleSetupMessage(ConsoleReader reader, String message) {
        this(reader, Action.ADD, message);
    }

    public ConsoleSetupMessage(ConsoleReader reader, Action action, String message) {
        super(message);
        this.reader = reader;
        this.action = action;
    }

    public ConsoleReader getReader() {
        return this.reader;
    }

    public Action getAction() {
        return this.action;
    }

    public static enum Action {
        ADD, REMOVE
    }
}
