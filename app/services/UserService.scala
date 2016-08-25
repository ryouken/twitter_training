package services

import models.Tables._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.db.slick._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.Tables._
import javax.inject.Inject
import scala.concurrent.Future

/**
  * Created by ryouken on 2016/08/16.
  */
object UserService {
  // TODO getしない
  def getSessionId(rs: Request[AnyContent]) = { rs.session.get("user_id").get.toInt }
  def getJSSessionId(rs: Request[JsValue]) = { rs.session.get("user_id").get.toInt }
}
//
//class UserService @Inject()(dbConfigProvider: DatabaseConfigProvider,
//                                   messagesApi: MessagesApi) extends Controller
//  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {
//
//
//  def getUnfollowedList(sessionUserId: Int) = {
//    val followedUsers = Relations.filter(_.followUserId === sessionUserId).map(_.followedUserId)
//    val unfollowedUsers = for {
//      u <- Users.filterNot(u => u.userId in followedUsers)
//    } yield u
//    db.run(unfollowedUsers.filterNot(u => u.userId === sessionUserId).sortBy(u => u.userId.desc).result)
//  }
//}
