package dev.fpas.zio
import zio.*

object ErrorHandlingMain extends ZIOAppDefault:

  def run = ZIO.fail("Oh uh!").ensuring(ZIO.dieMessage("Boom!"))
