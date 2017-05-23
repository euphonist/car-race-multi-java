package racing.net;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import racing.BonusPoint;
import racing.Player;
import racing.PlayerMP;

import racing.Racing;
import racing.net.packets.*;
import racing.net.packets.Packet.PacketTypes;

public class GameClient extends Thread {

    private InetAddress ipAddress;
    private DatagramSocket socket;
    private Racing game;
    public boolean confirmedLogin = false;
    private boolean loginFailed = false;

    public GameClient(Racing game, String ipAddress) {
        this.game = game;
        try {
            this.socket = new DatagramSocket();
            this.ipAddress = InetAddress.getByName(ipAddress);
        } catch (SocketException|UnknownHostException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (!loginFailed) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
                this.parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
            } catch (IOException|InterruptedException e) {
                System.out.println(e.getMessage());
            }
            if(!confirmedLogin) {
                Packet00Login loginPacket = new Packet00Login(game.player.getUsername(),
                        game.player.getX(), game.player.getY());
                sendData(loginPacket.getData());
            }
        }
        System.err.println("Cannot log in. Probably there is already a player with that username or race has started.");
        System.exit(0);
    }

    private void parsePacket(byte[] data, InetAddress address, int port) throws InterruptedException {
        String message = new String(data).trim();
        PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
        Packet packet;
        switch (type) {
        default:
        case INVALID:
            break;
        case LOGIN:
            packet = new Packet00Login(data);
            handleLogin((Packet00Login) packet, address, port);
            break;
        case DISCONNECT:
            packet = new Packet01Disconnect(data);
            System.out.println("[" + address.getHostAddress() + ":" + port + "] "
                    + ((Packet01Disconnect) packet).getUsername() + " has left the world...");
            game.removePlayerMP(((Packet01Disconnect) packet).getUsername());
            break;
        case MOVE:
            packet = new Packet02Move(data);
            handleMove((Packet02Move) packet);
            break;
        case GET_OIL:
            packet = new Packet05GetOil(data);
            game.setOil(getOil((Packet05GetOil) packet));
            break;
        case GET_BOMB:
            packet = new Packet06GetBomb(data);
            game.setBomb(getBomb((Packet06GetBomb) packet));
            break;
        case SEND_BONUSPOINTS:
            packet = new Packet11SendBonusPoints(data);
            game.setBonusPoints(getBonusPoints((Packet11SendBonusPoints) packet));
            break;
        case USE_BONUSPOINT:
            packet = new Packet12UseBonusPoint(data);
            useBonus((Packet12UseBonusPoint) packet);
            break;
        case SEND_MAP:
            packet = new Packet13SendMap(data);
            setMap((Packet13SendMap) packet);
            break;
        case START_RACE:
            packet = new Packet14StartRace(data);
            startRace((Packet14StartRace) packet);
            packet = new Packet24ConfirmStart(game.player.getUsername());
            sendData(packet.getData());
            break;
        case END_OF_RACE:
            packet = new Packet15EndOfRace(data);
            game.endOfRace(((Packet15EndOfRace)packet).getTime());
            packet = new Packet25ConfirmEnd(game.player.getUsername());
            sendData(packet.getData());
            break;
        case RESULTS:
            packet = new Packet20Results(data);
            game.showResults((Packet20Results) packet);
            break;
        case CONFIRM_LOGIN:
            confirmedLogin = true;
            break;
        case CANNOT_LOGIN:
            loginFailed = true;
            break;
        }
    }

    public void sendData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, GameServer.PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleLogin(Packet00Login packet, InetAddress address, int port) {
        PlayerMP player = new PlayerMP(packet.getX(), packet.getY(), packet.getAngle(), packet.getUsername(), address, port);
        boolean alreadyLogged = false;
        for(Player tempPlayer : game.getPlayers()) {
            if(tempPlayer.getUsername().equals(player.getUsername()))
                alreadyLogged = true;
        }
        if(!alreadyLogged) {
            System.out.println("[" + address.getHostAddress() + ":" + port + "] " + packet.getUsername()
                + " has joined the game...");
            game.addPlayer(player);
        }
    }

    private void handleMove(Packet02Move packet) {
        this.game.movePlayer(packet.getUsername(), packet.getX(), packet.getY(),
                packet.getAngle(), packet.getSpeed(), packet.hasOil(), 
                packet.hasBomb(), packet.getFreezeTime());
    }
    
    private List<Point> getOil(Packet05GetOil packet) {
        return packet.getOil();
    }
    
    private List<Point> getBomb(Packet06GetBomb packet) {
        return packet.getBomb();
    }
    
    private List<BonusPoint> getBonusPoints(Packet11SendBonusPoints packet) {
        return packet.getBonusPoints();
    }

    private void useBonus(Packet12UseBonusPoint packet) {
        this.game.useBonus(packet.getPoint());
    }
    
    private void setMap(Packet13SendMap packet) {
        this.game.setMap(packet.getFile());
    }
    
    private void startRace(Packet14StartRace packet) {
        this.game.startRace(packet.getTime());
    }
}