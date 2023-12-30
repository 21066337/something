package ch.makery.something.controllers

import akka.actor.typed.ActorRef
import ch.makery.something.Actor.LobbyServer
import ch.makery.something.Client
import ch.makery.something.models.GameClientModel
import ch.makery.something.Actor.GameClient
import javafx.fxml.FXML
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, RowConstraints, VBox}
import scalafx.scene.text.Font
import scalafxml.core.macros.sfxml
import scalafx.application.Platform


@sfxml
class LobbyController(@FXML var PlayerList: VBox,
                      @FXML var chatVBox: VBox,
                      @FXML var Message: TextField,
                      @FXML var ReadyButton: Button,
                      @FXML var SendButton: Button) {

  var members: Iterable[GameClientModel] = null
  var lobbyOwner: String = ""

  def initialize(name: String): Unit = {
    lobbyOwner = name
  }

  def updatePlayerListing(memberS:  Iterable[GameClientModel]): Unit = {

    //initialize the initial setup of the lobby ui
    members = memberS

    // Clear all children from the LobbyContainer
    PlayerList.children.clear()

    var rowCounter = 0 // Initialize a row counter

    members.foreach(player => {
      val gridPane = new GridPane {
        alignment = scalafx.geometry.Pos.Center
        prefHeight = 72.0
        prefWidth = 1201.0
        rowCounter += 1 // Increment the row counter for the next iteration

        val rowColor = if (rowCounter % 2 == 0) "#B8A7E8" else "#CEC2EF"
        style = s"-fx-background-color: $rowColor; -fx-background-radius: 8; -fx-border-radius: 8;" // Apply row colors



        columnConstraints = List(
          new ColumnConstraints {
            halignment = scalafx.geometry.HPos.Center
            hgrow = scalafx.scene.layout.Priority.Sometimes
            maxWidth = 964.6666259765625
            minWidth = 10.0
            prefWidth = 580.9999847412109
          },
          new ColumnConstraints {
            halignment = scalafx.geometry.HPos.Center
            hgrow = scalafx.scene.layout.Priority.Sometimes
            maxWidth = 964.6666259765625
            minWidth = 10.0
            prefWidth = 359.66668701171875
          }
        )

        rowConstraints = List(new RowConstraints {
          minHeight = 10.0
          prefHeight = 30.0
          vgrow = scalafx.scene.layout.Priority.Sometimes
        })

        val playerNameLabel = new Label {
          text = player.getPlayerName
          style = "-fx-font-family: 'Concert One', sans-serif; -fx-font-size: 36;"

        }

        val lobbyMasterLabel = new Label {
          if (player.getPlayerName() == lobbyOwner) {
            text = "Lobby Owner"
          } else {
            text = "Member"
          }
          style = "-fx-font-family: 'Concert One', sans-serif; -fx-font-size: 36;"
        }

        // Add labels to the GridPane
        add(playerNameLabel, 0, 0)
        add(lobbyMasterLabel, 1, 0)
      }

      PlayerList.children.add(gridPane)
    })
  }

  // Received lobby chat message
  def handleChatMessage(message: String): Unit = {
    println(s"LobbyController: Received chat message '$message' to update UI.")
    Platform.runLater(() => {
      try {
        println(s"LobbyController (UI Thread): Adding chat message '$message' to the UI.")
        val chatMessage = new Label(message) {
          font = Font.font("Poppins", 20)
          style = "-fx-padding: 10 5 0 20px;"
        }
        chatVBox.children.add(chatMessage)
        println(s"LobbyController (UI Thread): Chat message '$message' added to the UI.")
      } catch {
        case e: Exception =>
          println(s"LobbyController (UI Thread): Exception adding message '$message' to UI.")
          e.printStackTrace() // This will print the stack trace of any exception
      }
    })
  }

  // Press ready button
  def handleReady(action: ActionEvent): Unit = {
    val currentUser = Client.clientUserModel.getPlayerName()
    val currentPlayerOption = members.find(_.getPlayerName() == currentUser)

    currentPlayerOption match {
      case Some(player) =>
        println(s"[LobbyController] Setting ready status of player ${player.getPlayerName()} to true")

        player.setReadyState(true)
        ReadyButton.setDisable(true)

        Client.gameClientActor.foreach { gameClientActor =>
          gameClientActor ! GameClient.Ready
        }

      case None =>
        println(s"Player $currentUser not found in the lobby")
    }
  }

  // Method to handle quit button
  def handleQuit(action: ActionEvent): Unit = {
    println("Handling Quit Action")
    val currentUser = Client.clientUserModel.getPlayerName()

    // Send a chat message for the player leaving the lobby
    handleChatMessage(s"$currentUser has left the lobby.")

    Client.gameClientActor.foreach(_ ! GameClient.QuitLobby)
    println("Redirecting to main menu...")

    Client.showMainMenuScene()
  }

//  def handleKick(action: ActionEvent): Unit = {
//    // kick player out of lobby
//  }

  // Handle sending message
  def handleMessage(action: ActionEvent): Unit = {
    val input = Message.text.value
    Message.text = ""
    println(s"LobbyController: Sending message '$input' to LobbyMember actor.")
    Client.gameClientActor.foreach { gameClientActor =>
      gameClientActor ! GameClient.SendChatMessage(input)
    }
  }

}