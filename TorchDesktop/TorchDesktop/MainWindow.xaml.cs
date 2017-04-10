using System.Windows;
using System.Windows.Media.Imaging;

namespace TorchDesktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private const string TAG = "MainWindow";
        private const int PORT = 27015;

        public MainWindow()
        {
            InitializeComponent();
            Log.Debug(TAG, "MainWindow:");

            NetworkManager manager = new NetworkManager(PORT, this);
            Log.Debug(TAG, "network manager created");

            manager.StartReceiving(27015);
            Log.Debug(TAG, "started receiving");
        }

        public void SetFrame(BitmapSource frame)
        {
            Log.Debug(TAG, "SetFrame:");
            Dispatcher.Invoke(() => SetFrameComponent(frame));
            Log.Debug(TAG, "frame was set");
        }

        private void SetFrameComponent(BitmapSource frame)
        {
            Log.Debug(TAG, "SetFrameComponent");
            // currentFrame.Height = Height = frame.Height;
            // currentFrame.Width = Width = frame.Width;
            Log.Debug(TAG, "Dimensions are set");
            currentFrame.Source = frame;
            Log.Debug(TAG, "Image source is set");
        }
    }
}
