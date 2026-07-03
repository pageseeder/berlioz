package org.pageseeder.berlioz.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

final class GetFileSystemInfoTest {

  @TempDir
  Path folder;

  @Test
  void testProcessWithDetails() throws Exception {
    File publicFolder = Files.createDirectory(this.folder.resolve("public")).toFile();
    File privateFolder = Files.createDirectory(this.folder.resolve("private")).toFile();

    write(publicFolder, "root.txt", 4);
    write(publicFolder, "alpha/a.txt", 2);
    write(publicFolder, "alpha/nested/b.txt", 3);
    write(publicFolder, "beta/c.txt", 5);
    write(publicFolder, "WEB-INF/secret.txt", 11);
    write(privateFolder, "config.xml", 7);

    XmlStringBuilder xml = new XmlStringBuilder();
    new GetFileSystemInfo().generate(request(publicFolder, privateFolder, "true"), xml);

    Document doc = parse(xml.toString());
    Element root = doc.getDocumentElement();
    assertEquals("file-system", root.getTagName());
    assertFalse(root.getAttribute("free-space").isEmpty());
    assertFalse(root.getAttribute("total-space").isEmpty());

    Element publicInfo = child(root, "public");
    assertEquals("14", publicInfo.getAttribute("total-size"));
    assertEquals("4", publicInfo.getAttribute("total-count"));
    assertEquals(2, publicInfo.getElementsByTagName("directory").getLength());
    assertDirectory(publicInfo, 0, "alpha", "5", "2");
    assertDirectory(publicInfo, 1, "beta", "5", "1");

    Element privateInfo = child(root, "private");
    assertEquals("7", privateInfo.getAttribute("total-size"));
    assertEquals("1", privateInfo.getAttribute("total-count"));
  }

  @Test
  void testProcessWithoutDetails() throws Exception {
    File publicFolder = Files.createDirectory(this.folder.resolve("public")).toFile();
    File privateFolder = Files.createDirectory(this.folder.resolve("private")).toFile();

    XmlStringBuilder xml = new XmlStringBuilder();
    new GetFileSystemInfo().generate(request(publicFolder, privateFolder, null), xml);

    Element root = parse(xml.toString()).getDocumentElement();
    assertEquals("file-system", root.getTagName());
    assertNull(childOrNull(root, "public"));
    assertNull(childOrNull(root, "private"));
  }

  private static void assertDirectory(Element parent, int index, String name, String size, String count) {
    Element directory = (Element) parent.getElementsByTagName("directory").item(index);
    assertEquals(name, directory.getAttribute("name"));
    assertEquals(size, directory.getAttribute("file-size"));
    assertEquals(count, directory.getAttribute("file-count"));
  }

  private static Request request(File publicFolder, File privateFolder, String details) {
    Environment env = (Environment) Proxy.newProxyInstance(
        Environment.class.getClassLoader(),
        new Class<?>[] {Environment.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getPublicFolder":
              return publicFolder;
            case "getPrivateFolder":
              return privateFolder;
            default:
              throw new UnsupportedOperationException(method.getName());
          }
        });

    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[] {Request.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getEnvironment":
              return env;
            case "getParameter":
              return "details".equals(args[0]) ? details : null;
            default:
              return null;
          }
        });
  }

  private static void write(File root, String relativePath, int size) throws Exception {
    File file = new File(root, relativePath);
    Files.createDirectories(file.toPath().getParent());
    Files.write(file.toPath(), new byte[size]);
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static Element child(Element parent, String name) {
    Element child = childOrNull(parent, name);
    assertNotNull(child);
    return child;
  }

  private static Element childOrNull(Element parent, String name) {
    for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
      if (parent.getChildNodes().item(i) instanceof Element) {
        Element child = (Element) parent.getChildNodes().item(i);
        if (name.equals(child.getTagName())) {
          return child;
        }
      }
    }
    return null;
  }
}
