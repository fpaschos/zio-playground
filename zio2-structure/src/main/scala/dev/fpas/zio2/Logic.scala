package dev.fpas.zio2

import zio.*
import zio.ZLayer.FunctionConstructor.WithOut

object Logic:

  // Mock domain/model
  case class Car(make: String, model: String, plate: String)

  // Mock error
  case class LisencePlateExists(plate: String)

  // TOP level (facade)
  class CarApi(service: CarService):
    def register(input: String): UIO[String] = for {
      rid <- Random.nextUUID
      res <- ZIO.logSpan(s"Req $rid") {
        input.split(" ", 3).toSeq match {
          case Seq(make, model, plate) =>
            val car = Car(make, model, plate)
            service.register(car).as("Car Registered").catchAll {
              case LisencePlateExists(plate) =>
                ZIO
                  .logError(
                    s"Cannot register: $car, because a car with the same license plate already exists"
                  )
                  .as("Bad request: duplicate")
              case t: Throwable =>
                ZIO
                  .logErrorCause(s"Cannot register: $car, unknown error", Cause.fail(t))
                  .as("Internal server error")
            }
          case _ => ZIO.logError(s"Bad request $input").as("Bad Request")
        }
      }
    } yield res

  object CarApi:
    val live: URLayer[CarService, CarApi] = ZLayer.fromFunction(CarApi(_))

  class CarService(db: DB, carRepo: CarRepository):
    def register(car: Car): ZIO[Any, LisencePlateExists | Throwable, Unit] =
      db.transact {
        for {
          exists <- carRepo.exists(car.plate)
          res <- if exists then ZIO.fail(LisencePlateExists(car.plate)) else carRepo.insert(car)
        } yield res
      }

  object CarService:
    val live: URLayer[DB & CarRepository, CarService] = ZLayer.fromFunction(CarService(_, _))

  class CarRepository:
    def exists(plate: String): URIO[Connection, Boolean] =
      ZIO
        .service[Connection]
        .flatMap(c =>
          ZIO.logInfo(s"Using ${c.id} with exists query plate: $plate") *> ZIO.succeed(
            plate.startsWith("ION")
          )
        )
        .delay(100.millis) // Emulate query delay

    def insert(car: Car): URIO[Connection, Unit] =
      ZIO
        .service[Connection]
        .flatMap(c => ZIO.logInfo(s"Using ${c.id} to insert car: $car") *> ZIO.succeed(()))
        .delay(200.millis)

  end CarRepository

  object CarRepository:
    val live = ZLayer(ZIO.succeed(new CarRepository))

  // Object providing data base access using connection and transactioons
  class DB(pool: ConnectionPool):

    // Acquires a connection from the pool in a resource safe scoped manner
    private def connection: ZIO[Scope, Throwable, Connection] =
      ZIO.acquireRelease(pool.obtain)(c =>
        pool
          .release(c)
          .catchAll(t => ZIO.logErrorCause("Exception when releasing a connection", Cause.fail(t)))
      )

    // Emulating running a transaction using connection from the environment
    def transact[R, E, A](dbProgram: ZIO[Connection & R, E, A]): ZIO[R, E | Throwable, A] =
      ZIO.scoped {
        connection.flatMap { c =>
          dbProgram.provideSomeLayer(
            ZLayer.succeed(c)
          ) // Provide the connection as dependency to the wrapped dbProgram
        }
      }
  end DB

  object DB:
    val live: ZLayer[ConnectionPool, Nothing, DB] = ZLayer.fromZIO(for {
      a <- ZIO.service[ConnectionPool]
    } yield DB(a))

  // Mock connection
  case class Connection(id: String)

  // Mock connection pool
  class ConnectionPool(ref: Ref[Vector[Connection]]) {

    // Obtain a connection effect
    def obtain: Task[Connection] = ref
      .modify {
        case h +: rest => (h, rest)
        case _         => throw new IllegalStateException("No connection available!")
      }
      .tap(c => ZIO.logInfo(s"Obtained connection ${c.id}"))

    // Release a connection effect
    def release(c: Connection): Task[Unit] = ref
      .modify(cs => ((), cs :+ c))
      .tap(_ => ZIO.logInfo(s"Released connection ${c.id}"))

    // Close connection (never fais)
    def close: UIO[Unit] = ref
      .modify(cs => (cs, Vector.empty))
      .flatMap(cs => ZIO.foreachDiscard(cs)(c => ZIO.logInfo(s"Discarding connection ${c.id}")))
  }

  object ConnectionPool:

    // Define live ConnectionPool implementation (used for DI)
    val live: ULayer[ConnectionPool] =
      ZLayer.scoped(
        ZIO.acquireRelease {
          // Create a connection pool with mock connections on acquire
          Ref
            .make(
              Vector("Conn1", "Conn2", "Conn3")
                .map(Connection.apply)
            )
            .map(ConnectionPool(_))
        }(_.close)
      ) // Close the connection pool on release

end Logic
