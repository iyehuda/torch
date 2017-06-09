using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using TorchDesktop.Networking;

namespace TorchDesktop.Pages
{
    /// <summary>
    /// Interaction logic for LoginPage.xaml
    /// </summary>
    public partial class LoginPage : Page
    {
        const int PORT = 27014;

        NetworkManager networkManager;

        public LoginPage()
        {
            InitializeComponent();
            connectButton.Click += OnConnectButtonClick;
        }

        private void OnConnectButtonClick(object sender, RoutedEventArgs e)
        {
            connectButton.IsEnabled = false;
            networkManager = new NetworkManager();
            networkManager.Connected += OnConnected;
            networkManager.Disconnected += OnDisconnected;
            networkManager.Error += OnError;
            networkManager.Connect(addressTextBox.Text, PORT);
        }

        private void OnDisconnected(bool hadError)
        {
            Dispatcher.Invoke(() => connectButton.IsEnabled = true);
        }

        private void OnError(string error)
        {
            Dispatcher.Invoke(() =>
            {
                connectButton.IsEnabled = true;
                errorTextBlock.Text = error;
                errorTextBlock.IsEnabled = true;
            });
        }

        private void OnConnected()
        {
            Dispatcher.Invoke(() =>
            {
                errorTextBlock.Text = "";
                errorTextBlock.IsEnabled = false;
                NavigationService.Navigate(new MirroringPage(networkManager));
            });
        }
    }
}
