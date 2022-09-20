package dev.fpas.zio.iot

import zio.*

import Domain._

trait DriversRegistry:

  // TODO create drive
  def createDriver(info: DriverInfo): Task[Driver]

  /** Update the status of driver */
  def updateStatusOf(id: UserId, status: DriverStatus): Task[Unit]

  def getStatusOf(id: UserId): Task[Unit]

private case class CachedState(
    val drivers: Map[UserId, Driver] = Map.empty,
    val infos: Map[UserId, DriverInfo] = Map.empty
)

// TODO how to pass driver factory???
class CachedDriversRegistry(private val cache: Ref[CachedState])
    extends DriversRegistry:

  override def createDriver(info: DriverInfo) = ???

  override def updateStatusOf(id: UserId, status: DriverStatus): Task[Unit] =
    ???

  override def getStatusOf(id: UserId): Task[Unit] = ???

  private def initializeDrivers(infos: Seq[DriverInfo]): Task[Unit] = ???

object CachedDriversRegistry:
  def make(infos: Seq[DriverInfo] = Seq.empty): Task[CachedDriversRegistry] =
    for {
      initial <- Ref.make(CachedState())
      registry = CachedDriversRegistry(initial)
      _ <- registry.initializeDrivers(infos)
    } yield registry
