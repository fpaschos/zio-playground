package dev.fpas.zio
import zio.*

object Main1 extends scala.App:

  def errorCode = ZIO.succeed(12)

  def program = for {
    _ <- Console.printLine("TEST")
    msg <- errorCode
    _ <- ZIO.fail(RuntimeException(msg.toString))
  } yield ()

  def p2 = program
    .catchAll(err => Console.printLineError(s"Failed with error code $err"))

  val runtime = Runtime.default
  Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.run(p2).getOrThrowFiberFailure()
  }
