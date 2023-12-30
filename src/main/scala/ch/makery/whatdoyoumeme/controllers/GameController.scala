package ch.makery.something.controllers

import ch.makery.something.Actor.GameClient
import ch.makery.something.Client
import ch.makery.something.models.{Game, GameClientModel, MemeCard, MemeDeck, PromptDeck, User}
import ch.makery.something.utils.{GameLogic, Timer}
import javafx.fxml.FXML
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.VBox
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.{Font, Text}
import scalafxml.core.macros.sfxml

@sfxml
class GameController(@FXML var RoundNumber: Text,
                     @FXML var Prompt: Text,
                     @FXML var PrevMeme: Button,
                     @FXML var NextMeme: Button,
                     @FXML var DealButton: Button,
                     @FXML var ShuffleButton: Button,
                     @FXML var MemeImage: ImageView,
                     @FXML var WaitingLabel: Label,
                     @FXML var Shade: Rectangle,
                     @FXML var chatVBox: VBox,
                     @FXML var CardIndexView: Text,
                     @FXML var Message: TextField) {

  private var game: Game = null
  private var currentCard: MemeCard = null
  var currentPlayer: GameClientModel = null

  // pass in the game initialize by server
  def startGame(gameS: Game): Unit = {
    game = gameS

    displayFirstCard()
    showRound()
    showPrompt()
    WaitingLabel.visible = false
    Shade.visible = false
    enableButtons()
  }


  // Method to play distribute card animation
  private def displayFirstCard(): Unit = {

    // SHOULD GET THE GAMECLIENT MODEL TIED TO THE GAMECLIENT
    game.players.foreach(player => {
      if (player.getPlayerName() == Client.clientUserModel.getPlayerName()) {
        currentPlayer = player
      }
    })

    print(currentPlayer.getPlayerName())

    // Get current player hand
    val currentPlayerHand = currentPlayer.showHand()

    // UI Image view
    val cardImageView = MemeImage

    // Display the first card in the MemeImage ImageView
    if (currentPlayerHand.length != 0) {

      val firstCard = currentPlayerHand.head
      cardImageView.image = new Image(firstCard.getImage())

      currentCard = firstCard

      CardIndexView.text = "Card 1/" + currentPlayerHand.length

    } else {
      println("Game has ended in controller")

      //end game here
      Client.gameClientActor.foreach { gameClientActor =>
        gameClientActor ! GameClient.EndGame()
      }
    }
  }

  def handleChatMessage(message: String): Unit = {
    val chatMessage = new Label(message) {
      font = Font.font("Poppins", 20)
      style = "-fx-padding: 10 5 0 20px;"
    }
    chatVBox.children.add(chatMessage)
  }

  // Show round number in UI
  def showRound(): Unit = {
    RoundNumber.text = "Round " + game.getCurrentRound().toString
  }

  // show prompt in UI
  def showPrompt(): Unit = {
    Prompt.text = game.getCurrentPrompt().getPromptText()
  }

  // IF NOT ALL PLAYERS DEALT, CALL THIS METHOD
  def showWaitingLabel(): Unit = {
    WaitingLabel.visible = true
    Shade.visible = true
    PrevMeme.disable = true
    NextMeme.disable = true
    DealButton.disable = true
    ShuffleButton.disable = true
  }

  // To make every buttons clickable when waiting label is not visible
  def enableButtons(): Unit = {
    PrevMeme.disable = false
    NextMeme.disable = false
    DealButton.disable = false
    ShuffleButton.disable = false
  }

  // Method to handle start button clicked
  def handleDeal(action: ActionEvent): Unit = {

    // THIS SHOULD ALL BE DONE BY SERVER (JUST SEND SERVER WHO IS DEALING AND WHICH CARD TO DEAL)
    val currentPlayerHand = currentPlayer.showHand() // Get the deck in the players hand
    val selectedCardIndex = currentPlayerHand.indexOf(currentCard) // Get the index of the card they chose to deal in their hand

    println(selectedCardIndex)

    Client.gameClientActor.foreach { gameClientActor =>
      gameClientActor ! GameClient.DealCard(selectedCardIndex)
    }
  }

  def handleShuffle(action: ActionEvent): Unit = {
    val currentPlayerHand = currentPlayer.showHand() // Get the deck in the players hand

    Client.gameClientActor.foreach { gameClientActor =>
      gameClientActor ! GameClient.ShuffleCard()
    }
  }

  def handlePrevMeme(action: ActionEvent): Unit = {

    val currentPlayerHand = currentPlayer.showHand()

    // Get UI element
    val cardImageView = MemeImage

    // if there is more card than 1 on their hand
    if (currentPlayerHand.length > 1) {

      // Find the index of the current card
      val currentCardIndex = currentPlayerHand.indexOf(currentCard)

      // Calculate the index of the previous card (circular, looping to the last card if at the beginning)
      val prevCardIndex = (currentCardIndex - 1 + currentPlayerHand.length) % currentPlayerHand.length

      // Get the previous card
      val prevCard = currentPlayerHand(prevCardIndex)

      // Update card index number
      val cardIndex = prevCardIndex + 1
      CardIndexView.text = "Card " + cardIndex + "/" + currentPlayerHand.length

      // Update the current card in the game
      currentCard = prevCard

      // Update the image in the MemeImage ImageView
      cardImageView.image = new Image (currentCard.getImage())

    } else {
      // Handle the case when the player has only one card
      // You might choose to disable the "Prev Meme" button or take some other action
      // For example, you can display a message indicating that there's only one card.
      println("Player has only one card.")
    }
  }

  def handleNextMeme(action: ActionEvent): Unit = {
    val currentPlayerHand = currentPlayer.showHand()
    val cardImageView = MemeImage

    // Display the next card in the MemeImage ImageView
    if (currentPlayerHand.length > 1) {

      // Find the index of the current card
      val currentCardIndex = currentPlayerHand.indexOf(currentCard)

      // Calculate the index of the next card (circular, looping to the first card if at the end)
      val nextCardIndex = (currentCardIndex + 1) % currentPlayerHand.length

      // Get the next card
      val nextCard = currentPlayerHand(nextCardIndex)

      val cardIndex = nextCardIndex + 1
      CardIndexView.text = "Card " + cardIndex + "/" + currentPlayerHand.length

      // Update the current card in the game
      currentCard = nextCard

      // Update the image in the MemeImage ImageView
      cardImageView.image = new Image (currentCard.getImage())
    } else {
      println("Player has only one card")
    }
  }

  def handleMessage(action: ActionEvent): Unit = {
    val input = Message.text.value
    Message.text = ""

    println(s"GameController: Sending message '$input' to game client actor.")
    Client.gameClientActor.get ! GameClient.SendGameChatMessage(input)
  }
}

