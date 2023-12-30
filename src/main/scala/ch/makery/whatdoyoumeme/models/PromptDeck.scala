package ch.makery.something.models

import scala.util.Random
import upickle.default._

class PromptDeck extends Serializable{

  //list to store all 52 cards
  private var cards: List[PromptCard] = Nil

  // Method to initialize card objects
  def initializeDeck(): Unit = {


    // initialize card id range
    // Read JSON file into a Map[Int, String]
    val jsonString = scala.io.Source.fromResource("JSON/prompts.json").mkString
    val jsonMap: Map[Int, String] = read[Map[Int, String]](jsonString)

    // add all cards to deck
    cards = jsonMap.toList.map {
      case (id, prompt) => new PromptCard(id, prompt)
    }

  }

  // Method to shuffle deck
  def shuffle(): Unit = {

    // Shuffle the cards
    cards = Random.shuffle(cards)

  }

  // Method to draw top card of the deck
  def drawCard(): Option[PromptCard] = {
    cards match {
      case Nil => None
      case head :: tail =>
        cards = tail
        Some(head)
    }
  }

  // getter
  def getCards(): List[PromptCard] = {
    cards
  }

}
