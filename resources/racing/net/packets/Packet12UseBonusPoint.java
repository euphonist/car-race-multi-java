package racing.net.packets;

import java.awt.Point;
import racing.BonusPoint;
import racing.net.GameClient;
import racing.net.GameServer;


public class Packet12UseBonusPoint extends Packet {
    
    private Point point;
    
    public Packet12UseBonusPoint(byte[] data) {
        super(12);
        String[] dataArray = readData(data).split(",");
        int x = Integer.parseInt(dataArray[0]);
        int y = Integer.parseInt(dataArray[1]);
        point = new Point(x, y);
    }
    
    public Packet12UseBonusPoint(BonusPoint point) {
        super(12);
        this.point = new Point(point.getX(), point.getY());
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
        return ("12"+point.x+","+point.y).getBytes();
    }
    
    public Point getPoint() {
        return point;
    }
}