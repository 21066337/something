package ch.makery.something.Actor

import ch.makery.something.models.{GameClientModel, Lobby}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import ch.makery.something.{Client, ClientGuardian, GameServer, Server}
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
import scalafx.application.Platform
import scalafx.collections.ObservableHashSet

object LobbyServer {
  sealed trait Command extends Serializable

  case class HandleJoining(gameClient: GameClientModel) extends Command
  case class SendAllPlayerInLobby(gameClientRef: ActorRef[GameClient.Command]) extends Command
  case class HandleReadyClick(gameClient: GameClientModel) extends Command
  case class HandleQuitting(gameClient: GameClientModel) extends Command
  case class BroadcastChatMessage(message: String) extends Command
  case class StartGame() extends Command
  case class HandleReqToJoinBackLobby(gameClientRef: ActorRef[GameClient.Command]) extends Command
  case class GameEnded() extends Command

  // State Variables
  var numberOfPlayer: Int = 1
  val lobbyMembers = new ObservableHashSet[GameClientModel]()

  // track ready members
  val readyMembers = new ObservableHashSet[GameClientModel]()

  var lobbyID = ""
  var lobbyObj: Option[Lobby] = None

  var lobbyOwnerClientGuardianRef: Option[ActorRef[ClientGuardian.Command]] = None
  var lobbyOwnerMemberActor: Option[ActorRef[GameClient.Command]] = None

  def apply(lobbyIDGiven: String, lobbyOwnerClientGuardian: ActorRef[ClientGuardian.Command],
            lobbyOwnerMemberActorGiven: ActorRef[GameClient.Command], lobbyOwnerName: String): Behavior[LobbyServer.Command] = Behaviors.setup { context =>

    lobbyID = lobbyIDGiven
    lobbyObj = Some(Lobby(lobbyID, lobbyOwnerName))
    lobbyOwnerClientGuardianRef = Some(lobbyOwnerClientGuardian)
    lobbyOwnerMemberActor = Some(lobbyOwnerMemberActorGiven)

    Behaviors.receiveMessage { message =>
      message match {

        case HandleJoining(gameClientRef) =>
          lobbyMembers += gameClientRef
          // Update Number in Server List
          lobbyOwnerClientGuardianRef.map(_ ! ClientGuardian.UpdateClientLobbyNumber(lobbyID, 1))

          // Update the joining of new member to all the current lobby members (already exist and just joined)
          for(clientModel <- lobbyMembers){
            context.self ! SendAllPlayerInLobby(clientModel.gameClientRef)
          }

          Behaviors.same

        // send every player info in lobby to game client ref
        case SendAllPlayerInLobby(gameClientRef) =>
          gameClientRef ! GameClient.ReceiveAllLobbyPlayers(lobbyMembers.toList)
          Behaviors.same

        case HandleReadyClick(gameClientRef) =>
          println(s"[LobbyServer] Received ready status update from ${gameClientRef.getPlayerName()}")

          // Find the GameClientModel in the lobbyMembers set and update it
          lobbyMembers.find(_.getPlayerName() == gameClientRef.getPlayerName()).foreach { member =>
            // Update the ready state of the member
            member.setReadyState(true)

            // Update the readyMembers set
            readyMembers += member
          }

          // Log the status of each member
          lobbyMembers.foreach(member => println(s"Status of ${member.getPlayerName()}: ${member.getReadyState()}"))
          println(s"Ready Members: ${readyMembers.size}, Total Members: ${lobbyMembers.size}")

          // Check if game can be started
          context.self ! StartGame()

          Behaviors.same

        case HandleQuitting(gameClient) =>
          if (gameClient.getPlayerName() == lobbyObj.map(_.getLobbyMasterName).getOrElse("")) {
            println(s"Lobby owner ${gameClient.getPlayerName()} is quitting. Disbanding lobby.")

            // Disband the lobby
            lobbyOwnerClientGuardianRef.foreach(_ ! ClientGuardian.RemoveLobby(lobbyID))

            lobbyMembers.foreach(_.gameClientRef ! GameClient.LobbyDisbanded)
            lobbyMembers.clear()

            Behaviors.stopped
          } else {
            // normal member quitting
            println(s"Member ${gameClient.getPlayerName()} is quitting.")
            // Just remove this member
            lobbyMembers -= gameClient

            // Remove members from ready members list
            readyMembers -= gameClient

            // Update Number in Server List
            lobbyOwnerClientGuardianRef.map(_ ! ClientGuardian.UpdateClientLobbyNumber(lobbyID, -1))

            // Notify remaining members of the update
            lobbyMembers.foreach(member => context.self ! SendAllPlayerInLobby(member.gameClientRef))

            // Check if game can be started
            context.self ! StartGame()

            Behaviors.same
          }

        case BroadcastChatMessage(message) =>
          println(s"LobbyServer: Broadcasting chat message '$message' to all members.")
          lobbyMembers.foreach(_.gameClientRef ! GameClient.ReceiveChatMessage(message))
          Behaviors.same

        case StartGame() =>
          if (readyMembers.size == lobbyMembers.size){
            val gameStartMessage = "Game is starting"
            println(gameStartMessage) // Placeholder message for starting the game

            //broadcast onto Lobby Chat box
            context.self ! BroadcastChatMessage(gameStartMessage)

            // Spawn GameServerActor here
            Client.gameServerActor = Some(Client.mainSystem.spawn(GameServer(context.self), "GameServer"))

            // Reset lobby member's game state
            lobbyMembers.foreach(player => {
              player.clearHand()
              player.setHasShuffled(false)
              player.clearDealtCard()
            })

            // Initialize Game Resources
            Client.gameServerActor.get ! GameServer.StartActor(lobbyMembers.toList)

            // Update lobby status in ServerApp reference
            lobbyOwnerClientGuardianRef.map(_ ! ClientGuardian.UpdateClientLobbyStatus(lobbyID, true))
          }
          Behaviors.same

        // GameServerActor removed previously by GameServerActor already
        case GameEnded() =>
          readyMembers.clear()

          // Update lobby status in ServerApp reference
          lobbyOwnerClientGuardianRef.map(_ ! ClientGuardian.UpdateClientLobbyStatus(lobbyID, false))
          Behaviors.same

        case HandleReqToJoinBackLobby(gameClientRef)=>
          gameClientRef ! GameClient.JoinBackLobby(lobbyObj.get, lobbyMembers.toList)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}

