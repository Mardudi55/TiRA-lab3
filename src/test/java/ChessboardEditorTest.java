import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ChessboardEditorTest {

    private Chessboard board;

    @BeforeEach
    void setUp() {
        board = new Chessboard(8);
    }

    @Test
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

    @Test
    void shouldThrowExceptionOnInvalidFilePath() {
        AttackCalculator mockCalculator = Mockito.mock(AttackCalculator.class);
        AttackCounter mockCounter = Mockito.mock(AttackCounter.class);
        ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);

        String invalidPath = "/katalog_ktory_nie_istnieje/plik.json";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> editor.saveToFile(invalidPath));
        assertTrue(exception.getMessage().contains("Błąd podczas zapisu pliku"));
    }

    @Test
    void shouldThrowExceptionWhenPlacingKnightOutOfBounds() {
        AttackCalculator mockCalculator = Mockito.mock(AttackCalculator.class);
        AttackCounter mockCounter = Mockito.mock(AttackCounter.class);
        ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);

        Position outOfBoundsPos = new Position(-1, 9);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> editor.placeKnight(outOfBoundsPos));

        assertEquals("Pozycja poza szachownicą!", exception.getMessage());

        Mockito.verifyNoInteractions(mockCalculator, mockCounter);
    }

    @Test
    void shouldThrowExceptionWhenPlacingKnightOnOccupiedField() {
        AttackCalculator mockCalculator = Mockito.mock(AttackCalculator.class);
        AttackCounter mockCounter = Mockito.mock(AttackCounter.class);

        Mockito.when(mockCalculator.calculateAttack(any(), eq(board))).thenReturn(List.of());
        Mockito.when(mockCounter.count(any(), eq(board))).thenReturn(0);

        ChessboardEditor editor = new ChessboardEditor(board, mockCalculator, mockCounter);

        Position pos = new Position(3, 3);
        editor.placeKnight(pos);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> editor.placeKnight(pos));

        assertEquals("Pole jest już zajęte!", exception.getMessage());

        Mockito.verify(mockCalculator, Mockito.times(1)).calculateAttack(any(), eq(board));
    }
}