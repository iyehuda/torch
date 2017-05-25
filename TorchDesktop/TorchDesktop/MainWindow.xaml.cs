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

            manager = new MirroringNetworkManager();
            manager.ReceivedFrame += ReceivedFrameCallback;
            manager.StartReceiving();

            Closing += Cleanup;
        }

        private void SetFrame(BitmapSource frame)
        {
            double aspect = frame.Width / frame.Height;
            currentFrame.Width = Height * aspect;
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
