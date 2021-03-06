package controllers

import java.sql.Timestamp
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
object JsonTweetController {
  case class TweetForm(tweet_id: Int, tweet_text: String)
  case class TimelineForm(tweet_id: Int, tweet_user_name: String, tweet_text: String)

  implicit val tweetFormFormat = Json.format[TweetForm]
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
  implicit val timelineWritesFormat = new Writes[TimelineForm]{
    def writes(tweet: TimelineForm): JsValue = {
      Json.obj(
        "tweet_id"        -> tweet.tweet_id,
        "tweet_user_name" -> tweet.tweet_user_name,
        "tweet_text"      -> tweet.tweet_text
      )
    }
  }

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
      .sortBy(t => t._1._1.tweetId.desc).result).map { seq =>
        val json = seq.map { t =>
          TimelineForm(t._1._1.tweetId, t._2.userName, t._1._1.tweetText)
        }
      Ok(Json.obj("timeline" -> json))
    }
  }

  /**
    * 自ツイート表示
    */
  def mylist = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    db.run(Tweets.filter(t => t.userId === sessionUserId).sortBy(t => t.tweetId.desc).result).map { tweets =>
        Ok(Json.obj("tweets" -> tweets))
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    val sessionUserId = UserService.getJSSessionId(rs)
    val timestamp = new Timestamp(System.currentTimeMillis())
    rs.body.validate[TweetForm].map { form =>
      val tweet = TweetsRow(0, sessionUserId, form.tweet_text, timestamp)
      db.run(Tweets += tweet).map { _ =>
        Ok(Json.obj("result" -> "create_success"))
      }
    }.recoverTotal { e =>
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