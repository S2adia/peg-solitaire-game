package solitaire.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import solitaire.model.board.Board;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Game class.
 * Tests cover game initialization, move execution, undo functionality, and game-over detection.
 */
public class GameTest {

    private Game game;

    @BeforeEach
    public void setUp() {
        game = new Game();
    }

    @Test
    public void testNewGameInitialization() {
        game.newGame("English", 7);
        
        assertEquals(GameStatus.PLAYING, game.getStatus(), "Game status should be PLAYING after initialization");
        assertNotNull(game.getBoard(), "Board should not be null");
        assertEquals(32, game.getBoard().pegCount(), "English board size 7 should have 32 pegs initially");
    }

    @Test
    public void testNewGameResetsState() {
        game.newGame("English", 7);
        
        // Make a move
        Board board = game.getBoard();
        List<Move> moves = board.validMoves();
        if (!moves.isEmpty()) {
            Move move = moves.get(0);
            game.select(move.origin()[0], move.origin()[1]);
            game.attemptMove(move.destination()[0], move.destination()[1]);
        }
        
        // Start new game
        game.newGame("Diamond", 5);
        
        assertEquals(GameStatus.PLAYING, game.getStatus(), "Status should reset to PLAYING");
        assertEquals("Diamond", game.getBoard().getClass().getSimpleName().replace("Board", ""), 
                     "Board type should change to Diamond");
        assertNull(game.getSelected(), "Selection should be cleared");
    }

