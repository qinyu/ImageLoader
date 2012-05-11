package org.qinyu.utils;

import java.io.InputStream;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.text.TextUtils;
import android.util.FloatMath;

public class BitmapHelper {
    private static final int RETRY_TIMES = 3;

    public static void fillSuitableSampleSize(BitmapFactory.Options resample, int maxWidth, int maxHeight, int width,
            int height) {
        boolean withinBounds = (maxWidth <= 0 || width <= maxWidth) && (maxHeight <= 0 || height <= maxHeight);

        resample.inJustDecodeBounds = false;
        resample.inSampleSize = 1;
        resample.inPurgeable = true;
        resample.inInputShareable = false;
        if (!withinBounds) {
            int sampleSizeW = (int) FloatMath.floor((float) width / (float) maxWidth);
            int sampleSizeH = (int) FloatMath.floor((float) height / (float) maxHeight);
            int initialSize = Math.max(sampleSizeW, sampleSizeH);
            if (initialSize > 128) {
                initialSize = 128;
            }
            // if (x > 1) {
            // x--;
            // x |= x >> 1;
            // x |= x >> 2;
            // x |= x >> 4;
            // x |= x >> 8;
            // x |= x >> 16;
            // x++;
            // }

            int roundedSize;
            if (initialSize <= 8) {
                roundedSize = 1;
                while (roundedSize < initialSize) {
                    roundedSize <<= 1;
                }
            } else {
                roundedSize = (initialSize + 7) / 8 * 8;
            }

            resample.inSampleSize = roundedSize;
        }
    }

    private static abstract class AbstractDecoder {
        Bitmap decode(int maxWidth, int maxHeight) {
            Options bounds = new Options();
            bounds.inJustDecodeBounds = true;
            decode(bounds);
            int imgWidth = bounds.outWidth;
            int imgHeight = bounds.outHeight;
            boolean cancel = bounds.mCancel;
            if (!cancel && imgWidth != -1 && imgHeight != -1) {
                Bitmap decodeFile = null;
                Options suitableSampleSize = new Options();
                fillSuitableSampleSize(suitableSampleSize, maxWidth, maxHeight, imgWidth, imgHeight);
                int retry = RETRY_TIMES;
                while (retry > 0)
                    try {
                        decodeFile = decode(suitableSampleSize);
                        if (decodeFile != null)
                            break;
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {

                        }
                        retry--;
                        suitableSampleSize.inSampleSize *= 2;
                    }
                return decodeFile;
            }
            return null;

        }

        abstract Bitmap decode(Options opts);
    }

    private static class FileDecoder extends AbstractDecoder {
        private String path;

        public FileDecoder(String path) {
            super();
            this.path = path;
        }

        protected Bitmap decode(Options opts) {
            return BitmapFactory.decodeFile(path, opts);
        }
    }

    private static class StreamDecoder extends AbstractDecoder {
        private InputStream is;

        public StreamDecoder(InputStream is) {
            super();
            this.is = is;
        }

        protected Bitmap decode(Options opts) {
            return BitmapFactory.decodeStream(is, null, opts);
        }
    }

    private static class ResourceDecoder extends AbstractDecoder {
        private Resources res;
        private int id;

        public ResourceDecoder(Resources res, int id) {
            super();
            this.res = res;
            this.id = id;
        }

        protected Bitmap decode(Options opts) {
            return BitmapFactory.decodeResource(res, id, opts);
        }
    }

    public static Bitmap decodeFile(String path, int maxWidth, int maxHeight) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        if (path.startsWith(ContentResolver.SCHEME_FILE)) {
            path = path.substring(7);
        }
        return new FileDecoder(path).decode(maxWidth, maxHeight);
    }

    public static Bitmap decodeStream(InputStream is, int maxWidth, int maxHeight) {
        return new StreamDecoder(is).decode(maxWidth, maxHeight);
    }

    public static Bitmap decodeResource(Resources res, int id, int maxWidth, int maxHeight) {
        return new ResourceDecoder(res, id).decode(maxWidth, maxHeight);
    }

}
