package io.display.sdkapp;


import android.content.Intent;
import android.view.View;

public class LandscapActivity extends AbstractActivity {
    public void switchOrientation(View v) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("placementValues", placementValues);
        startActivity(i);
    }

}
