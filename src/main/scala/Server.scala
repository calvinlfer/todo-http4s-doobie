import cats.effect.IO
import config.Config
import db.Database
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import repository.TodoRepository
import service.TodoService
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config      <- Stream.eval(Config.load[IO]())
      transactor  <- Stream.eval(Database.transactor[IO](config.database))
      _           <- Stream.eval(Database.initialize[IO](transactor))
      exitCode    <- BlazeBuilder[IO]
                        .bindHttp(config.server.port, config.server.host)
                        .mountService(new TodoService[IO](new TodoRepository[IO](transactor)).service, "/")
                        .serve
    } yield exitCode
  }
}
