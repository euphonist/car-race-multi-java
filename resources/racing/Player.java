package racing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.JComponent;

public class Player extends JComponent {
		
    private static final long serialVersionUID = -8147987130820080891L;
    public static final double MAX_FORWARD_SPEED = 8;
    public static final double MAX_BACKWARD_SPEED = -3;
    private static final long FREEZE_TIME = 1000;
    
    private double x, y;
    private double angle;
    private final String username;
    private double speed;
    private boolean hasOil;
    private boolean hasBomb;
    private long freezeTime;
    private int lap;

    public Player(double x, double y, double angle, String username) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.username = username;
        this.hasOil = false;
        this.hasBomb = false;
        this.speed = 0.0;
        this.freezeTime = System.currentTimeMillis();
        this.lap = 1;
    }
    
    // normalize angle to <0; 2pi>
    public void normalize() {
        while (angle > 2*Math.PI) {
            angle -= 2*Math.PI;
        }
        while (angle < 0) {
            angle += 2*Math.PI;
        }
    }
	
    public void move(int x, int y) {
            this.x = x;
            this.y = y;
    }

    public void move(double x, double y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
    }
	
    // move car according to actual speed and angle
    public void move() {
            double newX = x + Math.sin(angle) * speed;
            double newY = y - Math.cos(angle) * speed;
            normalize();
            move(newX, newY, angle);
    }
	
	
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        /*
         *  Translate graphics to x, y
         *  and rotate it through an angle
         */
        g2d.translate(x, y);
        g2d.rotate(angle);

        // then, print avatar for the car
        Image carAvatar = Toolkit.getDefaultToolkit().getImage(Player.class.
                getResource("/images/buick_topDown.png"));
            g2d.drawImage(carAvatar, -25, -35, 50, 70, this);
            g2d.dispose();
            g2d.finalize();

    }
	
    private int doubleToInt(double doubleValue) {
        int baseInt = (int)doubleValue;
        if(doubleValue - baseInt >= 0.5) {
                return baseInt+1;
        } else {
                return baseInt;
        }
    }

    public void freeze() {
        freezeTime = System.currentTimeMillis();
    }
    
    // if specified time has passed after freeze() was called, return false
    public boolean isFreezed() {
        return System.currentTimeMillis() - freezeTime <= FREEZE_TIME;
    }
    
    // returns the absolute of speed or, if speed is 0, it returns 0.1
    public double getPositiveSpeed() {
        if (speed == 0)
            return 0.1;
        return Math.abs(speed);
    }
    
    // increase speed if it is not maximum
    // set it null if you are breaking from driving backward
    public void accelerate() {
        if (speed < 0) {
            speed = 0;
        }
        else if(speed < MAX_FORWARD_SPEED)
            speed += 0.1 / getPositiveSpeed();
        if (speed > MAX_FORWARD_SPEED + 0.5) {
            stop();
        }
    }

    // decrease speed if it is not maximum backward
    // more if you are breaking, less if you are driving backward
    public void slowdown() {
        if (speed > 0)
            speed -= 0.5 / getPositiveSpeed();
        else if (speed > MAX_BACKWARD_SPEED) {
            speed -= 0.05 / getPositiveSpeed();
        }
        if (speed < MAX_BACKWARD_SPEED - 0.5) {
            stop();
        }
    }

    // set speed higher if moving backward or lower if driving forward
    public void toStop() {
        if(speed > 0) {
            speed -= 0.2 / getPositiveSpeed();
        }
        else if (speed < 0) {
            speed += 0.1 / getPositiveSpeed();
        }
        if(getPositiveSpeed()<0.2) {
            stop();
        }
    }
    
    // slow the car immediately
    public void notOnRoad() {
        if(speed > 0) {
            speed -= 0.5;
        }
        else if(speed < 0) {
            speed += 0.2;
        }
    }
    
    // set speed to zero
    public void stop() {
        speed = 0;
    }

    public void turnRight() {
        if (speed != 0) {
            angle += Math.PI / 80 * Math.signum(speed);
        }
    }

    public void turnLeft() {
        if(speed != 0) {
            angle -= Math.PI / 80 * Math.signum(speed);
        }
    }

    public int getX() {
        return doubleToInt(x);
    }

    public int getY() {
        return doubleToInt(y);
    }

    public double getAngle() {
        return angle;
    }

    public double getSpeed() {
        return speed;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setX(double newposX) {
        x = newposX;
    }

    public void  setY(double newposY) {
        y = newposY;
    }

    public void setSpeed(double speed) {
        // do użytku tylko przez Racing, sprawdzenie klasy wywołującej
        if(Racing.class.getName().equals(new Exception().getStackTrace()[1].getClassName()))
            this.speed = speed;
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public boolean ifHasOil() {
        return hasOil;
    }
    
    public void addOil() {
        this.hasOil = true;
    }
    
    public void useOil() {
        if(ifHasOil()){
            this.hasOil = false;
        }
    }
    
    public boolean ifHasBomb() {
        return hasBomb;
    }
    
    public void addBomb() {
        this.hasBomb = true;
    }
    
    public void useBomb() {
        if(ifHasBomb()) {
            this.hasBomb = false;
        }
    }
    
    public void setFreezeTime(long freezeTime) {
        this.freezeTime = freezeTime;
    }
    
    public long getFreezeTime() {
        return this.freezeTime;
    }
    
    public void setBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
    }
    
    public void setOil(boolean hasOil) {
        this.hasOil = hasOil;
    }
    
    public void nextLap() {
        this.lap++;
    }
    
    public int getLap() {
        return this.lap;
    }
    
    public void setLap(int lap) {
        this.lap = lap;
    }
}

