using System.Windows;
using System.Threading;
using System.Drawing;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace TorchClient
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private const string TAG = "MainWindow";
        Thread frameReciver;

        public MainWindow()
        {
            InitializeComponent();
            Log.Debug(TAG, "MainWindow:");

            NetworkManager manager = new NetworkManager(27015, this); //some port
            Log.Debug(TAG, "network manager created");

            frameReciver = new Thread(new ThreadStart(manager.StartReceiving));
            frameReciver.Start();
            Log.Debug(TAG, "started receiving");
        }

        public void SetFrame(BitmapSource frame)
        {
            Log.Debug(TAG, "SetFrame:");
            Dispatcher.Invoke(delegate { currentFrame.Source = frame; });
            Log.Debug(TAG, "frame was set");
        }
    }
}
