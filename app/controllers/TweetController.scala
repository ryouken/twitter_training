package controllers

import java.sql.Timestamp
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import play.api.Logger
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._

object TweetController {
  // フォームの値を格納するケースクラス
  case class TweetForm(text: String)

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val tweetForm = Form(
    mapping(
      "text" -> nonEmptyText(maxLength = 140)
    )(TweetForm.apply)(TweetForm.unapply)
  )
}

class TweetController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import TweetController._

  /**
    * タイムライン表示
    */
  def timeline = TODO
//  def timeline = Action.async { implicit rs =>
//    val sessionUserId = rs.session.get("user_id").get.toInt
//    val query = for {
//      r <- Relations if r.followUserId === sessionUserId
//      u <- Users if u.userId === r.followedUserId
//      t <- Tweets if t.userId === r.followedUserId
//    } yield (u.userName, t.tweetText, t.timestamp)
//    val run: Future[Seq[(String, String, Timestamp)]] = db.run(query.result)
//    run.map { seq =>
//      val map: Seq[(String, Timestamp)] = seq.map { s =>
//        "user_name" -> s._1
//        "tweet_text" -> s._2
//        "timestamp" -> s._3
//      }
//      Ok(views.html.tweet.timeline(map))
//    }
//  }

  /**
    * 自ツイート表示
    */
  def mylist = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    db.run(Tweets.filter(t => t.userId === sessionUserId).result).flatMap { tweets =>
        Future { Ok(views.html.tweet.mylist(tweets)) }
    }
  }

  /**
    * 編集画面表示
    */
  def edit = Action.async { implicit rs =>
    val form = Future { tweetForm }
    form.flatMap { form =>
        Future { Ok(views.html.tweet.edit(form)) }
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val timestamp = new Timestamp(System.currentTimeMillis())
    // リクエストの内容をバインド
    tweetForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        db.run(Tweets.result).map { users =>
          Logger.debug("create_error", error = new Throwable)
          Redirect(routes.TweetController.mylist)
        }
      },
      // OKの場合
      form  => {
        // ツイートを登録
        val tweet = TweetsRow(0, sessionUserId, form.text, timestamp)
        db.run(Tweets += tweet).map { _ =>
          // 一覧画面へリダイレクト
          Redirect(routes.TweetController.mylist)
        }
      }
    )
  }

  /**
    * 削除実行
    */
  def remove(id: Int) = Action.async { implicit rs =>
    db.run(Tweets.filter(t => t.tweetId === id.bind).delete).map { _ =>
      // タイムラインへリダイレクト
      Redirect(routes.TweetController.timeline)
    }
  }

}