//package ch.makery.something.utils
//
//import ch.makery.something.controllers.LobbyController
//import ch.makery.something.models.Lobby
//import ch.makery.something.models.Player
//
//import scala.collection.mutable.ListBuffer
//
//object LobbyLogic {
//
//  private val lobbyController = new LobbyController
//
//  // Creates a new lobby and sets the lobby master, then returns the lobby
//  def createLobby(lobbyMaster: Player): Lobby = {
//    val lobby = lobbyController.createLobby(lobbyMaster)
//    lobby.setLobbyMaster(lobbyMaster)
//    lobby
//  }
//
//  // Joins a lobby using a code and returns the lobby
//  def joinLobby(code: String): Lobby = {
//    val lobbyOption = lobbyController.joinLobby(code)
//    lobbyOption match {
//      case Some(lobby) => lobby
//      case None => throw new Exception("Lobby with code not found")
//    }
//  }
//
//  // Adds a player to a lobby
//  def addPlayerToLobby(lobbyID: Int, player: Player): Unit = {
//    lobbyController.addPlayerToLobby(lobbyID, player)
//  }
//
//  // Removes a player from a lobby
//  def removePlayerFromLobby(lobbyID: Int, player: Player): Unit = {
//    lobbyController.removePlayerFromLobby(lobbyID, player)
//  }
//
//  // Returns the lobby master for a lobby
//  def getLobbyMaster(lobbyID: Int): Player = {
//    val lobbyMasterOption = lobbyController.getLobbyMaster(lobbyID)
//    lobbyMasterOption.orNull
//  }
//
//  // Checks if a player is the lobby master for a lobby
//  def isLobbyMaster(lobbyID: Int, player: Player): Boolean = {
//    lobbyController.isLobbyMaster(lobbyID, player)
//  }
//
//  // Returns a list of players in a lobby
//  def getPlayersInLobby(lobbyID: Int): ListBuffer[Player] = {
//    lobbyController.getPlayersInLobby(lobbyID)
//  }
//
//  // Validates a code to join the lobby
//  def validateCode(code: String): Boolean = {
//    lobbyController.validateCode(code)
//  }
//}
