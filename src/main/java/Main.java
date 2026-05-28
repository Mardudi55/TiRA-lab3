import java.util.List;

public class Main {
    public static BoardStorage storage = new RealBoardStorage();

    private static InputReader inputReader = new ConsoleInputReader();

    public static void setInputReader(InputReader reader) {
        inputReader = reader;
    }

    public static void main(String[] args) {
        AttackCalculator dummyCalculator = (pos, board) -> List.of(
                new Position(pos.x() + 1, pos.y() + 2),
                new Position(pos.x() + 2, pos.y() + 1)
        );
        AttackCounter dummyCounter = (pos, board) -> 2;

        Chessboard board = new Chessboard(8);
        ChessboardEditor editor = new ChessboardEditor(board, dummyCalculator, dummyCounter);

        System.out.println("=========================================");
        System.out.println("   Witaj w Edytorze Szachownicy (CLI)    ");
        System.out.println("=========================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("\nWybierz opcję: ");

            String input = inputReader.read();
            if (input == null) {
                System.out.println("Koniec strumienia wejściowego. Awaryjne zamykanie.");
                break;
            }

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Błąd: Wprowadź poprawną liczbę całkowitą!");
                continue;
            }

            switch (choice) {
                case 1 -> handlePlaceKnight(editor);
                case 2 -> handleSaveToFile(editor);
                case 3 -> handleLoadFromFile(editor);
                case 4 -> handleShowBoardState(editor);
                case 0 -> {
                    System.out.println("Zamykanie programu. Do widzenia!");
                    running = false;
                }
                default -> System.out.println("Nieznana opcja. Spróbuj ponownie.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Dodaj skoczka na planszę");
        System.out.println("2. Zapisz stan planszy do pliku (JSON)");
        System.out.println("3. Wczytaj stan planszy z pliku (JSON)");
        System.out.println("4. Pokaż aktualnie zajęte pola");
        System.out.println("0. Zakończ program");
    }

    private static void handlePlaceKnight(ChessboardEditor editor) {
        try {
            System.out.print("Podaj współrzędną X: ");
            String inputX = inputReader.read();
            if (inputX == null) return;
            int x = Integer.parseInt(inputX);

            System.out.print("Podaj współrzędną Y: ");
            String inputY = inputReader.read();
            if (inputY == null) return;
            int y = Integer.parseInt(inputY);

            editor.placeKnight(new Position(x, y));

        } catch (NumberFormatException e) {
            System.out.println("Błąd: Współrzędne muszą być liczbami!");
        } catch (IllegalArgumentException e) {
            System.out.println("Błąd logiki: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Wystąpił nieoczekiwany błąd: " + e.getMessage());
        }
    }

    private static void handleSaveToFile(ChessboardEditor editor) {
        System.out.print("Podaj ścieżkę do pliku (np. plansza.json): ");
        String path = inputReader.read();
        if (path == null) return;
        try {
            editor.saveToFile(path);
            System.out.println("Pomyślnie zapisano planszę do: " + path);
        } catch (RuntimeException e) {
            System.out.println("Błąd I/O: " + e.getMessage());
        }
    }

    private static void handleLoadFromFile(ChessboardEditor editor) {
        System.out.print("Podaj ścieżkę do pliku (np. plansza.json): ");
        String path = inputReader.read();
        if (path == null) return;
        try {
            editor.loadFromFile(path);
            System.out.println("Pomyślnie załadowano planszę z pliku!");
        } catch (RuntimeException e) {
            System.out.println("Błąd I/O: " + e.getMessage());
        }
    }

    private static void handleShowBoardState(ChessboardEditor editor) {
        Chessboard currentBoard = editor.getBoard();
        System.out.println("Rozmiar planszy: " + currentBoard.getSize() + "x" + currentBoard.getSize());

        if (currentBoard.getKnights().isEmpty()) {
            System.out.println("Plansza jest pusta (brak skoczków).");
        } else {
            System.out.println("Pozycje skoczków na planszy:");
            currentBoard.getKnights().forEach(pos ->
                    System.out.println(" - Skoczek na: X=" + pos.x() + ", Y=" + pos.y())
            );
        }
    }
}