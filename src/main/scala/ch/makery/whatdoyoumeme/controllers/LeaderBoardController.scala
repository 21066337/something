package ch.makery.something.controllers

import ch.makery.something.Actor.GameClient
import ch.makery.something.Client
import ch.makery.something.models.GameClientModel
import javafx.fxml.FXML
import scalafx.event.ActionEvent
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

@sfxml
class LeaderBoardController(@FXML var firstPlaceLabel: Label,
                            @FXML var secondPlaceLabel: Label,
                            @FXML var thirdPlaceLabel: Label) {

  // update leaderboard
  def updateLeaderboard(players: List[GameClientModel]): Unit = {
    val sortedPlayers = players.sortBy(_.showHand().length)

    firstPlaceLabel.text = s"1st Place: Player ${sortedPlayers(0).getPlayerName()}"
    secondPlaceLabel.text = s"2nd Place: Player ${sortedPlayers(1).getPlayerName()}"

    if(players.size >2){
      thirdPlaceLabel.text = s"3rd Place: Player ${sortedPlayers(2).getPlayerName()}"
    }else{
      thirdPlaceLabel.visible = false
    }
  }

  // Join back lobby
  def handleLobby(action: ActionEvent): Unit = {
    Client.gameClientActor.foreach(_ ! GameClient.RequestToJoinBackLobby())
  }

  // Quit lobby
  def handleQuit(action: ActionEvent): Unit = {

    Client.gameClientActor.foreach(_ ! GameClient.QuitLobby)

    println("Redirecting to main menu...")
    Client.showMainMenuScene()
  }

}