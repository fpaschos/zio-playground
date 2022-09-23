package dev.fpas.zio
package scala3

// Arbitrary stuff that I check while reading the scala 3 documentation

/*Dependent function types example from documentation see  http://dotty.epfl.ch/docs/reference/new-types/dependent-function-types-spec.html
 */
object DependentFTExample:
  trait Effect

  // Function X => Y
  abstract class Fun[-X, +Y]:
    type Eff <: Effect
    def apply(x: X): Eff ?=> Y

  class CanThrow extends Effect
  class CanIO extends Effect

  given ct: CanThrow = new CanThrow
  given ci: CanIO = new CanIO

  class IntToString extends Fun[Int, String]:
    type Eff = CanThrow
    override def apply(x: Int): Eff ?=> String = x.toString()

  class StringToInt extends Fun[String, Int]:

    type Eff = CanIO
    override def apply(x: String): Eff ?=> Int = x.length

  // def map(f: A => B)(xs: List[A]): List[B]
  def map[A, B](f: Fun[A, B])(xs: List[A]): f.Eff ?=> List[B] =
    xs.map(f.apply)

  // def mapFn[A, B]: (A => B) -> List[A] -> List[B]
  def mapFn[A, B]: (f: Fun[A, B]) => List[A] => f.Eff ?=> List[B] =
    f => xs => map(f)(xs)

  // def compose(f: A => B)(g: B => C)(x: A): C
  def compose[A, B, C](f: Fun[A, B])(g: Fun[B, C])(
      x: A
  ): f.Eff ?=> g.Eff ?=> C =
    g(f(x))

  // def composeFn: (A => B) -> (B => C) -> A -> C
  def composeFn[A, B, C]: (f: Fun[A, B]) => (g: Fun[B, C]) => A => f.Eff ?=> g.Eff ?=> C =
    f => g => x => compose(f)(g)(x)

  @main def testDependentFT =
    val i2s = new IntToString
    val s2i = new StringToInt

    assert(mapFn(i2s)(List(1, 2, 3)).mkString == "123")
    assert(composeFn(i2s)(s2i)(22) == 2)
end DependentFTExample

/* Polymorphic Function Types see  http://dotty.epfl.ch/docs/reference/new-types/polymorphic-function-types.html*/
object PolymorphicFTExample:

  enum Expr[A]:
    case Var(name: String)
    case Apply[A, B](fun: Expr[B => A], arg: Expr[B]) extends Expr[A]

  import Expr.*
  def mapSubexpressions[A](e: Expr[A])(f: [B] => Expr[B] => Expr[B]): Expr[A] =
    e match
      case Apply(fun, n) => Apply(f(fun), f(n))
      case Var(n)        => Var(n)

  @main def testPolymorhpicFT =
    val e0 = Apply(Var("f"), Var("a"))
    val e1 = mapSubexpressions(e0)(
      [B] => (se: Expr[B]) => Apply(Var[B => B]("wrap"), se)
    )

    println(e1) // Apply(Apply(Var(wrap),Var(f)),Apply(Var(wrap),Var(a)))
end PolymorphicFTExample

/* ADTs http://dotty.epfl.ch/docs/reference/enums/adts.html*/
object ADTExample:

  enum View[-T, +U] extends (T => U):
    case Refl[R](f: R => R) extends View[R, R]

    final def apply(t: T): U = this match
      case refl: Refl[r] => refl.f(t)

  val a: (Int => Int) = a => a

  val view = View.Refl(a)
  val b = view(1)

end ADTExample

/* Given Instances http://dotty.epfl.ch/docs/reference/contextual/givens.html*/
object GivenInstances:

  enum Ordering:
    case Less
    case Equal
    case Greater

  trait Ord[T]:
    def compare(a: T, b: T): Ordering

  given intOrd: Ord[Int] with
    import Ordering.*
    def compare(a: Int, b: Int): Ordering =
      if a < b then Less else if a > b then Greater else Equal

  @main def testGivenInstances() =
    val res = summon[Ord[Int]].compare(1, 2)
    assert(res == Ordering.Less, "1 Less than 2")

    def lessThan(a: Int, b: Int)(using ord: Ord[Int]): Boolean =
      ord.compare(a, b) == Ordering.Less

    assert(lessThan(1, 2), "1 Less than 2 (with using)")
end GivenInstances
