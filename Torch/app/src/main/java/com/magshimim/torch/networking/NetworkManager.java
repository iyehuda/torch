package com.magshimim.torch.networking;

import android.graphics.Bitmap;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.*;
import java.net.DatagramPacket;
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
            Log.e("NetworkManager",e.getMessage());
        }

    }

    public void connect (String address, int port)
    {
        InetAddress addr =null;
          try {
              addr = InetAddress.getByName(address);
          }
          catch (UnknownHostException e) {
              Log.e("NetworkManager, connect",e.getMessage());
          }
        socket.connect(addr, port);


    }
    public void sendFrame(Bitmap frame)
    {
        ByteArrayOutputStream streamToSend=null;
        boolean compress = frame.compress(Bitmap.CompressFormat.PNG, 100, streamToSend);
        byte[] bitmapArray = streamToSend.toByteArray();
        DatagramPacket packet = new DatagramPacket(bitmapArray,bitmapArray.length);
        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e("NetworkManager,Frame",e.getMessage());
        }
    }
    public void disconnect()
    {
        socket.disconnect();
    }
}
