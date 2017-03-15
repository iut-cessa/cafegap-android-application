package com.example.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public final static String USERNAME = "USERNAME", PASSWORD = "PASSWORD", EMPTY = "EMPTY", IP="ip_address";
    private String response = EMPTY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String username = sharedPref.getString(USERNAME, EMPTY);
        String password = sharedPref.getString(PASSWORD, EMPTY);
        String ip = sharedPref.getString(IP, EMPTY);

        // If username or password is saved, load them to layout
        if (!(username.equals(EMPTY) || password.equals(EMPTY) || ip.equals(EMPTY))) {
            ((EditText) findViewById(R.id.username)).setText(username);
            ((EditText) findViewById(R.id.password)).setText(password);
            ((EditText) findViewById(R.id.ip_address)).setText(ip);

            findViewById(R.id.login).performClick();
        }
    }

    /** Called when the user clicks the Login button */
    public void login(View view) {
        String username = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        String ip = ((EditText) findViewById(R.id.ip_address)).getText().toString();

        final String url = "http://" + ip + ":8080/check/?username=" + username +
                "&secret=" + getHash(password).toLowerCase();
        Thread net = new Thread() {
            public void run() {
                response = performPostCall(url, new HashMap<String, String>());
            }
        };
        net.start();

        try {
            net.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ((TextView) findViewById(R.id.response)).setText(response);  // TODO Delete this line.

        SharedPreferences.Editor editor = this.getPreferences(Context.MODE_PRIVATE).edit();
        try {
            if ((new JSONObject(response)).getString("Status").equals("Ok")) {
                editor.putString(USERNAME, username);
                editor.putString(PASSWORD, password);
                editor.putString(IP, ip);

                Intent participant_activity = new Intent(getApplicationContext(), ParticipantActivity.class);
                participant_activity.putExtra(USERNAME, username);
                participant_activity.putExtra(PASSWORD, password);
                participant_activity.putExtra(IP, ip);
                startActivity(participant_activity);
            } else {
                editor.remove(USERNAME);
                editor.remove(PASSWORD);
                editor.remove(IP);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.apply();
    }

    public static String getHash(String password) {
        MessageDigest digest;
        byte[] data = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            data = digest.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        return bin2hex(data);
    }

    public static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
    }

    public static String  performPostCall(String requestURL, HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
