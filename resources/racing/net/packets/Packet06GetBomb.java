package racing.net.packets;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet06GetBomb extends Packet { 
    
    private List<Point> bombPoints = new ArrayList<>();

    public Packet06GetBomb(byte[] data) {
        super(06);
        String[] dataArray = readData(data).split(",");
        int size = Integer.parseInt(dataArray[0]);
        for (int i=0; i < size; i++) {
            int x = Integer.parseInt(dataArray[2*i+1]);
            int y = Integer.parseInt(dataArray[2*i+2]);
            bombPoints.add(new Point(x, y));
        }
    }

    public Packet06GetBomb(List<Point> bombPoints) {
        super(06);
        this.bombPoints.addAll(bombPoints);
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
        String data;
        data = "06" + this.bombPoints.size();
        for(Point oilPoint : bombPoints) {
            data += "," + oilPoint.x + "," + oilPoint.y;
        }
        return data.getBytes();
    }
    
    public List<Point> getBomb() {
        return this.bombPoints;
    }
    
}
