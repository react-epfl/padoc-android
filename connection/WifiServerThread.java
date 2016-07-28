package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.Padoc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by gabriel on 29/06/16.
 */
public class WifiServerThread extends Thread{

    private final int PORT = 6000;

    private Padoc padoc;
    private ServerSocket serverSocket;

    public WifiServerThread(Padoc padoc){

        this.padoc = padoc;
        ServerSocket tmpSocket = null;

        try{
            tmpSocket = new ServerSocket(PORT);
        }catch (IOException e){
            e.printStackTrace();
        }

        if(tmpSocket != null) {
            this.serverSocket = tmpSocket;
        }else {
//            padoc.print("ERROR : Server initialization failed");
        }

    }

    public void run(){
//        padoc.print("Running WiFi server...");

        Socket socket = null;

        while(true){

            try{
                socket = serverSocket.accept();
            }catch (IOException e){
                e.printStackTrace();
            }

            if(socket != null){
                padoc.handleWifiConnectedSocket(socket);
            }
        }
    }

    public void close(){
        try{
            serverSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}