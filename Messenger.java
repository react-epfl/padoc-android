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

    private Map<String, Set<String>> cbsMsgTracker = new HashMap<String, Set<String>>();

    public Messenger(MainActivity mActivity, Router router, String localBluetoothAddress){

        this.mActivity = mActivity;
        this.mRouter = router;
        this.localBluetoothAddress = localBluetoothAddress;

    }

    /**
     * Decides what to do with any received message,
     * either prints the message if it is meant for this device and/or forwards it otherwise.
     * @param jsonMsg
     * @param fromThread
     */
    public void deliverMsg(JsonMsg jsonMsg, ConnectedThread fromThread){

        JsonMsg.Algo algo = jsonMsg.getAlgo();

        JsonMsg.ContentType contentType;

        switch (algo){

            case CBS:
                //TODO : CBS stuff

                String msgID = jsonMsg.getUUID();

                if(!cbsMsgTracker.containsKey(msgID)){
                    //First time we get this msg, initialize counter to one.

                    mActivity.debugPrint("Got unknown CBS");
                    Set sources = new HashSet();
                    sources.add(fromThread.getRemoteAddress());

                    cbsMsgTracker.put(msgID, sources);

                    new CBSThread(this, jsonMsg).start();

                    printMsg(jsonMsg);

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

                String destination = jsonMsg.getDestination();

                if(destination.equals(localBluetoothAddress)){
                    //Msg has reached its destination, display it.
                    printMsg(jsonMsg);
                }else{
                    //This is not the final destination of the message, forward it.
                    forwardMsg(jsonMsg);
                }
                break;

            case IDProp:

                contentType = jsonMsg.getContentType();

                switch (contentType){

                    case ID:

                        mActivity.debugPrint("Got ID");

                        String newAddress = jsonMsg.getMsg();
                        String sourceAddress = jsonMsg.getSource();

                        int hops = jsonMsg.getHops();

                        mActivity.debugPrint("jsonMsg:"+jsonMsg.toString());
                        if(fromThread.isOrphan() && hops == 0 && sourceAddress.equals(newAddress)){
                            //ID msg is original (not a forward)

                            mActivity.debugPrint("knows it?"+mRouter.knows(newAddress));
                            mRouter.identifyOrphanThread(fromThread, newAddress);
                            mActivity.debugPrint("Saved Original");
                            mActivity.debugPrint("knows it?"+mRouter.knows(newAddress));

                            //Because the peer is new we should greet him with the necessary info
                            sendMeshInfoTo(newAddress);

                            if(jsonMsg.getDestination().equals(ALL)) {
                                forwardBroadcastIDMsg(jsonMsg, fromThread.getRemoteAddress());
                            }
                        }

                        if(!fromThread.isOrphan() && (!mRouter.knows(newAddress) || (mRouter.knows(newAddress) && mRouter.getHopsFor(newAddress) > hops))){

                            //If we don't have this address registered yet, or if we do but this route is shorter, save it and broadcast.
                            mRouter.setRoute(newAddress, hops, fromThread.getRemoteAddress());
                            mActivity.debugPrint("Saved Route");

                            if(jsonMsg.getDestination().equals(ALL)){
                                forwardBroadcastIDMsg(jsonMsg, fromThread.getRemoteAddress());
                            }
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

    public void broadcastMsg(JsonMsg jsonMsg){

        if(jsonMsg.getDestination().equals(ALL)){

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(jsonMsg);
            }

        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    /**
     * Forwards the message to any direct node that is not in the banned list.
     * The banned list is typically constituted by the peers who we already received the msg from.
     * @param jsonMsg
     * @param bannedAddresses
     */
    public void forwardBroadcast(JsonMsg jsonMsg, Set<String> bannedAddresses){

        if(jsonMsg.getDestination().equals(ALL)){
            jsonMsg.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!bannedAddresses.contains(connectedEntry.getKey())){
                    connectedEntry.getValue().write(jsonMsg);
                }
            }
        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    public void forwardBroadcastIDMsg(JsonMsg jsonMsg, String fromAddress){

        if(jsonMsg.getDestination().equals(ALL)){

            jsonMsg.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!connectedEntry.getKey().equals(fromAddress)){
                    connectedEntry.getValue().write(jsonMsg);
                }
            }
        }else{
            mActivity.debugPrint("ERROR : This message is not for ALL");
        }
    }

    public void forwardMsg(JsonMsg jsonMsg){

        String destination = jsonMsg.getDestination();
        if(mRouter.knows(destination)){
            //TODO
            jsonMsg.incrementHop();
            mRouter.getRoutingThreadFor(destination).write(jsonMsg);

        }else{
            mActivity.debugPrint("ERROR : forward destination unknown");
        }
    }

    public void sendMsg(String msg, String destination){
        //TODO : Completely

        if(destination.equals(ALL)){

            JsonMsg jsonMsg = new JsonMsg(JsonMsg.Algo.CBS, JsonMsg.ContentType.MSG, msg, localBluetoothAddress, destination, 0);

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(jsonMsg);
            }
        }else if(mRouter.knows(destination)){

            JsonMsg jsonMsg = new JsonMsg(JsonMsg.Algo.SINGLE, JsonMsg.ContentType.MSG, msg, localBluetoothAddress, destination, 0);
            mRouter.getRoutingThreadFor(destination).write(jsonMsg);
            mActivity.debugPrint("ME : " + msg);

        }else if(destination.equals(localBluetoothAddress)){

            mActivity.debugPrint("ERROR : You cannot message yourself");
        }else {

            mActivity.debugPrint("ERROR : destination unknown");
        }
    }

    public void introduceMyselfToThread(ConnectedThread connectedThread){
        mActivity.debugPrint("Sending my ID");
        JsonMsg idJsonMsg = JsonMsg.getIDMsg(localBluetoothAddress);
        connectedThread.write(idJsonMsg);
    }

    public void sendMeshInfoTo(String newAddress){

        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
            String knwonAddress = knownPeer.getKey();
            if(!knwonAddress.equals(newAddress)){
                int hops = knownPeer.getValue().first+1;
                JsonMsg jsonMsg = new JsonMsg(JsonMsg.Algo.IDProp, JsonMsg.ContentType.ID, knwonAddress, localBluetoothAddress, newAddress, hops);
                mRouter.getRoutingThreadFor(newAddress).write(jsonMsg);
            }
        }
    }

    private void printMsg(JsonMsg jsonMsg){
        mActivity.debugPrint(jsonMsg.getSource() + " : " + jsonMsg.getMsg() + " (" + jsonMsg.getHops() + ")");
    }
}
