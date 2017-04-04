using System.Collections.Generic;
using System.Net.Sockets;
using System.Net;
using System.Windows.Media.Imaging;
using System;
using System.Threading;

namespace TorchClient
{
    class NetworkManager
    {
        private const string TAG = "NetworkManager";

        private MainWindow xamlWindow;

        private TcpListener listener;
        private Thread looper;
        private bool receiving;

        public NetworkManager(int port, MainWindow xamlWindow)
        {
            Log.Debug(TAG, "NetowrkManager:");
            this.xamlWindow = xamlWindow;
            receiving = false;
        }

        public void StartReceiving(int port)
        {
            Log.Debug(TAG, "StartReceiving:");
            listener = new TcpListener(port);
            Log.Debug(TAG, "Created TcpListener");
            listener.Start();
            Log.Debug(TAG, $"Listening on port {port}");
            looper = new Thread(ListenerLooper);
            looper.Start();
            Log.Debug(TAG, "Looper started");
        }
        
        public void StopReceiving()
        {
            Log.Debug(TAG, "StopReceiving:");
            receiving = false;
        }

        private void ListenerLooper()
        {
            Log.Debug(TAG, "ListenerLooper:");
            TcpClient client = listener.AcceptTcpClient();
            Log.Debug(TAG, $"Client accepted at {client.Client.RemoteEndPoint}");
            listener.Stop();
            Log.Debug(TAG, "Stopped listening to new connections");
            receiving = true;
            Log.Debug(TAG, "Starting receiving connections");
            while (receiving)
                HandleFrame(client.GetStream());
        }

        private void HandleFrame(NetworkStream stream)
        {
            Log.Debug(TAG, "HandleFrame:");
            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(stream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.Default);
                int count = decoder.Frames.Count;
                xamlWindow.SetFrame(decoder.Frames[count - 1]);
            }
            catch(Exception e)
            {
                Log.Error(TAG, "Unable to create JpegBitmapDecoder", e);
            }
        }
    }
}
