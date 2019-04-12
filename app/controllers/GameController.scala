package controllers

import java.net.{URLDecoder, URLEncoder}
import java.text.Normalizer.Form

import com.sun.corba.se.spi.orbutil.fsm.Action
import com.sun.corba.se.spi.orbutil.fsm.Guard.Result
import javax.inject.Inject
import models._
import play.api.data._
import play.api.mvc._

class GameController @Inject()(cc: MessagesControllerComponents)
                              (implicit system: ActorSystem, mat: Materializer) extends MessagesAbstractController(cc) with ControllerUtils {

  def testGame(players: Int): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    val game = GameManager.makeNewGame
    val playerNames: Seq[String] = ('A' to 'Z') map { _.toString } take players
    playerNames foreach { game.addPlayerToLobby }
    game.startAssignment()
    game.startPlay()
    Redirect(routes.GameController.showGame(game.gameId, Some("A")))
  }

  def index: Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.index(JoinForm.form))
  }

  def createGame: Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    val game = GameManager.makeNewGame
    Redirect(routes.GameController.index()).flashing("INFO" -> s"Your game ID is: ${game.gameId}")
  }

  def joinGame: Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    def errorFunction(formWithErrors: Form[JoinForm.JoinRequest]) =
      BadRequest(views.html.index(formWithErrors))

    def successFunction(joinRequest: JoinForm.JoinRequest): Result =
      onGame(joinRequest.id) { game =>
        val name = joinRequest.playerName
        if (game.getLobbiedPlayers.contains(name)) {
          Redirect(routes.GameController.index()).flashing("ERROR" -> s"Player with name $name already in queue")
        } else {
          game.addPlayerToLobby(name)
          Redirect(routes.GameController.showGame(joinRequest.id, Some(name)))
        }
      }

    JoinForm.form.bindFromRequest.fold(errorFunction, successFunction)
  }

  def startAssignment(gameId: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    onGame(gameId) { game: Game =>
      game.startAssignment()
      Redirect(routes.GameController.showGame(gameId))
    }
  }

  def startPlay(gameId: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    onGame(gameId) { game: Game =>
      game.startPlay()
      Redirect(routes.GameController.showGame(gameId))
    }
  }

  def showGame(gameId: String, playerName: Option[String]): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    var pName: String = playerName.getOrElse(URLDecoder.decode(request.cookies("playerName").value, "UTF-8"))
    pName = URLEncoder.encode(pName, "UTF-8")
    onGame(gameId) { game: Game =>
      game.gameState match {
        case Lobbying =>
          Ok(views.html.lobby(game.getLobbiedPlayers, gameId)).withCookies(Cookie("playerName", pName)).bakeCookies()
        case Assigning =>
          Ok(views.html.game(game)).withCookies(Cookie("playerName", pName)).bakeCookies()
        case Running =>
          Ok(views.html.gameboard(game)).withCookies(Cookie("playerName", pName)).bakeCookies()
        case _ =>
          Redirect(routes.GameController.index()).flashing("ERROR" -> "That part of the game hasn't been implemented yet")
      }
    }
  }

  def endTurn(gameId: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    onGame(gameId) { game: Game =>
      game.nextTurn()
      Redirect(routes.GameController.showGame(gameId))
    }
  }

  def addArmiesToTerritory(gameId: String, territoryId: Int, amount: Int): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    onGame(gameId) { game: Game =>
      game.board.territories(territoryId).armies += amount
      Redirect(routes.GameController.showGame(gameId))
    }
  }

}
