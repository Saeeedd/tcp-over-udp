import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TCPSocketImpl extends TCPSocket {
    private EnhancedDatagramSocket udtSocket;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.udtSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public void connectToAddress(InetAddress address, int port) throws TimeoutException, IOException {

        for (int i = 0; i < Constants.NUMBER_OF_CONNECTING_ATTEMPTS; i++) {
            byte[] message = TcpPacket.convertToByte(
                    new TcpPacket(
                            0,
                            0,
                            true,
                            false                )
            );

            this.udtSocket.send(new DatagramPacket(
                            message,
                            message.length,
                            Constants.getAddress(),
                            Constants.SERVER_PORT_PORT
                    )
            );

            try {
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 1000);
                if (packet.isSynFlag() && packet.isAckFlag()) {
                    byte[] ackMessage = TcpPacket.convertToByte(
                            new TcpPacket(
                                    0,
                                    0,
                                    false,
                                    true                )
                    );

                    for (int j = 0; j < 10; j++) {
                        this.udtSocket.send(new DatagramPacket(
                                        message,
                                        message.length,
                                        Constants.getAddress(),
                                        Constants.SERVER_PORT_PORT
                                )
                        );
                        TimeUnit.MILLISECONDS.sleep(10);
                    }
                    System.out.println("Client connection established");
                    return;
                }
            }
            catch (Exception exception) {
                System.out.println("Connection timeout");
            }
        }

        throw new TimeoutException();
    }

    @Override
    public void send(String pathToFile) throws Exception {
        List<String> chunks = Utils.splitFileByChunks(pathToFile);
        int i = 0;
        System.out.println("start sending");
        while (i < chunks.size()) {
            boolean lastFlag = false;
            if (i == (chunks.size() - 1))
                lastFlag = true;

            System.out.println(chunks.get(i));
            byte[] message = TcpPacket.convertToByte(new TcpPacket(lastFlag, chunks.get(i).getBytes()));
            this.udtSocket.send(new DatagramPacket(
                    message,
                    message.length,
                    InetAddress.getByName("127.0.0.1"),
                    Constants.ACCEPTED_SOCKET_PORT)
            );

            i++;
        }
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        boolean lastReceived = false;
        int i = 0;
        while (!lastReceived) {
            try {
                i++;
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 1000);
                System.out.println(String.valueOf(i) + " : " + new String(packet.getPayload()));
                lastReceived = packet.isLast();

            } catch (Exception exception) {
                System.out.println("Exception occured!");
            }
        }
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
