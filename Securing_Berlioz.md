# Introduction #

While Berlioz is primarily designed to render pages with XSLT and does not require authentication, some Berlioz services may need to be secured.
For example, the following services should be protected appropriately:
> - services used for administration tasks such as indexing,
> - services that expose the configuration or the underlying file system.

# Details #

Berlioz does not specifically provide or support any built-in mechanism to secure pages, instead it relies on the Web security provided by the Web container.

## Authentication Realm ##

The first step is to declare a new Authentication Realm in your servlet container.

With Jetty, this is done in the jetty configuration (in `jetty/etc` folder) by adding the following:
```
  <!-- =============================================================== -->
  <!-- Configure the Authentication Realms                             -->
  <!-- =============================================================== -->
  <Call name="addRealm">
    <Arg>
      <New class="org.mortbay.http.HashUserRealm">
        <Arg>Berlioz Admin</Arg>
        <Arg><SystemProperty name="jetty.home" default="."/>/etc/berlioz-admin.properties</Arg>
      </New>
    </Arg>
  </Call>
```

The example above tells Jetty that the user passwords and roles are defined in the `berlioz-admin.properties` file for the **Berlioz Admin** realm.

Below is a sample properties file used by Jetty:
```
#
# This file defines users passwords and roles for a HashUserRealm
#
# The format is
#  <username>: <password>[,<rolename> ...]
#
hector: berlioz,admin
```


## Updating the Web descriptor ##

The second step is to update the Web descriptor (**/WEB-INF/web.xml**) to protect Web resources by adding a security constraint using the Authentication Realm.

This example will secure all the services in the Berlioz configuration that match the `/admin` URL pattern:
```
  <!-- Security Constraint -->
  <security-constraint>
    <web-resource-collection>
      <web-resource-name>Admin Tools</web-resource-name>
      <url-pattern>/html/admin/*</url-pattern>
      <url-pattern>/xml/admin/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
      <description>Berlioz Administration</description>
      <role-name>admin</role-name>
    </auth-constraint>
  </security-constraint>

  <!-- Login Configuration-->
  <login-config>
    <auth-method>BASIC</auth-method>
    <realm-name>Berlioz Admin</realm-name>
  </login-config>

  <!-- Security roles -->
  <security-role>
    <description>This role is used for user wanting to perform administration tasks on Berlioz.</description>
    <role-name>admin</role-name>
  </security-role>
```