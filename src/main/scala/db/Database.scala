package db

import cats.Applicative
import cats.effect.Async
import config.DatabaseConfig
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.language.higherKinds

object Database {
  def transactor[F[_]: Async](config: DatabaseConfig): F[HikariTransactor[F]] = {
    HikariTransactor.newHikariTransactor[F](config.driver, config.url, config.user, config.password)
  }

  def initialize[F[_]: Applicative](transactor: HikariTransactor[F])(implicit A: Applicative[F]): F[Unit] = {
    transactor.configure { datasource =>
      A.point {
        val flyWay = new Flyway()
        flyWay.setDataSource(datasource)
        flyWay.migrate()
      }
    }
  }
}
