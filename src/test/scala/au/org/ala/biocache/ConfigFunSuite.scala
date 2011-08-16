package au.org.ala.biocache

import org.scalatest.FunSuite
import com.google.inject.Guice
/**
 * All tests should extend this to access the correct DI
 */
class ConfigFunSuite extends FunSuite {
    Config.inj = Guice.createInjector(new TestConfigModule)
    val pm = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    prepare
    def prepare() {

    }
}