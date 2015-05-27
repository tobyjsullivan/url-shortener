package controllers

import models.ShortUrl
import play.api._
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def redirect(hash: String) = Cached("redirect_"+hash) {
    Action.async {
      val fShortUrl = ShortUrl.lookup(hash)

      fShortUrl.map {
        case None => NotFound("That URL could not be found")
        case Some(shortUrl) => Redirect(shortUrl.fullUrl, 301)
      }
    }
  }

  def shorten = Action.async { request =>
    request.body.asJson.map(js => (js \ "fullurl").as[String]) match {
      case None => Future.successful(BadRequest("Must specify a fullrequest parameter"))
      case Some(fullUrl) => {
        val fShortUrl = ShortUrl.create(fullUrl)

        fShortUrl.map { shortUrl =>
          Ok(Json.obj("result" -> shortUrl))
        }
      }
    }

  }

}