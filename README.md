# PostgreSQL JDBC RDS Authentication Plugin

[![Developed during Spacetime at Spaceteams](https://raw.githubusercontent.com/spaceteams/badges/main/developed-during-spacetime.svg)](https://spaceteams.de)

Authentication plugin for PostgreSQL JDBC driver

The plugin uses the AWS RDS SDK to retrieve an IAM token which
can be used to authenticate against an AWS RDS PostgreSQL 
instance (when enabled).

IAM RDS tokens are short-lived tokens, which are only valid for 15 minutes.
To prevent authentication errors when creating new connections, 
the plugin will cache IAM tokens for that period of time and create a new
token when necessary.

## Prerequisites:
* This only works with combination of the PostgreSQL JDBC driver version 42.3.2+,
  where the AuthenticationPlugin mechanism is available.
* IAM access needs to be enabled on your RDS PostgreSQL database
* The classpath of your application needs to contain
  * The RDSAuthenticationPlugin.scala class
  * PostgreSQL JDBC driver 42.3.2+ (see [build.sbt](build.sbt))
  * AWS Java RDS SDK (see [build.sbt](build.sbt))
* AWS credentials are retrieved using the 
  [DefaultAWSCredentialProviderChain](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)

## Usage:

Note: Currently, one has to specify the region of the RDS instance, though it
could be extracted from the DNS name.

```scala
import de.spaceteams.postgres.jdbc.rds.authentication.RDSAuthenticationPlugin
import java.sql.DriverManager
import java.util.Properties
import org.postgresql.PGProperty

val props = RDSAuthenticationPlugin.initProperties(region = "eu-central-1")
// set other connection properties ...
props.put(PGProperty.USER.getName, "dbUser")

val connection = DriverManager.getConnection("jdbc:postgresql://my-rds-db.cluster-abcd1234.eu-central-1.rds.amazonaws.com/", props)

// ...
```
