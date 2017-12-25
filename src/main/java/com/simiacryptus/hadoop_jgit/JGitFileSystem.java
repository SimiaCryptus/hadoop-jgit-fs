/*
 * Copyright (c) 2017 by Andrew Charneski.
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
import java.net.URI;

public class JGitFileSystem extends org.apache.hadoop.fs.FileSystem implements AutoCloseable {
  @Override
  public URI getUri() {
    return null;
  }
  
  @Override
  public FSDataInputStream open(Path path, int i) throws IOException {
    return null;
  }
  
  @Override
  public FSDataOutputStream create(Path path, FsPermission fsPermission, boolean b, int i, short i1, long l, Progressable progressable) throws IOException {
    return null;
  }
  
  @Override
  public FSDataOutputStream append(Path path, int i, Progressable progressable) throws IOException {
    return null;
  }
  
  @Override
  public boolean rename(Path path, Path path1) throws IOException {
    return false;
  }
  
  @Override
  public boolean delete(Path path, boolean b) throws IOException {
    return false;
  }
  
  @Override
  public FileStatus[] listStatus(Path path) throws IOException {
    return new FileStatus[0];
  }
  
  @Override
  public Path getWorkingDirectory() {
    return null;
  }
  
  @Override
  public void setWorkingDirectory(Path path) {
  
  }
  
  @Override
  public boolean mkdirs(Path path, FsPermission fsPermission) throws IOException {
    return false;
  }
  
  @Override
  public FileStatus getFileStatus(Path path) throws IOException {
    return null;
  }
}
