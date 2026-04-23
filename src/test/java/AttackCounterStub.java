import static org.junit.jupiter.api.Assertions.*;

public class AttackCounterStub implements AttackCounter {
    @Override
    public int count(Position position, Chessboard board) {
        return 2; // Hardkodowana odpowiedź dla testów
    }
}