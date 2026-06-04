package com.pipelinepilot.library

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.pipelinepilot.JenkinsfileDetector

/**
 * Ctrl+Click / Go to Declaration on a shared-library step identifier inside a
 * Jenkinsfile jumps to the backing `vars/<step>.groovy` file.
 */
class SharedLibraryGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?,
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        if (!JenkinsfileDetector.isJenkinsfile(element.containingFile)) return null

        val name = element.text ?: return null
        if (name.isBlank() || !name.first().isJavaIdentifierStart()) return null

        val step = SharedLibrarySupport.findStep(name) ?: return null
        val vFile = LocalFileSystem.getInstance().findFileByIoFile(step.file) ?: return null
        val psiFile = PsiManager.getInstance(element.project).findFile(vFile) ?: return null
        return arrayOf(psiFile)
    }
}
