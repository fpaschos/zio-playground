package dev.fpas.zio.trivial.v3.qstateful
import zio.*

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
