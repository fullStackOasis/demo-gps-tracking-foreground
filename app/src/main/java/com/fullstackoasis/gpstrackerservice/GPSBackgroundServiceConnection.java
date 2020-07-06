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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class GPSBackgroundServiceConnection implements ServiceConnection {
    private static String TAG = GPSBackgroundServiceConnection.class.getCanonicalName();
    private Messenger mService;
    private Messenger activityMessenger;
    public GPSBackgroundServiceConnection(Messenger activityMessenger) {
        this.activityMessenger = activityMessenger;
    }
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the service object we can use to
        // interact with the service.  We are communicating with our
        // service through an IDL interface, so get a client-side
        // representation of that from the raw service object.
        Messenger mService = new Messenger(service);
        Log.d(TAG, "Attached.");

        // We want to monitor the service for as long as we are
        // connected to it.
        try {
            Message msg = Message.obtain(null,
                    GPSBackgroundService.MSG_REGISTER_CLIENT);
            msg.replyTo = activityMessenger;
            mService.send(msg);

            // Give it some value as an example.
            msg = Message.obtain(null,
                    GPSBackgroundService.MSG_SET_VALUE, this.hashCode(), 0);
            mService.send(msg);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }

        Log.d(TAG, "Connected");
    }

    /**
     * Use this to deliberately disconnect. Not part of the API.
     */
    public void disconnect() {
        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
            try {
                Message msg = Message.obtain(null,
                        GPSBackgroundService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = activityMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
        }

        Log.d(TAG, "Attempted to disconnect.");
    }

    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mService = null;
        Log.d(TAG, "Disconnected.");

    }

    @Override
    public void onBindingDied(ComponentName name) {

    }

    @Override
    public void onNullBinding(ComponentName name) {

    }
}
