package com.react.gabriel.wbam.padoc;

import com.react.gabriel.wbam.padoc.connection.ConnectedThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 18/05/16.
 */
public class Router {

    public final int ADDRESS_UNKNOWN = -1;
    public final int MAX_MISSED_ACKS = 3;

    private PadocManager padocManager;

    //connectedDevices<peerAddress, connectedThread> These are the devices directly connected to us
    private Map<String, ConnectedThread> connectedDevices = null;
    //ackLog<peerAddress, receivedACK>;
    private Map<String, Boolean> ackLog;
    //ackMissCount<peerAddress, missedACKs>;
    private Map<String, Integer> ackMissCount;
    //routes<peerAddress, Set<possibleRoutes>>
    private Map<String, Set<String>> routes = null;
    //hops<peerAddress@routingAddress, hops>
    private Map<String, Integer> hops = new HashMap<String, Integer>();
    //shortestRoute<peerAddress, shortestRoutingAddress>
    private Map<String, String> shortestRoute = new HashMap<String, String>();
    //names<peerAddress, name>
    private Map<String, String> names = new HashMap<String, String>();

    public Router(PadocManager padocManager){

        this.padocManager = padocManager;
        this.connectedDevices = new HashMap<String, ConnectedThread>();
        this.routes = new HashMap<String, Set<String>>();
        this.hops = new HashMap<String, Integer>();
        this.ackLog = new HashMap<String, Boolean>();
        this.ackMissCount = new HashMap<String, Integer>();

    }

    public void createNewDirectConnection(ConnectedThread connectedThread, String remoteAddress, String name){

        connectedDevices.put(remoteAddress, connectedThread);
        setRoute(name, remoteAddress, 0, remoteAddress);

        ackLog.put(remoteAddress, true);

    }

    public void setRoute(String name, String destinationAddress, int hops, String routingAddress){

        String hopsKey = destinationAddress+"@"+routingAddress;

        if(routes.containsKey(destinationAddress)){
            //We already know this Peer, thus we have at least one routing address for this destination

            Set<String> gateways = routes.get(destinationAddress);

            if(gateways.contains(routingAddress) && this.hops.get(hopsKey) > hops){
                //This specific gateway is already known, but this time it has less hops.

                //update the number of hops for this specific route
                this.hops.put(hopsKey, hops);

                if(hopsForShortestRouteTo(destinationAddress) > hops){
                    //This new route is actually shorter than the previous shortest one.

                    //Update the shortest route
                    this.shortestRoute.put(destinationAddress, routingAddress);

                }
            }else if(!gateways.contains(routingAddress)){
                //We now have a new possible gateway for this destination

                gateways.add(routingAddress);
                this.hops.put(hopsKey, hops);

                if(hopsForShortestRouteTo(destinationAddress) > hops){
                    //This new gateway is actually the shortest one

                    //Update the shortest route
                    this.shortestRoute.put(destinationAddress, routingAddress);
                }

            }
        }else {
            //This is a new Peer, hence a new route

            Set<String> routingSet = new HashSet<String>();
            routingSet.add(routingAddress);

            routes.put(destinationAddress, routingSet);
            shortestRoute.put(destinationAddress, routingAddress);
            this.hops.put(hopsKey, hops);
            names.put(destinationAddress, name);

        }
    }

    public int hopsForShortestRouteTo(String destinationAddress){
        return hops.get(destinationAddress+"@"+shortestRoute.get(destinationAddress));
    }

    public boolean knows(String remoteAddress){

        return routes.containsKey(remoteAddress);
    }

    public int numberOfActiveConnections(){
        return connectedDevices.size();
    }

    public int getHopsFor(String remoteAddress){

        if(routes.containsKey(remoteAddress)){
            return hopsForShortestRouteTo(remoteAddress);
        }else {
            return ADDRESS_UNKNOWN;
        }
    }

    public ConnectedThread getRoutingThreadFor(String address){
        return connectedDevices.get(shortestRoute.get(address));
    }

    public Set<Map.Entry<String, ConnectedThread>> getConnectedEntries(){
        return connectedDevices.entrySet();
    }

    public Collection<ConnectedThread> getConnectedThreads(){
        return connectedDevices.values();
    }

    public Set<String> getPeers(){
        return routes.keySet();
    }

    public String getNameFor(String address){
        return names.get(address);
    }

    public Set<java.util.Map.Entry<String, String>> getPeerNames(){

        return names.entrySet();
    }

    public String getRoutingAddressFor(String address){
        return shortestRoute.get(address);
    }

    public boolean isConnectedTo(String btAddress){
        return connectedDevices.containsKey(btAddress);
    }

    public void requestACKs(String localAddress){

        for(ConnectedThread connectedThread : getConnectedThreads()){
            connectedThread.write(Message.getACKRequestMessage(localAddress, connectedThread.getRemoteAddress()));
        }

    }

    public void resetACKLog(){

        ackLog.clear();

        for (String address : connectedDevices.keySet()){

            ackLog.put(address, false);
        }

    }

