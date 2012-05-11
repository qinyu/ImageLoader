package org.qinyu.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractFileCache.
 */
public abstract class AbstractFileCache implements Comparator<File> {

    /** The Constant DISK_CACHE_SDCARD. */
    protected static final int DISK_CACHE_SDCARD = 0;

    /** The Constant TAG. */
    protected static final String TAG = LimitSizeFileCache.class.getSimpleName();

    /** The m directory. */
    private String mDirectory;

    /** The m lock. */
    protected ReentrantLock mLock;

    /** The m buf. */
    private byte[] mBuf;

    /** The m size. */
    protected long mSize;

    /** The m count. */
    protected int mCount;

    /**
     * Instantiates a new abstract file cache.
     * 
     * @param context
     *            the context
     * @param storageDevice
     *            the storage device
     * @param cacheName
     *            the cache name
     */
    protected AbstractFileCache(Context context, int storageDevice, String cacheName) {
        Context appContext = context.getApplicationContext();

        mLock = new ReentrantLock();
        if (storageDevice == DISK_CACHE_SDCARD
                && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // SD-card available
            setDirectory(appContext.getExternalCacheDir().getAbsolutePath() + "/" + cacheName);
        } else {
            setDirectory(appContext.getCacheDir().getAbsolutePath() + "/" + cacheName);
        }

        if (assureDirectoryExist(getDirectory())) {
            File[] cachedFiles = new File(getDirectory()).listFiles();
            if (cachedFiles == null) {
                return;
            }

            for (int i = 0; i < cachedFiles.length; i++) {
                mSize += cachedFiles[i].length();
            }
            mCount += cachedFiles.length;
        }

    }

    private static boolean assureDirectoryExist(String dir) {
        File outFile = new File(dir);
        if (outFile.exists() || outFile.mkdirs()) {
            File nomedia = new File(dir, ".nomedia");
            if (!nomedia.exists()) {
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "Failed creating .nomedia file");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Put.
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @return the string
     */
    public String put(String key, InputStream value) {
        mLock.lock();
        try {
            free();
            return saveCacheFile(Integer.toHexString(key.hashCode()), value);
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Gets the.
     * 
     * @param key
     *            the key
     * @return the file
     */
    public File get(String key) {
        mLock.lock();
        try {
            File f = new File(getDirectory(), Integer.toHexString(key.hashCode()));
            if (f.exists() && f.isFile()) {
                f.setLastModified(System.currentTimeMillis());
                return f;
            } else {
                return null;
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Sort files.
     * 
     * @return the list
     */
    protected List<File> sortFiles() {
        File[] cachedFiles = new File(getDirectory()).listFiles();
        if (cachedFiles == null) {
            return null;
        }
        List<File> asList = Arrays.asList(cachedFiles);
        Collections.sort(asList, this);
        return asList;
    }

    /**
     * Save cache file.
     * 
     * @param fileName
     *            the file path
     * @param in
     *            the in
     * @return the string
     */
    private String saveCacheFile(String fileName, InputStream in) {
        mLock.lock();
        try {
            if (assureDirectoryExist(getDirectory())) {
                File f = new File(getDirectory(), fileName);
                try {
                    f.createNewFile();
                    FileOutputStream out = new FileOutputStream(f);
                    int count = 0;
                    byte[] buf = getBuf();
                    do {
                        count = in.read(buf);
                        if (count > 0) {
                            out.write(buf, 0, count);
                        }
                    } while (count > 0);
                    out.close();
                    mSize += f.length();
                    mCount++;
                    return f.getAbsolutePath();
                } catch (IOException e) {
                    Log.w(TAG, "Can not save to cache!", e);
                }
                f.delete();
            }
            return null;
        } finally {
            mLock.unlock();
        }
    }

    private byte[] getBuf() {
        mLock.lock();
        try {
            if (mBuf == null) {
                mBuf = new byte[4096];
            }
            return mBuf;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Free.
     */
    protected void free() {
        mLock.lock();
        try {
            if (needFree()) {
                List<File> asList = sortFiles();
                if (asList != null) {
                    for (int i = 0; i < asList.size() && needFree(); i++) {
                        File f = asList.get(i);
                        if (f.length() > 0) {
                            f.delete();
                            mSize -= f.length();
                            mCount--;
                        }
                    }
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    public void clear() {
        mLock.lock();
        try {
            File[] cachedFiles = new File(getDirectory()).listFiles();
            if (cachedFiles != null) {
                for (int i = 0; i < cachedFiles.length; i++) {
                    File f = cachedFiles[i];
                    if (f.exists()) {
                        f.delete();
                    }
                }
            }
            mSize = 0;
            mCount = 0;
        } finally {
            mLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(File file1, File file2) {
        long lastModified = file1.lastModified();
        long lastModified2 = file2.lastModified();
        if (lastModified == lastModified2) {
            return 0;
        }
        return lastModified < lastModified2 ? -1 : 1;
    }

    /**
     * Need free.
     * 
     * @return true, if successful
     */
    protected abstract boolean needFree();

    protected String getDirectory() {
        return mDirectory;
    }

    protected void setDirectory(String mDirectory) {
        this.mDirectory = mDirectory;
    }

}