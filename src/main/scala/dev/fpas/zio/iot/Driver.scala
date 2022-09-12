package dev.fpas.zio
package iot

import zio.*
import Domain.*

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
