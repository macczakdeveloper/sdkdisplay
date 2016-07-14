package io.display.sdk;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.display.sdk.ads.Ad;
import io.display.sdk.device.DeviceDescriptor;
import io.display.sdk.device.DeviceEventsListener;

public class Controller {
    static Controller Instance;
    public DeviceDescriptor deviceDescriptor;
    public HashMap<String, Placement> placements;
    public ErrorLogger errLogger;
    private ServiceClient serviceClient;
    private EventListener listener = null;
    private EventListener internalEventListener;
    private Context context;
    private Boolean adLock = false;
    String appId;
    Boolean initialized = false, deviceReady = false, initializing = false;
    public Boolean isAdLocked() {
        return adLock;
    }
    private Boolean obtainAdLock() {
        if(!adLock) {
            adLock = true;
            return true;
        }
        return false;
    }
    public void freeAdLock() {
        adLock = false;
    }

    private Controller() {
        errLogger = new ErrorLogger(appId);
        serviceClient = new ServiceClient(this);
        placements = new HashMap<String, Placement>();
    }

    public static Controller getInstance() {
        if (Instance == null) {
            Instance = new Controller();
        }
        return Instance;
    }

    public Boolean isInitialized() {
        return initialized;
    }

    public void init(Context context, String appId) {
        if(initialized || initializing) {
            return;
        }
        doInitialize(context, appId);
    }

    public void doInitialize(Context context, String appId) {
        Log.i("io.display.sdk", "initializing");
        initialized = false;
        initializing = true;
        this.appId = appId;
        this.context = context;
        deviceDescriptor = new DeviceDescriptor(context, new DeviceEventsListener() {
            @Override
            public void onDeviceIdRetrieved() {
                setDeviceReady();
                fetchPlacements();
            }
        });
        placements = new HashMap<String, Placement>();
    }

    public void setG(String g) {
        if (BuildConfig.BUILD_TYPE == "debug") {
            serviceClient.forcedGeo = g;
        }
    }

    public void setCourse(String course) {
        if (BuildConfig.BUILD_TYPE == "debug") {
            serviceClient.uri = course;
        }
    }

    public void triggerPlacementAction(String action, String placement) {

        if(listener != null) {
            Log.d("io.display.sdk", "Trigger " + action + "() for placement " + placement);
            switch(action) {
                case "onAdClick":
                    listener.onAdClick(placement);
                    break;
                case "onAdShown":
                    listener.onAdShown(placement);
                    break;
                case "onNoAds":
                    listener.onNoAds(placement);
                    break;
                case "onAdClose":
                    listener.onAdClose(placement);
                    break;
                case "onAdCompleted":
                    listener.onAdCompleted(placement);
            }
        }
    }

    void fetchPlacements() {
        try {
            serviceClient.getPlacements(appId, serviceClient.new ServiceResponseListener() {


                public void onSuccessResponse(JSONObject resp) {
                    try {
                        if (!resp.has("placements")) {
                            throw new DioSdkException("bad getPlacements() response, no placements");
                        }
                        JSONObject plcmnts = resp.getJSONObject("placements");
                        Iterator pkeys = plcmnts.keys();

                        while (pkeys.hasNext()) {
                            String placementId = (String) pkeys.next();
                            Placement placement = new Placement(placementId);
                            placement.setup(plcmnts.getJSONObject(placementId));
                            placements.put(placementId, placement);


                        }
                        setInitialized();

                    } catch (DioSdkException e) {
                        onError(e.getMessage(), resp);
                    } catch (JSONException e) {
                        onError(e.getMessage(), resp);
                    }
                }

                public void onErrorResponse(JSONObject resp) {
                    String serial = "";
                    if (resp != null) {
                        serial = resp.toString();
                    }
                    onInitError("badly formed response : " + serial);
                }

                public void onErrorResponse(String msg, JSONObject resp) {
                    String serial = "";
                    if (resp != null) {
                        serial = resp.toString();
                    }
                    onInitError(msg + ". response : " + serial);
                }

                public void onError(String msg, JSONObject resp) {
                    String serial = "";
                    if (resp != null) {
                        serial = resp.toString();
                    }
                    onInitError(msg + ". response : " + serial);
                }
            });
        } catch (DioSdkException e) {
            onInitError(e.getMessage());
        }
    }

