package dev.fpas.zio
package macros

import scala.compiletime.error
object Macros:
  inline def doSomething(inline mode: Boolean): Unit =
    if mode then println("mode active")
    else if !mode then println("mode inactive")
    else error("Mode must be a known value at compile time")

end Macros

object Runner extends App:
  import Macros.*

  // val mode: Boolean = false
  // doSomething(mode) // error: Mode must be known value at compile time
  doSomething(false)
