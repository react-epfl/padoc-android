package com.react.gabriel.wbam.padoc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;

/**
 * Created by gabriel on 03/06/16.
 */
public class Monitor extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        //TODO : need to stop service advertising only instead of whole WiFi

//        PadocManager padocManager = rootIntent.getParcelableExtra("padocManager");
//
//        padocManager.forceStopWifiDirectService();

//        WifiP2pManager wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        WifiP2pManager.Channel mChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
//
//        wifiP2pManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
//            public void onSuccess() {
//                System.out.println("PADOC is OFF");
//            }
//
//            public void onFailure(int reason) {
//                System.out.println("Error: Could not clear PADOC Service");
//            }
//        });

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);


        stopSelf();
    }
}
