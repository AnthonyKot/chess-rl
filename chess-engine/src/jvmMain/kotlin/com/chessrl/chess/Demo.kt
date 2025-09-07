package com.chessrl.chess

/**
 * Demo application to showcase the chess engine functionality
 */
fun main() {
    println("=== Chess Engine Demo ===")
    println()
    
    // Create a new chess game
    val game = ChessGame()
    
    // Display initial position
    println("Initial Position:")
    println(game.displayBoard())
    
    // Make some moves
    println("Making moves: e2-e4, e7-e5, Ng1-f3")
    game.makeMove(Move.fromAlgebraic("e2e4")!!)
    game.makeMove(Move.fromAlgebraic("e7e5")!!)
    game.makeMove(Move.fromAlgebraic("g1f3")!!)
    
    // Display position after moves
    println("Position after moves:")
    println(game.displayBoardWithLastMove())
    
    // Show move history
    println(game.getMoveHistoryDetailed())
    
    // Demonstrate FEN functionality
    println("Current FEN: ${game.getCurrentBoard().toFEN()}")
    
    // Load a custom position
    println("\nLoading custom position (King and Queen endgame):")
    game.loadFromFEN("8/8/8/8/8/8/4K3/4Q2k w - - 0 1")
    println(game.displayBoard())
    
    // Show position analysis
    println(game.analyzePosition())
    
    // Validate FEN
    println("=== FEN Validation Demo ===")
    val validationResult = ChessValidator.validateFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    println(validationResult.getReport())
    
    // Compare two positions
    println("=== Position Comparison Demo ===")
    val startingFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val afterE4FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    println(ChessValidator.comparePositions(startingFEN, afterE4FEN))
    
    println("Demo completed!")
}