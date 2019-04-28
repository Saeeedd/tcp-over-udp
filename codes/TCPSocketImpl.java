import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private EnhancedDatagramSocket udtSocket;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
//        System.out.println(port);
        this.udtSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        while (true) {
            byte[] message = TcpPacket.convertToByte(new TcpPacket(3000, 12345, pathToFile.getBytes()));
            this.udtSocket.send(new DatagramPacket(
                    message,
                    message.length,
                    InetAddress.getByName("127.0.0.1"),
                    12345)
            );
        }
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
