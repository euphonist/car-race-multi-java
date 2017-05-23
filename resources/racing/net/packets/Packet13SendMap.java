package racing.net.packets;

import java.io.File;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet13SendMap extends Packet {
    
    private final File file;
    
    public Packet13SendMap(byte[] data) {
        super(13);
        String dataArray = readData(data);
        this.file = new File(dataArray);
    }
    
    public Packet13SendMap(File file) {
        super(13);
        this.file = file;
    }
    
    @Override
    public void writeData(GameClient client) {
        client.sendData(getData());
    }

    @Override
    public void writeData(GameServer server) {
        server.sendDataToAllClients(getData());
    }

    @Override
    public byte[] getData() {
        return ("13" + this.file.getName()).getBytes();
    }
    
    public File getFile() {
        return file;
    }
}
