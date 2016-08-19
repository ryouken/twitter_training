package example

object CodeGen extends App {

  val slickDriver = "slick.driver.MySQLDriver"
  val jdbcDriver = "com.mysql.jdbc.Driver"
  val url = "jdbc:mysql://localhost/twitter_comp"
  val outputDir = "app"
  val pkg = "models"
  val user = "root"
  val password = "Twitter0823"

  slick.codegen.SourceCodeGenerator.main(
    Array(slickDriver, jdbcDriver, url, outputDir, pkg, user, password))
}