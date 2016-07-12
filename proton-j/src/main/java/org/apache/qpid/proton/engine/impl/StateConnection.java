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

package org.apache.qpid.proton.engine.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.ProtonJConnection;

public abstract class StateConnection extends EndpointImpl implements ProtonJConnection {
   public static final int MAX_CHANNELS = 65535;

   protected List<SessionImpl> _sessions = new ArrayList<SessionImpl>();

   protected int _maxChannels = MAX_CHANNELS;

   protected LinkNode<SessionImpl> _sessionHead;
   protected LinkNode<SessionImpl> _sessionTail;

   protected int _transportWorkSize = 0;
   protected String _localContainerId = "";
   protected String _localHostname;
   protected String _remoteContainer;
   protected String _remoteHostname;
   protected Symbol[] _offeredCapabilities;
   protected Symbol[] _desiredCapabilities;
   protected Symbol[] _remoteOfferedCapabilities;
   protected Symbol[] _remoteDesiredCapabilities;
   protected Map<Symbol, Object> _properties;
   protected Map<Symbol, Object> _remoteProperties;

   protected Object _context;
   protected Collector _collector;

}
