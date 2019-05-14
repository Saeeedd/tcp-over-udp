import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RtMethodGenerator;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
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
        ArrayList<byte[]> chunks = Utils.splitFileByChunks(pathToFile);
        System.out.println("start sending");
        this.congestionController = new CongestionController(this);
        int rwnd = Integer.MAX_VALUE;
        RTTComputer rttComputer = new RTTComputer(chunks.size());
        long timer = System.currentTimeMillis();
        this.congestionController.setMSS(1400/Utils.chunkSize);

        while (this.congestionController.getRecivedDataIndex() < chunks.size()-1) {
            if (this.congestionController.getNextSendIndex() != -1 && this.congestionController.getNextSendIndex() < chunks.size() && rwnd > 0) {

                int currentIndex = this.congestionController.getNextSendIndex();

                boolean lastPacket = false;


                byte[] packetPayload = new byte[0];
                while (currentIndex < chunks.size() && packetPayload.length + chunks.get(currentIndex).length < congestionController.getMSS()){
                    int prevLength = packetPayload.length;
                    byte[] chunk = chunks.get(currentIndex);
                    byte[] temp = new byte[prevLength + chunk.length];
                    System.arraycopy(packetPayload,0, temp, 0, prevLength);
                    System.arraycopy(chunk,0, temp, prevLength, chunk.length);
                    packetPayload = temp;
                    lastPacket = (currentIndex == (chunks.size() - 1));
                    currentIndex += 1;
                }

                int firstIndex = congestionController.getNextSendIndex();
                int lastIndex = currentIndex - 1;

                TcpPacket sendPacket = TcpPacket.generateDataPack(packetPayload, firstIndex, lastIndex, lastPacket);
                System.out.println("Sent packet sequence number : " + sendPacket.getSequenceNumber() + " ackNumber : " + sendPacket.getAcknowledgementNumber());
                rttComputer.sendEvent(lastIndex);
                TcpPacket.sendTcpPacket(this.udtSocket,sendPacket,Constants.ACCEPTED_SOCKET_PORT);
                this.congestionController.sendEvent(lastIndex - firstIndex + 1);
            }

            try {

                TcpPacket ackResponse = TcpPacket.receivePacket(this.udtSocket, 1);
                rwnd -= ackResponse.getRwnd();
                if ((!ackResponse.isAckFlag()) || (ackResponse.isSynFlag()))
                    continue;

                long rttTimeSample = rttComputer.getRTT(ackResponse.getAcknowledgementNumber());
                System.out.println("ack number : " + String.valueOf(ackResponse.getAcknowledgementNumber()));
                this.congestionController.ackNumHandler(ackResponse.getAcknowledgementNumber(),rttTimeSample);
                timer = System.currentTimeMillis();
            } catch (SocketTimeoutException e) {
                if(System.currentTimeMillis() - timer > this.congestionController.getTimeout()){
                    System.out.println("Timeout Occured : " + String.valueOf(System.currentTimeMillis() - timer));
                    this.congestionController.timeoutHandler();
                    timer = System.currentTimeMillis();
                }
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

                System.out.println("new data comes : " + String.valueOf(packet.getSequenceNumber()) + "-" + String.valueOf(packet.getAcknowledgementNumber()));

                chunksHandler.addChunk(packet.getPayload(), packet.getSequenceNumber(), packet.getAcknowledgementNumber());

                if (packet.isLast()){
                    lastDataIndex = packet.getAcknowledgementNumber();
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

