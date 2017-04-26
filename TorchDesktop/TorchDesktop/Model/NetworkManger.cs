using TorchDesktop.Model;

using System;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Windows.Media.Imaging;
using System.Security.Cryptography;
using System.Text;

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

        private void SaveImage(string fileName, byte[] data)
        {
            try
            {
                using (var fs = new FileStream(fileName, FileMode.Create, FileAccess.Write))
                {
                    fs.Write(data, 0, data.Length);
                    Log.Ok(TAG, $"Wrote data to {fileName}");
                }
            }
            catch(Exception e)
            {
                Log.Error(TAG, $"Could not save data to {fileName}", e);
            }
        }

        private BitmapFrame BytesToBitmapFrame(byte[] data)
        {
            Log.Debug(TAG, "BytesToBitmapFrame:");
            MemoryStream mStream = new MemoryStream(data);
            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(mStream, BitmapCreateOptions.None, BitmapCacheOption.Default);
                int count = decoder.Frames.Count;
                return decoder.Frames[count - 1];
            }
            catch (Exception e)
            {
                Log.Error(TAG, "Unable to deserialize image", e);
                Log.Debug(TAG, "Trying to save it");
                SaveImage("lastFrame.jpg", data);
                return null;
            }
        }

        private BitmapFrame DeserializeImage(Stream stream)
        {
            Log.Debug(TAG, "DeserializeImage:");

            ByteArray data;
            try
            {
                data = ByteArray.Parser.ParseDelimitedFrom(stream);
                Log.Debug(TAG, $"Data length is {data.Data.Length}");
            }
            catch(Exception e)
            {
                Log.Error(TAG, "Cannot deserialize data", e);
                return null;
            }

            byte[] innerData = data.Data.ToByteArray();
            // Log.Debug(TAG, $"Data hash: {Md5(innerData)}");
            
            return BytesToBitmapFrame(innerData);
        }

        private void HandleFrame(NetworkStream stream)
        {
            Log.Debug(TAG, "HandleFrame:");
            BitmapFrame frame = DeserializeImage(stream);
            if (frame != null)
                xamlWindow.SetFrame(frame);
            else
            {
                Log.Warning(TAG, "Client and server are out of sync, shutting down the connection");
                receiving = false;
            }
        }

        private string Md5(byte[] data)
        {
            HashAlgorithm algorithm = MD5.Create();
            byte[] hash = algorithm.ComputeHash(data);
            StringBuilder sb = new StringBuilder();
            foreach (byte b in hash)
                sb.Append(b.ToString("X2"));
            return sb.ToString();
        }
    }
}
