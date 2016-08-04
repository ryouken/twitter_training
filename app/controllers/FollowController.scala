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

/**
  * Created by ryoken.kojima on 2016/08/04.
  */

class FollowController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Relationships.result).flatMap { relationships =>
      db.run(Users.result).map { users =>
        // 一覧画面を表示
        Ok(views.html.follow.list(relationships, users))
      }
    }
  }

  def create = TODO

  def remove = TODO

}