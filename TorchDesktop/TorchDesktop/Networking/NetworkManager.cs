using Google.Protobuf;
using System;
using System.Diagnostics;
using System.Net.Sockets;
using System.Threading;
using TorchDesktop.Networking.Protos;

namespace TorchDesktop.Networking
{
    public delegate void Callback();
    public delegate void Callback<T>(T value);

    public class NetworkManager
    {
        private bool working;
        private int port;
        private string host;
        private NetworkStream stream;
        private TcpClient client;
        private Thread workerThread;

        public bool Working
        {
            get { return working; }
        }

        public event Callback Connected;
        public event Callback<TorchMessage> Message;
        public event Callback<string> Error;
        public event Callback<bool> Disconnected;

        public NetworkManager()
        {
            working = false;
        }

        public void Connect(string host, int port)
        {
            if (working || workerThread != null)
                throw new InvalidOperationException("Already working");

            this.host = host;
            this.port = port;
            workerThread = new Thread(ReceiveLooper);
            workerThread.Start();
        }
        
        public void Close()
        {
            working = false;
            if (workerThread.IsAlive)
                workerThread.Abort();
            if (client.Connected)
                client.Close();
        }

        public void Send(TorchMessage message)
        {
            if (!working || stream == null || !stream.CanWrite)
                throw new InvalidOperationException("Cannot write message");
            
            message.WriteDelimitedTo(stream);
        }

        private void CloseConnection(bool hadError = false)
        {
            working = false;
            client.Close();
            Emit(nameof(Disconnected), hadError);
        }

        private void ReceiveLooper()
        {
            bool error = false;

            try
            {
                client = new TcpClient(host, port);
                stream = client.GetStream();
                working = true;
                Emit(nameof(Connected));

                while (working && client.Connected)
                {
                    TorchMessage message = TorchMessage.Parser.ParseDelimitedFrom(stream);
                    Emit(nameof(Message), message);
                }
            }
            catch (Exception e)
            {
                error = true;
                Emit(nameof(Error), "Disconnected");
                Emit(nameof(Disconnected), true);
            }

            if (working)
                CloseConnection(error);
        }

        private void Emit(string eventName, object parameter = null)
        {
            switch (eventName)
            {
                case nameof(Connected):
                    Connected?.Invoke();
                    break;
                case nameof(Message):
                    TorchMessage message = parameter as TorchMessage;
                    if (message != null)
                        Message?.Invoke(message);
                    else
                        Trace.TraceWarning("Tried to emit empty message");
                    break;
                case nameof(Error):
                    string error = parameter as string;
                    if (error != null)
                        Error?.Invoke(error);
                    else
                        Trace.TraceWarning("Tried to emit empty error");
                    break;
                case nameof(Disconnected):
                    if (parameter?.GetType() == typeof(bool))
                        Disconnected?.Invoke((bool)parameter);
                    else
                        Trace.TraceWarning("Tried to emit non boolean");
                    break;
                default:
                    Trace.TraceWarning($"Unknown event name '{eventName}'");
                    break;
            }
        }
    }
}
