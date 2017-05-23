package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet15EndOfRace extends Packet {
    
    private long time;
    
    public Packet15EndOfRace(byte[] data) {
        super(15);
        time = System.currentTimeMillis();
    }

    public Packet15EndOfRace() {
        super(15);
        time = System.currentTimeMillis();
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
        return ("15"+time).getBytes();
    }
    
    public long getTime() {
        return time;
    }
}
