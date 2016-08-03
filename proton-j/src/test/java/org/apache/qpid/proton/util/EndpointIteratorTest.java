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

package org.apache.qpid.proton.util;

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.impl.ConnectionImpl;
import org.junit.Assert;
import org.junit.Test;

public class EndpointIteratorTest {

   @Test
   public void testFilter() {
      ArrayList<ConnectionImpl> implList = new ArrayList<>();
      implList.add(create(EndpointState.ACTIVE, EndpointState.ACTIVE));
      implList.add(create(EndpointState.ACTIVE, EndpointState.ACTIVE));
      implList.add(create(EndpointState.CLOSED, EndpointState.CLOSED));
      implList.add(create(EndpointState.ACTIVE, EndpointState.ACTIVE));
      implList.add(create(EndpointState.ACTIVE, EndpointState.ACTIVE));

      EndpointIterator<ConnectionImpl> iterator = new EndpointIterator(implList.iterator(), new EndpointQuery<>(EnumSet.of(EndpointState.CLOSED), EnumSet.of(EndpointState.CLOSED)));

      int count = 0;
      while (iterator.hasNext()) {
         ConnectionImpl connection = iterator.next();
         Assert.assertNotNull(connection);
         Assert.assertEquals(EndpointState.CLOSED, connection.getLocalState());
         Assert.assertEquals(EndpointState.CLOSED, connection.getRemoteState());
         count++;
      }
      Assert.assertEquals(1, count);


      count = 0;
      for (ConnectionImpl connection: new EndpointIterable<ConnectionImpl>(implList, new EndpointQuery(EnumSet.of(EndpointState.CLOSED), EnumSet.of(EndpointState.CLOSED)))) {
         Assert.assertNotNull(connection);
         Assert.assertEquals(EndpointState.CLOSED, connection.getLocalState());
         Assert.assertEquals(EndpointState.CLOSED, connection.getRemoteState());
         count++;
      }
      Assert.assertEquals(1, count);

      // this will go for anything
      iterator = new EndpointIterator(implList.iterator(), new EndpointQuery<>(null, null));

      count = 0;
      while (iterator.hasNext()) {
         ConnectionImpl connection = iterator.next();
         Assert.assertNotNull(connection);
         count++;
      }

      Assert.assertEquals(5, count);
      // this will go for anything

      count = 0;
      for (ConnectionImpl connection : new EndpointIterable<ConnectionImpl>(implList, new EndpointQuery(EnumSet.allOf(EndpointState.class), EnumSet.allOf(EndpointState.class)))) {
         Assert.assertNotNull(connection);
         count++;
      }
      Assert.assertEquals(5, count);

   }


   private ConnectionImpl create(EndpointState local, EndpointState remote) {
      ConnectionImpl conn = new ConnectionImpl();
      conn.setLocalState(local);
      conn.setRemoteState(remote);
      return conn;
   }
}
