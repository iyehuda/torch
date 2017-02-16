package com.magshimim.torch.networking;

import android.graphics.Bitmap;

/**
 * An interface that defines the usage of a network component for sending frames to a server.
 */
public interface INetworkManager {

    void connect(String address,int port);
    /**
     * Send a single frame to the remote server.
     * @param frame The frame to be sent
     */
    void sendFrame();

    /**
     * Disconnects from the server if the implementation uses TCP,
     * otherwise clears endpoint properties.
     */
    void disconnect();
}
