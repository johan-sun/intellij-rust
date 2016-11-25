package org.rust.lang.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.util.ancestors
import org.rust.lang.core.psi.util.findExpressionInRange
import org.rust.lang.core.psi.util.parentOfType
import java.util.*

class RustRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is RustPatBindingElement
    }

    override fun getIntroduceVariableHandler(): RustIntroduceVariableHandler {
        return RustIntroduceVariableHandler()
    }
}

class RustIntroduceVariableHandler : RefactoringActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(file)) return
        RustIntroduceVariableRefactoring(project, editor, file).invoke()
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
    }
}

private class RustIntroduceVariableRefactoring(
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile
) {
    fun invoke() {
        val candidates = extractableExpressions()
            ?: return

        IntroduceTargetChooser.showChooser(editor, candidates,
            object : Pass<RustExprElement>() {
                override fun pass(expression: RustExprElement) {
                    if (!expression.isValid) return

                    val description = prepareRefactoring(expression)
                        ?: return

                    OccurrencesChooser.simpleChooser<PsiElement>(editor)
                    val state = WriteCommandAction.runWriteCommandAction(project, Computable {
                        writeChanges(description)
                    })
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
                    RustVariableIntroducer(
                        state.declaration,
                        state.occurrences.toTypedArray()
                    ).performInplaceRefactoring(null)
                }
            },
            { if (it.isValid) it.text else "<invalid expression>" }
        )
    }

    private fun extractableExpressions(): List<RustExprElement>? {
        val leafExpression = editor.selectedExpression(file)

        if (leafExpression == null) {
            showCannotPerform("Select an expression")
            return null
        }

        if (leafExpression.parentOfType<RustBlockElement>() == null) {
            showCannotPerform(RefactoringBundle.message("refactoring.introduce.context.error"))
            return null
        }

        return leafExpression.ancestors
            .takeWhile { it !is RustBlockElement }
            .filterIsInstance<RustExprElement>()
            .toList()
    }

    private fun prepareRefactoring(target: RustExprElement): RefactoringDescription? {
        val varName = "i" // FIXME
        val anchor = findAnchor(target)
        if (anchor == null) {
            showCannotPerform(RefactoringBundle.message("refactoring.introduce.context.error"))
            return null
        }

        val occurrences = listOf(target)
        return RefactoringDescription(varName, target, occurrences, anchor)
    }

    private fun writeChanges(d: RefactoringDescription): PostRefactoringState {

        d.anchor.prepend(RustElementFactory.newLine(project))
        val varDeclaration = d.anchor.prepend(
            RustElementFactory.createVarDeclaration(project, d.name, d.target)
        ) as RustLetDeclElement
        val names = ArrayList<RustExprElement>()
        for (occurrence in d.occurrences) {
            val ref = RustElementFactory.createExpression(project, d.name)!!
            names += occurrence.replace(ref) as RustExprElement
        }

        return PostRefactoringState(
            (varDeclaration.pat as RustPatIdentElement).patBinding,
            names
        )
    }

    private fun showCannotPerform(message: String) {
        CommonRefactoringUtil.showErrorHint(project, editor,
            message,
            RefactoringBundle.getCannotRefactorMessage(null), "refactoring.extractVariable")
    }

    private data class RefactoringDescription(
        val name: String,
        val target: RustExprElement,
        val occurrences: List<RustExprElement>,
        val anchor: PsiElement
    )

    private data class PostRefactoringState(
        val declaration: RustPatBindingElement,
        val occurrences: List<RustExprElement>
    )

    private inner class RustVariableIntroducer(
        elementToRename: RustPatBindingElement,
        occurrences: Array<out PsiElement>
    ) : InplaceVariableIntroducer<PsiElement>(elementToRename, editor, project, "Introduce Variable", occurrences, null)
}

private fun Editor.selectedExpression(file: PsiFile): RustExprElement? =
    if (selectionModel.hasSelection())
        findExpressionInRange(file, selectionModel.selectionStart, selectionModel.selectionEnd)
    else
        findExpressionInRange(file, caretModel.offset - 1, caretModel.offset)

private fun findAnchor(expression: RustExprElement): PsiElement? =
    expression.parentOfType<RustStmtElement>()

private fun PsiElement.prepend(element: PsiElement): PsiElement = parent.addBefore(element, this)
