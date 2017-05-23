package racing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.IOException;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import racing.BonusPoint.Bonus;
import racing.net.GameClient;
import racing.net.GameServer;

import racing.net.packets.*;

public class Racing extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1169331112688629681L;
    
    private List<Player> players = new ArrayList<>();
    public Player player;
    private int[] keys;
    private CheckPoint[] checkpoints;
    private List<BonusPoint> bonusPoints;
    private List<Point> oilPoints, bombPoints;
    private Dimension screenSize;
    private Timer timer;
    private BufferedImage background;
    private BufferedImage raceTrack;
    private File map = null;
    
    private int actualCheckpoint;
    private int numberOfBonusPoints;
    private int startX, startY;
    private double startAngle;
    private long time, lapTime;
    private long bestLap = 0;
    private boolean isRaceStarted;
    private boolean isRaceEnded;
    
    public GameClient socketClient;
    public GameServer socketServer;
    

    public Racing() {
        init();
    }

    private void init() {
        
        String hostIP = JOptionPane.showInputDialog(this, "Please enter hosts ip");
        if (hostIP == null)
            System.exit(0);
        else if(hostIP.equals(""))
            hostIP = "localhost";
        socketClient = new GameClient(this, hostIP);
        socketClient.start();

        String username = JOptionPane.showInputDialog(this, "Please enter a username");
        if(username == null)
            System.exit(0);
        while("".equals(username)) {
            username = JOptionPane.showInputDialog(this, "Please enter a username");
            if(username == null)
                System.exit(0);
        }
        player = new PlayerMP(0, 0, 0, username.substring(0, username.length()>20?20:username.length()), null, -1);
        addPlayer(player);
        Packet00Login loginPacket = new Packet00Login(player.getUsername(),
                player.getX(), player.getY());
        loginPacket.writeData(socketClient);
        socketClient.confirmedLogin = false;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        }
        
        screenSize = new Dimension(1024, 768);
        setPreferredSize(screenSize);

        actualCheckpoint = 0;
        numberOfBonusPoints = 0;

        oilPoints = new ArrayList<>();
        bombPoints = new ArrayList<>();
        bonusPoints = new ArrayList<>();
        isRaceStarted = false;
        isRaceEnded = false;
        
        //loadMap();
        while(map == null) {
            Packet13SendMap packet = new Packet13SendMap(new File(""));
            packet.writeData(socketClient);
        }
                
        registerSettings();
        
        Packet05GetOil oilPacket = new Packet05GetOil(new ArrayList<>());
        oilPacket.writeData(socketClient);
        Packet06GetBomb bombPacket = new Packet06GetBomb(new ArrayList<>());
        bombPacket.writeData(socketClient);

        // set coordinates and angle from race settings

        registerKeyListener();
        timer = new Timer(15, this);
        timer.start();
        loadTrack();
    }
    
    private void loadTrack() {
        
        Paint texture = null;
        try {
            raceTrack = ImageIO.read(Player.class
                   .getResource("/maps/"+map));
            texture = new TexturePaint(ImageIO.read(Player.class
                   .getResource("/images/grass.jpg")),new Rectangle(344, 344));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        background = new BufferedImage(screenSize.width, screenSize.height,
                        BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = background.createGraphics();
        g2d.setPaint(texture);
        g2d.fillRect(0, 0, screenSize.width, screenSize.height);

        g2d.drawImage(raceTrack, 0, 0, null);
    }
    
    private void registerKeyListener() {
        keys = new int[256];
        Arrays.fill(keys, 0); // 0 = key is up

        KeyboardFocusManager kfm = KeyboardFocusManager
                                        .getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher((KeyEvent e) -> {
            if(e.getKeyCode()<256)
            switch (e.getID()) {
                case KeyEvent.KEY_PRESSED:
                    keys[e.getKeyCode()] = 1;
                    break;
                case KeyEvent.KEY_RELEASED:
                    keys[e.getKeyCode()] = 0;
                    break;
                default:
                    break;
            }
            return false;
        });

    }
    
    private void registerSettings() {
        checkpoints = new CheckPoint[5];
        
        // setting path is mapName.txt, where map path is mapName.png
        String settingsPath = "resources/maps/" + map.getName().
                                substring(0, map.getName().length()-4)+".txt";
        
        //set start coordinates and angle, and then checkpoints and bonusPoints
        try (Stream<String> stream = Files.lines(Paths.get(settingsPath))) {
            Iterator<String> iterator = stream.iterator();
            startX = Integer.parseInt(iterator.next());
            startY = Integer.parseInt(iterator.next());
            startAngle = Double.parseDouble(iterator.next());
            //player.move(startX, startY, startAngle);
            for(int i=0;i<5;i++) {
                String line = iterator.next();
                String delims = "[ ]+";
                String[] tokens = line.split(delims);
                char xOrY = line.charAt(0);
                int a = Integer.parseInt(tokens[1]);
                int b = Integer.parseInt(tokens[2]);
                int c = Integer.parseInt(tokens[3]);
                checkpoints[i] = new CheckPoint(xOrY, a, b, c);
            }
            numberOfBonusPoints = Integer.parseInt(iterator.next());
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

    //to paint track, car, bonuses and text (lap, time, etc.)
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(background, 0, 0, this);
        g2d.setColor(Color.white);
        g2d.fillRect(3, 5, 70, 35);
        g2d.setFont(new Font("TimesRoman", Font.BOLD, 16));
        g2d.setColor(Color.BLACK);
        if(this.isRaceStarted) {
            g2d.drawString("Lap: "+player.getLap(), 10, 30);
            if(isRaceEnded) {
                int timeMin = (int)(time) / 60000;
                g2d.drawString("Time: "+ timeMin+":"+
                    ((time-timeMin*60000)/1000.0), 8, 60);
            }
            else {
                int timeMin = (int)(System.currentTimeMillis() - time) / 60000;
                g2d.drawString("Time: "+ timeMin+":"+
                    ((System.currentTimeMillis()-time-timeMin*60000)/1000.0), 8, 60);
            }
            g2d.drawString("Best lap: "+bestLap/1000.0, 8, 90);
            paintBonusPoints(g);
        }
        for(Point oilPoint : oilPoints) {
            if(!oilPoint.equals(new Point())) {
                Image oilStain = Toolkit.getDefaultToolkit()
                    .getImage(Racing.class.getResource("/images/oilStain.png"));
                g2d.drawImage(oilStain, oilPoint.x-15, oilPoint.y-15, 30, 30, this);
            }
        }
        for(Point bombPoint : bombPoints) {
            if(!bombPoint.equals(new Point())) {
                Image oilStain = Toolkit.getDefaultToolkit()
                    .getImage(Racing.class.getResource("/images/bomb.png"));
                g2d.drawImage(oilStain, bombPoint.x-15, bombPoint.y-15, 30, 30, this);
            }
        }
        for (Player p : players) {
            p.paintComponent(g);
        }
        for (Player p : players) {
            g2d.setColor(java.awt.Color.GREEN);
            g2d.drawString(p.getUsername(), (float)p.getX()-p.getUsername().length()*4, 
            p.getY()<40?(float)p.getY()+40:(float)p.getY()-40);
        }
        
    }

    private void paintBonusPoints(Graphics g) {
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.isActive()) {
                bonusPoint.paintComponent(g);
            }
        }
    }
    
    private void moveCar() {
        if(player.isFreezed())
            return;
        keyProcessing();
        if(this.isRaceStarted) {
            player.move();
            controlRaceCar();
            checkBonusPoints();
            Packet02Move packet = new Packet02Move(player);
            packet.writeData(socketClient);
        }
    }
    
    private void checkBonusPoints() {
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.ifGetsTheBonuspoint(player.getX(),player.getY())) {
                Packet12UseBonusPoint useBonusPacket = 
                            new Packet12UseBonusPoint(bonusPoint);
                useBonusPacket.writeData(socketClient);
            }
        }
    }
    
    public void useBonus(Point point) {
        Bonus bonus = null;
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.getX() == point.x && bonusPoint.getY() == point.y) {
                bonus = bonusPoint.getBonus();
                break;
            }
        }
        if (bonus == null) {
            return;
        }
        System.out.println(bonus);
        switch (bonus) {
            case NONE:
                break;
            case RESTART:
                restartCar();
                break; 
            case ACCELERATION:
                player.setSpeed(Player.MAX_FORWARD_SPEED);
                break;
            case SLOWDOWN:
                player.stop();
                break; 
            case FREEZE:
                player.freeze();
                break;
            case BOMB:
                player.addBomb();
                break;
            case OIL:
                player.addOil();
                break;
            default:
                break;
        }
    }
    
    private void keyProcessing() {
        if(isRaceStarted) {
            if (keys[KeyEvent.VK_UP] == keys[KeyEvent.VK_DOWN]) {
                //if both UP and DOWN are pressed or released
                player.toStop();
            }
            else if (keys[KeyEvent.VK_UP] == 1) {
                player.accelerate();
            }
            else if (keys[KeyEvent.VK_DOWN] == 1) {
                player.slowdown();
            }
            if (keys[KeyEvent.VK_RIGHT] == 1) {
                player.turnRight();
            }
            else if (keys[KeyEvent.VK_LEFT] == 1) {
                player.turnLeft();
            }
        }
        if (keys[KeyEvent.VK_ESCAPE] == 1) {
            Packet01Disconnect packet = new Packet01Disconnect(player.getUsername());
            packet.writeData(socketClient);
            System.exit(0);
        }
        if (keys[KeyEvent.VK_R] == 1) {
            restartCar();
        }
        if (keys[KeyEvent.VK_SPACE] == 1) {
            useOil();
        }
        if (keys[KeyEvent.VK_B] == 1) {
            useBomb();
        }
        if (keys[KeyEvent.VK_S] == 1) {
            if(!this.isRaceStarted) {
                Packet14StartRace packet = new Packet14StartRace(0L);
                packet.writeData(socketClient);
            }
        }
    }
    
    private void useOil() {
        if(player.ifHasOil()) {
            player.useOil();
            Packet03Oil packet = new Packet03Oil(player.getUsername(), 
                    player.getX(), player.getY(), player.getAngle());
            packet.writeData(socketClient);
        }
    }
    
    public void useBomb() {
        if(player.ifHasBomb()) {
            player.useBomb();
            Packet04Bomb packet = new Packet04Bomb(player.getUsername(),
                    player.getX(), player.getY(), player.getAngle());
            packet.writeData(socketClient);
        }
    }
    
    private Color getColorFromInt(int rgb){
        //set color wihtout alpha 
        Color color = new Color(rgb, false);
        return color;
    }
    
    private void controlRaceCar() { 
        // control speed of car
        if(player.getSpeed() > Player.MAX_FORWARD_SPEED) {
            player.setSpeed(Player.MAX_FORWARD_SPEED);
        }
        else if (player.getSpeed() < Player.MAX_BACKWARD_SPEED) { 
            player.setSpeed(Player.MAX_BACKWARD_SPEED);
        }
        
        // stops car when it is on side of screen.
        int x = player.getX(), y = player.getY();
        if (x>screenSize.width-5) {
            player.setX(screenSize.width-5);	
            player.stop();
        }
        else if (x<5) {
            player.setX(5);
            player.stop();
        }
        if (y>screenSize.height-5) {
            player.setY(screenSize.height-5);
            player.stop();
        }
        else if (y<5) {
            player.setY(5);
            player.stop();
        }

        // check if car is on the road
        Color color = getColorFromInt(raceTrack.getRGB(player.getX(), player.getY()));
        if (!color.equals(Color.BLACK) && !color.equals(Color.WHITE)) {
            // slowdown on not white and not black
            player.notOnRoad();
        }
        
        // check if the car is not on the oil stain
        for (Point oilPoint : oilPoints) {
            if(x > oilPoint.x-10 && x < oilPoint.x+10 &&
                    y > oilPoint.y-10 && y < oilPoint.y+10) {
                player.stop();
                Packet07RemoveOil packet = new Packet07RemoveOil(oilPoint.x,
                        oilPoint.y);
                packet.writeData(socketClient);
                break;
            }
        }
        
        
        // check if the car is not on the bomb
        for (Point bombPoint : bombPoints) {
            if(x > bombPoint.x-10 && x < bombPoint.x+10 &&
                    y > bombPoint.y-10 && y < bombPoint.y+10) {
                //socketServer.getOilPoints().remove(bombPoint);
                // then stop it and place somewhere near with random angle  
                player.stop();
                Random generator = new Random();
                int randomX = generator.nextInt(20);
                int randomY = generator.nextInt(20);
                double randomAngle = generator.nextDouble() * 2*Math.PI;
                player.move(player.getX()+randomX-10, player.getY()+randomY-10, randomAngle);
                Packet10RemoveBomb packet = new Packet10RemoveBomb(bombPoint.x,
                        bombPoint.y);
                packet.writeData(socketClient);
                break;
            }
        }
        
        for (Player p : players) {
            if(player != p && isCollision(player, p)) {
                player.setSpeed(-player.getSpeed());
                player.move();
                player.setSpeed(0);
            }
        }
    }
    
    // check if the car is not on the checkpoint or at the new lap
    private void checkPoints() {
        if(checkpoints[actualCheckpoint].
                ifGetsTheCheckpoint(player.getX(), player.getY())) {
            actualCheckpoint++;
        }
        if (actualCheckpoint == 5) {
            actualCheckpoint = 0;
            player.nextLap();
            long thisLap = System.currentTimeMillis() - lapTime;
            if(bestLap == 0 || thisLap < bestLap) {
                bestLap = thisLap;
            }
            lapTime = System.currentTimeMillis();
        }
    }
    
    // check if it time to show new bonus or hide overdue one
    private void manageBonusPoints() {
        for(BonusPoint bonusPoint : bonusPoints) {
            if(bonusPoint.isActive()) {
                if(System.currentTimeMillis() - bonusPoint.getTime() >= 
                        BonusPoint.TIME_OF_ACTIVE_BONUS) {
                    bonusPoint.setUnactive();
                }
            }
        }
    }
    
    // move car to start coordinates and reset acutal checkpoint
    private void restartCar() {
        player.move(startX, startY, startAngle);
        actualCheckpoint = 0;
    }
    
    public synchronized void removePlayerMP(String username) {
         players.remove(getPlayerMPIndex(username));
    }

    public synchronized void addPlayer(Player player) {
        players.add(player);
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public int getPlayerMPIndex(String username) {
        int index = 0;
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                break;
            }
            index++;
        }
        if(index == players.size())
            System.err.println("Cannot find player "+username);
        return index;
    }

    public void movePlayer(String username, double x, double y, double angle, double speed, boolean hasOil, boolean hasBomb, long freezeTime) {
        int index = getPlayerMPIndex(username);
        PlayerMP player = (PlayerMP)players.get(index);
        player.setX(x);
        player.setY(y);
        player.setAngle(angle);
        player.setSpeed(speed);
        player.setOil(hasOil);
        player.setBomb(hasBomb);
        player.setFreezeTime(freezeTime);
    }
    
    public void setOil(List<Point> oilPoints) {
        synchronized(this) {
            this.oilPoints.clear();
            this.oilPoints.addAll(oilPoints);
        }
    }
    
    public void setBomb(List<Point> bombPoints) {
        synchronized(this) {
            this.bombPoints.clear();
            this.bombPoints.addAll(bombPoints);
        }
    }
    
    public void setBonusPoints(List<BonusPoint> bonusPoints) {
        synchronized(this) {
            this.bonusPoints.clear();
            this.bonusPoints.addAll(bonusPoints);
        }
    }

    public void setMap(File map) {
        this.map = map;
    }
    
    public void startRace(long time) {
        this.isRaceStarted = true;
        this.time = time;
        lapTime = time;
    }
    
    public void endOfRace(long time) {
        if(!isRaceEnded) {
            isRaceEnded = true;
            this.time = time - this.time;
        }
    }
    
    public boolean isCollision(Player player1, Player player2) {
        
        Rectangle2D rectPlayer1 = new Rectangle2D.Double(player1.getX()-13, player1.getY()-27, 24, 54);
        Rectangle2D rectPlayer2 = new Rectangle2D.Double(player2.getX()-13, player2.getY()-27, 24, 54);
        
        AffineTransform transform = new AffineTransform();
        transform.rotate(player1.getAngle(), player1.getX(), player1.getY());
        Shape shape1 = transform.createTransformedShape(rectPlayer1);
        
        transform.rotate(player2.getAngle(), player2.getX(), player2.getY());
        Shape shape2 = transform.createTransformedShape(rectPlayer2);
        
        return testIntersection(shape1, shape2);
    }
    
    private boolean testIntersection(Shape shapeA, Shape shapeB) {
        // if shapeA intersect shapeB, then return true
        Area areaA = new Area(shapeA);
        areaA.intersect(new Area(shapeB));
        return !areaA.isEmpty();
    }
    
    public void showResults(Packet20Results packet) throws InterruptedException {
        JFrame frame = new JFrame("Results of the race");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(new Dimension(33*13, (packet.getSize()+1)*20+50));
        //frame.setResizable(false); 
        JPanel panel = new JPanel() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setFont(new Font("Consolas", Font.PLAIN, 20));
                    g.drawString(String.format("%20s", "username") +"     "+
                            String.format("%8s", "raceTime"), 20, 20);
                    for(int i=0; i<packet.getSize(); i++) {
                        g.drawString(String.format("%20s", packet.getUsername(i))
                                +"     "+String.format("%8s", packet.getRaceTime(i)),
                                20, 20*(i+2));
                    }
                }
        };
        frame.add(panel);
        frame.validate();
        frame.repaint();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Thread.sleep(100000);
        frame.dispose();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
        checkPoints();
        manageBonusPoints();
        moveCar();
    }

    public static void main(String[] args) {
        int choose = JOptionPane.showConfirmDialog(null, "Do you want to run the server");
        switch (choose) {
            case 0:
                GameServer gameServer = new GameServer();
                gameServer.start();
                break;
            case 1:
                SwingUtilities.invokeLater(() -> {
                    createAndShowGUI();
                }); 
                break;
            case 2:
                System.exit(0);
        }
    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Racing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setPreferredSize(new Dimension(1035, 805));
        //frame.setResizable(false);    
        Racing race = new Racing();
        frame.add(race);
        frame.pack();
        frame.setLocationRelativeTo(null); 
        // set icon for the application
        Image icon = Toolkit.getDefaultToolkit().getImage(Racing.class.
                getResource("/images/racing.png"));
        frame.setIconImage(icon);
        frame.setVisible(true);
        
        // new window handler for disconnecting during closing the window
        new WindowHandler(race, frame);
    }
}