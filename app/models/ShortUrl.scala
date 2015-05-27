package models

import actors.UniqueIDActor
import akka.actor.ActorSystem.Settings
import akka.actor._
import akka.dispatch.{Dispatchers, Mailboxes}
import akka.event.{LoggingAdapter, EventStream}
import akka.util.Timeout
import play.api.db.DB
import play.api.libs.json.Json
import anorm._
import play.api.Play.current
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, ExecutionContext, Future}

case class ShortUrl(hash: String, fullUrl: String)

object ShortUrl {
  implicit val fmt = Json.format[ShortUrl]

  val system = ActorSystem("MySystem")
  val uniqueIdActor = system.actorOf(UniqueIDActor.props)

  def create(fullUrl: String)(implicit ec: ExecutionContext): Future[ShortUrl] = {
    // Get the next Unique ID from the managing actor
    implicit val timeout = Timeout(5 seconds)
    val fUniqueId = (uniqueIdActor ? UniqueIDActor.GetNextID).mapTo[Int]

    // Hash the ID and save it to the database before returning
    fUniqueId map { uniqueId =>
      val hash = encodeId(uniqueId)
      val shortUrl = ShortUrl(hash, fullUrl)
      ShortUrl.saveUrl(shortUrl)
      shortUrl
    }
  }

  private val HASH_INDEX = "abcdefghijklmnopqrstuvwxyz"
  private def encodeId(input: Int, acc: String = ""): String = {
    if (input == 0)
      acc
    else
      encodeId(input / HASH_INDEX.length, acc + HASH_INDEX.charAt(input % HASH_INDEX.length))
  }

  private def saveUrl(shortUrl: ShortUrl) = {
    DB.withConnection { implicit c =>
      val sql = SQL("INSERT INTO shorturls (short, fullurl) VALUES ({short}, {fullurl});")
        .on("short" -> shortUrl.hash, "fullurl" -> shortUrl.fullUrl)
      sql.executeInsert()
    }
  }

  def lookup(hash: String)(implicit ec: ExecutionContext): Future[Option[ShortUrl]] = {
    Future {
      findByHash(hash)
    }
  }

  private def findByHash(hash: String): Option[ShortUrl] = {
    DB.withConnection { implicit c =>
      val sql = SQL("SELECT short, fullurl FROM shorturls WHERE short = {short} LIMIT 1;")
        .on("short" -> hash)

      sql().headOption.map { row =>
        ShortUrl(row[String]("short"), row[String]("fullurl"))
      }
    }
  }
}