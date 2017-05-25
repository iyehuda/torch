package magshimim.torchmobile.networking;

import java.io.IOException;

public interface INetworkManager {
    void connect(String address, int port) throws IOException;

    void sendData(byte[] data);

    void disconnect();
}
