package dev.fpas.zio.trivial.states.v2

object Experiments:

  // Messages hierarchy
  // As an enum
  enum CommandEnum[+T]:
    case Get extends CommandEnum[Summary]
    case Do extends CommandEnum[ConfirmEnum]

  case class Summary(reason: String)

  enum ConfirmEnum:
    case Accept extends ConfirmEnum
    case Reject extends ConfirmEnum

  // As a sealed trait
  sealed trait CommandTrait[+T]
  object CommandTrait:
    case object Get extends CommandTrait[Summary]
    case object Do extends CommandTrait[ConfirmTrait]

  sealed trait ConfirmTrait
  object ConfirmTrait:
    case object Accept extends ConfirmTrait
    case object Reject extends ConfirmTrait

  // Type lambda experiment

  import zio.*
  type Pending[M[_], A] = (M[A], Promise[Throwable, A])
  type PendingT[M[_]] = {
    type A
    val value: (M[A], Promise[Throwable, A])
  }

  case class PendingC[M[_], A](m: M[A], p: Promise[Throwable, A])

  trait Askable[-M[+_]] {
    def ?[A](ma: M[A]): Task[A]
  }

  class Concrete[-M[+_]](
      // queue: Queue[Pending[M, A]],
      queueC: Queue[PendingC[M, _]]
      // queueT: Queue[PendingT[M]]
  ):

    def ask[A](m: M[A]): Task[A] = for {
      p <- Promise.make[Throwable, A]
      _ <- queueC.offer(PendingC(m, p))
      value <- p.await
    } yield value

    // Magic
    // def ask2[R >: A, M1[_] <: M[A]](m: M1[R]): Task[R] = ask(m)

    def tell[R](m: M[R]): Task[Unit] = ???

  end Concrete

  type Test = Any
  object Examples:
    import CommandTrait.*

    def example: Task[Unit] = for {
      queue <- Queue.unbounded[PendingC[CommandTrait, ?]]
      c = Concrete(queue)
      askDo <- c.ask(Do)
      askGet <- c.ask(Get)
      // askDo <- c.ask(Get)

      // askDoTyped <- c.ask2(Do)
      // getTask <- c.ask2(Get)
      // doTaskTell <- c.tell(Do)

    } yield ()

    def checkGenericsWidened: Unit = {
      class Container[-M[+_], A]:
        def methodInternal(m: M[A]): A = ???

        def method[M1[R] <: M[A], R >: A](m: M1[R]): R =
          methodInternal(m)

        def method1[M1[_] <: M[A]](m: M1[A]): A =
          methodInternal(m)

      class FullVarianceContainer[-M[+_], +A]:
        def methodInternal[R >: A](m: M[R]): A = ???
        def methodInternal2[R >: A](m: M[R]): R = ???
      end FullVarianceContainer

      // Usage
      val fvc = FullVarianceContainer[CommandEnum, ConfirmEnum]
      val a1 = fvc.methodInternal2(CommandEnum.Do)
      val a2 = fvc.methodInternal(CommandEnum.Do)

      class SimpleContainer[-M]:
        def internalMethod(m: M): Unit = ???
        def method[M1 <: M](m: M1): Unit = internalMethod(m)
      end SimpleContainer

      // Usage
      val c = Container[CommandEnum, ConfirmEnum]
      val a = c.methodInternal(CommandEnum.Do)
      // val b = c.method(CommandEnum.Do) // Bad???
      // val c = c.method1(CommandEnum.Do)

      val sc = SimpleContainer[ConfirmEnum]
      sc.method[ConfirmEnum](ConfirmEnum.Accept)
    }

    def checkSubtypes =
      summon[CommandEnum.Do.type <:< CommandEnum[ConfirmEnum]]
      summon[CommandEnum.Get.type <:< CommandEnum[Summary]]

      summon[CommandTrait.Do.type <:< CommandTrait[ConfirmTrait]]
      summon[CommandTrait.Get.type <:< CommandTrait[Summary]]
  end Examples

  object CheckWildcards:

    // trait Sample {
    //   def run[A] = for {
    //     list: List[?] <- Some[List[?]](List("1"))
    //     e <- exec(list)
    //     et <- execTyped(list)

    //   } yield Wrapper(et)

    //   def exec(list: List[?]): Option[List[?]] = ???

    //   def execTyped[A](list: List[A]): Option[List[A]] = ???
    // }

    // final case class Wrapper[M[_]](val m: M[?])

    // More complex

    case class Element[M[_], A](e: M[A])

    final case class ElementsWrapper[M[_]](elems: List[Element[M, ?]]) {
      def add[A](m: M[A]): Option[M[A]] =
        val a = elems :+ m // Emulates operation
        Some(m)
    }

    def maybeList[A]: Option[List[A]] = None

    trait ComplexSample[M[_]] {
      def create[A] = for {
        l <- maybeList[Element[M, ?]]
        _ <- op(l)

      } yield ElementsWrapper(l)

      def op[A](list: List[Element[M, ?]]): Option[A] = None
    }
  end CheckWildcards
