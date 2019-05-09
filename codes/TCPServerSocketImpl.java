import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class TCPServerSocketImpl extends TCPServerSocket {

    private EnhancedDatagramSocket udtSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.udtSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
        while (true) {
            TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 100000);

            if (packet.isSynFlag()) {
                System.out.println("request for connection");
                break;
            }
        }

        for (int i = 0; i < 10; i++) {
            byte[] message = TcpPacket.convertToByte(
                    new TcpPacket(
                            0,
                            -1,
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

            TimeUnit.MILLISECONDS.sleep(10);
        }


        for (int i = 0; i < 10; i++) {
            TcpPacket packet;
            try {
                packet = TcpPacket.receivePacket(this.udtSocket, 100);
            } catch (SocketTimeoutException socketTimeoutException) {
                continue;
            }
            if (packet.isAckFlag()) {
                break;
            }
        }

        System.out.println("Connection established");
        return new TCPSocketImpl(Constants.ADDRESS, Constants.ACCEPTED_SOCKET_PORT);
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
