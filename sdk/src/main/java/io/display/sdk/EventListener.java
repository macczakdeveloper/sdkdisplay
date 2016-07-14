package io.display.sdk;

/**
 * Created by gidi on 09/03/16.
 */
public abstract class EventListener {
    private String status = "active";
    public void onInit() {

    }
    public void onInitError(String msg) {

    }
    public void onAdShown(String placement) {

    }
    public void onNoAds(String placement) {

    }
    public void onAdCompleted(String placement) {

    }
    public void onAdClose(String placement) {

    }
    public void onAdClick(String placement) {

    }
    public void inactivate() {
        status = "inactive";
    }
    public void activate() {
        status = "active";
    }
    public Boolean isActive() {
        return status.equals("active");
    }
}
