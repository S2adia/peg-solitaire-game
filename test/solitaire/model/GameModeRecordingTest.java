package solitaire.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import solitaire.model.board.Board;
import solitaire.model.board.BoardFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameModeRecordingTest {

    // ---- Board.loadState() ----

    @Test
    @DisplayName("Board.loadState() replaces grid contents with the given state")
    void boardLoadState() {
        Board b = BoardFactory.create("English", 5);
        int initial = b.pegCount();

        // Remove one peg from a copy of the state
        Cell[][] state = b.getGrid();
        outer:
        for (Cell[] row : state)
            for (int c = 0; c < row.length; c++)
                if (row[c] == Cell.PEG) { row[c] = Cell.HOLE; break outer; }

        b.loadState(state);
        assertEquals(initial - 1, b.pegCount());
    }

    // ---- GameMode.applyMove() ----

    @Test
    @DisplayName("GameMode.applyMove() executes the move on the board and pushes to history")
    void gameModeApplyMove() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        int before = mode.getBoard().pegCount();
        Move m = mode.getBoard().validMoves().get(0);

        mode.applyMove(m);

        assertEquals(before - 1, mode.getBoard().pegCount());
        assertEquals(m, mode.getHistory().peek());
    }

    // ---- GameMode.loadBoardState() ----

    @Test
    @DisplayName("GameMode.loadBoardState() restores board to given state and stays PLAYING")
    void gameModeLoadBoardState() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 5);

        // Build a snapshot with 2 pegs removed
        Board tmp = BoardFactory.create("English", 5);
        tmp.apply(tmp.validMoves().get(0));
        tmp.apply(tmp.validMoves().get(0));
        Cell[][] snapshot = tmp.getGrid();

        mode.loadBoardState(snapshot);

        assertEquals(tmp.pegCount(), mode.getBoard().pegCount());
        assertEquals(GameStatus.PLAYING, mode.getStatus());
    }

    // ---- setRecording / getRecord ----

    @Test
    @DisplayName("getRecord() returns null when recording is not enabled")
    void noRecordByDefault() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        assertNull(mode.getRecord());
    }

    @Test
    @DisplayName("setRecording(true) creates a GameRecord with correct header")
    void setRecordingCreatesRecord() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);
        GameRecord r = mode.getRecord();
        assertNotNull(r);
        assertEquals("English", r.getBoardType());
        assertEquals(7,          r.getBoardSize());
        assertEquals("Manual",   r.getGameMode());
    }

    @Test
    @DisplayName("manual mode records MOVE events when recording is enabled")
    void manualRecordsMoves() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);

        Move m = mode.getBoard().validMoves().get(0);
        mode.handleCellClick(m.origin()[0], m.origin()[1]);
        mode.handleCellClick(m.destination()[0], m.destination()[1]);

        GameRecord r = mode.getRecord();
        assertEquals(1, r.getEvents().size());
        assertEquals(GameRecord.EventType.MOVE, r.getEvents().get(0).type());
        assertArrayEquals(m.origin(), r.getEvents().get(0).move().origin());
    }

    @Test
    @DisplayName("manual mode records each randomize step as a MOVE event")
    void manualRecordsRandomize() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);
        int initialPegCount = mode.getBoard().pegCount();
        mode.randomize();

        GameRecord r = mode.getRecord();
        assertFalse(r.getEvents().isEmpty());
        assertTrue(r.getEvents().stream()
            .allMatch(event -> event.type() == GameRecord.EventType.MOVE));
        assertEquals(initialPegCount - r.getEvents().size(), mode.getBoard().pegCount());
    }

    @Test
    @DisplayName("manual randomize saves explicit MOVE lines to the recording file")
    void manualRandomizeSavesMoveSteps(@TempDir Path dir) throws IOException {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);
        mode.randomize();

        Path file = dir.resolve("randomize.txt");
        mode.getRecord().save(file);

        assertTrue(Files.readAllLines(file).stream().skip(1)
            .allMatch(line -> line.startsWith("MOVE ")));
    }

    @Test
    @DisplayName("automated mode records MOVE events when recording is enabled")
    void automatedRecordsMoves() {
        AutomatedGameMode mode = new AutomatedGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);
        mode.autoplay();

        GameRecord r = mode.getRecord();
        assertNotNull(r);
        assertEquals(1, r.getEvents().size());
        assertEquals(GameRecord.EventType.MOVE, r.getEvents().get(0).type());
    }

    @Test
    @DisplayName("newGame() resets the record when recording stays enabled")
    void newGameResetsRecord() {
        ManualGameMode mode = new ManualGameMode();
        mode.newGame("English", 7);
        mode.setRecording(true);
        // make one move so record has an event
        Move m = mode.getBoard().validMoves().get(0);
        mode.handleCellClick(m.origin()[0], m.origin()[1]);
        mode.handleCellClick(m.destination()[0], m.destination()[1]);

        // start a new game while recording is still active
        mode.newGame("Diamond", 5);

        GameRecord r = mode.getRecord();
        assertNotNull(r);
        assertEquals("Diamond", r.getBoardType());
        assertEquals(0, r.getEvents().size());
    }
}
