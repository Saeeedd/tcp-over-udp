public class CongestionController {
    public CongestionController() {
        this.cwnd = 1;
        this.windowBase = 0;
        this.sentBase = 0;
    }

    public void ackReceived(int ack) {
        if (ack == (this.windowBase)) {
            this.windowBase++;
        }
    }

    public boolean isWindowFull() {
        return (sentBase - windowBase) >= cwnd;
    }

    public int nextChunkIndex() {
        this.sentBase++;
        return this.sentBase - 1;
    }

    public int getSssthreshold() {
        return 0;
    }

    public int getCwnd() {
        return cwnd;
    }

    private int cwnd;
    private int windowBase;
    private int sentBase;
}
