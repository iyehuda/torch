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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.magshimim.torch.TorchThread;

/**
 * Created by User on 1/28/2017.
 */

public class NetworkManager implements INetworkManager {
    private DatagramSocket socket;
    private TorchThread frameSender;
    private final Queue<Bitmap> framesTosend;
    private boolean sending;
    public NetworkManager()
    {
        framesTosend = new ConcurrentLinkedQueue<>();

        try {
            socket = new DatagramSocket();
        }
        catch (SocketException e) {
            Log.e("NetworkManager",e.getMessage());
        }
        frameSender = new TorchThread("queueSender",framesTosend,socket);
        sending = false;

    }

    public void connect (String address, int port)
    {
        InetAddress addr = null;
          try {
              addr = InetAddress.getByName(address);
          }
          catch (UnknownHostException e) {
              Log.e("NetworkManager, connect",e.getMessage());
          }
        socket.connect(addr, port);
        frameSender.start();
        sending = true;
    }
    public void sendFrame(Bitmap frame)
    {
        try {
            framesTosend.wait();
            framesTosend.add(frame);
            framesTosend.notify();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void disconnect()
    {
        frameSender.stopSending();
        socket.disconnect();
    }
}
