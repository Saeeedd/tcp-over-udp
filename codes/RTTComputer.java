import java.util.ArrayList;

public class RTTComputer {
    public RTTComputer(int initSize){
        this.startTimeList = new ArrayList<Long>(initSize);
    }

    public void sendEvent(int sequenceNum){
        int chunkListSize = this.startTimeList.size();
        long currentTime = System.currentTimeMillis();
        if (sequenceNum == chunkListSize){
            this.startTimeList.add(currentTime);
        }
        else if (sequenceNum > chunkListSize){
            for(int i = chunkListSize ; i < sequenceNum ; i ++){
                startTimeList.add(null);
            }
            startTimeList.add(currentTime);
        }
        else if (sequenceNum < chunkListSize){
            this.startTimeList.set(sequenceNum,currentTime);
        }
    }

    public long getRTT(int ackNum){
        if ( ackNum < this.startTimeList.size() && this.startTimeList.get(ackNum)!=null){
            return System.currentTimeMillis() - this.startTimeList.get(ackNum);
        }
        else{
            return -1;
        }

    }

    private ArrayList<Long> startTimeList;
}
