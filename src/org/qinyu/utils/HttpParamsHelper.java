package org.qinyu.utils;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpParamsHelper {

    public static final int CONNECTION_TIMEOUT = 15000;
    public static final int SOCKET_TIMEOUT = 15000;

    public static HttpParams setTimeout(HttpParams httpParameters) {
        // Set the timeout in milliseconds until a connection is
        // established.
        // The default value is zero, that means the timeout is not
        // used.
        HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIMEOUT);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);
        return httpParameters;
    }

}
