package tel.schich.idl.runner

import com.github.ajalt.clikt.core.subcommands
import tel.schich.idl.runner.command.InProcessGenerate
import tel.schich.idl.runner.command.Root
import tel.schich.idl.runner.command.Validate

fun main(args: Array<String>) {
    Root()
        .subcommands(InProcessGenerate(), Validate())
        .main(args)
}