package org.qinyu.imageloader;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.provider.MediaStore.Images.Thumbnails.getThumbnail;
import static java.lang.Long.parseLong;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.qinyu.cache.AbstractFileCache;
import org.qinyu.cache.LimitSizeFileCache;
import org.qinyu.utils.BitmapHelper;
import org.qinyu.utils.HttpParamsHelper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

/**
 * The Class ImageLoader.
 */
public class ImageLoader {
    /** The m future cache. */
    private Map<String, CallbackDelegator<?>> mDelegatorCache;

    /**
     * Instantiates a new image loader.
     * 
     * @param context
     *            the context
     */
    private ImageLoader(Context context) {
        mDelegatorCache = Collections.synchronizedMap(new HashMap<String, CallbackDelegator<?>>());
        mApp = context.getApplicationContext();
        mFileCache = new LimitSizeFileCache(mApp, 1, "image", 1024 * 1024 * 4);
        mBitmapCache = new LruCache<String, Bitmap>(100);
    }

    /**
     * Cancel.
     * 
     * @param spec
     *            the spec
     */
    public void cancel(ImageSpec spec) {
        if (spec != null) {
            CallbackDelegator<?> remove = mDelegatorCache.remove(spec.getCacheFileName());
            if (remove != null) {
                Log.d(TAG, " found and cancel:" + remove.hashCode() + ":" + spec);
                remove.f.cancel(false);
                remove.cancel();
            }
        }
    }

    /**
     * The Class ThumbDisplayInfo.
     * 
     * @param <T>
     *            the generic type
     */
    private static class CallbackDelegator<T> {

        /** The token. */
        T token;

        /** The callback. */
        Callback<T> callback;

        /** The spec. */
        ImageSpec spec;

        /** The b. */
        Bitmap b;

        /** The f. */
        Future<?> f;

        /**
         * Instantiates a new callback delegator.
         * 
         * @param token
         *            the token
         * @param spec
         *            the spec
         * @param callback
         *            the callback
         * @param f
         *            the f
         */
        private CallbackDelegator(T token, ImageSpec spec, Callback<T> callback, Future<?> f) {
            this.token = token;
            this.spec = spec;
            this.callback = callback;
            this.f = f;
        }

        /**
         * Display.
         */
        private void display() {
            callback.onSuccess(token, spec, b);
        }

        /**
         * Cancel.
         */
        private void cancel() {
            callback.onCancel(token, spec);
        }

        /**
         * Cancel.
         */
        private void start() {
            callback.onStart(token, spec);
        }

        /**
         * Fail.
         * 
         * @param e
         *            the e
         */
        private void fail(Exception e) {
            callback.onFail(token, spec, e);
        }
    }

    /**
     * Submit.
     * 
     * @param <T>
     *            the generic type
     * @param spec
     *            the spec
     * @param token
     *            the token
     * @param callback
     *            the instance
     */
    public <T> void submit(ImageSpec spec, T token, Callback<T> callback) {
        final ListenableFuture<Bitmap> task = getTask(spec);
        if (task != null && !task.isCancelled()) {
            CallbackDelegator<T> info = new CallbackDelegator<T>(token, spec, callback, task);
            Runnable action = getDelegateAction(info, task);
            if (task.isDone()) {
                action.run();
            } else {
                info.start();
                mDelegatorCache.put(spec.getCacheFileName(), info);
                getExecutors().execute(action);
            }
        }
    }

