public class CongestionController {
    public CongestionController() {
        this.cwnd = 1;
        this.windowBase = 0;
        this.sentBase = 0;
    }

    public void renderAck(int ack) {
        if (ack == (this.windowBase)) {
            System.out.println("windowBase increased");
            this.windowBase++;
        }
        else{
            System.out.println("Not good ack received");
            this.sentBase = this.windowBase;
        }
    }

    public boolean isWindowFull() {
        return (sentBase - windowBase) >= cwnd;
    }

    public int nextChunkIndex() {
        this.sentBase++;
        return this.sentBase - 1;
    }

    public int getSSThreshold() {
        return 0;
    }

    public int getCWND() {
        return cwnd;
    }

    public void timeoutAccured(){
        this.sentBase = this.windowBase;
    }

    public int getWindowHead(){
        return this.windowBase;
    }

    private int cwnd;
    private int windowBase;
    private int sentBase;
}
