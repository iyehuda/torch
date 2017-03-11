package com.magshimim.torch;
import android.graphics.Bitmap;
import java.lang.Object;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.io.*;
import java.net.DatagramPacket;
import android.util.Log;
import java.net.DatagramSocket;

/**
 * Created by User on 2/16/2017.
 */

public class TorchThread extends Thread {
    private static final String TAG = "TorchThread";
    private String name;
    private final Queue<Bitmap> framesToSend;
    private DatagramSocket socket;
    private boolean sending;

    public TorchThread(String name, final Queue<Bitmap> framesToSend, DatagramSocket socket){
        super(name);
        this.framesToSend = framesToSend;
        this.socket=socket;
        sending = false;
    }

    @Override
    public void run() {
        sending = true;
        Bitmap frame;

        while (sending) {
            try {
                framesToSend.wait(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error", e);
                continue;
            }
            synchronized (framesToSend) {
                if (!framesToSend.isEmpty())
                    frame = framesToSend.poll();
                else
                    continue;
            }

            ByteBuffer buffer = ByteBuffer.allocate(frame.getByteCount());
            frame.copyPixelsToBuffer(buffer);
            byte[] arrayToSend = buffer.array();
            DatagramPacket packet = new DatagramPacket(arrayToSend, arrayToSend.length);
            try {
                socket.send(packet);
            } catch (IOException e) {
                Log.e("NetworkManager,Frame", e.getMessage());
            }
        }
    }

    public void stopSending()
    {
        sending = false;
    }
}
