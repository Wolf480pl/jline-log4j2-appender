package com.github.wolf480pl.jline_log4j2_appender;

import jline.console.ConsoleReader;

import org.apache.logging.log4j.message.SimpleMessage;

public class ConsoleSetupMessage extends SimpleMessage {
    private final ConsoleReader reader;

    public ConsoleSetupMessage(ConsoleReader reader, String message) {
        super(message);
        this.reader = reader;
    }

    public ConsoleReader getReader() {
        return this.reader;
    }

}
