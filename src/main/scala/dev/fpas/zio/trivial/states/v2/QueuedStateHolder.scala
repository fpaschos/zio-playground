package dev.fpas.zio
package trivial
package state
package v2

// QStateful
// QStatefulRef

import zio.*
type Behaviour = (Msg, State) => ZIO[Any, Nothing, State]

enum Envelop[P] {
  case Simple(payload: P)
  case WithReply(payload: P)
}
// case class Envelop[Msg](msg: Msg, promise: Promise[Throwable, Any])

case class State(name: String, counter: Int)

enum Msg:
  case Inc extends Msg
  case Dec extends Msg
  case Get extends Msg

/** First attemt to create something like an actor a QueuedStateHolder That is a
  * service that holds INTERNAL STATE is supported by an async QUEUE receives
  * messages, handles errors and has a well defined protocol
  *
  * Problems with this implementation
  *   - Not generic enough
  *   - Does not handle holder fiber termination
  *   - Cannot explicit terminate
  *   - Termination notification (death watch)
  *   - Does not handle behaviour errors and errors in general
  *   - Does not support ask
  *   - Testing
  *
  * Addressed problems
  *   - TODO Support ask
  *   - TODO Generic implementation
  */
class QueuedStateHolder(private val queue: Queue[Envelop[Msg]]):
  import Envelop.*

  // def ask[E, A](msg: Msg): IO[E, A] = for {
  //   p <- Promise.make[E, A]

  //   r <- p.await
  // } yield r

  def tell[E, A](msg: Msg): Task[Unit] = queue.offer(Simple(msg)).unit

  def inc: ZIO[Any, Throwable, Any] = tell(Msg.Inc)
  def dec = tell(Msg.Dec)
  def get = tell(Msg.Get)
end QueuedStateHolder

object QueuedStateHolder:
  import Msg.*

  def create(state: State, behaviour: Behaviour) = for {
    queue <- Queue.unbounded[Envelop[Msg]]
    _ <- run(queue, state, behaviour).fork
  } yield QueuedStateHolder(queue)

  private def run(
      queue: Queue[Envelop[Msg]],
      state: State,
      behaviour: Behaviour
  ): ZIO[Any, Nothing, State] =
    ZIO.never

    queue.take
      .flatMap(envelop =>
        envelop match {
          case Envelop.Simple(msg) =>
            behaviour(msg, state)
          case _ => ZIO.succeed(state)
        }
      )
      .flatMap(state => run(queue, state, behaviour)) // recursive run forever

val behaviour: Behaviour = (msg, state: State) =>
  val res = msg match {
    case Msg.Inc =>
      val s = state.copy(counter = state.counter + 1)
      Console.printLine(s"${s.name}: Inc c = ${s.counter}") *> ZIO.succeed(s)

    case Msg.Dec =>
      val s = state.copy(counter = state.counter - 1)
      Console.printLine(s"${s.name} Dec c = ${s.counter}") *> ZIO.succeed(s)

    case Msg.Get =>
      Console.printLine(s"${state.name} State c = ${state.counter}") *> ZIO
        .succeed(
          state
        )
  }
  res.catchAll(err =>
    Console.printLineError(s"Error $err").orElse(ZIO.unit) *> ZIO
      .succeed(
        state
      )
  )
  //

object ExampleUsage extends ZIOAppDefault:
  val program = for
    _ <- Console.printLine("Starting state holder")
    actor1 <- QueuedStateHolder.create(State("actor1", 0), behaviour)
    actor2 <- QueuedStateHolder.create(State("actor2", 100), behaviour)
    _ <- actor1.inc.repeatN(99)
      <&> actor2.dec.repeatN(99) // Execute in parallel
    _ <- actor1.get
    _ <- actor2.get
    _ <- ZIO.sleep(
      2.seconds
    ) // Block main fiber here in order to see ALL the results
  yield ()

  // expected resalts actor1 c = 100 actor2 c = 0
  def run = program