    void setInitialized() {
        initialized = true;
        if (deviceReady) {
            onInit();
        }
    }

    void setDeviceReady() {
        deviceReady = true;
        if (initialized) {
            onInit();
        }
    }

    void onInit() {
        Log.i("io.display.sdk", "Inititialized");
        initializing = false;
        if (listener != null) {
            listener.onInit();
        }
        if(internalEventListener != null) {
            internalEventListener.onInit();
        }

    }

    void onInitError(String msg) {
        Log.d("io.display.sdk", "Init Error : " + msg);
        initializing = false;
        if (listener != null) {
            listener.onInitError(msg);
        }
        if(internalEventListener != null) {
            internalEventListener.onInitError(msg);
        }

    }

    public void refetchPlacement(final String placementId) {
        try {
            serviceClient.getPlacement(appId, placementId, serviceClient.new ServiceResponseListener() {
                public void onErrorResponse(String msg, JSONObject data) {
                }

                public void onSuccessResponse(JSONObject resp) {
                    try {
                        placements.get(placementId).setup(resp);
                    } catch (Exception e) {

                    }
                }

                public void onError(String error, JSONObject resp) {

                }

            });
        } catch (DioSdkException e) {

        }
    }

    public Context getContext() {
        return context;
    }

    public String getVer() {
        return BuildConfig.VERSION_NAME;
    }

    public boolean showAd(final String placementId) {

        Log.i("io.display.sdk", "Calling showAd() for placement " + placementId);
        if(this.initialized) {
            Placement placement = this.placements.get(placementId);
            Boolean loaded = false;
            if (placement != null) {
                Ad ad = placement.getNextAd();
                if (ad != null) {

                    Context ctx = this.getContext();
                    Intent i;
                    switch (ad.getActivityType()) {
                        case "translucent":
                            i = new Intent(ctx, DioTranslucentActivity.class);
                            break;
                        case "normal":
                        default:
                            i = new Intent(ctx, DioActivity.class);
                            break;
                    }
                    i.putExtra("placement", placementId);
                    i.putExtra("ad", ad.getId());
                    i.putExtra("cmd", "render");
                    if (!obtainAdLock()) {
                        Log.i("io.display.sdk", "Adlock occupied ignoring showAd()");
                        return false;
                    }
                    ctx.startActivity(i);
                    loaded = true;
                    Log.i("io.display.sdk", "Showing ad for placement " + placementId);
                } else {
                    Log.i("io.display.sdk", "Don't have an Ad for placement " + placementId);
                }
            } else {
                Log.i("io.display.sdk", "Don't know placement " + placementId);
            }
            return loaded;
        } else {
            if(!initializing) {
                Log.e("io.display.sdk", "calling showAd with before calling init()");
                return false;
            } else {
                setInternalEventListener(new EventListener() {
                    public void onInit() {
                        showAd(placementId);
                    }
                });
                return false;
            }
        }
    }
    private void setInternalEventListener(EventListener listener) {
        internalEventListener = listener;
    }
    public boolean showAd(Context ctx, String placementId) {
        this.context = ctx;
        return this.showAd(placementId);
    }

    public boolean showAd() {
        return showAd(context);
    }
    public boolean showAd(Context ctx) {
        if(placements.size() > 0) {
            String p = placements.keySet().iterator().next();
            return showAd(ctx, p);
        } else {
            if(!initialized && initializing) {
                setInternalEventListener(new EventListener() {
                    public void onInit() {
                        showAd();
                    }
                });
            }
            return false;
        }
    }
    public void logError(String err) {
        this.errLogger.log(err);
    }

    public EventListener setEventListener(EventListener listener) {
        Log.d("io.display.sdk", "setting event listener");
        this.listener = listener;
        return listener;
    }

    public EventListener getEventListener() {
        return listener;
    }
}

