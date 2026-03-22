package versioning

import org.gradle.api.Project

object BuildConfig {
  private var _shadePE: Boolean? = null

  fun init(project: Project) {
    _shadePE = resolveBool(project, "shadePE", altKey = "SHADE_PE", default = true)
  }

  val shadePE: Boolean
    get() = _shadePE ?: error("BuildConfig.shadePE accessed before init() was called")

  private fun resolveBool(
    project: Project,
    key: String,
    altKey: String,
    default: Boolean,
  ): Boolean =
    (System.getProperty(key) ?: project.findProperty(key)?.toString() ?: System.getenv(altKey))
      ?.lowercase()
      ?.toBooleanStrictOrNull() ?: default
}
