package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet04Bomb extends Packet {

    private String username;
    private double x, y;
    private double angle;

    public Packet04Bomb(byte[] data) {
        super(04);
        String[] dataArray = readData(data).split(",");
        this.username = dataArray[0];
        this.x = Double.parseDouble(dataArray[1]);
        this.y = Double.parseDouble(dataArray[2]);
        this.angle = Double.parseDouble(dataArray[3]);
    }

    public Packet04Bomb(String username, double x, double y, double angle) {
        super(04);
        this.username = username;
        this.x = x;
        this.y = y;
        this.angle = angle;
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
        return ("04" + this.username + "," + this.x + "," + this.y + 
                "," + this.angle).getBytes();
    }

    public String getUsername() {
        return username;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getAngle() {
        return angle;
    }

}