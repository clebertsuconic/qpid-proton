/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.qpid.proton.microbench;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.codec.AMQPDefinedTypes;
import org.apache.qpid.proton.codec.DecoderImpl;
import org.apache.qpid.proton.codec.EncoderImpl;
import org.apache.qpid.proton.microbench.protoype.RawAMQPParser;
import org.junit.Test;

/**
 * @author Clebert Suconic
 */

public class MicrobenchParserTest
{

    String strTransfer =
/*  1 */ "0000" + "001D" + "0200" + "0001" +
/*  2 */ "0053" + "14C0" + "1005" + "4352" +
/*  3 */ "04A0" + "0800" + "0000" + "0000" +
/*  4 */ "0000" + "0443" + "41";


/*
    Uncomment this if you want to do more comparisons
    @Test
    public void testRunInLooop() throws Throwable
    {
        for (int i = 0; i < 10; i++)
        {
            testRawParsing();
            testProtonDecoder();

        }
    } */


    @Test
    public void testProtonDecoder()
    {

        readMethod(strTransfer, 8, "testProtonDecoder");

    }

    private void readMethod(String parseString, int jump, String name)
    {

        byte[] allocated = hexStringToByteArray(parseString);

        DecoderImpl decoder = new DecoderImpl();
        EncoderImpl encoder = new EncoderImpl(decoder);
        AMQPDefinedTypes.registerAllTypes(decoder, encoder);

        ByteBuffer readableBuffer = ByteBuffer.allocate(allocated.length);
        readableBuffer.put(allocated);

        decoder.setByteBuffer(readableBuffer);

        long time = 0;
        for (long i = 0; i < 10000000L; i++)
        {
            if (i == 0)
            {
                time = System.currentTimeMillis();
            }
            readableBuffer.position(jump); // moving DOF

            decoder.readObject();
        }

        long timeEnd = System.currentTimeMillis() - time;

        System.out.println("Total " + name + " = " + timeEnd);
    }


    /** this is using the Raw parser which doesn't depend on Proton decoder */
    @Test
    public void testRawParsing()
    {
        byte[] allocated = hexStringToByteArray(strTransfer);

        ByteBuffer buffer = ByteBuffer.allocate(allocated.length);
        buffer.put(allocated);
        buffer.rewind();

        long time = 0;

        for (long i = 0; i < 10000000L; i++)
        {
            if (i == 0)
            {
                time = System.currentTimeMillis();
            }
            buffer.position(8); // moving DOF
            Transfer transfer = (Transfer) RawAMQPParser.parse(buffer);
        }

        long timeEnd = System.currentTimeMillis() - time;

        System.out.println("Total testRawParsing  = " + timeEnd);

    }


    @Test
    public void testValidateWires()
    {
        byte[] allocated = hexStringToByteArray(strTransfer);

        ByteBuffer buffer = ByteBuffer.allocate(allocated.length);
        buffer.put(allocated);
        buffer.rewind();

        buffer.position(8); // moving DOF
        Transfer transfer = (Transfer) RawAMQPParser.parse(buffer);

        buffer.position(8); // moving DOF



        DecoderImpl decoder = new DecoderImpl();
        EncoderImpl encoder = new EncoderImpl(decoder);
        AMQPDefinedTypes.registerAllTypes(decoder, encoder);

        decoder.setByteBuffer(buffer);


        Transfer transfer2 = (Transfer)decoder.readObject();


        Assert.assertEquals(transfer.toString(), transfer2.toString());

    }


    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
