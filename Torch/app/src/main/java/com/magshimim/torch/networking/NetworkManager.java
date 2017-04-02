package com.magshimim.torch.networking;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
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

    private Socket socket;
    private SenderThread frameSender;
    private final Queue<byte[]> framesToSend;
    private boolean sending;

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

    private byte[] compressBitmap(Bitmap bitmap) {
        // Compress to JPEG
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] compressBytes = byteArrayOutputStream.toByteArray();
        if (DEBUG) Log.d(TAG, "Compressed frame");
        return compressBytes;
    }


    public void sendFrame(Bitmap frame)
    {
        if(DEBUG) Log.d(TAG, "sendFrame");

        if(!sending) {
            Log.w(TAG, "Not connected");
            return;
        }
        if(frame == null) {
            Log.w(TAG, "frame is null");
            return;
        }

        byte[] data = compressBitmap(frame);
        synchronized (framesToSend) {
            framesToSend.add(data);
            if(DEBUG) Log.d(TAG, "added frame to queue");
            framesToSend.notify();
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
