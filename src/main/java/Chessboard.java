import java.util.HashSet;
import java.util.Set;

public class Chessboard {
    private int size;
    private Set<Position> knights = new HashSet<>();

    // Pusty konstruktor wymagany przez Jackson (JSON)
    public Chessboard() {}

    public Chessboard(int size) {
        this.size = size;
    }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public Set<Position> getKnights() { return knights; }
    public void setKnights(Set<Position> knights) { this.knights = knights; }

    public void addKnight(Position position) {
        knights.add(position);
    }
}