# Introduction #

This `Hello World` website demonstrates a simple search example using Weborganic Berlioz, Flaubert, Flint open source libraries. This code can also provide a starting point for the development of new projects.

## Prerequisites ##

Downloading the sample application from <a href='http://wo-berlioz.googlecode.com/files/HelloWorld.zip'>here</a>.

Use a Subversion (SVN) client, such as the following, to checkout the example code:<br>
<ul><li><a href='http://subclipse.tigris.org/update_1.6.x'>Subclipse</a><br>
</li><li><a href='http://community.polarion.com/projects/subversive/download/eclipse/2.0/update-site/'>Subversive</a><br>
OR<br>
Download the <a href='http://code.google.com/p/wo-flint/downloads/list'>zip</a> file and import it into your development environment, say, Eclipse.<br></li></ul>

Install a JSP and Servlet container such as Apache Tomcat.<br>
<br>
<h1>Running the Example</h1>

Copy the <code>HelloWorld.war</code> file to the servlet container's <code>webapp</code> folder.<br>
<br>
Input the appropriate URI, say: <code>http://127.0.0.1:8080/HelloWorld/</code> in a browser.<br>
<br>
<b>That's it!</b>

<h1>Details</h1>

<i><b>web.xml</b></i> -web configuration file<br>
<pre><code><br>
&lt;web-app&gt;<br>
  &lt;display-name&gt;Hello World Search&lt;/display-name&gt;<br>
<br>
<br>
  &lt;!-- Initialisation servlet (default config file is 'WEB-INF/config/global.prp') --&gt;<br>
  &lt;servlet&gt;<br>
    &lt;servlet-name&gt;Initialiser&lt;/servlet-name&gt;<br>
    &lt;servlet-class&gt;org.weborganic.berlioz.servlet.InitServlet&lt;/servlet-class&gt;<br>
    &lt;init-param&gt;<br>
      &lt;param-name&gt;config-name&lt;/param-name&gt;<br>
      &lt;param-value&gt;dev&lt;/param-value&gt;<br>
    &lt;/init-param&gt;<br>
    &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;<br>
  &lt;/servlet&gt;<br>
<br>
  &lt;!-- Berlioz servlet --&gt;<br>
  &lt;servlet&gt;<br>
    &lt;servlet-name&gt;Berlioz&lt;/servlet-name&gt;<br>
    &lt;servlet-class&gt;org.weborganic.berlioz.servlet.GlobalServlet&lt;/servlet-class&gt;<br>
    &lt;load-on-startup&gt;2&lt;/load-on-startup&gt;<br>
  &lt;/servlet&gt;<br>
<br>
  &lt;!-- Error handler servlet --&gt;<br>
  &lt;servlet&gt;<br>
    &lt;servlet-name&gt;ErrorHandlerServlet&lt;/servlet-name&gt;<br>
    &lt;servlet-class&gt;org.weborganic.berlioz.servlet.ErrorHandlerServlet&lt;/servlet-class&gt;<br>
  &lt;/servlet&gt;<br>
<br>
  &lt;!-- servlet mapping --&gt;<br>
  &lt;servlet-mapping&gt;<br>
    &lt;servlet-name&gt;Berlioz&lt;/servlet-name&gt;<br>
    &lt;url-pattern&gt;/berlioz/*&lt;/url-pattern&gt;<br>
  &lt;/servlet-mapping&gt;<br>
<br>
<br>
  &lt;welcome-file-list&gt;<br>
      &lt;welcome-file&gt;index.html&lt;/welcome-file&gt;<br>
  &lt;/welcome-file-list&gt;<br>
<br>
&lt;/web-app&gt;<br>
<br>
</code></pre>


<br>
<i><b>services.xml</b></i>  -services configuration file<br>
<pre><code><br>
&lt;?xml version="1.0" encoding="utf-8"?&gt;<br>
&lt;!DOCTYPE service-config PUBLIC "-//Berlioz//DTD::Services 1.0//EN"<br>
                            "../library/services-1.0.dtd" &gt;<br>
<br>
<br>
&lt;service-config version="1.0"&gt;<br>
<br>
  &lt;services group="helloWorldGroup"&gt;<br>
    <br>
    &lt;!-- Hello World Service --&gt;<br>
    &lt;service id="helloworld" method="get"&gt;<br>
      &lt;url pattern="/helloworld"&gt;&lt;/url&gt;<br>
      &lt;generator class="helloworld.HelloWorldGenerator"/&gt; <br>
    &lt;/service&gt;<br>
    &lt;!-- Hello World Service --&gt;<br>
    <br>
  &lt;/services&gt;<br>
<br>
&lt;/service-config&gt;<br>
<br>
</code></pre>


<br>
<h1>Relevant Libraries</h1>
<br>
<a href='http://code.google.com/p/wo-berlioz/'>Web Organic Berlioz</a>
<br>
<a href='http://code.google.com/p/wo-flint/'>Web Organic Flint</a>