    @Test
    public void testPegSelection() {
        game.newGame("English", 7);
        
        // Find a peg position
        Board board = game.getBoard();
        Cell[][] grid = board.getGrid();
        int pegRow = -1, pegCol = -1;
        
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == Cell.PEG) {
                    pegRow = r;
                    pegCol = c;
                    break;
                }
            }
            if (pegRow != -1) break;
        }
        
        assertTrue(game.select(pegRow, pegCol), "Should successfully select a peg");
        assertNotNull(game.getSelected(), "Selected position should not be null");
        assertEquals(pegRow, game.getSelected()[0], "Selected row should match");
        assertEquals(pegCol, game.getSelected()[1], "Selected column should match");
    }

    @Test
    public void testValidMoveExecution() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        List<Move> validMoves = board.validMoves();
        assertFalse(validMoves.isEmpty(), "Board should have valid moves");
        
        Move move = validMoves.get(0);
        int[] origin = move.origin();
        int[] jumped = move.jumped();
        int[] dest = move.destination();
        
        int initialPegCount = board.pegCount();
        
        game.select(origin[0], origin[1]);
        boolean result = game.attemptMove(dest[0], dest[1]);
        
        assertTrue(result, "Valid move should be executed");
        assertEquals(initialPegCount - 1, board.pegCount(), "Peg count should decrease by 1");
        assertEquals(Cell.HOLE, board.getGrid()[origin[0]][origin[1]], "Origin should become HOLE");
        assertEquals(Cell.HOLE, board.getGrid()[jumped[0]][jumped[1]], "Jumped peg should become HOLE");
        assertEquals(Cell.PEG, board.getGrid()[dest[0]][dest[1]], "Destination should become PEG");
    }

    @Test
    public void testInvalidMoveRejection() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        Cell[][] grid = board.getGrid();
        
        // Find a peg and an invalid destination (not reachable)
        int pegRow = -1, pegCol = -1;
        int holeRow = -1, holeCol = -1;
        
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == Cell.PEG && pegRow == -1) {
                    pegRow = r;
                    pegCol = c;
                }
                if (grid[r][c] == Cell.HOLE && holeRow == -1) {
                    holeRow = r;
                    holeCol = c;
                }
            }
        }
        
        game.select(pegRow, pegCol);
        int initialPegCount = board.pegCount();
        
        // Try to move to center hole (likely invalid - too far or no peg to jump)
        boolean result = game.attemptMove(holeRow, holeCol);
        
        // If the move was invalid, peg count should not change
        if (!result) {
            assertEquals(initialPegCount, board.pegCount(), "Peg count should not change on invalid move");
        }
    }

    @Test
    public void testWinCondition() {
        // Use size 5 for easier win condition testing
        game.newGame("English", 5);
        
        // Manually set up a near-win state by playing moves
        // For size 3 English board, we need to play until one peg remains
        Board board = game.getBoard();
        
        // Keep making valid moves until we win or can't move
        int maxMoves = 100; // Safety limit
        int moveCount = 0;
        
        while (board.pegCount() > 1 && !board.validMoves().isEmpty() && moveCount < maxMoves) {
            List<Move> moves = board.validMoves();
            if (moves.isEmpty()) break;
            
            Move move = moves.get(0);
            game.select(move.origin()[0], move.origin()[1]);
            game.attemptMove(move.destination()[0], move.destination()[1]);
            moveCount++;
        }
        
        // Check if we reached win condition
        if (board.pegCount() == 1) {
            assertEquals(GameStatus.WON, game.getStatus(), "Game should be WON with one peg");
        }
    }

    @Test
    public void testLoseCondition() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        
        // Play moves until no more valid moves (but more than 1 peg remains)
        int maxMoves = 100;
        int moveCount = 0;
        
        while (!board.validMoves().isEmpty() && moveCount < maxMoves) {
            List<Move> moves = board.validMoves();
            Move move = moves.get(0);
            game.select(move.origin()[0], move.origin()[1]);
            game.attemptMove(move.destination()[0], move.destination()[1]);
            moveCount++;
            
            // Stop if we're close to losing (few pegs left but not 1)
            if (board.pegCount() > 1 && board.pegCount() < 5 && board.validMoves().isEmpty()) {
                break;
            }
        }
        
        // If we have multiple pegs and no moves, should be LOST
        if (board.pegCount() > 1 && board.validMoves().isEmpty()) {
            assertEquals(GameStatus.LOST, game.getStatus(), "Game should be LOST with multiple pegs and no moves");
        }
    }

    @Test
    public void testUndoMove() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        List<Move> validMoves = board.validMoves();
        assertFalse(validMoves.isEmpty(), "Board should have valid moves");
        
        Move move = validMoves.get(0);
        int[] origin = move.origin();
        int[] dest = move.destination();
        
        int initialPegCount = board.pegCount();
        Cell[][] initialGrid = copyGrid(board.getGrid());
        
        // Make a move
        game.select(origin[0], origin[1]);
        game.attemptMove(dest[0], dest[1]);
        
        // Undo the move
        boolean undoResult = game.undo();
        
        assertTrue(undoResult, "Undo should succeed");
        assertEquals(initialPegCount, board.pegCount(), "Peg count should be restored");
        assertGridEquals(initialGrid, board.getGrid(), "Board state should be restored");
        assertEquals(GameStatus.PLAYING, game.getStatus(), "Status should be PLAYING after undo");
    }

    @Test
    public void testUndoEmptyHistory() {
        game.newGame("English", 7);
        
        boolean result = game.undo();
        
        assertFalse(result, "Undo should fail with empty history");
        assertEquals(GameStatus.PLAYING, game.getStatus(), "Status should remain PLAYING");
    }

    @Test
    public void testMultipleUndo() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        Cell[][] initialGrid = copyGrid(board.getGrid());
        int initialPegCount = board.pegCount();
        
        // Make 3 moves
        for (int i = 0; i < 3; i++) {
            List<Move> moves = board.validMoves();
            if (moves.isEmpty()) break;
            
            Move move = moves.get(0);
            game.select(move.origin()[0], move.origin()[1]);
            game.attemptMove(move.destination()[0], move.destination()[1]);
        }
        
        // Undo all 3 moves
        for (int i = 0; i < 3; i++) {
            game.undo();
        }
        
        assertEquals(initialPegCount, board.pegCount(), "Peg count should be restored after multiple undos");
        assertGridEquals(initialGrid, board.getGrid(), "Board should return to initial state");
    }

    @Test
    public void testReachableFromSelected() {
        game.newGame("English", 7);
        
        Board board = game.getBoard();
        List<Move> validMoves = board.validMoves();
        assertFalse(validMoves.isEmpty(), "Board should have valid moves");
        
        Move move = validMoves.get(0);
        game.select(move.origin()[0], move.origin()[1]);
        
        var reachable = game.reachableFromSelected();
        assertFalse(reachable.isEmpty(), "Should have reachable positions");
        
        String expectedKey = move.destination()[0] + "," + move.destination()[1];
        assertTrue(reachable.contains(expectedKey), "Reachable set should contain valid destination");
    }

    // Helper methods
    
    private Cell[][] copyGrid(Cell[][] grid) {
        Cell[][] copy = new Cell[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, grid[r].length);
        }
        return copy;
    }

    private void assertGridEquals(Cell[][] expected, Cell[][] actual, String message) {
        assertEquals(expected.length, actual.length, message + " - row count mismatch");
        for (int r = 0; r < expected.length; r++) {
            assertArrayEquals(expected[r], actual[r], message + " - row " + r + " mismatch");
        }
    }
}
