using TorchDesktop.Networking;

namespace TorchDesktop.FeatureManagers
{
    public abstract class FeatureManager
    {
        protected NetworkManager networkManager;

        public event Callback Closed;

        public FeatureManager(NetworkManager networkManager)
        {
            networkManager.Disconnected += OnDisconnected;
            this.networkManager = networkManager;
        }

        public virtual void Close()
        {
            networkManager.Disconnected -= OnDisconnected;
        }

        protected void EmitClosed()
        {
            Closed?.Invoke();
        }

        protected void OnDisconnected(bool value)
        {
            EmitClosed();
        }
    }
}
