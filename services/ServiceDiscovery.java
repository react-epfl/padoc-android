package com.react.gabriel.wbam.padoc.services;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.text.Html;

import com.react.gabriel.wbam.R;
import com.react.gabriel.wbam.padoc.Padoc;

import java.util.Map;

/**
 * Created by gabriel on 27/06/16.
 */
public class ServiceDiscovery {

    private final Padoc padoc;
    private final Resources resources;
    private final WifiP2pManager p2pManager;
    private final WifiP2pManager.Channel mChannel;

    private WifiP2pDnsSdServiceRequest request;

    public ServiceDiscovery(Padoc padoc){

        this.padoc = padoc;
        this.resources = padoc.getContext().getResources();
        this.p2pManager = (WifiP2pManager) padoc.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        this.mChannel = p2pManager.initialize(padoc.getContext(), padoc.getContext().getMainLooper(), null);

    }

    //Scanner handler
    private Handler discoveryHandler = new Handler();
    //TODO: This number shouldn't be a constant.

    private final int DISCOVERY_INTERVAL = 15000;

    //Scanner runnable
    private Runnable runDiscovery = new Runnable() {
        @Override
        public void run() {
            scanOnce();
            discoveryHandler.postDelayed(this, DISCOVERY_INTERVAL);
        }
    };

    public void startDiscovery(){
        scanOnce();
        discoveryHandler.postDelayed(runDiscovery, DISCOVERY_INTERVAL);
    }

    /**
     * Remember to call mManager.clearServiceRequest() before calling this function
     */
    public void scanOnce() {

        p2pManager.clearServiceRequests(mChannel, null);

        WifiP2pManager.DnsSdTxtRecordListener DnsSdTextRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {

                if(fullDomain.contains("padoc")){

                    String meshUUID = (String) record.get(resources.getString(R.string.MESH_UUID_VALUE));
                    int randomValue = Integer.valueOf((String) record.get(resources.getString(R.string.RANDOM_VALUE)));

                    String ssid = (String) record.get(resources.getString(R.string.SSID_VALUE));
                    String pass = (String) record.get(resources.getString(R.string.PASS_VALUE));

                    String btmac = (String) record.get(resources.getString(R.string.BTMAC_VALUE));

                    padoc.handleNewPadocDiscovery(meshUUID, randomValue, ssid, pass, btmac);
                }
            }
        };

        p2pManager.setDnsSdResponseListeners(mChannel, null, DnsSdTextRecordListener);
        request = WifiP2pDnsSdServiceRequest.newInstance();

        //We add the serviceRequest
        p2pManager.addServiceRequest(mChannel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // We finally start discovering services

                p2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
//                        mActivity.debugPrint("Scanning...");
                    }

                    @Override
                    public void onFailure(int code) {
                        // Discovery failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                        padoc.print("Error: Scan failed");
                    }
                });
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                padoc.print("Error: addServiceRequest failed");
            }
        });

    }

    /**
     * When using a custom actionListener do not forget to set discoveryIsRunning
     * @param actionListener
     */
    public void stopDiscovery(WifiP2pManager.ActionListener actionListener) {
        //First stop handler
        discoveryHandler.removeCallbacksAndMessages(null);
//        p2pManager.clearServiceRequests(mChannel, actionListener);
        p2pManager.removeServiceRequest(mChannel, request, null);
        request = null;
    }
}