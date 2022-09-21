package dev.fpas.zio
package trivial
package v3
package qstateful

// QStateful
// QStatefulRef

import zio.*

/** Third attemt to create something like an actor a QStateful
  *
  * That is a service that holds INTERNAL STATE is supported by an async QUEUE
  * receives messages, handles errors and has a well defined protocol
  *
  * Problems with this implementation
  *   - DOES NOT SUPPORT INTERNAL STATE FOR NOW !!
  *   - Does not handle holder fiber termination
  *   - Cannot explicitly terminate
  *   - Termination notification (death watch)
  *   - Does not handle behaviour errors and errors in general
  *   - Testing
  *
  * Addressed problems
  *   - Support ask
  *   - Generic implementation
  */
object QStateful:

  def create[M[+_]](b: Behaviour[M]): Task[QStatefulRef[M]] =
    b.create

  private[qstateful] case class PendingMessage[M[_], A](
      m: M[A],
      p: Promise[Throwable, A]
  )

  trait Behaviour[-M[+_]]:
    def receive[A](command: M[A]): Task[A]

    private[qstateful] def create: Task[QStatefulRef[M]] = for {
      queue <- Queue.unbounded[PendingMessage[M, Any]]
      _ <- run(queue, this).fork
    } yield new InternalRef[M](queue)

    private def run(
        queue: Queue[PendingMessage[M, Any]],
        behaviour: Behaviour[M]
    ): Task[Unit] =
      (for {
        pending <- queue.take
        res <- behaviour.receive(pending.m)
        _ <- pending.p.succeed(res)
      } yield run(queue, behaviour)).flatten

  end Behaviour

  trait QStatefulRef[-M[+_]] {
    def ?[A](ma: M[A]): Task[A]
    def ![A](ma: M[A]): Task[Unit]
  }

  private[qstateful] final class InternalRef[-M[+_]](
      queue: Queue[PendingMessage[M, Any]]
  ) extends QStatefulRef[M]:

    override def ![A](ma: M[A]): Task[Unit] = tell(ma)

    override def ?[A](ma: M[A]): Task[A] = ask(ma)

    def ask[A](ma: M[A]): Task[A] = for {
      p <- Promise.make[Throwable, Any]
      _ <- queue.offer(PendingMessage(ma, p))
      value <- p.await
    } yield value.asInstanceOf[A]

    def tell[A](ma: M[A]): Task[Unit] = for {
      p <- Promise.make[Throwable, Any]
      _ <- queue.offer(PendingMessage(ma, p))
    } yield ()

    // def ?[A1 :> A](ma: M[A1]): Task[A1] = ???

  end InternalRef

end QStateful
