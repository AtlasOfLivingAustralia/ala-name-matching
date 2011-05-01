package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.Ignore

@Ignore
class PersistenceTests extends FunSuite {

    test("simple put without id"){
        val uuid = Config.persistenceManager.put(null, "test", "dave-property", "dave-value")
        val retrievedValue = Config.persistenceManager.get(uuid, "test", "dave-property")
        expect("dave-value"){retrievedValue.getOrElse("")}
    }

    test("Simple put list"){
        val list = List(QualityAssertion(1),QualityAssertion(2))
        val uuid = Config.persistenceManager.putList(null, "test", "mylist", list, true)

        //retrieve the list
        println("UUID: " + uuid)
        val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
        val retrievedList = Config.persistenceManager.getList(uuid, "test", "mylist", theClass).asInstanceOf[List[QualityAssertion]]
        expect(2){retrievedList.size}
        expect(1){retrievedList.head.code}
    }
}