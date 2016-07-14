package io.display.sdk.ads;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.Layout;
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
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;

import io.display.sdk.Controller;
import io.display.sdk.DioGenericActivity;
import io.display.sdk.DioSdkException;
import io.display.sdk.R;

/**
 * Created by gidi on 15/03/16.
 */
class InterstitialVideo extends Ad {
    private ImageView imgView, nothanks;
    private VideoView videoView;
    private TextView skipText;
    private TextView timerText;
    private ProgressBar progressBar;
    private FrameLayout mainLayout;
    private RelativeLayout videoLayout;
    private MediaPlayer mediaPlayer;
    private int barHeight, commonTextSize;
    private Uri videoUri;
    private RelativeLayout landingCardLayout = null;
    private CountDownTimer timer;
    private Boolean initialized = false;
    public InterstitialVideo(String id, JSONObject data) {
        super(id, data);
    }
    public void preload(final AdPreloadListener listener) throws DioSdkException {
        try {
            final String url = data.getString("landingCard");
            loadBitmap("card", url, new BitmapLoadListener() {
                @Override
                public void onSuccess() {
                    listener.onLoaded();
                }

                @Override
                public void onError() {
                    Log.w("io.dsplay.sdk", "Failed to load interstitial creative");
                    Controller.getInstance().logError("could not retrieve ctv : " + url);
                    listener.onError();
                }
            });
        } catch (Exception e) {
            throw new DioSdkException("could not preload video ad, loading landing card");
        }
    }

