package ch.makery.something

import akka.cluster.typed._
import akka.{actor => classic}
import akka.discovery.{Discovery, Lookup, ServiceDiscovery}
import akka.discovery.ServiceDiscovery.Resolved
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.adapter._
import ch.makery.something.AKKA.MyConfiguration
import ch.makery.something.Actor.{GameClient, LobbyServer}
import com.typesafe.config.ConfigFactory
import ch.makery.something.models.{Game, GameClientModel, Lobby, MemeCard, User}
import ch.makery.something.util.MusicPlayer
import controllers.{GameController, HowToPlayController, LeaderBoardController, LobbyController, MainMenuController, SignInController, VotingController}
import scalafx.stage.{Modality, Stage, WindowEvent}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.Scene
import javafx.{scene => jfxs}
import scalafx.collections.ObservableHashSet
import scalafx.event.{ActionEvent, Event}
import scalafx.scene.image.Image

object Client extends JFXApp{
  // See if client switched off music
  var musicOff: Boolean = false

  // set primary stage
  stage = new PrimaryStage {
    title = "What Do You Meme"
    maximized = true
    icons += new Image(getClass.getResourceAsStream("/Images/Logo/icon.png")) // Replace with your icon path
  }

  // show sign in scene
  showSignInScene()

  // ===== Creation of Client Actor System and joining Server Actor and its Cluster =====
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  var clientASIP = ""
  var clientASPORT = 0
  var serverClusterLocalIP = ""
  var serverClusterLocalPort = 0

  // Create Client Actor System
  var mainSystem: akka.actor.ActorSystem = null
  var greeterMain: ActorSystem[Nothing] = null
  var cluster: Cluster = null
  var discovery: ServiceDiscovery = null
  var clientGuardianActor: ActorRef[ClientGuardian.Command] = null
  var clientUserModel: User = null

  def initializeClientActorSystem(username: String, clientASIPS: String, clientASPORTS: String, serverClusterLocalIPS: String,
                                  serverClusterLocalPortS: String): Unit = {
    clientASIP = clientASIPS
    clientASPORT = clientASPORTS.toInt
    serverClusterLocalIP = serverClusterLocalIPS
    serverClusterLocalPort = serverClusterLocalPortS.toInt

    mainSystem = akka.actor.ActorSystem("MainSystem", MyConfiguration.askDevConfigClient(clientASIP, clientASPORT))
    greeterMain = mainSystem.toTyped // converts untyped actor system) to a typed actor system (ActorSystem[Nothing]).
    cluster = Cluster(greeterMain) // initializes an Akka Cluster using the typed actor system
    discovery = Discovery(mainSystem).discovery //DNS

    // ===== Join local cluster =====
    val address = akka.actor.Address("akka", "MainSystem", serverClusterLocalIP, serverClusterLocalPort)
    // cluster manager of client will join the server cluster
    cluster.manager ! JoinSeedNodes(List(address))

    // ===== Create Client Guardian Actor =====
    clientGuardianActor = mainSystem.spawn(ClientGuardian(), "clientGuardian")

    // Start actor to find server AS that exists in cluster, and join server
    clientGuardianActor ! ClientGuardian.Start(username)
    clientUserModel = User(username, clientGuardianActor)
  }

  // ===== Creation of Client Actor System and joining Server Actor and its Cluster =====

  // ===== Client AS Other Actors =====
  var lobbyServerActor: Option[ActorRef[LobbyServer.Command]] = None
  var gameServerActor: Option[ActorRef[GameServer.Command]] = None

  var gameClientActor: Option[ActorRef[GameClient.Command]] = None
  // ===== Client AS Other Actors =====

  // Local changes will be reflected, but on a distributed server no. Changes made in server must reflect here
  var gameClientModel: GameClientModel = null
  // ===== Client Models =====

  // ===== Reference to controllers =====
  var signInController: SignInController#Controller = null
  var mainMenuController: MainMenuController#Controller = null
  var lobbyController: LobbyController#Controller = null
  var gameController: GameController#Controller = null
  var votingController: VotingController#Controller = null
  var leaderBoardController: LeaderBoardController#Controller = null
  var howToPlayController: HowToPlayController#Controller = null

