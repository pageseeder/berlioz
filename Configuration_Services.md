# Introduction #

The Berlioz services are entirely configured in the `/WEB-INF/config/services.xml` of your Web application.

This XML file will control what the XML generators are used and what URI Patterns they should be mapped to.

# Services #

The services configuration file must validate the [Service 1.0 DTD](http://code.google.com/p/wo-berlioz/source/browse/resource/library/services-1.0.dtd)

The service configuration should look like:
```
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE service-config PUBLIC "-//Berlioz//DTD::Services 1.0//EN"
                            "http://www.weborganic.org/schema/berlioz/services-1.0.dtd">
<service-config version="1.0">

   <!-- The actual configuration goes here -->

</service-config>
```

## Service group ##

Services are grouped together to provide more flexibility to XSLT.

```
  <!-- Default group of services -->
  <services group="default">
     <!-- services for this group are declared and mapped here -->
  </services>
```

This is when a website has multiple distinct areas or to set some services aside from others (eg. search related).

## Service ##

A service (short of a better name) is a collection of generators executed when the URL matches a specific URI pattern.

See example:
```
    <service id="static-page" method="get">
      <url pattern="/{+path}"/>
      <generator class="org.weborganic.flaubert.StaticPageGenerator" name="default" target="main"/>
    </service>
```

A service must have a unique ID, and specify the set of HTTP methods it supports.

_Note: This is not a Web service_

## Generator ##

Generators are java classes that must produce XML and implement the [ContentGenerator API](http://weborganic.org/apidoc/berlioz/core/org/weborganic/berlioz/content/ContentGenerator.html).

For convenience, they can be given a 'target' and a 'name' that will be passed on to the XSLT.
```
<generator class="org.weborganic.berlioz.generator.NoContent" name="default" target="main"/>
```

## URI Patterns ##

The URI Pattern notation is similar to that of URI Templates, and designed to address some of the shortcoming of the Servlet url patterns. In particular, they can be used to extract variables from the URL and pass them to the underlying generator.

For example, the URI pattern `/{user}/preferences` can be used to pass the **user** variable to the generator.

Some examples of URI patterns

```
   <url pattern="/{+path}"/>
   <url pattern="/{user}/details"/>
   <url pattern="/{user}/message/{id}"/>
   <url pattern="/search/{term}"/>
   <url pattern="/document{;match}/{id}"/>
```