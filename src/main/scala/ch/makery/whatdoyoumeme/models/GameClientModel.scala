package ch.makery.something.models

import akka.actor.typed.ActorRef
import ch.makery.something.Actor.GameClient
import com.hep88.protocol.JsonSerializable

import scala.collection.mutable.ListBuffer

case class GameClientModel(playerNameS: String) extends Serializable {

  private val playerName = playerNameS
  var gameClientRef: ActorRef[GameClient.Command] = null
  private var hand: ListBuffer[MemeCard] = ListBuffer.empty[MemeCard]
  private var isTurn: Boolean = false
  private var shuffled:Boolean = false
  private var dealtCard: MemeCard = null
  private var isReady: Boolean = false

  def getPlayerName(): String = {
    playerName
  }


  def addCardToHand(cardToAdd: MemeCard): Unit = {
    hand += cardToAdd
  }

  def removeCardFromHand(cardToRemove: MemeCard): Unit = {
    hand -= cardToRemove
  }

  def clearHand(): Unit = {
    hand = ListBuffer.empty[MemeCard]
  }

  def showHand(): ListBuffer[MemeCard] = {
    hand
  }

  // set player's turn
  def setTurn(turn: Boolean): Unit = {
    isTurn = turn
  }

  // get player's turn boolean
  def getTurn(): Boolean = {
    isTurn
  }

  // get player passed status
  def getHasShuffled(): Boolean = {
    shuffled
  }

  // set player's passed status
  def setHasShuffled(shuffle: Boolean): Unit = {
    shuffled = shuffle
  }

  // get dealt cards of player
  def getDealtCard(): MemeCard = {
    dealtCard
  }

  // set player's dealt cards
  def setDealtCard(card: MemeCard): Unit = {
    dealtCard = card
  }

  // empty dealt card list
  def clearDealtCard(): Unit = {
    dealtCard = null
  }

  def getReadyState(): Boolean = {
    isReady
  }

  def setReadyState(state: Boolean): Unit = {
    isReady = state
  }

  // Method to create a new instance with updated ready state


}
