import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
    /// TODO: Complete this method with splitting the file
    public static ArrayList<String> splitFileByChunks(String filePath) {
        ArrayList<String> chunks = new ArrayList<>();

        for (int i = 1; i <= 100; i++)
            chunks.add("chunk" + String.valueOf(i));

        return chunks;
    }
}
