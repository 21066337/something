package ch.makery.something.models

import akka.actor.typed.ActorRef
import ch.makery.something.ClientGuardian
import com.hep88.protocol.JsonSerializable
import scala.collection.mutable.ListBuffer

case class User(userNameS: String, ref: ActorRef[ClientGuardian.Command]) extends JsonSerializable {

  private val userName = userNameS

  def getPlayerName(): String = {
    userName
  }

}
