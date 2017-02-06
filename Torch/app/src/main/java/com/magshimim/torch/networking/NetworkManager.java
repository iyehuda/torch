package com.magshimim.torch.networking;

import android.graphics.Bitmap;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.content.Context;
import android.widget.PopupWindow;
import java.lang.Object;
import android.util.Log;

/**
 * Created by User on 1/28/2017.
 */

public class NetworkManager implements INetworkManager {
    private DatagramSocket socket;

    public NetworkManager()
    {
        try {
            socket = new DatagramSocket();
        }
        catch (SocketException e) {
            Log.e(e.getMessage(), "dsfs");
        }

    }

    public void connect (String address, int port)
    {
        InetAddress addr =null;
                try {
                    addr = InetAddress.getByName(address);
                }
                catch (UnknownHostException e) {
                    Log.e(e.getMessage(),"vscvcx");
                }
                socket.connect(addr, port);


    }
    public void sendFrame(Bitmap frame)
    {
        //OutputStream stream = new OutputStream();
    }
    public void disconnect()
    {

    }
}
