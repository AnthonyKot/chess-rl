package com.chessrl.chess

/**
 * Simple interactive CLI to manually verify the chess engine.
 *
 * Usage examples:
 *  - ./gradlew :chess-engine:runCli
 *  - ./gradlew :chess-engine:runCli -Dargs="--fen 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'"
 *
 * Commands inside the CLI:
 *  - index        Apply the move by its index in the legal moves list (e.g., 0)
 *  - e2e4         Apply a move by coordinate notation (supports promotions like e7e8q)
 *  - fen <FEN>    Load a new position from FEN
 *  - u            Undo last move (uses last entry's boardStateBefore)
 *  - q            Quit
 */
fun main(args: Array<String>) {
    val game = ChessGame()
    val detector = GameStateDetector()
    var selected: Position? = null

    // Parse optional --fen
    val fenArgIndex = args.indexOf("--fen")
    if (fenArgIndex >= 0 && fenArgIndex + 1 < args.size) {
        val fen = args[fenArgIndex + 1]
        val ok = game.loadFromFEN(fen)
        if (!ok) println("Warning: failed to load provided FEN; starting position will be used")
    }

    println("=== Chess Engine Interactive CLI ===")
    loop@ while (true) {
        // Show board and state
        val board = game.getCurrentBoard()
        println()
        val active = board.getActiveColor()
        val allLegal = detector.getAllLegalMoves(board, active)
        // Highlight selected square and its legal destinations, if any
        val highlights: Set<Position>? = selected?.let { sel ->
            val fromMoves = allLegal.filter { it.from == sel }
            if (fromMoves.isEmpty()) setOf(sel) else (setOf(sel) + fromMoves.map { it.to })
        }
        if (highlights != null) println(board.toASCIIWithHighlights(highlights)) else println(board.toASCII())
        val status = buildString {
            val inCheck = detector.isInCheck(board, active)
            val isMate = detector.isCheckmate(board, active)
            val isStale = detector.isStalemate(board, active)
            append("Side to move: $active")
            if (isMate) append(" | Checkmate")
            else if (isStale) append(" | Stalemate")
            else if (inCheck) append(" | Check")
        }
        println(status)

        // List legal moves (overall and from selected square)
        if (allLegal.isEmpty()) {
            val status = when {
                detector.isInCheck(board, active) -> "Checkmate"
                else -> "Stalemate or no legal moves"
            }
            println("No legal moves. Status: $status")
        } else {
            val sel = selected
            if (sel != null) {
                val legalFrom = allLegal.filter { it.from == sel }
                val sq = "${('a' + sel.file)}${('1' + sel.rank)}"
                if (legalFrom.isEmpty()) {
                    println("Selected: $sq — no legal moves from this square (empty or pinned)")
                } else {
                    println("Selected: $sq — legal moves (${legalFrom.size}):")
                    legalFrom.forEachIndexed { idx, mv -> println("  [$idx] ${mv.toAlgebraic()}") }
                }
            } else {
                println("Legal moves (${allLegal.size}). Type a square like e2 to filter, or 'list' to show all.")
            }
        }

        // Prompt
        print("Enter: '<sq>' (e2), '<sq> <sq>' (e2 e4), index (if selected), algebraic (e2e4), 'list', 'clear', 'fen'|'fen <FEN>', 'u', 'q', 'help' > ")
        val inputLine = readLine()
        if (inputLine == null) {
            println("No interactive input detected. Exiting.")
            break@loop
        }
        val input = inputLine.trim()
        if (input.isEmpty()) continue

        // Two-square input tolerant parser: e2 e4 (accepts extra spaces or trailing punctuation)
        val parts = input
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (parts.size >= 2 &&
            parts[0].length >= 2 && parts[1].length >= 2 &&
            parts[0].substring(0,2).matches(Regex("^[a-h][1-8]$")) &&
            parts[1].substring(0,2).matches(Regex("^[a-h][1-8]$")) ) {
            val fromSq = parts[0].substring(0,2)
            val toSq = parts[1].substring(0,2)
            val from = Position(fromSq[1] - '1', fromSq[0] - 'a')
            val to = Position(toSq[1] - '1', toSq[0] - 'a')
            val boardNow = game.getCurrentBoard()
            val legalFrom = detector.getAllLegalMoves(boardNow, boardNow.getActiveColor()).filter { it.from == from }
            val chosen = legalFrom.firstOrNull { it.to == to }
            if (chosen == null) {
                println("No legal move from $fromSq to $toSq in this position")
            } else {
                val result = game.makeMove(chosen)
                if (result != MoveResult.SUCCESS) println("Move failed: $result")
                selected = null
            }
            continue@loop
        }

        when {
            input.equals("q", ignoreCase = true) -> break@loop
            input.equals("help", ignoreCase = true) -> {
                println("Commands:")
                println("  <index>         Apply move by index shown in legal moves list")
                println("  e2e4            Apply move by coordinate notation (supports promotions: e7e8q)")
                println("  moves e2        Show legal moves from a square")
                println("  fen             Print current FEN")
                println("  fen <FEN>       Load a FEN position")
                println("  u               Undo last move")
                println("  q               Quit")
                continue@loop
            }
            input.equals("fen", ignoreCase = true) -> {
                println("Current FEN: ${board.toFEN()}")
                continue@loop
            }
            input.equals("list", ignoreCase = true) -> {
                val list = detector.getAllLegalMoves(board, board.getActiveColor())
                println("All legal moves (${list.size}):")
                list.forEachIndexed { idx, mv -> println("  [$idx] ${mv.toAlgebraic()}") }
                continue@loop
            }
            input.equals("clear", ignoreCase = true) -> { selected = null; continue@loop }
            input.equals("u", ignoreCase = true) -> {
                val hist = game.getMoveHistory()
                if (hist.isNotEmpty()) {
                    val prevFen = hist.last().boardStateBefore
                    val ok = game.loadFromFEN(prevFen)
                    if (!ok) println("Undo failed: could not load previous FEN")
                } else {
                    println("Nothing to undo")
                }
                selected = null
                continue@loop
            }
            input.startsWith("moves ", ignoreCase = true) -> {
                val sq = input.removePrefix("moves ").trim().lowercase()
                if (sq.length != 2) {
                    println("Use a square like e2")
                    continue@loop
                }
                val file = sq[0] - 'a'
                val rank = sq[1] - '1'
                if (file !in 0..7 || rank !in 0..7) {
                    println("Invalid square")
                    continue@loop
                }
                val pos = Position(rank, file)
                val pseudo = MoveValidationSystem().getValidMoves(board, pos)
                if (pseudo.isEmpty()) {
                    println("No pseudo-legal moves from $sq (might be empty square or pinned)")
                } else {
                    val legal = pseudo.filter { detector.isMoveLegal(board, it) }
                    if (legal.isEmpty()) {
                        println("No legal moves from $sq")
                    } else {
                        println("Legal moves from $sq:")
                        legal.forEach { mv -> println("  ${mv.toAlgebraic()}") }
                    }
                }
                continue@loop
            }
            // Square selection like e2
            input.length == 2 && input[0] in 'a'..'h' && input[1] in '1'..'8' -> {
                val file = input[0] - 'a'
                val rank = input[1] - '1'
                selected = Position(rank, file)
                continue@loop
            }
            input.startsWith("fen ", ignoreCase = true) -> {
                val fen = input.removePrefix("fen ").trim()
                val ok = game.loadFromFEN(fen)
                if (!ok) println("Failed to load FEN")
                selected = null
                continue@loop
            }
            input.all { it.isDigit() } -> {
                val idx = input.toIntOrNull()
                if (idx == null) { println("Invalid index"); continue@loop }
                val sel = selected
                val boardNow = game.getCurrentBoard()
                if (sel == null) {
                    println("Select a square first (e.g., e2)")
                    continue@loop
                }
                val legalFrom = detector.getAllLegalMoves(boardNow, boardNow.getActiveColor()).filter { it.from == sel }
                if (idx !in legalFrom.indices) {
                    println("Invalid index for selected square")
                    continue@loop
                }
                val result = game.makeMove(legalFrom[idx])
                if (result != MoveResult.SUCCESS) println("Move failed: $result")
                selected = null
                continue@loop
            }
            else -> {
                // Try coordinate algebraic like e2e4 or e7e8q
                val mv = Move.fromAlgebraic(input)
                if (mv == null) {
                    println("Unrecognized input. Use index, e2e4, 'fen <FEN>', 'u', or 'q'.")
                    continue@loop
                }
                // Ensure move is legal in current position
                val boardNow = game.getCurrentBoard()
                val legal = detector.getAllLegalMoves(boardNow, boardNow.getActiveColor())
                val chosen = legal.firstOrNull { it.from == mv.from && it.to == mv.to && it.promotion == mv.promotion }
                if (chosen == null) {
                    println("That move is not legal in this position")
                    continue@loop
                }
                val result = game.makeMove(chosen)
                if (result != MoveResult.SUCCESS) println("Move failed: $result")
                selected = null
                continue@loop
            }
        }
    }

    println("Goodbye.")
}
