package actors

import akka.actor._
import play.api.libs.ws.WS
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UniqueIDActor extends Actor {
  def receive = {
    case UniqueIDActor.GetNextID =>
      val mySender = sender()
      generateNextID() map { id =>
        mySender ! id
      }
  }

  private val countEndpointURL = "http://count.io/vb/urlshortener/hash+"
  private val countRequestHolder = WS.url(countEndpointURL)

  private def generateNextID(): Future[Int] = {
    val fResponse = countRequestHolder.post("")
    fResponse.map { response =>
      (response.json \ "count").as[Int]
    }
  }
}

object UniqueIDActor {
  def props = Props(new UniqueIDActor)

  case object GetNextID
}