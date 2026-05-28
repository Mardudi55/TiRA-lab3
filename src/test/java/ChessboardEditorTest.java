import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

class ChessboardEditorTest {

    private Chessboard board;
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    @BeforeEach
    void setUp() {
        board = new Chessboard(8);
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private void provideInput(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes()));
    }

    // =========================================================================
    // TESTY JEDNOSTKOWE (LOGIKA EDYTORA)
    // =========================================================================
    @Nested
    @DisplayName("Unit Testy")
    class UnitTests {


        @Test
        @DisplayName("Unit: Zapis i odczyt stanu (użycie Stubów)")
        void shouldSaveAndLoadBoardStateCorrectly() throws Exception {
            AttackCalculator stubCalculator = new AttackCalculatorStub();
            AttackCounter stubCounter = new AttackCounterStub();
            ChessboardEditor editor = new ChessboardEditor(board, stubCalculator, stubCounter);

            editor.placeKnight(new Position(4, 4));

            File tempFile = File.createTempFile("chess_test", ".json");
            tempFile.deleteOnExit();

            editor.saveToFile(tempFile.getAbsolutePath());

            ChessboardEditor newEditor = new ChessboardEditor(new Chessboard(8), stubCalculator, stubCounter);
            newEditor.loadFromFile(tempFile.getAbsolutePath());

            assertTrue(newEditor.getBoard().getKnights().contains(new Position(4, 4)));
            assertEquals(8, newEditor.getBoard().getSize());
        }
    }

    @Nested
    @DisplayName("Unit Testy Mockito (Błędy i Walidacja)")
    class ChessboardEditorMockitoTest {
        private AttackCalculator mockCalculator;
        private AttackCounter mockCounter;

        @BeforeEach
        void setUp() {
            mockCalculator = Mockito.mock(AttackCalculator.class);
            mockCounter = Mockito.mock(AttackCounter.class);
        }

        @Test
        void shouldThrowExceptionOnInvalidFilePath() {
            ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);
            String invalidPath = "/nieistniejacy_katalog/test.json";

            assertThrows(RuntimeException.class, () -> editor.saveToFile(invalidPath));
        }

        @Test
        void shouldThrowExceptionWhenPlacingKnightOutOfBounds() {
            ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);
            Position outOfBoundsPos = new Position(-1, 9);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> editor.placeKnight(outOfBoundsPos));

            assertEquals("Pozycja poza szachownicą!", exception.getMessage());
            Mockito.verifyNoInteractions(mockCalculator, mockCounter);
        }

        @Test
        void shouldThrowExceptionWhenPlacingKnightOnOccupiedField() {
            Mockito.when(mockCalculator.calculateAttack(any(), eq(board))).thenReturn(List.of());
            Mockito.when(mockCounter.count(any(), eq(board))).thenReturn(0);

            ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);
            Position pos = new Position(3, 3);

            editor.placeKnight(pos);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> editor.placeKnight(pos));

            assertEquals("Pole jest już zajęte!", exception.getMessage());
            Mockito.verify(mockCalculator, Mockito.times(1)).calculateAttack(any(), eq(board));
        }
    }

    // =========================================================================
    // TESTY INTEGRACYJNE (SYMULACJA CLI / MAIN)
    // =========================================================================
    @Nested
    @DisplayName("Testy CLI")
    class MainTests {


        @Test
        @DisplayName("CLI: Symulacja pełnej interakcji użytkownika")
        void shouldSimulateMainInteractionAndPlaceKnight() {
            provideInput("1\n2\n6\n4\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Wstawiono skoczka"), "Brak info o wstawieniu");
            assertTrue(output.contains("X=2, Y=6"), "Skoczek nie pojawił się w podsumowaniu stanu");
        }

        @Test
        @DisplayName("CLI: Odporność na wpisanie liter zamiast liczb")
        void shouldHandleInputMismatchInMain() {
            provideInput("nie_liczba\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Wprowadź poprawną liczbę całkowitą!"), "Program powinien obsłużyć błędny typ danych");
        }



        @Test
        @DisplayName("CLI: Pełny cykl zapisu i odczytu przez menu (w pamięci)")
        void shouldSaveAndLoadViaMenuSystem() throws IOException {
            try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
                Path tempFile = fs.getPath("/main_save_test.json");
                String path = tempFile.toAbsolutePath().toString();

                provideInput("1\n1\n1\n2\n" + path + "\n0\n");
                Main.main(new String[]{});


                testOut.reset();
                provideInput("3\n" + path + "\n4\n0\n");
                Main.main(new String[]{});

                String output = testOut.toString();
                assertTrue(output.contains("Pomyślnie załadowano planszę"), "Błąd ładowania");
                assertTrue(output.contains("X=1, Y=1"), "Skoczek zniknął po przeładowaniu");

            }
        }

        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej X")
        void shouldHandleGarbageInXCoordinate() {
            provideInput("1\nX\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Współrzędne muszą być liczbami!"), "Program powinien złapać błąd typu danych wewnątrz metody handlePlaceKnight");
        }

        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej Y")
        void shouldHandleGarbageInYCoordinate() {
            provideInput("1\n4\nY\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Współrzędne muszą być liczbami!"), "Program powinien obsłużyć błąd nawet jeśli X był poprawny");
        }

        @Test
        @DisplayName("CLI: Wybór nieistniejącej opcji menu")
        void shouldHandleInvalidMenuChoice() {
            provideInput("99\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Nieznana opcja. Spróbuj ponownie."), "Program powinien zareagować na wybór spoza zakresu 0-4");
        }
    }

    // =========================================================================
    // TESTY INTEGRACYJNE Z MOCKITO (WERYFIKACJA INTERAKCJI Z ZALEŻNOŚCIAMI)
    // =========================================================================
    @Nested
    @DisplayName("Testy CLI z Mockito (MockConstruction)")
    class MainTestsMockito {

        private final InputStream originalIn = System.in;
        private final PrintStream originalOut = System.out;
        private ByteArrayOutputStream testOut;

        @BeforeEach
        void setUp() {
            testOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(testOut));
        }


        private void provideInput(String data) {
            System.setIn(new ByteArrayInputStream(data.getBytes()));
        }

        @Test
        @DisplayName("CLI: Symulacja pełnej interakcji i weryfikacja wstawienia")
        void shouldSimulateMainInteractionAndPlaceKnight() {
            provideInput("1\n4\n4\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                assertEquals(1, mocked.constructed().size(), "Powinien zostać stworzony tylko jeden edytor");
                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verify(mockEditor, Mockito.times(1)).placeKnight(new Position(4, 4));
            }
        }

        @Test
        @DisplayName("CLI: Pełny cykl zapisu i odczytu przez menu")
        void shouldSaveAndLoadViaMenuSystem() {
            provideInput("2\nplansza.json\n3\nplansza.json\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verify(mockEditor, Mockito.times(1)).saveToFile("plansza.json");
                Mockito.verify(mockEditor, Mockito.times(1)).loadFromFile("plansza.json");
            }
        }

        @Test
        @DisplayName("CLI: Pokaż stan planszy (weryfikacja wywołania getBoard)")
        void shouldShowBoardState() {
            provideInput("4\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class,
                    (mock, context) -> Mockito.when(mock.getBoard()).thenReturn(new Chessboard(8)))) {

                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verify(mockEditor, Mockito.atLeastOnce()).getBoard();
            }
        }

        @Test
        @DisplayName("CLI: Odporność na wpisanie liter zamiast liczb (główne menu)")
        void shouldHandleInputMismatchInMain() {
            provideInput("nie_liczba\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verifyNoInteractions(mockEditor);

                assertTrue(testOut.toString().contains("Błąd: Wprowadź poprawną liczbę całkowitą!"));
            }
        }

        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej X")
        void shouldHandleGarbageInXCoordinate() {
            provideInput("1\nX\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verify(mockEditor, Mockito.never()).placeKnight(any());
                assertTrue(testOut.toString().contains("Błąd: Współrzędne muszą być liczbami!"));
            }
        }

        @Test
        @DisplayName("CLI: Wybór nieistniejącej opcji menu")
        void shouldHandleInvalidMenuChoice() {
            provideInput("99\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verifyNoInteractions(mockEditor);
                assertTrue(testOut.toString().contains("Nieznana opcja. Spróbuj ponownie."));
            }
        }
    }
}