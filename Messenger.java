package com.react.gabriel.wbam.padoc;

import android.util.Pair;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.bluetooth.ConnectedThread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 17/05/16.
 */
public class Messenger {

    private static final String ALL = "ALL";

    private MainActivity mActivity = null;
    private Router mRouter;
    private String localBluetoothAddress;
    private String localName;

    private Map<String, Set<String>> cbsMsgTracker = new HashMap<String, Set<String>>();

    public Messenger(MainActivity mActivity, Router router, String localBluetoothAddress, String localName){

        this.mActivity = mActivity;
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

        Message.Algo algo = message.getAlgo();

        Message.ContentType contentType;

        switch (algo){

            case CBS:

                String msgID = message.getUUID();

                if(!cbsMsgTracker.containsKey(msgID)){
                    //First time we get this msg, initialize counter to one.

                    mActivity.debugPrint("Got CBS");
                    Set sources = new HashSet();
                    sources.add(fromThread.getRemoteAddress());

                    cbsMsgTracker.put(msgID, sources);

                    new CBSThread(this, message).start();

                    printMsg(message);

                    //Initialize RAD
                }else{
                    //We are in RAD, msg has already been received, increment counter by one.
                    Set sources = cbsMsgTracker.get(msgID);
                    mActivity.debugPrint("Got CBS again");

                    if(!sources.contains(fromThread.getRemoteAddress())){
                        sources.add(fromThread.getRemoteAddress());
                        cbsMsgTracker.put(msgID, sources);
                    }else {
                        mActivity.debugPrint("ERROR : Got duplicate msg from same source");
                    }
                }

                break;

            case SINGLE:

                String destination = message.getDestination();

                if(destination.equals(localBluetoothAddress)){
                    //Msg has reached its destination, display it.
                    printMsg(message);
                }else{
                    //This is not the final destination of the message, forward it.
                    forwardMsg(message);
                }
                break;

            case IDProp:

                String[] newAddressInfo;
                String newAddress;
                String newName;
                String sourceAddress;
                int hops;

                contentType = message.getContentType();

                switch (contentType){

                    case ID:
                        //This type of messages can come from clients (introducing themselves) or from anyone forwarding an ID message.
                        mActivity.debugPrint("Got ID");

                        newAddressInfo = message.getMsg().split("-");
                        newAddress = newAddressInfo[0];
                        newName = newAddressInfo[1];
                        sourceAddress = message.getSource();

                        hops = message.getHops();

                        if(fromThread.isOrphan() && hops == 0 && sourceAddress.equals(newAddress)){
                            //ID msg is original (not a forward)

                            mRouter.identifyOrphanThread(newName, fromThread, newAddress);
                            mActivity.debugPrint("Saved original ID");

                            //Because the peer is new we should greet him with the necessary info
                            sendMeshInfoTo(newAddress);

                            if(message.getDestination().equals(ALL)){
                                forwardBroadcastIDMsg(message, fromThread.getRemoteAddress());
                            }

                        }

                        if(!fromThread.isOrphan() && (!mRouter.knows(newAddress) || (mRouter.knows(newAddress) && mRouter.getHopsFor(newAddress) > hops))){
                            //If we don't have this address registered yet, or if we do but this route is shorter, save it and broadcast.

                            //TODO : why not use sourceAddress instead of getRemoteAddress()
                            mRouter.setRoute(newName, newAddress, hops, fromThread.getRemoteAddress());
                            mActivity.debugPrint("Saved ID route");

                            if(message.getDestination().equals(ALL)){
                                forwardBroadcastIDMsg(message, fromThread.getRemoteAddress());
                            }

                        }


                        break;

                    case IDS:
                        //This type of messages only come from servers.
                        mActivity.debugPrint("Got IDS");

                        String newAddresses = message.getMsg();

                        sourceAddress = message.getSource();

                        int n = 0;

                        for(String address : newAddresses.split(">")){

                            newAddressInfo = address.split("<");

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

                        if(message.getDestination().equals(ALL)){
                            forwardBroadcastIDMsg(message, fromThread.getRemoteAddress());
                        }

                        break;
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

        if(message.getDestination().equals(ALL)){

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }

        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    /**
     * Forwards the message to any direct node that is not in the banned list.
     * The banned list is typically constituted by the peers who we already received the msg from.
     * @param message
     * @param bannedAddresses
     */
    public void forwardBroadcast(Message message, Set<String> bannedAddresses){

        if(message.getDestination().equals(ALL)){
            message.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!bannedAddresses.contains(connectedEntry.getKey())){
                    connectedEntry.getValue().write(message);
                }
            }
        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    public void forwardBroadcastIDMsg(Message message, String fromAddress){

        if(message.getDestination().equals(ALL)){

            message.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!connectedEntry.getKey().equals(fromAddress)){
                    connectedEntry.getValue().write(message);
                }
            }
        }else{
            mActivity.debugPrint("ERROR : This message is not for ALL");
        }
    }

    public void forwardMsg(Message message){

        String destination = message.getDestination();
        if(mRouter.knows(destination)){
            message.incrementHop();
            mRouter.getRoutingThreadFor(destination).write(message);

        }else{
            mActivity.debugPrint("ERROR : forward destination unknown");
        }
    }

    public void sendMsg(String msg, String destination){

        if(destination.equals(ALL)){

            Message message = new Message(Message.Algo.CBS, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }
        }else if(mRouter.knows(destination)){

            Message message = new Message(Message.Algo.SINGLE, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);
            mRouter.getRoutingThreadFor(destination).write(message);
            mActivity.debugPrint("ME : " + msg);

        }else if(destination.equals(localBluetoothAddress)){

            mActivity.debugPrint("ERROR : You cannot message yourself");
        }else {

            mActivity.debugPrint("ERROR : destination unknown");
        }
    }

    public void introduceMyselfToThread(ConnectedThread connectedThread){
        mActivity.debugPrint("Sending my ID");
        Message IDMessage = Message.getIDMsg(localName, localBluetoothAddress);
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
            Message knownPeersMessage = new Message(Message.Algo.IDProp, Message.ContentType.IDS, knownPeersString, localBluetoothAddress, newAddress, 0);
            mActivity.debugPrint("Sending " + n + " known peers.");
            mRouter.getRoutingThreadFor(newAddress).write(knownPeersMessage);
        }

//        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
//            String knwonAddress = knownPeer.getKey();
//            if(!knwonAddress.equals(newAddress)){
//                mActivity.debugPrint("Sending peer : " + knwonAddress);
//                int hops = knownPeer.getValue().first+1;
//                Message message = new Message(Message.Algo.IDProp, Message.ContentType.ID, knwonAddress, localBluetoothAddress, newAddress, hops);
//                mRouter.getRoutingThreadFor(newAddress).write(message);
//            }
//        }
    }

    private void printMsg(Message message){
        String sourceAddress = message.getSource();
        mActivity.debugPrint(mRouter.getNameFor(sourceAddress) + " (" + mRouter.getHopsFor(sourceAddress) + ") : " + message.getMsg());
    }
}
