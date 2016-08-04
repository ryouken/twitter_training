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
  case class UserForm(user_id: Option[Int], user_name: String, password: String)

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val userForm = Form(
    mapping(
      "user_id" -> optional(number),
      "user_name" -> nonEmptyText(maxLength = 20),
      "password" -> nonEmptyText(maxLength = 20)
    )(UserForm.apply)(UserForm.unapply)
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
    def edit(id: Option[Int]) = Action.async { implicit rs =>
      // リクエストパラメータにIDが存在する場合
      val form = if (id.isDefined) {
        // IDからユーザ情報を1件取得
        db.run(Users.filter(t => t.userId === id.get.bind).result.head).map { user =>
          // 値をフォームに詰める
          userForm.fill(UserForm(Some(user.userId), user.userName, user.password))
        }
      } else {
        // リクエストパラメータにIDが存在しない場合
        Future { userForm }
      }

      form.flatMap { form =>
        // 会社一覧を取得
        db.run(Users.sortBy(_.userId).result).map { users =>
          Ok(views.html.user.edit(form))
        }
      }
    }

    /**
      * 登録実行
      */
    def create = Action.async { implicit rs =>
      Logger.debug("---create---")
      // リクエストの内容をバインド
      userForm.bindFromRequest.fold(
        // エラーの場合
        error => {
          db.run(Users.sortBy(t => t.userId).result).map { users =>
            Logger.debug("create_error", error = new Throwable)
            BadRequest(views.html.user.edit(error))
          }
        },
        // OKの場合
        form => {
          // ユーザを登録
          val user = UsersRow(0, form.user_name, form.password)
          db.run(Users += user).flatMap { _ =>
            db.run(Users.result).map { u =>
            // 一覧画面へリダイレクト
            Redirect(routes.TweetController.mylist(user.userId))
            }
          }
        }
      )
    }

    /**
      * 更新実行
      */
    def update = Action.async { implicit rs =>
      Logger.debug("---update---")
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
          val user = UsersRow(form.user_id.get, form.user_name, form.password)
          db.run(Users.filter(t => t.userId === user.userId.bind).update(user)).map { u =>
            // 一覧画面にリダイレクト
            Redirect(routes.TweetController.mylist(user.userId))
          }
        }
      )
    }

    /**
      * 削除実行
      */
    def remove(id: Int) = Action.async { implicit rs =>
      // ユーザを削除
      db.run(Users.filter(t => t.userId === id.bind).delete).map { _ =>
        // 一覧画面へリダイレクト
        Redirect(routes.UserController.list)
      }
    }

    def login = Action.async { implicit rs =>
      val form = Future { userForm }
      form.map { form =>
        Ok(views.html.user.login(form))
      }
    }

    def authenticate = Action.async { implicit request =>
      userForm.bindFromRequest.fold(
        formWithErrors => Future { BadRequest(views.html.user.login(formWithErrors)) },
        form => {
          db.run(Users.filter(t => t.userName === form.user_name && t.password === form.password).result.headOption).map {
            case Some(user) => Redirect(routes.TweetController.mylist(user.userId))
            case _       => BadRequest(views.html.user.login(userForm))
          }
        }
      )
    }

  }