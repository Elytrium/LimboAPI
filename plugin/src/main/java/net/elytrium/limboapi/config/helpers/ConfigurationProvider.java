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

package net.elytrium.limboapi.config.helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationProvider {

  private static final Map<Class<? extends ConfigurationProvider>, ConfigurationProvider> providers = new HashMap<>();

  static {
    try {
      providers.put(YamlConfiguration.class, new YamlConfiguration());
    } catch (NoClassDefFoundError ex) {
      // Ignore, no SnakeYAML
    }

    try {
      providers.put(JsonConfiguration.class, new JsonConfiguration());
    } catch (NoClassDefFoundError ex) {
      // Ignore, no Gson
    }
  }

  public static ConfigurationProvider getProvider(Class<? extends ConfigurationProvider> provider) {
    return providers.get(provider);
  }

  /*------------------------------------------------------------------------*/
  public abstract void save(Configuration config, File file) throws IOException;

  public abstract void save(Configuration config, Writer writer);

  public abstract Configuration load(File file) throws IOException;

  public abstract Configuration load(File file, Configuration defaults) throws IOException;

  public abstract Configuration load(Reader reader);

  public abstract Configuration load(Reader reader, Configuration defaults);

  public abstract Configuration load(InputStream is);

  public abstract Configuration load(InputStream is, Configuration defaults);

  public abstract Configuration load(String string);

  public abstract Configuration load(String string, Configuration defaults);
}
