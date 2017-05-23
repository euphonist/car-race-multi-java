package racing.net.packets;

import java.awt.Point;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet07RemoveOil extends Packet {
    
    private Point point;
    
    public Packet07RemoveOil(byte[] data) {
        super(07);
        String[] dataArray = readData(data).split(",");
        int x = Integer.parseInt(dataArray[0]);
        int y = Integer.parseInt(dataArray[1]);
        point = new Point(x,y);
    }
    
    public Packet07RemoveOil(int x, int y) {
        super(07);
        point = new Point(x,y);
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
        return ("07"+point.x+","+point.y).getBytes();
    }
    
    public Point getPoint() {
        return point;
    }
    
}
