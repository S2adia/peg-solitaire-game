package solitaire.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import solitaire.model.board.Board;
import solitaire.model.board.BoardFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRecordTest {

    @Test
    @DisplayName("stores board configuration in header fields")
    void storesBoardConfig() {
        GameRecord r = new GameRecord("English", 7, "Manual");
        assertEquals("English", r.getBoardType());
        assertEquals(7, r.getBoardSize());
        assertEquals("Manual", r.getGameMode());
    }

    @Test
    @DisplayName("starts with empty events list")
    void startsEmpty() {
        assertTrue(new GameRecord("English", 7, "Manual").getEvents().isEmpty());
    }

    @Test
    @DisplayName("addMove appends a MOVE event with the correct move")
    void addMoveAppendsEvent() {
        GameRecord r = new GameRecord("English", 7, "Manual");
        Move m = Move.of(2, 3, 3, 3, 4, 3);
        r.addMove(m);
        assertEquals(1, r.getEvents().size());
        assertEquals(GameRecord.EventType.MOVE, r.getEvents().get(0).type());
        assertArrayEquals(new int[]{2, 3}, r.getEvents().get(0).move().origin());
        assertArrayEquals(new int[]{3, 3}, r.getEvents().get(0).move().jumped());
        assertArrayEquals(new int[]{4, 3}, r.getEvents().get(0).move().destination());
    }

    @Test
    @DisplayName("addRandomize appends a RANDOMIZE event with a deep-copied board state")
    void addRandomizeAppendsEvent() {
        GameRecord r = new GameRecord("English", 5, "Manual");
        Board board = BoardFactory.create("English", 5);
        r.addRandomize(board.getGrid());

        assertEquals(1, r.getEvents().size());
        assertEquals(GameRecord.EventType.RANDOMIZE, r.getEvents().get(0).type());
        assertNotNull(r.getEvents().get(0).boardState());
    }

    @Test
    @DisplayName("save() writes correct header line")
    void savesHeader(@TempDir Path dir) throws IOException {
        GameRecord r = new GameRecord("Diamond", 5, "Automated");
        Path f = dir.resolve("g.txt");
        r.save(f);
        assertEquals("Diamond 5 Automated", Files.readAllLines(f).get(0));
    }

    @Test
    @DisplayName("save() writes MOVE event as 'MOVE r0 c0 rj cj rd cd'")
    void savesMoveEvent(@TempDir Path dir) throws IOException {
        GameRecord r = new GameRecord("English", 7, "Manual");
        r.addMove(Move.of(2, 3, 3, 3, 4, 3));
        Path f = dir.resolve("g.txt");
        r.save(f);
        List<String> lines = Files.readAllLines(f);
        assertEquals("MOVE 2 3 3 3 4 3", lines.get(1));
    }

    @Test
    @DisplayName("save() writes RANDOMIZE event starting with 'RANDOMIZE ' followed by size*size tokens")
    void savesRandomizeEvent(@TempDir Path dir) throws IOException {
        GameRecord r = new GameRecord("English", 5, "Manual");
        Board board = BoardFactory.create("English", 5);
        r.addRandomize(board.getGrid());
        Path f = dir.resolve("g.txt");
        r.save(f);
        List<String> lines = Files.readAllLines(f);
        assertTrue(lines.get(1).startsWith("RANDOMIZE "));
        String[] parts = lines.get(1).split(" ");
        assertEquals(1 + 5 * 5, parts.length); // "RANDOMIZE" + 25 cell tokens
    }

    @Test
    @DisplayName("load() restores header fields")
    void loadsHeader(@TempDir Path dir) throws IOException {
        GameRecord original = new GameRecord("Hexagon", 5, "Automated");
        Path f = dir.resolve("g.txt");
        original.save(f);
        GameRecord loaded = GameRecord.load(f);
        assertEquals("Hexagon",    loaded.getBoardType());
        assertEquals(5,             loaded.getBoardSize());
        assertEquals("Automated",  loaded.getGameMode());
    }

    @Test
    @DisplayName("load() round-trips MOVE events with correct coordinates")
    void roundTripMoves(@TempDir Path dir) throws IOException {
        GameRecord original = new GameRecord("English", 7, "Manual");
        original.addMove(Move.of(2, 3, 3, 3, 4, 3));
        original.addMove(Move.of(4, 5, 4, 4, 4, 3));
        Path f = dir.resolve("g.txt");
        original.save(f);

        GameRecord loaded = GameRecord.load(f);
        assertEquals(2, loaded.getEvents().size());

        GameRecord.Event e0 = loaded.getEvents().get(0);
        assertEquals(GameRecord.EventType.MOVE, e0.type());
        assertArrayEquals(new int[]{2, 3}, e0.move().origin());
        assertArrayEquals(new int[]{3, 3}, e0.move().jumped());
        assertArrayEquals(new int[]{4, 3}, e0.move().destination());

        GameRecord.Event e1 = loaded.getEvents().get(1);
        assertArrayEquals(new int[]{4, 5}, e1.move().origin());
        assertArrayEquals(new int[]{4, 4}, e1.move().jumped());
        assertArrayEquals(new int[]{4, 3}, e1.move().destination());
    }

    @Test
    @DisplayName("load() round-trips RANDOMIZE events with identical grid")
    void roundTripRandomize(@TempDir Path dir) throws IOException {
        GameRecord original = new GameRecord("English", 5, "Manual");
        Board board = BoardFactory.create("English", 5);
        board.apply(board.validMoves().get(0));
        original.addRandomize(board.getGrid());
        Path f = dir.resolve("g.txt");
        original.save(f);

        GameRecord loaded = GameRecord.load(f);
        assertEquals(1, loaded.getEvents().size());
        assertEquals(GameRecord.EventType.RANDOMIZE, loaded.getEvents().get(0).type());

        Cell[][] orig   = original.getEvents().get(0).boardState();
        Cell[][] result = loaded.getEvents().get(0).boardState();
        assertEquals(orig.length, result.length);
        for (int r = 0; r < orig.length; r++)
            assertArrayEquals(orig[r], result[r], "row " + r + " differs");
    }

    @Test
    @DisplayName("load() handles mixed MOVE and RANDOMIZE events in order")
    void roundTripMixed(@TempDir Path dir) throws IOException {
        GameRecord original = new GameRecord("English", 5, "Manual");
        original.addMove(Move.of(1, 2, 2, 2, 3, 2));
        Board board = BoardFactory.create("English", 5);
        original.addRandomize(board.getGrid());
        original.addMove(Move.of(3, 0, 3, 1, 3, 2));
        Path f = dir.resolve("g.txt");
        original.save(f);

        GameRecord loaded = GameRecord.load(f);
        assertEquals(3, loaded.getEvents().size());
        assertEquals(GameRecord.EventType.MOVE,      loaded.getEvents().get(0).type());
        assertEquals(GameRecord.EventType.RANDOMIZE, loaded.getEvents().get(1).type());
        assertEquals(GameRecord.EventType.MOVE,      loaded.getEvents().get(2).type());
    }
}
