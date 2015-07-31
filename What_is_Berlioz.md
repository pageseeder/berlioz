# Introduction #

In a nutshell, Berlioz is a simple, Java Servlet-based, development framework that associates URI patterns with generated XML for processing with XSLT 2.0.

Developers use the Berlioz Java API to create custom XML generators.

# Basic configuration #

To use Berlioz in a servlet container, simply edit the web descriptor (`/WEB-INF/web.xml`) to add:
```
  <!-- Global servlet that returns the details as HTML -->
  <servlet>
    <servlet-name>Berlioz</servlet-name>
    <servlet-class>org.weborganic.berlioz.servlet.BerliozServlet</servlet-class>
  </servlet>
```

Berlioz will look for a file called **services.xml** in the `/WEB-INF/config` folder. This file defines the generator(s) that will be called for each URI Pattern. Following is an example:
```
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE service-config PUBLIC "-//Berlioz//DTD::Services 1.0//EN"
                            "http://www.weborganic.org/schema/berlioz/services-1.0.dtd">
<service-config version="1.0">

  <!-- Data related services -->
  <services group="default">

    <service id="static-page" method="get">
      <url pattern="/{+path}"/>
      <generator class="org.weborganic.berlioz.generator.NoContent" name="default" target="default"/>
    </service>

  </services>

</service-config>
```