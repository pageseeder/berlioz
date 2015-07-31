# Introduction #

There are several different types of configuration for Berlioz:
  * Web Descriptor
  * Berlioz Services
  * Berlioz Properties
  * Berlioz Logging
  * Java Environment variables

# Web descriptor #

The Web descriptor (`WEB-INF/web.xml`) is used by the Web container to declare servlets and map them to URLs.

## Global Servlet ##

Initiating Berlioz is a two stage process.

The first step is to declare the Berlioz servlet:
```
  <servlet>
    <servlet-name>Berlioz</servlet-name>
    <servlet-class>org.weborganic.berlioz.servlet.GlobalServlet</servlet-class>
  </servlet>
```

This servlet can take a number of initialisation parameters, see [Java API: Berlioz Servlet](http://weborganic.org/apidoc/berlioz/core/org/weborganic/berlioz/servlet/GlobalServlet.html).

The second step is to map the servlet to a URL:
```
  <servlet-mapping>
    <servlet-name>Berlioz</servlet-name>
    <url-pattern>/html/*</url-pattern>
  </servlet-mapping>
```

Note: A typical Berlioz implementation will declare and map multiple servlets. In addition to the servlet which may generates the main HTML view, specific servlets may be used for generating specific aspects of a website (navigation, alternate views of information). Even more servlets may be used for generating non-HTML formats such as RSS or ICal (see [GeneratingICalendar](GeneratingICalendar.md)).

## Initialisation Servlet ##

The init servlet is used to provide additional configuration options before classes are loaded by the global servlet. Although not requires, this servlet can be very useful and does not need to be mapped.
```
  <servlet>
    <servlet-name>Initialiser</servlet-name>
    <servlet-class>org.weborganic.berlioz.servlet.InitServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
  </servlet>
```

# Berlioz Services #

The Services file is an XML file where the generators are declared and mapped to URI Patterns.

See Configuration\_Services for details.

# Berlioz Properties #


# Berlioz Logging #



# Java Environment variables #

Java Environment variables are _not required_, but they can be used to better control aspects of the application when Java starts.

They are set when java starts using the `-D` option.

## berlioz.config ##

This variable allows Berlioz to look for configuration files ending with `'-'+config`.

For example, when starting your server with:
```
  java -Dberlioz.config=dev
```
Berlioz will look for logging and properties file ending with `-dev`.

This is most useful, when slight variations in the configuration are required.
For example, if different logging configurations are required when using a development server, a staging server and a production server.