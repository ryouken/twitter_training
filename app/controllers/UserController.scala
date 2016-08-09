package controllers

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

object UserController {

  // フォームの値を格納するケースクラス
  case class UserForm(email: String, user_name: String, password: String, profile_text: Option[String])
  case class LoginForm(email: String, password: String)

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val userForm = Form(
    mapping(
      "email"        -> email,
      "user_name"    -> nonEmptyText(maxLength = 20),
      "password"     -> nonEmptyText(maxLength = 20),
      "profile_text" -> optional(text(maxLength = 140))
    )(UserForm.apply)(UserForm.unapply)
  )
  val loginForm = Form(
    mapping(
      "email"        -> email,
      "password"     -> nonEmptyText(minLength = 8, maxLength = 20)
    )(LoginForm.apply)(LoginForm.unapply)
  )
}

class UserController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

  import UserController._

  /**
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Users.result).map { users =>
      // 一覧画面を表示
      Ok(views.html.user.list(users))
    }
  }

  /**
    * 編集画面表示
    */
  def edit = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    val form = {
      // IDからユーザ情報を1件取得
      db.run(Users.filter(t => t.userId === sessionUserId).result.head).map { user =>
        // 値をフォームに詰める
        userForm.fill(UserForm(user.email, user.userName, user.password, user.profileText))
      }
    }
    form.flatMap { form =>
      db.run(Users.sortBy(_.userId).result).map { users =>
        Ok(views.html.user.edit(form))
      }
    }
  }

  /**
    * 新規会員登録画面表示
    */
  def register = Action.async { implicit rs =>
    val form = Future { userForm }
    form.map { form =>
      Ok(views.html.user.register(form))
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async { implicit rs =>
    // リクエストの内容をバインド
    userForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        db.run(Users.result).map { users =>
          Logger.debug("create_error", error = new Throwable)
          BadRequest(views.html.user.edit(error))
        }
      },
      // OKの場合
      form => {
        // ユーザを登録
        val user = UsersRow(0, form.user_name, form.password, form.profile_text, form.email)
        db.run(Users += user).map { _ =>
          // ログイン画面へリダイレクト
          Redirect(routes.UserController.login)
        }
      }
    )
  }

  /**
    * 更新実行
    */
  def update = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    // リクエストの内容をバインド
    userForm.bindFromRequest.fold(
      // エラーの場合は登録画面に戻す
      error => {
        db.run(Users.sortBy(t => t.userId).result).map { users =>
          BadRequest(views.html.user.edit(error))
        }
      },
      // OKの場合は登録を行い一覧画面にリダイレクトする
      form => {
        // ユーザ情報を更新
        val user = UsersRow(sessionUserId, form.user_name, form.password, form.profile_text, form.email)
        db.run(Users.filter(t => t.userId === sessionUserId).update(user)).map { u =>
          // 一覧画面にリダイレクト
          Redirect(routes.TweetController.mylist)
        }
      }
    )
  }

  /**
    * 削除実行
    */
  def delete(id: Int) = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    // ユーザを削除
    db.run(Users.filter(t => t.userId === sessionUserId).delete).map { _ =>
      // 一覧画面へリダイレクト
      Redirect(routes.UserController.list)
    }
  }

  /**
    * ログイン画面表示
    */
  def login = Action.async { implicit rs =>
    val form = Future { loginForm }
    form.map { form =>
      Logger.debug("reach login")
      Ok(views.html.user.login(form))
    }
  }

  /**
    * ログイン認証実行
    */
  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Future { BadRequest(views.html.user.login(formWithErrors)) } },
      form => {
        db.run(Users.filter(t => t.email === form.email && t.password === form.password).result.headOption).map {
          case Some(user) => Redirect(routes.UserController.list).withSession("user_id" -> user.userId.toString)
          case _          => BadRequest(views.html.user.login(loginForm))
        }
      }
    )
  }

  /**
    * ログアウト実行
    */
  def logout = Action.async { implicit rs =>
    Future { Redirect(routes.UserController.list).withNewSession }
  }

  def remove = TODO

}