package dev.fpas.zio
package trivial
package v3
package qstateful

// QStateful
// QStatefulRef

import zio.*

/** Third attemt to create something like an actor a QStateful
  *
  * That is a service that holds INTERNAL STATE is supported by an async QUEUE
  * receives messages, handles errors and has a well defined protocol
  *
  * Problems with this implementation
  *   - Does not handle holder fiber termination
  *   - Cannot explicitly terminate
  *   - Termination notification (death watch)
  *   - Does not handle behaviour errors and errors in general
  *   - Testing
  *
  * Addressed problems
  *   - Support ask
  *   - Generic implementation
  */
object QStateful:

  def create[M[+_]](b: Behavior[M]): Task[QStatefulRef[M]] =
    b.create

end QStateful
