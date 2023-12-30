package ch.makery.something.controllers

import akka.actor.typed.ActorRef
import ch.makery.something.models.User
import ch.makery.something.{Client, ClientGuardian}
import javafx.fxml.FXML
import scalafx.event.ActionEvent
import scalafx.scene.control.TextField
import scalafxml.core.macros.sfxml


@sfxml
class SignInController(@FXML var usernameField: TextField,
                       @FXML var serverIpField: TextField,
                       @FXML var serverPortField: TextField,
                       @FXML var clientIpField: TextField,
                       @FXML var clientPortField: TextField) {

  // Handle joining logic
  def handleJoin(): Unit = {
    if (usernameField != null && serverIpField != null && serverPortField != null &&
      clientIpField != null && clientPortField != null
    ) {
      Client.initializeClientActorSystem(usernameField.text.value, clientIpField.text.value, clientPortField.text.value,
        serverIpField.text.value, serverPortField.text.value)

    }
  }

  // when signIn button is pressed
  def handleSignIn(action: ActionEvent): Unit = {
    handleJoin()

    // open new menu scene when when handleSignIn button is pressed and Join is done
    Client.showMainMenuScene()
  }
}
