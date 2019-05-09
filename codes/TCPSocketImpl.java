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
        System.out.println("start sending");
        this.congestionController = new CongestionController();

        int currentIndex = this.congestionController.nextChunkIndex();
        while (this.congestionController.getWindowHead() >= chunks.size()) {
            while(currentIndex < chunks.size() && !this.congestionController.isWindowFull()){
                boolean lastPacket = (currentIndex == chunks.size()-1);
                String packetPayload = chunks.get(currentIndex);
                TcpPacket sendPacket = TcpPacket.generateDataPack(packetPayload.getBytes(),currentIndex,lastPacket);
                byte[] outStream = TcpPacket.convertToByte(sendPacket);
                this.udtSocket.send(new DatagramPacket(outStream,outStream.length));
                currentIndex = this.congestionController.nextChunkIndex();
            }
            TcpPacket ackResponse;
            try{
                ackResponse = TcpPacket.receivePacket(this.udtSocket,1000);
                this.congestionController.renderAck(ackResponse.getAcknowledgementNumber());
            }
            catch (Exception e){
                this.congestionController.timeoutAccured();
                currentIndex = this.congestionController.nextChunkIndex();
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
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 1000);
                System.out.println("new data comes : " + new String(packet.getPayload()));
                TcpPacket ackPack = TcpPacket.generateAck(lastPacketNumberRecieved+1);
                byte[] outStream = TcpPacket.convertToByte(ackPack);
                this.udtSocket.send(new DatagramPacket(outStream,outStream.length));
                if (packet.getAcknowledgementNumber() == lastPacketNumberRecieved+1){
                    fileChunks.add(packet.getPayload());
                    lastPacketNumberRecieved++;
                    lastReceived = packet.isLast();
                }
            } catch (Exception exception) {
                System.out.println("Exception occured!");
            }
        }
        for (int __ = 0 ; __ < 10 ; __ ++){
            TcpPacket ackPack = TcpPacket.generateAck(lastPacketNumberRecieved);
            byte[] outStream = TcpPacket.convertToByte(ackPack);
            this.udtSocket.send(new DatagramPacket(outStream,outStream.length));
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
