package ch.makery.something.models

import com.hep88.protocol.JsonSerializable

// store the state of the game
case class Game (playersS: List[GameClientModel], memeDeck: MemeDeck, promptDeck: PromptDeck) {

  // initialize game attributes
  var players: List[GameClientModel] = playersS
  var gameFinished: Boolean = false
  var round: Int = 0
  var currentPrompt: PromptCard = null

  private var currentCard: MemeCard = _ // This is for UI changing purpose, should be moved to controller class

  def startGame(): Unit = {

    memeDeck.initializeDeck()

    println("Memedeck size: " + memeDeck.getCards().length)
    promptDeck.initializeDeck()
    promptDeck.shuffle()

    distributeCards()
    startNextRound()
  }

  // Method to distribute cards
  private def distributeCards(): Unit = {

    /// get number of players
    val totalPlayers = players.length

    // Ensure there are enough cards in the deck
    if (memeDeck.getCards().length < totalPlayers * 6) {
      throw new RuntimeException("Not enough cards in the deck for all players.")
    }

    memeDeck.shuffle()

    // Distribute 6 cards to each player
    for (i <- 0 until 6) {
      for (j <- 0 until totalPlayers) {
        players(j).addCardToHand(memeDeck.drawCard().getOrElse(
          throw new RuntimeException("Not enough cards in the deck for all players.")
        ))
      }
    }

    println("distributed cards")
    players.foreach(p => println(p.getPlayerName() + " got " + p.showHand().length + " cards"))
  }

  // Method to get current player of game
//  def getCurrentPlayer(): GameClientModel = {
//
//    // get player by index
//    players(currentPlayerIndex)
//
//  }
//
//  // Method to get next player of game
//  def getNextPlayer(): GameClientModel = {
//
//    // calculate next player index
//    val nextPlayerIndex = (currentPlayerIndex + 1) % players.length
//
//    // get player by index
//    players(nextPlayerIndex)
//  }

  // Method to get the current card (SHOULD BE MOVED TO CONTROLLER)
  def getCurrentCard(): MemeCard = currentCard

  // Method to set the current card (SHOULD BE MOVED TO CONTROLLER)
  def setCurrentCard(card: MemeCard): Unit = {
    currentCard = card
  }

  def getCurrentPrompt(): PromptCard = currentPrompt

  def setCurrentPrompt(prompt: PromptCard): Unit = {
    currentPrompt = prompt
  }

  def nextRound(): Unit = {
    round = round + 1
  }

  def getCurrentRound(): Int = {
    round
  }

  def resetRound(): Unit = {
    round = 1
  }

  // Method to start next turn of game
  def startNextRound(): Unit = {

    nextRound()
    setCurrentPrompt(promptDeck.drawCard().get)
  }

  def endGame(): Unit = {
    gameFinished = true
  }

  def getMemeDeck(): MemeDeck = {
    memeDeck
  }


}
