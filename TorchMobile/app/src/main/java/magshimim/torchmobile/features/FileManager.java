package magshimim.torchmobile.features;

import com.google.protobuf.ByteString;

import magshimim.torchmobile.networking.NetworkManager;
import magshimim.torchmobile.networking.protos.TorchMessageOuterClass;

public class FileManager extends FeatureManager {

    public FileManager(NetworkManager networkManager) {
        super(networkManager);
    }

    @Override
    protected void handleMessage(TorchMessageOuterClass.TorchMessage message) {
        TorchMessageOuterClass.FileRequest request = message.getFileRequest();
        TorchMessageOuterClass.FileResponse.Builder response = TorchMessageOuterClass.FileResponse.newBuilder();
        if(request.getFileType() == TorchMessageOuterClass.FileRequest.FileType.DIRECTORY) {
            TorchMessageOuterClass.DirecotryData.Builder dir = TorchMessageOuterClass.DirecotryData.newBuilder()
                    .setDirectoryName(request.getPath());
            for(String file : readDirectory(request.getPath()))
                dir.addFiles(file);
            response.setDirectory(dir.build());
        }
        else if(request.getFileType() == TorchMessageOuterClass.FileRequest.FileType.FILE)
            response.setFile(TorchMessageOuterClass.FileData.newBuilder()
            .setFileName(request.getPath())
            .setData(ByteString.copyFrom(readFile(request.getPath())))
            .build());

    }

    @Override
    protected boolean messageFilter(TorchMessageOuterClass.TorchMessage message) {
        return message.getType() != TorchMessageOuterClass.TorchMessage.MessageType.FILEREQUEST ||
                message.getFileRequest() == null;
    }

    private String[] readDirectory(String path) {
        return new String[0];
    }

    private byte[] readFile(String path) {
        return new byte[0];
    }
}
