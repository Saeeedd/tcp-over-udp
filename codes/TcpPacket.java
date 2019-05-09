import java.io.*;
import java.net.DatagramPacket;
import java.net.SocketException;

public class TcpPacket implements Serializable {

    private TcpPacket(int sequenceNumber,
                     int acknowledgementNumber,
                     byte[] payload,
                     boolean synFlag,
                     boolean ackFlag,
                     boolean last) {
        this.sequenceNumber = sequenceNumber;
        this.acknowledgementNumber = acknowledgementNumber;
        this.synFlag = synFlag;
        this.ackFlag = ackFlag;
        this.last = last;
        this.payload = payload;
    }

    public static TcpPacket generateAck(int acknowledgementNumber){
        byte[] payload = {};
        return new TcpPacket(0,acknowledgementNumber,payload,false,true,false);
    }

    public static TcpPacket generateDataPack(byte[] payload,int sequenceNumber, boolean last){
        return new TcpPacket(sequenceNumber,0,payload,false,false,last);
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


    public TcpPacket(boolean last,
                     byte[] payload) {
        this.synFlag = false;
        this.ackFlag = false;
        this.last = last;
        this.payload = payload;
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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getAcknowledgementNumber() {
        return acknowledgementNumber;
    }

    public void setAcknowledgementNumber(int acknowledgementNumber) {
        this.acknowledgementNumber = acknowledgementNumber;
    }

    public boolean isSynFlag() {
        return synFlag;
    }

    public void setSynFlag(boolean synFlag) {
        this.synFlag = synFlag;
    }

    public boolean isAckFlag() {
        return ackFlag;
    }

    public void setAckFlag(boolean ackFlag) {
        this.ackFlag = ackFlag;
    }


    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }


    private int sequenceNumber;
    private int acknowledgementNumber;
    private boolean synFlag;
    private boolean ackFlag;
    private boolean last;
    private byte[] payload;
}
