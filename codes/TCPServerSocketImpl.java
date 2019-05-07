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
    public TCPSocket accept() throws Exception {
        TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 100000);

        if (packet.isSynFlag()) {
            System.out.println("request for connection");
        }

        byte[] message = TcpPacket.convertToByte(
                new TcpPacket(
                        0,
                        0,
                        true,
                        true
                )
        );

        this.udtSocket.send(new DatagramPacket(
                        message,
                        message.length,
                        Constants.getAddress(),
                        Constants.CLIENT_SOCKET_PORT
                )
        );

        return new TCPSocketImpl(Constants.ADDRESS, Constants.ACCEPTED_SOCKET_PORT);
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
