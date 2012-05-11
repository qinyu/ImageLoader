package org.qinyu.imageloader;

import android.graphics.Bitmap;

public interface Callback<T> {

    void onStart(T t, ImageSpec spec);

    void onCancel(T t, ImageSpec spec);

    void onSuccess(T t, ImageSpec spec, Bitmap bitmap);

    void onFail(T t, ImageSpec spec, Exception e);
}
