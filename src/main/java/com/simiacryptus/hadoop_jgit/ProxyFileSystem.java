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

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;

/**
 * The type Proxy file system.
 */
public abstract class ProxyFileSystem extends ConfigurableFileSystem {
  private Path workingDirectory;
  
  /**
   * Route git repo file system.
   *
   * @param f the f
   * @return the git repo file system
   */
  protected abstract GitRepoFileSystem route(Path f);
  
  /**
   * Filter path.
   *
   * @param f the f
   * @return the path
   */
  protected abstract Path filter(Path f);
  
  @Override
  public FSDataInputStream open(final Path f, final int bufferSize) throws IOException {
    return route(f).open(filter(f), bufferSize);
  }
  
  @Override
  public FileStatus[] listStatus(final Path f) throws IOException {
    return route(f).listStatus(filter(f));
  }
  
  @Override
  public Path getWorkingDirectory() {
    return workingDirectory;
  }
  
  @Override
  public void setWorkingDirectory(final Path new_dir) {
    this.workingDirectory = new_dir;
  }
  
  @Override
  public FileStatus getFileStatus(final Path f) throws IOException {
    return route(f).getFileStatus(filter(f));
  }
  
  @Override
  public FSDataOutputStream create(
    final Path f,
    final FsPermission permission,
    final boolean overwrite,
    final int bufferSize,
    final short replication,
    final long blockSize,
    final Progressable progress
  )
  {
    return route(f).create(filter(f), permission, overwrite, bufferSize, replication, blockSize, progress);
  }
  
  @Override
  public FSDataOutputStream append(final Path f, final int bufferSize, final Progressable progress) {
    return route(f).append(filter(f), bufferSize, progress);
  }
  
  @Override
  public boolean rename(final Path src, final Path dst) {
    return route(src).rename(filter(src), dst);
  }
  
  @Override
  public boolean delete(final Path f, final boolean recursive) {
    return route(f).delete(filter(f), recursive);
  }
  
  @Override
  public boolean mkdirs(final Path f, final FsPermission permission) {
    return route(f).mkdirs(filter(f), permission);
  }
}
