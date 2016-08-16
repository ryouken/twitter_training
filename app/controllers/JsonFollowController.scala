package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.db.slick._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.Tables._
import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

import scala.concurrent.Future
import services.UserService

/**
  * Created by ryoken.kojima on 2016/08/04.
  */

object JsonFollowController {
  case class FollowList(userName: String, profileText: Option[String])
  case class FollowForm(relation_id: Int, followed_id: Int)

  implicit val FollowFormFormat  = Json.format[FollowForm]

  implicit val relationsRowWritesFormat = new Writes[RelationsRow]{
    def writes(relation: RelationsRow): JsValue = {
      Json.obj(
        "relation_id"       -> relation.relationId,
        "follow_user_id"    -> relation.followUserId,
        "followed_user_id"  -> relation.followedUserId
      )
    }
  }

  val followForm = Form(
    mapping(
      "relation_id"   -> number,
      "followed_id"   -> number
    )(FollowForm.apply)(FollowForm.unapply)
  )
}

class JsonFollowController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                 val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonFollowController._

  def followlist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    val query = for {
      r <- Relations if r.followUserId === sessionUserId
      u <- Users     if u.userId       === r.followedUserId
    } yield (u.userName, u.profileText)
    db.run(query.result).map { seq =>
      val json = Json.toJson(
        seq.map{ s =>
          Map("user_name" -> s._1, "profile_text" -> s._2.getOrElse(""))
        }
      )
      Ok(json)
    }
  }

  def followedlist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    val query = for {
      r <- Relations if r.followedUserId === sessionUserId
      u <- Users     if u.userId         === r.followUserId
    } yield (u.userName, u.profileText)
    db.run(query.result).map { seq =>
      val json = Json.toJson(
        seq.map{ s =>
          Map("user_name" -> s._1, "profile_text" -> s._2.getOrElse(""))
        }
      )
      Ok(json)
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
      // NGの場合はバリデーションエラーを返す
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