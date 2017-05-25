package magshimim.torchmobile.networking;

import android.util.Log;

import com.google.protobuf.ByteString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import magshimim.torchmobile.networking.protos.ByteArrayOuterClass;
import magshimim.torchmobile.utils.ObjectContainer;

class SenderThread extends Thread {
    private static final String TAG = "SenderThread";

    private final ObjectContainer<byte[]> dataToSend;
    private Socket socket; // Connection to the server
    private DataOutputStream out; // Stream to write messages
    private boolean sending; // Determines if sending in action

    /**
     * Initializes inner data, including messages queue and socket
     * @param name The thread's name
     * @param dataContainer Messages queue
     * @param connection Connection to the server
     */
    SenderThread(String name,
                 ObjectContainer<byte[]> dataContainer,
                 Socket connection) {
        super(name);
        dataToSend = dataContainer;
        socket = connection;
        sending = false;
    }

    private byte[] getData() {
        byte[] tmp;
        synchronized (dataToSend) {
            try {
                dataToSend.wait(1000);
            } catch (InterruptedException e) {
                return null;
            }
            tmp = dataToSend.get();
            dataToSend.set(null);
        }
        if(tmp != null && tmp.length > 0)
            return tmp;
        return null;
    }

    private void send(byte[] data) throws IOException {
        if(out == null || data == null || data.length == 0)
            return;
        try {
            /*TorchMessageOuterClass.TorchMessage.newBuilder()
                    .setType(TorchMessageOuterClass.TorchMessage.MessageType.FRAME)
                    .setFrame(TorchMessageOuterClass.ByteArray.newBuilder()
                    .setData(ByteString.copyFrom(data))
                    .build())
                    .build()
                    .writeDelimitedTo(out);*/
            ByteArrayOuterClass.ByteArray.newBuilder()
                    .setData(ByteString.copyFrom(data))
                    .build().writeDelimitedTo(out);
        } catch (Exception e) {
            Log.e(TAG, "Could not send data", e);
            throw e;
        }
    }

    private void cleanup() {
        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close DataOutputStream", e);
            }
            out = null;
        }
        socket = null;
        sending = false;
    }

    @Override
    public void run() {
        if(dataToSend == null || socket == null)
            return;
        try {
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, "Unable to create DataOutputStream", e);
            return;
        }

        sending = true;
        byte[] data;

        try {
            while (sending) {
                data = getData();
                if (data != null)
                    send(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while sending, cleaning up");
        }

        cleanup();
    }

    void stopSending() {
        if(!sending)
            return;
        interrupt();
        cleanup();
    }
}
