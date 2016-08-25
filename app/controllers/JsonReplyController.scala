package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import play.api.libs.json._
import services.UserService

/**
  * Created by ryoken.kojima on 2016/08/08.
  */
// TODO バリデーション
object JsonReplyController {
  case class ReplyListForm(tweet_id: Int)
  case class ReplyCreateForm(reply_id: Int, tweet_id: Int, reply_text: String)

  implicit val replyListFormFormat = Json.format[ReplyListForm]
  implicit val replyCreateFormFormat = Json.format[ReplyCreateForm]
}

class JsonReplyController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                    val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonReplyController._

  /**
    * リスト表示
    */
  // TODO Mapおかしい
  def list = Action.async(parse.json) { implicit rs =>
    rs.body.validate[ReplyListForm].map { form =>
      db.run(Replies.filter(rp => rp.tweetId === form.tweet_id)
                    .join(Users).on(_.userId === _.userId)
                    .sortBy(rp => rp._1.replyId.asc).result).map { t =>
        val json = Json.toJson(
          t.map { tt =>
            Map(
              "reply_id"        -> tt._1.replyId.toString,
              "reply_user_name" -> tt._2.userName,
              "reply_text"      -> tt._1.replyText
            )
          }
        )
        Ok(json)
      }
    }.recoverTotal { e =>
      Future { BadRequest(Json.obj("result" -> "delete_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    val sessionUserId = UserService.getJSSessionId(rs)
    rs.body.validate[ReplyCreateForm].map { form =>
      val reply = RepliesRow(0, sessionUserId, form.tweet_id, form.reply_text)
      db.run(Replies += reply).map { _ =>
        Ok(Json.obj("result" -> "create_success"))
      }
    }.recoverTotal { e =>
      Future { BadRequest(Json.obj("result" -> "create_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 削除実行
    */
  // TODO delete追加
  def delete = TODO
  //  def delete = Action.async(parse.json) { implicit rs =>
  //    rs.body.validate[ReplyForm].map { form =>
  //      db.run(Replies.filter(t => t.replyId === form.reply_id.bind).delete).map { _ =>
  //        Ok(Json.obj("result" -> "delete_success"))
  //      }
  //    }.recoverTotal { e =>
  //      Future { BadRequest(Json.obj("result" -> "delete_failure", "error" -> JsError.toJson(e))) }
  //    }
  //  }

}