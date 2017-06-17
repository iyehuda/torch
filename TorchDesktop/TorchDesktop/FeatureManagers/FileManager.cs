using TorchDesktop.Networking;
using TorchDesktop.Networking.Protos;

namespace TorchDesktop.FeatureManagers
{
    public class FileManager : FeatureManager
    {
        public class Directory
        {
            public string name;
            public string[] content;
        }

        public class File
        {
            public string name;
            public byte[] content;
        }

        public event Callback<Directory> DirectoryReceived;
        public event Callback<File> FileReceived;

        public FileManager(NetworkManager networkManager)
            : base(networkManager)
        {
            networkManager.Message += OnMessage;
        }

        public void GetDirectory(string path)
        {
            networkManager.Send(new TorchMessage()
            {
                Type = TorchMessage.Types.MessageType.Filerequest,
                FileRequest = new FileRequest()
                {
                    FileType = FileRequest.Types.FileType.Directory,
                    Path = path
                }
            });
        }

        public void GetFile(string path)
        {
            networkManager.Send(new TorchMessage()
            {
                Type = TorchMessage.Types.MessageType.Filerequest,
                FileRequest = new FileRequest()
                {
                    FileType = FileRequest.Types.FileType.File,
                    Path = path
                }
            });
        }

        private void OnMessage(TorchMessage message)
        {
            if (message == null || message.Type != TorchMessage.Types.MessageType.Fileresponse || message.FileResponse == null)
                return;

            FileResponse response = message.FileResponse;
            if (response.Directory != null)
            {
                string[] array = new string[response.Directory.Files.Count];
                response.Directory.Files.CopyTo(array, 0);
                DirectoryReceived?.Invoke(new Directory()
                {
                    name = response.Directory.DirectoryName,
                    content = array
                });
            }
            else if (response.File != null)
                FileReceived?.Invoke(new File()
                {
                    name = response.File.FileName,
                    content = response.File.Data.ToByteArray()
                });
        }
    }
}
