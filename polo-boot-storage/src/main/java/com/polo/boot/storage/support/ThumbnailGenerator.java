package com.polo.boot.storage.support;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThumbnailGenerator {

    public InputStream generate(InputStream inputStream, int width, int height) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("生成缩略图失败", ex);
        }
    }
}
