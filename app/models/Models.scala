package models

import play.api.libs.json._

case class Hoge(user_name: String, profile_text: Option[String], relation_id: Int)
case class FollowListForm(rows: Seq[Hoge])

case class Book(title: String, authors: Seq[Author])
case class Author(name: String)

object Formatters {
  implicit val HogeFormat = Json.format[Hoge]
  implicit val FollowListFormat = Json.format[FollowListForm]

  implicit val authorFormat = Json.format[Author]
  implicit val bookFormat = Json.format[Book]
}