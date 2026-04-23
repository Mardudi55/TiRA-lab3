import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class AttackCalculatorStub implements AttackCalculator {
    @Override
    public List<Position> calculateAttack(Position position, Chessboard board) {
        // Hardkodowana odpowiedź dla testów
        return List.of(new Position(position.x() + 1, position.y() + 2),
                new Position(position.x() + 2, position.y() + 1));
    }
}