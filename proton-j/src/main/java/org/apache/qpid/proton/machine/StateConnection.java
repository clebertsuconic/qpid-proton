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

import java.util.ArrayList;
import java.util.List;

import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.ProtonJConnection;
import org.apache.qpid.proton.engine.impl.DeliveryImpl;
import org.apache.qpid.proton.engine.impl.EndpointImpl;
import org.apache.qpid.proton.engine.impl.EventImpl;
import org.apache.qpid.proton.engine.impl.LinkImpl;
import org.apache.qpid.proton.engine.impl.LinkNode;
import org.apache.qpid.proton.engine.impl.SessionImpl;

public abstract class StateConnection extends EndpointImpl implements ProtonJConnection {
   public static final int MAX_CHANNELS = 65535;

   protected List<SessionImpl> _sessions = new ArrayList<SessionImpl>();
   protected EndpointImpl _transportTail;
   protected EndpointImpl _transportHead;
   protected int _maxChannels = MAX_CHANNELS;

   protected LinkNode<SessionImpl> _sessionHead;
   protected LinkNode<SessionImpl> _sessionTail;


   protected LinkNode<LinkImpl> _linkHead;
   protected LinkNode<LinkImpl> _linkTail;


   protected DeliveryImpl _workHead;
   protected DeliveryImpl _workTail;

   protected Collector _collector;


   @Override
   public SessionImpl session()
   {
      SessionImpl session = new SessionImpl(this);
      _sessions.add(session);


      return session;
   }

   public EventImpl put(Event.Type type, Object context)
   {
      if (_collector != null) {
         return _collector.put(type, context);
      } else {
         return null;
      }
   }


   public LinkNode<SessionImpl> addSessionEndpoint(SessionImpl endpoint)
   {
      LinkNode<SessionImpl> node;
      if(_sessionHead == null)
      {
         node = _sessionHead = _sessionTail = LinkNode.newList(endpoint);
      }
      else
      {
         node = _sessionTail = _sessionTail.addAtTail(endpoint);
      }
      return node;
   }

   public void removeSessionEndpoint(LinkNode<SessionImpl> node)
   {
      LinkNode<SessionImpl> prev = node.getPrev();
      LinkNode<SessionImpl> next = node.getNext();

      if(_sessionHead == node)
      {
         _sessionHead = next;
      }
      if(_sessionTail == node)
      {
         _sessionTail = prev;
      }
      node.remove();
   }


   public void freeSession(SessionImpl session)
   {
      _sessions.remove(session);
   }


   public void addModified(EndpointImpl endpoint)
   {
      if(_transportTail == null)
      {
         endpoint.setTransportNext(null);
         endpoint.setTransportPrev(null);
         _transportHead = _transportTail = endpoint;
      }
      else
      {
         _transportTail.setTransportNext(endpoint);
         endpoint.setTransportPrev(_transportTail);
         _transportTail = endpoint;
         _transportTail.setTransportNext(null);
      }
   }

   public void removeModified(EndpointImpl endpoint)
   {
      if(_transportHead == endpoint)
      {
         _transportHead = endpoint.transportNext();
      }
      else
      {
         endpoint.transportPrev().setTransportNext(endpoint.transportNext());
      }

      if(_transportTail == endpoint)
      {
         _transportTail = endpoint.transportPrev();
      }
      else
      {
         endpoint.transportNext().setTransportPrev(endpoint.transportPrev());
      }
   }



}
