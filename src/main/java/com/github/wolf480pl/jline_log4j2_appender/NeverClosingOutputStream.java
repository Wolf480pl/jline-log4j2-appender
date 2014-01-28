package com.github.wolf480pl.jline_log4j2_appender;

import java.io.FilterOutputStream;
import java.io.OutputStream;

public class NeverClosingOutputStream extends FilterOutputStream {

    public NeverClosingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() {
        // Do nothing!
    }

}
