/*
 * Copyright 2016 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.mock.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class MockServletOutputstream extends ServletOutputStream {

  private final ByteArrayOutputStream out;

  private WriteListener listener;

  public MockServletOutputstream() {
    this(new ByteArrayOutputStream());
  }

  public MockServletOutputstream(ByteArrayOutputStream out) {
    this.out = out;
  }

  @Override
  public void write(int b) throws IOException {
    this.out.write(b);
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
    this.listener = writeListener;
  }

  public byte[] toByteArray() {
    return this.out.toByteArray();
  }

  public void reset() {
    this.out.reset();
  }

  public WriteListener getWriteListener() {
    return this.listener;
  }
}
