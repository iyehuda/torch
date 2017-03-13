using System.Windows;
using System.Threading;

namespace TorchClient
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        Thread frameReciver;
        public MainWindow()
        {
            InitializeComponent();
            NetworkManager manager = new NetworkManager(27015, this);//some port
            frameReciver = new Thread(new ThreadStart(manager.handleRecievedBitmap));
        }
    }
}
