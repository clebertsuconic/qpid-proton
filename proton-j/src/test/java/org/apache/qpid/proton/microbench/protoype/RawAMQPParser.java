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

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.transport.Transfer;

/**
 *
 * This is a bare bone parser that can pretty much only do Transfer and a few things...
 * This is to measure the impact of the framework of the codec of what would be a simple reading
 * from the wire.
 * This is used to measure how much the current decoder is optimizable
 *
 * @author Clebert Suconic
 */

public class RawAMQPParser
{

   static ListParser smallList = new SmallListParser();

   public static Transfer parseTransfer(ByteBuffer buffer)
   {
      Transfer transfer = new Transfer();

      byte listType = buffer.get();


      ListParser listParser;

      // we could make it != some list object
      if (listType == (byte) 0xc0)
      {
         listParser = smallList;
      }
      else
      {
         return null;
      }

      // not used
      int size = listParser.size(buffer);
      int elements = listParser.numberOfElements(buffer);

      // System.out.println("elements : " + elements);


      for (int i = 0; i < elements; i++)
      {
         // System.out.println("Element " + i + " Pos: " + buffer.position());
         int type = buffer.get(buffer.position()) & 0xff;

         // System.out.println(" Type: " + Integer.toHexString(type) + "H" + " or " + type + " in decimals");

         Object obj = RawAMQPParser.parse(buffer);

         switch (i)
         {
            case 0:
               int value = ((Number)obj).intValue();
               transfer.setHandle(new UnsignedInteger(value));
               break;
            case 1:
                int value2 = ((Number)obj).intValue();
               transfer.setDeliveryId(new UnsignedInteger(value2));
               break;
            case 2:
                Binary binary = new Binary((byte []) obj);
                transfer.setDeliveryTag(binary);
               break;
            case 3:
                int value3 = ((Number)obj).intValue();
               transfer.setMessageFormat(new UnsignedInteger(value3));
               break;
            case 4:
               transfer.setSettled((Boolean) obj);
         }
      }

      return transfer;
   }

   public static Object parse(ByteBuffer buffer)
   {
      byte type = buffer.get();


      switch (type)
      {
         case (byte) 0x0:
            Number objectType = (Number) parse(buffer);  // we could also use
            if (objectType.intValue() == 0x14)
            {
               return parseTransfer(buffer);
            }
            break;
         case (byte) 0x43:
            return 0;
         case (byte) 0x52:
         case (byte) 0x53:
            return buffer.get() & 0xff;
         case (byte) 0x41:
            return true;
         case (byte) 0x42:
            return false;
         case (byte) 0xa0:
            int size = buffer.get() & 0xff;
            byte[] data = new byte[size];
            buffer.get(data);
            return data;
         default:
            throw new IllegalStateException("Invalid type " + Integer.toHexString(type & 0xff) + " !!!");
      }

      return null;
   }

}
