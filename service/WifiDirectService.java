package com.react.gabriel.wbam.padoc.service;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;

import com.react.gabriel.wbam.MainActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gabriel on 25/05/16.
 */
public class WifiDirectService {

    private final MainActivity mActivity;
    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;

    private boolean serviceIsRunning = false;

    public WifiDirectService(MainActivity mActivity){

        this.mActivity = mActivity;
        this.mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        this.mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);

    }

    /**
     * When specifying custom actionListener, do not forget to set serviceIsRunning
     * @param actionListener
     */
    public void startService(String btName, String btMAC, WifiP2pManager.ActionListener actionListener) {
        if(!serviceIsRunning){
            Map<String, String> values = new HashMap<String, String>();
            //http://files.dns-sd.org/draft-cheshire-dnsext-dns-sd.txt | Sections 6.5 & 6.6
            values.put(WifiDirectManager.BTMAC, btMAC);
            values.put(WifiDirectManager.BTNAME, btName);

            WifiP2pServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("PADOC", "_http._tcp", values);

            if(actionListener != null){
                mManager.addLocalService(mChannel, serviceInfo, actionListener);
            }else{
                mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        serviceIsRunning = true;
                        mActivity.debugPrint("PADOC service added successfully");
                    }
                    @Override
                    public void onFailure(int reasonCode) {
                        serviceIsRunning = false;
                        mActivity.debugPrint("Error: PADOC service addition failed");
                    }
                });
            }
        }else{
            mActivity.debugPrint("Error: PADOC Service already ON");
        }
    }

    /**
     * When using a custom actionListener do not forget to set serviceIsRunning
     * @param actionListener
     */
    public void stopService(WifiP2pManager.ActionListener actionListener) {
        if(serviceIsRunning) {
            if(actionListener != null){
                mManager.clearLocalServices(mChannel, actionListener);
            }else{
                mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        serviceIsRunning = false;
                        mActivity.debugPrint("PADOC service is OFF");
                    }

                    public void onFailure(int reason) {
                        serviceIsRunning = true;
                        mActivity.debugPrint("Error: Could not clear local PADOC Service");
                    }
                });
            }
        }else {
            mActivity.debugPrint("Error: PADOC Service is already OFF");
        }
    }

    public void forceStopService(){
        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                serviceIsRunning = false;
                mActivity.debugPrint("PADOC is OFF");
            }

            public void onFailure(int reason) {
                serviceIsRunning = true;
                mActivity.debugPrint("Error: Could not clear PADOC Service");
            }
        });
    }

    public void setServiceIsRunning(boolean isRunning){
        this.serviceIsRunning = isRunning;
    }
}
