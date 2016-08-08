package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.db.slick._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.Tables._
import javax.inject.Inject
import play.api.libs.json._

/**
  * Created by ryoken.kojima on 2016/08/04.
  */

object JsonFollowController {
  // TweetsRowをJSONに変換するためのWritesを定義
  implicit val relationsRowWritesFormat = new Writes[RelationsRow]{
    def writes(relation: RelationsRow): JsValue = {
      Json.obj(
        "relation_id"       -> relation.relationId,
        "follow_user_id"    -> relation.followUserId,
        "followed_user_id"  -> relation.followedUserId
      )
    }
  }
}

class JsonFollowController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                 val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

  def followlist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val query = for {
      r <- Relations if r.followUserId === sessionUserId
      u <- Users     if u.userId       === r.followedUserId
    } yield (u.userName, u.profileText)
    db.run(query.result).map { seq =>
      val json = Json.toJson(
        seq.map{ s =>
          Map("user_name" -> s._1, "profile_text" -> s._2)
        }
      )
      Ok(json)
    }
  }

  def followedlist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val query = for {
      r <- Relations if r.followedUserId === sessionUserId
      u <- Users     if u.userId         === r.followUserId
    } yield (u.userName, u.profileText, r.followUserId)
    db.run(query.result).map { seq =>
      val json = Json.toJson(
        seq.map{ s =>
          Map("user_name" -> s._1, "profile_text" -> s._2)
        }
      )
      Ok(json)
    }
  }

  def create(followed_id: Int) = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val relation = RelationsRow(0, sessionUserId, followed_id)
    db.run(Relations += relation).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }

  def remove(id: Int) = Action.async { implicit rs =>
    db.run(Relations.filter(t => t.relationId === id.bind).delete).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }

}