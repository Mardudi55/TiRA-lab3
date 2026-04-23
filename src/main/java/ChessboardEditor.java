import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ChessboardEditor {
    private Chessboard board;
    private final AttackCalculator calculator;
    private final AttackCounter counter;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChessboardEditor(Chessboard board, AttackCalculator calculator, AttackCounter counter) {
        this.board = board;
        this.calculator = calculator;
        this.counter = counter;
    }

    public void placeKnight(Position pos) {
        if (pos.x() < 0 || pos.x() >= board.getSize() || pos.y() < 0 || pos.y() >= board.getSize()) {
            throw new IllegalArgumentException("Pozycja poza szachownicą!");
        }
        if (board.getKnights().contains(pos)) {
            throw new IllegalArgumentException("Pole jest już zajęte!");
        }

        board.addKnight(pos);

        int attacksCount = counter.count(pos, board);
        List<Position> attackedFields = calculator.calculateAttack(pos, board);

        System.out.println("Wstawiono skoczka. Liczba atakowanych pól: " + attacksCount);
        System.out.println("Położenie atakowanych pól: " + attackedFields);
    }

    public void saveToFile(String path) {
        try {
            mapper.writeValue(new File(path), board);
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas zapisu pliku: " + path, e);
        }
    }

    public void loadFromFile(String path) {
        try {
            this.board = mapper.readValue(new File(path), Chessboard.class);
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas odczytu pliku: " + path, e);
        }
    }

    public Chessboard getBoard() {
        return board;
    }
}