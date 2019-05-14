import java.util.ArrayList;

public class FileChunksHandler {

    public void addChunk(byte[] payload, int firstIndex, int lastIndex){
        this.setElement(firstIndex,payload);
        for (int i=firstIndex+1; i<= lastIndex ; i++){
            this.setElement(i, new byte[0]);
        }
    }

    private void setElement(int index, byte[] data){
        int chunkListSize = this.fileChunks.size();
        if (index == chunkListSize){
            this.fileChunks.add(data);
        }
        else if (index > chunkListSize){
            for(int i = chunkListSize ; i < index ; i ++){
                fileChunks.add(null);
            }
            fileChunks.add(data);
        }
        else if (index < chunkListSize){
            this.fileChunks.set(index, data);
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

    public ArrayList<byte[]> getFileChunks() {
        return fileChunks;
    }

    private ArrayList<byte[]> fileChunks = new ArrayList<byte[]>();
}
