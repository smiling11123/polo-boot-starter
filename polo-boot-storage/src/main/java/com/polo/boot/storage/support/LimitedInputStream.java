package com.polo.boot.storage.support;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 只允许读取指定长度的输入流包装器。
 */
public class LimitedInputStream extends FilterInputStream {

    private long remaining;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.remaining = Math.max(limit, 0);
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int value = super.read();
        if (value != -1) {
            remaining--;
        }
        return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int actualLength = (int) Math.min(len, remaining);
        int read = super.read(b, off, actualLength);
        if (read > 0) {
            remaining -= read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(Math.min(n, remaining));
        remaining -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(super.available(), remaining);
    }
}
