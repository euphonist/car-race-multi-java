package racing.net;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import racing.BonusPoint;

import racing.PlayerMP;
import racing.net.packets.*;
import racing.net.packets.Packet.PacketTypes;

public class GameServer extends Thread {

    public static final int PORT = 11331;
    
    private DatagramSocket socket;
    protected List<PlayerMP> connectedPlayers = new ArrayList<>();
    protected List<Point> oilPoints, bombPoints;
    protected List<BonusPoint> bonusPoints;
    protected List<Double> raceTimes;
    private File map;
    private List<ParseUserPacket> userThreads;
    private List<Boolean> confirmedStart;    
    
    protected int bonusPointCounter = 0;
    protected int startX, startY;
    protected double startAngle;
    protected long time;
    protected boolean isRaceStarted;
    protected int numberOfLaps;
    private boolean allConfirmedStart;
    
    private final Object lockUserList = new Object();
            
    public GameServer() {
        numberOfLaps = Integer.parseInt(JOptionPane
                .showInputDialog(null, "Please enter number of laps"));
        oilPoints = new ArrayList<>();
        bombPoints = new ArrayList<>();
        bonusPoints = new ArrayList<>();
        raceTimes = new ArrayList<>();
        userThreads = new ArrayList<>();
        isRaceStarted = false;
        
        loadMap();
        
        time = System.currentTimeMillis();
        try {
            this.socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {  
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                if(isRaceStarted) {
                    manageBonusPoints();
                    if(connectedPlayers.isEmpty()) {
                        isRaceStarted = false;
                        restartGame();
                    }
                    else if(!allConfirmedStart) {
                        int i = 0;
                        allConfirmedStart = true;
                        for(Boolean confirmed : confirmedStart) {
                            if (confirmed.booleanValue() == Boolean.FALSE) {
                                Packet14StartRace startPacket = 
                                        new Packet14StartRace(time);
                                this.sendData(startPacket.getData(),
                                        connectedPlayers.get(i).ipAddress, 
                                        connectedPlayers.get(i).port);
                                allConfirmedStart = false;
                            }
                            i++;
                        }
                    }
                }
                for(int i=0; i<raceTimes.size();i++) {
                    if(raceTimes.get(i) == 0.) break;
                    else if(i==raceTimes.size()-1) {
                        // send message about end of the race with the results
                        Packet20Results resultsPacket = 
                                new Packet20Results(raceTimes, connectedPlayers);
                        this.sendDataToAllClients(resultsPacket.getData());
                        isRaceStarted = false;
                    }
                }
                socket.receive(packet);
                this.parsePacket(packet, packet.getAddress(), packet.getPort());
            } catch (IOException|InterruptedException e) {
              System.out.println(e.getMessage());
            }
        }
    }

    private void parsePacket(DatagramPacket dpacket, InetAddress address, int port) throws InterruptedException {
        byte[] data = dpacket.getData();
        String message = new String(data).trim();
        PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
        Packet packet;
        switch (type) {
        case INVALID:
            break;
        case LOGIN:
            if(isRaceStarted) {
                return;
            }
            packet = new Packet00Login(data);
            PlayerMP player = new PlayerMP(startX+50*connectedPlayers.size(), 
                    startY+50*connectedPlayers.size(), startAngle,
                    ((Packet00Login) packet).getUsername(), address, port);
            if(addConnection(player, (Packet00Login) packet)) {
                System.out.println("[" + address.getHostAddress() + ":" + port + "] "
                    + ((Packet00Login) packet).getUsername() + " has connected...");
                packet = new Packet02Move(player);
                sendDataToAllClients(packet.getData());
                ParseUserPacket pup = new ParseUserPacket(address, port, 
                        this, socket, player.getUsername());
                userThreads.add(pup);
                while(!userThreads.contains(pup))
                    userThreads.add(pup);
                pup.start();
            }
            break;
        case START_RACE:
            isRaceStarted = true;
            time = System.currentTimeMillis();
            confirmedStart = new ArrayList<>(Arrays
                    .asList(new Boolean[connectedPlayers.size()]));
            Collections.fill(confirmedStart, Boolean.FALSE);
            allConfirmedStart = false;
            packet = new Packet14StartRace(time);
            packet.writeData(this);
            break;
        case DISCONNECT:
            packet = new Packet01Disconnect(data);
            System.out.println("[" + address.getHostAddress() + ":" + port + "] "
                    + ((Packet01Disconnect) packet).getUsername() + " has left...");
            int index = getPlayerMPIndex(((Packet01Disconnect)packet).getUsername());
            userThreads.get(index).close();
            removeConnection((Packet01Disconnect) packet);
            break;
        default:
            boolean isUserThread = false;
            for (ParseUserPacket userThread : userThreads) {
                if (userThread.getAddress().equals(address) &&
                        userThread.getPort()==port) {
                    userThread.addPacket(dpacket);
                    isUserThread = true;
                    break;
                }
            }
            if(!isUserThread) {
                Packet22CannotLogin alreadyConnectedPacket = 
                        new Packet22CannotLogin();
                sendData(alreadyConnectedPacket.getData(), dpacket.getAddress(),
                        dpacket.getPort());
            }
            break;
        }
    }
    
