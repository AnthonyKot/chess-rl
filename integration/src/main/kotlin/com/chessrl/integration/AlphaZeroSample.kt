package com.chessrl.integration

data class AlphaZeroSample(
    val state: DoubleArray,        // 839-dim board features
    val policyTarget: DoubleArray, // 4096-dim normalized MCTS visit distribution (π)
    val valueTarget: Double        // game outcome z ∈ {-1.0, 0.0, 1.0} from this player's POV
)
