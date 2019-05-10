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
        this.timeout = 200;
        this.shouldResend = false;
        this.highWater = 0;
        this.MSS = 10;
        this.cwnd = 10;
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
        if (this.shouldResend){
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

        return this.cwnd;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getSsthresh() {
        return ssthresh;
    }

    private boolean canSendMore(){

        return this.sentBase - this.windowBase <= cwnd;
    }

    public void ackNumHandler(int ack){
        this.changeDupAckNum(ack);

        switch (this.state){
            case SLOW_START:
                if((this.dupAckNum == 0) && (this.cwnd+this.MSS < this.ssthresh)){   // new ack and not reach tresh
                    this.setCwnd(this.cwnd + this.MSS);
                }
                else if (this.dupAckNum == 2){
                    this.dupAckNum = 0;
                    this.ssthresh = this.cwnd /2;
                    this.setCwnd(this.ssthresh + 3);
                    this.shouldResend = true;
                    this.state = State.FAST_RECOVERY;
                    this.highWater = this.sentBase - 1;
                }
                else if(this.cwnd + this.MSS >= this.ssthresh){
                    this.dupAckNum = 0;
                    this.setCwnd(this.cwnd + this.MSS);
                    this.state = State.CONGESTION_AVOIDANCE;
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
                }
                else if(this.dupAckNum == 2){
                    this.dupAckNum = 0;
                    this.ssthresh = this.cwnd /2;
                    this.setCwnd(this.ssthresh + 3);
                    this.highWater = this.sentBase;
                    this.shouldResend = true;
                    this.state = State.FAST_RECOVERY;
                }
                break;
            case EXPONENTIAL_BACKOFF:
                this.state = State.SLOW_START;
                break;
        }

        this.changeWindowBase(ack);
    }

    public void timeoutHandler(){
        if(this.state != State.EXPONENTIAL_BACKOFF) {
            this.ssthresh = this.cwnd / 2;
            this.state = State.EXPONENTIAL_BACKOFF;
        }
        this.setCwnd(1);
        if(this.timeout < 1000){
            this.timeout += 20;         // should *= 2
        }
        this.shouldResend = true;
    }

    private void changeDupAckNum(int ack){
        if (ack == this.windowBase-1){
            this.dupAckNum ++;
        }
        else{
            this.dupAckNum = 0;
        }
    }

    private void changeWindowBase(int ack){

        this.windowBase = ack + 1;
    }

    private void setCwnd(int cwnd) {
        if(cwnd < 1){
            cwnd = 1;
        }
        this.cwnd = cwnd;
        this.socket.onWindowChange();
    }

    private TCPSocket socket;
    private State state;
    private int cwnd;
    private int windowBase;
    private int sentBase;
    private int dupAckNum;
    private int ssthresh;
    private int MSS;
    private int timeout;
    private boolean shouldResend ;
    private int highWater;     // last sent data save point when going to FAST RECOVERY state
}

