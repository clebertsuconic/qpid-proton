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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.Open;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Endpoint;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.ProtonJConnection;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.machine.BareConnection;
import org.apache.qpid.proton.reactor.Reactor;

public class ConnectionImpl extends EndpointImpl implements ProtonJConnection
{
    public static final int MAX_CHANNELS = 65535;

    private List<Session> _sessions = new ArrayList<>();
    private Endpoint _transportTail;
    private Endpoint _transportHead;
    private int _maxChannels = MAX_CHANNELS;

    // TODO-now: use Collections here
    private LinkNode<Session> _sessionHead;
    private LinkNode<Session> _sessionTail;

    // TODO-now: use Collections here
    private LinkNode<Link> _linkHead;
    private LinkNode<Link> _linkTail;


    // TODO-now: use Collections here
    private Delivery _workHead;
    private Delivery _workTail;

    // TODO-now: use Collections here
    private Delivery _transportWorkHead;
    private Delivery _transportWorkTail;

    private TransportImpl _transport;

    private int _transportWorkSize = 0;
    private String _localContainerId = "";
    private String _localHostname;
    private String _remoteContainer;
    private String _remoteHostname;
    private Symbol[] _offeredCapabilities;
    private Symbol[] _desiredCapabilities;
    private Symbol[] _remoteOfferedCapabilities;
    private Symbol[] _remoteDesiredCapabilities;
    private Map<Symbol, Object> _properties;
    private Map<Symbol, Object> _remoteProperties;

    private Object _context;
    private CollectorImpl _collector;
    private Reactor _reactor;

    private static final Symbol[] EMPTY_SYMBOL_ARRAY = new Symbol[0];

    /**
     * @deprecated This constructor's visibility will be reduced to the default scope in a future release.
     * Client code outside this module should use {@link org.apache.qpid.proton.engine.Connection.Factory#create()} instead.
     */
    @Deprecated public ConnectionImpl()
    {
    }

    @Override
    public SessionImpl session()
    {
        SessionImpl session = new SessionImpl(this);
        _sessions.add(session);


        return session;
    }

    public void freeSession(Session session)
    {
        _sessions.remove(session);
    }

