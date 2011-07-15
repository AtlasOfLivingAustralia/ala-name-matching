package au.org.ala.biocache

import java.util.Properties
import com.google.inject.name.Names
import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.inject.{Scopes, Guice, Injector, AbstractModule}
import org.slf4j.LoggerFactory

/**
 * Simple singleton wrapper for Guice (or spring)
 */
object Config {
    private val inj:Injector = Guice.createInjector(new ConfigModule())
    def getInstance(classs:Class[_]) = inj.getInstance(classs)

    val occurrenceDAO = getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    val persistenceManager = getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    val nameIndex = getInstance(classOf[CBIndexSearch]).asInstanceOf[CBIndexSearch]
}

/**
 * Guice module
 */
class ConfigModule extends AbstractModule {

    protected val logger = LoggerFactory.getLogger("ConfigModule")

    override def configure() {

        val properties = new Properties()
        properties.load(this.getClass.getResourceAsStream("/biocache.properties"))
        Names.bindProperties(this.binder,properties)

        //bind concrete implementations
        bind(classOf[OccurrenceDAO]).to(classOf[OccurrenceDAOImpl]).in(Scopes.SINGLETON)
        bind(classOf[IndexDAO]).to(classOf[SolrIndexDAO]).in(Scopes.SINGLETON)

        try {
            val nameIndex = new CBIndexSearch(properties.getProperty("nameIndexLocation"))
            bind(classOf[CBIndexSearch]).toInstance(nameIndex)
        } catch {
            case e: Exception => logger.warn("Lucene indexes arent currently available.")
        }

        properties.getProperty("db") match {
            case "cassandra" => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
            case "mongodb" => bind(classOf[PersistenceManager]).to(classOf[MongoDBPersistenceManager]).in(Scopes.SINGLETON)
            case _ => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
        }
    }
}