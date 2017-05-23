package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet23Empty extends Packet {
    
    public Packet23Empty(byte[] data) {
        super(23);
    }
    
    public Packet23Empty() {
        super(23);
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
        return ("23").getBytes();
    }

}
