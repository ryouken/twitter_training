package controllers

/**
  * Created by ryoken.kojima on 2016/08/08.
  */

import java.sql.Timestamp
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import play.api.libs.json._
import services.UserService

object JsonReplyController {
  // フォームの値を格納するケースクラス
  case class ReplyCreateForm(reply_id: Int, tweet_id: Int, reply_text: String)

  implicit val replyCreateFormFormat = Json.format[ReplyCreateForm]

//   ReplysRowをJSONに変換するためのWritesを定義
  implicit val repliesRowWritesFormat = new Writes[RepliesRow]{
    def writes(reply: RepliesRow): JsValue = {
      Json.obj(
        "reply_id"    -> reply.replyId,
        "user_id"     -> reply.userId,
        "tweet_id"    -> reply.tweetId,
        "reply_text"  -> reply.replyText
      )
    }
  }

  // formから送信されたデータ ⇔ ケースクラスの変換を行う

  val replyCreateForm = Form(
    mapping(
      "reply_id"   -> number,
      "tweet_id"   -> number,
      "reply_text" -> nonEmptyText(maxLength = 140)
    )(ReplyCreateForm.apply)(ReplyCreateForm.unapply)
  )

}

class JsonReplyController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                    val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonReplyController._

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    val sessionUserId = UserService.getJSSessionId(rs)
    rs.body.validate[ReplyCreateForm].map { form =>
      // OKの場合はリプライを登録
      val reply = RepliesRow(0, sessionUserId, form.tweet_id, form.reply_text)
      db.run(Replies += reply).map { _ =>
        Ok(Json.obj("result" -> "create_success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" -> "create_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 削除実行
    */
  def delete = TODO
  //  def delete = Action.async(parse.json) { implicit rs =>
  //    rs.body.validate[ReplyForm].map { form =>
  //      db.run(Replys.filter(t => t.replyId === form.reply_id.bind).delete).map { _ =>
  //        Ok(Json.obj("result" -> "delete_success"))
  //      }
  //    }.recoverTotal { e =>
  //      Future { BadRequest(Json.obj("result" -> "delete_failure", "error" -> JsError.toJson(e))) }
  //    }
  //  }

}