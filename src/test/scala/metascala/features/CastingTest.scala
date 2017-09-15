package metascala
package features

import utest._

import metascala.{BufferLog, Gen, TestUtil}
import Gen.chk
import java.awt.geom.Point2D

import TestUtil._
class CastingTest extends utest.TestSuite {

  def tests = Tests {
    val buffer = new BufferLog(4000)
    "if else" - {
      val tester = new VM()
      "basicCasts" - {
        for (x <- Seq("omg", Array(1, 2, 3), new Object())) {
          tester.test(
            try {
              val s = x.asInstanceOf[String]
              s.length()
            } catch {
              case x: ClassCastException =>
                -1
            }
          )
        }
      }
      "arrayCasts" - {
        val cases = Seq(
          Array(1, 2, 3),
          Array("omg", "wtf", "bbq"),
          Array(new Object(), new Object()),
          Array(new Point2D.Double(), new Point2D.Double())
        )
        for (x <- cases) tester.test {
          try {
            val s: Array[Point2D] = x.asInstanceOf[Array[Point2D]]
            s.length
          } catch {
            case e: ClassCastException =>
              -1
          }
        }
      }

      "primArrayCasts" - {
        val cases = Seq(
          Array(1, 2, 3),
          Array(1.0, 2.0, 3.0),
          Array("omg", "wtf", "bbq")
        )
        for (x <- cases) tester.test {
          try {
            val s: Array[Int] = x.asInstanceOf[Array[Int]]
            s.length
          } catch {
            case e: ClassCastException =>
              -1
          }
        }
      }
      "instanceOf" - {
        val cases = Seq(
          "omg",
          Array(1, 2, 3),
          new Object(),
          new Point2D.Double(1, 1)
        )
        for (x <- cases) tester.test(
          if (x.isInstanceOf[Point2D]) 0 else 1
        )

      }
      "instanceOfArray" - {
        val cases = Seq(
          Array(1, 2, 3),
          Array("omg", "wtf", "bbq"),
          Array(new Object(), new Object()),
          Array(new Point2D.Double(), new Point2D.Double())
        )
        for (x <- cases) tester.test(
          if (x.isInstanceOf[Array[Point2D]]) 0 else 1
        )
      }
      "instanceOfPrimArray" - {
        val cases = Seq(
          Array(1, 2, 3),
          Array(1.0, 2.0, 3.0),
          Array("omg", "wtf", "bbq")
        )
        for (x <- cases) tester.test(
          if (x.isInstanceOf[Array[Int]]) 0 else 1
        )
      }

    }

  }


}