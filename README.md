# Padoc

Automatic peer-to-peer multihop library for Android

To use in your project add the following to the manifest file :


<manifest ...>

    <uses-sdk android:minSdkVersion="19" />

    <uses-permission android:name="android.permission.INTERNET" />

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />


    <application...>

        <service android:name=".padoc.PadocMonitor" android:stopWithTask="false" />
        <service android:name=".padoc.messaging.multicast.MulticastReceiverService"/>

        ...
        
    </application>

</manifest>

To instantiate, simply implement the PadocInterface and initialize the Padoc object with a reference to it as well as a the application context:

Padoc padoc = new Padoc(padocInterface, getApplicationContext());
