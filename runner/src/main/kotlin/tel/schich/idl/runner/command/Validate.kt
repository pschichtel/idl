package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import tel.schich.idl.runner.generate.loadInputs
import java.nio.file.Path

class Validate : CliktCommand() {
    private val moduleSources: List<Path> by moduleSourcesOption()

    override fun run() {
        val inputs = loadInputs(resolveModulePaths(moduleSources), emptyList())
        validate(inputs.modules)
    }
}