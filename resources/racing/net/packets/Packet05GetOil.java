package racing.net.packets;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet05GetOil extends Packet {
     
    private List<Point> oilPoints = new ArrayList<>();

    public Packet05GetOil(byte[] data) {
        super(05);
        String[] dataArray = readData(data).split(",");
        int size = Integer.parseInt(dataArray[0]);
        for (int i=0; i < size; i++) {
            int x = Integer.parseInt(dataArray[2*i+1]);
            int y = Integer.parseInt(dataArray[2*i+2]);
            oilPoints.add(new Point(x, y));
        }
    }

    public Packet05GetOil(List<Point> oilPoints) {
        super(05);
        this.oilPoints.addAll(oilPoints);
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
        data = "05" + this.oilPoints.size();
        for(Point oilPoint : oilPoints) {
            data += "," + oilPoint.x + "," + oilPoint.y;
        }
        return data.getBytes();
    }
    
    public List<Point> getOil() {
        return oilPoints;
    }
}
