package tel.schich.idl.runner

import com.github.ajalt.clikt.core.subcommands
import tel.schich.idl.runner.command.Generate
import tel.schich.idl.runner.command.Root
import tel.schich.idl.runner.command.Validate

fun main(args: Array<String>) {
    Root()
        .subcommands(Generate(), Validate())
        .main(args)
}