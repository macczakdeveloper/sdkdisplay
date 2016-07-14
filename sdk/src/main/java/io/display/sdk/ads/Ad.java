package io.display.sdk.ads;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import io.display.sdk.Controller;
import io.display.sdk.DioActivity;
import io.display.sdk.DioGenericActivity;
import io.display.sdk.DioSdkException;

public abstract class Ad {
    protected  JSONObject data;
    protected String id;
    String placement;

    protected DioGenericActivity activity;
    protected HashMap<String, Bitmap> imgBitmaps = new HashMap<String, Bitmap> ();

    protected HashMap<String, BitmapLoadListener> imgBitmapListeners = new HashMap<String, BitmapLoadListener> ();


    abstract public void preload(AdPreloadListener listener) throws DioSdkException;

    public Ad(String id, JSONObject data) {
        this.data = data;
        this.id = id;
    }



    public static Ad factory(String id,JSONObject data) {
        Ad ad = null;
        try {
            switch (data.getString("type")) {
                case "interstitial":
                    switch (data.getString("subtype")) {
                        case "video":
                            ad = new InterstitialVideo(id, data.getJSONObject("data"));
                            break;
                        case "banner":
                            ad = new Interstitial(id, data.getJSONObject("data"));
                            break;
                    }

                    break;
            }
        } catch (JSONException e) {
            return null;

        }
        return ad;
    }
    abstract public void render(DioGenericActivity activity);

    public String getId() {
        return id;
    }
    public String getActivityType() {
        return "normal";
    }
    public void setPlacementId(String id) {
        placement = id;
    }
    protected void redirect() {
        try {
            Controller.getInstance().triggerPlacementAction("onAdClick", placement);
            String url = data.getString("clk");
            Intent intent = new Intent(activity, DioActivity.class);
            intent.putExtra("clk", url);
            intent.putExtra("cmd", "redirect");
            activity.startActivity(intent);
            activity.finish();
        } catch (Exception e) {
            activity.finish();
        }
    }
    protected void loadBitmap(String ns,String url, BitmapLoadListener listener) {
        imgBitmapListeners.put(ns, listener);
        PreloadImageTask task = new PreloadImageTask();
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
             task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ns, url);
        } else {
             task.execute(ns, url);
        }
    }

    protected Bitmap getBitmap(String ns) {
        Bitmap bitmap = null;
        if(imgBitmaps.containsKey(ns)) {
            bitmap = imgBitmaps.get(ns);
        }
        return bitmap;
    }
    protected void callImpBecon()  throws JSONException {
        String url  = data.getString("imp");
        CallImpEndpointTask impTask = new CallImpEndpointTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            impTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
        } else {
            impTask.execute(url);
        }
    }



    protected  class PreloadImageTask extends AsyncTask<String, String, NsBitmap> {
        @Override
        protected NsBitmap doInBackground(String[] params) {
            NsBitmap nsBm = new NsBitmap();
            try {
                nsBm.ns = params[0];
                String url = params[1];
                nsBm.bitmap = getImageBitmap(url);
            } catch (Exception e) {

            }
            return nsBm;
        }
        @Override
        protected void onPostExecute (NsBitmap nsBitmap){
            imgBitmaps.put(nsBitmap.ns, nsBitmap.bitmap);

            if (imgBitmapListeners.containsKey(nsBitmap.ns)) {
                if (nsBitmap.bitmap == null) {
                    imgBitmapListeners.get(nsBitmap.ns).onError();
                } else {
                    imgBitmapListeners.get(nsBitmap.ns).onSuccess();
                }
            }
        }
        private Bitmap getImageBitmap(String url) throws IOException {
            Bitmap bm = null;
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
            return bm;
        }

    }
    class NsBitmap {
        public String ns;
        public Bitmap bitmap;
    }
    public abstract class BitmapLoadListener {
        public abstract void onSuccess();
        public abstract void onError();
    }


    protected class CallImpEndpointTask extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground (String[] url) {
            try {
                URL aURL = new URL(url[0]);
                URLConnection conn = aURL.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }
}
