package dev.fpas.zio.scala3

/** [Implementing Type classes](http://dotty.epfl.ch/docs/reference/contextual/type-classes.html)
  */
object ImplementingTC:

// Reader monad with and without type lambdas

  trait Functor[F[_]]:
    extension [A](x: F[A]) def map[B](f: A => B): F[B]

  trait Monad[F[_]] extends Functor[F]:
    def pure[A](x: A): F[A]

    extension [A](x: F[A])
      def flatMap[B](f: A => F[B]): F[B]

      // Implement map in terms of flatmap
      override def map[B](f: A => B): F[B] =
        // x.flatMap(f.andThen(pure)) or
        x.flatMap[B]((x: A) => pure[B](f(x)))

  // Implement Monad for Option
  given Monad[Option] with
    def pure[A](x: A): Option[A] = Option(x)

    extension [A](xo: Option[A])
      def flatMap[B](f: A => Option[B]): Option[B] = xo match {
        case None    => None
        case Some(a) => f(a)
      }

  // Reader Monad

  trait Config

  def compute(i: Int)(c: Config): String = i.toString
  def show(str: String)(c: Config): Unit = println(str)

  // Reader monad simplifies this expression (used for dependency injection of config)
  def computeAndShow(i: Int)(c: Config): Unit = show(compute(i)(c))(c)

  // First implementation
  type ConfigDependent[Result] = Config => Result // Function1[Config, Result]

  given configDependentMonad: Monad[ConfigDependent] with
    def pure[A](x: A): ConfigDependent[A] = config => x

    extension [A](x: ConfigDependent[A])
      def flatMap[B](f: A => ConfigDependent[B]): ConfigDependent[B] =
        config => f(x(config))(config)

  // Usage
  def computeD(i: Int): ConfigDependent[String] =
    summon[Monad[ConfigDependent]].pure(i.toString)

  def showD(str: String): ConfigDependent[Unit] =
    summon[Monad[ConfigDependent]].pure(println(str))

  def computeAndShowD(i: Int)(c: Config): Unit =
    computeD(i).flatMap(showD)(c)
    // or
    // (for {
    //   res <- computeD(i)
    // } yield showD(res))(c)

end ImplementingTC

object UsingTypeLambdas:
  import ImplementingTC.Monad

  // Using type lambdas
  // type CD = [Res] =>> Config => Res =:= ConfigDependent[Res]
  given readerMonad[Ctx]: Monad[[Res] =>> Ctx => Res] with
    def pure[A](x: A): Ctx => A = config => x

    extension [A](x: Ctx => A)
      def flatMap[B](f: A => Ctx => B): Ctx => B =
        config => f(x(config))(config)

  case class Reader[-Ctx, +Res](run: Ctx => Res)

  given readerM[Ctx]: Monad[[Res] =>> Reader[Ctx, Res]] with
    def pure[A](x: A): Reader[Ctx, A] = Reader(config => x)

    extension [A](x: Reader[Ctx, A])
      def flatMap[B](f: A => Reader[Ctx, B]): Reader[Ctx, B] =
        Reader(config => f(x.run(config)).run(config))

  given [Ctx, Res]: Conversion[Reader[Ctx, Res], Ctx => Res] with
    def apply(reader: Reader[Ctx, Res]) =
      reader.run

  given [Ctx, Res]: Conversion[Ctx => Res, Reader[Ctx, Res]] with
    def apply(f: Ctx => Res) = Reader(f)

  object Reader:
    def apply[Ctx, Res](f: Ctx => Res): Reader[Ctx, Res] = new Reader(f)

end UsingTypeLambdas

import ImplementingTC.{Config, compute, show}
import UsingTypeLambdas.Reader
// import UsingTypeLambdas.given

def computeAndShow(i: Int)(conf: Config) =
  // import UsingTypeLambdas.readerMonad
  // import UsingTypeLambdas.readerToRaw
  import UsingTypeLambdas.given
  def c(i: Int) = Reader(compute(i))
  def s(str: String) = Reader(show(str))
  c(i).flatMap(s)(conf)

  // val a = summon[readerM[Config]].flatMap(c(i))(s)
