import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RtMethodGenerator;

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
        List<byte[]> chunks = Utils.splitFileByChunks(pathToFile);
        System.out.println("start sending");
        this.congestionController = new CongestionController(this);
        int rwnd = Integer.MAX_VALUE;
        RTTComputer rttComputer = new RTTComputer(chunks.size());

        while (this.congestionController.getRecivedDataIndex() < chunks.size()-1) {
            while (this.congestionController.getNextSendIndex() != -1 && this.congestionController.getNextSendIndex() < chunks.size() && rwnd > 0) {

                int currentIndex = this.congestionController.getNextSendIndex();

                boolean lastPacket = (currentIndex == (chunks.size() - 1));
                byte[] packetPayload = chunks.get(currentIndex);
                TcpPacket sendPacket = TcpPacket.generateDataPack(packetPayload, currentIndex, lastPacket);
                System.out.println("Sent packet sequence number : " + sendPacket.getSequenceNumber());
                rttComputer.sendEvent(currentIndex);
                TcpPacket.sendTcpPacket(this.udtSocket,sendPacket,Constants.ACCEPTED_SOCKET_PORT);
                this.congestionController.sendEvent();
            }

            try {

                TcpPacket ackResponse = TcpPacket.receivePacket(this.udtSocket, this.congestionController.getTimeout());
                rwnd -= ackResponse.getRwnd();
                if ((!ackResponse.isAckFlag()) || (ackResponse.isSynFlag()))
                    continue;

                long rttTimeSample = rttComputer.getRTT(ackResponse.getAcknowledgementNumber());
                System.out.println("ack number : " + String.valueOf(ackResponse.getAcknowledgementNumber()));
                this.congestionController.ackNumHandler(ackResponse.getAcknowledgementNumber(),rttTimeSample);
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout Occured : " + String.valueOf(this.congestionController.getTimeout()));
                this.congestionController.timeoutHandler();
            }
        }
        this.congestionController = null;
    }

    @Override
    public void receive(String pathToFile) {
        int lastDataIndex = -2;

        FileChunksHandler chunksHandler = new FileChunksHandler();

        while (chunksHandler.getLastCompletedIndex() != lastDataIndex) {
            try {
                System.out.println("waiting for chunk");
                TcpPacket packet = TcpPacket.receivePacket(this.udtSocket, 100);

                if (packet.isAckFlag() || packet.isSynFlag()){
                    System.out.println("junk");
                    continue;
                }

                System.out.println("new data comes : " + String.valueOf(packet.getSequenceNumber()));

                chunksHandler.addChunk(packet.getPayload(), packet.getSequenceNumber());

                if (packet.isLast()){
                    lastDataIndex = packet.getSequenceNumber();
                }


                int ackNumber = chunksHandler.getLastCompletedIndex();
                if (ackNumber != -1) {
                    System.out.println("Sent ack : " + String.valueOf(ackNumber));
                    TcpPacket ackPack = TcpPacket.generateAck(ackNumber, 1);
                    TcpPacket.sendTcpPacket(this.udtSocket, ackPack, Constants.CLIENT_SOCKET_PORT);
                }

            }

            catch (SocketTimeoutException exception) {
                System.out.println("Timeout occured!");
            }

            catch (Exception ex) {
                System.out.println("IO occured!");

            }
        }


        for (int __ = 0; __ < 10; __++) {
            try {
                int ackNumber = chunksHandler.getLastCompletedIndex();
                TcpPacket ackPack = TcpPacket.generateAck(ackNumber, 1);
                System.out.println("last ack : " + String.valueOf(ackNumber));
                TcpPacket.sendTcpPacket(this.udtSocket, ackPack, Constants.CLIENT_SOCKET_PORT);
                TimeUnit.MILLISECONDS.sleep(100);

            } catch (Exception ex) {
            }
        }

//        System.out.print(chunksHandler.getFileChunks());

        Utils.writeChunksToFile(chunksHandler.getFileChunks());


    }

    @Override
    public void close() {
        this.udtSocket.close();
    }

    @Override
    public long getSSThreshold() {
        return this.congestionController.getSsthresh();
    }

    @Override
    public long getWindowSize() {
        return this.congestionController.getCwnd();
    }
}

