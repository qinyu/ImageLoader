package org.qinyu.imageloader;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.qinyu.utils.Utils;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.Monitor;

/**
 * The Class ImageViewManager.
 */
public class ImageViewManager {

    /** The Constant SUBMIT. */
    private static final int SUBMIT = 1;

    /** The m callback. */
    private Callback<ImageViewManager.ViewHolder> mCallback = new Callback<ImageViewManager.ViewHolder>() {

        @Override
        public void onStart(final ViewHolder t, final ImageSpec spec) {
            final ImageView iv = t.mImageViewWeakRef.get();
            if (iv != null) {
                showLoading(iv, t.mDefaultImageRes, t.mProgressBarWeakRef.get(), spec);
            }
        }

        @Override
        public void onCancel(final ViewHolder t, final ImageSpec spec) {
            final ImageView iv = t.mImageViewWeakRef.get();
            if (iv != null) {
                showDefault(iv, t.mProgressBarWeakRef.get(), t.mDefaultImageRes, spec);
            }
        }

        @Override
        public void onSuccess(final ViewHolder t, final ImageSpec spec, final Bitmap bitmap) {
            final ImageView iv = t.mImageViewWeakRef.get();
            if (iv != null) {
                showImage(iv, t.mProgressBarWeakRef.get(), bitmap, spec);
            }
        }

        @Override
        public void onFail(final ViewHolder t, final ImageSpec spec, Exception e) {
            final ImageView iv = t.mImageViewWeakRef.get();
            if (iv != null) {
                showDefault(iv, t.mProgressBarWeakRef.get(), t.mDefaultImageRes, spec);
            }
        }

    };

    /**
     * Instantiates a new image view manager.
     */
    private ImageViewManager() {
        mImageViewMap = Collections.synchronizedMap(new WeakHashMap<ImageView, ImageSpec>());
        mMapMonitor = new Monitor();
    }

    /** The s info cache. */
    private Map<ImageView, ImageSpec> mImageViewMap;

    /** The m looper. */
    private Looper mLooper;

    /** The m handler. */
    private SubmitHandler mHandler;

    /** The m map monitor. */
    private Monitor mMapMonitor;

    /**
     * Checks if is image view waiting.
     * 
     * @param iv
     *            the iv
     * @param spec
     *            the info
     * @return true, if is image view waiting
     */
    private boolean isImageViewWaiting(ImageView iv, ImageSpec spec) {
        if (iv != null && spec != null) {
            mMapMonitor.enter();
            try {
                return mImageViewMap.containsKey(iv) && spec.equals(mImageViewMap.get(iv));
            } finally {
                mMapMonitor.leave();
            }
        }
        return false;
    }

