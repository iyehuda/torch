package com.magshimim.torch.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.magshimim.torch.BuildConfig;

public class NetworkManager implements INetworkManager {
    private final static String TAG = "NetworkManager";
    private final static boolean DEBUG = BuildConfig.DEBUG;

    private Socket socket; // Connection to the server
    private SenderThread frameSender; // Thread that able to send messages from queue
    private final Queue<byte[]> framesToSend; // A queue to hold data
    private boolean sending; // Determines whether the thread sends data

    /**
     * Initializes inner data
     */
    public NetworkManager()
    {
        if(DEBUG) Log.d(TAG, "NetworkManager");
        framesToSend = new ConcurrentLinkedQueue<>();
        socket = new Socket();
        if(DEBUG) Log.d(TAG, "Socket is created");
        frameSender = new SenderThread("queueSender", framesToSend, socket);
        if(DEBUG) Log.d(TAG, "Thread is created");
        sending = false;
    }

    /**
     * Connect to the server and start waiting for messages to send
     * @param address The server IP
     * @param port The server port
     */
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

    /**
     * Add a frame to the sending queue
     * @param data The data to be sent
     */
    public void sendData(byte[] data)
    {
        if(DEBUG) Log.d(TAG, "sendData");

        if(!sending) {
            Log.w(TAG, "Not connected");
            return;
        }
        if(data == null) {
            Log.w(TAG, "data is null");
            return;
        }
        if(data.length == 0) {
            Log.w(TAG, "no data");
            return;
        }

        synchronized (framesToSend) {
            framesToSend.add(data);
            if(DEBUG) Log.d(TAG, "added frame to queue");
            framesToSend.notify();
        }
    }

    /**
     * Disconnect from the server
     */
    public void disconnect()
    {
        if(DEBUG) Log.d(TAG, "disconnect");
        if(!sending) {
            Log.w(TAG, "not sending");
            return;
        }

        if(frameSender != null) {
            frameSender.stopSending();
            frameSender = null;
        } else Log.w(TAG, "frameSender is null");

        if (socket != null) {
            try {
                if(!socket.isClosed())
                    socket.close();
                else
                    Log.w(TAG, "socket is already closed");
            } catch (IOException e) {
                Log.e(TAG, "cannot close the socket");
            }
            socket = null;
        } else Log.w(TAG, "socket is null");

        sending = false;
    }
}
