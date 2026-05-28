import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Klasa testowa dla aplikacji edytora szachownicy.
 * Zawiera kompleksowy zestaw testów podzielony na:
 * <ul>
 * <li>Testy jednostkowe logiki edytora (z użyciem Stubów).</li>
 * <li>Testy jednostkowe walidacji i błędów (z użyciem Mockito).</li>
 * <li>Testy integracyjne symulujące działanie klasycznego CLI.</li>
 * <li>Testy integracyjne CLI weryfikujące interakcje za pomocą Mockito.</li>
 * </ul>
 */
class ChessboardEditorTest {

    private Chessboard board;
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    /**
     * Przygotowuje środowisko przed każdym testem.
     * Inicjalizuje nową szachownicę i przekierowuje standardowe wyjście (System.out)
     * do bufora w pamięci, aby móc weryfikować komunikaty wypisywane na konsolę.
     */
    @BeforeEach
    void setUp() {
        board = new Chessboard(8);
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    /**
     * Przywraca oryginalne strumienie wejścia/wyjścia po każdym teście,
     * aby zapobiec wpływowi testów na siebie nawzajem lub na środowisko uruchomieniowe.
     */
    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    /**
     * Metoda pomocnicza do symulacji danych wpisywanych przez użytkownika z klawiatury.
     * Podmienia standardowe wejście (System.in) na strumień zawierający przekazany tekst.
     *
     * @param data Ciąg znaków symulujący wejście (np. komendy oddzielone znakiem nowej linii \n)
     */
    private void provideInput(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes()));
    }

    // =========================================================================
    // TESTY JEDNOSTKOWE (LOGIKA EDYTORA)
    // =========================================================================

    /**
     * Zestaw testów jednostkowych wykorzystujących proste klasy typu Stub
     * do weryfikacji poprawności przepływu danych.
     */
    @Nested
    @DisplayName("Unit Testy")
    class UnitTests {

        /**
         * Weryfikuje, czy stan szachownicy (pozycje skoczków) jest poprawnie
         * zapisywany do pliku tymczasowego, a następnie z niego odczytywany.
         */
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

    /**
     * Zestaw testów jednostkowych wykorzystujących bibliotekę Mockito
     * do symulowania zachowań zależności i testowania obsługi błędów oraz walidacji.
     */
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

        /**
         * Weryfikuje, czy podanie nieprawidłowej ścieżki do zapisu pliku skutkuje
         * rzuceniem wyjątku RuntimeException.
         */
        @Test
        void shouldThrowExceptionOnInvalidFilePath() {
            ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);
            String invalidPath = "/nieistniejacy_katalog/test.json";

            assertThrows(RuntimeException.class, () -> editor.saveToFile(invalidPath));
        }

        /**
         * Weryfikuje rzucanie wyjątku IllegalArgumentException podczas próby
         * postawienia skoczków na koordynatach przekraczających rozmiar szachownicy.
         */
        @Test
        void shouldThrowExceptionWhenPlacingKnightOutOfBounds() {
            ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);
            Position outOfBoundsPos = new Position(-1, 9);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> editor.placeKnight(outOfBoundsPos));

            assertEquals("Pozycja poza szachownicą!", exception.getMessage());
            Mockito.verifyNoInteractions(mockCalculator, mockCounter);
        }

        /**
         * Sprawdza mechanizm zabezpieczający przed ustawieniem skoczka na polu,
         * na którym znajduje się już inna figura.
         */
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

    /**
     * Zestaw testów integracyjnych weryfikujących działanie interfejsu konsolowego (CLI).
     * Symulują one wpisywanie komend przez użytkownika i analizują surowy tekst z konsoli (System.out).
     */
    @Nested
    @DisplayName("Testy CLI")
    class MainTests {

        @AfterEach
        void tearDown() {
            System.setOut(originalOut);
            Main.storage = new RealBoardStorage();
        }

        /**
         * Przechodzi przez typowy scenariusz użytkowania: wybór opcji dodania skoczka,
         * podanie współrzędnych, wyświetlenie statusu oraz zakończenie programu.
         */
        @Test
        @DisplayName("CLI: Symulacja pełnej interakcji użytkownika")
        void shouldSimulateMainInteractionAndPlaceKnight() {
            provideInput("1\n2\n6\n4\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Wstawiono skoczka"), "Brak info o wstawieniu");
            assertTrue(output.contains("X=2, Y=6"), "Skoczek nie pojawił się w podsumowaniu stanu");
        }

        /**
         * Weryfikuje zachowanie programu w głównym menu po wpisaniu tekstu zamiast numeru opcji.
         */
        @Test
        @DisplayName("CLI: Odporność na wpisanie liter zamiast liczb")
        void shouldHandleInputMismatchInMain() {
            provideInput("nie_liczba\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Wprowadź poprawną liczbę całkowitą!"), "Program powinien obsłużyć błędny typ danych");
        }

        /**
         * Symuluje pełen proces tworzenia, zapisu i odczytu stanu planszy używając menu CLI.
         * Wymaga podmiany storage'u na wariant in-memory (Stub).
         */
        @Test
        @DisplayName("CLI: Pełny cykl zapisu i odczytu przez menu (Stub)")
        void shouldSaveAndLoadViaMenuSystem() {
            Main.storage = new InMemoryStorageStub();

            String dummyPath = "testowy_zapis.json";

            provideInput("1\n1\n1\n2\n" + dummyPath + "\n0\n");
            Main.main(new String[]{});

            testOut.reset();
            provideInput("3\n" + dummyPath + "\n4\n0\n");
            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Pomyślnie załadowano planszę"), "Błąd ładowania");
            assertTrue(output.contains("X=1, Y=1"), "Skoczek zniknął po przeładowaniu");
        }

        /**
         * Testuje zachowanie programu podczas wprowadzania koordynaty X, gdy
         * wprowadzony znak nie jest liczbą całkowitą.
         */
        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej X")
        void shouldHandleGarbageInXCoordinate() {
            provideInput("1\nX\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Współrzędne muszą być liczbami!"), "Program powinien złapać błąd typu danych wewnątrz metody handlePlaceKnight");
        }

        /**
         * Testuje zachowanie programu podczas wprowadzania koordynaty Y, gdy
         * wprowadzony znak nie jest liczbą całkowitą (przy poprawnym X).
         */
        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej Y")
        void shouldHandleGarbageInYCoordinate() {
            provideInput("1\n4\nY\n0\n");

            Main.main(new String[]{});

            String output = testOut.toString();
            assertTrue(output.contains("Błąd: Współrzędne muszą być liczbami!"), "Program powinien obsłużyć błąd nawet jeśli X był poprawny");
        }

        /**
         * Sprawdza komunikaty CLI, gdy użytkownik wybierze opcję nieobecną w menu.
         */
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

    /**
     * Zestaw testów weryfikujących poprawność komunikacji między warstwą prezentacji (Main)
     * a warstwą logiki biznesowej (ChessboardEditor) z wykorzystaniem Mockito.
     * Zapobiega poleganiu na surowym tekście z konsoli, skupiając się na weryfikacji wywołanych metod.
     */
    @Nested
    @DisplayName("Testy CLI z Mockito (MockConstruction i InputReader)")
    class MainTestsMockito {

        private final PrintStream originalOut = System.out;
        private PrintStream mockOut;
        private InputReader mockReader;

        /**
         * Podmienia strumień wyjściowy oraz własny system odczytu (InputReader) na mocki.
         */
        @BeforeEach
        void setUp() {
            mockOut = Mockito.mock(PrintStream.class);
            System.setOut(mockOut);

            mockReader = Mockito.mock(InputReader.class);
            Main.setInputReader(mockReader);
        }

        @AfterEach
        void restoreStreams() {
            System.setOut(originalOut);
            Main.setInputReader(new ConsoleInputReader());
        }

        /**
         * Konstruuje sekwencję zwracanych wartości dla zamockowanego InputReadera.
         *
         * @param data Ciąg znaków (oddzielony \n) reperezentujący sekwencję operacji w menu
         */
        private void provideInput(String data) {
            String[] lines = data.split("\n");
            if (lines.length > 0) {
                if (lines.length == 1) {
                    Mockito.when(mockReader.read()).thenReturn(lines[0]);
                } else {
                    String first = lines[0];
                    String[] tail = java.util.Arrays.copyOfRange(lines, 1, lines.length);
                    Mockito.when(mockReader.read()).thenReturn(first, tail);
                }
            }
        }

        /**
         * Weryfikuje, czy przekazanie odpowiednich instrukcji do CLI powoduje
         * wywołanie metody {@code placeKnight()} na odpowiednim nowo powstałym obiekcie edytora.
         */
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

        /**
         * Sprawdza, czy odgórne interakcje w menu CLI skutkują prawidłowymi wywołaniami metod
         * odpowiedzialnych za serializację (zapis) i deserializację (odczyt).
         */
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

        /**
         * Weryfikuje wywołanie metody pobierającej szachownicę, gdy w CLI
         * zażądano wyświetlenia jej stanu.
         */
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

        /**
         * Upewnia się, że niepoprawny wybór w menu (litery zamiast liczby) nie ingeruje
         * w wewnętrzne struktury edytora (brak interakcji) i wyrzuca odpowiedni błąd do PrintStream.
         */
        @Test
        @DisplayName("CLI: Odporność na wpisanie liter zamiast liczb (główne menu)")
        void shouldHandleInputMismatchInMain() {
            provideInput("nie_liczba\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verifyNoInteractions(mockEditor);
                Mockito.verify(mockOut, Mockito.atLeastOnce()).println(Mockito.contains("Błąd: Wprowadź poprawną liczbę całkowitą!"));
            }
        }

        /**
         * Weryfikuje bezpieczne anulowanie operacji i brak interakcji z metodą {@code placeKnight()},
         * gdy podczas żądania X podano ciąg niebędący liczbą całkowitą.
         */
        @Test
        @DisplayName("CLI: Litera zamiast współrzędnej X")
        void shouldHandleGarbageInXCoordinate() {
            provideInput("1\nX\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verify(mockEditor, Mockito.never()).placeKnight(any());
                Mockito.verify(mockOut, Mockito.atLeastOnce()).println(Mockito.contains("Błąd: Współrzędne muszą być liczbami!"));
            }
        }

        /**
         * Zapewnia, że wybór nieobsługiwanego numeru z menu wyprowadzi bezpieczny komunikat
         * błędu na PrintStream bez uszkadzania stanu edytora.
         */
        @Test
        @DisplayName("CLI: Wybór nieistniejącej opcji menu")
        void shouldHandleInvalidMenuChoice() {
            provideInput("99\n0\n");

            try (MockedConstruction<ChessboardEditor> mocked = Mockito.mockConstruction(ChessboardEditor.class)) {
                Main.main(new String[]{});

                ChessboardEditor mockEditor = mocked.constructed().getFirst();

                Mockito.verifyNoInteractions(mockEditor);
                Mockito.verify(mockOut, Mockito.atLeastOnce()).println(Mockito.contains("Nieznana opcja. Spróbuj ponownie."));
            }
        }
    }
}