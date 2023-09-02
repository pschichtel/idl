package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import tel.schich.idl.core.ModuleReference
import java.nio.file.Path
import kotlin.system.exitProcess

class Generate : CliktCommand() {
    private val moduleSources: List<Path> by moduleSourcesOption()
    private val subjectModuleReference by argument(name = "subject").convert { arg ->
        ModuleReference.parse(arg)
            ?: fail("the subject must be a valid module reference of the format module-name:version")
    }

    override fun run() {
        val modules = loadModules(moduleSources)
        validate(modules)

        val subjectModule = modules.find { it.reference == subjectModuleReference }
        if (subjectModule == null) {
            echo("Module $subjectModuleReference not found in loaded modules:", err = true)
            for (module in modules) {
                error(" - ${module.reference}")
            }
            exitProcess(1)
        }

        for ((name, module) in modules) {
            echo("Module $name:")
            echo("    $module")
            echo()
        }

        echo("Subject: ${subjectModule.metadata.name}")
    }
}