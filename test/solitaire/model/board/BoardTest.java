package solitaire.model.board;

import org.junit.jupiter.api.Test;
import solitaire.model.Cell;
import solitaire.model.Move;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Board classes.
 * Tests board creation, move validation, and board-specific behavior.
 */
public class BoardTest {

    @Test
    public void testBoardFactoryEnglish() {
        Board board = BoardFactory.create("English", 7);
        
        assertNotNull(board, "Board should not be null");
        assertEquals("EnglishBoard", board.getClass().getSimpleName(), "Should create EnglishBoard");
        assertEquals(7, board.getSize(), "Board size should be 7");
    }

    @Test
    public void testBoardFactoryDiamond() {
        Board board = BoardFactory.create("Diamond", 5);
        
        assertNotNull(board, "Board should not be null");
        assertEquals("DiamondBoard", board.getClass().getSimpleName(), "Should create DiamondBoard");
        assertEquals(5, board.getSize(), "Board size should be 5");
    }

    @Test
    public void testBoardFactoryHexagon() {
        Board board = BoardFactory.create("Hexagon", 7);
        
        assertNotNull(board, "Board should not be null");
        assertEquals("HexagonBoard", board.getClass().getSimpleName(), "Should create HexagonBoard");
        assertEquals(7, board.getSize(), "Board size should be 7");
    }

