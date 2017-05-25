package magshimim.torchmobile.networking;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import magshimim.torchmobile.utils.ObjectContainer;

public class MirroringNetworkManager implements INetworkManager {
    private final static String TAG = "MirroringNetworkManager";

    private final ObjectContainer<byte[]> currentData;
    private Socket socket;
    private SenderThread frameSender;
    private boolean sending = false;

    public MirroringNetworkManager() {
        Log.d(TAG, "Hello from the other side");
        currentData = new ObjectContainer<>();
        socket = new Socket();
        frameSender = new SenderThread("FrameSender", currentData, socket);
        sending = false;
    }

    public void connect(String address, int port) throws IOException, IllegalStateException {
        if(sending)
            throw new IllegalStateException("Already connected");
        SocketAddress socketAddress = new InetSocketAddress(address, port);
        socket.connect(socketAddress, 2000);
        if(!socket.isConnected())
            throw new IllegalStateException("Not connected");
        frameSender.start();
        sending = true;
    }

    public void sendData(byte[] data) {
        if(!sending || !frameSender.isAlive() || data == null || data.length == 0)
            return;
        synchronized (currentData) {
            currentData.set(data);
            currentData.notify();
        }
    }

    public void disconnect() {
        if(!sending)
            return;

        frameSender.stopSending();
        frameSender = null;

        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the socket", e);
        }

        socket = null;
        sending = false;
    }
}
