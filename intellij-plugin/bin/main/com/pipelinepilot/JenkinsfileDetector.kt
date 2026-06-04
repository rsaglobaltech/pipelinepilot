package com.pipelinepilot

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * Decides whether a given file is a Jenkins Pipeline file.
 *
 * Recognized:
 *   - exactly "Jenkinsfile"            (case-insensitive)
 *   - "<anything>.jenkinsfile"
 *   - "Jenkinsfile.<env>"  e.g. Jenkinsfile.prod, Jenkinsfile.staging
 */
object JenkinsfileDetector {

    fun isJenkinsfile(file: VirtualFile?): Boolean {
        val name = file?.name ?: return false
        return matches(name)
    }

    fun isJenkinsfile(file: PsiFile?): Boolean {
        val name = file?.name ?: return false
        return matches(name)
    }

    private fun matches(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "jenkinsfile" ||
            lower.endsWith(".jenkinsfile") ||
            lower.startsWith("jenkinsfile.")
    }

    /** Filename glob patterns used to associate Jenkinsfiles with the Groovy file type. */
    val ASSOCIATION_PATTERNS: List<String> = listOf(
        "Jenkinsfile",
        "*.jenkinsfile",
        "Jenkinsfile.*",
    )
}
