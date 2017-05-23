
package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet14StartRace extends Packet {
    
    private long time;
    
    public Packet14StartRace(byte[] data) {
        super(14);
        String dataArray = readData(data);
        time = Long.parseLong(dataArray);
    }
    
    public Packet14StartRace(Long time) {
        super(14);
        this.time = time;
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
        return ("14"+this.time).getBytes();
    }
    
    public long getTime() {
        return time;
    }
}
