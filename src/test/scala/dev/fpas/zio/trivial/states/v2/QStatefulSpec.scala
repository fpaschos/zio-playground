package dev.fpas.zio.trivial.states.v2

import zio.*
import zio.test.*
import zio.test.Assertion.{equalTo}

import dev.fpas.zio.trivial.state.v2.QStateful.Behaviour
import dev.fpas.zio.trivial.state.v2.QStateful.QStatefulRef

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
                  Console.printLine("Do after 1m") *>
                  ZIO.succeed(Confirmation.Accept)
              case Get =>
                Console.printLine("Get") *>
                  ZIO.succeed(Summary("ok"))
            }
        }

        for {
          ref: QStatefulRef[Command] <- stateless.create
          _ <- (ref ? Do).fork
          _ <- (ref ? Do).fork
          _ <- TestClock.adjust(2.minute)
          summary <- ref ? Get
          output <- TestConsole.output
        } yield assert(summary)(equalTo(Summary("ok")))
          && assert(output(0))(equalTo("Do after 1m\n"))
          && assert(output(1))(equalTo("Do after 1m\n"))
          && assert(output(2))(equalTo("Get\n"))
      }
    )
