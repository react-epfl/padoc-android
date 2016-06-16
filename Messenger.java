package com.react.gabriel.wbam.padoc;

import android.util.Pair;
import android.view.MotionEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 17/05/16.
 */
public class Messenger {


    private MainActivity mActivity = null;
    private PadocManager padocManager;
    private Router mRouter;
    private String localBluetoothAddress;
    private String localName;

    //Map<UUID, Set<Sources>>
    private Map<String, Set<String>> cbsMsgTracker = new HashMap<String, Set<String>>();

    private Set<String> floodMsgTracker = new HashSet<String>();

    public Messenger(MainActivity mActivity, PadocManager padocManager, Router router, String localBluetoothAddress, String localName){

        this.mActivity = mActivity;
        this.padocManager = padocManager;
        this.mRouter = router;
        this.localBluetoothAddress = localBluetoothAddress;
        this.localName = localName;

    }

    /**
     * Decides what to do with any received message,
     * either prints the message if it is meant for this device and/or forwards it otherwise.
     * @param message
     * @param fromThread
     */
    public void deliverMsg(Message message, ConnectedThread fromThread){

//        System.out.println("got msg to deliver");

        Message.Algo algo = message.getAlgo();

        Message.ContentType contentType;

        switch (algo){

            case FLOOD:

//                String[] newAddressInfo;
                String newAddress = null;
                String newName = null;
                String newMesh = null;
                String sourceAddress;
                int hops;

                contentType = message.getContentType();

                switch (contentType){


                    case ID:
                        //This type of messages can come from clients (introducing themselves) or from anyone forwarding an ID message.
//                        mActivity.debugPrint("Got ID");

                        try{
                            JSONObject jsonMessage = new JSONObject(message.getMsg());
                            newAddress = jsonMessage.getString(Message.ADDRESS);
                            newName = jsonMessage.getString(Message.NAME);
                            newMesh = jsonMessage.getString(Message.MESH);
                        }catch (JSONException e){
                            e.printStackTrace();
                        }

                        if (newAddress == null || newName == null || newMesh == null){
                            mActivity.debugPrint("ERROR : could not get message fields.");
                            break;
                        }

//                        newAddressInfo = message.getMsg().split("-");
//                        newAddress = newAddressInfo[0];
//                        newName = newAddressInfo[1];

                        sourceAddress = message.getSource();

                        hops = message.getHops();

                        if(fromThread.isOrphan() && hops == 0 && sourceAddress.equals(newAddress)){
                            //ID msg is original (not a forward)

                            padocManager.connectionFromRemoteClientSucceeded(fromThread, newAddress, newName, newMesh);

                            if(message.getDestination().equals(Message.ALL)){
                                forwardBroadcastFLOODMsg(message, fromThread.getRemoteAddress());
                            }

                        }

                        if(!fromThread.isOrphan() && (!mRouter.knows(newAddress) || (mRouter.knows(newAddress) && mRouter.getHopsFor(newAddress) > hops))){
                            //If we don't have this address registered yet, or if we do but this route is shorter, save it and broadcast.

                            //TODO : why not use sourceAddress instead of getRemoteAddress()
                            mRouter.setRoute(newName, newAddress, hops, fromThread.getRemoteAddress());
                            mActivity.debugPrint("Saved route to : " + newName);

                            if(message.getDestination().equals(Message.ALL)){
                                forwardBroadcastFLOODMsg(message, fromThread.getRemoteAddress());
                            }

                        }


                        break;

                    case IDS:
                        //This type of messages only come from servers.
//                        mActivity.debugPrint("Got IDS");

                        String newAddresses = message.getMsg();

                        sourceAddress = message.getSource();

                        int n = 0;

                        for(String address : newAddresses.split(">")){

                            String[] newAddressInfo = address.split("<");

                            String[] newMacAndName = newAddressInfo[0].split("-");
                            newAddress = newMacAndName[0];
                            newName = newMacAndName[1];
                            hops = Integer.parseInt(newAddressInfo[1]);

                            if(!mRouter.knows(newAddress) || (mRouter.knows(newAddress) && mRouter.getHopsFor(newAddress) > hops)){

                                mRouter.setRoute(newName, newAddress, hops, sourceAddress);
                                n++;
                            }
                        }

                        if(n > 0) mActivity.debugPrint("Saved " + n + " new addresses");

                        if(message.getDestination().equals(Message.ALL)){
                            forwardBroadcastFLOODMsg(message, fromThread.getRemoteAddress());
                        }

                        break;
                    case MSG:
                        //TODO

                        String msgID = message.getUUID();
                        String destination = message.getDestination();

                        if(!floodMsgTracker.contains(msgID)){
                            //This is a brand new flooding msg. yay.

                            floodMsgTracker.add(msgID);

                            if(destination.equals(Message.ALL) || destination.equals(localBluetoothAddress)){
                                printMsg(message);
                            }

                            forwardBroadcastFLOODMsg(message, fromThread.getRemoteAddress());

                        }else {
                            //TODO reset timer for tracking deletion
                        }

                        break;
                }
                break;

            case CBS:

                String msgID = message.getUUID();
                String destination = message.getDestination();

                if(!cbsMsgTracker.containsKey(msgID)){
                    //First time we get this msg, initialize counter to one.

//                    mActivity.debugPrint("Got CBS");
                    Set sources = new HashSet();
                    sources.add(fromThread.getRemoteAddress());

                    cbsMsgTracker.put(msgID, sources);

                    new CBSThread(this, message).start();

                    if(destination.equals(localBluetoothAddress) || destination.equals(Message.ALL)){
                        printMsg(message);
                        mActivity.debugPrint("From : " + mRouter.getNameFor(fromThread.getRemoteAddress()));
                    }

                }else{
                    //We are in RAD, msg has already been received, increment counter by one.
                    Set sources = cbsMsgTracker.get(msgID);
//                    mActivity.debugPrint("Got CBS again");

                    String source = fromThread.getRemoteAddress();

                    if(!sources.contains(source)){
                        sources.add(source);
                        cbsMsgTracker.put(msgID, sources);
                    }else {
                        mActivity.debugPrint("ERROR : Got duplicate CBS from same source");
                    }
                }

                break;
            case ROUTE:

//                System.out.println("It's a ROUTE");

                destination = message.getDestination();

                if(destination.equals(localBluetoothAddress)){
                    //Msg has reached its destination, display it.
                    printMsg(message);

                }else if(mRouter.knows(destination)){
//                    System.out.println("It knows the destination");
                    //This is not the final destination of the message, forward it.
                    forwardMsg(message);

                }else if(destination.equals(Message.ALL)){
                    mActivity.debugPrint("ERROR : Got malformed message, ROUTE algorithm cannot be used to deliver to ALL");

                }else {
                    mActivity.debugPrint("ERROR : forward destination unknown");

                }
                break;
        }
    }

