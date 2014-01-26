package com.github.wolf480pl.jline_log4j2_appender;

import java.io.IOException;
import java.io.OutputStream;

public class SystemOutStream extends OutputStream {

    @Override
    public void flush() throws IOException {
        System.out.flush();
    }

    @Override
    public void write(final byte b[]) throws IOException {
        System.out.write(b);
    }

    @Override
    public void write(final byte b[], final int off, final int len) throws IOException {
        System.out.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        System.out.write(b);
    }

}
