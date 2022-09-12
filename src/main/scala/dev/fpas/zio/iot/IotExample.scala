package dev.fpas.zio
package iot

import zio.*
import java.time.Instant

object IotExample:

  def run = ???

// Domain
type UserId = Int
case class DriverStatus(id: UserId, lat: Double, lng: Double, time: Instant)

/** Questions
  *   - How to implement internal state -> Lock free data structures
  *   - How to implement concurrency / parallelism -> Light weight async
  *   - How to implement COMPLEX state machine -> Expressive and composable
  *     (ZIO)
  *   - Gracefull finalization -> ZIO
  *   - Error Handling Domain & Operational -> ZIO
  */
trait Driver:

  /** Update the status of driver */
  def status(status: DriverStatus): Task[Unit]

  def getStatus(): Task[DriverStatus]

class DriverImpl extends Driver:

  override def status(status: DriverStatus): Task[Unit] = ???

  override def getStatus(): Task[DriverStatus] = ???

object DriverImpl:
  def create() = ???

class DriverManager:
  // Dummy state implementation
  var drivers: Map[UserId, Driver] = ???

  // TODO create drive
  def create(): Task[Driver] = ???

  /** Update the status of driver */
  def status(id: UserId, status: DriverStatus): Task[Unit] = ???

  def getStatus(id: UserId): Task[Unit] = ???

//Flow
// 1. Create Drivers
// 2. Read rabbit message -> delegate via DriverManager -> Driver
