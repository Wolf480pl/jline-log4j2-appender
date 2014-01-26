package com.github.wolf480pl.jline_log4j2_appender;

import java.io.IOException;
import java.io.OutputStream;

public class SystemErrStream extends OutputStream {

    @Override
    public void flush() throws IOException {
        System.err.flush();
    }

    @Override
    public void write(final byte b[]) throws IOException {
        System.err.write(b);
    }

    @Override
    public void write(final byte b[], final int off, final int len) throws IOException {
        System.err.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        System.err.write(b);
    }

}
