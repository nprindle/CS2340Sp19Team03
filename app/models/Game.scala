package models

import scala.collection.mutable
import scala.util.Random

class Game(val gameId: String) {
  val turn: Int = 0
  val board: Board = new Board()
  private val lobbiedPlayers: mutable.ArrayBuffer[String] = mutable.ArrayBuffer[String]()
  var players: Seq[Player] = Seq[Player]()
  var gameState: GameState = Lobbying

  def addPlayerToLobby(name: String): Unit = lobbiedPlayers += name

  def startAssignment(): Unit = {
    if (gameState == Lobbying) {
      gameState = Assigning
      val armies = 50 - 5 * lobbiedPlayers.length
      players = Random.shuffle(for {
        name <- lobbiedPlayers
      } yield Player(name, armies, gameId))
    }
  }

  def startPlay(): Unit = {
    if (gameState == Assigning) {
      gameState = Running
    }
  }

  def getLobbiedPlayers: Seq[String] = lobbiedPlayers

}

object Game {
  val idLength: Int = 4
}
