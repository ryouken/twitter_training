package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import scala.concurrent.Future

class UserController @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi) extends Controller
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

//  /**
//    * 一覧表示
//    */
//  def list = TODO
//
//  /**
//    * 編集画面表示
//    */
//  def edit(id: Option[Long]) = TODO
//
//  /**
//    * 登録実行
//    */
//  def create = TODO
//
//  /**
//    * 更新実行
//    */
//  def update = TODO
//
//  /**
//    * 削除実行
//    */
//  def remove(id: Long) = TODO

}