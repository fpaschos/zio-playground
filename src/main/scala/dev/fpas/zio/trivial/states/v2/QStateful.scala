package dev.fpas.zio
package trivial
package state
package v2

// QStateful
// QStatefulRef

import zio.*

/** Second attemt to create something like an actor a QStateful
  *
  * That is a service that holds INTERNAL STATE is supported by an async QUEUE
  * receives messages, handles errors and has a well defined protocol
  *
  * Problems with this implementation
  *   - Does not handle holder fiber termination
  *   - Cannot explicit terminate
  *   - Termination notification (death watch)
  *   - Does not handle behaviour errors and errors in general
  *   - Testing
  *
  * Addressed problems
  *   - Support ask
  *   - Generic implementation
  */
object QStateful:

  // TODO ???
  // def create[M[_]](b: Behaviour[M]) = b.create

  private[v2] case class PendingMessage[M[_], A](
      m: M[A],
      p: Promise[Throwable, A]
  )

  trait Behaviour[-M[+_]]:
    def receive[A](command: M[A]): Task[A]

    // private def process[A]: Task[Unit] = ???

    def create: Task[QStatefulRef[M]] = for {
      queue <- Queue.unbounded[PendingMessage[M, ?]]
      _ <- run(queue, this).fork
    } yield new InternalRef[M](queue)

    private def run[A](
        queue: Queue[PendingMessage[M, ?]],
        behaviour: Behaviour[M]
    ): Task[Unit] =
      (for {
        pending <- queue.take
        res <- behaviour.receive(pending.m)
        _ <- pending.p.succeed(res)
      } yield run[A](queue, behaviour)).flatten

  end Behaviour

  trait QStatefulRef[-M[+_]] {
    def ?[A](ma: M[A]): Task[A]
  }

  private[v2] final class InternalRef[-M[+_]](
      queue: Queue[PendingMessage[M, ?]]
  ) extends QStatefulRef[M]:

    override def ?[A](ma: M[A]): Task[A] = ask(ma)

    def ask[A](ma: M[A]): Task[A] = for {
      p <- Promise.make[Throwable, A]
      _ <- queue.offer(PendingMessage(ma, p))
      value <- p.await
    } yield value

    // def ?[A1 :> A](ma: M[A1]): Task[A1] = ???

  end InternalRef

end QStateful
