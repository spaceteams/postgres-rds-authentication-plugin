package de.spaceteams.postgres.jdbc.rds.authentication

import RDSAuthenticationPlugin._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.rds.auth.{GetIamAuthTokenRequest, RdsIamAuthTokenGenerator}
import org.postgresql.PGProperty
import org.postgresql.plugin.{AuthenticationPlugin, AuthenticationRequestType}

import java.util.Properties

/**
 * The authentication plugin mechanism of the PostgreSQL JDBC driver creates
 * a new instance of the authentication plugin everytime a new connection is
 * opened. That is why it is necessary to use an companion object to preserve
 * state across the lifetime of the application.
 */
object RDSAuthenticationPlugin {

  /** Supplier to retrieve a new IAM token */
  var tokenSupplier: Option[() => String] = None

  /**
   * Expiry time of the cached IAM token in milliseconds.
   * Defaults to the RDS IAM token lifetime of 15 minutes
   */
  var tokenCacheExpiryInMs: Long = 15 * 60 * 1000

  /** Plugin name to be used for configuring the connection properties */
  val pluginName: String = this.getClass.getCanonicalName.replace("$", "")

  /** Key for region property */
  private val regionProperty = "region"

  /** Last retrieval of current token  */
  private var lastAuthentication: Option[Long] = None

  /** Current cached IAM token  */
  private var currentToken: Option[String] = None

  def initProperties(region: String): Properties = {
    val props = new Properties
    props.put(PGProperty.AUTHENTICATION_PLUGIN_CLASS_NAME.getName, pluginName)
    props.put(regionProperty, region)
    props
  }

}

class RDSAuthenticationPlugin(props: Properties) extends AuthenticationPlugin {
  private val region: String = loadParameterAsString(regionProperty)
  private val host: String = loadParameterAsString(PGProperty.PG_HOST.getName)
  private val port: Int = loadParameterAsInt(PGProperty.PG_PORT.getName)
  private val user: String = loadParameterAsString(PGProperty.USER.getName)

  override def getPassword(`type`: AuthenticationRequestType): Array[Char] = {
    val initialTokenRetrieval = lastAuthentication.isEmpty || currentToken.isEmpty
    val tokenUpdateNeeded = lastAuthentication
      .exists(ts => System.currentTimeMillis() - ts >= tokenCacheExpiryInMs)

    if (initialTokenRetrieval || tokenUpdateNeeded) {
      lastAuthentication = Some(System.currentTimeMillis())
      val token = tokenSupplier
        .getOrElse(defaultRDSTokenSupplier)
        .apply()
      currentToken = Some(token)
    }

    assert(currentToken.isDefined)
    currentToken.get.toCharArray
  }

  /**
   * Uses the default AWS credentials to retrieve an RDS IAM token
   * @return RDS IAM token to be used for database connections
   */
  private def defaultRDSTokenSupplier: () => String = () => {
    val generator: RdsIamAuthTokenGenerator = RdsIamAuthTokenGenerator
      .builder
      .credentials(new DefaultAWSCredentialsProviderChain)
      .region(region)
      .build

    try {
      generator.getAuthToken(
        GetIamAuthTokenRequest
          .builder
          .hostname(host)
          .port(port)
          .userName(user)
          .build
      )
    } catch {
      case e: Exception => throw RDSAuthenticationPluginException("Could not authenticate with AWS", e)
    }
  }

  private def loadParameterAsString(parameterName: String): String = {
    loadParameter(parameterName).asInstanceOf[String]
  }

  private def loadParameterAsInt(parameterName: String): Int = {
    Integer.parseInt(loadParameter(parameterName).asInstanceOf[String])
  }

  private def loadParameter(parameterName: String): AnyRef = {
    if (!props.containsKey(parameterName)) {
      throw RDSAuthenticationPluginException(s"Required parameter '$parameterName' not set.")
    }
    props.get(parameterName)
  }
}

trait RDSAuthenticationPluginException extends RuntimeException
object RDSAuthenticationPluginException {
  def apply(msg: String) = new RuntimeException(msg) with RDSAuthenticationPluginException
  def apply(msg: String, e: Exception) = new RuntimeException(msg, e) with RDSAuthenticationPluginException
}
