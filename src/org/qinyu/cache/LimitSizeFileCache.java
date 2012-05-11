package org.qinyu.cache;


import android.content.Context;

public class LimitSizeFileCache extends AbstractFileCache {

    private long mMaxSize;

    @Override
    protected boolean needFree() {
        return mSize > mMaxSize;
    }

    public LimitSizeFileCache(Context context, int storageDevice, String cacheName, long maxSize) {
        super(context, storageDevice, cacheName);
        mMaxSize = maxSize;
    }
}
