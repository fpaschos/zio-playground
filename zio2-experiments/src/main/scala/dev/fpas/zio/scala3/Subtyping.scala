package dev.fpas.zio.scala3

object Subtyping:
  type Id = Int

  trait Record {
    val getId: Id
  }

  def example[Id, T <: { def getId: Id }](param: T): Unit = {
    import reflect.Selectable.reflectiveSelectable
    param.getId
  }

  // Try call

  val record: Record = ???
  example(record)

  trait Identity[I] {
    def getId: I
  }

  // Using traits
  def exampleTrait[T <: Identity[Int]](param: T): Unit = {
    println(param.getId)
  }

  val ident: Identity[String] = ???
  example(ident)

  case class Person(id: Int, name: String) extends Identity[Int] {
    override def getId: Int = id
  }
  // Call
  val p = Person(0, "Foo")
  exampleTrait(p) // Works
  example(p) // Also works!!!

  case class Payment(id: Int, amount: BigDecimal)

  val payment = Payment(id = 1, amount = 2323)

  // trait WithId[Id, A] {
  //   extension (a: A) def id: Id
  // }

  // given WithId[Int, Payment] with
  //   extension (p: Payment) def id = p.id

  // given [Id, A]: Conversion[WithId[Id, A], Identity[Id]] with
  //   def apply(withId: WithId[Id, A]): Identity[Id] = new Identity[Id] {
  //     override def getId: Id = summon(withId).id
  //   }