    public void checkACKLog(){

        for(Map.Entry<String, Boolean> ackEntry : ackLog.entrySet()){

            String peer = ackEntry.getKey();

            if(!ackEntry.getValue()){
                //If we do not have the latest ACK for this peer

                if(MAX_MISSED_ACKS == 1){
                    //This peer has missed its ACK

                    removeGateway(peer);

                }else if(MAX_MISSED_ACKS > 1){

                    if(ackMissCount.containsKey(peer)){

                        if(ackMissCount.get(peer) == MAX_MISSED_ACKS - 1){
                            //This peer has successively missed the maximum limit allowed.

                            //Remove this entry
                            ackMissCount.remove(peer);
                            removeGateway(peer);

                        }else {
                            //This peer is a a series of missed ACKs but has not reached the maximum limit yet.

                            ackMissCount.put(peer, ackMissCount.get(peer) + 1);
                        }
                    }else {
                        //This is the first missed ACK from this peer.

                        ackMissCount.put(peer, 1);
                    }
                }
            }else {
                //We did get an ACK

                //Rest the missed count if it exists
                if(ackMissCount.containsKey(peer))  ackMissCount.remove(peer);
            }
        }
    }

    private void removeGateway(String offlineGateway){

        if(connectedDevices.containsKey(offlineGateway)){
            //Should always be true

            String offlineGatewayName = getNameFor(offlineGateway);
            padocManager.debugPrint("Lost Gateway : " + offlineGatewayName);

            //Remove the device from connectedDevices
            connectedDevices.remove(offlineGateway);

            //Remove the corresponding routing entry
            routes.remove(offlineGateway);
            //Remove the shortest route
            shortestRoute.remove(offlineGateway);
            //Remove the corresponding hops entry
            hops.remove(offlineGateway+"@"+offlineGateway);
            //Remove the corresponding name entry
            names.remove(offlineGateway);

            //Notify others that this route has changed
            padocManager.lostConnectionToPeer(offlineGateway);

            removeRoutesThatGoThrough(offlineGateway);

        }else{
            padocManager.debugPrint("ERROR, should not happen!");
        }

    }

    private void removeRoutesThatGoThrough(String offlineGateway){

        for(Iterator<Map.Entry<String, Set<String>>> entryIterator = routes.entrySet().iterator(); entryIterator.hasNext();){
            //We check for any destinationAddress that has a route involving the gateway that just went offline

            Map.Entry<String, Set<String>> entry = entryIterator.next();
            String destinationAddress = entry.getKey();
            Set<String> gateways = entry.getValue();

            if(gateways.contains(offlineGateway)) {
                //This destination address is not reachable through this gatewat anymore

                gateways.remove(offlineGateway);
                entry.setValue(gateways);

                //Remove the corresponding hops entry
                this.hops.remove(destinationAddress+"@"+offlineGateway);

//                padocManager.debugPrint("Route " + getNameFor(destinationAddress) + "@" + offlineGatewayName + " has been removed 0");

                if(gateways.size() == 0){
                    //This was the only gateway to this peer, we should remove this peer as well.

                    padocManager.debugPrint(getNameFor(destinationAddress) + " is not reachable anymore 0");

                    entryIterator.remove();

                    //Because this was the only gateway, it was also the shortest route
                    shortestRoute.remove(destinationAddress);
                    //And the name
                    names.remove(destinationAddress);

                }else if(getRoutingAddressFor(destinationAddress).equals(offlineGateway)){
                    //This was not the only gateway to this destination but it was the shortest one

                    Set<String> remainingGateways = routes.get(destinationAddress);

                    String newShortestRoute = new String();
                    int leastHops = Integer.MAX_VALUE;

                    for(String gateway : remainingGateways){

                        int gatewayHops = hops.get(destinationAddress+"@"+gateway);
                        if (gatewayHops < leastHops){

                            leastHops = gatewayHops;
                            newShortestRoute = gateway;
                        }
                    }

                    shortestRoute.put(destinationAddress, newShortestRoute);
                    hops.put(destinationAddress, leastHops);

                }
            }
        }
    }

    private void removeRoute(String peerAddress, String gatewayAddress){

        //Remove the gateway from the set
        routes.get(peerAddress).remove(gatewayAddress);
        //Remove the corresponding hops entry
        this.hops.remove(peerAddress+"@"+gatewayAddress);

//            padocManager.debugPrint("Route " + getNameFor(peerAddress) + "@" + getNameFor(gatewayAddress) + " has been removed");

        if(routes.get(peerAddress).size() == 0){
            //This was the only gateway to this peer, we should remove this peer as well.

            padocManager.debugPrint(getNameFor(peerAddress) + " is not reachable anymore");

            routes.remove(peerAddress);

            //Because this was the only gateway, it was also the shortest route
            shortestRoute.remove(peerAddress);
            //And the name
            names.remove(peerAddress);


        }else if(getRoutingAddressFor(peerAddress).equals(gatewayAddress)){
            //This was not the only gateway to this destination but it was the shortest one

            Set<String> remainingGateways = routes.get(peerAddress);

            String newShortestRoute = new String();
            int leastHops = Integer.MAX_VALUE;

            for(String gateway : remainingGateways){
                int gatewayHops = hops.get(peerAddress+"@"+gateway);
                if (gatewayHops < leastHops){

                    leastHops = gatewayHops;
                    newShortestRoute = gateway;
                }
            }

            shortestRoute.put(peerAddress, newShortestRoute);
            hops.put(peerAddress, leastHops);

        }

    }

