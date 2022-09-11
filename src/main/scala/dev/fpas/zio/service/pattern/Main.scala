package dev.fpas.zio.service.pattern
import zio.*
object Main extends ZIOAppDefault:
  import repos.*

  val app =
    for {
      doc <-
        DocRepo.save(
          Doc(
            None,
            "doc1",
            "content".getBytes()
          )
        )
      doc <- DocRepo.get(doc.id.get) // Never fails
      _ <- Console.printLine(
        s"""
          |Downloaded the document with ${doc.id} id:
          |  title: ${doc.title}
          |""".stripMargin
      )
    } yield ()

  def run =
    app.provide(
      DocRepoImpl.layer,
      InMemoryBlobStorage.layer,
      InMemoryMetadataRepo.layer
    )
