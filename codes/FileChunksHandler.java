import java.util.ArrayList;

public class FileChunksHandler {

    public void addChunk(byte[] payload, int index){
        int chunkListSize = this.fileChunks.size();
        if (index == chunkListSize){
            this.fileChunks.add(payload);
        }
        else if (index > chunkListSize){
            for(int i = chunkListSize ; i < index ; i ++){
                fileChunks.add(null);
            }
            fileChunks.add(payload);
        }
        else if (index < chunkListSize && this.fileChunks.get(index)==null){
           this.fileChunks.set(index,payload);
        }
    }

    public int getLastCompletedIndex(){

        for (int i = 0; i < this.fileChunks.size(); i++){
            if(this.fileChunks.get(i)==null){
                return i-1;
            }
        }
        return this.fileChunks.size()-1;
    }

    public boolean isChunksComplete(){
        return getLastCompletedIndex() == this.fileChunks.size()-1;
    }

    public ArrayList<byte[]> getFileChunks() {
        return fileChunks;
    }

    private ArrayList<byte[]> fileChunks = new ArrayList<byte[]>();
}
