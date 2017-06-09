using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using TorchDesktop.FeatureManagers;
using TorchDesktop.Networking;

namespace TorchDesktop.Pages
{
    /// <summary>
    /// Interaction logic for MirroringPage.xaml
    /// </summary>
    public partial class MirroringPage : Page
    {
        MirroringManager mirroringManager;

        public MirroringPage(NetworkManager networkManager)
        {
            InitializeComponent();

            Loaded += OnPageLoaded;
            Unloaded += OnPageUnloaded;

            mirroringManager = new MirroringManager(networkManager);
        }

        private void OnPageLoaded(object sender, RoutedEventArgs e)
        {
            mirroringManager.Disconnected += OnMirroringDisconnected;
            mirroringManager.FrameReceived += SetFrame;
        }

        private void OnPageUnloaded(object sender, RoutedEventArgs e)
        {
            mirroringManager.Close();
        }

        private void OnMirroringDisconnected()
        {
            mirroringManager.Close();
            Dispatcher.Invoke(() => NavigationService.GoBack());
        }

        private void SetFrame(BitmapSource frame)
        {
            Dispatcher.Invoke(() => currentFrame.Source = frame);
        }
    }
}
