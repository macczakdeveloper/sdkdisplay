package io.display.sdk;


import android.os.Build;
import android.transition.Explode;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class DioActivity extends DioGenericActivity {
    protected void preCreate() {
        requestWindowFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Explode());
        }
    }
    protected void postCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          //  getWindow().setExitTransition(new Explode());
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
