using System.ComponentModel;
using System.Windows;
using System.Windows.Media.Imaging;
using TorchDesktop.FeatureManagers;
using TorchDesktop.Networking;

namespace TorchDesktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private NetworkManager networkManager;
        private MirroringManager mirroringManager;

        public MainWindow()
        {
            InitializeComponent();

            networkManager = new NetworkManager();
            networkManager.Connected += NetworkConnectedCallback;
            networkManager.Disconnected += NetworkDisconnectedCallback;
            networkManager.Error += NetworkErrorCallback;
            networkManager.Connect("127.0.0.1", 27014);
            
            Closing += Cleanup;
        }

        private void NetworkConnectedCallback()
        {
            MessageBox.Show("Connected");
            StartMirroring();
        }

        private void NetworkDisconnectedCallback(bool hadError)
        {
            Dispatcher.Invoke(Close);
        }

        private void NetworkErrorCallback(string error)
        {
            MessageBox.Show(error);
        }

        private void StartMirroring()
        {
            mirroringManager = new MirroringManager(networkManager);
            mirroringManager.FrameReceived += ReceivedFrameCallback;
        }

        private void SetFrame(BitmapSource frame)
        {
            currentFrame.Source = frame;
        }

        private void ReceivedFrameCallback(BitmapSource bitmap)
        {
            Dispatcher.Invoke(() => SetFrame(bitmap));
        }

        private void Cleanup(object sender, CancelEventArgs e)
        {
            if (mirroringManager != null)
            {
                mirroringManager.Close();
                mirroringManager = null;
            }

            if(networkManager.Working)
                networkManager.Close();
        }
    }
}
