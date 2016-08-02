package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.driver.MySQLDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Relationships.schema ++ Tweets.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Relationships
   *  @param relationId Database column relation_id SqlType(INT), PrimaryKey
   *  @param followUserId Database column follow_user_id SqlType(INT)
   *  @param followedUserId Database column followed_user_id SqlType(INT) */
  case class RelationshipsRow(relationId: Int, followUserId: Int, followedUserId: Int)
  /** GetResult implicit for fetching RelationshipsRow objects using plain SQL queries */
  implicit def GetResultRelationshipsRow(implicit e0: GR[Int]): GR[RelationshipsRow] = GR{
    prs => import prs._
    RelationshipsRow.tupled((<<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table relationships. Objects of this class serve as prototypes for rows in queries. */
  class Relationships(_tableTag: Tag) extends Table[RelationshipsRow](_tableTag, "relationships") {
    def * = (relationId, followUserId, followedUserId) <> (RelationshipsRow.tupled, RelationshipsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(relationId), Rep.Some(followUserId), Rep.Some(followedUserId)).shaped.<>({r=>import r._; _1.map(_=> RelationshipsRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column relation_id SqlType(INT), PrimaryKey */
    val relationId: Rep[Int] = column[Int]("relation_id", O.PrimaryKey)
    /** Database column follow_user_id SqlType(INT) */
    val followUserId: Rep[Int] = column[Int]("follow_user_id")
    /** Database column followed_user_id SqlType(INT) */
    val followedUserId: Rep[Int] = column[Int]("followed_user_id")

    /** Foreign key referencing Users (database name relationships_ibfk_1) */
    lazy val usersFk1 = foreignKey("relationships_ibfk_1", followUserId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing Users (database name relationships_ibfk_2) */
    lazy val usersFk2 = foreignKey("relationships_ibfk_2", followedUserId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Relationships */
  lazy val Relationships = new TableQuery(tag => new Relationships(tag))

  /** Entity class storing rows of table Tweets
   *  @param tweetId Database column tweet_id SqlType(INT), PrimaryKey
   *  @param userId Database column user_id SqlType(INT)
   *  @param tweetText Database column tweet_text SqlType(VARCHAR), Length(300,true)
   *  @param timestamp Database column timestamp SqlType(DATETIME) */
  case class TweetsRow(tweetId: Int, userId: Int, tweetText: String, timestamp: java.sql.Timestamp)
  /** GetResult implicit for fetching TweetsRow objects using plain SQL queries */
  implicit def GetResultTweetsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[TweetsRow] = GR{
    prs => import prs._
    TweetsRow.tupled((<<[Int], <<[Int], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table tweets. Objects of this class serve as prototypes for rows in queries. */
  class Tweets(_tableTag: Tag) extends Table[TweetsRow](_tableTag, "tweets") {
    def * = (tweetId, userId, tweetText, timestamp) <> (TweetsRow.tupled, TweetsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(tweetId), Rep.Some(userId), Rep.Some(tweetText), Rep.Some(timestamp)).shaped.<>({r=>import r._; _1.map(_=> TweetsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column tweet_id SqlType(INT), PrimaryKey */
    val tweetId: Rep[Int] = column[Int]("tweet_id", O.PrimaryKey)
    /** Database column user_id SqlType(INT) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column tweet_text SqlType(VARCHAR), Length(300,true) */
    val tweetText: Rep[String] = column[String]("tweet_text", O.Length(300,varying=true))
    /** Database column timestamp SqlType(DATETIME) */
    val timestamp: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("timestamp")

    /** Foreign key referencing Users (database name tweets_ibfk_1) */
    lazy val usersFk = foreignKey("tweets_ibfk_1", userId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Tweets */
  lazy val Tweets = new TableQuery(tag => new Tweets(tag))

  /** Entity class storing rows of table Users
   *  @param userId Database column user_id SqlType(INT), PrimaryKey
   *  @param userName Database column user_name SqlType(VARCHAR), Length(255,true)
   *  @param password Database column password SqlType(CHAR), Length(30,false)
   *  @param profileText Database column profile_text SqlType(VARCHAR), Length(300,true), Default(None) */
  case class UsersRow(userId: Int, userName: String, password: String, profileText: Option[String] = None)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends Table[UsersRow](_tableTag, "users") {
    def * = (userId, userName, password, profileText) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(userId), Rep.Some(userName), Rep.Some(password), profileText).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column user_id SqlType(INT), PrimaryKey */
    val userId: Rep[Int] = column[Int]("user_id", O.PrimaryKey)
    /** Database column user_name SqlType(VARCHAR), Length(255,true) */
    val userName: Rep[String] = column[String]("user_name", O.Length(255,varying=true))
    /** Database column password SqlType(CHAR), Length(30,false) */
    val password: Rep[String] = column[String]("password", O.Length(30,varying=false))
    /** Database column profile_text SqlType(VARCHAR), Length(300,true), Default(None) */
    val profileText: Rep[Option[String]] = column[Option[String]]("profile_text", O.Length(300,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
