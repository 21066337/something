package ch.makery.something.controllers

import akka.util.Helpers.Requiring
import ch.makery.something.Client
import ch.makery.something.Client.showHowToPlayDialog
import ch.makery.something.util.MusicPlayer
import javafx.fxml.FXML
import scalafx.event.{ActionEvent, Event}
import scalafx.scene.layout.AnchorPane
import scalafx.scene.media.{Media, MediaPlayer, MediaView}
import scalafx.stage.{Stage, WindowEvent}
import scalafxml.core.macros.sfxml
import scalafx.Includes._

@sfxml
class HowToPlayController(@FXML var tutorial: MediaView,
                          @FXML var root: AnchorPane) {

  // Initialize media and media player
  var media: Media = _
  private var mediaPlayer: MediaPlayer = _

  // Method to play video
  def initialize(): Unit = {

    // get video to be played
    val resourceUrl = getClass.getResource("/Video/tutorial.mp4")

    // test line for video source
    println("Resource URL: " + resourceUrl)

    if (resourceUrl != null) {

      // set video as media
      media = new Media(resourceUrl.toExternalForm)

      // set media to media player
      mediaPlayer = new MediaPlayer(media)

      // set media player to scene media player element
      tutorial.setMediaPlayer(mediaPlayer)

      // play video
      mediaPlayer.play()

    } else {
      //print error message if video not found
      println("Resource not found: Video/tutorial.mp4")

    }
  }

  // Method to handle window close event
  val handleWindowClose: Event => Unit = (_: Event) => {
    // Stop the media player when the window is closed
    if (mediaPlayer != null) {
      mediaPlayer.stop()
    }
  }

  // Method to handle pause button clicked
  def handlePause(action: ActionEvent): Unit = {

    //play button clicked sound
    val file = getClass.getResource("/Sounds/click.wav")
    MusicPlayer.playSoundEffect(file)

    // pause video
    val mediaPlayer = tutorial.mediaPlayer.value
    mediaPlayer.pause()

  }

  // Method to handle play button clicked
  def handlePlay(action: ActionEvent): Unit = {

    //play button clicked sound
    val file = getClass.getResource("/Sounds/click.wav")
    MusicPlayer.playSoundEffect(file)

    // Play video
    val mediaPlayer = tutorial.mediaPlayer.value
    mediaPlayer.play()

  }

  // Method to handle quit button clicked
  def handleQuit(action: ActionEvent): Unit = {

    //play button clicked sound
    val file = getClass.getResource("/sounds/click.wav")
    MusicPlayer.playSoundEffect(file)

    // hide how to play window
    root.getScene.getWindow.hide()

  }
}
