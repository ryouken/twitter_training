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
  lazy val schema: profile.SchemaDescription = Relations.schema ++ Tweets.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Relations
   *  @param relationId Database column relation_id SqlType(INT), AutoInc, PrimaryKey
   *  @param followUserId Database column follow_user_id SqlType(INT)
   *  @param followedUserId Database column followed_user_id SqlType(INT) */
  case class RelationsRow(relationId: Int, followUserId: Int, followedUserId: Int)
  /** GetResult implicit for fetching RelationsRow objects using plain SQL queries */
  implicit def GetResultRelationsRow(implicit e0: GR[Int]): GR[RelationsRow] = GR{
    prs => import prs._
    RelationsRow.tupled((<<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table relations. Objects of this class serve as prototypes for rows in queries. */
  class Relations(_tableTag: Tag) extends Table[RelationsRow](_tableTag, "relations") {
    def * = (relationId, followUserId, followedUserId) <> (RelationsRow.tupled, RelationsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(relationId), Rep.Some(followUserId), Rep.Some(followedUserId)).shaped.<>({r=>import r._; _1.map(_=> RelationsRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column relation_id SqlType(INT), AutoInc, PrimaryKey */
    val relationId: Rep[Int] = column[Int]("relation_id", O.AutoInc, O.PrimaryKey)
    /** Database column follow_user_id SqlType(INT) */
    val followUserId: Rep[Int] = column[Int]("follow_user_id")
    /** Database column followed_user_id SqlType(INT) */
    val followedUserId: Rep[Int] = column[Int]("followed_user_id")

    /** Foreign key referencing Users (database name relations_ibfk_1) */
    lazy val usersFk1 = foreignKey("relations_ibfk_1", followUserId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing Users (database name relations_ibfk_2) */
    lazy val usersFk2 = foreignKey("relations_ibfk_2", followedUserId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    /** Uniqueness Index over (followUserId,followedUserId) (database name follow_user_id) */
    val index1 = index("follow_user_id", (followUserId, followedUserId), unique=true)
  }
  /** Collection-like TableQuery object for table Relations */
  lazy val Relations = new TableQuery(tag => new Relations(tag))

  /** Entity class storing rows of table Tweets
   *  @param tweetId Database column tweet_id SqlType(INT), AutoInc, PrimaryKey
   *  @param userId Database column user_id SqlType(INT)
   *  @param tweetText Database column tweet_text SqlType(VARCHAR), Length(300,true)
   *  @param createdAt Database column created_at SqlType(DATETIME) */
  case class TweetsRow(tweetId: Int, userId: Int, tweetText: String, createdAt: java.sql.Timestamp)
  /** GetResult implicit for fetching TweetsRow objects using plain SQL queries */
  implicit def GetResultTweetsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[TweetsRow] = GR{
    prs => import prs._
    TweetsRow.tupled((<<[Int], <<[Int], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table tweets. Objects of this class serve as prototypes for rows in queries. */
  class Tweets(_tableTag: Tag) extends Table[TweetsRow](_tableTag, "tweets") {
    def * = (tweetId, userId, tweetText, createdAt) <> (TweetsRow.tupled, TweetsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(tweetId), Rep.Some(userId), Rep.Some(tweetText), Rep.Some(createdAt)).shaped.<>({r=>import r._; _1.map(_=> TweetsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column tweet_id SqlType(INT), AutoInc, PrimaryKey */
    val tweetId: Rep[Int] = column[Int]("tweet_id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(INT) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column tweet_text SqlType(VARCHAR), Length(300,true) */
    val tweetText: Rep[String] = column[String]("tweet_text", O.Length(300,varying=true))
    /** Database column created_at SqlType(DATETIME) */
    val createdAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")

    /** Foreign key referencing Users (database name tweets_ibfk_1) */
    lazy val usersFk = foreignKey("tweets_ibfk_1", userId, Users)(r => r.userId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Tweets */
  lazy val Tweets = new TableQuery(tag => new Tweets(tag))

  /** Entity class storing rows of table Users
   *  @param userId Database column user_id SqlType(INT), AutoInc, PrimaryKey
   *  @param email Database column email SqlType(VARCHAR), Length(255,true)
   *  @param userName Database column user_name SqlType(VARCHAR), Length(255,true)
   *  @param password Database column password SqlType(VARCHAR), Length(300,true)
   *  @param profileText Database column profile_text SqlType(VARCHAR), Length(300,true), Default(None) */
  case class UsersRow(userId: Int, email: String, userName: String, password: String, profileText: Option[String] = None)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends Table[UsersRow](_tableTag, "users") {
    def * = (userId, email, userName, password, profileText) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(userId), Rep.Some(email), Rep.Some(userName), Rep.Some(password), profileText).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column user_id SqlType(INT), AutoInc, PrimaryKey */
    val userId: Rep[Int] = column[Int]("user_id", O.AutoInc, O.PrimaryKey)
    /** Database column email SqlType(VARCHAR), Length(255,true) */
    val email: Rep[String] = column[String]("email", O.Length(255,varying=true))
    /** Database column user_name SqlType(VARCHAR), Length(255,true) */
    val userName: Rep[String] = column[String]("user_name", O.Length(255,varying=true))
    /** Database column password SqlType(VARCHAR), Length(300,true) */
    val password: Rep[String] = column[String]("password", O.Length(300,varying=true))
    /** Database column profile_text SqlType(VARCHAR), Length(300,true), Default(None) */
    val profileText: Rep[Option[String]] = column[Option[String]]("profile_text", O.Length(300,varying=true), O.Default(None))

    /** Uniqueness Index over (email) (database name email) */
    val index1 = index("email", email, unique=true)
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
