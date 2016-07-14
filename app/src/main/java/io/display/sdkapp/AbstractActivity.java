package io.display.sdkapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.display.sdk.Controller;
import io.display.sdk.EventListener;
import io.display.sdk.Placement;


abstract public class AbstractActivity extends AppCompatActivity {
    Spinner placements;
    Button loadPlacement;
    Button debug;
    Button refreshPlacements;
    EditText appInput;
    Spinner envInput;
    Spinner geoInput;
    ProgressBar preloader;
    Controller ctrl;
    EventListener listener;
    HashMap<String, String> placementValues = new  HashMap<String, String>();
    @Override
    protected void onPause() {
        listener.inactivate();
        super.onPause();
    }
    @Override
    protected void onResume() {
        listener.activate();
        super.onResume();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preloader = (ProgressBar) this.findViewById(R.id.progressBar);
        placements = (Spinner) this.findViewById(R.id.placements);
        loadPlacement = (Button) this.findViewById(R.id.loadPlacement);
        debug = (Button) this.findViewById(R.id.debug);
        refreshPlacements = (Button) this.findViewById(R.id.refreshPlacements);
        appInput = (EditText) this.findViewById(R.id.appId);
        geoInput = (Spinner) this.findViewById(R.id.geo);
        envInput = (Spinner) this.findViewById(R.id.env);
        ctrl = Controller.getInstance();
        Intent i = getIntent();
        final Activity actvt = this;
        final RelativeLayout layout = (RelativeLayout) this.findViewById(R.id.layout);
        ViewTreeObserver observer = layout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                envInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (getEnv()) {
                            case "DEV":
                                ctrl.setCourse("https://appsrv.displayio.local/srv?XDEBUG_SESSION_START=netbeans-xdebug");
                                break;
                            case "PROD":
                                ctrl.setCourse("https://appsrv.display.io/srv");
                        }

                    }
                });
                geoInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ctrl.doInitialize(actvt, getAppId());
                        ctrl.setG(getGeo());
                        preloader.setVisibility(View.VISIBLE);
                        placementValues.clear();
                    }
                });
            }
        });
       // uri = "";
        if(i.hasExtra("placementValues")) {
            placementValues = (HashMap<String, String> )i.getSerializableExtra("placementValues");
            renderPlacementSelect();
        }
        listener = ctrl.setEventListener(new EventListener() {
                                  @Override
                                  public void onInit() {
                                      preloader.setVisibility(View.INVISIBLE);
                                      ArrayList<String> list = new ArrayList<String>();
                                      Iterator it = ctrl.placements.entrySet().iterator();

                                      while (it.hasNext()) {
                                          Map.Entry<String, Placement> e = (Map.Entry<String, Placement>) it.next();
                                          Placement p = e.getValue();

                                          placementValues.put(p.getName(), p.id);
                                          list.add(p.getName());
                                      }
                                      renderPlacementSelect();
                                  }

                                  @Override
                                  public void onInitError(String msg) {
                                      preloader.setVisibility(View.INVISIBLE);
                                      AlertDialog alert = new AlertDialog.Builder(ctrl.getContext()).create();
                                      alert.setTitle("Init error");
                                      alert.setMessage(msg);
                                      alert.show();

                                  }
                              }
        );

    }

    public void showDebug(View view) {
        Iterator it = ctrl.placements.entrySet().iterator();
        String msg = new String();
        while (it.hasNext()) {
            Map.Entry<String, Placement> e = (Map.Entry<String, Placement>) it.next();
            Placement p = e.getValue();
            try {
                JSONObject debugData = p.getDebugData();
                JSONArray ads = debugData.getJSONArray("ads");
                msg += "\n\nPlacement " + p.id + " \"" + p.getName() + "\" (" + debugData.getString("status") + " " + ads.length() + " ads )\n\n";
                for(int i = 0; i < ads.length(); i++ ) {
                    JSONObject adData = (JSONObject)ads.get(i);
                    msg += "Ad no." + Integer.toString(i + 1) + " : " +adData.getJSONObject("ad").getString("type") + ":" +  adData.getJSONObject("ad").getString("subtype")
                            + " Campaign ID " + Integer.toString(adData.getInt("cpn")) + " (" + adData.getJSONObject("offering").getString("id") + ")\n\n";
                }
            } catch (JSONException E) {
                msg = "Exception : " +  E.getMessage();
            }
            placementValues.put(p.getName(), p.id);
        }
        Context ctx = ctrl.getContext();
        AlertDialog alert = new AlertDialog.Builder(ctx).create();
        alert.setTitle("Loaded Successfully");
        alert.setMessage(msg);
        alert.show();
    }

    protected void renderPlacementSelect() {
        Iterator i = placementValues.entrySet().iterator();
        ArrayList<String> list = new ArrayList<String>();
        while(i.hasNext()) {
            HashMap.Entry<String, String> entry = (HashMap.Entry<String, String>) i.next();
            list.add(entry.getKey());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_item, list);
        placements.setAdapter(adapter);
    }
    public String getEnv() {
        return envInput.getSelectedItem().toString();
    }

    public String getAppId() {
        return appInput.getText().toString();
    }
    public String getGeo() {
        return geoInput.getSelectedItem().toString();
    }

    public void getAppPlacements(View v) {
        ctrl.doInitialize(this, this.getAppId());
        switch(getEnv()) {
            case "DEV":
                ctrl.setCourse("https://appsrv.displayio.local/srv?XDEBUG_SESSION_START=netbeans-xdebug");
                break;
            case "PROD":
                ctrl.setCourse("https://appsrv.display.io/srv");
        }
        ctrl.setG(getGeo());
        preloader.setVisibility(View.VISIBLE);
        placementValues.clear();


    }

    public void showAd(View v) {
        Object selectedItem = this.placements.getSelectedItem();
        if(selectedItem != null) {
            String plcname = selectedItem.toString();
            String plcid = placementValues.get(plcname);
            ctrl.showAd(plcid);
        }

    }
    abstract public void switchOrientation(View v);

}
