import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TCPServerSocketImpl extends TCPServerSocket {

    private EnhancedDatagramSocket udtSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.udtSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public TCPSocket accept() throws RuntimeException, IOException {
        byte[] buffer = new byte[2048];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, 256);
        this.udtSocket.receive(datagramPacket);
        TcpPacket packet = TcpPacket.makePacket(datagramPacket.getData());

        System.out.println(new String(packet.getPayload()));
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
