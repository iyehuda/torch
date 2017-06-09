using System.ComponentModel;
using System.Windows;
using System.Windows.Media.Imaging;
using TorchDesktop.FeatureManagers;
using TorchDesktop.Networking;
using TorchDesktop.Pages;

namespace TorchDesktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
            currentPage.Content = new LoginPage();
        }
    }
}
