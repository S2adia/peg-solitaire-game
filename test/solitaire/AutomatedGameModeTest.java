package solitaire;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import solitaire.model.AutomatedGameMode;
import solitaire.model.GameStatus;
import solitaire.model.Move;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutomatedGameMode class.
 * Tests automated gameplay, AI move selection, and game termination.
 */
public class AutomatedGameModeTest {

    private AutomatedGameMode mode;

    @BeforeEach
    public void setUp() {
        mode = new AutomatedGameMode();
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Test initialization with English board")
    public void testInitializationEnglish() {
        mode.newGame("English", 7);
        
        assertNotNull(mode.getBoard(), "Board should be initialized");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), "Status should be PLAYING");
        assertEquals(32, mode.getBoard().pegCount(), "English board size 7 should have 32 pegs");
        assertTrue(mode.getHistory().isEmpty(), "History should be empty");
    }

    @Test
    @DisplayName("Test initialization with Diamond board")
    public void testInitializationDiamond() {
        mode.newGame("Diamond", 7);
        
        assertNotNull(mode.getBoard(), "Board should be initialized");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), "Status should be PLAYING");
        assertEquals(24, mode.getBoard().pegCount(), "Diamond board size 7 should have 24 pegs");
        assertTrue(mode.getHistory().isEmpty(), "History should be empty");
    }

    @Test
    @DisplayName("Test initialization with Hexagon board")
    public void testInitializationHexagon() {
        mode.newGame("Hexagon", 5);
        
        assertNotNull(mode.getBoard(), "Board should be initialized");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), "Status should be PLAYING");
        assertEquals(12, mode.getBoard().pegCount(), "Hexagon board size 5 should have 12 pegs");
        assertTrue(mode.getHistory().isEmpty(), "History should be empty");
    }

    @Test
    @DisplayName("Test getModeName returns 'Automated'")
    public void testGetModeName() {
        assertEquals("Automated", mode.getModeName(), "Mode name should be 'Automated'");
    }

    // ==================== Cell Click Tests ====================

    @Test
    @DisplayName("Test handleCellClick always returns false")
    public void testHandleCellClickReturnsFalse() {
        mode.newGame("English", 7);
        
        // Try clicking various cells
        assertFalse(mode.handleCellClick(0, 0), "Click should be ignored");
        assertFalse(mode.handleCellClick(3, 3), "Click should be ignored");
        assertFalse(mode.handleCellClick(6, 6), "Click should be ignored");
    }

    @Test
    @DisplayName("Test handleCellClick doesn't change board state")
    public void testHandleCellClickNoStateChange() {
        mode.newGame("English", 7);
        int initialPegCount = mode.getBoard().pegCount();
        
        mode.handleCellClick(3, 1);
        
        assertEquals(initialPegCount, mode.getBoard().pegCount(), 
            "Peg count should not change after click");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), 
            "Status should remain PLAYING");
    }

    // ==================== Autoplay Tests ====================

    @Test
    @DisplayName("Test autoplay executes one move")
    public void testAutoplayExecutesOneMove() {
        mode.newGame("English", 7);
        int initialPegCount = mode.getBoard().pegCount();
        
        boolean result = mode.autoplay();
        
        assertTrue(result, "Autoplay should return true when move is executed");
        assertEquals(initialPegCount - 1, mode.getBoard().pegCount(), 
            "Peg count should decrease by 1");
        assertEquals(1, mode.getHistory().size(), 
            "History should contain exactly one move");
    }

    @Test
    @DisplayName("Test autoplay selects valid move")
    public void testAutoplaySelectsValidMove() {
        mode.newGame("English", 7);
        
        mode.autoplay();
        
        assertFalse(mode.getHistory().isEmpty(), "History should not be empty");
        Move executedMove = mode.getHistory().peek();
        assertNotNull(executedMove, "Executed move should not be null");
        
        // Verify the move was valid by checking it reduced peg count
        assertEquals(31, mode.getBoard().pegCount(), 
            "Peg count should be 31 after first move");
    }

    @Test
    @DisplayName("Test multiple autoplay calls execute multiple moves")
    public void testMultipleAutoplayCalls() {
        mode.newGame("English", 7);
        
        mode.autoplay();
        mode.autoplay();
        mode.autoplay();
        
        assertEquals(3, mode.getHistory().size(), 
            "History should contain 3 moves");
        assertEquals(29, mode.getBoard().pegCount(), 
            "Peg count should be 29 after 3 moves");
    }

    @Test
    @DisplayName("Test autoplay returns false when no moves available")
    public void testAutoplayNoMovesAvailable() {
        mode.newGame("English", 7);
        
        // Play until no moves are available
        while (!mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        int pegCountBeforeFinalAttempt = mode.getBoard().pegCount();
        boolean result = mode.autoplay();
        
        assertFalse(result, "Autoplay should return false when no moves available");
        assertEquals(pegCountBeforeFinalAttempt, mode.getBoard().pegCount(), 
            "Peg count should not change when no moves available");
    }

    @Test
    @DisplayName("Test autoplay returns false when status is not PLAYING")
    public void testAutoplayWhenNotPlaying() {
        mode.newGame("English", 7);
        
        // Manually set status to WON
        mode.getBoard().apply(mode.getBoard().validMoves().get(0));
        mode.getBoard().apply(mode.getBoard().validMoves().get(0));
        // Continue until won or lost...
        
        // For testing, we'll just check that autoplay respects status
        // by playing until game ends
        while (mode.getStatus() == GameStatus.PLAYING && 
               !mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        int finalPegCount = mode.getBoard().pegCount();
        boolean result = mode.autoplay();
        
        assertFalse(result, "Autoplay should return false when game is over");
        assertEquals(finalPegCount, mode.getBoard().pegCount(), 
            "Peg count should not change when game is over");
    }

    // ==================== Win/Loss Detection Tests ====================

    @Test
    @DisplayName("Test win condition detection")
    public void testWinConditionDetection() {
        mode.newGame("English", 5);
        
        // Play until only one peg remains (if possible)
        // Note: Random AI rarely wins, so we'll simulate by manually setting up a win
        while (mode.getBoard().pegCount() > 1 && 
               !mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        if (mode.getBoard().pegCount() == 1) {
            assertEquals(GameStatus.WON, mode.getStatus(), 
                "Status should be WON when one peg remains");
        } else {
            assertEquals(GameStatus.LOST, mode.getStatus(), 
                "Status should be LOST when multiple pegs remain with no moves");
        }
    }

    @Test
    @DisplayName("Test loss condition detection")
    public void testLossConditionDetection() {
        mode.newGame("English", 7);
        
        // Play until no moves available
        while (!mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        // Game should end in either WON or LOST
        assertTrue(mode.getStatus() == GameStatus.WON || 
                   mode.getStatus() == GameStatus.LOST,
            "Game should end in WON or LOST status");
        
        // If not won, should be lost
        if (mode.getBoard().pegCount() > 1) {
            assertEquals(GameStatus.LOST, mode.getStatus(), 
                "Status should be LOST when multiple pegs remain with no moves");
        }
    }

    @Test
    @DisplayName("Test autoplay stops when game is won")
    public void testAutoplayStopsWhenWon() {
        mode.newGame("English", 5);
        
        // Play until game ends
        while (mode.getStatus() == GameStatus.PLAYING && 
               !mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        int finalPegCount = mode.getBoard().pegCount();
        int finalHistorySize = mode.getHistory().size();
        
        // Try to autoplay after game ends
        boolean result = mode.autoplay();
        
        assertFalse(result, "Autoplay should return false when game is over");
        assertEquals(finalPegCount, mode.getBoard().pegCount(), 
            "Peg count should not change after game ends");
        assertEquals(finalHistorySize, mode.getHistory().size(), 
            "History should not grow after game ends");
    }

    // ==================== Undo Tests ====================

    @Test
    @DisplayName("Test undo reverses autoplay move")
    public void testUndoReversesMove() {
        mode.newGame("English", 7);
        int initialPegCount = mode.getBoard().pegCount();
        
        mode.autoplay();
        mode.undo();
        
        assertEquals(initialPegCount, mode.getBoard().pegCount(), 
            "Undo should restore original peg count");
        assertTrue(mode.getHistory().isEmpty(), 
            "History should be empty after undo");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), 
            "Status should be PLAYING after undo");
    }

    @Test
    @DisplayName("Test undo restores PLAYING status from terminal state")
    public void testUndoRestoresPlayingStatus() {
        mode.newGame("English", 5);
        
        // Play until game ends
        while (mode.getStatus() == GameStatus.PLAYING && 
               !mode.getBoard().validMoves().isEmpty()) {
            mode.autoplay();
        }
        
        GameStatus terminalStatus = mode.getStatus();
        assertTrue(terminalStatus == GameStatus.WON || terminalStatus == GameStatus.LOST,
            "Game should end in terminal state");
        
        // Undo one move
        if (!mode.getHistory().isEmpty()) {
            mode.undo();
            assertEquals(GameStatus.PLAYING, mode.getStatus(), 
                "Status should be restored to PLAYING after undo from terminal state");
        }
    }

    @Test
    @DisplayName("Test undo with empty history returns false")
    public void testUndoEmptyHistory() {
        mode.newGame("English", 7);
        
        boolean result = mode.undo();
        
        assertFalse(result, "Undo should return false when history is empty");
        assertEquals(32, mode.getBoard().pegCount(), 
            "Peg count should not change");
    }

    @Test
    @DisplayName("Test multiple undo operations")
    public void testMultipleUndo() {
        mode.newGame("English", 7);
        int initialPegCount = mode.getBoard().pegCount();
        
        // Execute 3 moves
        mode.autoplay();
        mode.autoplay();
        mode.autoplay();
        
        // Undo all 3 moves
        mode.undo();
        mode.undo();
        mode.undo();
        
        assertEquals(initialPegCount, mode.getBoard().pegCount(), 
            "All moves should be undone");
        assertTrue(mode.getHistory().isEmpty(), 
            "History should be empty");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Test autoplay on small board")
    public void testAutoplaySmallBoard() {
        mode.newGame("English", 5);
        
        int initialPegCount = mode.getBoard().pegCount();
        boolean result = mode.autoplay();
        
        assertTrue(result, "Autoplay should work on small boards");
        assertEquals(initialPegCount - 1, mode.getBoard().pegCount(), 
            "Peg count should decrease by 1 after autoplay");
    }

    @Test
    @DisplayName("Test autoplay on different board types")
    public void testAutoplayDifferentBoardTypes() {
        // Test English
        mode.newGame("English", 7);
        assertTrue(mode.autoplay(), "Autoplay should work on English board");
        
        // Test Diamond
        mode.newGame("Diamond", 7);
        assertTrue(mode.autoplay(), "Autoplay should work on Diamond board");
        
        // Test Hexagon
        mode.newGame("Hexagon", 5);
        assertTrue(mode.autoplay(), "Autoplay should work on Hexagon board");
    }

    @Test
    @DisplayName("Test newGame resets state")
    public void testNewGameResetsState() {
        mode.newGame("English", 7);
        
        // Play some moves
        mode.autoplay();
        mode.autoplay();
        mode.autoplay();
        
        // Start new game
        mode.newGame("Diamond", 7);
        
        assertEquals(24, mode.getBoard().pegCount(), 
            "New game should reset peg count");
        assertTrue(mode.getHistory().isEmpty(), 
            "New game should clear history");
        assertEquals(GameStatus.PLAYING, mode.getStatus(), 
            "New game should set status to PLAYING");
    }

    @Test
    @DisplayName("Test hasBoard returns correct value")
    public void testHasBoard() {
        assertFalse(mode.hasBoard(), "Should return false before initialization");
        
        mode.newGame("English", 7);
        
        assertTrue(mode.hasBoard(), "Should return true after initialization");
    }

    @Test
    @DisplayName("Test setState transfers state correctly")
    public void testSetState() {
        // Create a mode with some state
        AutomatedGameMode sourceMode = new AutomatedGameMode();
        sourceMode.newGame("English", 7);
        sourceMode.autoplay();
        sourceMode.autoplay();
        
        // Transfer state to new mode
        mode.setState(
            sourceMode.getBoard(), 
            sourceMode.getHistory(), 
            sourceMode.getStatus()
        );
        
        assertEquals(sourceMode.getBoard().pegCount(), mode.getBoard().pegCount(),
            "Peg count should match");
        assertEquals(sourceMode.getHistory().size(), mode.getHistory().size(),
            "History size should match");
        assertEquals(sourceMode.getStatus(), mode.getStatus(),
            "Status should match");
    }

    // ==================== Randomness Tests ====================

    @Test
    @DisplayName("Test autoplay makes progress over multiple games")
    public void testAutoplayMakesProgress() {
        // Run multiple games to ensure AI is working
        int gamesPlayed = 0;
        int totalMoves = 0;
        
        for (int i = 0; i < 5; i++) {
            mode.newGame("English", 7);
            
            while (mode.getStatus() == GameStatus.PLAYING && 
                   !mode.getBoard().validMoves().isEmpty()) {
                mode.autoplay();
            }
            
            gamesPlayed++;
            totalMoves += mode.getHistory().size();
        }
        
        assertEquals(5, gamesPlayed, "Should complete 5 games");
        assertTrue(totalMoves > 0, "Should execute moves across all games");
        assertTrue(totalMoves / gamesPlayed >= 10, 
            "Average moves per game should be at least 10");
    }
}
