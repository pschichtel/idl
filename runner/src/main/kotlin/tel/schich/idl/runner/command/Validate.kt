package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Path

class Validate : CliktCommand() {
    private val moduleSources: List<Path> by moduleSourcesOption()

    override fun run() {
        validate(loadModules(moduleSources))
    }
}