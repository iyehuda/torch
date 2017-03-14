package com.magshimim.torch.networking;

import android.graphics.Bitmap;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.magshimim.torch.BuildConfig;

/**
 * Created by User on 1/28/2017.
 */

public class NetworkManager implements INetworkManager {
    private final static String TAG = "NetworkManager";
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private DatagramSocket socket;
    private TorchThread frameSender;
    private final Queue<Bitmap> framesTosend;
    private boolean sending;
    public NetworkManager()
    {
        if(DEBUG) Log.d(TAG, "NetworkManager");
        framesTosend = new ConcurrentLinkedQueue<>();

        try {
            socket = new DatagramSocket();
            if(DEBUG) Log.d(TAG, "Socket is created");
        }
        catch (SocketException e) {
            Log.e("NetworkManager",e.getMessage());
        }
        frameSender = new TorchThread("queueSender",framesTosend,socket);
        if(DEBUG) Log.d(TAG, "Thread is created");
        sending = false;
    }

    public void connect (String address, int port)
    {
        if(DEBUG) Log.d(TAG, "connect");
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(address);
        }
        catch (UnknownHostException e) {
            Log.e(TAG, "failed to convert hostname", e);
            return;
        }
        socket.connect(addr, port);
        if(DEBUG) Log.d(TAG, "socket is connected");
        frameSender.start();
        if(DEBUG) Log.d(TAG, "sender has started");
        sending = true;
    }

    public void sendFrame(Bitmap frame)
    {
        if(DEBUG) Log.d(TAG, "sendFrame");
        synchronized (framesTosend) {
            framesTosend.add(frame);
            if(DEBUG) Log.d(TAG, "added frame to queue");
            framesTosend.notify();
            if(DEBUG) Log.d(TAG, "notified thread");
        }
    }
    public void disconnect()
    {
        if(DEBUG) Log.d(TAG, "disconnect");

        if(frameSender != null) {
            frameSender.stopSending();
            frameSender = null;
        } else Log.w(TAG, "frameSender is null");

        if (socket != null) {
            socket.disconnect();
            socket = null;
        } else Log.w(TAG, "socket is null");
    }
}
