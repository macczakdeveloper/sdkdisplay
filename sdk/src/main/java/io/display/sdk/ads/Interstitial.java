package io.display.sdk.ads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.display.sdk.Controller;
import io.display.sdk.DioGenericActivity;
import io.display.sdk.DioSdkException;
import io.display.sdk.R;

/**
 * Created by gidi on 07/03/16.
 */
class Interstitial extends Ad {
    private ImageView imgView;
    private ProgressBar progressBar;
    private CountDownTimer timer;
    private int skipAfter = 7;
    protected RelativeLayout layout;
    protected RelativeLayout innerLayout;
    protected ImageView nothanks;
    public Interstitial(String id, JSONObject data) {
        super(id, data);
    }

    public String getActivityType() {
        return "translucent";
    }
    public void preload(final AdPreloadListener listener) throws DioSdkException {
        try {
            Log.d("io.dsplay.sdk", "preloading interstitial creative");
            final String url  = data.getString("ctv");
            loadBitmap("ctv", data.getString("ctv"), new BitmapLoadListener() {
                @Override
                public void onSuccess() {
                    Log.d("io.dsplay.sdk", "Successfully loaded interstitial creative");
                    listener.onLoaded();
                }

                @Override
                public void onError() {
                    Log.w("io.dsplay.sdk", "Failed to load interstitial creative");
                    Controller.getInstance().logError("could not retrieve ctv : " + url);
                    listener.onError();

                }
            });

        } catch (JSONException e) {
            Log.e("io.dsplay.sdk", "Error");
            Controller.getInstance().logError("bad json interstitial ad data: " + data.toString());
            throw new DioSdkException("melformatted");
        }
    }

    public void render(DioGenericActivity act) {
        try {
            this.activity = act;
            Log.i("io.dsplay.sdk", "Rendering interstitial ad");

            /**
             *
             */

            layout = new RelativeLayout(activity);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            layout.setLayoutParams(lp);
            activity.setContentView(layout);
            HashMap deviceProps = Controller.getInstance().deviceDescriptor.getProps();
            int x = Math.round((int)deviceProps.get("w") / 2);
            int y = Math.round((int)deviceProps.get("h") / 2);

            Animation anim = new ScaleAnimation(0,1, 0, 1, x, y);
            anim.setDuration(300); // duration = 300
            layout.startAnimation(anim);

            /**
             *  SETUP IMAGE/PROGRESS BAR STARTS
             */
            imgView = new ImageView(activity);

            imgView.setScaleType(ImageView.ScaleType.CENTER);
            RelativeLayout.LayoutParams imgLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            imgLp.setMargins(35, 50, 35, 50);
            imgView.setLayoutParams(imgLp);
            imgView.setAdjustViewBounds(true);

            progressBar = new ProgressBar(activity);
            RelativeLayout.LayoutParams progressLp =  new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            progressLp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

            progressBar.setLayoutParams(progressLp);


            /**
             * SETUP IMAGE/PROGRESS BAR ENDS
             * SETUP NO THANKS STARTS
             */

//            nothanks.setText("No Thanks");
//            nothanks.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
//            nothanks.setTextColor(Color.BLACK);
//            nothanks.setShadowLayer(1, 2, 2, Color.WHITE);
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                GradientDrawable nothanksBg =  new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
//                    Color.parseColor("#CCEFEFEF"), Color.parseColor("#CCDADADA")
//                });
//                nothanksBg.setCornerRadius(10);
//                nothanks.setBackground(nothanksBg);
//            } else {
//                nothanks.setBackgroundColor(Color.WHITE);
//            }

            //nothanksContainer.addView(nothanks);
            nothanks = new ImageView(activity);
            InputStream xButtonStream = this.getClass().getResourceAsStream("/images/x.png");
            if (null != xButtonStream) {
                Bitmap xBmp = BitmapFactory.decodeStream(xButtonStream);
                nothanks.setImageBitmap(xBmp);
            }
            nothanks.setPadding(0, 40, 10, 0);
            nothanks.setAdjustViewBounds(true);
            nothanks.setScaleType(ImageView.ScaleType.FIT_XY);



            nothanks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //timer.cancel();
                    HashMap deviceProps = Controller.getInstance().deviceDescriptor.getProps();
                    int x = Math.round((int) deviceProps.get("w") / 2);
                    int y = Math.round((int) deviceProps.get("h") / 2);

                    Animation anim = new ScaleAnimation(1, 0, 1, 0, x, y);
                    anim.setDuration(300); // duration = 300
                    layout.startAnimation(anim);
                    activity.finish();
                    Controller.getInstance().triggerPlacementAction("onAdClose", placement);
                }
            });

            /**
             * SETUP NO THANKS ENDS
             * SETUP INNER LAYOUT STARTS
             */


            innerLayout = new RelativeLayout(activity);
            RelativeLayout.LayoutParams innerLp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            innerLayout.setLayoutParams(innerLp);
            innerLayout.setBackgroundColor(Color.parseColor("#88AAAAAA"));
            innerLayout.addView(imgView, 0);
            innerLayout.addView(progressBar, 1);
            layout.addView(innerLayout, 0);
            //layout.setBackgroundColor(Color.parseColor("#22110000"));


            if(getBitmap("ctv") == null) {
                this.imgBitmapListeners.put("ctv", new BitmapLoadListener() {
                    @Override
                    public void onError() {
                        activity.finish();
                        Controller.getInstance().triggerPlacementAction("onAdClose", placement);
                    }
                    public void onSuccess() {
                        displayAd();
                    }
                });
            } else {
                displayAd();
            }
        } catch (Exception e) {
            activity.finish();
            Controller.getInstance().triggerPlacementAction("onAdClose", placement);
        }

    }
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    protected void displayAd() {
        try {
            progressBar.setVisibility(View.INVISIBLE);
            imgView.setImageBitmap(imgBitmaps.get("ctv"));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                imgView.setId(View.generateViewId());
            } else {
                imgView.setId(generateViewId());

            }
            RelativeLayout.LayoutParams notanksLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            int w = (int)Controller.getInstance().deviceDescriptor.getProps().get("w");
  notanksLp.width=90;
            notanksLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            notanksLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            nothanks.setLayoutParams(notanksLp);

            innerLayout.addView(nothanks, 1);
            imgView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progressBar.setVisibility(View.VISIBLE);
                        redirect();
                    }
                });
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
               public void onClick(View v) {

                 activity.finish();
                }
            });
            callImpBecon();

            Controller.getInstance().triggerPlacementAction("onAdShown", placement);
        } catch (Exception e) {
            activity.finish();
            Controller.getInstance().triggerPlacementAction("onAdClose", placement);
        }
    }
}
