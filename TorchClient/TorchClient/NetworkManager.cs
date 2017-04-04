using System.Collections.Generic;
using System.Net.Sockets;
using System.Net;
using System.Windows.Media.Imaging;
using System;

namespace TorchClient
{
    class NetworkManager
    {
        private const string TAG = "NetworkManager";

        private TcpClient socket;
        private IPEndPoint ip;
        private MainWindow xamlWindow;
        private bool receiving;

        public NetworkManager(int port, MainWindow xamlWindow)
        {
            Log.Debug(TAG, "NetowrkManager:");
            this.xamlWindow = xamlWindow;
            receiving = false;
            socket = new TcpClient();
            ip = new IPEndPoint(IPAddress.Parse("127.0.0.1"), port);

            Log.Debug(TAG, $"Connecting to {ip.Address}:{ip.Port}");
            socket.Connect(ip);
            Log.Debug(TAG, "Connected");
        }

        private void HandleFrame()
        {
            Log.Debug(TAG, "HandleFrame:");
            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(socket.GetStream(), BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.Default);
                int count = decoder.Frames.Count;
                xamlWindow.SetFrame(decoder.Frames[count - 1]);
            }
            catch(Exception e)
            {
                Log.Error(TAG, "Unable to create JpegBitmapDecoder", e);
            }
        }

        public void StartReceiving()
        {
            Log.Debug(TAG, "StartReceiving:");
            receiving = true;
            while (receiving)
                HandleFrame();
            Log.Debug(TAG, "Endded receiving");
        }

        public void StopReceiving()
        {
            Log.Debug(TAG, "StopReceiving:");
            receiving = false;
        }
    }
}
