package com.magshimim.torch.networking;

import android.graphics.Bitmap;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.magshimim.torch.BuildConfig;

public class NetworkManager implements INetworkManager {
    private final static String TAG = "NetworkManager";
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private Socket socket;
    private TorchThread frameSender;
    private final Queue<Bitmap> framesTosend;
    private boolean sending;
    public NetworkManager()
    {
        if(DEBUG) Log.d(TAG, "NetworkManager");
        framesTosend = new ConcurrentLinkedQueue<>();

        socket = new Socket();
        if(DEBUG) Log.d(TAG, "Socket is created");
        frameSender = new TorchThread("queueSender", framesTosend, socket);
        if(DEBUG) Log.d(TAG, "Thread is created");
        sending = false;
    }

    public void connect (String address, int port)
    {
        if(DEBUG) Log.d(TAG, "connect");
        try {
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            socket.connect(socketAddress, 2000);
        } catch (IOException e) {
            Log.e(TAG, String.format("Could not connect to %s:%d", address, port), e);
            return;
        }
        if(DEBUG) Log.d(TAG, "socket is connected");
        frameSender.start();
        if(DEBUG) Log.d(TAG, "sender has started");
        sending = true;
    }

    public void sendFrame(Bitmap frame)
    {
        if(DEBUG) Log.d(TAG, "sendFrame");
        if(!sending) {
            Log.w(TAG, "Not connected");
            return;
        }
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
            try {
                if(!socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        } else Log.w(TAG, "socket is null");

        sending = false;
    }
}
