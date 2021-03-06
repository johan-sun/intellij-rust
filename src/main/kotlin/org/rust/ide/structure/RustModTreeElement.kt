package org.rust.ide.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import org.rust.lang.core.psi.*

open class RustModTreeElement(item: RustMod) : PsiTreeElementBase<RustMod>(item) {
    override fun getPresentableText() = element?.name

    override fun getChildrenBase(): Collection<StructureViewTreeElement> =
        element?.allItems.orEmpty().mapNotNull { toTreeElement(it) }

    private fun toTreeElement(it: RustItemElement): StructureViewTreeElement? =
        when (it) {
            is RustConstItemElement     -> RustConstTreeElement(it)
            is RustEnumItemElement      -> RustEnumTreeElement(it)
            is RustFnItemElement        -> RustFnTreeElement(it)
            is RustImplItemElement      -> RustImplTreeElement(it)
            is RustModDeclItemElement   -> RustModDeclTreeElement(it)
            is RustModItemElement       -> RustModTreeElement(it)
            is RustStaticItemElement    -> RustStaticTreeElement(it)
            is RustStructItemElement    -> RustStructTreeElement(it)
            is RustTraitItemElement     -> RustTraitTreeElement(it)
            is RustTypeItemElement      -> RustTypeTreeElement(it)
            else                        -> null
        }
}
