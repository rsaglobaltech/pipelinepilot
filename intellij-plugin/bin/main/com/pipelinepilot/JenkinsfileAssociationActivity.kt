package com.pipelinepilot

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileTypes.WildcardFileNameMatcher
import com.intellij.openapi.startup.ProjectActivity

/**
 * Associates Jenkinsfile naming patterns with the bundled Groovy file type so they get
 * syntax highlighting and PSI parsing for free. Idempotent and safe to run per project.
 */
class JenkinsfileAssociationActivity : ProjectActivity {

    private val log = logger<JenkinsfileAssociationActivity>()

    override suspend fun execute(project: Project) {
        val ftm = FileTypeManager.getInstance()
        val groovy = ftm.findFileTypeByName("Groovy") ?: run {
            log.warn("Groovy file type not found; Jenkinsfile association skipped")
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                JenkinsfileDetector.ASSOCIATION_PATTERNS.forEach { pattern ->
                    val matcher = WildcardFileNameMatcher(pattern)
                    if (ftm.getFileTypeByFileName(pattern) != groovy) {
                        (ftm as? FileTypeManagerEx)?.associate(groovy, matcher)
                    }
                }
            }
        }
    }
}
