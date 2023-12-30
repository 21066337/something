package ch.makery.something.controllers

import ch.makery.something.Actor.GameClient
import ch.makery.something.Client
import ch.makery.something.models.{Game, GameClientModel, MemeCard}
import ch.makery.something.utils.{GameLogic, Timer}
import javafx.application.Platform
import javafx.fxml.FXML
import scalafx.event.ActionEvent
import scalafx.scene.control.Button
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.animation.AnimationTimer

@sfxml
class VotingController(@FXML var TimerCount: Text,
                       @FXML var Prompt: Text,
                       @FXML var Image0_0: ImageView,
                       @FXML var Player0_0: Text,
                       @FXML var Shuffled0_0: Text,
                       @FXML var Grid0_0: Button,
                       @FXML var Image1_0: ImageView,
                       @FXML var Player1_0: Text,
                       @FXML var Shuffled1_0: Text,
                       @FXML var Grid1_0: Button,
                       @FXML var Image2_0: ImageView,
                       @FXML var Player2_0: Text,
                       @FXML var Shuffled2_0: Text,
                       @FXML var Grid2_0: Button,
                       @FXML var Image0_1: ImageView,
                       @FXML var Player0_1: Text,
                       @FXML var Shuffled0_1: Text,
                       @FXML var Grid0_1: Button,
                       @FXML var Image1_1: ImageView,
                       @FXML var Player1_1: Text,
                       @FXML var Shuffled1_1: Text,
                       @FXML var Grid1_1: Button,
                       @FXML var Image2_1: ImageView,
                       @FXML var Player2_1: Text,
                       @FXML var Shuffled2_1: Text,
                       @FXML var Grid2_1: Button) {

  private var game: Game = null
  var clickedButtonId: String = null
  private var playersDealtCards:  Map[GameClientModel, MemeCard] = null

  def setGame(gameS: Game, cards:  Map[GameClientModel, MemeCard]): Unit = {
    game = gameS
    playersDealtCards = cards
    setTimer()
    Prompt.text = game.getCurrentPrompt().getPromptText()
    updatePlayersUI()
  }

  def updatePlayersUI(): Unit = {

    var counter = 0
    // Loop through players and update UI
    for (i <- game.players.indices) {
      val player = game.players(i)
      val dealtCard = playersDealtCards(player)

      if(i < 3){
        // Update image and text based on dealt card
        val image = getImageElement(i, 0)
        val text = getTextElement(i, 0)
        val button = getButtonElement(i, 0)
        val shuffled = getShuffledElement(i, 0)
        updatePlayerUI(player, dealtCard, image, text, button, shuffled)
      } else {
        // For the second row
        val image2 = getImageElement(counter, 1)
        val text2 = getTextElement(counter, 1)
        val button2 = getButtonElement(counter, 1)
        val shuffled2 = getShuffledElement(counter, 1)
        updatePlayerUI(player, dealtCard, image2, text2, button2, shuffled2)

        counter += 1
      }
    }
  }

  // Method to update the UI for a specific player
  private def updatePlayerUI(player: GameClientModel, dealtCard: MemeCard, image: ImageView, text: Text, button: Button, shuffled: Text): Unit = {
    if (dealtCard != null) {
      image.visible = true
      text.visible = true
      button.visible = true
      shuffled.visible = false
      image.image = new Image(dealtCard.getImage())
      text.text = s"Player ${player.getPlayerName()}"
      button.disable = false
    } else {
      // Handle case where player has shuffled
      shuffled.visible = true
      image.visible = true
      text.visible = true
      button.visible = true

      image.image = null
      text.text = s"Player ${player.getPlayerName()}"
      button.disable = true
    }
  }

  // Method to get the ImageView for a player in the grid
  private def getImageElement(row: Int, col: Int): ImageView = {
    val imageId = s"Image${row}_${col}"
    getClass.getDeclaredField(imageId).get(this).asInstanceOf[ImageView]
  }

  // Method to get the Text element for a player in the grid
  private def getTextElement(row: Int, col: Int): Text = {
    val textId = s"Player${row}_${col}"
    getClass.getDeclaredField(textId).get(this).asInstanceOf[Text]
  }

  // Method to get the Button element for a player in the grid
  private def getButtonElement(row: Int, col: Int): javafx.scene.control.Button = {
    val buttonId = s"Grid${row}_${col}"
    getClass.getDeclaredField(buttonId).get(this).asInstanceOf[Button]
  }

  // Method to get the Text element for a player in the grid
  private def getShuffledElement(row: Int, col: Int): Text = {
    val textId = s"Shuffled${row}_${col}"
    getClass.getDeclaredField(textId).get(this).asInstanceOf[Text]
  }

  // Method to handle button clicks for voting
  def handleClick(actionEvent: ActionEvent): Unit = {
    val selectedButton = actionEvent.source.asInstanceOf[javafx.scene.control.Button]

    // Check if the button ID is not null
    if (selectedButton.id() != null) {
      // Now you can use selectedButton as a ScalaFX Button
      clickedButtonId = selectedButton.id()

      // Iterate through all buttons and update their styles
      for {
        row <- 0 to 2
        col <- 0 to 1
        buttonId = s"Grid${row}_${col}"
        button = getButtonElement(row, col)
      } {
        if (buttonId == clickedButtonId) {
          // Set style for the clicked button
          button.setStyle("-fx-border-color: #6B3FA0; -fx-border-width: 3px; -fx-background-color: transparent; -fx-background-radius: 20; -fx-padding: 10px; -fx-border-radius: 20;")
        } else {
          // Clear style for other buttons
          button.setStyle("-fx-border-color: transparent; -fx-border-width: 2px; -fx-background-color: transparent;")
        }
      }
    } else {
      println("No button clicked")
      // You can add some default behavior here, or simply return without doing anything
    }
  }

  def setTimer(): Unit = {
    // Start the countdown timer
    val startTime = System.currentTimeMillis()

    Timer.startCountdown(5, () => {
      val player = getPlayerForButton(clickedButtonId)

      Client.gameClientActor.foreach { gameClientActor =>
        gameClientActor ! GameClient.GetPlayerToVote(player.getPlayerName())
      }
    })

    // Update the countdown label in real-time
    val animationTimer = AnimationTimer { _ =>
      val currentTime = System.currentTimeMillis()
      val elapsedTime = currentTime - startTime
      val remainingTime = 15 * 1000 - elapsedTime

      if (remainingTime <= 0) {
        TimerCount.text = "0" // Ensure the label shows 0 when the countdown finishes
      } else {
        TimerCount.text = (remainingTime / 1000).toString
      }
    }

    //start animation
    animationTimer.start()
  }

  // Method to get the player for a clicked button
  private def getPlayerForButton(buttonId: String): GameClientModel = {
    // Check for null before pattern matching
    if (buttonId != null) {
      // Parse the buttonId to extract row and column information
      val pattern = "Grid(\\d)_(\\d)".r

      buttonId match {
        case pattern(col, row) =>
          val rowIndex = row.toInt
          val colIndex = col.toInt
          // Use rowIndex and colIndex to find the corresponding player
          if (rowIndex < 2) {
            game.players(colIndex)
          } else {
            game.players(3 + colIndex)
          }
      }
    } else {
      println("no player voted")
      null
    }
  }
}
