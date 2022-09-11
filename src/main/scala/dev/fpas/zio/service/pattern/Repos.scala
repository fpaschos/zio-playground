/** Example taken from https://zio.dev/reference/service-pattern/
  */
package dev.fpas.zio.service.pattern
package repos
import zio.*

type Id = String

enum Error extends Exception:
  case EntityNotFound(id: Id) extends Error

case class Doc(id: Option[Id], title: String, content: Array[Byte])

trait DocRepo:
  def get(id: Id): Task[Doc]
  def save(d: Doc): Task[Doc]

object DocRepo:
  def get(id: Id) = ZIO.serviceWithZIO[DocRepo](_.get(id))
  def save(d: Doc) = ZIO.serviceWithZIO[DocRepo](_.save(d))

class DocRepoImpl(val metaRepo: MetadataRepo, val storage: BlobStorage)
    extends DocRepo:

  override def get(id: Id): Task[Doc] =
    for
      meta <- metaRepo.get(id)
      content <- storage.get(id)
    yield Doc(id = Some(meta.id), title = meta.title, content = content)

  override def save(doc: Doc): Task[Doc] =
    for
      id <- doc.id.fold(storage.put(doc.content))(id =>
        storage.put(id, doc.content)
      )
      // Insert or update using id
      _ <- metaRepo.put(Metadata(id, doc.title))
    yield doc.copy(id = Some(id))

end DocRepoImpl

object DocRepoImpl:
  val layer: ZLayer[BlobStorage & MetadataRepo, Nothing, DocRepo] = ZLayer {
    for
      metaRepo <- ZIO.service[MetadataRepo]
      storage <- ZIO.service[BlobStorage]
    yield DocRepoImpl(metaRepo, storage)
  }

case class Metadata(id: String, title: String)

trait MetadataRepo:
  def get(id: String): Task[Metadata]
  def put(m: Metadata): Task[Metadata]

class InMemoryMetadataRepo extends MetadataRepo:
  import Error.*
  import scala.collection.*

  private val internal: mutable.Map[Id, Metadata] = mutable.Map.empty

  def get(id: String): IO[EntityNotFound, Metadata] =
    ZIO.fromOption(internal.get(id)).mapError(_ => Error.EntityNotFound(id))
  def put(m: Metadata): UIO[Metadata] =
    ZIO.succeed(internal.put(m.id, m)) *> ZIO.succeed(m)

object InMemoryMetadataRepo:
  val layer = ZLayer.succeed(new InMemoryMetadataRepo)

trait BlobStorage:
  def get(id: Id): Task[Array[Byte]]

  // Insert
  def put(content: Array[Byte]): Task[Id]

  // Update
  def put(id: Id, content: Array[Byte]): Task[Id]

class InMemoryBlobStorage extends BlobStorage:
  import Error.*
  import scala.collection.*

  private val internal: mutable.Map[Id, Array[Byte]] = mutable.Map.empty

  def get(id: Id): IO[EntityNotFound, Array[Byte]] =
    ZIO.fromOption(internal.get(id)).mapError(_ => Error.EntityNotFound(id))

  // Insert
  def put(content: Array[Byte]): UIO[Id] = for {
    id <- Random.nextInt.map(_.toString)
    _ <- ZIO.succeed(internal.put(id, content))
  } yield id

  // Update
  def put(id: Id, content: Array[Byte]): UIO[Id] =
    ZIO.succeed(internal.put(id, content)) *> ZIO.succeed(id)

object InMemoryBlobStorage:
  val layer =
    ZLayer.succeed(new InMemoryBlobStorage)
