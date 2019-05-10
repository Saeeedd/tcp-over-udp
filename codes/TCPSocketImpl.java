import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
                            false)
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

                    for (int j = 0; j < 2; j++) {
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
            } catch (Exception exception) {
                System.out.println("Connection timeout");
            }
        }

        throw new TimeoutException();
    }

    @Override
    public void send(String pathToFile) throws Exception {
        List<String> chunks = Utils.splitFileByChunks(pathToFile);
        System.out.println("start sending");
        this.congestionController = new CongestionController(this);
        boolean isFirst = true;
        while (this.congestionController.getWindowHead() < chunks.size()) {
            while (true) {
                if (this.congestionController.isWindowFull()) {
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

            try {
                TcpPacket ackResponse;
                if (isFirst) {
                    System.out.println("first packet");
                    ackResponse = TcpPacket.receivePacket(this.udtSocket, 1000);
                    System.out.println("First ack : " + String.valueOf(ackResponse.getAcknowledgementNumber()));
                    if (!ackResponse.isSynFlag())
                        isFirst = false;
                }
                else
                    ackResponse = TcpPacket.receivePacket(this.udtSocket, 100);

                if (!ackResponse.isAckFlag())
                    continue;
                if (ackResponse.isSynFlag())
                    continue;

                if (ackResponse.getAcknowledgementNumber() == (chunks.size() - 1)) {
                    break;
                }

                System.out.println("ack number : " + String.valueOf(ackResponse.getAcknowledgementNumber()));
                this.congestionController.renderAck(ackResponse.getAcknowledgementNumber());
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout Occured");

                if (isFirst) {
                    TcpPacket sendPacket = TcpPacket.generateDataPack(chunks.get(0).getBytes(), 0, false);
                    System.out.println("Sent packet sequence number : " + sendPacket.getSequenceNumber());
                    byte[] outStream = TcpPacket.convertToByte(sendPacket);
                    this.udtSocket.send(new DatagramPacket(
                            outStream,
                            outStream.length,
                            Constants.getAddress(),
                            Constants.ACCEPTED_SOCKET_PORT)
                    );
                }

                else
                    this.congestionController.timeoutOccured();
            }
        }

        this.congestionController = null;
    }

    @Override
    public void receive(String pathToFile) {
        boolean lastReceived = false;

        FileChunksHandler chunksHandler = new FileChunksHandler();

        while (true) {
            try {
                System.out.println("waiting for chunk");
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 100);
                System.out.println("new data comes : " + String.valueOf(packet.getSequenceNumber()));

                chunksHandler.addChunk(packet.getPayload(),packet.getSequenceNumber());

                if(packet.isLast() && chunksHandler.isChunksComplete()){
                    break;
                }

                int ackNumber = chunksHandler.getLastCompletedIndex();
                if (ackNumber != -1) {
                    System.out.println("Sent ack : " + String.valueOf(ackNumber));
                    TcpPacket ackPack = TcpPacket.generateAck(ackNumber);

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

            }

            catch (SocketTimeoutException exception) {
                System.out.println("Timeout occured!");
            }

            catch (IOException ioException) {
                System.out.println("IO occured!");

            }
        }


        for (int __ = 0; __ < 10; __++) {
            try {
                int ackNumber = chunksHandler.getLastCompletedIndex();
                TcpPacket ackPack = TcpPacket.generateAck(ackNumber);
                System.out.println("last ack : " + String.valueOf(ackNumber));
                byte[] outStream = TcpPacket.convertToByte(ackPack);
                this.udtSocket.send(new DatagramPacket(
                        outStream,
                        outStream.length,
                        Constants.getAddress(),
                        Constants.CLIENT_SOCKET_PORT)
                );
                TimeUnit.MILLISECONDS.sleep(100);

            } catch (IOException exception) {
            } catch (InterruptedException exception) {
            }
        }

        System.out.print(chunksHandler.getFileChunks());

    }

    @Override
    public void close() {
        this.udtSocket.close();
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
