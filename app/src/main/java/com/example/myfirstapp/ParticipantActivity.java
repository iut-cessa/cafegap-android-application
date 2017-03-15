package com.example.myfirstapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.myfirstapp.barcode.BarcodeCaptureActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ParticipantActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private String username, password, ip, response, part_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participant);

        Intent intent = getIntent();
        username = intent.getStringExtra(MainActivity.USERNAME);
        password = intent.getStringExtra(MainActivity.PASSWORD);
        ip = intent.getStringExtra(MainActivity.IP);
    }

    public void scan(View view) {
        ((TextView) findViewById(R.id.id)).setText("");
        ((TextView) findViewById(R.id.name)).setText("");
        ((CheckBox) findViewById(R.id.reception)).setChecked(false);
        ((CheckBox) findViewById(R.id.lunch)).setChecked(false);
        ((TextView) findViewById(R.id.status)).setText("");
        findViewById(R.id.lunch).setClickable(true);
        ((CheckBox) findViewById(R.id.lunch)).setTextColor(Color.BLACK);
        part_id = "";

        Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
        startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    // Point[] p = barcode.cornerPoints;
                    // ((TextView) findViewById(R.id.response)).setText(barcode.displayValue);

                    String[] info = barcode.rawValue.split("\\r?\\n");
                    for (String d : info)
                        if (d.toLowerCase().startsWith("id:"))
                            part_id = d.split(":")[1];

                    final String url = "http://" + ip + ":8080/info/?username=" + username +
                            "&part_id=" + part_id + "&auth=" +
                            MainActivity.getHash(username + part_id + password).toLowerCase();
                    Thread net = new Thread() {
                        public void run() {
                            response = MainActivity.performPostCall(url, new HashMap<String, String>());
                        }
                    };
                    net.start();

                    try {
                        net.join(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        JSONObject res = new JSONObject(response);
                        if (res.getString("Status").equals("Ok")) {
                            ((TextView) findViewById(R.id.status)).setText(R.string.ok);

                            ((TextView) findViewById(R.id.id)).setText(part_id);
                            ((TextView) findViewById(R.id.name)).setText(res.getString("name"));
                            ((CheckBox) findViewById(R.id.reception)).setChecked(res.getBoolean("registered"));
                            ((CheckBox) findViewById(R.id.lunch)).setChecked(res.getBoolean("got_lunch"));

                            if (! res.getBoolean("has_lunch")) {
                                findViewById(R.id.lunch).setClickable(false);
                                ((CheckBox) findViewById(R.id.lunch)).setTextColor(Color.RED);
                            }
                        } else {
                            ((TextView) findViewById(R.id.status)).setText(R.string.error);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else ((TextView) findViewById(R.id.status)).setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    public void reception(View view) {
        final String url = "http://" + ip + ":8080/update/?username=" + username +
                "&part_id=" + part_id + "&parameter=registered&value=" +
                (((CheckBox) findViewById(R.id.reception)).isChecked()? "true": "false") + "&auth=" +
                MainActivity.getHash(username + part_id + "registered" +
                        (((CheckBox) findViewById(R.id.reception)).isChecked()? "true": "false") +
                        password).toLowerCase();
        Thread net = new Thread() {
            public void run() {
                response = MainActivity.performPostCall(url, new HashMap<String, String>());
            }
        };
        net.start();

        try {
            net.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lunch(View view) {
        final String url = "http://" + ip + ":8080/update/?username=" + username +
                "&part_id=" + part_id + "&parameter=got_lunch&value=" +
                (((CheckBox) findViewById(R.id.lunch)).isChecked()? "true": "false") + "&auth=" +
                MainActivity.getHash(username + part_id + "got_lunch" +
                        (((CheckBox) findViewById(R.id.lunch)).isChecked()? "true": "false") +
                        password).toLowerCase();
        Thread net = new Thread() {
            public void run() {
                response = MainActivity.performPostCall(url, new HashMap<String, String>());
            }
        };
        net.start();

        try {
            net.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
