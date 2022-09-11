package dev.fpas.zio

import zio.*

// Taken from https://zio.dev/reference/services/
object EnvServicesExample extends ZIOAppDefault:

  val myApp1 = for {
    date <- Clock.currentDateTime
    _ <- ZIO.logInfo(s"Current date time is $date")
  } yield ()

  def printForEver: ZIO[Any, Throwable, Nothing] =
    Clock.currentDateTime.flatMap(t => ZIO.logInfo(t.toString))
      *> ZIO.sleep(1.second)
      *> printForEver

  val myApp2 = for {
    f <- printForEver.fork <* ZIO.sleep(5.seconds)
    _ <- f.interrupt
  } yield ()
  def run =
    myApp2.flatMap(r => ZIO.logInfo(s"Program terminated with result $r"))
