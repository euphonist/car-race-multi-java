package racing.net.packets;

import java.util.ArrayList;
import java.util.List;
import racing.BonusPoint;
import racing.BonusPoint.Bonus;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet11SendBonusPoints extends Packet {
    
    private List<BonusPoint> bonusPoints = new ArrayList<>();
    
    public Packet11SendBonusPoints(byte[] data) {
        super(11);
        String[] dataArray = readData(data).split(",");
        int size = Integer.parseInt(dataArray[0]);
        for (int i=0; i < size; i++) {
            int x = Integer.parseInt(dataArray[5*i+1]);
            int y = Integer.parseInt(dataArray[5*i+2]);
            boolean isActive = Integer.parseInt(dataArray[5*i+3]) == 1;
            long time = Long.parseLong(dataArray[5*i+4]);
            Bonus bonus = Bonus.valueOf(dataArray[5*i+5]);
            bonusPoints.add(new BonusPoint(x, y, isActive, time, bonus));
        }
    }
    
    public Packet11SendBonusPoints(List<BonusPoint> bonusPoints) {
        super(11);
        this.bonusPoints.addAll(bonusPoints);
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
        data = "11" + this.bonusPoints.size();
        for(BonusPoint bonusPoint : bonusPoints) {
            data += "," + bonusPoint.getX() + "," + bonusPoint.getY() + "," +
                    (bonusPoint.isActive()?"1":"0") + "," + bonusPoint.getTime() + "," +
                    bonusPoint.getBonus().name();
        }
        return data.getBytes();
    }
    
    public List<BonusPoint> getBonusPoints() {
        return bonusPoints;
    }
    
}