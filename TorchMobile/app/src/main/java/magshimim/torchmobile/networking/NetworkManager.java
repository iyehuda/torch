package magshimim.torchmobile.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import magshimim.torchmobile.networking.protos.TorchMessageOuterClass;
import magshimim.torchmobile.utils.Event;

public class NetworkManager {
    private boolean working;
    private int port;
    private final Object writeLock;
    private String host;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;
    private Thread workerThread;

    public Event<Void> onConnected;
    public Event<Boolean> onDisconnected;
    public Event<Exception> onError;
    public Event<TorchMessageOuterClass.TorchMessage> onMessage;

    public NetworkManager() {
        onConnected = new Event<>();
        onDisconnected = new Event<>();
        onError = new Event<>();
        onMessage = new Event<>();
        working = false;
        writeLock = new Object();
    }

    public boolean isConnected() {
        return working &&
                socket != null &&
                socket.isConnected();
    }

    public void connect(String host, int port) {
        if(working || workerThread != null)
            throw new IllegalStateException("Connection has already been established or started");

        this.host = host;
        this.port = port;
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveLooper();
            }
        });
        workerThread.start();
    }

    public void close() {
        close(false);
    }

    public synchronized void send(TorchMessageOuterClass.TorchMessage message) {
        if(message == null)
            return;

        if(!canWrite())
            throw new IllegalStateException("Output stream is closed");

        try {
            synchronized (writeLock) {
                message.writeDelimitedTo(outputStream);
            }
        } catch (IOException e) {
            if(working) {
                onError.invoke(e);
                close(true);
            } else e.printStackTrace();
        }
    }

    private boolean canRead() {
        return isConnected() &&
                !socket.isInputShutdown() &&
                inputStream != null;
    }

    private boolean canWrite() {
        return isConnected() &&
                !socket.isOutputShutdown() &&
                outputStream != null;
    }

    private void close(boolean hadError) {
        if(!working)
            return;

        working = false;

        if(workerThread != null) {
            if(workerThread.getId() != Thread.currentThread().getId() && workerThread.isAlive())
                workerThread.interrupt();
            workerThread = null;
        }

        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                hadError = true;
                e.printStackTrace();
            }
            inputStream = null;
        }

        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                hadError = true;
                e.printStackTrace();
            }
            outputStream = null;
        }

        if(socket != null) {
            if (socket.isConnected())
                try {
                    socket.close();
                } catch (IOException e) {
                    hadError = true;
                    e.printStackTrace();
                }
            socket = null;
        }

        onDisconnected.invoke(hadError);
    }

    private void receiveLooper() {
        boolean hadError = false;

        try {
            socket = new Socket(host, port);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream((socket.getOutputStream()));
            working = true;
            onConnected.invoke(null);

            while (canRead()) {
                TorchMessageOuterClass.TorchMessage message = TorchMessageOuterClass.TorchMessage
                        .parseDelimitedFrom(inputStream);
                onMessage.invoke(message);
            }
        } catch (IOException e) {
            hadError = true;
            if(working) onError.invoke(e);
            else e.printStackTrace();
        }

        close(hadError);
    }
}