    private void restartGame() {
        this.bombPoints.clear();
        this.oilPoints.clear();
        bonusPoints.stream().forEach((bonusPoint) -> {
            bonusPoint.setUnactive();
        });
    }

    private boolean addConnection(PlayerMP player, Packet00Login packet) {
        boolean alreadyConnected = false;
        synchronized(lockUserList) {
            for (PlayerMP p : this.connectedPlayers) {
                if (player.getUsername().equalsIgnoreCase(p.getUsername())) {
                    alreadyConnected = true;
                    break;
                }
            }
            if (!alreadyConnected) {
                Packet21ConfirmLogin confirmLoginPacket =
                        new Packet21ConfirmLogin();
                sendData(confirmLoginPacket.getData(), player.ipAddress, player.port);
                for (PlayerMP p : this.connectedPlayers) {
                    // relay to the current connected player that there is a new
                    // player
                    sendData(packet.getData(), p.ipAddress, p.port);

                    // relay to the new player that the currently connected player
                    // exists
                    Packet00Login tmpPacket = new Packet00Login(p.getUsername(), p.getX(), p.getY());
                    sendData(tmpPacket.getData(), player.ipAddress, player.port);
                }
                connectedPlayers.add(player);
                raceTimes.add(0.);
                return true;
            }
            else {
                Packet21ConfirmLogin confirmLoginPacket =
                        new Packet21ConfirmLogin();
                sendData(confirmLoginPacket.getData(), player.ipAddress, player.port);
                return false;
            }
        }
    }

    private void removeConnection(Packet01Disconnect packet) {
        synchronized(lockUserList) {
            int index = getPlayerMPIndex(packet.getUsername());
            this.connectedPlayers.remove(index);
            this.raceTimes.remove(index);
            packet.writeData(this);
        }
    }

    public PlayerMP getPlayerMP(String username) {
        for (PlayerMP player : this.connectedPlayers) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }

    public int getPlayerMPIndex(String username) {
        int index = 0;
        for (PlayerMP player : this.connectedPlayers) {
            if (player.getUsername().equals(username)) {
                break;
            }
            index++;
        }
        return index;
    }

