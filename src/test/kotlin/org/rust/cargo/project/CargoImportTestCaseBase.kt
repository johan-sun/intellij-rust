package org.rust.cargo.project

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import org.assertj.core.api.Assertions.assertThat
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.module.util.findExternCrateByName
import org.rust.cargo.project.module.util.targets
import org.rust.cargo.project.settings.CargoProjectSettings

abstract class CargoImportTestCaseBase : ExternalSystemImportingTestCase() {
    override fun getTestsTempDir(): String = "cargoImportTests"

    override fun getExternalSystemConfigFileName(): String = "Cargo.toml"

    override fun getCurrentExternalProjectSettings(): CargoProjectSettings? =
        RustSdkType.INSTANCE.suggestHomePath()?.let {
            CargoProjectSettings(it)
        }

    override fun getExternalSystemId(): ProjectSystemId = CargoConstants.PROJECT_SYSTEM_ID

    override fun runTest() {
        if (currentExternalProjectSettings == null) {
            System.err?.println("SKIP $name: no Rust sdk found")
            return
        }
        super.runTest()
    }

    protected fun assertTargets(vararg paths: String) {
        assertThat(module.targets.map { it.path })
            .containsOnly(*paths)
    }

    protected fun assertExternCrates(vararg crateNames: String) {
        for (name in crateNames) {
            assertThat(module.findExternCrateByName(name))
                .isNotNull()
        }
    }

    private val module: Module get() =
        ModuleManager.getInstance(myProject).modules.single()
}
