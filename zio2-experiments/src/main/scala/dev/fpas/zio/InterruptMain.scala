package dev.fpas.zio
import zio.*

object InterruptMain extends ZIOAppDefault:

  def validate(number: Int): Task[Int] = if number < 5 then
    ZIO.sleep(Duration.fromMillis(1000)) *> ZIO.fail(
      new ArithmeticException(s"Number $number too small")
    )
  else ZIO.succeed(number)

  def errorCode = ZIO.succeed(12)

  def program = for {
    f <- validate(1).fork
    r <- f.interrupt
    // r <- f.join
  } yield r

  def runAndReport = program
    .flatMap(r => Console.printLine(s"Result is $r"))
    .catchAll(err => Console.printLineError(s"Failed cause $err"))

  def run = runAndReport
