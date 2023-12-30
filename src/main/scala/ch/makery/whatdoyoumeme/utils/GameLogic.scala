package ch.makery.something.utils

import ch.makery.something.{Client, GameServer}
import ch.makery.something.models.{Game, GameClientModel, MemeCard, MemeDeck}
import scalafx.animation.AnimationTimer

// game loop
class GameLogic(gameS: Game) {

  // initialize variables
  var game: Game = gameS
  var gameInProgress = false

  // Map to keep track of players who have dealt cards in the current round
  private var playersDealt: Set[GameClientModel] = Set()
  var playersDealtCard: Map[GameClientModel, MemeCard] = Map()
  private var thisRoundEnd: Boolean = false
  private var timerEnded: Boolean = false
  private var playersVote: Map[GameClientModel, Int] = Map()
  var voteRoundEnd: Boolean = false
  var votes: Int = 0

  // Method to initialize game
  def initializeGame(): Unit = {

    // set game and game status
    gameInProgress = true

    //start game loop
    game.startGame()
    startGameLoop()

    // Initialize the playersDealtCard map with all player IDs
    playersDealtCard = game.players.map(player => player -> null).toMap

    // test line for game loop
    println("loop started")

  }

  def shuffleCards(player: GameClientModel): Unit = {

    game.players.find(_ == player).foreach { player =>

      // Check if the player has already shuffled in this round
      if (!player.getHasShuffled) {
        val playerHandLength = player.showHand().length

        player.showHand().foreach { card =>
          game.memeDeck.addCard(card)
          player.removeCardFromHand(card)
        }

        // Shuffle the meme deck
        game.memeDeck.shuffle()

        for (i <- 0 until playerHandLength) {

          val newCard =  game.memeDeck.drawCard().get
          println(newCard.getMemeID())

          player.addCardToHand(newCard)

        }

        // Mark the player as having shuffled
        // Mark the player as having dealt a card for the current round
        playersDealt += player
        // Update the dealt card in the playersDealtCard map
        playersDealtCard += (player -> null)

        player.setHasShuffled(true)
      }
    }
  }

  def dealCard(playerGiven: GameClientModel, cardIndex: Int): Unit = {

    game.players.find(_ == playerGiven).foreach { player =>
      // Check if the provided index is valid
      if (cardIndex >= 0 && cardIndex < player.showHand().length) {
        val selectedCard = player.showHand()(cardIndex)

        // Update the game state and remove the dealt card from the player's hand
        player.setDealtCard(selectedCard)

        // Mark the player as having dealt a card for the current round
        playersDealt += player
        // Update the dealt card in the playersDealtCard map
        playersDealtCard += (player -> selectedCard)
      } else {
        println("Invalid card index")
      }
    }
  }

  def getUpdatedGame(): Game = {
    game
  }

  def addVote(player: GameClientModel): Unit = {
    val currentVote = playersVote.getOrElse(player, 0)
    val newVote = currentVote + 1
    playersVote += (player -> newVote)
    votes += 1

    println("voted for " + player.getPlayerName())
  }

  def checkPlayerDealStatus(): Boolean = {
    if (playersDealt.size == game.players.size) {
      // Clear the playersDealt set for the next round
      playersDealt = Set()
      true
    } else {
      false
    }
  }

  def checkVotingEnd(): Boolean = {
    println("votes: " + votes + " Players: " + game.players.size)
    votes == game.players.size
  }

  def updateRound(): Unit = {
    playersVote = Map()
    thisRoundEnd = true
    voteRoundEnd = false
  }

  def getWinningPlayer(): Unit = {
    // Find the player with the maximum votes
    val winningPlayer = playersVote.maxBy(_._2)._1
    println("Winning player: " + winningPlayer.getPlayerName())

    if(winningPlayer != null){
      val dealtCard = winningPlayer.getDealtCard()

      println("Winning player dealt: " + dealtCard.getMemeID())

      if (dealtCard != null) {
        winningPlayer.removeCardFromHand(dealtCard)
        println(s"Player ${winningPlayer.getPlayerName()} wins with ${playersVote(winningPlayer)} votes!")
      }
    } else {
      println("Its a Tie!")
    }

  }

  // Method to handle player's turns
  def handlePlayerTurns(): Unit = {

    votes = 0

    getWinningPlayer()
    println("winning player ran")

    game.players.foreach(player => {
      player.setHasShuffled(false)
      player.clearDealtCard()
    })

    updateRound()
    game.startNextRound()

  }

  def endGame(): Unit = {
    game.endGame()
  }

  // Method of main game loop
  private def startGameLoop(): Unit = {

    // game loop
    val gameLoop: AnimationTimer = AnimationTimer { deltaTime =>

      // check statement for game loop
      if (gameInProgress) {

        // stop game loop if game is finished
        if (game.gameFinished) {
          gameInProgress = false

          // test line for loop stopping
          println("loop stopped")
        }
      }
    }

    // start game loop
    gameLoop.start()

  }

}