    @Test
    public void testBoardFactoryInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            BoardFactory.create("Triangle", 7);
        }, "Should throw exception for invalid board type");
    }

    @Test
    public void testBoardSizeValidation() {
        // Test minimum size (too small)
        assertThrows(IllegalArgumentException.class, () -> {
            BoardFactory.create("English", 3);
        }, "Should throw exception for size < 5");
        
        // Test even size (invalid)
        assertThrows(IllegalArgumentException.class, () -> {
            BoardFactory.create("English", 6);
        }, "Should throw exception for even size");
        
        // Test maximum size (too large)
        assertThrows(IllegalArgumentException.class, () -> {
            BoardFactory.create("English", 11);
        }, "Should throw exception for size > 9");
        
        // Test valid sizes
        assertDoesNotThrow(() -> BoardFactory.create("English", 5), "Size 5 should be valid");
        assertDoesNotThrow(() -> BoardFactory.create("English", 7), "Size 7 should be valid");
        assertDoesNotThrow(() -> BoardFactory.create("English", 9), "Size 9 should be valid");
    }

    @Test
    public void testEnglishBoardShape() {
        Board board = BoardFactory.create("English", 7);
        Cell[][] grid = board.getGrid();
        
        // Check corners are EMPTY
        assertEquals(Cell.EMPTY, grid[0][0], "Top-left corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[0][6], "Top-right corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[6][0], "Bottom-left corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[6][6], "Bottom-right corner should be EMPTY");
        
        // Check center is HOLE
        assertEquals(Cell.HOLE, grid[3][3], "Center should be HOLE");
        
        // Check cross arms have PEGs
        assertEquals(Cell.PEG, grid[0][3], "Top arm should have PEG");
        assertEquals(Cell.PEG, grid[3][0], "Left arm should have PEG");
        assertEquals(Cell.PEG, grid[3][6], "Right arm should have PEG");
        assertEquals(Cell.PEG, grid[6][3], "Bottom arm should have PEG");
    }

    @Test
    public void testDiamondBoardShape() {
        Board board = BoardFactory.create("Diamond", 5);
        Cell[][] grid = board.getGrid();
        
        // Check corners are EMPTY
        assertEquals(Cell.EMPTY, grid[0][0], "Top-left corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[0][4], "Top-right corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[4][0], "Bottom-left corner should be EMPTY");
        assertEquals(Cell.EMPTY, grid[4][4], "Bottom-right corner should be EMPTY");
        
        // Check center is HOLE
        assertEquals(Cell.HOLE, grid[2][2], "Center should be HOLE");
        
        // Check diamond edges have PEGs
        assertEquals(Cell.PEG, grid[0][2], "Top edge should have PEG");
        assertEquals(Cell.PEG, grid[2][0], "Left edge should have PEG");
        assertEquals(Cell.PEG, grid[2][4], "Right edge should have PEG");
        assertEquals(Cell.PEG, grid[4][2], "Bottom edge should have PEG");
    }

    @Test
    public void testHexagonBoardShape() {
        Board board = BoardFactory.create("Hexagon", 5);
        Cell[][] grid = board.getGrid();
        
        // Check center is HOLE
        assertEquals(Cell.HOLE, grid[2][2], "Center should be HOLE");
        
        // Hexagon should have more playable cells than diamond
        int playableCount = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] != Cell.EMPTY) {
                    playableCount++;
                }
            }
        }
        
        assertTrue(playableCount > 0, "Hexagon should have playable cells");
    }

    @Test
    public void testInitialPegCount() {
        Board englishBoard = BoardFactory.create("English", 7);
        assertEquals(32, englishBoard.pegCount(), "English board size 7 should have 32 pegs");
        
        Board diamondBoard = BoardFactory.create("Diamond", 5);
        assertEquals(12, diamondBoard.pegCount(), "Diamond board size 5 should have 12 pegs");
    }

    @Test
    public void testValidMovesNotEmpty() {
        Board board = BoardFactory.create("English", 7);
        List<Move> moves = board.validMoves();
        
        assertFalse(moves.isEmpty(), "Initial board should have valid moves");
        assertTrue(moves.size() > 0, "Should have at least one valid move");
    }

    @Test
    public void testValidMoveStructure() {
        Board board = BoardFactory.create("English", 7);
        List<Move> moves = board.validMoves();
        
        Move move = moves.get(0);
        assertNotNull(move.origin(), "Move should have origin");
        assertNotNull(move.jumped(), "Move should have jumped position");
        assertNotNull(move.destination(), "Move should have destination");
        
        assertEquals(2, move.origin().length, "Origin should have 2 coordinates");
        assertEquals(2, move.jumped().length, "Jumped should have 2 coordinates");
        assertEquals(2, move.destination().length, "Destination should have 2 coordinates");
    }

    @Test
    public void testApplyMove() {
        Board board = BoardFactory.create("English", 7);
        List<Move> moves = board.validMoves();
        Move move = moves.get(0);
        
        int initialPegCount = board.pegCount();
        Cell[][] grid = board.getGrid();
        
        // Verify initial state
        assertEquals(Cell.PEG, grid[move.origin()[0]][move.origin()[1]], "Origin should have PEG");
        assertEquals(Cell.PEG, grid[move.jumped()[0]][move.jumped()[1]], "Jumped should have PEG");
        assertEquals(Cell.HOLE, grid[move.destination()[0]][move.destination()[1]], "Destination should have HOLE");
        
        board.apply(move);
        
        // Verify state after move
        assertEquals(Cell.HOLE, grid[move.origin()[0]][move.origin()[1]], "Origin should become HOLE");
        assertEquals(Cell.HOLE, grid[move.jumped()[0]][move.jumped()[1]], "Jumped should become HOLE");
        assertEquals(Cell.PEG, grid[move.destination()[0]][move.destination()[1]], "Destination should become PEG");
        assertEquals(initialPegCount - 1, board.pegCount(), "Peg count should decrease by 1");
    }

    @Test
    public void testReverseMove() {
        Board board = BoardFactory.create("English", 7);
        List<Move> moves = board.validMoves();
        Move move = moves.get(0);
        
        Cell[][] gridBefore = copyGrid(board.getGrid());
        int pegCountBefore = board.pegCount();
        
        board.apply(move);
        board.reverse(move);
        
        Cell[][] gridAfter = board.getGrid();
        
        // Verify state is restored
        assertGridEquals(gridBefore, gridAfter, "Grid should be restored after reverse");
        assertEquals(pegCountBefore, board.pegCount(), "Peg count should be restored");
    }

    @Test
    public void testMoveRoundTrip() {
        Board board = BoardFactory.create("English", 7);
        List<Move> moves = board.validMoves();
        
        for (int i = 0; i < Math.min(5, moves.size()); i++) {
            Move move = moves.get(i);
            Cell[][] gridBefore = copyGrid(board.getGrid());
            int pegCountBefore = board.pegCount();
            
            board.apply(move);
            board.reverse(move);
            
            assertGridEquals(gridBefore, board.getGrid(), "Round trip should restore state for move " + i);
            assertEquals(pegCountBefore, board.pegCount(), "Peg count should be restored for move " + i);
        }
    }

    @Test
    public void testEnglishBoardDirections() {
        Board board = BoardFactory.create("English", 7);
        List<int[]> directions = board.directions();
        
        assertEquals(4, directions.size(), "English board should have 4 directions");
    }

    @Test
    public void testHexagonBoardDirections() {
        Board board = BoardFactory.create("Hexagon", 7);
        List<int[]> directions = board.directions();
        
        assertEquals(6, directions.size(), "Hexagon board should have 6 directions");
    }

    @Test
    public void testIsPlayable() {
        Board board = BoardFactory.create("English", 7);
        
        // Test playable positions
        assertTrue(board.isPlayable(3, 3), "Center should be playable");
        assertTrue(board.isPlayable(0, 3), "Top arm should be playable");
        
        // Test non-playable positions
        assertFalse(board.isPlayable(0, 0), "Corner should not be playable");
        assertFalse(board.isPlayable(-1, 0), "Negative index should not be playable");
        assertFalse(board.isPlayable(10, 10), "Out of bounds should not be playable");
    }

    @Test
    public void testBoardSizeRange() {
        // Test all valid odd sizes
        for (int size = 5; size <= 9; size += 2) {
            Board board = BoardFactory.create("English", size);
            assertNotNull(board, "Should create board for size " + size);
            assertEquals(size, board.getSize(), "Board size should match for size " + size);
        }
    }

    @Test
    public void testAllBoardTypes() {
        for (String type : BoardFactory.TYPES) {
            Board board = BoardFactory.create(type, 7);
            assertNotNull(board, "Should create board for type " + type);
            assertTrue(board.pegCount() > 0, "Board should have pegs for type " + type);
            assertFalse(board.validMoves().isEmpty(), "Board should have valid moves for type " + type);
        }
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
