package services

import play.api.libs.json.JsValue
import play.api.mvc._

/**
  * Created by ryouken on 2016/08/16.
  */
object UserService {

  def getSessionId(rs: Request[AnyContent]) = { rs.session.get("user_id").get.toInt }
  def getJSSessionId(rs: Request[JsValue]) = { rs.session.get("user_id").get.toInt }

}
