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

object JsonTweetController {
  // フォームの値を格納するケースクラス
  case class TweetForm(tweet_id: Int, tweet_text: String)

  implicit val tweetFormFormat = Json.format[TweetForm]

  // TweetsRowをJSONに変換するためのWritesを定義
  implicit val tweetsRowWritesFormat = new Writes[TweetsRow]{
    def writes(tweet: TweetsRow): JsValue = {
      Json.obj(
        "tweet_id"    -> tweet.tweetId,
        "user_id"     -> tweet.userId,
        "tweet_text"  -> tweet.tweetText,
        "created_at"  -> tweet.createdAt
      )
    }
  }

  implicit val usersRowWritesFormat = new Writes[UsersRow]{
    def writes(user: UsersRow): JsValue = {
      Json.obj(
        "user_id"        -> user.userId,
        "email"          -> user.email,
        "user_name"      -> user.userName,
        "password"       -> user.password,
        "profile_text"   -> user.profileText
      )
    }
  }

  implicit val relationsRowWritesFormat = new Writes[RelationsRow]{
    def writes(relation: RelationsRow): JsValue = {
      Json.obj(
        "relation_id"       -> relation.relationId,
        "follow_user_id"    -> relation.followUserId,
        "followed_user_id"  -> relation.followedUserId
      )
    }
  }

  implicit val repliesRowWritesFormat = new Writes[RepliesRow]{
    def writes(reply: RepliesRow): JsValue = {
      Json.obj(
        "reply_id"   -> reply.replyId,
        "user_id"    -> reply.userId,
        "tweet_id"   -> reply.tweetId,
        "reply_text" -> reply.replyText
      )
    }
  }

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val tweetForm = Form(
    mapping(
      "tweet_id"   -> number,
      "tweet_text" -> nonEmptyText(maxLength = 140)
    )(TweetForm.apply)(TweetForm.unapply)
  )

}

class JsonTweetController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                    val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonTweetController._

  def timeline = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    db.run(Tweets.join(Relations).on(_.userId === _.followedUserId)
      .join(Users).on(_._1.userId === _.userId)
      .filter(_._1._2.followUserId === sessionUserId)
      .sortBy(t => t._1._1.tweetId.desc).result).map { t =>
      val json = Json.toJson(
        t.map { tt =>
          Map(
            "tweet_id"        -> tt._1._1.tweetId.toString,
            "tweet_user_name" -> tt._2.userName,
            "tweet_text"      -> tt._1._1.tweetText
          )
        }
      )
      Ok(json)
    }
  }

  /**
    * 自ツイート表示
    */
  def mylist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    db.run(Tweets.filter(t => t.userId === sessionUserId).sortBy(t => t.tweetId.desc).result).flatMap { tweets =>
      db.run(Users.result).map { users =>
        Ok(Json.obj("tweets" -> tweets, "users" -> users))
      }
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    val sessionUserId = UserService.getJSSessionId(rs)
    val timestamp = new Timestamp(System.currentTimeMillis())
    rs.body.validate[TweetForm].map { form =>
      // OKの場合はツイートを登録
      val tweet = TweetsRow(0, sessionUserId, form.tweet_text, timestamp)
      db.run(Tweets += tweet).map { _ =>
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
  def delete = Action.async(parse.json) { implicit rs =>
    rs.body.validate[TweetForm].map { form =>
      db.run(Tweets.filter(t => t.tweetId === form.tweet_id.bind).delete).map { _ =>
        Ok(Json.obj("result" -> "delete_success"))
      }
    }.recoverTotal { e =>
      Future { BadRequest(Json.obj("result" -> "delete_failure", "error" -> JsError.toJson(e))) }
    }
  }

}