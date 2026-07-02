package org.pageseeder.berlioz.sample.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Request;

/**
 * Tiny file-backed store for the direct API sample.
 *
 * <p>This is not intended to be a production persistence abstraction. It exists to give the sample
 * API a visible side effect: a POST request changes something that a later GET request can return.
 * The data is stored under Berlioz appdata when available, which matches the Gretty configuration
 * in this sample ({@code -Dberlioz.appdata=local/appdata}).</p>
 */
final class NoteStore {

  private NoteStore() {
  }

  /**
   * Returns the saved note text, or an empty string before the first update.
   *
   * @param req the current request, used to resolve the Berlioz appdata fallback
   * @return the saved note text, never {@code null}
   * @throws IOException if the file exists but cannot be read
   */
  static synchronized String read(Request req) throws IOException {
    File note = noteFile(req);
    if (!note.isFile()) return "";
    return Files.readString(note.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Saves the note text.
   *
   * <p>The methods are synchronized because this sample writes a single file and should remain
   * deterministic when several local test requests arrive at nearly the same time.</p>
   *
   * @param req the current request, used to resolve the Berlioz appdata fallback
   * @param text the note text to save
   * @throws IOException if the note file cannot be created or written
   */
  static synchronized void write(Request req, String text) throws IOException {
    File note = noteFile(req);
    File parent = note.getParentFile();
    if (parent != null) Files.createDirectories(parent.toPath());
    Files.writeString(note.toPath(), text, StandardCharsets.UTF_8);
  }

  private static File noteFile(Request req) {
    // Prefer appdata so local writes survive webapp rebuilds and stay out of WEB-INF.
    File appData = GlobalSettings.getAppData();
    File root = appData != null ? appData : req.getEnvironment().getPrivateFolder();
    return new File(root, "api/note.txt");
  }
}