    public int getCBSCountForMsg(String uuid){
        return cbsMsgTracker.get(uuid).size();
    }

    public Set<String> getCBSBannedListForMsg(String uuid){
        return cbsMsgTracker.get(uuid);
    }

    public void clearCBSTrack(String uuid){
        cbsMsgTracker.remove(uuid);
    }

    public void broadcastMsg(Message message){

        if(message.getDestination().equals(Message.ALL)){

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }

        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    /**
     * Forwards the message to any direct node that is not in the banned list.
     * The banned list contains the peers who we already received the msg from.
     * @param message
     * @param bannedAddresses
     */
    public void forwardCBS(Message message, Set<String> bannedAddresses){

        message.incrementHop();

        for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
            if(!bannedAddresses.contains(connectedEntry.getKey())){
                connectedEntry.getValue().write(message);
            }
        }
    }

    public void forwardBroadcastFLOODMsg(Message message, String fromAddress){

        message.incrementHop();

        for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
            if(!connectedEntry.getKey().equals(fromAddress)){
                connectedEntry.getValue().write(message);
            }
        }
    }

    public void forwardMsg(Message message){

        String destination = message.getDestination();
        String destName = mRouter.getNameFor(destination);
        String sourceName = mRouter.getNameFor(message.getSource());
        String gateName = mRouter.getNameFor(mRouter.getRoutingAddressFor(destination));

//        mActivity.debugPrint("FWD: (" + sourceName + "," + destName + ")" + " : " + gateName);

        message.incrementHop();
        mRouter.getRoutingThreadFor(destination).write(message);

    }

    public void sendMsg(String msg, String destination, Message.Algo algo){

        Message message;

        if(destination.equals(Message.ALL) && !algo.equals(Message.Algo.ROUTE)){

            message = new Message(algo, Message.ContentType.MSG, msg, localBluetoothAddress, Message.ALL, 0);

            switch (algo) {
                case FLOOD:

                    floodMsgTracker.add(message.getUUID());

                    break;
                case CBS:

                    HashSet<String> localSourceSet = new HashSet<>();
                    localSourceSet.add(localBluetoothAddress);

                    cbsMsgTracker.put(message.getUUID(), localSourceSet);
                    break;
            }

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }

//            mActivity.debugPrint("(" + algo.toString() + ") ME : " + msg);

        }else if(mRouter.knows(destination)){

            message = new Message(algo, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);
            ConnectedThread routingThread = mRouter.getRoutingThreadFor(destination);

//            System.out.println("Routing thread is " + mRouter.getNameFor(routingThread.getRemoteAddress()));

            routingThread.write(message);

            mActivity.debugPrint("ME : " + msg);
        }else if(destination.equals(localBluetoothAddress)){
            mActivity.debugPrint("You cannot message yourself");
        }else {
            mActivity.debugPrint("ERROR : Destination unknown");
        }

