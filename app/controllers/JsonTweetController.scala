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

object JsonTweetController {
  // フォームの値を格納するケースクラス
  case class TweetForm(text: String)

  implicit val tweetFormFormat = Json.format[TweetForm]

  // TweetsRowをJSONに変換するためのWritesを定義
  implicit val tweetsRowWritesFormat = new Writes[TweetsRow]{
    def writes(tweet: TweetsRow): JsValue = {
      Json.obj(
        "tweet_id"    -> tweet.tweetId,
        "user_id"     -> tweet.userId,
        "tweet_text"  -> tweet.tweetText,
        "timestamp"   -> tweet.timestamp
      )
    }
  }

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val tweetForm = Form(
    mapping(
      "text" -> nonEmptyText(maxLength = 140)
    )(TweetForm.apply)(TweetForm.unapply)
  )
}

class JsonTweetController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonTweetController._

  /**
    * タイムライン表示
    */
  def timeline = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val query = for {
      r <- Relations if r.followUserId === sessionUserId
      u <- Users     if u.userId       === r.followedUserId
      t <- Tweets    if t.userId       === r.followedUserId
    } yield (u.userName, t.tweetText, t.timestamp)
    db.run(query.result).map { seq =>
      val json = Json.toJson(
        seq.map{ s =>
          Map("user_name" -> s._1, "tweet_text" -> s._2, "timestamp" -> s._3)
        }
      )
      Ok(json)
    }
  }

  /**
    * 自ツイート表示
    */
  def mylist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    db.run(Tweets.filter(t => t.userId === sessionUserId).result).map { tweets =>
      Ok(Json.obj("tweets" -> tweets))
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val timestamp = new Timestamp(System.currentTimeMillis())
    rs.body.validate[TweetForm].map { form =>
      // OKの場合はユーザを登録
      val tweet = TweetsRow(0, sessionUserId, form.text, timestamp)
      db.run(Tweets += tweet).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future {
        BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
      }
    }
  }

  /**
    * 削除実行
    */
  def remove(id: Int) = Action.async { implicit rs =>
    // ユーザを削除
    db.run(Tweets.filter(t => t.tweetId === id.bind).delete).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }

}