package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet21ConfirmLogin extends Packet {
     
    
    public Packet21ConfirmLogin(byte[] data) {
        super(21);
    }
    
    public Packet21ConfirmLogin() {
        super(21);
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
        return ("21").getBytes();
    }
}
