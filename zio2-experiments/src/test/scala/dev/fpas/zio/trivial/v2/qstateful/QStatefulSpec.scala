package dev.fpas.zio.trivial.v2.qstatful

import zio.*
import zio.test.*
import zio.test.Assertion.{equalTo}

import dev.fpas.zio.trivial.v2.qstateful.QStateful
import dev.fpas.zio.trivial.v2.qstateful.QStateful.*

object DoGetProtocol:
  // Messages hierarchy
  // As an enum
  enum Command[+R]:
    case Get extends Command[Summary]
    case Do extends Command[Confirmation]

  enum Confirmation:
    case Accept extends Confirmation
    case Reject extends Confirmation

  case class Summary(value: String)

end DoGetProtocol

object QStatefulSpec extends ZIOSpecDefault:
  def spec =
    suite("QStateful v2 basic behaviour")(
      test(
        "Definition and message processing of stateless behaviour with time delays"
      ) {

        import DoGetProtocol.*
        import Command.*
        val stateless = new Behaviour[Command] {
          override def receive[A](command: Command[A]): Task[A] =
            command match {
              case Do =>
                ZIO.sleep(1.minute) *>
                  Console.print(".") *>
                  ZIO.succeed(Confirmation.Accept)
              case Get =>
                Console.print(".") *>
                  ZIO.succeed(Summary("ok"))
            }
        }

        for {
          ref: QStatefulRef[Command] <- QStateful.create(stateless)
          _ <- (ref ? Do).fork
          _ <- (ref ? Do).fork
          _ <- TestClock.adjust(2.minute)
          summary <- ref ? Get
          _ <- ref ! Get
          _ <- ref ! Get
          output <- TestConsole.output
        } yield assert(summary)(equalTo(Summary("ok")))
          && assert(output(0))(equalTo("."))
          && assert(output(1))(equalTo("."))
          && assert(output(2))(equalTo("."))
      }
    )
