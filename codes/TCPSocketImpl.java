import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TCPSocketImpl extends TCPSocket {
    private EnhancedDatagramSocket udtSocket;
    private CongestionController congestionController = null;

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
                                    true
                            )
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
        System.out.println("start sending");
        this.congestionController = new CongestionController();

        while (this.congestionController.getWindowHead() < chunks.size()) {
            while (true) {
                if(this.congestionController.isWindowFull()){
                    System.out.println("window is full");
                    break;
                }

                int currentIndex = this.congestionController.nextChunkIndex();

                if (currentIndex >= chunks.size()) {

                    break;
                }

                System.out.println("Current index: " + String.valueOf(currentIndex));
                boolean lastPacket = (currentIndex == (chunks.size() - 1));
                String packetPayload = chunks.get(currentIndex);
                TcpPacket sendPacket = TcpPacket.generateDataPack(packetPayload.getBytes(), currentIndex, lastPacket);
                System.out.println("Sent packet sequence number : " + sendPacket.getSequenceNumber());
                byte[] outStream = TcpPacket.convertToByte(sendPacket);
                this.udtSocket.send(new DatagramPacket(
                        outStream,
                        outStream.length,
                        Constants.getAddress(),
                        Constants.ACCEPTED_SOCKET_PORT)
                );
            }

            TcpPacket ackResponse;
            try {
                ackResponse = TcpPacket.receivePacket(this.udtSocket,10);
                if (!ackResponse.isAckFlag())
                    continue;
                if (ackResponse.isSynFlag() && ackResponse.isAckFlag())
                    continue;
                System.out.println("ack number : " + String.valueOf(ackResponse.getAcknowledgementNumber()));
                this.congestionController.renderAck(ackResponse.getAcknowledgementNumber());
            }

            catch (Exception e){
                System.out.println("Timeout Occured");
                this.congestionController.timeoutAccured();
            }
        }

        this.congestionController = null;
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        boolean lastReceived = false;
        int lastPacketNumberRecieved = -1;

        ArrayList<byte[]> fileChunks = new ArrayList<>() ;

        while (!lastReceived) {
            try {
                System.out.println("waiting for chunk");
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 10);
                System.out.println("new data comes : " + String.valueOf(packet.getSequenceNumber()));

                if (packet.getSequenceNumber() == (lastPacketNumberRecieved + 1)) {
                    fileChunks.add(packet.getPayload());
                    lastPacketNumberRecieved++;
                    lastReceived = packet.isLast();
                }

                if (lastPacketNumberRecieved > -1) {
                    System.out.println("Sent ack : " + String.valueOf(lastPacketNumberRecieved));
                    TcpPacket ackPack = TcpPacket.generateAck(lastPacketNumberRecieved);

                    byte[] outStream = TcpPacket.convertToByte(ackPack);

                    this.udtSocket.send(
                            new DatagramPacket(
                                    outStream,
                                    outStream.length,
                                    Constants.getAddress(),
                                    Constants.CLIENT_SOCKET_PORT
                            )
                    );
                }

            } catch (Exception exception) {
                System.out.println("Exception occured!");
            }
        }

        for (int __ = 0 ; __ < 10 ; __ ++){
//            TcpPacket ackPack = TcpPacket.generateAck(lastPacketNumberRecieved);
//            byte[] outStream = TcpPacket.convertToByte(ackPack);
//            this.udtSocket.send(new DatagramPacket(
//                    outStream,
//                    outStream.length,
//                    Constants.getAddress(),
//                    Constants.CLIENT_SOCKET_PORT)
//            );
        }

        System.out.print(fileChunks);
    }

    @Override
    public void close() throws Exception {
        this.udtSocket.close();
//        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        return this.congestionController.getSSThreshold();
    }

    @Override
    public long getWindowSize() {
        return this.congestionController.getCWND();
    }
}
