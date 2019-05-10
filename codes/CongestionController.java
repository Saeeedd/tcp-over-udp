class CongestionController {
    public enum State {
        SLOW_START,
        CONGESTION_AVOIDANCE,
        FAST_RECOVERY,

    }

    public CongestionController(TCPSocket tcpSocket) {
        this.socket = tcpSocket;
        this.cwnd = 1;
        this.windowBase = 0;
        this.sentBase = 0;
        this.lastAck = -1;
        this.dupAckNum = 0;
        this.state = State.SLOW_START;
        this.cwndLittleChange = 0;
        this.sshtresh = 0;
    }

    public void renderAck(int ack) {
        if (this.cwnd > sshtresh && this.state == State.SLOW_START) {
            this.state = State.CONGESTION_AVOIDANCE;
            this.cwndLittleChange = 0;
        }

        if (lastAck == ack) {
            this.dupAckNum++;
        }

        else {
            this.dupAckNum = 0;
        }

        if (this.dupAckNum >= 3) {
            System.out.println("triple dup ack");
            this.dupAckNum = 0;
            this.state = State.SLOW_START;
            this.cwndLittleChange = 0;
            this.sshtresh = this.cwnd / 2;
            this.cwnd = 1;
            this.socket.onWindowChange();

            this.sentBase = this.windowBase;
        }

        System.out.println(this.cwnd);

        if (ack >= (this.windowBase)) {
            System.out.println("windowBase increased");

            switch (this.state) {
                case SLOW_START:
                    this.cwnd += 1;
                    this.socket.onWindowChange();
                    break;
                case CONGESTION_AVOIDANCE:
                    this.cwndLittleChange += 1;
                    if (this.cwndLittleChange == this.cwnd) {
                        this.cwnd += 1;
                        this.socket.onWindowChange();
                        this.cwndLittleChange = 0;
                    }
                    break;
            }

            this.windowBase = ack;
            this.lastAck = ack;
        }

        else {
            System.out.println("Not good ack received");
            this.sentBase = this.windowBase;
        }

        switch (this.state) {
            case SLOW_START: System.out.println("Slow start"); break;
            case CONGESTION_AVOIDANCE: System.out.println("Congestion Avoidance"); break;
            case FAST_RECOVERY: System.out.println("Fast Recovery"); break;
        }

        System.out.println("CWND : " + String.valueOf(this.cwnd));

    }

    public boolean isWindowFull() {
        return (this.sentBase - this.windowBase) >= this.cwnd;
    }

    public int nextChunkIndex() {
        this.sentBase++;
        return this.sentBase - 1;
    }

    public int getSSThreshold() {
        return this.sshtresh;
    }

    public int getCWND() {
        return this.cwnd;
    }

    public void timeoutOccured() {
        this.state = State.SLOW_START;
        this.sshtresh = cwnd / 2;
        this.cwnd = 1;
        socket.onWindowChange();
        this.sentBase = this.windowBase;
    }

    public int getWindowHead() {
        return this.windowBase;
    }

    private TCPSocket socket;
    private State state;
    private int cwnd;
    private int cwndLittleChange;
    private int windowBase;
    private int sentBase;
    private int lastAck;
    private int dupAckNum;
    private int sshtresh;
}
