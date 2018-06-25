package repository

import cats.Monad
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{Importance, Todo, TodoNotFoundError}

import scala.language.higherKinds

class TodoRepository[F[_]](transactor: Transactor[F])(implicit M: Monad[F]) {
  private implicit val importanceMeta: Meta[Importance] = Meta[String].xmap(Importance.unsafeFromString, _.value)

  def getTodos: Stream[F, Todo] = {
    sql"SELECT id, description, importance FROM todo".query[Todo].stream.transact(transactor)
  }

  def getTodo(id: Long): F[Either[TodoNotFoundError.type, Todo]] = {
    sql"SELECT id, description, importance FROM todo WHERE id = $id"
      .query[Todo].option
      .transact(transactor).map {
      case Some(todo) => Right(todo)
      case None => Left(TodoNotFoundError)
    }
  }

  def createTodo(todo: Todo): F[Todo] = {
    sql"INSERT INTO todo (description, importance) VALUES (${todo.description}, ${todo.importance})".update.withUniqueGeneratedKeys[Long]("id").transact(transactor).map { id =>
      todo.copy(id = Some(id))
    }
  }

  def deleteTodo(id: Long): F[Either[TodoNotFoundError.type, Unit]] = {
    sql"DELETE FROM todo WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(())
      } else {
        Left(TodoNotFoundError)
      }
    }
  }

  def updateTodo(id: Long, todo: Todo): F[Either[TodoNotFoundError.type, Todo]] = {
    sql"UPDATE todo SET description = ${todo.description}, importance = ${todo.importance} WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(todo.copy(id = Some(id)))
      } else {
        Left(TodoNotFoundError)
      }
    }
  }
}
