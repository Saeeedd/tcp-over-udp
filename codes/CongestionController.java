class CongestionController {


    public enum State {
        SLOW_START,
        CONGESTION_AVOIDANCE,
        FAST_RECOVERY,
        EXPONENTIAL_BACKOFF,
    }

    public CongestionController(TCPSocket tcpSocket) {
        this.socket = tcpSocket;
        this.windowBase = 0;
        this.sentBase = 0;
        this.dupAckNum = 0;
        this.state = State.SLOW_START;
        this.ssthresh = 20;
        this.timeout = 100;
        this.shouldResend = false;
        this.highWater = 0;
        this.MSS = 1;
        this.cwnd = 1;
    }

    public int getNextSendIndex(){
        if (this.shouldResend){
            return this.windowBase;
        }
        else if(this.canSendMore()){
            return this.sentBase;
        }
        else{
            return -1;
        }

    }

    public void sendEvent(){
        if (this.shouldResend) {
            this.shouldResend = false;
        }
        else if(this.canSendMore()){
            this.sentBase += 1;
        }
    }

    public int getRecivedDataIndex() {
        return this.windowBase - 1 ;
    }

    public int getCwnd(){

        return (int)this.cwnd;
    }

    public int getTimeout() {
        return (int) (this.timeout + 10);
    }

    public int getSsthresh() {
        return ssthresh;
    }

    private boolean canSendMore(){

        return this.sentBase - this.windowBase <= cwnd;
    }

    public void ackNumHandler(int ack, long rttTimeSample){
        this.changeDupAckNum(ack);

        switch (this.state){
            case SLOW_START:
                if((this.dupAckNum == 0) && (this.cwnd + this.MSS < this.ssthresh)){   // new ack and not reach tresh
                    this.setCwnd(this.cwnd + this.MSS);
                    this.setTimeout(rttTimeSample);
                }
                else if (this.dupAckNum == 2){
                    this.dupAckNum = 0;
                    this.ssthresh = (int)(this.cwnd /2);
                    this.setCwnd(this.ssthresh + 3);
                    this.shouldResend = true;
                    this.state = State.FAST_RECOVERY;
                    System.out.println("Fast Recovery");
                    this.highWater = this.sentBase - 1;
                }
                else if(this.cwnd + this.MSS >= this.ssthresh){
                    this.dupAckNum = 0;
                    this.setCwnd(this.cwnd + this.MSS);
                    this.state = State.CONGESTION_AVOIDANCE;
                    System.out.println("Congestion Avoidance");
                }
                break;
            case FAST_RECOVERY:
                if(this.dupAckNum != 0){        // dup accure
                    this.setCwnd(this.cwnd + this.MSS);
                }
                else{
                    if(ack < this.highWater){
                        this.setCwnd(this.cwnd - this.windowBase);
                        this.shouldResend = true;
                    }
                    else{
                        this.setCwnd(this.ssthresh);
                    }
                }
                break;
            case CONGESTION_AVOIDANCE:
                if (this.dupAckNum ==0){
                    this.setCwnd(this.cwnd + this.MSS * (this.MSS / this.cwnd));
                    this.setTimeout(rttTimeSample);
                }
                else if(this.dupAckNum == 2){
                    this.dupAckNum = 0;
                    this.ssthresh = (int)(this.cwnd / 2);
                    this.setCwnd(this.ssthresh + 3);
                    this.highWater = this.sentBase;
                    this.shouldResend = true;
                    this.state = State.FAST_RECOVERY;
                    System.out.println("Fast Recovery");
                }
                break;
            case EXPONENTIAL_BACKOFF:
                this.state = State.SLOW_START;
                System.out.println("Slow start");
                break;
        }

        this.changeWindowBase(ack);
    }

    public void timeoutHandler(){
        if(this.state != State.EXPONENTIAL_BACKOFF) {
            this.ssthresh = (int)(this.cwnd / 2);
            this.state = State.EXPONENTIAL_BACKOFF;
            System.out.println("Exp - Backoff");
        }
        this.setCwnd(1);
        this.setTimeout(this.timeout * 2);  // should *= 2
        this.shouldResend = true;
    }

    private void changeDupAckNum(int ack){
        if (ack == this.windowBase - 1){
            this.dupAckNum ++;
        }
        else {
            this.dupAckNum = 0;
        }
    }

    private void changeWindowBase(int ack){
        if(ack >= this.windowBase){
            this.windowBase = ack + 1;
        }
    }

    private void setCwnd(float cwnd) {
        if(cwnd < 1){
            cwnd = 1;
        }

        this.cwnd = cwnd;
        this.socket.onWindowChange();
    }

    private void setTimeout(long timeout){
        if (timeout == -1){
            return;
        }
        if (timeout <= 1000){
            this.timeout = timeout;
        }
    }

    private TCPSocket socket;
    private State state;
    private float cwnd;
    private int windowBase;
    private int sentBase;
    private int dupAckNum;    private int ssthresh;
    private int MSS;
    private long timeout;
    private boolean shouldResend ;
    private int highWater;     // last sent data save point when going to FAST RECOVERY state
}

