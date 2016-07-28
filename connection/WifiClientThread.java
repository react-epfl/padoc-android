package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.Padoc;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by gabriel on 29/06/16.
 */
public class WifiClientThread extends Thread{

    private final int PORT = 6000;

    private final String SERVER_ADDRESS = "192.168.49.1";

    private Padoc padoc;
    private Socket clientSocket = null;

    public WifiClientThread(Padoc padoc){

        this.padoc = padoc;

    }

    //TODO : Implement timeout (wifiSocketConnectionTimeout) like in bluetooth client, it should call onWifiSocketConnectionFailed()

    public void run(){

//        padoc.print("Running client...");

        try {
            clientSocket = new Socket(SERVER_ADDRESS, PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (clientSocket != null && clientSocket.isConnected()){

            padoc.handleWifiConnectedSocket(clientSocket);

        }else {
//            padoc.print("ERROR : Wifi client initialization failed.");
        }

    }
}
