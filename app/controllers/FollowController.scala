package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import slick.driver.MySQLDriver.api._

/**
  * Created by ryoken.kojima on 2016/08/04.
  */

class FollowController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

  def followlist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val query = for {
      r <- Relations if r.followUserId === sessionUserId
      u <- Users if u.userId === r.followedUserId
    } yield (u.userName, u.profileText, r.followedUserId)
    db.run(query.result).map { seq =>
      Ok(views.html.follow.list(seq))
    }
  }

  def followedlist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val query = for {
      r <- Relations if r.followedUserId === sessionUserId
      u <- Users if u.userId === r.followUserId
    } yield (u.userName, u.profileText, r.followUserId)
    db.run(query.result).map { seq =>
      Ok(views.html.followed.list(seq))
    }
  }

  def create(followed_id: Int) = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val relation = RelationsRow(0, sessionUserId, followed_id)
    db.run(Relations += relation).map { _ =>
      // 一覧画面へリダイレクト
      Redirect(routes.UserController.list)
    }
  }

  def delete(id: Int) = Action.async { implicit rs =>
    db.run(Relations.filter(t => t.relationId === id.bind).delete).map { _ =>
      // 一覧画面へリダイレクト
      Redirect(routes.FollowController.followlist)
    }
  }

}