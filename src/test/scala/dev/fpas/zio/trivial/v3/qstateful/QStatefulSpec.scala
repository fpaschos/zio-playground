package dev.fpas.zio.trivial
package v3
package qstateful

import zio.*
import zio.test.*
import zio.test.Assertion.{equalTo}

import dev.fpas.zio.trivial.v3.qstateful.QStateful
import dev.fpas.zio.trivial.v3.qstateful.QStateful.*

object DoGetProtocol:
  // Messages hierarchy
  // As an enum
  enum Command[+Res]:
    case Get extends Command[Summary]
    case Do extends Command[Confirmation]

  enum Confirmation:
    case Accept extends Confirmation
    case Reject extends Confirmation

  case class Summary(value: String)

end DoGetProtocol

object Counter:

  enum Command[+A]:
    case Inc extends Command[Unit]
    case Dec extends Command[Unit]
    case Get extends Command[Summary]

  // Summary response of the counter returns current value and total commands accepted
  case class Summary(value: Int, totalCommands: Int)

  case class State(value: Int, totalCommands: Int)

  def create: Task[QStatefulRef[Command]] =
    QStateful.create(new Counter().initialized())

end Counter

class Counter:

  import Counter.*
  import Counter.Command.*
  private def initialized(state: State = State(0, 0)): Behavior[Command] =
    new Behavior[Command] {
      override def receive[A](command: Command[A]): Task[A] = command match {
        case Inc => ZIO.succeed(())
        case Dec => ZIO.succeed(())
        case Get => ZIO.succeed(Summary(state._1, state._2))
      }
    }

end Counter

object QStatefulSpec extends ZIOSpecDefault:
  def spec =
    suite("QStateful v3 basic behaviour")(
      test(
        "Definition and message processing of a simple Counter"
      ) {
        import Counter.Command.*

        for {
          ref <- Counter.create
          _ <- ref ! Inc
          summary <- ref ? Get
        } yield assert(summary)(equalTo(Counter.Summary(1, 1)))
      }
    )
