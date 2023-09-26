package tel.schich.idl.plugin.tasks

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import tel.schich.idl.generator.kotlin.KotlinGenerator

@CacheableTask
abstract class GenerateKotlinTask : IdlGenerateTask() {

    init {
        output.convention(project.layout.buildDirectory.map { it.dir("generated-kotlin") })
    }

    @TaskAction
    private fun generate() {
        generate(KotlinGenerator())
    }
}