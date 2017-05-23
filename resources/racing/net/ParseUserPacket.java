package racing.net;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import racing.PlayerMP;
import racing.net.packets.*;

public class ParseUserPacket extends Thread {
    
    private static final int MOVES_PER_SECOND = 100;
    private final InetAddress address;
    private final int port;
    private final GameServer gameServer;
    private final DatagramSocket socket;
    private boolean confirmedEnd = true;
    private final List<DatagramPacket> packets;
    private boolean close = false;
    private long time;
    ThreadLocal<Integer> moveCounter;
    private final String username;
    
    private final Object lockPacket = new Object();
    
    public ParseUserPacket(InetAddress address, int port, GameServer gameServer, DatagramSocket socket, String username) {
        this.address = address;
        this.port = port;
        this.gameServer = gameServer;
        this.socket = socket;
        this.packets = new ArrayList<>();    
        this.username = username;
        time = System.currentTimeMillis();
        moveCounter = new ThreadLocal<Integer>(){
            @Override protected Integer initialValue(){
                return 0;
                }
        };
    }
    
    @Override
    public void run() {
        while (!close) {
            managePackets();
            if(confirmedEnd == false) {
                Packet15EndOfRace packet = new Packet15EndOfRace();
                sendData(packet.getData());
            }
            manageMoveCounter();
        }
    }
    
    private void managePackets() {
        synchronized(lockPacket) {
            packets.forEach((packet)->parsePacket(packet));
            packets.clear();
        }
    }
    
    public void addPacket(DatagramPacket packet) {
        synchronized(lockPacket) {
            packets.add(packet);
        }
    }
    
    private void parsePacket(DatagramPacket dpacket) {
        byte[] data = dpacket.getData();
        String message = new String(data).trim();
        Packet.PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
        Packet packet;
        switch (type) {
        default:
            break;
        case INVALID:
            break;
        case MOVE:
            packet = new Packet02Move(data);
            this.handleMove(((Packet02Move) packet));
            break;
        case OIL:
            packet = new Packet03Oil(data);
            gameServer.handleOil((Packet03Oil) packet);
            packet = new Packet05GetOil(gameServer.oilPoints);
            packet.writeData(gameServer);
            break;
        case BOMB:
            packet = new Packet04Bomb(data);
            gameServer.handleBomb((Packet04Bomb) packet);
            packet = new Packet06GetBomb(gameServer.bombPoints);
            packet.writeData(gameServer);
            break;
        case GET_OIL:
            packet = new Packet05GetOil(gameServer.oilPoints);
            sendData(packet.getData());
            break;
        case GET_BOMB:
            packet = new Packet06GetBomb(gameServer.bombPoints);
            sendData(packet.getData());
            break;
        case REMOVE_OIL:
            packet = new Packet07RemoveOil(data);
            gameServer.removeOilPoint((Packet07RemoveOil) packet);
            packet = new Packet05GetOil(gameServer.oilPoints);
            packet.writeData(gameServer);
            break;
        case REMOVE_BOMB:
            packet = new Packet10RemoveBomb(data);
            gameServer.removeBombPoint((Packet10RemoveBomb) packet);
            packet = new Packet06GetBomb(gameServer.bombPoints);
            packet.writeData(gameServer);
            break;
        case SEND_BONUSPOINTS:
            packet = new Packet11SendBonusPoints(gameServer.bonusPoints);
            packet.writeData(gameServer);
            break;
        case USE_BONUSPOINT:
            packet = new Packet12UseBonusPoint(data);
            if(gameServer.canUseBonusPoint((Packet12UseBonusPoint) packet)) {
                sendData(packet.getData());
                Packet11SendBonusPoints sendBonusPointsPacket = 
                    new Packet11SendBonusPoints(gameServer.bonusPoints);
                sendBonusPointsPacket.writeData(gameServer);
            }
            break;
        case SEND_MAP:
            packet = new Packet13SendMap(gameServer.getMap());
            sendData(packet.getData());
            break;        
        case CONFIRM_START:
            packet = new Packet24ConfirmStart(data);
            gameServer.setConfirmedStart(((Packet24ConfirmStart)packet).getUsername());
            break;
        case CONFIRM_END:
            confirmedEnd = true;
            break;
        }
    }

    private void handleMove(Packet02Move packet) {
        if (gameServer.getPlayerMP(packet.getUsername()) != null) {
            int index = gameServer.getPlayerMPIndex(packet.getUsername());
            PlayerMP player = gameServer.connectedPlayers.get(index);        
            if(player.getLap() == gameServer.numberOfLaps+1) {
                Packet15EndOfRace eorPacket = new Packet15EndOfRace();
                sendData(eorPacket.getData());
                gameServer.isRaceStarted = false;
                confirmedEnd = false;
            }
            for(PlayerMP otherPlayer : gameServer.connectedPlayers) {
                if(!otherPlayer.equals(player) && isCollision(packet, otherPlayer)) {
                    packet = new Packet02Move(player);
                    gameServer.sendDataToAllClients(packet.getData());
                }
                else if(!otherPlayer.equals(player)){
                    gameServer.sendData(packet.getData(), otherPlayer.ipAddress, otherPlayer.port);
                }
                if(username.equals(player.getUsername())) {
                    if(moveCounter.get() > MOVES_PER_SECOND * gameServer.connectedPlayers.size()) {
                        packet = new Packet02Move(player);
                        gameServer.sendData(packet.getData(), address, port);
                        return;
                    }
                    else {
                        moveCounter.set(moveCounter.get()+1);
                    }
                }
            }
            player.setX(packet.getX());
            player.setY(packet.getY());
            player.setAngle(packet.getAngle());
            player.setSpeed(packet.getSpeed());
            if(player.getLap() == gameServer.numberOfLaps+1) {
                gameServer.raceTimes.set(index, (System.currentTimeMillis() - gameServer.time)/1000.);
                player.setLap(0);
            }
            else if(player.getLap() != 0)
                player.setLap(packet.getLap());
        }
    }
    
    
    private boolean isCollision(Packet02Move packet, PlayerMP player) {
        
        Rectangle2D rectPacket = new Rectangle2D.Double(packet.getX()-12, packet.getY()-27, 24, 54);
        Rectangle2D rectPlayer = new Rectangle2D.Double(player.getX()-12, player.getY()-27, 24, 54);
        
        AffineTransform transform = new AffineTransform();
        transform.rotate(packet.getAngle(), packet.getX(), packet.getY());
        Shape packetShape = transform.createTransformedShape(rectPacket);
        
        transform.rotate(player.getAngle(), player.getX(), player.getY());
        Shape playerShape = transform.createTransformedShape(rectPlayer);
        
        return testIntersection(packetShape, playerShape);
    }
    
    private boolean testIntersection(Shape shapeA, Shape shapeB) {
        // if shapeA intersect shapeB, then return true
        Area areaA = new Area(shapeA);
        areaA.intersect(new Area(shapeB));
        return !areaA.isEmpty();
    }
    
    public InetAddress getAddress() {
        return this.address;
    }
    
    public int getPort() {
        return this.port;
    }
    
    private void sendData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }   
    
    public void close() {
        close = true;
    }

    private void manageMoveCounter() {
        if(System.currentTimeMillis() - time > 1000){
            moveCounter.set(0);
            time = System.currentTimeMillis();
        }
    }
}
