A simple, Java-based web framework that uses XLST for rendering and a URI template notation to allocate services. Implemented as a set of servlets, Berlioz is both powerful and simple. It is also easy to deploy and extend.

# What is Berlioz? #

The founding principle of Berlioz is [separation of concerns](http://en.wikipedia.org/wiki/Separation_of_concerns). The design objectives are:
  * implement the business logic in Java
  * generate the XML necessary for the site
  * use XSLT to generate specific views of the data

By using the URI template notation as URI patterns, Berlioz provides more sophisticated URL matching than what is supported by Java servlets.

For example, Berlioz fire a service for any URL that match patterns like `/{user}/message/{id}`, `/wiki/{term}` or `/documents{;type,size}/title`; pass each URI parameter to the underlying Berlioz service.

A Berlioz service is simply a list of Java classes that generate XML. Think of it as a service model, not a formal **Web Service**. Once the XNL data has been created by the servlet, it is processed by XSLT into the destination format. While this is typically HTML, it can be any format that can be supported by XSLT (RSS, iCAl, .docx etc etc etc).

# Why Berlioz? #

With so many web frameworks around, this is a very reasonable question. The answer is that there are surprisingly few options based on Java and XSLT and even fewer that are lightweight and robust. We have used Berlioz for a lot of implementations over the past decade and feel that it is a really good toolset for people with Java and XSLT skills. We posted it on Google Code as motivation to create the best quality of code and documentation possible. Right or wrong, our experience is that strictly internal projects don't get the same level of attention to detail as public projects.

If you want a sense of what Berlioz is all about, think modest features, highly extensible, quality code, simplicity and robustness. Also, Berlioz is production ready. It was designed for professional publishing applications and that is the environment it has evolved in.

Let us know if there is anything that we can do to help you use Berlioz. We want to create something that developers find useful, so feedback is welcome.

## API Documentation ##

The Java API documentation can be found [here](http://pageseeder.org/apidocs/berlioz/latest/index.html)