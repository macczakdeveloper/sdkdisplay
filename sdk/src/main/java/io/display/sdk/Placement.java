package io.display.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.display.sdk.ads.Ad;
import io.display.sdk.ads.AdPreloadListener;

/**
 * Created by gidi on 07/03/16.
 */
public class Placement {
    public String id;
    public String status;
    public JSONObject data;
    public int viewsLeft;
    public boolean capViews = false;

    private LinkedHashMap<String, Ad> ads = new LinkedHashMap<String, Ad>();
    private LinkedHashMap<String, Ad> lastAdStack = new LinkedHashMap<String, Ad>();
    private ArrayList<String> queue = new ArrayList<String>();

    public Placement(String id) throws DioSdkException{
        this.id = id;

    }
    public void setup( JSONObject d) throws DioSdkException {
        data = d;
        try {
            status = data.getString("status");
            if(data.has("viewsLeft")) {
                capViews = true;
                viewsLeft = data.getInt("viewsLeft");
            }
            if(status.equals( "enabled" )){
                processAds(data.getJSONArray("ads"));
                rebuildQueue();
                preloadNextAd();
            }
        } catch (JSONException e) {
            throw new DioSdkException("bad placement data");
        }
    }
    private void processAds(JSONArray adsData) {
        lastAdStack = ads;
        ads = new LinkedHashMap<String, Ad>();
        for(int i = 0; i < adsData.length(); i++ ) {
            try {
                JSONObject entry = (JSONObject)adsData.get(i);
                String adId = entry.getString("adId");
                Ad ad = Ad.factory(adId, entry.getJSONObject("ad"));
                ad.setPlacementId(id);
                ads.put( adId, ad );
            } catch (Exception e) {

            }
        }
        if(adsData.length() == 0) {
            Controller.getInstance().triggerPlacementAction("onNoAds", this.id);
        }
    }
    private void rebuildQueue() {

        Iterator it = ads.entrySet().iterator();
        queue.clear();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            queue.add(entry.getKey().toString());
        }

    }
    public Ad getAd(String id) {
        Ad ad = null;
        if(ads.containsKey(id)) {
            ad = ads.get(id);
        } else {
            if(lastAdStack.containsKey(id)) {
                ad = lastAdStack.get(id);
            }
         }
        return ad;
    }
    public Ad getNextAd() {
        String adid = null;
        Ad ad = null;
        if(ads.size() > 0) {
            if(queue.size() == 0) {
                Controller.getInstance().refetchPlacement(this.id);
                // we rebuild the queue so we have what to serve till we get a new set
                rebuildQueue();
            }
            adid = queue.get(0);
            queue.remove(0);
            ad = ads.get(adid);
            preloadNextAd();
        }

        return ad;
    }
    private void preloadNextAd() {
        if (queue.size() > 0) {
            final String adid = queue.get(0);
            try {
                Ad ad = ads.get(adid);
                if(ad != null) {
                    ad.preload(new AdPreloadListener() {
                        public void onError() {
                            ads.remove(adid);
                            queue.remove(0);
                            preloadNextAd();
                        }
                        public void onLoaded() {
                        }
                    });
                }
            } catch(DioSdkException E) {
                ads.remove(adid);
                queue.remove(0);
                preloadNextAd();
            }
        }
    }
    public JSONObject getDebugData() {
       return data;
    }
    public int getQueueSize() {
        return queue.size();
    }
    public Boolean hasAd() {
        return ads.size() > 0;
    }

    public boolean isOperative()  {
        Boolean retval = false;
        retval = (this.status.equals("enabled")) && (capViews == false || viewsLeft > 0);
        return retval;
    }

    public String getName() {
        try {
            return this.data.getString("name");
        } catch (JSONException e) {
            return "UNKNOWN";
        }
    }
}
