package com.chessrl.integration.config

/**
 * JVM runner for the configuration system demonstration.
 * 
 * This can be used to test the configuration system from the command line.
 */
fun main(args: Array<String>) {
    if (args.contains("--help") || args.contains("-h")) {
        println("Chess RL Configuration System Demo")
        println()
        println("Usage:")
        println("  --demo                 Run all configuration demos")
        println("  --profiles             Show profile demonstrations")
        println("  --validation           Show validation demonstrations")
        println("  --parse <args...>      Parse the provided arguments")
        println("  --help, -h             Show this help")
        println()
        return
    }
    
    when {
        args.contains("--demo") -> {
            ConfigDemo.runAllDemos()
        }
        args.contains("--profiles") -> {
            ConfigDemo.demonstrateProfiles()
        }
        args.contains("--validation") -> {
            ConfigDemo.demonstrateValidation()
        }
        args.contains("--parse") -> {
            val parseIndex = args.indexOf("--parse")
            val parseArgs = args.drop(parseIndex + 1).toTypedArray()
            println("Parsing arguments: ${parseArgs.joinToString(" ")}")
            val config = ConfigParser.parseArgs(parseArgs)
            println()
            println("Parsed Configuration:")
            println(config.getSummary())
            println()
            val validation = config.validate()
            validation.printResults()
        }
        else -> {
            println("Chess RL Configuration System")
            println("Run with --help for usage information")
            println()
            ConfigDemo.demonstrateBasicUsage()
        }
    }
}