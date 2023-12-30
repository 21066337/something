package ch.makery.something.Actor

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.cluster.typed._
import akka.{actor => classic}
import akka.discovery.{Discovery, Lookup, ServiceDiscovery}
import akka.discovery.ServiceDiscovery.Resolved
import akka.actor.typed.scaladsl.adapter._
import scalafx.collections.ObservableHashSet
import scalafx.application.Platform
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.Address
import ch.makery.something.Actor.LobbyServer
import ch.makery.something.{Client, GameServer}
import ch.makery.something.models.{Game, GameClientModel, Lobby, MemeCard}
import com.hep88.protocol.JsonSerializable
import scalafx.application.Platform
import scalafx.collections.ObservableHashSet
import ch.makery.something.controllers.LobbyController

object GameClient {
  sealed trait Command extends Serializable

  // Protocols
  case class StartJoin(lobbyServerRef: ActorRef[LobbyServer.Command]) extends Command
  case class StartActor(gameClientModelGiven: GameClientModel) extends Command
  case class RequestAllLobbyPlayers() extends Command
  case class ReceiveAllLobbyPlayers(lobbyPlayersReceived: Iterable[GameClientModel]) extends Command
  case object Ready extends Command
  case object QuitLobby extends Command
  case object LobbyDisbanded extends Command
  case class ReceiveChatMessage(message: String) extends Command
  case class ReceiveGameChatMessage(message: String) extends Command
  case class SendChatMessage(message: String) extends Command
  case class SendGameChatMessage(message: String) extends Command
  case class ReceiveGameInstance(gameInstance: Game) extends Command
  case class JoinGameServer(gameServer: ActorRef[GameServer.Command]) extends Command
  case class DealCard(cardIndex: Int) extends Command
  case class ShuffleCard() extends Command
  case class ShowWaitingLabel() extends Command
  case class ShowVotingScene(game: Game, dealtCard:  Map[GameClientModel, MemeCard]) extends Command
  case class GetPlayerToVote(playerName: String) extends Command
  case class EndGame() extends Command
  case class showLeaderBoard(players: List[GameClientModel]) extends Command
  case class RequestToJoinBackLobby() extends Command
  case class JoinBackLobby(lobby: Lobby, lobbyPlayersReceived: Iterable[GameClientModel]) extends Command

  var currentLobbyServer : Option[ActorRef[LobbyServer.Command]] = None
  var currentGameServer : Option[ActorRef[GameServer.Command]] = None
  var gameClientModel: Option[GameClientModel] = None
  var currentLobbyMembers = new ObservableHashSet[GameClientModel]()

  currentLobbyMembers.onChange { (ns, _) =>
    Platform.runLater {
      Client.lobbyController.updatePlayerListing(currentLobbyMembers)
    }
  }

