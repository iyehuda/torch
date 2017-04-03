package com.magshimim.torch.networking;
import android.support.annotation.Nullable;
import android.util.Log;

import com.magshimim.torch.BuildConfig;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;


class SenderThread extends Thread {
    private static final String TAG = "SenderThread";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final Queue<byte[]> dataToSend;
    private Socket socket;
    private DataOutputStream out;
    private boolean sending;


    SenderThread(String name, final Queue<byte[]> dataToSend, Socket socket) {
        super(name);
        if(DEBUG) Log.d(TAG, " SenderThread:");
        this.dataToSend = dataToSend;
        this.socket = socket;
        sending = false;
    }

    @Nullable
    private byte[] getData() {
        if(DEBUG) Log.d(TAG, "getData:");
        synchronized (dataToSend) {
            try {
                dataToSend.wait(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error waiting on queue", e);
                return null;
            }
            if(dataToSend.isEmpty()) {
                Log.w(TAG, "queue is empty");
                return null;
            }
            byte[] data = dataToSend.poll();
            if(data == null) Log.w(TAG, "null queue element");
            return data;
        }
    }

    private void send(byte[] data) {
        if (DEBUG) Log.d(TAG, "send:");
        if(out == null) {
            Log.w(TAG, "output stream is null");
            return;
        }
        try {
            out.write(data,0 , data.length);
            if(DEBUG) Log.d(TAG, String.format("%d bytes sent", data.length));
        } catch (Exception e) {
            Log.e(TAG, "could not send data");
        }
    }

    private void cleanup() {
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "could not close socket", e);
            }
            socket = null;
        } else Log.w(TAG, "socket is null");

        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "could not close output stream", e);
            }
            out = null;
        } else Log.w(TAG, "out is null");

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
        byte[] data;

        while (sending) {
            if(DEBUG) Log.d(TAG, "waiting for frame");
            data = getData();
            if(data != null)
                send(data);
        }

        cleanup();
    }

    void stopSending()
    {
        if(DEBUG) Log.d(TAG, "stopSending");
        sending = false;
    }
}
