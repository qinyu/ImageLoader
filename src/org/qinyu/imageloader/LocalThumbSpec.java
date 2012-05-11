package org.qinyu.imageloader;

import org.qinyu.imageloader.ImageSpec;

import android.provider.MediaStore.Images.Thumbnails;

public class LocalThumbSpec extends ImageSpec {

    final int type;

    public LocalThumbSpec(String url, int type) {
        super(url);
        this.type = type;
        if (type != Thumbnails.MICRO_KIND && type != Thumbnails.MICRO_KIND) {
            throw new IllegalArgumentException();
        }

        if (this.type == Thumbnails.MINI_KIND) {
            this.width = 512;
            this.height = 384;
        } else {
            this.width = 128;
            this.height = 128;
        }
    }

}
