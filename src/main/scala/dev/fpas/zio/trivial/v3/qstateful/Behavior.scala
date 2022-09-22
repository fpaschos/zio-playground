package dev.fpas.zio.trivial.v3.qstateful
import zio.*

private[qstateful] case class PendingMessage[M[_], A](
    m: M[A],
    p: Promise[Throwable, A]
)

trait Behavior[-M[+_]]:
  def receive[A](command: M[A]): Task[(A, Behavior[M])]

  private[qstateful] def create: Task[QStatefulRef[M]] = for {
    queue <- Queue.unbounded[PendingMessage[M, Any]]
    _ <- run(queue, this).fork
  } yield new InternalRef[M](queue)

  private def run(
      queue: Queue[PendingMessage[M, Any]],
      behaviour: Behavior[M]
  ): Task[Unit] =
    (for {
      pending <- queue.take
      next <- behaviour.receive(pending.m)
      (res, b) = next
      _ <- pending.p.succeed(res)
    } yield run(queue, b)).flatten

end Behavior