    /**
     * Gets the delegate action.
     * 
     * @param <T>
     *            the generic type
     * @param info
     *            the info
     * @param task
     *            the task
     * @return the delegate action
     */
    private <T> Runnable getDelegateAction(final CallbackDelegator<T> info, final ListenableFuture<Bitmap> task) {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = task.get();
                    Log.d(TAG, "display:" + info.spec.toString());
                    if (bitmap != null) {
                        info.b = bitmap;
                        info.display();
                        Log.d(TAG, "display:" + info.spec.toString());
                    } else {
                        info.fail(new Exception("Can not decode bitmap"));
                    }
                } catch (InterruptedException e) {
                } catch (CancellationException e) {
                } catch (ExecutionException e) {
                    info.fail(e);
                } finally {
                    mDelegatorCache.remove(info.spec.getCacheFileName());
                }
            }
        };
        return command;
    }

    /** The singleton instance. */
    private static ImageLoader sInstance;

    /**
     * Gets the single instance of ThumbnailLoader.
     * 
     * @param context
     *            the context
     * @return single instance of ThumbnailLoader
     */
    public static ImageLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoader(context.getApplicationContext());
        }
        return sInstance;
    }

    /** Retry times for download file. */
    private static final int NUM_ATTEMPTS = 3;

    private static final String TAG = null;

    /** The m app. */
    private Context mApp;

    /** The bitmap cache. */
    private LruCache<String, Bitmap> mBitmapCache;

    /** The m file cache. */
    private AbstractFileCache mFileCache;

    /**
     * Clear bitmap cache.
     */
    public void clearMemCache() {
        mBitmapCache.evictAll();
    }

    /**
     * Close.
     */
    public void close() {
        if (mExecutors != null) {
            mExecutors.shutdownNow();
            mExecutors = null;
        }
        synchronized (mDelegatorCache) {
            Collection<CallbackDelegator<?>> values = mDelegatorCache.values();
            for (Iterator<CallbackDelegator<?>> iterator = values.iterator(); iterator.hasNext();) {
                ((CallbackDelegator<?>) iterator.next()).f.cancel(true);
            }
            mDelegatorCache.clear();
        }
        // clearBitmapCache();
    }

    /**
     * Submit cache file task.
     * 
     * @param spec
     *            the spec
     * @return the listenable future
     */
    private ListenableFuture<String> getFetchTask(final ImageSpec spec) {
        return getExecutors().submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    return fetch(spec);
                } catch (Exception e) {
                    throw e;
                }
            }
        });
    }

    /**
     * Submit task.
     * 
     * @param spec
     *            the spec
     * @return the listenable future
     */
    private ListenableFuture<Bitmap> getTask(final ImageSpec spec) {
        Bitmap bitmap = mBitmapCache.get(spec.getCacheFileName());
        if (bitmap != null) {
            SettableFuture<Bitmap> create = SettableFuture.<Bitmap> create();
            create.set(bitmap);
            return create;
        } else {
            return Futures.transform(getFetchTask(spec), new Function<String, Bitmap>() {
                @Override
                public Bitmap apply(String input) {
                    return decode(spec, input);
                }
            });
        }
    }

    /**
     * Find in cache.
     * 
     * @param spec
     *            the spec
     * @return the bitmap
     */
    public Bitmap findInCache(ImageSpec spec) {
        return mBitmapCache.get(spec.getCacheFileName());
    }

    /**
     * Download.
     * 
     * @param spec
     *            the spec
     * @return the string
     */
    private String fetch(ImageSpec spec) {
        Log.d(TAG, "download:" + spec.toString());

        // check file in cache
        File file = mFileCache.get(spec.getCacheFileName());
        if (file != null && file.exists()) {
            return file.getAbsolutePath();
        }

        // check file is local
        final String url = spec.getUrl();
        if (url.startsWith("/") && new File(url).exists()) {
            return url;
        } else {
            String scheme = Uri.parse(url).getScheme();
            if (SCHEME_FILE.equals(scheme) || SCHEME_CONTENT.equals(scheme)) {
                return url;
            }
        }

        // download file and save in cache
        int timesTried = 1;
        String filePath = null;
        while (timesTried <= NUM_ATTEMPTS) {
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient(HttpParamsHelper.setTimeout(new BasicHttpParams()));
                HttpResponse response = httpClient.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 200) {
                    filePath = mFileCache.put(spec.getCacheFileName(), response.getEntity().getContent());
                    break;
                } else {
                    filePath = null;
                    Log.w(TAG,
                            "download:error:response:" + statusLine.getStatusCode() + " "
                                    + statusLine.getReasonPhrase());
                    break;
                }

            } catch (IOException e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    return null;
                }
                timesTried++;
                if (timesTried > NUM_ATTEMPTS) {
                    break;
                }
            }
        }
        Log.d(TAG, "download:" + spec.toString() + ";file:" + filePath);
        return filePath;
    }

    /**
     * Decode.
     * 
     * @param spec
     *            the spec
     * @param uri
     *            the uri
     * @return the bitmap
     */
    private Bitmap decode(ImageSpec spec, String uri) {
        Log.d(TAG, "decode1:" + spec.toString() + "uri:" + uri);
        Bitmap bitmap = null;
        if (spec == null || uri == null)
            return null;
        int rotation = spec.orientation;
        if (uri.startsWith("/")) {
            bitmap = BitmapHelper.decodeFile(uri, spec.width, spec.height);
        } else {
            Uri parse = Uri.parse(uri);
            String scheme = parse.getScheme();
            if (SCHEME_FILE.equals(scheme)) {
                bitmap = BitmapHelper.decodeFile(parse.getPath(), spec.width, spec.height);
            } else if (SCHEME_CONTENT.equals(scheme)) {
                ContentResolver contentResolver = mApp.getContentResolver();
                String type = contentResolver.getType(parse);
                if (!TextUtils.isEmpty(type) && type.startsWith("image/")) {
                    if (USE_FAST_THUMB && spec instanceof LocalThumbSpec) {
                        bitmap = getThumbnail(contentResolver, parseLong(parse.getLastPathSegment()),
                                ((LocalThumbSpec) spec).type, null);
                    } else {
                        bitmap = decodeFromMediaStore(spec, uri);
                    }
                    Cursor query = contentResolver.query(parse, new String[] { Media.ORIENTATION }, null, null, null);
                    if (query != null) {
                        if (query.moveToFirst()) {
                            rotation = query.getInt(query.getColumnIndex(Media.ORIENTATION));
                        }
                        query.close();
                    }
                }
            }
        }

        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > 0 && height > 0) {
                float scale = ((float) spec.width) / width;

                if (scale < 0.5 || rotation != 0) {
                    Matrix scaleMatrix = new Matrix();
                    scaleMatrix.setScale(scale, scale);
                    scaleMatrix.setRotate(rotation);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, scaleMatrix, false);
                }
            }
        }

        if (bitmap != null) {
            mBitmapCache.put(spec.getCacheFileName(), bitmap);
        }
        Log.d(TAG, "decode2:" + spec.toString() + ",thumb:" + bitmap.toString());

        return bitmap;
    }

    /**
     * Gets the image media thumb.
     * 
     * @param spec
     *            the spec
     * @param uri
     *            the uri
     * @return the image media thumb
     */
    private Bitmap decodeFromMediaStore(final ImageSpec spec, String uri) {
        Bitmap bitmap = null;
        Cursor c = mApp.getContentResolver().query(Uri.parse(uri), null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                String filePath = c.getString(c.getColumnIndex(Media.DATA));
                bitmap = BitmapHelper.decodeFile(filePath, spec.width, spec.height);
            }
            c.close();
        }
        return bitmap;
    }

    /**
     * Compress.
     * 
     * @param spec
     *            the spec
     * @param bitmap
     *            the bitmap
     */
    @SuppressWarnings("unused")
    private void compress(final ImageSpec spec, Bitmap bitmap) {
        if (bitmap != null && spec != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 100, stream);
            mFileCache.put(spec.getCacheFileName(), new ByteArrayInputStream(stream.toByteArray()));
        }
    }

    /** The Constant CORE_POOL_SIZE. */
    private static final int CORE_POOL_SIZE = 4;

    /** The Constant MAXIMUM_POOL_SIZE. */
    private static final int MAXIMUM_POOL_SIZE = 8;

    /** The Constant KEEP_ALIVE. */
    private static final int KEEP_ALIVE = 1;

    /** The Constant sThreadFactory. */
    private final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "ThumbThread#" + mCount.getAndIncrement());
            thread.setPriority(THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };

    /** The m excutors. */
    private ListeningExecutorService mExecutors;

    /**
     * Gets the executors.
     * 
     * @return the executors
     */
    private ListeningExecutorService getExecutors() {
        if (mExecutors == null) {
            mExecutors = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                    KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), mThreadFactory));
        }
        return mExecutors;
    }

    /** The Constant USE_FAST_THUMB. */
    private static final boolean USE_FAST_THUMB = true;
}
