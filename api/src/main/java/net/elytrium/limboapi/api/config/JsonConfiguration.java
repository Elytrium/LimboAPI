/*
 * This file is part of BungeeCord, licensed under the BSD License (BSD).
 *
 * Copyright (c) 2012 md_5
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * The name of the author may not be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *     * You may not use the software for commercial software hosting services without
 *       written permission from the author.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.elytrium.limboapi.api.config;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonConfiguration extends ConfigurationProvider {

  private final Gson json = new GsonBuilder()
      .serializeNulls()
      .setPrettyPrinting()
      .registerTypeAdapter(
          Configuration.class,
          (JsonSerializer<Configuration>) (src, typeOfSrc, context) -> context.serialize(src.self))
      .create();

  JsonConfiguration() {
  }

  @Override
  public void save(Configuration config, File file) throws IOException {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
      this.save(config, writer);
    }
  }

  @Override
  public void save(Configuration config, Writer writer) {
    this.json.toJson(config.self, writer);
  }

  @Override
  public Configuration load(File file) throws IOException {
    return this.load(file, null);
  }

  @Override
  public Configuration load(File file, Configuration defaults) throws IOException {
    try (FileInputStream is = new FileInputStream(file)) {
      return this.load(is, defaults);
    }
  }

  @Override
  public Configuration load(Reader reader) {
    return this.load(reader, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Configuration load(Reader reader, Configuration defaults) {
    Map<String, Object> map = this.json.fromJson(reader, LinkedHashMap.class);
    if (map == null) {
      map = new LinkedHashMap<>();
    }
    return new Configuration(map, defaults);
  }

  @Override
  public Configuration load(InputStream is) {
    return this.load(is, null);
  }

  @Override
  public Configuration load(InputStream is, Configuration defaults) {
    return this.load(new InputStreamReader(is, Charsets.UTF_8), defaults);
  }

  @Override
  public Configuration load(String string) {
    return this.load(string, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Configuration load(String string, Configuration defaults) {
    Map<String, Object> map = this.json.fromJson(string, LinkedHashMap.class);
    if (map == null) {
      map = new LinkedHashMap<>();
    }
    return new Configuration(map, defaults);
  }
}
