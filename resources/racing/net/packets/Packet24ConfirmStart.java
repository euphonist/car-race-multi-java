
package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet24ConfirmStart extends Packet {
    
    private final String username;
    
    public Packet24ConfirmStart(byte[] data) {
        super(24);
        username = readData(data);
    }
    
    public Packet24ConfirmStart(String username) {
        super(24);
        this.username = username;
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
        return ("24"+this.username).getBytes();
    }
    
    public String getUsername() {
        return username;
    }
}
