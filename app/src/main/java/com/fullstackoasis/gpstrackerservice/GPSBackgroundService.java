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

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class GPSBackgroundService extends Service {
    private static String TAG = GPSBackgroundService.class.getCanonicalName();
    private LatLngQueue<LatLngPojo> q =
            new LatLngQueue<LatLngPojo>(250);

    public void addLatLng(LatLngPojo latLong) {
        q.add(latLong);
        sendMessage(q);
    }

    /** Keeps track of all current registered clients. See
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    private ArrayList<Messenger> clients = new ArrayList<Messenger>();
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    static final int MSG_SET_VALUE = 3;
    private int value; // for message arg1
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from clients.
     * https://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    GPSBackgroundService.this.sendMessage(GPSBackgroundService.this.q);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        try {
                            clients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, value, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            clients.remove(i);
                        }
                    }
                    break;
                case MSG_SET_VALUE:
                    value = msg.arg1;
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        try {
                            clients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, value, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            clients.remove(i);
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public static final String ID_CHANNEL = "AccelerometerChannel";
    private static final int ONGOING_NOTIFICATION_ID = 29;
    public static final int CHANGED = 1;
    private int startId;
    // This debug flag is only here for testing to see if this service has been started as a
    // foreground service. If so, a log message is printed periodically.
    private static boolean DEBUG = false;
    private Handler handler;
    private IBinder binder;
    private static int N_MOCKED_POINTS = 260;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GPSBackgroundService.onCreate");
        binder = new LocalBinder();
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Ooops returning");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        Log.d(TAG, "onCreate, mocking data");
        if (DEBUG) {
            for (int i = 0; i < N_MOCKED_POINTS; i++) {
                double wiggle = Math.random();
                LatLngPojo x = new LatLngPojo(37.7749 + wiggle,
                        -122.4192 + wiggle);
                q.add(x);
            }
        }
        // For debug only
        if (DEBUG) {
            handler = new Handler();
            startRepeatingTask();
        }
    }

    /**
     * WARNING! ONLY USE THIS IN DEBUG MODE.
     */
    void startRepeatingTask() {
        updater.run();
    }

    void stopRepeatingTask() {
        handler.removeCallbacks(updater);
    }

    /**
     * WARNING! ONLY USE THIS IN DEBUG MODE.
     */
    Runnable updater = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG,
                        "is service running in foreground?? " + isServiceRunningInForeground(GPSBackgroundService.this,
                                GPSBackgroundService.class));
                // handleSensorChanged(null); TODO FIXME
            } finally {
                // Make sure this happens, even if exception is thrown
                handler.postDelayed(updater, 5000);
            }
        }
    };

    private int getNotificationIcon() {
        return R.drawable.logo_accelerometer_project;
    }

    /**
     * Get the PendingIntent that will open the app's home screen when a user clicks on the
     * Notification.
     * @return PendingIntent for home screen (MainActivity)
     */
    private PendingIntent getPendingIntentToMainActivity() {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return pendingIntent;
    }

    /**
     * This method does two things: (1) it starts the app as a foreground service in Android >
     * Build.VERSION_CODES.O. Also, it emits a notification. I am not 100% sure the notification
     * is needed, and it may be removed.
     * @param intent
     * @param flags
     * @param startId
     */
    private void notifyStartForeground(Intent intent, int flags, int startId) {
        Log.d(TAG, "is service running in foreground?? " + isServiceRunningInForeground(this,
                GPSBackgroundService.class));
        Log.d(TAG,
                "Build.VERSION.SDK_INT " + Build.VERSION.SDK_INT + ", Build.VERSION_CODES.O "+ Build.VERSION_CODES.O );

        PendingIntent pendingIntent =  getPendingIntentToMainActivity();//PendingIntent
        // .getActivity(this, 0, intent, 0);

        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = null;
        // The id of the channel.

        int importance;

        // Dev note: You must create Builder with a non-null idChannel, otherwise notification
        // does not happen.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ID_CHANNEL);
        builder.setContentTitle(this.getString(R.string.app_name))
                .setSmallIcon(getNotificationIcon())
                .setContentIntent(pendingIntent)
                .setColorized(false)
                .setContentText(getString(R.string.notification_message));
        int color = ContextCompat.getColor(this, R.color.colorAccent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // https://github.com/android/user-interface-samples/tree/master/Notifications
            // Notification Channels arrived in SDK 29.
            // VERSION_CODES.O is 26.
            // "Notification Channel Id is ignored for Android pre O (26)."

            Log.d(TAG, "Here");
            // With SDK 26 and after, use IMPORTANCE_HIGH, setImportance, not setPriority
            // Prior to SDK 26, use Notification.PRIORITY_HIGH for example.
            // Says use at least PRIORITY_LOW for foreground apps.
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            mChannel = new NotificationChannel(ID_CHANNEL, getString(R.string.app_name), importance);
            // Configure the notification channel.
            mChannel.setDescription(getString(R.string.notification_message));
            mChannel.enableLights(true);
            mChannel.setShowBadge(true);
            mChannel.setLightColor(color);
            mChannel.setVibrationPattern(new long[]{100});
            try {
                mNotificationManager.createNotificationChannel(mChannel);
                //startForegroundService(intent);
                Notification notification = builder.build();
                // DO NOT DO THIS! You wind up with a double bling sound, which is overkill.
                //mNotificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
                startForeground(ONGOING_NOTIFICATION_ID, notification);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.d(TAG, "There");
            // Prior to SDK 26, use Notification.PRIORITY_HIGH for example.
            // Says use at least PRIORITY_LOW for foreground apps.
            importance = Notification.PRIORITY_DEFAULT;
            // https://developer.android.com/reference/android/app/Notification.Builder#setPriority
            builder.setPriority(importance)
                    .setColor(color)
                    .setVibrate(new long[]{100})
                    .setLights(Color.YELLOW, 500, 5000)
                    .setAutoCancel(true);
            mNotificationManager.notify(ONGOING_NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GPSBackgroundService.onStartCommand");
        Log.d(TAG, "onStartCommand got startId " + startId);
        if (intent != null) {
            Log.d(TAG, "onStartCommand got intent action " + intent.getAction());
        } else {
            Log.d(TAG, "onStartCommand intent was null");
        }
        Log.d(TAG, "onStartCommand got intent flags " + flags);
        this.startId = startId;
        if (!isServiceRunningInForeground(GPSBackgroundService.this,
                GPSBackgroundService.class)) {
            notifyStartForeground(intent, flags, startId);
        }
        return Service.START_STICKY;
    }

    public void sendMessage(LatLngQueue<LatLngPojo> q) {
        if (DEBUG) Log.d(TAG, "Got message to send message... " + clients.size());
        for (int i = clients.size() - 1; i >= 0; i--) {
            try {
                if (DEBUG) Log.d(TAG, "Service sending q ");
                clients.get(i).send(Message.obtain(null, MSG_SET_VALUE, q));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                clients.remove(i);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (DEBUG) {
            stopRepeatingTask();
        }
    }

    /**
     * Do not allow binding, always return null.
     * "You must always implement this method; however, if you don't want to allow binding, you
     * should return null."
     * https://developer.android.com/guide/components/services#Basics
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        GPSBackgroundService getService() {
            // Return this instance of AccelerometerSensorService so clients can call public methods
            return GPSBackgroundService.this;
        }
    }
    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
