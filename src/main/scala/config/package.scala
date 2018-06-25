import cats.effect.Effect
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

import scala.language.higherKinds

package object config {
  case class ServerConfig(host: String ,port: Int)
  case class DatabaseConfig(driver: String, url: String, user: String, password: String)
  case class Config(server: ServerConfig, database: DatabaseConfig)

  object Config {
    import pureconfig._
    import cats.syntax.flatMap._

    def load[F[_]](configFile: String = "application.conf")(implicit effect: Effect[F]): F[Config] = {
      effect.point {
        loadConfig[Config](ConfigFactory.load(configFile))
      }.flatMap {
        case Left(e) => effect.raiseError[Config](new ConfigReaderException[Config](e))
        case Right(config) => effect.point(config)
      }
    }
  }
}
