import java.util.ArrayList;

public class NagleBuffer {

    private static final int bufferTreshold = 10000;

    public byte[] getElement(int index){
        return this.bufferedChunks.get(index);
    }

    public byte[] getElementOrFlush(int index, int length){
        if(index < this.bufferedChunks.size()){
            System.out.println("queue: " + String.valueOf(this.sizeOfQueue()) + " , buffer: " + String.valueOf(this.buffer.length) + " index: " + String.valueOf(index));
            return this.getElement(index);
        }
        else if(index == this.bufferedChunks.size()){
            System.out.println("queue: " + String.valueOf(this.sizeOfQueue()) + " , buffer: " + String.valueOf(this.buffer.length) + " flushing: " + String.valueOf(length));
            return this.flushBufferdData(length);
        }
        else{
            System.out.println("FUCKKK");
            return null;
        }
    }

    public void addToBuffer(byte[] chunk){
        System.out.println("add chunk: " + String.valueOf(chunk.length) + " to buffer: " + String.valueOf(this.buffer.length));
        byte[] newBuffer = new byte[this.buffer.length + chunk.length];
        System.arraycopy(this.buffer,0,newBuffer,0,this.buffer.length);
        System.arraycopy(chunk,0,newBuffer,this.buffer.length,chunk.length);
        this.buffer = newBuffer;
    }

//    public byte[] flushAllBuffered(){
//        this.bufferedChunks.add(this.buffer);
//        this.buffer = new byte[0];
//        return this.bufferedChunks.get(this.bufferedChunks.size()-1);
//    }

    public byte[] flushBufferdData(int length){
        int newArrayLen = 0;
        if(length < this.buffer.length){
            newArrayLen = length;
        }else{
            newArrayLen = this.buffer.length;
        }
        byte[] newData = new byte[newArrayLen];
        for (int i=0 ; i<newArrayLen ; i++){
            newData[i] = this.buffer[i];
        }
        this.bufferedChunks.add(newData);

        byte[] newBuffer = new byte[0];
        if(newArrayLen < this.buffer.length){
            newBuffer = new byte[this.buffer.length-newArrayLen];
            System.arraycopy(this.buffer,newArrayLen-1,newBuffer,0,this.buffer.length-newArrayLen);
        }
        this.buffer = newBuffer;

        return newData;
    }

//    public int availableBufferData(){
//        return this.buffer.length;
//    }

    public void done(){
        this.isDone = true;
        System.out.println("buffering done with queue: " + String.valueOf(this.sizeOfQueue()) + " buffer: " + String.valueOf(this.buffer.length));
    }

    public int sizeOfQueue(){
        return this.bufferedChunks.size();
    }

    public boolean canGetIndex(int index){
        if(index < 0){
            return false;
        }
        if (buffer.length == 0){
            return index < this.bufferedChunks.size();
        }
        else{
            return index <= this.bufferedChunks.size();
        }
    }

    public boolean canGetBuffer(int index, int length){
        if(index < 0){
            return false;
        }
        if (buffer.length < length){
            return index < this.bufferedChunks.size();
        }
        else{
            return index <= this.bufferedChunks.size();
        }
    }

    public boolean isLastchunk(int index){
        return (index == this.sizeOfQueue() - 1) && (this.buffer.length==0) && (this.isDone) ;
    }

    public boolean canBufferMore(){
        return this.buffer.length < bufferTreshold;
    }

    private ArrayList<byte[]> bufferedChunks = new ArrayList<>();
    private byte[] buffer = new byte[0];
    private boolean isDone = false;
}
