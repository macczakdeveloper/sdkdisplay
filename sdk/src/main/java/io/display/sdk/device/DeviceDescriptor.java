package io.display.sdk.device;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class DeviceDescriptor {
    public String googleAid;
    HashMap props;
    public DeviceDescriptor(Context context, final DeviceEventsListener listener) {
        props = new HashMap();
        props.put("model", Build.MODEL);
        props.put("make", Build.MANUFACTURER);
        props.put("os", "android");
        props.put("osVer", Build.VERSION.RELEASE);
       int apiLevel = Build.VERSION.SDK_INT;
        if(apiLevel >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getDeviceResolution1(context);
        } else {
            getDeviceResolution2(context);
        }
        AdvertisingIdFetcher fetcher = new AdvertisingIdFetcher() {
            @Override
            public void onPostExecute(String id) {
                googleAid = id;
                listener.onDeviceIdRetrieved();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            fetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context);
        } else {
            fetcher.execute(context);
        }


    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void getDeviceResolution1(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        props.put("w", size.x);
        props.put("h" , size.y);
    }
    private void getDeviceResolution2(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int apiLevel = Build.VERSION.SDK_INT;
        Point size = new Point();

        props.put("w", display.getWidth());
        props.put("h" ,display.getHeight());
    }
    public HashMap getProps() {
        return props;
    }

    class AdvertisingIdFetcher extends AsyncTask<Context, String, String> {
        @Override
        protected String doInBackground(Context[] context) {
            String id = "";
            try {
                AdvertisingIdClient.AdInfo adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context[0]);
                id = adInfo.getId();
            } catch (Exception e) {
                Log.i("io.display.sdk", "couldn't get advertising ID");

            }
            return id;
        }


    }


}