    public void sendData(byte[] data, InetAddress ipAddress, int port) {
        synchronized(this) {
            DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
            try {
                this.socket.send(packet);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void sendDataToAllClients(byte[] data) {
        for (PlayerMP p : connectedPlayers) {
            sendData(data, p.ipAddress, p.port);
        }
    }
    
    protected void handleOil(Packet03Oil packet) {
                //wyliczenie punktów wylania oleju tak, by w niego nie wjechać
                double oilX, oilY;
                if(packet.getAngle() <= Math.PI/2.0) {
                    oilX = packet.getX()-10;
                    oilY = packet.getY()+10;
                }
                else if(packet.getAngle() <= Math.PI) {
                    oilX = packet.getX()-10;
                    oilY = packet.getY()-10;
                }
                else if(packet.getAngle() > 1.5 * Math.PI) {
                    oilX = packet.getX()+10;
                    oilY = packet.getY()+10;
                }
                else {
                    oilX = packet.getX()+10;
                    oilY = packet.getY()-10;
                }
                oilPoints.add(new Point(doubleToInt(oilX), doubleToInt(oilY)));    
    }   
    
    protected void handleBomb(Packet04Bomb packet) {   
        // wyliczenie punktów pozostawienia bomby tak, by w nią nie wjechać
        double bombX, bombY;
        if(packet.getAngle() <= Math.PI/2.0) {
            bombX = packet.getX()-10;
            bombY = packet.getY()+10;
        }
        else if(packet.getAngle() <= Math.PI) {
            bombX = packet.getX()-10;
            bombY = packet.getY()-10;
        }
        else if(packet.getAngle() > 1.5 * Math.PI) {
            bombX = packet.getX()+10;
            bombY = packet.getY()+10;
        }
        else {
            bombX = packet.getX()+10;
            bombY = packet.getY()-10;
        }
        bombPoints.add(new Point(doubleToInt(bombX), doubleToInt(bombY)));   
    }
    
    protected void removeOilPoint(Packet07RemoveOil packet) {
        synchronized(this) {
            int x = packet.getPoint().x;
            int y = packet.getPoint().y;
            Iterator<Point> iter = oilPoints.iterator();
            while(iter.hasNext()) {
                Point oilPoint = iter.next();
                if(oilPoint.x == x && oilPoint.y == y) {
                    iter.remove();
                    return;
                }
            }
        }
    }
    
    protected void removeBombPoint(Packet10RemoveBomb packet) {
        synchronized (this) {
            int x = packet.getPoint().x;
            int y = packet.getPoint().y;
            Iterator<Point> iter = oilPoints.iterator();
            while(iter.hasNext()) {
                Point bombPoint = iter.next();
                if(bombPoint.x == x && bombPoint.y == y) {
                    iter.remove();
                    return;
                }
            }
        }
    }
    
    protected boolean canUseBonusPoint(Packet12UseBonusPoint packet) {
        Point point = packet.getPoint();
        int index = 0;
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.getX() == point.x && bonusPoint.getY() == point.y) {
                break;
            }
            index++;
        }
        if(index == bonusPoints.size())
            return false;
        synchronized(this) {
            if(bonusPoints.get(index).isActive() == false)
                return false;
            System.out.println(this.bonusPoints.get(index).getBonus());
            this.bonusPoints.get(index).setUnactive();
            return true;
        }
    }
    
    private void manageBonusPoints() throws InterruptedException {
        long timeNow = System.currentTimeMillis() - time;
        //  new bonus every 10 seconds (+- 20 ms)
        if(timeNow % 10000 < 20) {
            bonusPoints.get(bonusPointCounter).setBonus(System.currentTimeMillis());
            Packet11SendBonusPoints sendBonusPointPacket =
                    new Packet11SendBonusPoints(bonusPoints);
            sendBonusPointPacket.writeData(this);
            bonusPointCounter++;
            if(bonusPointCounter == bonusPoints.size())
                bonusPointCounter = 0;
             //sleep for 20 ms in case of not setting more than one bonus
                           // because of offtime +- 20 ms
            Thread.sleep(20);
        }
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.isActive()) {
                if(System.currentTimeMillis() - bonusPoint.getTime() >= 
                        BonusPoint.TIME_OF_ACTIVE_BONUS) {
                    bonusPoint.setUnactive();
                }
            }
        }
    }
    
    private void loadMap() {
        JFileChooser chooser = new JFileChooser();
        File workingDirectory = 
               new File(System.getProperty("user.dir")+"\\resources\\maps");
        chooser.setCurrentDirectory(workingDirectory);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
           "PNG maps", "png");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           map = chooser.getSelectedFile();
        }
        else System.exit(0);
        String settingsPath = "resources/maps/" + map.getName().
                                substring(0, map.getName().length()-4)+".txt";

        // set start coordinates and angle, and then checkpoints and bonusPoints
        try (Stream<String> stream = Files.lines(Paths.get(settingsPath))) {
            Iterator<String> iterator = stream.iterator();
            startX = Integer.parseInt(iterator.next());
            startY = Integer.parseInt(iterator.next());
            startAngle = Double.parseDouble(iterator.next());
            for(int i=0;i<5;i++) {
                iterator.next();
            }
            int numberOfBonusPoints = Integer.parseInt(iterator.next());
            for(int i=0; i<numberOfBonusPoints; i++) {
                String line = iterator.next();
                String delims = "[ ]+";
                String[] tokens = line.split(delims);
                int x = Integer.parseInt(tokens[0]);
                int y = Integer.parseInt(tokens[1]);
                bonusPoints.add(new BonusPoint(x, y));
            }
        }
        catch (IOException e) {
            System.out.println("Watch out, cannot open checkpoints file!");
            System.exit(2);
        }
    }
    
    private int doubleToInt(double doubleValue) {
        int baseInt = (int)doubleValue;
        if(doubleValue - baseInt >= 0.5) {
                return baseInt+1;
        } else {
                return baseInt;
        }
    }
    
    public File getMap() {
        return this.map;
    }
    
    public void setConfirmedStart(String username) {
        int index = getPlayerMPIndex(username);
        confirmedStart.set(index, true);
    } 
}