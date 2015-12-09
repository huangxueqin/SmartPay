package com.android.smartpay.scanner;

import com.google.zxing.LuminanceSource;

/**
 * Created by xueqin on 2015/11/28 0028.
 */
public class GrayLuminanceSource extends LuminanceSource {
    int left;
    int top;
    int dataWidth;
    int dataHeight;
    byte[] luminances;
    public GrayLuminanceSource(int width, int height, byte[] data) {
        super(width, height);
        left = 0;
        top = 0;
        dataWidth = width;
        dataHeight = height;
        luminances = data;
    }

    public GrayLuminanceSource(int dataWidth, int dataHeight, int left, int top, int width, int height, byte[] data) {
        super(width, height);
        if (left + width > dataWidth || top + height > dataHeight) {
            throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
        }
        this.luminances = data;
        this.dataWidth = dataWidth;
        this.dataHeight = dataHeight;
        this.left = left;
        this.top = top;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Requested row is outside the image: " + y);
        }
        int width = getWidth();
        if (row == null || row.length < width) {
            row = new byte[width];
        }
        int offset = (y + top) * dataWidth + left;
        System.arraycopy(luminances, offset, row, 0, width);
        return row;
    }

    @Override
    public byte[] getMatrix() {
        int width = getWidth();
        int height = getHeight();

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return luminances;
        }

        int area = width * height;
        byte[] matrix = new byte[area];
        int inputOffset = top * dataWidth + left;

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(luminances, inputOffset, matrix, 0, area);
            return matrix;
        }

        // Otherwise copy one cropped row at a time.
        byte[] rgb = luminances;
        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            System.arraycopy(rgb, inputOffset, matrix, outputOffset, width);
            inputOffset += dataWidth;
        }
        return matrix;
    }

    @Override
    public boolean isCropSupported() {
        return true;
    }

    @Override
    public LuminanceSource crop(int left, int top, int width, int height) {
        return new GrayLuminanceSource(
                dataWidth,
                dataHeight,
                this.left + left,
                this.top + top,
                width,
                height, luminances);
    }
}
