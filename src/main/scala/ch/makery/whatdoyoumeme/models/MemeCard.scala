package ch.makery.something.models

import scalafx.scene.image.Image

class MemeCard (memeIDS: Int, imagePath: String) extends Serializable{

  private val memeID = memeIDS
  private val cardImagePath = imagePath

  def getMemeID(): Int = {
    memeID
  }

  def getImage(): String = {
    cardImagePath
  }

}
