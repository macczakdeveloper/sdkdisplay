package io.display.sdk;

/**
 * Created by gidi on 07/03/16.
 */
public class DioSdkException extends Exception {
    public DioSdkException(String s) {
        super(s);
        Controller.getInstance().logError(s);
    }

    public DioSdkException(Throwable e) {
        super(e);
        Controller.getInstance().logError(e.getMessage());

    }
    public DioSdkException(String s, Throwable e) {

        super(s, e);
    }
}
