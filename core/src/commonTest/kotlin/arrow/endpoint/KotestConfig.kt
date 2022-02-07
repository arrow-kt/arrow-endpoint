package arrow.endpoint

import io.kotest.core.config.AbstractProjectConfig

class KotestConfig : AbstractProjectConfig() {
  override val globalAssertSoftly: Boolean = true
}
