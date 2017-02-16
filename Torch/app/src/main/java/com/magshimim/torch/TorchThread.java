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
    private String name;
    private Queue<Bitmap> framestoSend;
    private DatagramSocket socket;
    public TorchThread(String name,Queue<Bitmap> framesTosend,DatagramSocket socket){
        super(name);
        this.framestoSend=framestoSend;
        this.socket=socket;
    }
    public void run()
    {
        Bitmap frameTosend = framestoSend.poll();
        ByteArrayOutputStream streamToSend = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(frameTosend.getByteCount());
        frameTosend.copyPixelsToBuffer(buffer);
        byte[] arraytoSend = buffer.array();
        DatagramPacket packet = new DatagramPacket(arraytoSend,arraytoSend.length);
        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e("NetworkManager,Frame", e.getMessage());
        }
    }
}