    public LinkNode<Session> addSessionEndpoint(Session endpoint)
    {
        LinkNode<Session> node;
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

    // TODO-now: Remove this by regular connections
    public void removeSessionEndpoint(LinkNode<Session> node)
    {
        LinkNode<Session> prev = node.getPrev();
        LinkNode<Session> next = node.getNext();

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


    public LinkNode<Link> addLinkEndpoint(Link endpoint)
    {
        LinkNode<Link> node;
        if(_linkHead == null)
        {
            node = _linkHead = _linkTail = LinkNode.newList(endpoint);
        }
        else
        {
            node = _linkTail = _linkTail.addAtTail(endpoint);
        }
        return node;
    }

    public void removeLinkEndpoint(LinkNode<Link> node)
    {
        LinkNode<Link> prev = node.getPrev();
        LinkNode<Link> next = node.getNext();

        if(_linkHead == node)
        {
            _linkHead = next;
        }
        if(_linkTail == node)
        {
            _linkTail = prev;
        }
        node.remove();
    }


    @Override
    public Session sessionHead(final EnumSet<EndpointState> local, final EnumSet<EndpointState> remote)
    {
        if(_sessionHead == null)
        {
            return null;
        }
        else
        {
            LinkNode.Query<Session> query = new EndpointImplQuery(local, remote);
            LinkNode<Session> node = query.matches(_sessionHead) ? _sessionHead : _sessionHead.next(query);
            return node == null ? null : node.getValue();
        }
    }

    @Override
    public Link linkHead(EnumSet<EndpointState> local, EnumSet<EndpointState> remote)
    {
        if(_linkHead == null)
        {
            return null;
        }
        else
        {
            LinkNode.Query<Link> query = new EndpointImplQuery(local, remote);
            LinkNode<Link> node = query.matches(_linkHead) ? _linkHead : _linkHead.next(query);
            return node == null ? null : node.getValue();
        }
    }

    @Override
    protected ConnectionImpl getConnectionImpl()
    {
        return this;
    }

    @Override
    void postFinal() {
        put(Event.Type.CONNECTION_FINAL, this);
    }

    @Override
    void doFree() {
        List<Session> sessions = new ArrayList<>(_sessions);
        for(Session session : sessions) {
            session.free();
        }
        _sessions = null;
    }

    public void modifyEndpoints() {
        if (_sessions != null) {
            for (Session ssn: _sessions) {
                ssn.modifyEndpoints();
            }
        }
        if (!freed) {
            modified();
        }
    }

    void handleOpen(Open open)
    {
        // TODO - store state
        setRemoteState(EndpointState.ACTIVE);
        setRemoteHostname(open.getHostname());
        setRemoteContainer(open.getContainerId());
        setRemoteDesiredCapabilities(open.getDesiredCapabilities());
        setRemoteOfferedCapabilities(open.getOfferedCapabilities());
        setRemoteProperties(open.getProperties());
        put(Event.Type.CONNECTION_REMOTE_OPEN, this);
    }


    Endpoint getTransportHead()
    {
        return _transportHead;
    }

    Endpoint getTransportTail()
    {
        return _transportTail;
    }

    public void addModified(Endpoint endpoint)
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

    public void removeModified(Endpoint endpoint)
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

    @Override
    public int getMaxChannels()
    {
        return _maxChannels;
    }

    public String getLocalContainerId()
    {
        return _localContainerId;
    }

    @Override
    public void setLocalContainerId(String localContainerId)
    {
        _localContainerId = localContainerId;
    }

    @Override
    public Delivery getWorkHead()
    {
        return _workHead;
    }

    @Override
    public void setContainer(String container)
    {
        _localContainerId = container;
    }

    @Override
    public String getContainer()
    {
        return _localContainerId;
    }

    @Override
    public void setHostname(String hostname)
    {
        _localHostname = hostname;
    }

    @Override
    public String getRemoteContainer()
    {
        return _remoteContainer;
    }

    @Override
    public String getRemoteHostname()
    {
        return _remoteHostname;
    }

    @Override
    public void setOfferedCapabilities(Symbol[] capabilities)
    {
        _offeredCapabilities = capabilities;
    }

    @Override
    public void setDesiredCapabilities(Symbol[] capabilities)
    {
        _desiredCapabilities = capabilities;
    }

    @Override
    public Symbol[] getRemoteOfferedCapabilities()
    {
        return _remoteOfferedCapabilities == null ? EMPTY_SYMBOL_ARRAY : _remoteOfferedCapabilities;
    }

    @Override
    public Symbol[] getRemoteDesiredCapabilities()
    {
        return _remoteDesiredCapabilities == null ? EMPTY_SYMBOL_ARRAY : _remoteDesiredCapabilities;
    }


    Symbol[] getOfferedCapabilities()
    {
        return _offeredCapabilities;
    }

    Symbol[] getDesiredCapabilities()
    {
        return _desiredCapabilities;
    }

    void setRemoteOfferedCapabilities(Symbol[] remoteOfferedCapabilities)
    {
        _remoteOfferedCapabilities = remoteOfferedCapabilities;
    }

    void setRemoteDesiredCapabilities(Symbol[] remoteDesiredCapabilities)
    {
        _remoteDesiredCapabilities = remoteDesiredCapabilities;
    }


    Map<Symbol, Object> getProperties()
    {
        return _properties;
    }

    @Override
    public void setProperties(Map<Symbol, Object> properties)
    {
        _properties = properties;
    }

    @Override
    public Map<Symbol, Object> getRemoteProperties()
    {
        return _remoteProperties;
    }

    void setRemoteProperties(Map<Symbol, Object> remoteProperties)
    {
        _remoteProperties = remoteProperties;
    }

    @Override
    public String getHostname()
    {
        return _localHostname;
    }

    void setRemoteContainer(String remoteContainerId)
    {
        _remoteContainer = remoteContainerId;
    }

    void setRemoteHostname(String remoteHostname)
    {
        _remoteHostname = remoteHostname;
    }

    // TODO-now: remove this. collection usage
    Delivery getWorkTail()
    {
        return _workTail;
    }

    public void removeWork(Delivery delivery)
    {
        if (!delivery.isWork()) return;

        Delivery next = delivery.getWorkNext();
        Delivery prev = delivery.getWorkPrev();

        if (prev != null) {
            prev.setWorkNext(next);
        }

        if (next != null) {
            next.setWorkPrev(prev);
        }


        if(_workHead == delivery)
        {
            _workHead = next;

        }

        if(_workTail == delivery)
        {
            _workTail = prev;
        }

        delivery.setWork(false);
    }


    public void addWork(Delivery delivery)
    {
        if (delivery.isWork()) return;

        delivery.setWorkNext(null);
        delivery.setWorkPrev(_workTail);

        if (_workTail != null) {
            _workTail.setWorkNext(delivery);
        }

        _workTail = delivery;

        if (_workHead == null) {
            _workHead = delivery;
        }

        delivery.setWork(true);
    }

    public Iterator<Delivery> getWorkSequence()
    {
        return new WorkSequence(_workHead);
    }

    void setTransport(TransportImpl transport)
    {
        _transport = transport;
    }

    @Override
    public TransportImpl getTransport()
    {
        return _transport;
    }

    // TODO-now: remove now, use collections
    private static class WorkSequence implements Iterator<Delivery>
    {
        private Delivery _next;

        public WorkSequence(Delivery workHead)
        {
            _next = workHead;
        }

        @Override
        public boolean hasNext()
        {
            return _next != null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Delivery next()
        {
            Delivery next = _next;
            if(next != null)
            {
                _next = next.getWorkNext();
            }
            return next;
        }
    }

    // TODO-now: use collections
    Delivery getTransportWorkHead()
    {
        return _transportWorkHead;
    }

    int getTransportWorkSize() {
        return _transportWorkSize;
    }

    @Override
    public void removeTransportWork(Delivery delivery)
    {
        if (!delivery.isTransportWork()) return;

        Delivery next = delivery.getTransportWorkNext();
        Delivery prev = delivery.getTransportWorkPrev();

        if (prev != null) {
            prev.setTransportWorkNext(next);
        }

        if (next != null) {
            next.setTransportWorkPrev(prev);
        }


        if(_transportWorkHead == delivery)
        {
            _transportWorkHead = next;

        }

        if(_transportWorkTail == delivery)
        {
            _transportWorkTail = prev;
        }

        delivery.setTransportWork(false);
        _transportWorkSize--;
    }

    @Override
    public void addTransportWork(Delivery delivery)
    {
        modified();
        if (delivery.isTransportWork()) return;

        delivery.setTransportWorkNext(null);
        delivery.setTransportWorkPrev(_transportWorkTail);

        if (_transportWorkTail != null) {
            _transportWorkTail.setTransportWorkNext(delivery);
        }

        _transportWorkTail = delivery;

        if (_transportWorkHead == null) {
            _transportWorkHead = delivery;
        }

        delivery.setTransportWork(true);
        _transportWorkSize++;
    }

    public void workUpdate(Delivery delivery)
    {
        if(delivery != null)
        {
            if(!delivery.isSettled() &&
               (delivery.isReadable() ||
                delivery.isWritable() ||
                delivery.isUpdated()))
            {
                addWork(delivery);
            }
            else
            {
                removeWork(delivery);
            }
        }
    }

    @Override
    public Object getContext()
    {
        return _context;
    }

    @Override
    public void setContext(Object context)
    {
        _context = context;
    }

    @Override
    public void collect(Collector collector)
    {
        _collector = (CollectorImpl) collector;

        put(Event.Type.CONNECTION_INIT, this);

        LinkNode<Session> ssn = _sessionHead;
        while (ssn != null) {
            put(Event.Type.SESSION_INIT, ssn.getValue());
            ssn = ssn.getNext();
        }

        LinkNode<Link> lnk = _linkHead;
        while (lnk != null) {
            put(Event.Type.LINK_INIT, lnk.getValue());
            lnk = lnk.getNext();
        }
    }

    public Event put(Event.Type type, Object context)
    {
        if (_collector != null) {
            return _collector.put(type, context);
        } else {
            return null;
        }
    }

    @Override
    void localOpen()
    {
        put(Event.Type.CONNECTION_LOCAL_OPEN, this);
    }

    @Override
    void localClose()
    {
        put(Event.Type.CONNECTION_LOCAL_CLOSE, this);
    }

    @Override
    public Reactor getReactor() {
        return _reactor;
    }

    public void setReactor(Reactor reactor) {
        _reactor = reactor;
    }
}
