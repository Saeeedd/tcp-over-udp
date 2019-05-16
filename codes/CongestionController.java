class CongestionController {


    public enum State {
        SLOW_START,
        CONGESTION_AVOIDANCE,
        FAST_RECOVERY,
    }

    public CongestionController(TCPSocket tcpSocket) {
        this.socket = tcpSocket;
        this.windowBase = 0;
        this.sentBase = 0;
        this.dupAckNum = 0;
        this.state = State.SLOW_START;
        this.timeout = 1000;
        this.shouldResend = false;
        this.highWater = 0;
        this.MSS = 1;
        this.cwnd = this.MSS;
        this.ssthresh = 30 * this.MSS;

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



    public int getTimeout() {
        return (int) (this.timeout + 2);
    }

    public long getSsthresh() {
        return ssthresh;
    }

    public long getCwnd(){
        return (long) (this.cwnd);
    }

    private boolean canSendMore(){
        return this.sentBase - this.windowBase < (int)(this.cwnd);
    }

    public void ackNumHandler(int ack, long rttTimeSample){

            this.changeDupAckNum(ack);

            switch (this.state){
                case SLOW_START:
                    if(this.cwnd + this.MSS < this.ssthresh){   // new ack and not reach tresh
                        this.setCwnd(this.cwnd + this.MSS);
                        this.setTimeout(rttTimeSample);
                        System.out.println("add MSS to cwnd in slow start");
                    }
                    else if(this.cwnd + this.MSS >= this.ssthresh){
                        this.dupAckNum = 0;         //UNKNOWN
                        this.setCwnd(this.cwnd + this.MSS);
                        this.state = State.CONGESTION_AVOIDANCE;
                        System.out.println("Congestion Avoidance");
                    }
                    break;
                case CONGESTION_AVOIDANCE:
                    if (this.dupAckNum == 3){
                        this.ssthresh = (int)(this.cwnd / 2);
                        this.setCwnd(this.ssthresh + 3);
                        this.shouldResend = true;
                        this.state = State.FAST_RECOVERY;
                        this.highWater = this.sentBase;
                        System.out.println("Fast Recovery");
                    }
                    else {
                        this.setCwnd(this.cwnd + this.MSS * (this.MSS / this.cwnd));
                        this.setTimeout(rttTimeSample);
                        System.out.println("add 1 to cwnd in congestion");
                    }
                    break;
                case FAST_RECOVERY:
                    if(this.dupAckNum != 0){
                        this.setCwnd(this.cwnd + (float)(0.4) );  //UNKNOWN
                        System.out.println("dupAck in fastRecovery");
                        if(this.dupAckNum >= this.ssthresh){
                            this.dupAckNum = 1;
                            shouldResend = true;
                        }
                    }
                    else {
                        if(ack < highWater ){
                            this.setCwnd(this.cwnd + (float)(0.4) );  //UNKNOWN
                            this.shouldResend = true;
                            System.out.println("new ack in fast recovery");
                        }
                        else{
                            this.setCwnd(this.ssthresh);
                            this.state = State.CONGESTION_AVOIDANCE;
                            System.out.println("CONGESTION_AVOIDANCE");
                        }
                    }
                    break;
            }

            this.changeWindowBase(ack);
    }

    public void timeoutHandler(){
        if(this.state != State.SLOW_START) {
            this.ssthresh = (int)(this.cwnd / 2);
            this.state = State.SLOW_START;
        }
        this.setCwnd(1);
        this.sentBase = this.windowBase;
        this.setTimeout(this.timeout * 2);  // should *= 2
        System.out.println("Timeout");
    }

    private void changeDupAckNum(int ack){
        if (ack == this.windowBase - 1){
            this.dupAckNum ++;
        }
        else if(ack >= this.windowBase) {
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
        System.out.print("cwnd = " + String.valueOf(this.cwnd) + " : ");
        this.socket.onWindowChange();
    }

    private void setTimeout(long timeout){
        if (timeout == -1){
            return;
        }
        if (timeout <= 1000){
            this.timeout = timeout;
        }
        else{
            this.timeout = 1000;
        }
    }

    private TCPSocket socket;
    private State state;
    private float cwnd;
    private int windowBase;
    private int sentBase;
    private int dupAckNum;
    private int ssthresh;
    private int MSS;
    private long timeout;
    private boolean shouldResend ;
    private int highWater;     // last sent data save point when going to FAST RECOVERY state
}

