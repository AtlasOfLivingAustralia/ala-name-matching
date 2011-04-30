package au.org.ala.biocache

import org.scalatest.FunSuite

class PersistenceTests extends FunSuite {

    test("simple put by id"){



    }

    test("simple get by id"){

        println("running test")
        val pm = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
        val result = pm.get("scientificName", "Balanus amphitrite")
        println(result.getOrElse("failed"))
    }
}