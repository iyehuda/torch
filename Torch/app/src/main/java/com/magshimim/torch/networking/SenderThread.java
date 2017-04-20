package com.magshimim.torch.networking;
import android.nfc.Tag;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.google.protobuf.ByteString;
import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.model.ByteArrayOuterClass;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.util.Queue;


class SenderThread extends Thread {
    private static final String TAG = "SenderThread";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final Queue<byte[]> dataToSend; // The messages queue
    private Socket socket; // Connection to the server
    private DataOutputStream out; // Stream to write messages
    private boolean sending; // Determines if sending in action

    /**
     * Initializes inner data, including messages queue and socket
     * @param name The thread's name
     * @param dataToSend Messages queue
     * @param socket Connection to the server
     */
    SenderThread(String name, final Queue<byte[]> dataToSend, Socket socket) {
        super(name);
        if(DEBUG) Log.d(TAG, " SenderThread:");
        this.dataToSend = dataToSend;
        this.socket = socket;
        sending = false;
    }

    /**
     * Pops the last message from the queue
     * @return Byte array, could be null
     */
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

            byte[] data = null;
            while(!dataToSend.isEmpty()) {
                byte[] temp = dataToSend.poll();
                if(temp != null && temp.length > 0)
                    data = temp;    // Get the last frame from the queue,
                                    // the rest are not interesting
            }
            if(data == null) Log.w(TAG, "null queue element");
            return data;
        }
    }

    /**
     * Converts a byte array to hexadecimal string
     * @param data A byte array
     * @return A string
     */
    private String hexString(byte[] data) {
        return String.format("%032x", new BigInteger(1, data));
    }

    /**
     * Calculates MD5 hash of a byte array
     * @param data a byte array
     * @return A string
     * @throws NoSuchAlgorithmException Would never be thrown
     */
    private String md5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] rawDigest = messageDigest.digest(data);
        return hexString(rawDigest);
    }

    /**
     * Sends a byte array to the server
     * @param data Data to send
     */
    private void send(byte[] data) {
        if (DEBUG) Log.d(TAG, "send:");
        if(out == null) {
            Log.w(TAG, "output stream is null");
            return;
        }
        try {
                // Create a protobuf object that wraps a byte array
                ByteArrayOuterClass.ByteArray byteArray = ByteArrayOuterClass.ByteArray.
                        newBuilder()
                        .setData(ByteString.copyFrom(data))
                        .build();
                byteArray.writeDelimitedTo(out);
                if(DEBUG) {
                    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    Date time = Calendar.getInstance().getTime();
                    Log.d(TAG, format.format(time));
                    Log.d(TAG, String.format("%d bytes sent", byteArray.getData().size()));
                    Log.d(TAG, "Data hash: " + md5(data));
                }
        } catch (Exception e) {
            Log.e(TAG, "could not send data");
        }
    }

    /**
     * Cleans up resources
     */
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

    /**
     * The entry point of the thread.
     * Iterates over the messages queue and sends the messages
     */
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

    /**
     * Sets sending field to false
     */
    void stopSending()
    {
        if(DEBUG) Log.d(TAG, "stopSending");
        sending = false;
    }
}
