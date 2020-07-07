/**
 * Copyright 2020 Marya Doery
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.fullstackoasis.gpstrackerservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getCanonicalName();
    // ACCESS_FINE_LOCATION_GRANTED has been granted
    private static final int ACCESS_FINE_LOCATION_GRANTED=2;
    // If you want deep links turned on, set DEEP_LINK to true.
    // Then if you click a lat+long pair, you'll be taken to google maps.
    private static boolean DEEP_LINK = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();

    }

    private void performAction() {

    }

    /**
     * See https://developer.android.com/training/permissions/requesting.html
     */
    private void checkPermissions() {
        // Developer note: Just calling ContextCompat.checkSelfPermission
        // does not result in any popup window asking for permissions.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d(TAG, "request ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_GRANTED);
        } else {
            Log.i(TAG, "request done, searching for location");
            // Yay, we have all permissions. NOW we can actually request Location updates TYVM.
            //locationObservable.beginLocatingDevice();
            startGPSBackgroundService();
        }
    }

    /**
     * See https://developer.android.com/training/permissions/requesting.html
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult method is called");
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_GRANTED :
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    startGPSBackgroundService();

                } else {
                    checkPermissions(); // request permissions if needed.
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i(TAG, "This app can't do anything without Location permissions");
                    Toast.makeText(this, "This app can't do anything without Location " +
                            "permissions!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case GPSBackgroundService.MSG_SET_VALUE:
                    LatLngQueue<LatLngPojo> q =
                            (LatLngQueue<LatLngPojo>)msg.obj;
                    if (q != null) {
                        Log.d(TAG, "q was not null... remove all text views");
                        removeAllTextViews();
                        int counter = 1;
                        Log.d(TAG, "q was not null... add all text views");
                        for (LatLngPojo item : q) {
                            addItem(counter, item);
                            counter++;
                        }
                    }
                    break;
                default:
                    Log.d(TAG, "Received from service: but not sure what");
                    super.handleMessage(msg);
            }
        }
    }

    private void removeAllTextViews() {
        LinearLayout ll = findViewById(R.id.linearLayout);
        ll.removeAllViews();
    }
    private void addItem(int counter, LatLngPojo item) {
        TextView tv = new TextView(this);
        final String latLngPair = item.getLat() + "," + item.getLng();
        final String latLngText = "(" + counter + ") " + latLngPair;
        Log.d(TAG, "addItem: " + latLngText);
        tv.setText(latLngText);
        float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,4F,
                this.getResources().getDisplayMetrics());
        tv.setPadding(4, 30, 4, 30);
        tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tv.setTextSize(myTextSize);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (DEEP_LINK) {
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doDeepLink(latLngPair);
                }
            });
        }
        LinearLayout ll = findViewById(R.id.linearLayout);
        ll.addView(tv);
    }

    private void doDeepLink(String latLngText) {
        Log.d(TAG, "Deep linking with text " + latLngText);
        Uri gmmIntentUri = Uri.parse("geo:" + latLngText);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private GPSBackgroundServiceConnection mConnection =
            new GPSBackgroundServiceConnection(mMessenger);
    private boolean bound = false;

    private void startGPSBackgroundService() {
        Intent i = new Intent(this, GPSBackgroundService.class);
        Log.d(TAG, "Going to start the background GPS service");
        startService(i);
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this,
                GPSBackgroundService.class), mConnection, Context.BIND_AUTO_CREATE);
        bound = true;
        Log.d(TAG, "StepGraphActivity.binding");
    }

    void doUnbindService() {
        if (bound && mConnection != null) {
            mConnection.disconnect();
            this.unbindService(mConnection);
        }
        bound = false;
        Log.d(TAG, "Unbinding.");
    }
    /**
     * Pauses graph when activity is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        doUnbindService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, GPSBackgroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Resumes graph when activity is resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
    }
}
