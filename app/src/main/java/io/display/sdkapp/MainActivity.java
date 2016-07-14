package io.display.sdkapp;


import android.content.Intent;
import android.view.View;

public class MainActivity extends AbstractActivity {

    public void switchOrientation(View v) {
        Intent i = new Intent(this, LandscapActivity.class);
        i.putExtra("placementValues", placementValues);
        startActivity(i);
    }
}
