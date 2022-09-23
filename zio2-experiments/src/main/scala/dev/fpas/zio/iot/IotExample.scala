package dev.fpas.zio
package iot

import zio.*
import zio.stream.*
import Domain.*

//Flow
// 1. Create Drivers
// 2. Read from stream -> delegate via DriverManager -> Driver

// Dummy for now generate 100 driver info
// Suppose we do exernal call here
def fetchDriversInfo(): UIO[Seq[DriverInfo]] = {
  ZStream
    .range(1, 100, 100)
    .map { id =>
      val name = s"vehicle-$id"
      DriverInfo(name, id, name, Vehicle.Car)
    }
    .runCollect
}

object IotExample extends ZIOAppDefault:

  val program = for {
    infos <- fetchDriversInfo()
    registry <- CachedDriversRegistry.make(infos)
  } yield ()

  def run = program

// Domain
