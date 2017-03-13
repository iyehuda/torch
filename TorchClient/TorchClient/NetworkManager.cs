using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Net;
using System.Drawing;
using System.IO;
using System.Drawing.Imaging;
using System.Threading;
using System.Windows.Media.Imaging;
using System.Windows.Media;


namespace TorchClient
{
    class NetworkManager
    {
        private UdpClient socket;
        private IPEndPoint ip;
        private Queue<RecievedMassage> massages;
        private MainWindow xamlWindow;
        public NetworkManager(int port,MainWindow xamlWindow)
        {
            socket = new UdpClient();
            ip = new IPEndPoint(IPAddress.Parse("127.0.0.1"), port);
            socket.Connect(ip);
            this.xamlWindow = xamlWindow;//need thw window to show to bitmap that recieved on the xaml form
        }
        public void handleRecievedBitmap()
        {
            byte[] array = socket.Receive(ref ip);
            MemoryStream ms = new MemoryStream(array);
            Bitmap toDraw = new Bitmap(ms);
            BitmapImage b = convertToBitmapImage(toDraw);
            ImageSource source = b;
            xamlWindow.fromServer.Source = source;
        }
        private BitmapImage convertToBitmapImage(Bitmap b)
        {
            MemoryStream ms = new MemoryStream();
            b.Save(ms, ImageFormat.Png);
            ms.Position = 0;
            BitmapImage bitmapImage = new BitmapImage();
            bitmapImage.BeginInit();
            bitmapImage.StreamSource = ms;
            bitmapImage.CacheOption = BitmapCacheOption.OnLoad;
            bitmapImage.EndInit();
            return bitmapImage;
        }
        
    }
}
