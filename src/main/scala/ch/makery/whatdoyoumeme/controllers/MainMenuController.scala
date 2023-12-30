package ch.makery.something.controllers

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import ch.makery.something.Actor.{GameClient, LobbyServer}
import ch.makery.something.Client.{clientGuardianActor, mainSystem}
import ch.makery.something.models.{GameClientModel, Lobby, ServerLobbyItem, User}
import ch.makery.something.{Client, ClientGuardian, util}
import ch.makery.something.util.MusicPlayer
import javafx.fxml.FXML
import scalafx.animation.TranslateTransition
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{AnchorPane, ColumnConstraints, GridPane, RowConstraints, VBox}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml
import scalafx.util.Duration
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.event.ActionEvent
import java.awt.Desktop
import java.net.URI


@sfxml
class MainMenuController (@FXML private val logo1: ImageView,
                          @FXML private val logo2: ImageView,
                          @FXML private var volumeButton: Button,
                          @FXML var Username: Text,
                          @FXML var RoomCode: TextField,
                          @FXML var LobbyContainer: VBox,
                          @FXML var viewLobby: AnchorPane,
                          private var isMusicPlaying: Boolean = true) {

  var clientGuardianActor: Option[ActorRef[ClientGuardian.Command]] = None
  var currentLobbies: Iterable[ServerLobbyItem] = None

  def initialize(): Unit = {
    // get all newest lobbies, set currentLobbies variable
    Client.clientGuardianActor ! ClientGuardian.GetAllLobbies()

    logoAnimation()
    startBGM()

    Username.text = Client.clientUserModel.getPlayerName()
    viewLobby.visible = false
  }

  // Method to set zoom in transition to logo
  private def logoAnimation(): Unit = {

    val slideLeft = new TranslateTransition(Duration(700))

    slideLeft.setNode(logo1)
    slideLeft.setFromX(-800)
    slideLeft.setToX(0)
    slideLeft.play()

    val slideRight = new TranslateTransition(Duration(700))

    slideRight.setNode(logo2)
    slideRight.setFromX(800)
    slideRight.setToX(0)
    slideRight.play()
  }

  private def startBGM(): Unit = {
    if(!Client.musicOff){
      val file = getClass.getResource("/BGM/mainmenubgm.wav")
      MusicPlayer.playBackgroundMusic(file)
      val image = new Image("Images/Icons/soundOn.png") // Replace with your muted icon image path
      volumeButton.setGraphic(new ImageView(image))
      isMusicPlaying = true
    } else {
      isMusicPlaying = false
      MusicPlayer.stopBackgroundMusic()
      val image = new Image("Images/Icons/soundOff.png") // Replace with your muted icon image path
      volumeButton.setGraphic(new ImageView(image))
    }

  }

  def handleGitButton(action: ActionEvent): Unit = {
    val gitHubLink = "https://github.com/cheryl-toh/What-Do-Your-Meme"
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop.browse(new URI(gitHubLink))
    } else {
      // Handle if desktop browsing is not supported
      println("Desktop browsing is not supported on this platform.")
    }
  }

  def handleVolumeButtonClicked(action: ActionEvent): Unit = {
    if (isMusicPlaying) {
      Client.musicOff = true
      isMusicPlaying = false
      MusicPlayer.stopBackgroundMusic()
      val image = new Image("Images/Icons/soundOff.png") // Replace with your muted icon image path
      volumeButton.setGraphic(new ImageView(image))
    } else {
      Client.musicOff = false
      val file = getClass.getResource("/BGM/mainmenubgm.wav")
      MusicPlayer.playBackgroundMusic(file)
      isMusicPlaying = true
      val image = new Image("Images/Icons/soundOn.png") // Replace with your muted icon image path
      volumeButton.setGraphic(new ImageView(image))
    }
  }

  // Given current lobbies, add them to be seen in UI
  def addLobbyToUI(lobbylist: Iterable[ServerLobbyItem]): Unit = {
    currentLobbies = lobbylist

    if (LobbyContainer != null || LobbyContainer.children != null) {
      // Clear all children from the LobbyContainer except the first one
      LobbyContainer.children.remove(1, LobbyContainer.children.size())
    }

    currentLobbies.foreach(lobby => {
      val gridPane = new GridPane {
        alignment = scalafx.geometry.Pos.Center
        prefHeight = 30.0
        prefWidth = 505.0

        columnConstraints = List.tabulate(4) { i =>
          new ColumnConstraints {
            halignment = scalafx.geometry.HPos.Center
            hgrow = scalafx.scene.layout.Priority.Sometimes
            maxWidth = if (i == 3) 363.0 else 479.0
            minWidth = 10.0
            prefWidth = if (i == 3) 251.0 else 322.0
          }
        }

        rowConstraints = List(new RowConstraints {
          minHeight = 10.0
          prefHeight = 30.0
          valignment = scalafx.geometry.VPos.Center
          vgrow = scalafx.scene.layout.Priority.Sometimes
        })
      }

      val lobbyIdLabel = new Label {
        id = "LobbyId"
        text = lobby.LobbyIDS
      }

      val lobbyMasterLabel = new Label {
        id = "LobbyMasterName"
        text = lobby.nameS
      }

      val playerNumberLabel = new Label {
        id = "PlayerNumber"
        text = s"${lobby.numberOfPlayer}/6"
      }

      val joinButton = new Button {
        mnemonicParsing = false
        onAction = (event: scalafx.event.ActionEvent) => handleJoin(event, lobby.LobbyIDS)
        prefHeight = 12.0
        prefWidth = 101.0
        text = "Join"
        style = "-fx-background-color: #9F82EE; -fx-text-fill: white; -fx-font-weight: bold;"

      }

      // Add the components to the GridPane
      gridPane.add(lobbyMasterLabel, 0, 0)
      gridPane.add(lobbyIdLabel, 1, 0)
      gridPane.add(playerNumberLabel, 2, 0)
      gridPane.add(joinButton, 3, 0)

      // Add the GridPane to the lobbyContainer (VBox)
      LobbyContainer.children.add(gridPane)
    })

  }

  // Method to show existing lobbies to join
  def handleShowLobby(action: ActionEvent): Unit = {
    refreshLobbyList()

    //open lobby dialog
    viewLobby.visible = true
  }

  def handleBack(action: ActionEvent): Unit = {
    viewLobby.visible = false
  }

  // Method for create new lobby button
  def handleNewLobby(action: ActionEvent): Unit = {
    // Get code for new lobby
    val roomCode = RoomCode.text.value

    // Check if existing lobby has the same code, if yes don't create
    for(existingLobby <- currentLobbies){
      if(existingLobby.LobbyIDS == roomCode){
        println(s"Cannot create lobby server, room code '$roomCode' already exist and taken by another lobby server")
        return
      }
    }

    // Create Client Lobby Member Actor First
    Client.gameClientModel = GameClientModel(Client.clientUserModel.getPlayerName())

    Client.gameClientActor = Some(Client.mainSystem.spawn(GameClient(), "lobbyMember"))
    Client.gameClientModel.gameClientRef = Client.gameClientActor.get

    Client.gameClientActor.get ! GameClient.StartActor(Client.gameClientModel) // start up lobby member actor

    // Create new LobbyServerActor for all Clients to run on (To add owner of lobby ref here)
    Client.lobbyServerActor = Some(Client.mainSystem.spawn(LobbyServer(roomCode, Client.clientGuardianActor,
      Client.gameClientActor.get, Client.clientUserModel.getPlayerName()), "lobbyServer"))

    // Create Lobby Entry into server (LobbyRef)
    clientGuardianActor map ((x) => x ! ClientGuardian.CreateLobby(
      roomCode, Client.lobbyServerActor.get,
      Client.clientUserModel.getPlayerName())
      )

    // adding current client into his own lobby
    Client.gameClientActor map ((x) => x ! GameClient.StartJoin(Client.lobbyServerActor.get))
    Client.showLobbyScene(Client.clientUserModel.getPlayerName())
  }

  // let player join selected lobby
  def handleJoin(action: ActionEvent, lobbyID: String): Unit = {
    // Check if the lobby exists in the available lobbies
    val selectedLobby = currentLobbies.find(_.LobbyIDS == lobbyID)

    // will account for empty selected lobbys
    selectedLobby.foreach { lobby =>
      if(selectedLobby.get.numberOfPlayer < 6 && !selectedLobby.get.inGameStatus) {
        Client.gameClientModel = GameClientModel(Client.clientUserModel.getPlayerName())

        Client.gameClientActor = Some(Client.mainSystem.spawn(GameClient(), "lobbyMember"))
        Client.gameClientModel.gameClientRef = Client.gameClientActor.get

        Client.gameClientActor.get ! GameClient.StartActor(Client.gameClientModel) // start up lobbymember actor

        // Send a request to join the lobby
        Client.gameClientActor map ((x) => x ! GameClient.StartJoin(lobby.lobbyServerRef))

        // Show the lobby scene
        Client.showLobbyScene(lobby.nameS)
      }
    }
  }

  // Method to handle how to play button clicked
  def handleHowToPlay(action: ActionEvent): Unit = {
    //play button clicked sound
    val file = getClass.getResource("/Sounds/click.wav")
    MusicPlayer.playSoundEffect(file)

    // stop background music
    MusicPlayer.stopBackgroundMusic()

    Client.showHowToPlayDialog()
  }

  def refreshLobbyList() : Unit = Platform.runLater {
    clientGuardianActor.foreach(_ ! ClientGuardian.GetAllLobbies())
  }
}