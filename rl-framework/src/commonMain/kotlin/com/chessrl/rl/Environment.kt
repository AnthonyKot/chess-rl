package com.chessrl.rl

/**
 * RL Framework Package - Reinforcement learning interfaces and algorithms
 * This package provides the core RL framework that can work with any environment,
 * including the chess environment.
 */

/**
 * Generic environment interface for reinforcement learning
 */
interface Environment<S, A> {
    fun reset(): S
    fun step(action: A): StepResult<S>
    fun getValidActions(state: S): List<A>
    fun isTerminal(state: S): Boolean
    fun getStateSize(): Int
    fun getActionSize(): Int
}

/**
 * Result of taking a step in the environment
 */
data class StepResult<S>(
    val nextState: S,
    val reward: Double,
    val done: Boolean,
    val info: Map<String, Any> = emptyMap()
)

/**
 * Generic agent interface for reinforcement learning
 */
interface Agent<S, A> {
    fun selectAction(state: S, validActions: List<A>): A
    fun learn(experience: Experience<S, A>)
    fun save(path: String)
    fun load(path: String)
}

/**
 * Experience tuple for reinforcement learning
 */
data class Experience<S, A>(
    val state: S,
    val action: A,
    val reward: Double,
    val nextState: S,
    val done: Boolean
)