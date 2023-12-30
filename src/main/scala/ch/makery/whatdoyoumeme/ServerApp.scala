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
import ch.makery.something.Actor.{LobbyServer}
import ch.makery.something.models.{User, ServerLobbyItem}

import scala.util.control.Breaks._

//Actors communicate with each other through messages, and the AKKA actor model
// ensures that these messages are processed asynchronously.
object Server {
  sealed trait Command

  // ===== Protocols =====
  case class JoinServer(name: String, clientRef: ActorRef[ClientGuardian.Command]) extends Command
  case class LeaveServer(name: String, clientRef: ActorRef[ClientGuardian.Command]) extends Command

  case class AddNewLobby(lobbyIDGiven: String, lobbyServerRef: ActorRef[LobbyServer.Command], lobbyMasterName: String) extends Command
  case class RemoveLobby(lobbyItem: String) extends Command
  case class updateLobbyNumber(lobbyIDGiven: String, number: Int) extends Command
  case class updateLobbyGameStatus(lobbyIDGiven: String, status: Boolean) extends Command
  case class ShowAllLobbies(clientBackendRef: ActorRef[ClientGuardian.Command]) extends Command
  // ===== Protocols =====

  //ServiceKey. We are setting up server service with name "Server". Everyone else can use this to find us
  val ServerKey: ServiceKey[Server.Command] = ServiceKey("Server")

  // == Server States Variables ==
  // ALL members connected and joined (given credentials) to server. Can have a change function since observable
  val serverMembers = new ObservableHashSet[User]()
  val lobbies = new ObservableHashSet[ServerLobbyItem]()

  // every changes notify all clients of existing lobbies
  lobbies.onChange { (ns, _) =>
    serverMembers.foreach(_.ref ! ReceiveAllLobbies(Server.lobbies.toList))
  }

  // Defines the behavior of the Server actor setup runs only one time
  def apply(): Behavior[Server.Command] = Behaviors.setup { context =>
    // Register itself with ServerKey. Telling the receptionist this actor ref is offering this service.
    // Receptionist (AKKA) just keeps a record of a service that an actor offers, other actors can find that service.
    // when we create an actor system, each will have their own receptionist.
    context.system.receptionist ! Receptionist.Register(ServerKey, context.self)

    // Behaviour can be nested. Inside setup, create receive behaviour inside to determine behaviour to process message
    Behaviors.receiveMessage { message =>
      message match {
        case JoinServer(name, clientBackendRef) =>
          Server.serverMembers += User(name, clientBackendRef)
          println("JOINED NEW DEVICE: " + name)

          clientBackendRef ! JoinedServer(Server.serverMembers.toList) // message sent to Actor referenced by ref. Joined
          Behaviors.same

        case LeaveServer(name, clientRef) =>
          members -= User(name, clientRef)
          Behaviors.same

        case AddNewLobby(lobbyIDGiven, lobbyServerRef, name) =>
          // Create Lobby Master
          Server.lobbies += ServerLobbyItem(lobbyIDGiven, lobbyServerRef, name)
          println(s"Lobby Added: ${Server.lobbies}")
          Behaviors.same

        case RemoveLobby(lobbyIDGiven) =>
          lobbies -= lobbies.find(_.LobbyIDS == lobbyIDGiven).orNull
          println(s"Lobby removed: ${Server.lobbies}")
          Behaviors.same

        case ShowAllLobbies(clientBackendRef) =>
          clientBackendRef ! ReceiveAllLobbies(Server.lobbies.toList)
          Behaviors.same

        case updateLobbyNumber(lobbyIDGiven, number) =>
          breakable{
            for (lobby <- lobbies) {
              if (lobby.LobbyIDS == lobbyIDGiven) {
                lobby.changePlayerCount(number)
                break
              }
            }
          }
          Behaviors.same

        case updateLobbyGameStatus(lobbyIDGiven, status) =>
          breakable {
            for (lobby <- lobbies) {
              if (lobby.LobbyIDS == lobbyIDGiven) {
                lobby.inGameStatus = status
                break
              }
            }
          }
          Behaviors.same
      }
    }
  }
}

object ServerApp extends App {
  // Actor System is
  val mainSystem = akka.actor.ActorSystem("MainSystem", MyConfiguration.askDevConfigServer()) //classic

  // Create server actor inside actor system
  mainSystem.spawn(Server(), "Server")

  // Create own cluster with one actor system inside for now
  val typedSystem: ActorSystem[Nothing] = mainSystem.toTyped
  val cluster = Cluster(typedSystem)

  cluster.manager ! Join(cluster.selfMember.address)
  AkkaManagement(mainSystem).start()
  ClusterBootstrap(mainSystem).start()
}


//object ServerApp extends App {
//  val config = ConfigFactory.load()
//  val mainSystem = akka.actor.ActorSystem("HelloSystem", MyConfiguration.askDevConfig().withFallback(config)) //classic
//  // Akka system is like a hotel. Its a hotel that provides basic infrastructures like multi-broadcasting,
//  // people to register services, collect undelivarable messages. There are a few system actors
//  // pre-created. So we create the actor system and hotel is connected to the network. The constructor of the behaviour
//  // system will construct according to apply() function that returns a behavior.
//  // without cluster here, the default cluster is just the machine.
//  val greeterMain: ActorSystem[Server.Command] = ActorSystem(Server(), "HelloSystem")
//}

// in chap 10, Dr Chin has another cluster not default cluster that has Management System which allows you
// to see every Actor System in your cluster, which is hosted in your machine. If main server actor is gone, automatically
// a new leader will be chosen however assuming everyone else is client, they do not have server service which
// means its useless. Network still exist. Client can have click become server button to replace current behaviour into
// server behaviour thus becoming server.

// * PUT RELIABILITY CODE HERE SO WHEN NODE DISCONNECT AUTOMATICALLY KNOWS