package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet22CannotLogin extends Packet {
    
    
    public Packet22CannotLogin(byte[] data) {
        super(22);
    }
    
    public Packet22CannotLogin() {
        super(22);
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
        return ("22").getBytes();
    }
}
