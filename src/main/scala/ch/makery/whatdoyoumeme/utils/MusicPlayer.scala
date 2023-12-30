package ch.makery.something.util

import java.net.URL
import javax.sound.sampled.{AudioSystem, Clip}


object MusicPlayer {

  // initialize music clip
  private var backgroundMusicClip: Option[Clip] = None


  // Method to play background music
  def playBackgroundMusic(filePath: URL): Unit = {

    // stop initial background music
    stopBackgroundMusic()

    try {
      // music player logic
      val audioInput = AudioSystem.getAudioInputStream(filePath)
      val clip = AudioSystem.getClip
      clip.open(audioInput)
      clip.start()

      // music loops continuously
      clip.loop(Clip.LOOP_CONTINUOUSLY)

      backgroundMusicClip = Some(clip)
    } catch {
      // print error if any
      case ex: Exception =>
        ex.printStackTrace()
    }
  }


  // Method to stop background music
  def stopBackgroundMusic(): Unit = {

    // stop background music clip if it is running
    backgroundMusicClip.foreach { clip =>
      if (clip.isRunning) {
        clip.stop()
      }
      clip.close()
    }

    // empty background music clip
    backgroundMusicClip = None
  }


  // Method to play sound effect
  def playSoundEffect(filePath: URL): Unit = {

    try {
      // logic for playing sound effects
      val audioInput = AudioSystem.getAudioInputStream(filePath)
      val clip = AudioSystem.getClip
      clip.open(audioInput)

      // start playing clip
      clip.start()

    } catch {
      // print error if any
      case ex: Exception =>
        ex.printStackTrace()
    }
  }

}