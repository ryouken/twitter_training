package controllers

import play.api.Logger
import play.api.db.slick._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.Tables._
import javax.inject.Inject
import scala.concurrent.Future
import services.UserService
import play.api.libs.Crypto

/**
  * Created by ryoken.kojima on 2016/08/08.
  */
object JsonUserController {

  // TODO フォーム
  // フォームの値を格納するケースクラス
  case class UserForm(user_id: Int, email: String, user_name: String, password: String, profile_text: Option[String])
  case class LoginForm(email: String, password: String)

  implicit val userFormFormat  = Json.format[UserForm]
  implicit val loginFormFormat = Json.format[LoginForm]

  // UsersRowをJSONに変換するためのWritesを定義
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

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val userForm = Form(
    mapping(
      "user_id"      -> number,
      "email"        -> email,
      "user_name"    -> nonEmptyText(maxLength = 20),
      "password"     -> nonEmptyText(minLength = 8, maxLength = 20),
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

class JsonUserController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                   val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonUserController._

  /**
    * 一覧表示
    */
  def list = Action.async { implicit  rs =>
    val sessionUserId = UserService.getSessionId(rs)
    val q = Relations.filter(_.followUserId === sessionUserId).map(_.followedUserId)
    val query = for {
      u <- Users.filterNot(u => u.userId in q)
    } yield u
    db.run(query.filterNot(u => u.userId === sessionUserId).sortBy(u => u.userId.desc)result).map { u =>
      Ok(Json.obj("users" -> u))
    }
  }

  /**
    * 編集画面表示
    */
  def edit = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    // IDからユーザ情報を1件取得
    db.run(Users.filter(t => t.userId === sessionUserId).result.head).map { user =>
      Ok(Json.obj("user" -> user))
    }
  }

  /**
    * 登録実行
    */
  // TODO Crypt使わない
  // TODO service層との切り分け
  def create = Action.async(parse.json) { implicit rs =>
    rs.body.validate[UserForm].map { form =>
      val hashedPW = Crypto.sign(form.password)
      val user = UsersRow(0, form.email, form.user_name, hashedPW, form.profile_text)
      db.run(Users += user).map { _ =>
        Ok(Json.obj("result" -> "create_success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" -> "create_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 更新実行
    */
  // TODO Crypt使わない
  // TODO service層との切り分け
  def update = Action.async(parse.json) { implicit rs =>
    val sessionUserId = UserService.getJSSessionId(rs)
    rs.body.validate[UserForm].map { form =>
      val hashedPW = Crypto.sign(form.password)
      // OKの場合はユーザ情報を更新
      val user = UsersRow(sessionUserId, form.email, form.user_name, hashedPW, form.profile_text)
      db.run(Users.filter(t => t.userId === sessionUserId).update(user)).map { u =>
        Ok(Json.obj("result" -> "update_success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" ->"update_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 削除実行
    */
  def delete = Action.async { implicit rs =>
    val sessionUserId = UserService.getSessionId(rs)
    db.run(Users.filter(t => t.userId === sessionUserId).delete).map { _ =>
      Ok(Json.obj("result" -> "delete_success"))
    }
  }

  /**
    * ログイン認証実行
    */
  def authenticate = Action.async(parse.json) { implicit rs =>
    rs.body.validate[LoginForm].map { form => {
      val hashedPW = Crypto.sign(form.password)
      db.run(Users.filter(t => t.email === form.email && t.password === hashedPW).result.headOption).map {
        case Some(user) => Ok(Json.obj("result" -> "login_success")).withSession("user_id" -> user.userId.toString)
        case _          => BadRequest(Json.obj("result" -> "login_failure"))
      }
    }}.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" -> "login_failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * ログアウト実行
    */
  def logout = Action.async { implicit rs =>
    Future { Ok(Json.obj("result" -> "logout_success")).withNewSession }
  }

}