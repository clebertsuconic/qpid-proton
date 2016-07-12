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

package org.apache.qpid.proton.machine;

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.impl.ConnectionImpl;

public class AMQPMachine {


   private ConnectionImpl _connectionEndpoint;



   public void bind(ConnectionImpl conn)
   {
      // TODO - check if already bound

      this._connectionEndpoint =  conn;
      _connectionEndpoint.put(Event.Type.CONNECTION_BOUND, conn);
      _connectionEndpoint.setTransport(this);
      _connectionEndpoint.incref();

      if(getRemoteState() != EndpointState.UNINITIALIZED)
      {
         _connectionEndpoint.handleOpen(_open);
         if(getRemoteState() == EndpointState.CLOSED)
         {
            _connectionEndpoint.setRemoteState(EndpointState.CLOSED);
         }

         _frameParser.flush();
      }
   }


}
