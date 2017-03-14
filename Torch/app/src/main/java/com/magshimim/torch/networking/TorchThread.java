package com.magshimim.torch.networking;
import android.graphics.Bitmap;
import java.lang.Object;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.io.*;
import java.net.DatagramPacket;
import android.util.Log;

import com.magshimim.torch.BuildConfig;

import java.net.DatagramSocket;


public class TorchThread extends Thread {
    private static final String TAG = "TorchThread";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final Queue<Bitmap> framesToSend;
    private Socket socket;
    private DataOutputStream out;
    private boolean sending;

    public TorchThread(String name, final Queue<Bitmap> framesToSend, Socket socket){
        super(name);
        if(DEBUG) Log.d(TAG, "TorchThread");
        this.framesToSend = framesToSend;
        this.socket=socket;
        try {
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        sending = false;
    }

    @Override
    public void run() {
        if(DEBUG) Log.d(TAG, "run");
        sending = true;
        Bitmap frame;

        while (sending) {
            if(DEBUG) Log.d(TAG, "waiting for frame");
            synchronized (framesToSend) {
                try {
                    framesToSend.wait(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error", e);
                    continue;
                }
                if (!framesToSend.isEmpty()) {
                    frame = framesToSend.poll();
                    if(DEBUG) Log.d(TAG, "got frame");
                }
                else {
                    Log.w(TAG, "queue is empty");
                    continue;
                }
            }

            ByteBuffer buffer = ByteBuffer.allocate(frame.getByteCount());
            frame.copyPixelsToBuffer(buffer);
            if(DEBUG) Log.d(TAG, "copied frame to buffer");
            byte[] arrayToSend = buffer.array();
            DatagramPacket packet = new DatagramPacket(arrayToSend, arrayToSend.length);
            if(DEBUG) Log.d(TAG, "Created datagram packet, length = " + packet.getLength());
            try {
                out.write(arrayToSend);
                if(DEBUG) Log.d(TAG, "packet is sent");
            } catch (IOException e) {
                Log.e("NetworkManager,Frame", e.getMessage());
            }
        }

        try {
            socket.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopSending()
    {
        if(DEBUG) Log.d(TAG, "stopSending");
        sending = false;
    }
}
