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

/**
  * Created by ryoken.kojima on 2016/08/08.
  */
object JsonUserController {

  // フォームの値を格納するケースクラス
  case class UserForm(email: String, user_name: String, password: String, profile_text: Option[String])

  implicit val userFormFormat = Json.format[UserForm]

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
      "email"        -> email,
      "user_name"    -> nonEmptyText(maxLength = 20),
      "password"     -> nonEmptyText(maxLength = 20),
      "profile_text" -> optional(text(maxLength = 140))
    )(UserForm.apply)(UserForm.unapply)
  )
}

class JsonUserController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
  import JsonUserController._

  /**
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Users.result).map { users =>
      Ok(Json.obj("users" -> users))
    }
  }

  /**
    * 編集画面表示
    */
  def edit = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
      // IDからユーザ情報を1件取得
      db.run(Users.filter(t => t.userId === sessionUserId).result.head).map { user =>
        Ok(Json.obj("user" -> user))
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async(parse.json) { implicit rs =>
    rs.body.validate[UserForm].map { form =>
      // OKの場合はユーザを登録
      val user = UsersRow(0, form.user_name, form.password, form.profile_text, form.email)
      db.run(Users += user).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 更新実行
    */
  def update = Action.async(parse.json) { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    rs.body.validate[UserForm].map { form =>
      // OKの場合はユーザ情報を更新
      val user = UsersRow(sessionUserId, form.user_name, form.password, form.profile_text, form.email)
      db.run(Users.filter(t => t.userId === sessionUserId).update(user)).map { u =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" ->"failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * 削除実行
    */
  def remove(id: Int) = Action.async { implicit rs =>
    val sessionUserId = rs.session.get("user_id").get.toInt
    db.run(Users.filter(t => t.userId === sessionUserId).delete).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }

  /**
    * ログイン認証実行
    */
  def authenticate = Action.async(parse.json) { implicit rs =>
    rs.body.validate[UserForm].map { form => {
        db.run(Users.filter(t => t.userName === form.user_name && t.password === form.password).result.headOption).map {
          case Some(user) => Ok(Json.obj("result" -> "success")).withSession("user_id" -> user.userId.toString)
          case _ => BadRequest(Json.obj("result" -> "login_failure"))
        }
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future { BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e))) }
    }
  }

  /**
    * ログアウト実行
    */
  def logout = Action.async { implicit rs =>
    Future { Ok(Json.obj("result" -> "success")).withNewSession }
  }

}