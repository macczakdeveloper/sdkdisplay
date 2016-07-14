package io.display.sdk;

import java.util.ArrayList;

/**
 * Created by gidi on 15/03/16.
 */
class ErrorLogger {
    ArrayList<String> entryList = new ArrayList<>();
    String appId;
    public ErrorLogger(String appId) {
        this.appId = appId;
    }
    public void log(String msg) {
        entryList.add(msg);
    }
    public void flush() {

    }
}
