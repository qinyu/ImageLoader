package org.qinyu.utils;

import org.qinyu.imageloader.BuildConfig;

import android.content.Context;
import android.util.Log;

/**
 * The Class Debug.
 * 
 * 1.Provide functions same as {@link Log} but check whether the build is
 * debugable before .</br>
 * 
 * 2.Provide functions with default tag(simple class name): </br>
 * {@link #d(String)}, {@link #d(String, Throwable)}, {@link #e(String)},
 * {@link #e(String, Throwable)}, {@link #i(String)},
 * {@link #i(String, Throwable)}, {@link #println(int, String)},
 * {@link #v(String)}, {@link #v(String, Throwable)}, {@link #w(String)},
 * {@link #wMsg(String, Throwable)}, {@link #wtf(String)},
 * {@link #wtfMsg(String, Throwable)}
 * 
 * {@link #init(Context)} must be called when application starts.
 * 
 * @author qinyu
 */
public class Debug {

    /**
     * @see Log#d(String, String)
     */
    public static int d(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.d(tag, msg) : 0;
    }

    /**
     * @see Log#d(String, String)
     */
    public static int d(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.d(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#e(String, String)
     */
    public static int e(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.e(tag, msg) : 0;
    }

    /**
     * @see Log#e(String, String)
     */
    public static int e(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.e(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#i(String, String)
     */
    public static int i(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.i(tag, msg) : 0;
    }

    /**
     * @see Log#i(String, String)
     */
    public static int i(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.i(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#v(String, String)
     */
    public static int v(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.v(tag, msg) : 0;
    }

    /**
     * @see Log#v(String, String)
     */
    public static int v(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.v(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#w(String, String)
     */
    public static int w(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.w(tag, msg) : 0;
    }

    /**
     * @see Log#w(String, String)
     */
    public static int w(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.w(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#wtf(String, String)
     */
    public static int wtf(String tag, String msg) {
        return BuildConfig.DEBUG ? Log.wtf(tag, msg) : 0;
    }

    /**
     * @see Log#wtf(String, String)
     */
    public static int wtf(String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.wtf(wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    /**
     * @see Log#d(String, String, Throwable)
     */
    public static int d(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.d(tag, msg, tr) : 0;
    }

    /**
     * @see Log#d(String, String, Throwable)
     */
    public static int d(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.d(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#e(String, String, Throwable)
     */
    public static int e(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.e(tag, msg, tr) : 0;
    }

    /**
     * @see Log#e(String, String, Throwable)
     */
    public static int e(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.e(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#i(String, String, Throwable)
     */
    public static int i(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.i(tag, msg, tr) : 0;
    }

    /**
     * @see Log#i(String, String, Throwable)
     */
    public static int i(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.i(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#v(String, String, Throwable)
     */
    public static int v(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.d(tag, msg, tr) : 0;
    }

    /**
     * @see Log#v(String, String, Throwable)
     */
    public static int v(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.v(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#w(String, String, Throwable)
     */
    public static int w(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.w(tag, msg, tr) : 0;
    }

    /**
     * @see Log#w(String, String, Throwable)
     */
    public static int wMsg(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.w(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#wtf(String, String, Throwable)
     */
    public static int wtf(String tag, String msg, Throwable tr) {
        return BuildConfig.DEBUG ? Log.wtf(tag, msg, tr) : 0;
    }

    /**
     * @see Log#wtf(String, String, Throwable)
     */
    public static int wtfMsg(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.wtf(wrapInfo[0], wrapInfo[1], tr);
        }
        return 0;
    }

    /**
     * @see Log#w(String, Throwable)
     */
    public static int w(String tag, Throwable tr) {
        return BuildConfig.DEBUG ? Log.w(tag, tr) : 0;
    }

    /**
     * @see Log#wtf(String, Throwable)
     */
    public static int wtf(String tag, Throwable tr) {
        return BuildConfig.DEBUG ? Log.wtf(tag, tr) : 0;
    }

    /**
     * @see Log#println(int, String, String)
     */
    public static int println(int priority, String tag, String msg) {
        return BuildConfig.DEBUG ? Log.println(priority, tag, msg) : 0;
    }

    /**
     * @see Log#println(int, String, String)
     */
    public static int println(int priority, String msg) {
        if (BuildConfig.DEBUG) {
            String[] wrapInfo = wrapInfo(msg);
            return Log.println(priority, wrapInfo[0], wrapInfo[1]);
        }
        return 0;
    }

    private static String[] wrapInfo(String msg) {
        Throwable t = new Throwable();
        StackTraceElement e = t.getStackTrace()[2];
        return new String[] { e.getClassName(), "[" + e.getMethodName() + ":" + e.getLineNumber() + "]" + msg };
    }

}