  // Method for switching to main menu scene
  def showSignInScene(): Unit = {
    // initialize resource and loader
    val resource = getClass.getResource("/view/SignIn.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]

    // load controller, initialize it
    signInController = loader.getController[SignInController#Controller]

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)
  }

  // Method for switching to main menu scene
  def showMainMenuScene(): Unit = {

    // initialize resource and loader
    val resource = getClass.getResource("/view/MainMenu.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    mainMenuController = loader.getController[MainMenuController#Controller]

    // Set controller's empty clientGuardianActor with this clientApp instance's backend ref
    mainMenuController.clientGuardianActor = Option(clientGuardianActor)

    mainMenuController.initialize()

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)
  }

  // Method for switching to lobby scene
  def showLobbyScene(lobbyOwnerName: String): Unit = {
    // initialize resource and loader
    val resource = getClass.getResource("/view/Lobby.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    lobbyController = loader.getController[LobbyController#Controller]

    lobbyController.initialize(lobbyOwnerName)

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)

    //play background music
    if(!musicOff){
      val file = getClass.getResource("/BGM/lobbybgm.wav")
      MusicPlayer.playBackgroundMusic(file)
    }
  }

  // Method for switching to game scene
  def showNewGameScene(gameInstance: Game): Unit = {
    // initialize resource and loader
    val resource = getClass.getResource("/view/Game.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    gameController = loader.getController[GameController#Controller]
    gameController.startGame(gameInstance)

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)

    //play background music
    if(!musicOff) {
      val file = getClass.getResource("/BGM/gamebgm.wav")
      MusicPlayer.playBackgroundMusic(file)
    }
  }

  // Method for switching to game scene
  def showVotingScene(game: Game, dealtCards:  Map[GameClientModel, MemeCard]): Unit = {

    // initialize resource and loader
    val resource = getClass.getResource("/view/Voting.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    votingController = loader.getController[VotingController#Controller]
    votingController.setGame(game, dealtCards)

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)

    //play background music
    if (!musicOff) {
      val file = getClass.getResource("/BGM/votebgm.wav")
      MusicPlayer.playBackgroundMusic(file)
    }
  }

  // Method for switching to game scene
  def showLeaderBoardScene(players: List[GameClientModel]): Unit = {

    // initialize resource and loader
    val resource = getClass.getResource("/view/LeaderBoard.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    // load resources
    loader.load()

    // initialize root and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    leaderBoardController = loader.getController[LeaderBoardController#Controller]

    leaderBoardController.updateLeaderboard(players)

    // Create a new Scene object with the gameRoot as the root node
    val gameScene = new Scene(roots2)

    // Set the game scene to the primary stage to display it
    stage.setScene(gameScene)

    //play background music
    if(!musicOff) {
      val file = getClass.getResource("/BGM/leaderboardbgm.wav")
      MusicPlayer.playBackgroundMusic(file)
    }

  }

  // Method to show how to play dialog
  def showHowToPlayDialog(): Unit = {

    // initialize resource and loader
    val resource = getClass.getResource("/view/HowToPlay.fxml")
    val loader = new FXMLLoader(resource, NoDependencyResolver)

    loader.load();

    // initialize roots and controller
    val roots2 = loader.getRoot[jfxs.Parent]
    howToPlayController = loader.getController[HowToPlayController#Controller]

    // set up stage for dialog
    val dialog = new Stage() {
      initModality(Modality.ApplicationModal)
      initOwner(stage)

      // set up scene
      scene = new Scene {
        root = roots2
      }
    }

    dialog.onShown = { _: WindowEvent =>
      // call initialize method of controller
      howToPlayController.initialize()

      // Add a window hidden event handler to the pop-up dialog
      dialog.onHidden = howToPlayController.handleWindowClose
    }

    // show dialog
    dialog.showAndWait()
  }
}