  def apply(): Behavior[GameClient.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage { message =>
      message match {
        case StartActor(gameClientModelGiven: GameClientModel) =>
          gameClientModel = Some(gameClientModelGiven)
          Behaviors.same
        case StartJoin(lobbyServerRef: ActorRef[LobbyServer.Command]) =>
          currentLobbyServer = Some(lobbyServerRef)
          currentLobbyServer.get ! LobbyServer.HandleJoining(gameClientModel.get)
          Behaviors.same

        case RequestAllLobbyPlayers() =>
          currentLobbyServer.get ! LobbyServer.SendAllPlayerInLobby(context.self)
          Behaviors.same
        case ReceiveAllLobbyPlayers(lobbyPlayersReceived) =>
          currentLobbyMembers.clear()
          currentLobbyMembers ++= lobbyPlayersReceived
          Behaviors.same

        case Ready =>
          println(s"[LobbyMember] Sending ready status update for ${gameClientModel.get.getPlayerName()}")
          currentLobbyServer.foreach(_ ! LobbyServer.HandleReadyClick(gameClientModel.get))
          currentLobbyServer.foreach(_ ! LobbyServer.BroadcastChatMessage(s"${gameClientModel.get.getPlayerName()} is ready!"))
          Behaviors.same
        case QuitLobby =>
          currentLobbyServer.foreach(_ ! LobbyServer.BroadcastChatMessage(s"${gameClientModel.get.getPlayerName()} has left the lobby."))
          currentLobbyServer.foreach(_ ! LobbyServer.HandleQuitting(gameClientModel.get))

          Client.gameClientActor = None // remove this server actor
          Behaviors.stopped

        case JoinGameServer(gameServer) =>
          currentGameServer = Some(gameServer)
          Behaviors.same

        case LobbyDisbanded =>
          Platform.runLater(() => {
            Client.showMainMenuScene()
          })

          println("Lobby has been disbanded, Redirecting to main menu...")
          Client.gameClientActor = None

          Behaviors.stopped

        case ReceiveChatMessage(message) =>
          println(s"LobbyMember: Received chat message '$message'. Updating UI.")
          Platform.runLater(() => {
              Client.lobbyController.handleChatMessage(message)
          })
          Behaviors.same
        case ReceiveGameChatMessage(message) =>
          println(s"LobbyMember: Received chat message '$message'. Updating UI.")
          Platform.runLater(() => {
            Client.gameController.handleChatMessage(message)
          })

          Behaviors.same

        case SendChatMessage(message) =>
          println(s"LobbyMember: Sending chat message '$message' to LobbyServer for broadcasting.")
          currentLobbyServer.foreach(_ ! LobbyServer.BroadcastChatMessage(s"${gameClientModel.get.getPlayerName}: $message"))
          Behaviors.same
        case SendGameChatMessage(message) =>
          println(s"GameClient: Sending chat message '$message' to LobbyServer for broadcasting.")
          currentGameServer.foreach(_ ! GameServer.BroadcastChatMessage(s"${gameClientModel.get.getPlayerName}: $message"))
          Behaviors.same

        case ReceiveGameInstance(gameInstance) =>
          println(s"Received game instance ${gameClientModel.get.getPlayerName()}")

          Platform.runLater(() => {
            Client.showNewGameScene(gameInstance)
          })

          println("Successfully ran")
          Behaviors.same
          
        // Deal Card Function
        case DealCard(cardIndex) =>
          currentGameServer.foreach(_ ! GameServer.DealCard(gameClientModel.get.getPlayerName(), cardIndex))
          Behaviors.same

        // Shuffle Card Function
        case ShuffleCard() =>
          currentGameServer.foreach(_ ! GameServer.ShuffleCard(gameClientModel.get.getPlayerName()))
          Behaviors.same
        // Receive new set of card (after shuffling)

        // show waiting label
        case ShowWaitingLabel() =>
          Platform.runLater(() => {
            Client.gameController.showWaitingLabel()
          })
          Behaviors.same

        // show voting scene
        case ShowVotingScene(game, dealtCards) =>
          Platform.runLater(() => {
            Client.showVotingScene(game, dealtCards)
          })
          Behaviors.same

        // get dealt card (to show in voting scene)
        case GetPlayerToVote(playerName) =>
          currentGameServer.foreach(_ ! GameServer.VotePlayer(playerName))

          Behaviors.same

        // Send request to end game
        case EndGame() =>
          currentGameServer.foreach(_ ! GameServer.EndGame())
          println("Asked server to end game")
          Behaviors.same

        case showLeaderBoard(players) =>
          Platform.runLater(() => {
            Client.showLeaderBoardScene(players)
          })

          Behaviors.same

        case RequestToJoinBackLobby() =>
          currentLobbyServer.get ! LobbyServer.HandleReqToJoinBackLobby(context.self)
          Behaviors.same

        case JoinBackLobby(lobby, lobbyPlayersReceived) =>
          currentLobbyMembers.clear()
          currentLobbyMembers ++= lobbyPlayersReceived

          Platform.runLater(() => {
            Client.showLobbyScene(lobby.nameS)
            Client.lobbyController.updatePlayerListing(currentLobbyMembers)
          })

          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}

