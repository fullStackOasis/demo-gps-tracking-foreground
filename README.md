# Android GPS tracker

This is a simple Android app written in Java that lets you track your GPS history. It's a proof of concept that allows you to open an app, start storing GPS coordinates, then minimize the app and use your mobile device for other things (phone calls, texting, and so on). This app won't be shut down because it runs a "[foreground service](https://developer.android.com/reference/android/app/Service)". Quoting the documentation:
The "system considers [the app] to be something the user is actively aware of and thus not a candidate for killing when low on memory. (It is still theoretically possible for the service to be killed under extreme memory pressure from the current foreground application, but in practice this should not be a concern.)"

Latitude, longitude pairs are stored and displayed in a list. The list can be as long as you want it to be. In this demo, it's 250 points.

You can click a lat+long pair in the displayed list, when the app is opened, and you'll get taken to a Google Map via "deep linking".

This is just a proof of concept! That means there are some things that aren't done "properly". In particular, data storage isn't handled well. The foreground service is in charge of a queue of GPS coordinates. If that service ends, your data goes poof. The UI is really very simple and could use additional work; it might be wise to replace this with a `RecyclerView`. But for now, the app is working fine for me.

The app will break when you first add it to Android Studio, because it needs a png file. You can add your own png, and that should fix the issue.
