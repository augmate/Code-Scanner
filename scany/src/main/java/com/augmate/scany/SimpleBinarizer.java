package com.augmate.scany;

import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.qrcode.encoder.ByteMatrix;

public class SimpleBinarizer {
    LuminanceSource source;
    byte[] luminances = new byte[0];

    protected SimpleBinarizer(LuminanceSource source) {
        this.source = source;
    }

    public ByteMatrix getBlackMatrix() throws NotFoundException {

        if (luminances.length < source.getWidth()) {
            luminances = new byte[source.getWidth()];
        }

        int width = source.getWidth();
        int height = source.getHeight();

        ByteMatrix bytes = new ByteMatrix(width, height);

        for (int y = 0; y < height; y++) {
            source.getRow(y, luminances);

            for (int x = 0; x < width; x++) {
                int pixel = luminances[x] & 0xff;
                bytes.set(x, y, pixel < 128);
            }
        }

        return bytes;
    }
}
