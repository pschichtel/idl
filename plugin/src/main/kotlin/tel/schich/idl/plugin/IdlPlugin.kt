package tel.schich.idl.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create

interface IdlPluginExtension {
    val message: Property<String>
}

class IdlPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Add the 'greeting' extension object
        val extension = project.extensions.create<IdlPluginExtension>("idl-plugin")
        extension.message.convention("Hello from GreetingPlugin")
        // Add a task that uses configuration from the extension object
        project.tasks.register("generate") {
            doLast {
                println(extension.message.get())
            }
        }
    }
}