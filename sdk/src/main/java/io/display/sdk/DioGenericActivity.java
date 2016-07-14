package io.display.sdk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;

import io.display.sdk.ads.Ad;

public abstract class DioGenericActivity extends Activity {
    private Boolean backEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preCreate();
        super.onCreate(savedInstanceState);
        postCreate();
        Intent i = getIntent();
        switch(i.getStringExtra("cmd")) {
            case "render":
                try {
                    render();
                } catch(DioSdkException E) {
                    this.finish();
                }
                break;
            case "redirect":
                redirect();
                break;
        }
    }
    abstract protected void preCreate();
    abstract protected void postCreate();

    private void redirect() {
        Intent i = getIntent();
        setBackEnabled(true);
        WebView webv = new WebView(this);
        Log.i("io.display.sdk", "Redirecting to ad click");

        webv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.matches(".*://play.google.com.*")) {
                    url = url.replaceFirst(".*://play.google.com/.*/details", "market://details");
                }
                if (url != null && url.startsWith("market://")) {
                    try {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        finish();
                        return true;
                    } catch (Exception e) {
                        view.loadUrl(url);
                        return false;
                    }
                } else {
                    view.loadUrl(url);
                    return false;
                }
            }
        });

        webv.getSettings().setJavaScriptEnabled(true);
        try {
            webv.loadUrl(i.getStringExtra("clk"));
            setContentView(webv);
        } catch (Exception e) {
            finish();
        }
    }
    private void render() throws DioSdkException {
        Controller ctrl = Controller.getInstance();
        Intent i = getIntent();
        String placementId = i.getStringExtra("placement");
        if(!ctrl.placements.containsKey(placementId)) {
            throw new DioSdkException("placewment "+ placementId +" is not present when trying to render ad");
        }

        Ad ad = ctrl.placements.get(placementId).getAd(i.getStringExtra("ad"));
        if (ad == null) {
            this.finish();
        } else {
            ad.render(this);
        }
    }
    @Override
    public void onDestroy() {

        super.onDestroy();
        Controller.getInstance().freeAdLock();
        Log.i("io.display.sdk", "Ad Closed");
    }

    @Override
    public void onBackPressed() {
        if(backEnabled) {
            super.onBackPressed();
        }
    }
    public void setBackEnabled(Boolean enabled) {
        backEnabled = enabled;
    }
}
