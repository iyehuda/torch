using System;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Windows.Controls;
using System.Windows.Media.Imaging;

namespace TorchDesktop
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
#pragma warning disable CS0618 // Type or member is obsolete
            listener = new TcpListener(port);
#pragma warning restore CS0618 // Type or member is obsolete
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

        private int DeserializeInt32(Stream stream)
        {
            Log.Debug(TAG, "DeserializeInt32:");
            byte[] rawLength = new byte[4];
            stream.Read(rawLength, 0, 4);
            Array.Reverse(rawLength);
            return BitConverter.ToInt32(rawLength, 0);
        }

        private BitmapFrame DeserializeImage(Stream stream)
        {
            Log.Debug(TAG, "DeserializeImage:");
            int dataLength = DeserializeInt32(stream);
            Log.Debug(TAG, $"Data length is {dataLength}");
            byte[] data = new byte[dataLength];
            stream.Read(data, 0, dataLength);
            MemoryStream mStream = new MemoryStream(data);
            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(mStream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.Default);
                int count = decoder.Frames.Count;
                return decoder.Frames[count - 1];
            }
            catch(Exception e)
            {
                Log.Error(TAG, "Unable to deserialize image", e);
                return null;
            }
        }

        private void HandleFrame(NetworkStream stream)
        {
            Log.Debug(TAG, "HandleFrame:");
            BitmapFrame frame = DeserializeImage(stream);
            if (frame != null)
                xamlWindow.SetFrame(frame);
        }
    }
}
