package com.react.gabriel.wbam.padoc.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.PadocManager;

/**
 * Created by gabriel on 25/05/16.
 */
public class WifiDirectManager extends BroadcastReceiver {

    public static final String BTMAC = "btmac";
    public static final String BTNAME = "btname";

    public enum State {
        STATE_NULL,
        STATE_WIFI_P2P_RESETTING,
        STATE_WIFI_P2P_STARTING,
        STATE_WIFI_P2P_ENABLED,
        STATE_SERVICE_REGISTERED,
        STATE_DISCOVERY_ON,
        STATE_RUNNING;
    }

    private State state = State.STATE_NULL;

    private boolean isRunning = false;
    private final MainActivity mActivity;
    private PadocManager padocManager;
    private final WifiManager wifiManager;
    private final WifiDirectService wdService;
    private final WifiDirectDiscovery wdDiscovery;

    public WifiDirectManager(MainActivity mActivity, PadocManager padocManager){

        this.mActivity = mActivity;
        this.padocManager = padocManager;

        this.wifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) {
            //Device does not support Wi-Fi

            mActivity.debugPrint("ERROR : Device does not support Wi-Fi.");
            this.wdService = null;
            this.wdDiscovery = null;

        }else {
            //Device supports Wi-Fi

            //Make sure Wifi-Direct is on
//            this.wifiManager.setWifiEnabled(true);

            //Initialize the service object
            this.wdService = new WifiDirectService(mActivity);

            //Initialize the discovery object
            this.wdDiscovery = new WifiDirectDiscovery(mActivity, this);

//            this.isRunning = true;
        }

    }

    public void initialize(){

        switch(state) {
            case STATE_NULL :

                if(!wifiManager.isWifiEnabled()) {
                    //Wifi is not enabled

                    if(wifiManager.setWifiEnabled(true)){
                        state = State.STATE_WIFI_P2P_STARTING;
                    }else {
                        mActivity.debugPrint("ERROR : Could not turn on WiFi");
                    }
                }else {
                    //TODO : check if a reset is necessary
                    //Sometimes discovery does not work, resetting wifi solves it. for now..

                    if(wifiManager.setWifiEnabled(false)){
                        state = State.STATE_WIFI_P2P_RESETTING;
                    }else {
                        mActivity.debugPrint("ERROR : Could not turn off WiFi.");
                    }
                }

                break;
            case STATE_WIFI_P2P_ENABLED :

                WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        wdService.setServiceIsRunning(true);
                        mActivity.debugPrint("PADOC service added successfully");
                        state = State.STATE_SERVICE_REGISTERED;
                        initialize();
                    }

                    @Override
                    public void onFailure(int reason) {
                        wdService.setServiceIsRunning(false);
                        mActivity.debugPrint("ERROR : PADOC service failed");
//                        initialize();
                    }
                };

                startService(actionListener);

                break;
            case STATE_SERVICE_REGISTERED :

                startDiscovery();

                this.state = State.STATE_RUNNING;

                padocManager.initialize();

                break;
            case STATE_RUNNING :

                mActivity.debugPrint("Wifi service and discovery is running.");

                padocManager.initialize();

                break;
        }

    }

    public void onReceive(Context context, Intent intent){

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Have access to: EXTRA_WIFI_STATE
            // Constant Value: "android.net.wifi.p2p.STATE_CHANGED"

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                mActivity.debugPrint("WiFi-Direct ENABLED");

                if(this.state.equals(State.STATE_WIFI_P2P_STARTING)) {
                    this.state = State.STATE_WIFI_P2P_ENABLED;
                    initialize();
                }
            } else {
                // Wi-Fi P2P is not enabled
                mActivity.debugPrint("WiFi-Direct DISABLED");

                if(this.state.equals(State.STATE_WIFI_P2P_RESETTING)){
                    if(wifiManager.setWifiEnabled(true)) {
                        this.state = State.STATE_WIFI_P2P_STARTING;
                    }else {
                        mActivity.debugPrint("ERROR : Could not enable WiFi.");
                    }
                }
            }
        }
    }

    public void startService(WifiP2pManager.ActionListener actionListener) {
        String btAddress = padocManager.getLocalBluetoothAddress();
        String btName = padocManager.getLocalName();
        if(btAddress != null){
            wdService.startService(btName, btAddress, actionListener);
        }else {
            padocManager.debugPrint("ERROR : local Bluetooth address is missing!");
        }
    }

    public void stopService(){
        wdService.stopService(null);
    }

    public void forceStopService(){
        wdService.forceStopService();
    }

    public void startDiscovery(){
        wdDiscovery.startDiscovery();
    }

    public void stopDiscovery(){
        wdDiscovery.stopDiscovery(null);
    }

    public void stopWifi(){
        wifiManager.setWifiEnabled(false);
    }

    public void handleNewWifiDirectDiscovery(String name, String btMacAddress){
        //TODO : Need to try connecting directly with the btMacAddress, without bluetooth discovery
        padocManager.handleNewWifiDirectDiscovery(name, btMacAddress);
    }

    //Getters

    public State getState(){
        return this.state;
    }

    public void setState(State state){
        this.state = state;
    }

    public boolean isRunning(){
        return this.isRunning;
    }
}
