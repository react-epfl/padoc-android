package com.react.gabriel.wbam.padoc.wifidirect;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;

import com.react.gabriel.wbam.MainActivity;

import java.util.Map;

/**
 * Created by gabriel on 25/05/16.
 */
public class WifiDirectDiscovery {

    private final MainActivity mActivity;
    private final WifiP2pManager mManager;
    private final Channel mChannel;
    private final WifiDirectManager wdManager;

    private boolean discoveryIsRunning = false;

    public WifiDirectDiscovery(MainActivity mActivity, WifiDirectManager wdManager){

        this.mActivity = mActivity;
        this.mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        this.mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);
        this.wdManager = wdManager;

    }

    //Scanner handler
    private Handler discoveryHandler = new Handler();
    //TODO: This number shouldn't be a constant.
    private final int DELAY = 5000;

    //Scanner runnable
    private Runnable runDiscovery = new Runnable() {
        @Override
        public void run() {
            scanOnce();
            discoveryHandler.postDelayed(this, DELAY);
        }
    };

    public void startDiscovery(){
        if(!discoveryIsRunning){
//            runDiscovery.run();
            discoveryIsRunning = true;
            mActivity.debugPrint("Started Service discovery");
            scanOnce();
            discoveryHandler.postDelayed(runDiscovery, DELAY);
        }else{
            mActivity.debugPrint("Error: Discovery is already running");
        }
    }

    /**
     * Remember to call mManager.clearServiceRequest() before calling this function
     */
    public void scanOnce() {

        mManager.clearServiceRequests(mChannel, null);

        WifiP2pManager.DnsSdTxtRecordListener DnsSdTextRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                //TODO: handle discovered devices
                mActivity.debugPrint("Discovered : " + device.deviceName);
//                if (DBG) mActivity.debugPrint("ssid : " + record.get("ssid"));
                if(fullDomain.contains("padoc") && record.get(WifiDirectManager.BTMAC)!=null){
//                    if (DBG) mActivity.debugPrint("Attempting connection to : " + record.get("ssid") + "|"+record.get("key"));

                    wdManager.handleNewWifiDirectDiscovery((String)record.get(WifiDirectManager.BTMAC));
                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, null, DnsSdTextRecordListener);
        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();

        //We add the serviceRequest
        mManager.addServiceRequest(mChannel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // We finally start discovering services

                mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mActivity.debugPrint("Scanning...");
                    }

                    @Override
                    public void onFailure(int code) {
                        // Discovery failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        mActivity.debugPrint("Error: Scan failed");
                    }
                });
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                mActivity.debugPrint("Error: addServiceRequest failed");
            }
        });

    }

    /**
     * When using a custom actionListener do not forget to set discoveryIsRunning
     * @param actionListener
     */
    public void stopDiscovery(WifiP2pManager.ActionListener actionListener) {
        if(discoveryIsRunning){

            //First stop handler
            discoveryHandler.removeCallbacksAndMessages(null);

            //Then clear service requests
            if (actionListener != null){
                mManager.clearServiceRequests(mChannel, actionListener);
            }else {
                mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        discoveryIsRunning = false;
                        mActivity.debugPrint("Service discovery is OFF");
                    }

                    public void onFailure(int reason) {
                        discoveryIsRunning = true;
                        mActivity.debugPrint("Stopping service discovery failed, error code " + reason);
                    }
                });
            }
        }else {
            mActivity.debugPrint("Service discovery is already OFF");
        }
    }
}