package org.qinyu.utils;

import android.content.Context;
import android.view.View;

public class Utils {

    public static void runOnUIThread(final View view, Runnable r) {
        if (view != null) {
            if (isOnUiThread(view.getContext())) {
               r.run();
            } else {
                view.post(r);
            }
        }
    }

    public static boolean isOnUiThread(Context context) {
        return Thread.currentThread() == context.getMainLooper().getThread();
    }

}