    public boolean peerGoesThroughGateway(String peer, String gateway){
        return hops.containsKey(peer+"@"+gateway);
    }

    public void peerIsNotReachableThroughGateway(String peerAddress, String gatewayAddress){
        //Need to check if it was the only gateway or not

//        padocManager.debugPrint(routes.toString());
        if(routes.get(peerAddress) != null && routes.get(peerAddress).contains(gatewayAddress)){
            //Should always be true

            removeRoute(peerAddress, gatewayAddress);

        }else {
            padocManager.debugPrint("ERROR : routes does not have this entry");
        }
    }

//    public void cleanUpDirectConnections(){
//
//        int n = 0;
//
//        for(String offlineGateway : ackLog.keySet()){
//
//            if(!ackLog.get(offlineGateway)){
//                //If we did not get an ACK we assume the peer disconnected, we need to clean everything Up.
//
//                if (connectedDevices.containsKey(offlineGateway)){
//                    //Should always be true
//
//                    String offlineGatewayName = getNameFor(offlineGateway);
//                    padocManager.debugPrint("Lost direct connection to " + offlineGatewayName);
//
//                    //Remove the device from connectedDevices
//                    connectedDevices.remove(offlineGateway);
//
//                    //Remove the corresponding routing entry
//                    routes.remove(offlineGateway);
//                    //Remove the shortest route
//                    shortestRoute.remove(offlineGateway);
//                    //Remove the corresponding hops entry
//                    hops.remove(offlineGateway+"@"+offlineGateway);
//                    //Remove the corresponding name entry
//                    names.remove(offlineGateway);
//
//                    padocManager.lostConnectionToPeer(offlineGateway);
//
//                    //CleanUp routes, shortestRoute, hops, and names.
//                    for(Iterator<Map.Entry<String, Set<String>>> entryIterator = routes.entrySet().iterator(); entryIterator.hasNext();){
//                        //We check for any destinationAddress that has a route involving the gateway that just went offline
//
//                        Map.Entry<String, Set<String>> entry = entryIterator.next();
//                        String destinationAddress = entry.getKey();
//                        Set<String> gateways = entry.getValue();
//
//                        if(gateways.contains(offlineGateway)) {
////                            peerIsNotReachableThroughGateway(destinationAddress, offlineGateway);
//
//                            gateways.remove(offlineGateway);
//                            entry.setValue(gateways);
//
//
//                            //Remove the corresponding hops entry
//                            this.hops.remove(destinationAddress+"@"+offlineGateway);
//
//                            padocManager.debugPrint("Route " + getNameFor(destinationAddress) + "@" + offlineGatewayName + " has been removed 0");
//
//                            if(gateways.size() == 0){
//                                //This was the only gateway to this peer, we should remove this peer as well.
//
//                                padocManager.debugPrint(getNameFor(destinationAddress) + " is not reachable anymore 0");
//
////                                routes.remove(destinationAddress);
//                                entryIterator.remove();
//                                shortestRoute.remove(destinationAddress);
//                                hops.remove(destinationAddress+"@"+offlineGateway);
//                                names.remove(destinationAddress);
//
//                            }else if(getRoutingAddressFor(destinationAddress).equals(offlineGateway)){
//                                //This was not the only gateway to this destination but it was the shortest one
//
//                                Set<String> remainingGateways = routes.get(destinationAddress);
//
//                                String newShortestRoute = new String();
//                                int lessHops = Integer.MAX_VALUE;
//
//                                for(String gateway : remainingGateways){
//                                    int gatewayHops = hops.get(destinationAddress+"@"+gateway);
//                                    if (gatewayHops < lessHops){
//
//                                        lessHops = gatewayHops;
//                                        newShortestRoute = gateway;
//                                    }
//                                }
//
//                                shortestRoute.put(destinationAddress, newShortestRoute);
//                                hops.put(destinationAddress, lessHops);
//
//                            }
//                        }
//                    }
//
//                }else{
//                    padocManager.debugPrint("ERROR : connectedDevices does not have entry to be deleted");
//                }
//
//                if (connectedDevices.size() == 0){
//                    //This device no longer has active connections.
//                    padocManager.lostConnectionToMesh();
//                }
//            }else {
//                //If we did get an ACK we increment the counter.
//                n++;
//            }
//        }
//
//        if( n != connectedDevices.size()){
//            padocManager.debugPrint("ERROR : number of ACKs received (" + n + ") and number of Connected Devices (" + connectedDevices.size() + ") do not match.");
//        }
//    }

    public void receivedACKFrom(String address){
        ackLog.put(address, true);
    }

}