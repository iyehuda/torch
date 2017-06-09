using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;
using TorchDesktop.Networking;
using TorchDesktop.Networking.Protos;

namespace TorchDesktop.FeatureManagers
{
    public class MirroringManager : FeatureManager
    {
        public event Callback<BitmapSource> FrameReceived;

        public MirroringManager(NetworkManager networkManager)
            :base(networkManager)
        {
            networkManager.Message += OnMessage;
        }

        public override void Close()
        {
            base.Close();
            networkManager.Message -= OnMessage;
        }

        private void OnMessage(TorchMessage message)
        {
            if (message.Type != TorchMessage.Types.MessageType.Frame || message.Frame == null)
                return;
            Task.Run(() =>
            {
                BitmapSource result = DecompressBitmap(message.Frame.Data.ToByteArray());
                if (result != null)
                    EmitFrameReceived(result);
                else
                    Trace.TraceWarning("Could not decompress bitmap");
            });
        }

        private void EmitFrameReceived(BitmapSource bitmap)
        {
            FrameReceived?.Invoke(bitmap);
        }

        private static BitmapFrame DecompressBitmap(byte[] data)
        {
            MemoryStream memoryStream = new MemoryStream(data);

            try
            {
                JpegBitmapDecoder decoder = new JpegBitmapDecoder(memoryStream, BitmapCreateOptions.None, BitmapCacheOption.Default);
                return decoder.Frames[decoder.Frames.Count - 1];
            }
            catch (Exception e)
            {
                Trace.WriteLine(e);
                return null;
            }
        }
    }
}
