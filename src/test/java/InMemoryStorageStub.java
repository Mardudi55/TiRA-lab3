import java.util.HashMap;
import java.util.Map;

public class InMemoryStorageStub implements BoardStorage {
    public final Map<String, String> virtualDisk = new HashMap<>();

    @Override
    public void save(String path, Chessboard board) {
        virtualDisk.put(path, "SAVED_STATE");
    }

    @Override
    public void load(String path, Chessboard board) {
        if (!virtualDisk.containsKey(path)) {
            throw new RuntimeException("Plik nie istnieje!");
        }
        board.getKnights().clear();
        board.addKnight(new Position(1, 1));
    }
}