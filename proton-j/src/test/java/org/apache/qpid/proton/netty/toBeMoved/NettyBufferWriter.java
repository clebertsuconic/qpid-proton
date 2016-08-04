/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.qpid.proton.netty.toBeMoved;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.apache.qpid.proton.codec.WritableBuffer;

public class NettyBufferWriter implements WritableBuffer {

   final ByteBuf buf;

   public NettyBufferWriter(ByteBuf buf) {
      this.buf = buf;
   }

   @Override
   public void put(byte b) {
      buf.writeByte(b);
   }

   @Override
   public void putFloat(float f) {
      buf.writeFloat(f);
   }

   @Override
   public void putDouble(double d) {
      buf.writeDouble(d);
   }

   @Override
   public void put(byte[] src, int offset, int length) {
      buf.writeBytes(src, offset, length);
   }

   @Override
   public void putShort(short s) {
      buf.writeShort(s);
   }

   @Override
   public void putInt(int i) {
      buf.writeInt(i);
   }

   @Override
   public void putLong(long l) {
      buf.writeLong(l);
   }

   @Override
   public boolean hasRemaining() {
      return buf.writerIndex() < buf.capacity();
   }

   @Override
   public int remaining() {
      return buf.capacity() - buf.writerIndex();
   }

   @Override
   public int position() {
      return buf.writerIndex();
   }

   @Override
   public void position(int position) {
      buf.writerIndex(position);
   }

   @Override
   public void put(ByteBuffer payload) {
      buf.writeBytes(payload);
   }

   @Override
   public int limit() {
      return buf.capacity();
   }
}
