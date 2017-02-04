package com.magshimim.torch.networking;

import android.graphics.Bitmap;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.content.Context;

/**
 * Created by User on 1/28/2017.
 */

public class NetworkManager implements INetworkManager {
    private DatagramSocket socket;

    public NetworkManager(String address, int port)
    {
        try {
            InetAddress addr = InetAddress.getByName(address);
            try {
                socket = new DatagramSocket(port,addr);
            }
            catch (SocketException e)
            {
                Toast t = Toast.makeText(null,e.getMessage(),Toast.LENGTH_SHORT);
            }
        }
        catch (UnknownHostException e)
        {
            Toast t = Toast.makeText(null,e.getMessage(),Toast.LENGTH_SHORT);
        }

    }

    public void connect (String address, int port)
    {

    }
    public void sendFrame(Bitmap frame)
    {
        //OutputStream stream = new OutputStream();
    }
    public void disconnect()
    {

    }
}
