package racing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Random;
import javax.swing.JComponent;

public class BonusPoint extends JComponent {
    public enum Bonus {
        NONE,
        RESTART,
        ACCELERATION,
        SLOWDOWN,
        FREEZE,
        BOMB,
        OIL
    }
    //time in ms
    public static long TIME_OF_ACTIVE_BONUS = 35000;
    private int x, y;
    private boolean isActive;
    private long time;
    private Bonus bonus;
    
    public BonusPoint() {
        this.x = 0;
        this.y = 0;
        this.isActive = false;
        this.bonus = Bonus.NONE;
        this.time = 0;
    }
    
    public BonusPoint(int x, int y) {
        this.x = x;
        this.y = y;
        this.isActive = false;
        this.bonus = Bonus.NONE;
        this.time = 0;
    }
    
    public BonusPoint(double x, double y) {
        this.x = Math.toIntExact(Math.round(x));
        this.y = Math.toIntExact(Math.round(y));
        this.isActive = false;
        this.bonus = Bonus.NONE;
        this.time = 0;
    }
    
     public BonusPoint(int x, int y, boolean isActive, long time, Bonus bonus) {
        this.x = Math.toIntExact(Math.round(x));
        this.y = Math.toIntExact(Math.round(y));
        this.isActive = isActive;
        this.bonus = bonus;
        this.time = time;
    }
    
    public void setActive() {
        this.isActive = true;
    }
    
    public void setUnactive() {
        this.isActive = false;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public Bonus getBonus() {
        return this.bonus;
    }
    
    public void setBonus(Bonus bonus, long time) {
        this.bonus = bonus;
        this.time = time;
        this.setActive();
    }
    
    public void setBonus(long time) {
        Random generator = new Random();
        int randInt = generator.nextInt(Bonus.values().length);
        this.bonus = Bonus.values()[randInt];
        this.time = time;
        this.setActive();
    }
    
    public boolean ifGetsTheBonuspoint(int x, int y) {
        if(this.isActive()) {
            if(x < this.x+15 && x > this.x-15 && y < this.y+15 && y > this.y-15)
                return true;
        }
        return false;
    }
    
    public void setTime(long time) {
        this.time = time;
    }
    
    public long getTime() {
        return time;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // draw the sign for the bonus
        Graphics2D g2d = (Graphics2D)g;
        Image bonusSign = Toolkit.getDefaultToolkit().getImage(BonusPoint.class.
                getResource("/images/BonusSign.png"));
            g2d.drawImage(bonusSign, x-15, y-15, 30, 30, this);
    }
}