    public void render(DioGenericActivity act) {
        try {
            this.activity = act;
            int orientation  = activity.getResources().getConfiguration().orientation;
            if(orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
                callImpBecon();
                HashMap props = Controller.getInstance().deviceDescriptor.getProps();
                int deviveHeight = (int)props.get("w");
                barHeight = Math.round(deviveHeight / 25);
                commonTextSize = Math.round(barHeight / 3);


                mainLayout = new FrameLayout(activity);
                videoLayout = new RelativeLayout(activity);

                landingCardLayout = null;
                videoView = new VideoView(activity);
                RelativeLayout.LayoutParams videoLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                videoLp.addRule(RelativeLayout.CENTER_VERTICAL);
                videoLp.addRule(Gravity.CENTER_VERTICAL);
                videoView.setLayoutParams(videoLp);
                skipText = new TextView(activity);
                skipText.setTextColor(Color.parseColor("#EEEEEE"));
                skipText.setShadowLayer(1, 2, 2, Color.parseColor("#222222"));
                skipText.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                skipText.setPadding(5, 5, 10, 35);
                skipText.setTextSize(commonTextSize);


                activity.setContentView(mainLayout);
                mainLayout.addView(videoLayout);
                imgView = new ImageView(activity);
                videoLayout.addView(videoView, 0);
                videoLayout.addView(skipText, 1);

                RelativeLayout.LayoutParams timerLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                timerLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                timerLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);




                timerText = new TextView(activity);
                timerText.setTextSize(commonTextSize);
                timerText.setTextColor(Color.parseColor("#555555"));
                timerText.setShadowLayer(1, 2, 2, Color.parseColor("#EEEEEE"));
                timerText.setLayoutParams(timerLp);

                videoLayout.addView(timerText);

                videoLayout.setBackgroundColor(Color.BLACK);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                videoUri  = Uri.parse(data.getString("video"));
                videoView.setVideoURI(videoUri);
                videoView.setLayoutParams(lp);
                final int duration = data.getInt("duration");
                int skipIn;
                if(data.has("skippableIn")) {
                    skipIn = data.getInt("skippableIn");
                } else {
                    skipIn = 0;
                }
                final int finalSkip = skipIn;


                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer = mp;
                        mp.start();
                        if(duration > 15) {
                            setSkipTimer((int)(mp.getDuration()/ 1000), finalSkip);
                        } else {
                            setSkipTimer((int)(mp.getDuration()/ 1000), 0);
                        }
                    }

                });

                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Controller.getInstance().logError("video error no." + Integer.toString(what));
                        return false;
                    }
                });
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Controller.getInstance().triggerPlacementAction("onAdCompleted", placement);
                        showLanding();
                    }
                });
                Controller.getInstance().triggerPlacementAction("onAdShown", placement);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } catch (Exception e) {
            activity.finish();
            Controller.getInstance().triggerPlacementAction("onAdClose", placement);
        }

    }
    private void setSkipTimer(final int duration , final int skipSecs) {

        timer = new CountDownTimer(duration * 1000, 1000) {

            @Override
            public void onTick(long leftTimeInMilliseconds) {
                int secsLeft = (int)(leftTimeInMilliseconds / 1000);;
                int secsPassed = duration - secsLeft;
                updateTimer(secsLeft);
                if(skipSecs > 0) {
                    int skipIn = (int) (skipSecs - secsPassed);
                    if (skipIn > 0) {
                        updateSkip(skipIn);
                    } else {
                        makeSkippable();
                    }
                }

            }
            @Override
            public void onFinish() {
            }
        }.start();


    }
    private void makeSkippable() {
        skipText.setText("Skip");
        skipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                videoView.stopPlayback();
                showLanding();
                timer.cancel();
            }
        });
    }
    private void updateSkip(int secs) {
        skipText.setText("Skip in " + String.valueOf(secs));

    }
    private void updateTimer(int secs) {
        timerText.setText("Video will end in " + String.valueOf(secs) + " seconds");
    }
    private void showLanding() {
        initializeLanding();
        videoView.stopPlayback();
        videoLayout.setVisibility(View.GONE);
        landingCardLayout.setVisibility(View.VISIBLE);
    }
    private void replay() {
        landingCardLayout.setVisibility(View.GONE);
        videoLayout.setVisibility(View.VISIBLE);
        videoView.setVideoURI(videoUri);

    }
    private void initializeLanding() {
        if(landingCardLayout == null) {


            landingCardLayout = new RelativeLayout(activity);
            mainLayout.addView(landingCardLayout);

            nothanks = new ImageView(activity);

            RelativeLayout.LayoutParams nothanksLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            nothanksLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            nothanksLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            nothanksLp.width=90;
            nothanks = new ImageView(activity);
            InputStream xButtonStream = this.getClass().getResourceAsStream("/images/x.png");
            if (null != xButtonStream) {
                Bitmap xBmp = BitmapFactory.decodeStream(xButtonStream);
                nothanks.setImageBitmap(xBmp);
            }
            nothanks.setPadding(15, 10, 0, 0);
          nothanks.setMaxHeight(100);
            nothanks.setMaxWidth(100);
            nothanks.setAdjustViewBounds(true);
            nothanks.setScaleType(ImageView.ScaleType.FIT_XY);
            nothanks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
            timer.cancel();
                    activity.finish();
                    Controller.getInstance().triggerPlacementAction("onAdClose", placement);
                }
            });

            ImageView replay = new ImageView(activity);
            InputStream replayStream = this.getClass().getResourceAsStream("/images/reload_icon.png");
            if (null != replayStream) {
                Bitmap replayBmp = BitmapFactory.decodeStream(replayStream);
                replay.setImageBitmap(replayBmp);
            }
            //replay.setImageResource(R.drawable.reload_icon);
            replay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    replay();
                }
            });


            /***************/

            imgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    redirect();
                    Controller.getInstance().triggerPlacementAction("onAdClose", placement);
                }
            });




            View bottomBorder = new View(activity);
            bottomBorder.setBackgroundColor(Color.BLACK);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);




            landingCardLayout.addView(imgView, 0);
            landingCardLayout.addView(nothanks, 1);




            landingCardLayout.setBackgroundColor(Color.parseColor("#110000"));
            imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            RelativeLayout.LayoutParams imgLp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            imgView.setLayoutParams(imgLp);
            imgView.setImageBitmap(imgBitmaps.get("card"));
        }
    }

}
