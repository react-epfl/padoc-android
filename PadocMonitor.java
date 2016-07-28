package com.react.gabriel.wbam.padoc;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import java.util.List;

/**
 * Created by gabriel on 03/06/16.
 */
public class PadocMonitor extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        //TODO : need to stop service advertising only instead of whole WiFi

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //forget wifi networks
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

        if(list != null){

            for( WifiConfiguration i : list ) {
                wifiManager.removeNetwork(i.networkId);
                wifiManager.saveConfiguration();
            }
        }

        //Disable wifi before leaving
        wifiManager.setWifiEnabled(false);

        //Disable bluetooth before leaving
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.disable();

        stopSelf();
    }
}
