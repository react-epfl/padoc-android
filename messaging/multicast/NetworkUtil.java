package com.react.gabriel.wbam.padoc.messaging.multicast;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by gabriel on 10/07/16.
 */
public class NetworkUtil {

    private static final String TAG = NetworkUtil.class.getSimpleName();
    private static final String MULTICAST_GROUP_IP = "239.255.1.1";
    private static final String NETWORK_INTERFACE_NAME = "p2p-wlan0-0";
    private static final String ALTERNATE_NETWORK_INTERFACE_NAME = "p2p-p2p0";
    private static final int PORT = 40000;

    public static int getPort() {
        return PORT;
    }

    public static InetAddress getMulticastGroupAddress() throws UnknownHostException {
        return InetAddress.getByName(MULTICAST_GROUP_IP);
    }

    public static NetworkInterface getNetworkInterface(boolean isParent){
        if(isParent){
            return getWifiP2pNetworkInterface();
        }else{
            return getWlanEth();
        }
    }

    private static NetworkInterface getWlanEth() {
        Enumeration<NetworkInterface> enumeration = null;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        NetworkInterface wlan0 = null;
        StringBuilder sb = new StringBuilder();
        while (enumeration.hasMoreElements()) {
            wlan0 = enumeration.nextElement();
            sb.append(wlan0.getName() + " ");
            if (wlan0.getName().equals("wlan0")) {
                //there is probably a better way to find ethernet interface
                Log.i(TAG, "wlan0 found");
                return wlan0;
            }
        }

        return null;
    }

    private static NetworkInterface getWifiP2pNetworkInterface() {
        Enumeration<NetworkInterface> networkInterfaceEnumeration = null;
        try {
            networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            if (isWifiP2pInterface(networkInterface)) {
                return networkInterface;
            }
        }
        return null;
    }

    private static boolean isWifiP2pInterface(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp() && (networkInterface.getDisplayName().equals(NETWORK_INTERFACE_NAME) || networkInterface.getDisplayName().contains(ALTERNATE_NETWORK_INTERFACE_NAME));
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getMyIpAddress(boolean isParent){
        if(isParent){
            return getMyWifiP2pIpAddress();
        }else {
            return getWifiIpAddress();
        }
    }

    private static String getMyWifiP2pIpAddress() {
        Enumeration<InetAddress> inetAddressEnumeration = getWifiP2pNetworkInterface().getInetAddresses();
        while (inetAddressEnumeration.hasMoreElements()) {
            InetAddress inetAddress = inetAddressEnumeration.nextElement();
//                if (isIpv4Address(inetAddress)) {
            Log.e(TAG, "Wifi P2P address is : " + inetAddress.getHostAddress().toString());
            return inetAddress.getHostAddress().toString();
//                }
        }
        return null;
    }

    private static String getWifiIpAddress() {

        Enumeration<InetAddress> inetAddressEnumeration = getWlanEth().getInetAddresses();
        while (inetAddressEnumeration.hasMoreElements()) {
            InetAddress inetAddress = inetAddressEnumeration.nextElement();
//                if (isIpv4Address(inetAddress)) {
            Log.e(TAG, "Wifi P2P address is : " + inetAddress.getHostAddress().toString());
            return inetAddress.getHostAddress().toString();
//                }
        }

        return null;
    }

}
