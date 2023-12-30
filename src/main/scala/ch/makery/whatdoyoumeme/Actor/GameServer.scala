package ch.makery.something

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.adapter._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.discovery.Discovery
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory
import scalafx.collections.ObservableHashSet
import ClientGuardian._
import ch.makery.something.AKKA.MyConfiguration
import ch.makery.something.Actor.{GameClient, LobbyServer}
import ch.makery.something.Client
import ch.makery.something.models.{Game, GameClientModel, Lobby, MemeCard, MemeDeck, PromptDeck, ServerLobbyItem, User}
import ch.makery.something.utils.GameLogic
import scalafx.application.Platform


object GameServer {
  sealed trait Command

  // ===== Protocols =====
  case class StartActor(list: List[GameClientModel]) extends Command
  case class StartGame() extends Command
  case class SendGameInstance() extends Command
  case class DealCard(playerName: String, memeCardIndex: Int) extends Command
  case class ShuffleCard(playerName: String) extends Command
  case class BroadcastChatMessage(message: String) extends Command
  case class UpdateGameObj(updatedGame: Game) extends Command
  case class checkPlayerDealtStatus(player: Option[GameClientModel]) extends Command
  case class VotePlayer(playerName: String) extends Command
  case class checkVotingEnd() extends Command
  case class EndGame() extends Command
  case class SendGameRefToPlayers() extends Command
  // ===== Protocols =====

  // Member
  //game object
  var clientPlayers = List[GameClientModel]()

  var gameObj: Game = null

  var gameLogicObj: GameLogic = null
  var memeDeck: MemeDeck = null
  var promptDeck: PromptDeck = null
  var lobbyServerRef: Option[ActorRef[LobbyServer.Command]] = None

  // Defines the behavior of the Server actor setup runs only one time
  def apply(lobbyServerRefS: ActorRef[LobbyServer.Command]): Behavior[GameServer.Command] = Behaviors.setup { context =>
    lobbyServerRef = Some(lobbyServerRefS)

    Behaviors.receiveMessage { message =>
      message match {

        // Set up game and game logic
        case StartActor(playersList) =>
          memeDeck = new MemeDeck()
          promptDeck =  new PromptDeck()

          clientPlayers = null
          clientPlayers = playersList

          gameObj = Game(clientPlayers, memeDeck, promptDeck)
          gameLogicObj = new GameLogic(gameObj)

          for (player <- clientPlayers) {
            player.gameClientRef ! GameClient.JoinGameServer(context.self)
          }

          context.self ! StartGame()
          Behaviors.same

        // Start game loop
        case StartGame() =>
          gameLogicObj.initializeGame()

          context.self ! UpdateGameObj(gameLogicObj.getUpdatedGame())

          // send initial game instance
          context.self ! SendGameRefToPlayers()
          context.self ! SendGameInstance()

          Behaviors.same

        case SendGameRefToPlayers()=>
          println("Game Initializing: sending game server actor ref")
          for (player <- clientPlayers) {
            player.gameClientRef ! GameClient.JoinGameServer(context.self)
          }
          Behaviors.same

        case SendGameInstance() =>
          println("Game Initializing: sending game instance")
          for (player <- clientPlayers){
            player.gameClientRef ! GameClient.ReceiveGameInstance(gameObj)
            println(s"${player} sent instance")
          }
          Behaviors.same

        // Deal card function
        case DealCard(playerName, memeCardIndexGiven) =>

          val playerOption = gameObj.players.find(_.getPlayerName() == playerName)
          playerOption.foreach { player =>
            gameLogicObj.dealCard(player, memeCardIndexGiven)
          }

          context.self ! BroadcastChatMessage(playerOption.get.getPlayerName() + " has dealt a card")
          context.self ! UpdateGameObj(gameLogicObj.getUpdatedGame())
          context.self ! checkPlayerDealtStatus(playerOption)


          Behaviors.same

        // Shuffle card function
        case ShuffleCard(playerName) =>

          val playerOption = gameObj.players.find(_.getPlayerName() == playerName)

          playerOption.foreach { player =>
            gameLogicObj.shuffleCards(player)
          }

          context.self ! BroadcastChatMessage(playerOption.get.getPlayerName() + " has dealt a card")
          context.self ! UpdateGameObj(gameLogicObj.getUpdatedGame())
          context.self ! checkPlayerDealtStatus(playerOption)

          Behaviors.same

        case checkPlayerDealtStatus(playerOption) =>
          // if all player has dealt
          if (gameLogicObj.checkPlayerDealStatus()) {
            // show voting scene for all players
            println("Show Voting Scene")
            clientPlayers.foreach(_.gameClientRef ! GameClient.ShowVotingScene(gameObj, gameLogicObj.playersDealtCard))
          } else {
            // show waiting view for this player only
            println("Show Waiting Scene")
            playerOption.get.gameClientRef ! GameClient.ShowWaitingLabel()
          }
          Behaviors.same

        case UpdateGameObj(updatedGame) =>
          gameObj = updatedGame
          clientPlayers = gameObj.players
          Behaviors.same

        case VotePlayer(playerName) =>
          val playerOption = gameObj.players.find(_.getPlayerName() == playerName)

          playerOption.foreach { player =>
            gameLogicObj.addVote(player)
          }

          context.self ! checkVotingEnd()
          context.self ! UpdateGameObj(gameLogicObj.getUpdatedGame())

          Behaviors.same

        case checkVotingEnd() =>
          if(gameLogicObj.checkVotingEnd()){
            gameLogicObj.handlePlayerTurns()

            context.self ! UpdateGameObj(gameLogicObj.getUpdatedGame())

            // send initial game instance
            context.self ! SendGameInstance()
          }

          Behaviors.same

        case BroadcastChatMessage(message) =>
          println(s"GameServer: Broadcasting chat message '$message' to all members.")
          clientPlayers.foreach(_.gameClientRef ! GameClient.ReceiveGameChatMessage(message))
          Behaviors.same

        // Game handled by GameServer has ended. Called by Client that first finishes its cards
        case EndGame() =>
          gameLogicObj.endGame()
          lobbyServerRef.get ! LobbyServer.GameEnded()

          clientPlayers.foreach(_.gameClientRef ! GameClient.showLeaderBoard(clientPlayers))

          // End game and stop game server actor
          Client.gameServerActor = None
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}