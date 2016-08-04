package controllers

import java.sql.Timestamp
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonController {
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
}

class JsonController @Inject()(val dbConfigProvider: DatabaseConfigProvider) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] {
  import JsonController._

  /**
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Tweets.result).map { tweets =>
      // ユーザの一覧をJSONで返す
      Ok(Json.obj("tweets" -> tweets))
    }
  }

  /**
    * ユーザ登録
    */
  def create = Action.async(parse.json) { implicit rs =>
    val timestamp = new Timestamp(System.currentTimeMillis())
    rs.body.validate[TweetForm].map { form =>
      // OKの場合はユーザを登録
      val tweet = TweetsRow(0, 1, form.text, timestamp)
      db.run(Tweets += tweet).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future {
        BadRequest(Json.obj("result" ->"failure", "error" -> JsError.toJson(e)))
      }
    }
  }

  /**
    * ユーザ更新
    */
  def update = Action.async(parse.json) { implicit rs =>
    val timestamp = new Timestamp(System.currentTimeMillis())
    rs.body.validate[TweetForm].map { form =>
      // OKの場合はユーザを登録
      val tweet = TweetsRow(0, 0, form.text, timestamp)
      db.run(Tweets += tweet).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future {
        BadRequest(Json.obj("result" ->"failure", "error" -> JsError.toJson(e)))
      }
    }
  }

  /**
    * ユーザ削除
    */
  def remove(id: Int) = Action.async { implicit rs =>
    // ユーザを削除
    db.run(Tweets.filter(t => t.tweetId === id.bind).delete).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }
}