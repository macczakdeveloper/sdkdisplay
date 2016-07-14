package io.display.sdk;


import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ServiceClient {
    Controller controller;
    String forcedGeo = null;
    private int sigChar1 = 402;
    private int sigChar2 = 178;
    private int remainder;
    private int remainderFactor = 12;

    String uri = "https://appsrv.display.io/srv";
    public ServiceClient(Controller ctrl) {
        controller = ctrl;
        if(BuildConfig.BUILD_TYPE == "debug") {

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            try {
                HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                SSLSocketFactory sfact = sc.getSocketFactory();
                HttpsURLConnection.setDefaultSSLSocketFactory(sfact);
            } catch (Exception e) {
            }
        }
        remainder = 1536 / remainderFactor;
    }
    public void alterCourse(String course) {
        this.uri = course;
    }
    private String getSprefix() {
        return  String.valueOf(Character.toChars((int)Math.ceil(sigChar1 / 3.5))) + String.valueOf(Character.toChars((int)Math.ceil(sigChar2 / 1.7)));
    }
    private JSONObject getSignedRequest(JSONObject payload) throws DioSdkException, JSONException {
        JSONObject req = new JSONObject();
        try {

            JSONObject deviceData = new JSONObject(controller.deviceDescriptor.getProps());
            JSONObject deviceIds  = new JSONObject();
            deviceIds.put("google_aid", controller.deviceDescriptor.googleAid);
            deviceData.put("ids", deviceIds);
            payload.put("device", deviceData);
            String SigKey =  getSprefix() + "g";
            if(forcedGeo != null) {
                payload.put("forceGeo" , forcedGeo);
            }
            payload.put("sdkVer", controller.getVer());
            payload.put("pkgName", controller.getContext().getApplicationContext().getPackageName());
            String json = payload.toString() + "ss";


            byte[] bytesOfMessage;
            json =  json + "d";
            String ObsfucationJson =  json + remainder;
            bytesOfMessage = ObsfucationJson.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1,thedigest);

            req.put(SigKey, bigInt.toString(16));
            req.put("data", payload);
        } catch (NoSuchAlgorithmException e) {
            throw new DioSdkException("no md5 algorithm on device");
        }
        return req;
    }

    public void getPlacements(String app, final ServiceResponseListener listener)  throws DioSdkException {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "getPlacements");
            req.put("app", app);
            this.makeCall(this.getSignedRequest(req), listener);
        } catch (JSONException e) {
            throw new DioSdkException("JSON exception ", e);
        }

    }
    public void getPlacement(String app, String placementId, final ServiceResponseListener listener)  throws DioSdkException {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "getPlacement");
            req.put("app", app);
            req.put("placement", placementId);
            this.makeCall(this.getSignedRequest(req), listener);
        } catch (JSONException e) {
            throw new DioSdkException("JSON exception ", e);
        }

    }
    private void makeCall(final JSONObject params,final ServiceResponseListener listener) {
        ServiceRequest asyncCall = new ServiceRequest() {
            @Override
            protected void onPostExecute(JSONObject response)  {
                if(exception != null) {
                    listener.onErrorResponse(this.exception.getClass() + " Exception: " + exception.getMessage(), response);
                }
                try {
                    if(response == null) {
                        listener.onErrorResponse("null response on " + params.getString("action"), response);
                    } else {
                        if (!response.has("data")) {
                            listener.onErrorResponse("no data section in response", response);
                        }
                        listener.onSuccessResponse(response.getJSONObject("data"));
                    }

                } catch(JSONException e) {
                    listener.onErrorResponse("no data section in response", response);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            asyncCall.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            asyncCall.execute(params);
        }
    }
    private class NullHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            Log.i("RestUtilImpl", "Approving certificate for " + hostname);
            return true;
        }

    }
    abstract private class ServiceRequest extends AsyncTask<JSONObject, JSONObject, JSONObject> {
        protected Throwable exception;
        public String rawResponse = "";
        @Override
        protected JSONObject doInBackground(JSONObject[] requests)  {
            JSONObject request = requests[0];
            StringBuilder str = new StringBuilder();
            JSONObject resp = null;

            try {
                URL url = new URL(uri);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(20000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(request.toString());
                writer.flush();
                writer.close();
                os.close();
                //conn.setUseCaches(false);
                conn.connect();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    str.append(line + "\n");
                }
                in.close();
                rawResponse =  str.toString();
                resp = new JSONObject( rawResponse );
            } catch (MalformedURLException e) {
               this.exception = e;
            } catch (IOException e) {
                this.exception = e;
            } catch (JSONException e) {
                this.exception = new Exception("bad json response : " + rawResponse);
            }

            return resp;
        }

    }


    abstract class ServiceResponseListener {
        abstract void onSuccessResponse(JSONObject response);
        abstract void onErrorResponse(String msg, JSONObject response);
        abstract void onError(String err, JSONObject data);
    }
}
