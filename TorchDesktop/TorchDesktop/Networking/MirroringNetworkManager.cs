using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Windows.Media.Imaging;
using TorchDesktop.Networking.Protos;

namespace TorchDesktop.Networking
{
    class MirroringNetworkManager
    {
        public delegate void BitmapHandler(BitmapSource bitmap);
        public const int PORT = 27015;

        private TcpListener tcpListener;
        private Thread looper;
        private bool working;

        public event BitmapHandler ReceivedFrame;

        public MirroringNetworkManager()
        {
            working = false;
        }

        private byte[] DeserializeMessage(NetworkStream stream)
        {
            return ByteArray.Parser.ParseDelimitedFrom(stream).Data.ToByteArray();
        }

        private BitmapFrame DecompressBitmap(byte[] data)
        {
            MemoryStream memoryStream = new MemoryStream(data);

            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(memoryStream, BitmapCreateOptions.None, BitmapCacheOption.Default);
                return decoder.Frames[decoder.Frames.Count - 1];
            }
            catch(Exception e)
            {
                Trace.WriteLine(e);
                return null;
            }
        }

        private void ListenerLooper()
        {
            while (working)
            {
                TcpClient client = tcpListener.AcceptTcpClient();
                while(client.Connected)
                {
                    try
                    {
                        byte[] data = DeserializeMessage(client.GetStream());
                        BitmapFrame frame = DecompressBitmap(data);
                        if (frame != null)
                            ReceivedFrame?.Invoke(frame);
                    }
                    catch(Exception e)
                    {
                        Trace.WriteLine(e);
                        client.Close();
                    }
                }
            }
        }

        public void StartReceiving()
        {
            tcpListener = new TcpListener(IPAddress.Any, PORT);
            tcpListener.Start();
            looper = new Thread(ListenerLooper);
            looper.Start();
            working = true;
        }

        public void StopListening()
        {
            tcpListener?.Stop();
            tcpListener = null;
            looper?.Abort();
            looper = null;
            working = false;
        }
    }
}
