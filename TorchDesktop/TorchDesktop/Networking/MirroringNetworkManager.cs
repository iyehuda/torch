﻿using System;
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
        public const int PORT = 27014;

        private TcpClient tcpClient;
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
            tcpClient = new TcpClient("127.0.0.1", PORT);

            while(tcpClient.Connected && working)
            {
                try
                {
                    byte[] data = DeserializeMessage(tcpClient.GetStream());
                    BitmapFrame frame = DecompressBitmap(data);
                    if (frame != null)
                        ReceivedFrame?.Invoke(frame);
                }
                catch(Exception e)
                {
                    Trace.WriteLine(e);
                }
            }

            Trace.WriteLine("Closing TcpClient");
            tcpClient.Close();
            tcpClient = null;
        }

        public void StartReceiving()
        {
            looper = new Thread(ListenerLooper);
            looper.Start();
            working = true;
        }

        public void StopListening()
        {
            if (looper != null)
            {
                if (looper.IsAlive)
                    looper.Abort();
                looper = null;
            }
            if(tcpClient != null)
            {
                tcpClient.Close();
                tcpClient = null;
            }
            working = false;
        }
    }
}
