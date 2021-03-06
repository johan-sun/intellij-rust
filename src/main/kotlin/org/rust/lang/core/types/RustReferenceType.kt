package org.rust.lang.core.types

import com.intellij.openapi.project.Project
import org.rust.lang.core.psi.RustFnElement
import org.rust.lang.core.types.util.stripAllRefsIfAny
import org.rust.lang.core.types.visitors.RustTypeVisitor

class RustReferenceType(val referenced: RustType, val mutable: Boolean = false) : RustTypeBase() {

    override fun getNonStaticMethodsIn(project: Project): Sequence<RustFnElement> =
        super.getNonStaticMethodsIn(project) + stripAllRefsIfAny().getNonStaticMethodsIn(project)

    override fun <T> accept(visitor: RustTypeVisitor<T>): T = visitor.visitReference(this)

    override fun toString(): String = "${if (mutable) "&mut" else "&"} $referenced"
}
