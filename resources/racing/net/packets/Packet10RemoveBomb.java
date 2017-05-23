package racing.net.packets;

import java.awt.Point;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet10RemoveBomb extends Packet {
    
    private Point point;
    
    public Packet10RemoveBomb(byte[] data) {
        super(10);
        String[] dataArray = readData(data).split(",");
        int x = Integer.parseInt(dataArray[0]);
        int y = Integer.parseInt(dataArray[1]);
        point = new Point(x, y);
    }
    
    public Packet10RemoveBomb(int x, int y) {
        super(10);
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
        return ("10"+point.x+","+point.y).getBytes();
    }
    
    public Point getPoint() {
        return point;
    }
    
}