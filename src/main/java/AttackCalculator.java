import java.util.List;

public interface AttackCalculator {
    List<Position> calculateAttack(Position position, Chessboard board);
}