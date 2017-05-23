package racing;

public class CheckPoint {
    private boolean horizontal;
    private int x1, x2;
    private int y1, y2;
    
    public CheckPoint() {
        horizontal = true;
        x1 = 0;
        x2 = 0;
        y1 = 0;
        y2 = 0;
    }
    
    // it is checkLine in fact, it is horizontal or vertical and has two points
                                // telling where the checkLine has start and end
    public CheckPoint(char direction, int a, int b, int c) {
        if (direction == 'x') {
            this.horizontal = true;
            this.x1 = a;
            this.x2 = b;
            this.y1 = c;
        }
        else if(direction == 'y') {
            this.horizontal = false;
            this.x1 = a;
            this.y1 = b;
            this.y2 = c;
        }
    }
    
    public boolean isHorizontal() {
        return horizontal;
    }
    
    public int getX() {
        return x1;
    }
    
    public int getY() {
        return y1;
    }
    
    // return length between 
    public int getLenght() {
        if(horizontal == true) {
            return x2-x1;
        }
        return y2-y1;
    }
    
    public boolean ifGetsTheCheckpoint(int x, int y) {
        if(this.isHorizontal()) {
            if(y <= this.getY()+10 && y >= this.getY()-10) {
                if(x >= this.getX()-10 && x <= this.getX()+this.getLenght()+10) {
                    return true;
                }
            }
        } 
        else {
            if(x <= this.getX()+10 && x >= this.getX()-10) {
                if(y >= this.getY()-10 && y <= this.getY()+this.getLenght()+10) {
                    return true;
                }
            }
        }
        return false;
    }
}
