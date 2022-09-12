package dev.fpas.zio
package iot

import java.time.Instant

object Domain:

  type UserId = String
  type DriverId = Int

  enum Vehicle:
    case Car
    case Bike

  case class DriverInfo(
      id: UserId,
      driverId: DriverId,
      name: String,
      vehicle: Vehicle
  )

  case class DriverStatus(id: UserId, lat: Double, lng: Double, time: Instant)