    /**
     * Show default.
     * 
     * @param iv
     *            the iv
     * @param view
     *            the view
     * @param defaultRes
     *            the default res
     * @param spec
     *            the spec
     */
    protected void showDefault(final ImageView iv, final ProgressBar view, final int defaultRes, final ImageSpec spec) {
        Utils.runOnUIThread(iv, new Runnable() {
            @Override
            public void run() {
                mMapMonitor.enter();
                boolean imageViewWaiting = false;
                try {
                    imageViewWaiting = isImageViewWaiting(iv, spec);
                    mImageViewMap.remove(iv);
                } finally {
                    mMapMonitor.leave();
                }
                if (imageViewWaiting) {
                    if (defaultRes > 0) {
                        iv.setImageResource(defaultRes);
                    }
                    if (view != null) {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    /**
     * Display thumbnail.
     * 
     * @param progress
     *            the progress
     * @param iv
     *            the iv
     * @param spec
     *            the spec
     * @param defaultRes
     *            the default res
     */
    public void display(ProgressBar progress, ImageView iv, ImageSpec spec, int defaultRes) {
        if (iv == null || spec == null) {
            return;
        }

        boolean imageViewWaiting = true;
        mMapMonitor.enter();
        try {
            imageViewWaiting = isImageViewWaiting(iv, spec);
            if (!imageViewWaiting) {
                cancel(iv);
                mImageViewMap.put(iv, spec);
            }
        } finally {
            mMapMonitor.leave();
        }

        if (!imageViewWaiting) {
            final Bitmap bitmap = ImageLoader.getInstance(iv.getContext()).findInCache(spec);
            if (bitmap != null) {
                showImage(iv, progress, bitmap, spec);
                return;
            }
            showLoading(iv, defaultRes, progress, spec);
            getSubmitHandler().obtainMessage(SUBMIT, new SubmitInfo(new ViewHolder(iv, progress, defaultRes), spec))
                    .sendToTarget();
        }
    }

    /**
     * Cancel.
     * 
     * @param iv
     *            the iv
     */
    public void cancel(ImageView iv) {
        if (iv != null) {
            ImageSpec spec = null;
            boolean isIn = false;
            mMapMonitor.enter();
            try {
                spec = mImageViewMap.remove(iv);
                isIn = mImageViewMap.containsValue(spec);
            } finally {
                mMapMonitor.leave();
            }
            if (spec != null && !isIn) {
                ImageLoader.getInstance(iv.getContext()).cancel(spec);
            }
        }
    }

    /**
     * Gets the handler.
     * 
     * @return the handler
     */
    private SubmitHandler getSubmitHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread("ThumbHandlerThread", THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mLooper = thread.getLooper();
            mHandler = new SubmitHandler(mLooper);
        }
        return mHandler;
    }

    /**
     * Cancel all.
     */
    private void cancelAll() {
        if (mHandler != null) {
            mHandler.removeMessages(SUBMIT);
        }

        mMapMonitor.enter();
        try {
            Set<Entry<ImageView, ImageSpec>> entrySet = mImageViewMap.entrySet();
            for (Iterator<Entry<ImageView, ImageSpec>> iterator = entrySet.iterator(); iterator.hasNext();) {
                Entry<ImageView, ImageSpec> next = iterator.next();
                ImageLoader.getInstance(next.getKey().getContext()).cancel(next.getValue());
            }
            mImageViewMap.clear();
        } finally {
            mMapMonitor.leave();
        }
    }

    /**
     * Close.
     */
    public void close() {
        if (mHandler != null) {
            mHandler.removeMessages(SUBMIT);
        }
        if (mLooper != null) {
            mLooper.quit();
            mLooper = null;
            mHandler = null;
        }
        cancelAll();
    }

    /**
     * Show image.
     * 
     * @param iv
     *            the iv
     * @param view
     *            the view
     * @param bitmap
     *            the bitmap
     * @param spec
     *            the spec
     */
    protected void showImage(final ImageView iv, final ProgressBar view, final Bitmap bitmap, final ImageSpec spec) {
        Utils.runOnUIThread(iv, new Runnable() {
            @Override
            public void run() {
                if (isImageViewWaiting(iv, spec)) {
                    iv.setImageBitmap(bitmap);
                    if (view != null) {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    /**
     * Show loading.
     * 
     * @param iv
     *            the iv
     * @param defaultRes
     *            the default res
     * @param view
     *            the view
     * @param spec
     *            the spec
     */
    protected void showLoading(final ImageView iv, final int defaultRes, final ProgressBar view, final ImageSpec spec) {
        Utils.runOnUIThread(iv, new Runnable() {
            @Override
            public void run() {
                if (isImageViewWaiting(iv, spec)) {
                    if (defaultRes > 0) {
                        iv.setImageResource(defaultRes);
                    }
                    if (view != null) {
                        view.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The iv ref. */
        private WeakReference<ImageView> mImageViewWeakRef;

        /** The progress ref. */
        private WeakReference<ProgressBar> mProgressBarWeakRef;

        /** The default res. */
        private int mDefaultImageRes;

        /**
         * Instantiates a new view holder.
         * 
         * @param imageView
         *            the iv
         * @param progressBar
         *            the progress
         * @param defaultImageRes
         *            the default res
         */
        private ViewHolder(ImageView imageView, ProgressBar progressBar, int defaultImageRes) {
            mImageViewWeakRef = new WeakReference<ImageView>(imageView);
            mProgressBarWeakRef = new WeakReference<ProgressBar>(progressBar);
            mDefaultImageRes = defaultImageRes;
        }

    }

    /**
     * The Class ThumbHandler.
     */
    private final class SubmitHandler extends Handler {

        /**
         * Instantiates a new submit handler.
         * 
         * @param looper
         *            the looper
         */
        private SubmitHandler(Looper looper) {
            super(looper);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case SUBMIT:
                SubmitInfo info = (SubmitInfo) msg.obj;
                ImageView imageView = ((ViewHolder) info.mViewHolder).mImageViewWeakRef.get();
                if (imageView != null) {
                    if (isImageViewWaiting(imageView, info.mImageSpec)) {
                        ImageLoader.getInstance(imageView.getContext()).submit(info.mImageSpec, info.mViewHolder, mCallback);
                    }
                }
                break;

            default:
                super.handleMessage(msg);
            }

        }
    }

    /**
     * The Class SubmitInfo.
     */
    private static class SubmitInfo {

        /** The token. */
        private ViewHolder mViewHolder;

        /** The spec. */
        private ImageSpec mImageSpec;

        /**
         * Instantiates a new submit info.
         * 
         * @param viewHolder
         *            the token
         * @param imageSpec
         *            the spec
         */
        private SubmitInfo(ViewHolder viewHolder, ImageSpec imageSpec) {
            mViewHolder = viewHolder;
            mImageSpec = imageSpec;
        }
    }

    /** The singleton instance. */
    private static ImageViewManager sInstance;

    /**
     * Gets the single instance of ImageViewManager.
     * 
     * @return single instance of ImageViewManager
     */
    public static ImageViewManager getInstance() {
        if (sInstance == null) {
            sInstance = new ImageViewManager();
        }
        return sInstance;
    }

}
