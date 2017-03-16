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
        sending = false;
    }

    @Override
    public void run() {
        if(DEBUG) Log.d(TAG, "run");
        try {
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, "cannot create DataOutputStream", e);
            return;
        }
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
            try {
                out.write(arrayToSend);
                if(DEBUG) Log.d(TAG, "frame sent");
            } catch (IOException e) {
                Log.e(TAG, "Could not send frame", e);
                break;
            }
        }

        try {
            socket.close();
            socket = null;
        } catch (IOException e) {
            Log.e(TAG, "could not close socket", e);
        }

        try {
            out.close();
            out = null;
        } catch (IOException e) {
            Log.e(TAG, "could not close output stream", e);
        }

        sending = false;
    }

    public void stopSending()
    {
        if(DEBUG) Log.d(TAG, "stopSending");
        sending = false;
    }
}
