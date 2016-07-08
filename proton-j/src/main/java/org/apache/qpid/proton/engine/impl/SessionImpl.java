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
package org.apache.qpid.proton.engine.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.ProtonJSession;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

public class SessionImpl extends EndpointImpl implements ProtonJSession
{
    private final Connection _connection;

    private Map<String, Sender> _senders = new LinkedHashMap<>();
    private Map<String, Receiver>  _receivers = new LinkedHashMap<>();
    private List<Link> _oldLinksToFree = new ArrayList<>();
    private TransportSession _transportSession;
    private int _incomingCapacity = 1024*1024;
    private int _incomingBytes = 0;
    private int _outgoingBytes = 0;
    private int _incomingDeliveries = 0;
    private int _outgoingDeliveries = 0;
    private long _outgoingWindow = Integer.MAX_VALUE;

    private LinkNode<Session> _node;


    SessionImpl(Connection connection)
    {
        _connection = connection;
        _connection.incref();
        _node = _connection.addSessionEndpoint(this);
        _connection.put(Event.Type.SESSION_INIT, this);
    }

    @Override
    public Sender sender(String name)
    {
        Sender sender = _senders.get(name);
        if(sender == null)
        {
            sender = new SenderImpl(this, name);
            _senders.put(name, sender);
        }
        else
        {
            if(sender.getLocalState() == EndpointState.CLOSED
                  && sender.getRemoteState() == EndpointState.CLOSED)
            {
                _oldLinksToFree.add(sender);

                sender = new SenderImpl(this, name);
                _senders.put(name, sender);
            }
        }
        return sender;
    }

    @Override
    public Receiver receiver(String name)
    {
        Receiver receiver = _receivers.get(name);
        if(receiver == null)
        {
            receiver = new ReceiverImpl(this, name);
            _receivers.put(name, receiver);
        }
        else
        {
            if(receiver.getLocalState() == EndpointState.CLOSED
                  && receiver.getRemoteState() == EndpointState.CLOSED)
            {
                _oldLinksToFree.add(receiver);

                receiver = new ReceiverImpl(this, name);
                _receivers.put(name, receiver);
            }
        }
        return receiver;
    }

    @Override
    public Session next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote)
    {
        LinkNode.Query<Session> query = new EndpointImplQuery(local, remote);

        LinkNode<Session> sessionNode = _node.next(query);

        return sessionNode == null ? null : sessionNode.getValue();
    }

    @Override
    protected Connection getConnectionImpl()
    {
        return _connection;
    }

    @Override
    public Connection getConnection()
    {
        return getConnectionImpl();
    }

    @Override
    void postFinal() {
        _connection.put(Event.Type.SESSION_FINAL, this);
        _connection.decref();
    }

    @Override
    void doFree() {
        _connection.freeSession(this);
        _connection.removeSessionEndpoint(_node);
        _node = null;

        List<Sender> senders = new ArrayList<Sender>(_senders.values());
        for(Sender sender : senders) {
            sender.free();
        }
        _senders.clear();

        List<Receiver> receivers = new ArrayList<Receiver>(_receivers.values());
        for(Receiver receiver : receivers) {
            receiver.free();
        }
        _receivers.clear();

        List<Link> links = new ArrayList<Link>(_oldLinksToFree);
        for(Link link : links) {
            link.free();
        }
    }

    public void modifyEndpoints() {
        for (Sender snd : _senders.values()) {
            snd.modifyEndpoints();
        }

        for (Receiver rcv : _receivers.values()) {
            rcv.modifyEndpoints();
        }
        modified();
    }

    TransportSession getTransportSession()
    {
        return _transportSession;
    }

    void setTransportSession(TransportSession transportSession)
    {
        _transportSession = transportSession;
    }

    void setNode(LinkNode<Session> node)
    {
        _node = node;
    }

    void freeSender(SenderImpl sender)
    {
        String name = sender.getName();
        Sender existing = _senders.get(name);
        if (sender.equals(existing))
        {
            _senders.remove(name);
        }
        else
        {
            _oldLinksToFree.remove(sender);
        }
    }

    void freeReceiver(Receiver receiver)
    {
        String name = receiver.getName();
        Receiver existing = _receivers.get(name);
        if (receiver.equals(existing))
        {
            _receivers.remove(name);
        }
        else
        {
            _oldLinksToFree.remove(receiver);
        }
    }

    @Override
    public int getIncomingCapacity()
    {
        return _incomingCapacity;
    }

    @Override
    public void setIncomingCapacity(int capacity)
    {
        _incomingCapacity = capacity;
    }

    @Override
    public int getIncomingBytes()
    {
        return _incomingBytes;
    }

    void incrementIncomingBytes(int delta)
    {
        _incomingBytes += delta;
    }

    @Override
    public int getOutgoingBytes()
    {
        return _outgoingBytes;
    }

    void incrementOutgoingBytes(int delta)
    {
        _outgoingBytes += delta;
    }

    void incrementIncomingDeliveries(int delta)
    {
        _incomingDeliveries += delta;
    }

    int getOutgoingDeliveries()
    {
        return _outgoingDeliveries;
    }

    void incrementOutgoingDeliveries(int delta)
    {
        _outgoingDeliveries += delta;
    }

    @Override
    void localOpen()
    {
        getConnectionImpl().put(Event.Type.SESSION_LOCAL_OPEN, this);
    }

    @Override
    void localClose()
    {
        getConnectionImpl().put(Event.Type.SESSION_LOCAL_CLOSE, this);
    }

    @Override
    public void setOutgoingWindow(long outgoingWindow) {
        if(outgoingWindow < 0 || outgoingWindow > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("Value '" + outgoingWindow + "' must be in the"
                    + " range [0 - 2^32-1]");
        }

        _outgoingWindow = outgoingWindow;
    }

    @Override
    public long getOutgoingWindow()
    {
        return _outgoingWindow;
    }
}
