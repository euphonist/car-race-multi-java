package racing.net.packets;

import java.util.ArrayList;
import java.util.List;
import racing.PlayerMP;
import racing.net.GameClient;
import racing.net.GameServer;

public class Packet20Results extends Packet {
    
    private final List<Double> raceTimes = new ArrayList<>();
    private final List<String> usernames = new ArrayList<>();
    
    public Packet20Results(byte[] data) {
        super(20);
        String[] dataArray = readData(data).split(",");
        int size = Integer.parseInt(dataArray[0]);
        for (int i=0; i<size; i++) {
            raceTimes.add(Double.parseDouble(dataArray[i*2+1]));
            usernames.add(dataArray[i*2+2]);
        }
    }

    public Packet20Results(List<Double> raceTimes, List<PlayerMP> players) {
        super(20);
        this.raceTimes.addAll(raceTimes);
        for(PlayerMP player : players) {
            usernames.add(player.getUsername());
        }
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
        data = "20" + raceTimes.size();
        for(int i=0; i<raceTimes.size(); i++) {
            data += "," + raceTimes.get(i) + "," + usernames.get(i);
        }
        return data.getBytes();
    }
    
    public Double getRaceTime(int i) {
        return raceTimes.get(i);
    }
    
    public String getUsername(int i) {
        return usernames.get(i);
    }
    
    public int getSize() {
        return raceTimes.size();
    }
}
