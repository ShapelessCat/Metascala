package metascala.features

import metascala.TestUtil._
import org.scalatest.FreeSpec

class InvokeDynamicTests extends FreeSpec {

  "invokedynamic" - {
    "findStaticField" in {
      val tester = new Tester("metascala.features.javac.InvokeDynamic")
      tester.run("findStaticGetter", false)
      tester.run("findStaticGetter", true)
      tester.run("findStaticGetterBoxed", false)
      tester.run("findStaticGetterBoxed", true)

      tester.run("findStaticSetter1", false)
      tester.run("findStaticSetter1", true)
      tester.run("findStaticSetter2", false)
      tester.run("findStaticSetter2", true)
      tester.run("findStaticSetter3", false)
      tester.run("findStaticSetter3", true)
      tester.run("findStaticSetter4", false)
      tester.run("findStaticSetter4", true)
    }

//    "findInstanceField" in {
//      val tester = new Tester("metascala.features.javac.InvokeDynamic")
//      tester.run("findFieldGetter", false)
//      tester.run("findFieldGetter", true)
//      tester.run("findFieldSetter", false)
//      tester.run("findFieldSetter", true)
//    }
//    "findStaticMethod" in {
//      val tester = new Tester("metascala.features.javac.InvokeDynamic")
//      tester.run("findStaticMethod", false)
//      tester.run("findStaticMethod", true)
//    }
  }
}