//        if(destination.equals(ALL)){
//
//            message = new Message(Message.Algo.CBS, Message.ContentType.MSG, msg, localBluetoothAddress, ALL, 0);
//
//            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
//                connectedThread.write(message);
//            }
//        }else if(mRouter.knows(destination)){
//
//            Message message = new Message(Message.Algo.ROUTE, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);
//            mRouter.getRoutingThreadFor(destination).write(message);
//            mActivity.debugPrint("ME : " + msg);
//
//        }else if(destination.equals(localBluetoothAddress)){
//
//            mActivity.debugPrint("ERROR : You cannot message yourself");
//
//        }else {
//
//            mActivity.debugPrint("ERROR : destination unknown");
//        }
    }

    public void introduceMyselfToThread(ConnectedThread connectedThread){
        mActivity.debugPrint("Sending my ID");
        Message IDMessage = Message.getIDMsg(localBluetoothAddress, localName, padocManager.getMeshUUID());
        connectedThread.write(IDMessage);
    }

    public void sendMeshInfoTo(String newAddress){
        String knownPeersString = "";

        int n = 0;
        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
            String knownAddress = knownPeer.getKey();
            String knownAddressName = mRouter.getNameFor(knownAddress);

            if(!knownAddress.equals(newAddress)){
                n++;
                int hops = knownPeer.getValue().first+1;
                knownPeersString += knownAddress+"-"+knownAddressName+"<"+hops+">";
            }
        }

        if(n > 0){
            Message knownPeersMessage = new Message(Message.Algo.FLOOD, Message.ContentType.IDS, knownPeersString, localBluetoothAddress, newAddress, 0);
            mActivity.debugPrint("Sending " + n + " known peers.");
            mRouter.getRoutingThreadFor(newAddress).write(knownPeersMessage);
        }

//        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
//            String knwonAddress = knownPeer.getKey();
//            if(!knwonAddress.equals(newAddress)){
//                mActivity.debugPrint("Sending peer : " + knwonAddress);
//                int hops = knownPeer.getValue().first+1;
//                Message message = new Message(Message.Algo.FLOOD, Message.ContentType.ID, knwonAddress, localBluetoothAddress, newAddress, hops);
//                mRouter.getRoutingThreadFor(newAddress).write(message);
//            }
//        }
    }

    //EVALUATION
    private Map<String, Integer> messageCount = new HashMap<String, Integer>();

    private void addEvaluationResults(String address){
        if(messageCount.containsKey(address)){
            messageCount.put(address, messageCount.get(address)+1);
        }else {
            messageCount.put(address, 1);
        }
    }

    public void clearMessageCount(){
        messageCount.clear();
        floodMsgTracker.clear();
        cbsMsgTracker.clear();
    }

    public void printMessageCount(){

        for(String address : messageCount.keySet()){
            mActivity.debugPrint(mRouter.getNameFor(address) + " : " + messageCount.get(address) + " messages received.");
        }

    }

    private void printMsg(Message message){

        String sourceAddress = message.getSource();

        addEvaluationResults(sourceAddress);

        mActivity.debugPrint(mRouter.getNameFor(sourceAddress) + " (" + mRouter.getHopsFor(sourceAddress) + ") (" + message.getHops() + ") : " + message.getMsg());
    }
}
