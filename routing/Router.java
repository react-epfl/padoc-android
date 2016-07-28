package com.react.gabriel.wbam.padoc.routing;

import com.react.gabriel.wbam.padoc.connection.BluetoothConnectedThread;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;
import com.react.gabriel.wbam.padoc.connection.WifiConnectedThread;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 05/07/16.
 */
public interface Router{

    void handleNewConnection(ConnectedThread connectedThread);

    boolean isConnectedTo(String remoteAddress);

    boolean knows(String remoteAddress);

    boolean hasLeafs();

    boolean hasSiblings();

    int getNumberOfWifiConnections();

    int getNumberOfBluetoothConnections();

    Set<String> getPeerAddresses();

    Set<Map.Entry<String, String>> getPeers();

    String getPeerName(String address);

    String getRoutingAddressFor(String address);

    ConnectedThread getRoutingThreadFor(String address);

    void setRoute(String destinationAddress, String routingAddress, String name, int hops);

    int getHopsFor(String address);

    Set<Map.Entry<String, BluetoothConnectedThread>> getConnectedSiblings();

    Collection<BluetoothConnectedThread> getConnectedSiblingsThreads();

    Set<Map.Entry<String, WifiConnectedThread>> getConnectedLeafs();
}