package ch.makery.something.utils

import scalafx.animation.{Animation, KeyFrame, Timeline}
import scalafx.util.Duration

object Timer {

  // Initialize attributes
  private var onTimerFinishCallback: () => Unit = () => {}
  private var currentCountdownDuration: Int = 0
  private val timeline: Timeline = new Timeline {
    cycleCount = 1 // Only one cycle (not repeating)
    onFinished = (_: javafx.event.ActionEvent) => {
      // Execute the onTimerFinish callback when the countdown finishes
      onTimerFinishCallback()
    }
  }


  // Method to start timer countdown
  def startCountdown(duration: Int, onFinish: () => Unit): Unit = {

    // Stop the timeline if it is already running
    if (timeline.status == Animation.Status.Running) {
      timeline.stop()
    }

    // set duration and callback of timer
    currentCountdownDuration = duration
    onTimerFinishCallback = onFinish

    // Create the KeyFrame with the duration and an empty event handler (we don't need it)
    val keyFrame = KeyFrame(Duration(duration * 1000), onFinished = (_: javafx.event.ActionEvent) => {
      // Decrement the current countdown duration and update the label
      currentCountdownDuration -= 1
    })

    // Set the KeyFrame to the timeline
    timeline.keyFrames = Seq(keyFrame)

    // Start the timeline
    timeline.playFromStart()

  }


  // Method to get current countdown duration
  def getCurrentCountdownDuration: Int = currentCountdownDuration

  // Method to stop countdown
  def stopCountdown(): Unit = {

    // Stop the timeline if it is running
    if (timeline.getStatus == Animation.Status.Running) {
      timeline.stop()
    }
  }

}
