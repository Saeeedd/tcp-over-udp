import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
    /// TODO: Complete this method with splitting the file
    public static final int chunkSize = 50;

    public static ArrayList<byte[]> splitFileByChunks(String filePath) {
        ArrayList<byte[]> chunks = new ArrayList<>();

        byte[] chunk = new byte[chunkSize];
        int readBytes;
        File file = new File(filePath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            readBytes = fis.read(chunk);

            if (readBytes < chunkSize) {
                chunk = Arrays.copyOfRange(chunk, 0, readBytes);
            }

            chunks.add(chunk);
            while (readBytes == chunkSize) {
                chunk = new byte[chunkSize];
                readBytes = fis.read(chunk);
                if (readBytes < chunkSize) {
                    chunk = Arrays.copyOfRange(chunk, 0, readBytes);
                }
                chunks.add(chunk);
            }

            fis.close();
        } catch (Exception exception) {
            System.out.println("IO exception");
        }

        return chunks;
    }

    public static void writeChunksToFile(ArrayList<byte[]> chunks) {
        File file = new File("out.txt");
        FileOutputStream fis = null;

        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        try {
            fis = new FileOutputStream(file);
//            chunks.remove(chunks.size() - 1);
            for (byte[] chunk : chunks) {
                fis.write(chunk);
            }
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
    }
}
