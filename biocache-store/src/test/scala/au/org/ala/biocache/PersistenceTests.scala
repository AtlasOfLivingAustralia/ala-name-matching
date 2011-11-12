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
        val uuid = Config.persistenceManager.putList[QualityAssertion](null, "test", "mylist", list, classOf[QualityAssertion], true)

        //retrieve the list
        println("UUID: " + uuid)
        val retrievedList = Config.persistenceManager.getList[QualityAssertion](uuid, "test", "mylist", classOf[QualityAssertion])
        expect(2){retrievedList.size}
        expect(1){retrievedList.head.code}
    }
}