/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.apache.qpid.proton.microbench.protoype;

import java.nio.ByteBuffer;

/**
 * @author Clebert Suconic
 */

public class SmallListParser extends ListParser
{
   public int size(ByteBuffer buffer)
   {
      return buffer.get() & 0xff;
   }

   public int numberOfElements(ByteBuffer buffer)
   {
      return buffer.get() & 0xff;
   }

   public Object parseElement(ByteBuffer buffer)
   {
      return RawAMQPParser.parse(buffer);
   }

}
