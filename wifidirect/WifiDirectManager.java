package com.react.gabriel.wbam.padoc.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.PadocManager;

import java.util.Map;

/**
 * Created by gabriel on 25/05/16.
 */
public class WifiDirectManager extends BroadcastReceiver {

    public static final String BTMAC = "btmac";

    private final MainActivity mActivity;
    private PadocManager padocManager;
    private final WifiP2pManager mManager;
    private final WifiDirectService wdService;
    private final WifiDirectDiscovery wdDiscovery;

    private boolean wifiDirectEnabled = false;

    public WifiDirectManager(MainActivity mActivity, PadocManager padocManager){

        this.mActivity = mActivity;
        this.padocManager = padocManager;
        this.mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        this.wdService = new WifiDirectService(mActivity);
        this.wdDiscovery = new WifiDirectDiscovery(mActivity, this);

    }

    public void onReceive(Context context, Intent intent){

        String action = intent.getAction();

//        if (DBG) mActivity.debugPrint("=== "+ action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Have access to: EXTRA_WIFI_STATE
            // Constant Value: "android.net.wifi.p2p.STATE_CHANGED"

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                mActivity.debugPrint("WiFi-Direct ENABLED");
                wifiDirectEnabled = true;
            } else {
                // Wi-Fi P2P is not enabled
                mActivity.debugPrint("Error: WiFi-Direct DISABLED");
                wifiDirectEnabled = false;
                //TODO: Need to enable WiFi. and maybe change AMAPisON?
            }
        }
    }

    public void handleNewAMAPDevice(WifiP2pDevice device, Map record){
        //TODO: can I send a BluetoothDevice object through the record? I think yes.

        //And connect to the device if chosen

    }

    public void startService() {
        String btAddress = padocManager.getLocalBluetoothAddress();
        if(btAddress != null){
            wdService.startService(btAddress, null);
        }else {
            padocManager.debugPrint("ERROR : local Bluetooth address is missing!");
        }
    }

    public void stopService(){
        wdService.stopService(null);
    }

    public void startDiscovery(){
        wdDiscovery.startDiscovery();
    }

    public void stopDiscovery(){
        wdDiscovery.stopDiscovery(null);
    }

    public void handleNewWifiDirectDiscovery(String btMacAddress){
        padocManager.registerNewBluetoothAddress(btMacAddress);
    }
}
