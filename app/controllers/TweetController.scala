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
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Tweets.result).flatMap { tweets =>
      db.run(Users.result).map { users =>
      // 一覧画面を表示
      Ok(views.html.tweet.list(tweets, users))
      }
    }
  }

  /**
    * 編集画面表示
    */

  def edit(id: Option[Int]) = Action.async { implicit rs =>
    // リクエストパラメータにIDが存在する場合
    val form = if(id.isDefined) {
      // IDからユーザ情報を1件取得
      db.run(Tweets.filter(t => t.tweetId === id.get.bind).result.head).map { tweet =>
        // 値をフォームに詰める
        tweetForm.fill(TweetForm(tweet.tweetText))
      }
    } else {
      // リクエストパラメータにIDが存在しない場合
      Future { tweetForm }
    }

    form.flatMap { form =>
      // 会社一覧を取得
      db.run(Users.sortBy(_.userName).result).map { companies =>
        Ok(views.html.tweet.edit(form, companies))
      }
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async { implicit rs =>
    Logger.debug("create")
  val timestamp = new Timestamp(System.currentTimeMillis())
    // リクエストの内容をバインド
    tweetForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        db.run(Tweets.sortBy(t => t.tweetId).result).map { users =>
          Logger.debug("create_error", error = new Throwable)
          Redirect(routes.TweetController.list)
        }
      },
      // OKの場合
      form  => {
          Logger.debug("create_ok")
        // ツイートを登録
        val tweet = TweetsRow(0, 1, form.text, timestamp)
        db.run(Tweets += tweet).map { _ =>
          // 一覧画面へリダイレクト
          Redirect(routes.TweetController.list)
        }
      }
    )
  }

  /**
    * 更新実行
    */
  def update = Action.async { implicit rs =>

    Logger.debug("update")
    val timestamp = new Timestamp(System.currentTimeMillis())
    // リクエストの内容をバインド
    tweetForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        db.run(Users.sortBy(t => t.userId).result).map { users =>
          BadRequest(views.html.tweet.edit(error, users))
        }
      },
      // OKの場合
      form  => {
        // ユーザを登録
        val tweet = TweetsRow(0, 0 , form.text, timestamp)
        db.run(Tweets += tweet).map { _ =>
          // 一覧画面へリダイレクト
          Redirect(routes.HomeController.index)
        }
      }
    )
  }

  /**
    * 削除実行
    */
  def remove(id: Int) = Action.async { implicit rs =>
    // ユーザを削除
    db.run(Tweets.filter(t => t.tweetId === id.bind).delete).map { _ =>
      // 一覧画面へリダイレクト
      Redirect(routes.TweetController.list)
    }
  }

}