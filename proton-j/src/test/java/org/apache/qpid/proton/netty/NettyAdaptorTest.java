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

package org.apache.qpid.proton.netty;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import com.sun.codemodel.internal.util.EncoderFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.transport.Begin;
import org.apache.qpid.proton.codec.DecoderFactory;
import org.apache.qpid.proton.netty.toBeMoved.NettyBufferReader;
import org.apache.qpid.proton.netty.toBeMoved.NettyBufferWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NettyAdaptorTest {

   ServerBootstrap serverBootstrap;

   NioEventLoopGroup eventGroup;
   @Before
   public void startServer() {
      serverBootstrap = new ServerBootstrap();

      eventGroup = new NioEventLoopGroup();
      serverBootstrap.group(eventGroup).channel(NioServerSocketChannel.class);

      ChannelInitializer<Channel> factory = new ChannelInitializer<Channel>() {
         @Override
         public void initChannel(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new ProtonInitDecoder());
         }
      };
      serverBootstrap.childHandler(factory);
      serverBootstrap.bind(new InetSocketAddress("localhost", 8000)).syncUninterruptibly();
   }

   MyClient client = new MyClient();

   class MyClient extends SimpleChannelInboundHandler<ByteBuf> {

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
         System.out.println("Received client " + ByteBufUtil.hexDump(msg));
         msg.readerIndex(msg.writerIndex());
      }
   }

   @After
   public void stopServer() {
      eventGroup.shutdownGracefully();
   }

   class ClientChannel extends ChannelInitializer<SocketChannel> {

      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
         ch.pipeline().addLast(new MyClient());
      }
   }



   @Test
   public void testLoop() throws Exception {
      EventLoopGroup group = new NioEventLoopGroup();
      try {

         if (true) {
            Bootstrap bootstrap = new Bootstrap();
            ClientChannel clientChannel = new ClientChannel();
            bootstrap.group(group).channel(NioSocketChannel.class).remoteAddress(new InetSocketAddress("localhost", 8000)).handler(clientChannel);
            ChannelFuture futureChannel = bootstrap.connect();

            ByteBuf buf = Unpooled.buffer(8);
            buf.writeByte('A');
            buf.writeByte('M');
            buf.writeByte('Q');
            buf.writeByte('P');
            buf.writeByte(1);
            buf.writeByte(0);
            buf.writeByte(0);
            buf.writeByte(4);

            futureChannel.awaitUninterruptibly();

            Channel channel = futureChannel.channel();

            channel.writeAndFlush(buf);

            Begin begin = new Begin();

            for (int i = 0; i < 100; i++) {
               ByteBuf bufWrite = Unpooled.buffer(1024);
               begin.setOutgoingWindow(new UnsignedInteger(1000));
               begin.setIncomingWindow(new UnsignedInteger(1000));
               begin.setNextOutgoingId(new UnsignedInteger(1));

               bufWrite.writerIndex(4);
               DecoderFactory.getEncoder().writeObject(new NettyBufferWriter(bufWrite), begin);

               System.out.println("Hex::" + ByteBufUtil.hexDump(bufWrite));
               int position = bufWrite.writerIndex();
               bufWrite.writerIndex(0);
               bufWrite.writeInt(position);
               bufWrite.writerIndex(position);
               System.out.println("Hex::" + ByteBufUtil.hexDump(bufWrite));

               System.out.println("ReaderIndex::" + bufWrite.readerIndex() + "  / writerIndex:" + bufWrite.writerIndex());
               System.out.println("Buffer :: " + ByteBufUtil.hexDump(bufWrite));
               System.out.println("ReaderIndex::" + bufWrite.readerIndex() + "  / writerIndex:" + bufWrite.writerIndex());

//               try {
//                  Object read = DecoderFactory.getDecoder().readObject(new NettyBufferReader(bufWrite));
//                  //               System.out.println("Read::" + read);
//               }
//               catch (Exception e) {
//                  e.printStackTrace();
//               }
               bufWrite.readerIndex(0);

               channel.writeAndFlush(bufWrite);
               Thread.sleep(500);
            }
         }


         while (true)
            Thread.sleep(36000000);

      }
      catch (Exception e) {
         e.printStackTrace();
      }
//      Messenger messenger = new MessengerImpl();
//      messenger.start();
//      Message msg = new MessageImpl();
//      msg.setAddress("amqp://127.0.0.1:8000");
//      msg.setBody(new AmqpValue("hello world"));
//      messenger.put(msg);
//      messenger.send();
//      messenger.stop();

   }


   class ProtonInitDecoder extends ChannelInboundHandlerAdapter {

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object objIn) throws Exception {
         ByteBuf in = (ByteBuf)objIn;
         if (in.readableBytes() < 8) {
            System.out.println("not yet!!!");
            return;
         }

         byte[] bytes = new byte[8];
         in.getBytes(0, bytes);

         if (bytes[0] == 'A' && bytes[1] == 'M' && bytes[2] == 'Q' && bytes[3] == 'P') {
            System.err.println("init worked.. yay");
         } else {
            System.err.println("did not work");
         }


         ctx.pipeline().addLast(new MyFramer());
         ctx.pipeline().addLast(new ProtonDecoder());
         ctx.pipeline().remove(this);

         ctx.writeAndFlush(Unpooled.wrappedBuffer(bytes));

//         super.channelRead(ctx, objIn);


      }



   }


   class ProtonDecoder extends ByteToMessageDecoder {
      @Override
      protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
         int size = in.readInt();
         System.out.println("Size::" + size);
         Object obj = DecoderFactory.getDecoder().readObject(new NettyBufferReader(in));
         System.out.println("Decoding ProtonDecoder::" + obj);
         out.add(obj);
      }
   }


   public class MyFramer extends LengthFieldBasedFrameDecoder {

      public MyFramer() {
         // The interface itself is part of the buffer (hence the -4)
         super(Integer.MAX_VALUE, 0, 4, -4, 0);
      }

      protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
         System.out.println(ByteBufUtil.hexDump(in));
         return super.decode(ctx, in);
      }


   }



}
