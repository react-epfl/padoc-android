package com.react.gabriel.wbam.padoc;

import android.util.Pair;

import com.react.gabriel.wbam.padoc.bluetooth.BluetoothManager;
import com.react.gabriel.wbam.padoc.bluetooth.ConnectedThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 18/05/16.
 */
public class Router {

    public final int ADDRESS_UNKNOWN = -1;
    private final String localAddress;

    //here are the devices that connected through the server thread and still need to be identified
    private Set<ConnectedThread> orphanDevices = null;
    //connectedDevices<macAddress, connectedThread>
    private Map<String, ConnectedThread> connectedDevices = null;
    //route<destinationAddress, <hops, routingAddress>>
    private Map<String, Pair<Integer, String>> route = null;

    public Router(String localAddress){

        this.localAddress = localAddress;
        this.connectedDevices = new HashMap<String, ConnectedThread>();
        this.orphanDevices = new HashSet<>();
        this.route = new HashMap<String, Pair<Integer, String>>();

    }

    public boolean threadIsConsideredOrphan(ConnectedThread thread){
        return orphanDevices.contains(thread);
    }

    public void identifyOrphanThread(ConnectedThread connectedThread, String remoteAddress){
        if(threadIsConsideredOrphan(connectedThread)){

            connectedThread.setRemoteAddress(remoteAddress);
            connectedDevices.put(remoteAddress, connectedThread);

            setRoute(remoteAddress, 0, remoteAddress);
            orphanDevices.remove(connectedThread);

        }else{
            System.out.println("Error, thread is not considered orphan");
        }
    }

    public boolean knows(String remoteAddress){

        return route.containsKey(remoteAddress);
    }

    public int getHopsFor(String remoteAddress){
        if(route.containsKey(remoteAddress)){
            return route.get(remoteAddress).first;
        }else {
            return ADDRESS_UNKNOWN;
        }
    }

    public Pair<Integer, String> setRoute(String destinationAddress, int hops, String routeAddress){

        return route.put(destinationAddress, new Pair<Integer, String>(hops, routeAddress));
    }

    public ConnectedThread getRoutingThreadFor(String address){

        String routingAddress = route.get(address).second;
        return connectedDevices.get(routingAddress);
    }

    public ConnectedThread setConnectedDevice(String remoteAddress, ConnectedThread connectedThread){

        setRoute(remoteAddress, 0, remoteAddress);
        return connectedDevices.put(remoteAddress, connectedThread);
    }

    public boolean setOrphanThread(ConnectedThread connectedThread) {

        return orphanDevices.add(connectedThread);
    }

    public Set<Map.Entry<String, ConnectedThread>> getConnectedEntries(){
        return connectedDevices.entrySet();
    }

    public Collection<ConnectedThread> getConnectedThreads(){
        return connectedDevices.values();
    }

    public Set<Map.Entry<String, Pair<Integer, String>>> getKnownPeers(){
        return route.entrySet();
    }

    public String getRoutingAddressFor(String address){
        return route.get(address).second;
    }

}
