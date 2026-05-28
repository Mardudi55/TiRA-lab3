public interface BoardStorage {
    void save(String path, Chessboard board) throws Exception;
    void load(String path, Chessboard board) throws Exception;
}