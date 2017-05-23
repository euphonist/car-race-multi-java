package racing.net.packets;

import racing.Player;
import racing.net.GameClient;
import racing.net.GameServer;



public class Packet02Move extends Packet {

    private final String username;
    private final double x, y;

    private final double angle;
    private double speed;
    private final boolean hasOil;
    private final boolean hasBomb;
    private final long freezeTime;
    private final int lap;
    
    public Packet02Move(byte[] data) {
        super(02);
        String[] dataArray = readData(data).split(",");
        this.username = dataArray[0];
        this.x = Double.parseDouble(dataArray[1]);
        this.y = Double.parseDouble(dataArray[2]);
        this.angle = Double.parseDouble(dataArray[3]);
        this.speed = Double.parseDouble(dataArray[4]);
        this.hasOil = Integer.parseInt(dataArray[5]) == 1;
        this.hasBomb = Integer.parseInt(dataArray[6]) == 1;
        this.freezeTime = Long.parseLong(dataArray[7]);
        this.lap = Integer.parseInt(dataArray[8]);
        
    }

    public Packet02Move(String username, double x, double y, double angle,
            double speed, boolean hasOil, boolean hasBomb, long freezeTime, int lap) {
        super(02);
        this.username = username;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.hasOil = hasOil;
        this.hasBomb = hasBomb;
        this.freezeTime = freezeTime;
        this.lap = lap;
    }
    
    public Packet02Move(String username, int x, int y, double angle,
            double speed, boolean hasOil, boolean hasBomb, long freezeTime, int lap) {
        super(02);
        this.username = username;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.hasOil = hasOil;
        this.hasBomb = hasBomb;
        this.freezeTime = freezeTime;
        this.lap = lap;
    }
     
    public Packet02Move(Player player) {
        super(02);
        this.username = player.getUsername();
        this.x = player.getX();
        this.y = player.getY();
        this.angle = player.getAngle();
        this.speed = player.getSpeed();
        this.hasOil = player.ifHasOil();
        this.hasBomb = player.ifHasBomb();
        this.freezeTime = player.getFreezeTime();
        this.lap = player.getLap();
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
        return ("02" + this.username + "," + this.x + "," + this.y + "," + 
                this.angle + "," + this.speed + "," + (hasOil ? 1 : 0)
                + "," + (hasBomb ? 1 : 0) + "," + this.freezeTime + "," +
                this.lap).getBytes();

    }

    public String getUsername() {
        return username;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getAngle() {
        return angle;
    }
    
    public double getSpeed() {
        return speed;
    }

    public boolean hasOil() {
        return hasOil;
    }

    public boolean hasBomb() {
        return hasBomb;
    }
    
    public long getFreezeTime() {
        return freezeTime;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    public int getLap() {
        return this.lap;
    }
}
