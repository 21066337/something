package ch.makery.something.models

import akka.actor.typed.ActorRef
import ch.makery.something.Actor.LobbyServer

import scala.collection.mutable.ListBuffer

case class Lobby(LobbyIDS: String, nameS: String) extends Serializable {

  private var lobbyID: String = LobbyIDS
  private var players: ListBuffer[GameClientModel] = ListBuffer.empty[GameClientModel]
  private var numberOfPlayer: Int = 1
  private val lobbyMasterName: String = nameS

  def getLobbyID: String = {
    lobbyID
  }

  def getLobbyMasterName: String = {
    lobbyMasterName
  }

  def getNumberOfPlayer: Int = {
    numberOfPlayer
  }

  def setNumberOfPlayer(number: Int): Unit = {
    numberOfPlayer += number
  }

  def addPlayer(player: GameClientModel): Unit = {
    players += player
  }

  def removePlayer(player: GameClientModel): Unit = {
    players -= player
  }

  def getPlayers: ListBuffer[GameClientModel] = {
    players
  }

  def validateCode(code: String): Boolean = {
    code == lobbyID
  }

  def createCode(code: String): Unit = {
    lobbyID = code
  }

//  def isLobbyMaster(player: Player): Boolean = {
//    player. == nameS
//  }
//
//  def removePlayerFromLobby(player: Player): Unit = {
//    if (isLobbyMaster(player)) {
//      throw new Exception("LobbyMaster cannot be removed")
//    } else {
//      removePlayer(player)
//    }
//  }

//  def quitLobby(player: Player): Unit = {
//    if (isLobbyMaster(player)) {
//      //deactivate lobby
//      //remove player as lobby master
//    } else {
//      removePlayer(player)
//    }
//  }
}
