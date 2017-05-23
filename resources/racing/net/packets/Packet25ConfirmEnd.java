package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet25ConfirmEnd extends Packet {
    
    private final String username;
    
    public Packet25ConfirmEnd(byte[] data) {
        super(25);
        username = readData(data);
    }
    
    public Packet25ConfirmEnd(String username) {
        super(25);
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
        return ("25"+this.username).getBytes();
    }
    
    public String getUsername() {
        return username;
    }
}
