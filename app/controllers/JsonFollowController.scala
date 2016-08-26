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

import scala.concurrent.Future
import services.UserService

/**
  * Created by ryoken.kojima on 2016/08/04.
  */
// TODO バリデーション
object JsonFollowController {
  case class FollowForm(relation_id: Int, followed_id: Int)
  case class FollowListForm(relation_id:Int, user_name:String, profile_text: Option[String])
  case class FollowedListForm(user_id: Int, user_name: String, profile_text: Option[String])

  implicit val FollowFormFormat = Json.format[FollowForm]
  implicit val FollowListWritesFormat = new Writes[FollowListForm]{
    def writes(followlist: FollowListForm): JsValue = {
      Json.obj(
        "relation_id"  -> followlist.relation_id,
        "user_name"    -> followlist.user_name,
        "profile_text" -> followlist.profile_text
      )
    }
  }
  implicit val FollowedListWritesFormat = new Writes[FollowedListForm]{
    def writes(followedlist: FollowedListForm): JsValue = {
      Json.obj(
        "user_id"      -> followedlist.user_id,
        "user_name"    -> followedlist.user_name,
        "profile_text" -> followedlist.profile_text
      )
    }
  }
}

class JsonFollowController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                 val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonFollowController._

  def followlist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    val query = for {
      r <- Relations if r.followUserId === sessionUserId
      u <- Users if u.userId === r.followedUserId
    } yield (r.relationId, u.userName, u.profileText)
    db.run(query.sortBy(r => r._3.desc).result).map { seq =>
      val json = seq.map { t =>
        FollowListForm(t._1, t._2, t._3)
      }
      Ok(Json.obj("follow" -> json))
    }
  }

  def followedlist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    val query = for {
      r <- Relations if r.followedUserId === sessionUserId
      u <- Users     if u.userId         === r.followUserId
    } yield (u.userId, u.userName, u.profileText, r.relationId)
    db.run(query.sortBy(r => r._4.desc).result).map { seq =>
      val json = seq.map { t =>
          FollowedListForm(t._1, t._2, t._3)
        }
      Ok(Json.obj("followed" -> json))
    }
  }

  def create = Action.async(parse.json) { implicit rs =>
    rs.body.validate[FollowForm].map { form =>
      val sessionUserId = UserService.getJSSessionId(rs)
      val relation = RelationsRow(0, sessionUserId, form.followed_id)
      db.run(Relations += relation).map { _ =>
        Ok(Json.obj("result" -> "create_success"))
      }
    }.recoverTotal { e =>
      Future { BadRequest(Json.obj("result" -> "create_failure", "error" -> JsError.toJson(e))) }
    }
  }

  def delete = Action.async(parse.json) { implicit rs =>
    rs.body.validate[FollowForm].map { form =>
      db.run(Relations.filter(t => t.relationId === form.relation_id.bind).delete).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      Future { BadRequest(Json.obj("result" -> "delete_failure", "error" -> JsError.toJson(e))) }
    }
  }

}