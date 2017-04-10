package com.magshimim.torch.networking;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.support.annotation.NonNull;
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
     * Compress a bitmap to JPEG format
     * @param bitmap A bitmap
     * @return Byte array contains JPEG formatted data
     */
    private byte[] compressBitmap(Bitmap bitmap) {
        // Compress to JPEG
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
        byte[] compressBytes = byteArrayOutputStream.toByteArray();
        if (DEBUG) Log.d(TAG, "Compressed frame");
        return compressBytes;
    }

    /**
     * Add a frame to the sending queue
     * @param frame The frame to be sent
     */
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
        if(frame.isRecycled()) {
            Log.w(TAG, "frame is recycled");
            return;
        }

        byte[] data = compressBitmap(frame);

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
