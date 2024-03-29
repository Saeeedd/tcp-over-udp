import java.io.*;
import java.net.DatagramPacket;
import java.net.SocketException;

public class TcpPacket implements Serializable {

    private TcpPacket(int sequenceNumber,
                     int acknowledgementNumber,
                     int rwnd,
                     byte[] payload,
                     boolean synFlag,
                     boolean ackFlag,
                     boolean last) {
        this.rwnd = rwnd;
        this.sequenceNumber = sequenceNumber;
        this.acknowledgementNumber = acknowledgementNumber;
        this.synFlag = synFlag;
        this.ackFlag = ackFlag;
        this.last = last;
        this.payload = payload;
    }

    public static TcpPacket generateAck(int acknowledgementNumber, int rwnd){
        byte[] payload = {};
        return new TcpPacket(0,acknowledgementNumber, rwnd,payload,false,true,false);
    }

    public static TcpPacket generateDataPack(byte[] payload, int sequenceNumber, int ackNumber, boolean last){
        return new TcpPacket(
                sequenceNumber,
                ackNumber,
                0,
                payload,
                false,
                false,
                last
        );
    }

    public static TcpPacket generateHandshakePacket(boolean ackFlag,boolean synFlag){
        byte[] payload = {};
        return new TcpPacket(0,0, 0, payload, synFlag, ackFlag,false);
    }


    public TcpPacket(int sequenceNumber,
                     int acknowledgementNumber,
                     boolean synFlag,
                     boolean ackFlag) {
        this.sequenceNumber = sequenceNumber;
        this.acknowledgementNumber = acknowledgementNumber;
        this.synFlag = synFlag;
        this.ackFlag = ackFlag;
        this.last = false;
    }


    public static byte[] convertToByte(TcpPacket tcpPacket) {
        byte[] stream = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeObject(tcpPacket);
            stream = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream;
    }

    public static TcpPacket makePacket(byte[] stream) {
        TcpPacket packet = null;

        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(stream);
                ObjectInputStream ois = new ObjectInputStream(bais);
            ) {
            packet = (TcpPacket) ois.readObject();
        } catch (IOException e) {
            // Error in de-serialization
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // You are converting an invalid stream to Student
            e.printStackTrace();
        }
        return packet;
    }

    public static TcpPacket receivePacket(EnhancedDatagramSocket udtSocket, int timeout) throws SocketException, IOException {
        udtSocket.setSoTimeout(timeout);
        byte[] buffer = new byte[2048];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, 256);
        udtSocket.receive(datagramPacket);
        return TcpPacket.makePacket(datagramPacket.getData());
    }

    public static void sendTcpPacket(EnhancedDatagramSocket udtSocket, TcpPacket packet , int port) throws Exception{
        byte[] outStream = TcpPacket.convertToByte(packet);
        udtSocket.send(new DatagramPacket(
                outStream,
                outStream.length,
                Constants.getAddress(),
                port)
        );
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAcknowledgementNumber() {
        return acknowledgementNumber;
    }

    public boolean isSynFlag() {
        return synFlag;
    }

    public boolean isAckFlag() {
        return ackFlag;
    }


    public byte[] getPayload() {
        return payload;
    }

    public boolean isLast() {
        return last;
    }

    public int getRwnd() {
        return rwnd;
    }

    public void setRwnd(int rwnd) {
        this.rwnd = rwnd;
    }

    private int rwnd;
    private int sequenceNumber;
    private int acknowledgementNumber;
    private boolean synFlag;
    private boolean ackFlag;
    private boolean last;
    private byte[] payload;
}
