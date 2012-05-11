package org.qinyu.cache;


import android.content.Context;

public class LimitCountFileCache extends AbstractFileCache {

    private long mMaxCount;

    @Override
    protected boolean needFree() {
        return mCount > mMaxCount;
    }

    public LimitCountFileCache(Context context, int storageDevice, String cacheName, int maxCount) {
        super(context, storageDevice, cacheName);
        mMaxCount = maxCount;
    }
}
