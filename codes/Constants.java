import java.net.InetAddress;
import java.net.UnknownHostException;

public class Constants {
    public static final int SERVER_PORT_PORT = 12345;
    public static final int ACCEPTED_SOCKET_PORT = 4000;
    public static final int CLIENT_SOCKET_PORT = 3000;
    public static final String ADDRESS = "127.0.0.1";
    public static InetAddress getAddress() {
        try {
            return InetAddress.getByName("127.0.0.1");
        }
        catch (UnknownHostException exception) {
            return null;
        }
    }

    public static final int NUMBER_OF_CONNECTING_ATTEMPTS = 10;
}
