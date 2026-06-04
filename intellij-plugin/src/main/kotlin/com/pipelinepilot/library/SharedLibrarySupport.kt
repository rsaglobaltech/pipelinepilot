package com.pipelinepilot.library

import com.pipelinepilot.settings.PipelinePilotSettings
import java.io.File

/** A global var step (e.g. vars/deploy.groovy → step name "deploy"). */
data class VarStep(val name: String, val file: File)

/**
 * Resolves shared-library symbols from local checkouts configured in settings.
 * Each root is expected to follow the Jenkins shared-library layout:
 *   <root>/vars/<step>.groovy   -> global step  (call as `<step>(...)`)
 *   <root>/src/**/*.groovy      -> library classes
 */
object SharedLibrarySupport {

    private val LIBRARY_ANNOTATION = Regex("""@Library\(\s*\[?\s*['"]([^'"]+)['"]""")

    fun roots(settings: PipelinePilotSettings = PipelinePilotSettings.getInstance()): List<File> =
        settings.state.sharedLibraryRoots.map(::File).filter { it.isDirectory }

    /** All global var steps across configured roots, de-duplicated by name. */
    fun varSteps(settings: PipelinePilotSettings = PipelinePilotSettings.getInstance()): List<VarStep> =
        roots(settings)
            .map { File(it, "vars") }
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir.listFiles { f -> f.isFile && f.extension == "groovy" }?.asList().orEmpty()
            }
            .map { VarStep(it.nameWithoutExtension, it) }
            .distinctBy { it.name }

    fun findStep(name: String, settings: PipelinePilotSettings = PipelinePilotSettings.getInstance()): VarStep? =
        varSteps(settings).firstOrNull { it.name == name }

    /** Parse `@Library('name@ref')` declarations from a Jenkinsfile's text. */
    fun declaredLibraries(text: String): List<String> =
        LIBRARY_ANNOTATION.findAll(text).map { it.groupValues[1] }.toList()
}
