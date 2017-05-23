package racing.net.packets;

import racing.net.GameClient;
import racing.net.GameServer;

public class Packet03Oil extends Packet {

    private String username;
    private int x, y;
    private double angle;

    public Packet03Oil(byte[] data) {
        super(03);
        String[] dataArray = readData(data).split(",");
        this.username = dataArray[0];
        this.x = Integer.parseInt(dataArray[1]);
        this.y = Integer.parseInt(dataArray[2]);
        this.angle = Double.parseDouble(dataArray[3]);
    }

    public Packet03Oil(String username, int x, int y, double angle) {
        super(03);
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
        return ("03" + this.username + "," + this.x + "," + this.y + 
                "," + this.angle).getBytes();
    }

    public String getUsername() {
        return username;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public double getAngle() {
        return angle;
    }

}