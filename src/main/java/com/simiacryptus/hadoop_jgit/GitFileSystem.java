/*
 * Copyright (c) 2018 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.hadoop_jgit;

import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.WeakHashMap;

public class GitFileSystem extends ProxyFileSystem {
  protected static final Logger logger = LoggerFactory.getLogger(GitFileSystem.class);
  private static final Map<URI, GitRepoFileSystem> cache = new WeakHashMap<>();
  
  public GitFileSystem() {
  }
  
  @Override
  public URI getUri() {
    try {
      return new URI("git:///");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected GitRepoFileSystem route(final Path f) {
    URI uri = f.toUri();
    ParsePath parsePath = new ParsePath(uri).invoke();
    try {
      URI basePath = new URI(String.format("https://%s/%s%s/", uri.getRawAuthority(), parsePath.getRepoPath(), parsePath.getRepoBranch()));
      return cache.computeIfAbsent(basePath, path -> {
        try {
          return new GitRepoFileSystem(path);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected Path filter(final Path f) {
    URI uri = f.toUri();
    return new Path("https", uri.getRawAuthority(), uri.getRawPath());
  }
  
  
}
