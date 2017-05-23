package racing;

import java.net.InetAddress;

public class PlayerMP extends Player {

    public InetAddress ipAddress;
    public int port;

    public PlayerMP(double x, double y, double angle, String username, InetAddress ipAddress, int port) {
        super(x, y, angle, username);
        this.ipAddress = ipAddress;
        this.port = port;
    }
}