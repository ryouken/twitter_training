package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject

import org.joda.time.DateTime

import scala.concurrent.Future
import slick.driver.H2Driver.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonController {
  // TweetsRowをJSONに変換するためのWritesを定義
  implicit val tweetsRowWritesFormat = new Writes[TweetsRow]{
    import JsonController._

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

  /**
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Tweets.sortBy(t => t.tweetId).result).map { tweets =>
      // ユーザの一覧をJSONで返す
      Ok(Json.obj("tweets" -> tweets))
    }
  }

  /**
    * ユーザ登録
    */
  def create = TODO

  /**
    * ユーザ更新
    */
  def update = TODO

  /**
    * ユーザ削除
    */
  def remove(id: Long) = TODO
}