package de.spaceteams.postgres.jdbc.rds.authentication

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.postgresql.PGProperty
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.sql.DriverManager
import java.util.Properties
import scala.language.postfixOps
import scala.util.Random

class RDSAuthenticationPluginTest extends AnyFlatSpec {

  private val testString = "test"

  "RDSAuthenticationPlugin" should "throw exception if region is undefined" in {
    // given
    val props = new Properties
    // when
    val thrown = the [Exception] thrownBy new RDSAuthenticationPlugin(props)
    // then
    thrown shouldBe a[RDSAuthenticationPluginException]
  }

  "RDSAuthenticationPlugin" should "should set initial properties" in {
    // given
    var props = new Properties
    // when
    props = RDSAuthenticationPlugin.initProperties(testString)
    // then
    props.get("region") shouldBe testString
    props.get(PGProperty.AUTHENTICATION_PLUGIN_CLASS_NAME.getName) shouldBe RDSAuthenticationPlugin.pluginName
  }

  "RDSAuthenticationPlugin" should "return a token to the caller" in {
    // given
    val tokenStub = createRandomString()
    val plugin = createRDSAuthenticationPlugin(tokenSupplier = () => tokenStub)
    // when
    val token = plugin.getPassword(null)
    // then
    token.mkString shouldBe tokenStub
  }

  "RDSAuthenticationPlugin" should "return the same cached token" in {
    // given
    val plugin = createRDSAuthenticationPlugin()
    // when
    val firstToken = plugin.getPassword(null)
    val secondToken = plugin.getPassword(null)
    // then
    secondToken.mkString shouldBe firstToken.mkString
  }

  "RDSAuthenticationPlugin" should "return a different token when cache expired" in {
    // given
    val cacheExpiryInMillis = 100
    val plugin = createRDSAuthenticationPlugin(expiry = cacheExpiryInMillis)
    // when
    val firstToken = plugin.getPassword(null)
    Thread.sleep(cacheExpiryInMillis + 10)
    val secondToken = plugin.getPassword(null)
    // then
    secondToken.mkString should not be firstToken.mkString
  }

  "RDSAuthenticationPlugin" should "should be used by PostgreSQL JDBC driver" in {
    // given
    RDSAuthenticationPlugin.tokenSupplier = Some(() => testString)
    val container = PostgreSQLContainer(
      databaseName = testString,
      username = testString,
      password = testString,
      mountPostgresDataToTmpfs = true
    )
    container.start()
    val props = createTestProperties()
    // when
    val connection = DriverManager.getConnection(container.jdbcUrl, props)
    val stmt = connection.createStatement()
    val result = stmt.executeQuery(s"select '$testString'")
    result.next()
    val dbResult = result.getString(1)
    // then
    dbResult shouldBe testString
    // cleanup resources
    stmt.close()
    connection.close()
    container.stop()
  }

  private def createRDSAuthenticationPlugin(
    props: Properties = createTestProperties(),
    tokenSupplier: () => String = () => createRandomString(),
    expiry: Long = 1000
  ): RDSAuthenticationPlugin = {
    RDSAuthenticationPlugin.tokenCacheExpiryInMs = expiry
    RDSAuthenticationPlugin.tokenSupplier = Some(tokenSupplier)
    new RDSAuthenticationPlugin(props)
  }

  private def createTestProperties(): Properties = {
    val props = RDSAuthenticationPlugin.initProperties(region = testString)
    props.put(PGProperty.PG_HOST.getName, testString)
    props.put(PGProperty.PG_PORT.getName, "5432")
    props.put(PGProperty.USER.getName, testString)
    props
  }

  private def createRandomString(): String = {
    Random.alphanumeric take 8 mkString
  }

}
