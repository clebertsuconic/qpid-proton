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

import io.netty.buffer.ByteBuf;
import org.apache.qpid.proton.codec.*;

public class NettyBufferReader implements ReadableBuffer {

   final ByteBuf buf;

   public NettyBufferReader(ByteBuf buf) {
      this.buf = buf;
   }

   @Override
   public byte get() {
      return buf.readByte();
   }

   @Override
   public int getInt() {
      return buf.readInt();
   }

   @Override
   public long getLong() {
      return buf.readLong();
   }

   public int getUnsignedShort() {
      return buf.readUnsignedShort();
   }

   @Override
   public short getShort() {
      return buf.readShort();
   }

   @Override
   public float getFloat() {
      return buf.readFloat();
   }

   @Override
   public double getDouble() {
      return buf.readDouble();
   }

   @Override
   public ReadableBuffer get(byte[] data, int offset, int length) {
      buf.readBytes(data, offset, length);
      return this;
   }

   @Override
   public ReadableBuffer get(byte[] data) {
      buf.readBytes(data);
      return this;
   }

   @Override
   public ReadableBuffer position(int position) {
      buf.readerIndex(position);
      return this;
   }

   @Override
   public ReadableBuffer slice() {
      return new NettyBufferReader(buf.slice());
   }

   @Override
   public ReadableBuffer flip() {

      buf.capacity(buf.readerIndex());
      buf.readerIndex(0);
      return this;
   }

   @Override
   public ReadableBuffer limit(int limit) {
      buf.capacity(limit);
      return this;
   }

   @Override
   public int limit() {
      return buf.capacity();
   }

   @Override
   public int remaining() {
      return buf.readableBytes();
   }

   @Override
   public int position() {
      return buf.readerIndex();
   }

   @Override
   public boolean hasRemaining() {
      return buf.readableBytes() > 0;
   }

   @Override
   public ReadableBuffer duplicate() {
      return new NettyBufferReader(buf.duplicate());
   }

   @Override
   public String readUTF8() {
      return org.apache.qpid.proton.codec.UTF8Util.readUTF(this);
   }
}
