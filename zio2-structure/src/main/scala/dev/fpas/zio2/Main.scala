package dev.fpas.zio2

import zio.*

object Main extends ZIOAppDefault:

  import Logic.*

  def run =

    def program(api: CarApi) = for {
      _ <- api.register("Toyota Corolla WE98765").flatMap(Console.printLine(_))
      _ <- api.register("VW Golf WN12345").flatMap(Console.printLine(_))
      _ <- api.register("Tesla").flatMap(Console.printLine(_))
    } yield ()

    ZLayer
      .make[CarApi](
        CarApi.live,
        CarService.live,
        CarRepository.live,
        DB.live,
        ConnectionPool.live
      )
      .build
      .map(_.get[CarApi])
      .flatMap(program)
