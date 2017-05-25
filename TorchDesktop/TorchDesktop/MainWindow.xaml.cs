using System;
using System.Diagnostics;
using System.Windows;
using System.Windows.Media.Imaging;
using TorchDesktop.Networking;

namespace TorchDesktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private MirroringNetworkManager manager;

        public MainWindow()
        {
            InitializeComponent();
            Trace.WriteLine("Started", "DEBUG");
            manager = new MirroringNetworkManager();
            manager.ReceivedFrame += ReceivedFrameCallback;
            manager.StartReceiving();

            Closing += Cleanup;
        }

        private void SetFrame(BitmapSource frame)
        {
            //double aspect = Convert.ToDouble(frame.Width) / frame.Height;
            //currentFrame.Width = Convert.ToInt32(Height * aspect);
            currentFrame.Source = frame;
        }

        private void ReceivedFrameCallback(BitmapSource bitmap)
        {
            Dispatcher.Invoke(() => SetFrame(bitmap));
        }

        private void Cleanup(object sender, System.ComponentModel.CancelEventArgs e)
        {
            manager.StopListening();
        }
    }
}
