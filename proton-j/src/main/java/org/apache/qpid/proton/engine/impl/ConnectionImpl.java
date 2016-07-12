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
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.ProtonJConnection;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.machine.StateConnection;
import org.apache.qpid.proton.reactor.Reactor;

public class ConnectionImpl extends StateConnection
{

    private TransportImpl _transport;
    private DeliveryImpl _transportWorkHead;
    private DeliveryImpl _transportWorkTail;
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
    private Reactor _reactor;

    private static final Symbol[] EMPTY_SYMBOL_ARRAY = new Symbol[0];

    /**
     * @deprecated This constructor's visibility will be reduced to the default scope in a future release.
     * Client code outside this module should use {@link org.apache.qpid.proton.engine.Connection.Factory#create()} instead.
     */
    @Deprecated public ConnectionImpl()
    {
    }


    public void modified(boolean emit) {
        TransportImpl trans = getTransport();
        if (trans != null) {
            put(Event.Type.TRANSPORT, trans);
        }
    }
}


    protected LinkNode<LinkImpl> addLinkEndpoint(LinkImpl endpoint)
    {
        LinkNode<LinkImpl> node;
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


    void removeLinkEndpoint(LinkNode<LinkImpl> node)
    {
        LinkNode<LinkImpl> prev = node.getPrev();
        LinkNode<LinkImpl> next = node.getNext();

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
            LinkNode.Query<SessionImpl> query = new EndpointImplQuery<SessionImpl>(local, remote);
            LinkNode<SessionImpl> node = query.matches(_sessionHead) ? _sessionHead : _sessionHead.next(query);
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
            LinkNode.Query<LinkImpl> query = new EndpointImplQuery<LinkImpl>(local, remote);
            LinkNode<LinkImpl> node = query.matches(_linkHead) ? _linkHead : _linkHead.next(query);
            return node == null ? null : node.getValue();
        }
    }

    @Override
    protected ConnectionImpl getConnectionImpl()
    {
        return this;
    }

    @Override
    protected void postFinal() {
        put(Event.Type.CONNECTION_FINAL, this);
    }

    @Override
    protected void doFree() {
        List<SessionImpl> sessions = new ArrayList<SessionImpl>(_sessions);
        for(Session session : sessions) {
            session.free();
        }
        _sessions = null;
    }

    protected void modifyEndpoints() {
        if (_sessions != null) {
            for (SessionImpl ssn: _sessions) {
                ssn.modifyEndpoints();
            }
        }
        if (!freed) {
            modified();
        }
    }

    protected void handleOpen(Open open)
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


    protected EndpointImpl getTransportHead()
    {
        return _transportHead;
    }

    protected EndpointImpl getTransportTail()
    {
        return _transportTail;
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
    public DeliveryImpl getWorkHead()
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

    DeliveryImpl getWorkTail()
    {
        return _workTail;
    }

    void removeWork(DeliveryImpl delivery)
    {
        if (!delivery._work) return;

        DeliveryImpl next = delivery.getWorkNext();
        DeliveryImpl prev = delivery.getWorkPrev();

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

        delivery._work = false;
    }

    void addWork(DeliveryImpl delivery)
    {
        if (delivery._work) return;

        delivery.setWorkNext(null);
        delivery.setWorkPrev(_workTail);

        if (_workTail != null) {
            _workTail.setWorkNext(delivery);
        }

        _workTail = delivery;

        if (_workHead == null) {
            _workHead = delivery;
        }

        delivery._work = true;
    }

    public Iterator<DeliveryImpl> getWorkSequence()
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

    private static class WorkSequence implements Iterator<DeliveryImpl>
    {
        private DeliveryImpl _next;

        public WorkSequence(DeliveryImpl workHead)
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
        public DeliveryImpl next()
        {
            DeliveryImpl next = _next;
            if(next != null)
            {
                _next = next.getWorkNext();
            }
            return next;
        }
    }

    DeliveryImpl getTransportWorkHead()
    {
        return _transportWorkHead;
    }

    int getTransportWorkSize() {
        return _transportWorkSize;
    }

    public void removeTransportWork(DeliveryImpl delivery)
    {
        if (!delivery._transportWork) return;

        DeliveryImpl next = delivery.getTransportWorkNext();
        DeliveryImpl prev = delivery.getTransportWorkPrev();

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

        delivery._transportWork = false;
        _transportWorkSize--;
    }

    void addTransportWork(DeliveryImpl delivery)
    {
        modified();
        if (delivery._transportWork) return;

        delivery.setTransportWorkNext(null);
        delivery.setTransportWorkPrev(_transportWorkTail);

        if (_transportWorkTail != null) {
            _transportWorkTail.setTransportWorkNext(delivery);
        }

        _transportWorkTail = delivery;

        if (_transportWorkHead == null) {
            _transportWorkHead = delivery;
        }

        delivery._transportWork = true;
        _transportWorkSize++;
    }

    void workUpdate(DeliveryImpl delivery)
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
        _collector =  collector;

        put(Event.Type.CONNECTION_INIT, this);

        LinkNode<SessionImpl> ssn = _sessionHead;
        while (ssn != null) {
            put(Event.Type.SESSION_INIT, ssn.getValue());
            ssn = ssn.getNext();
        }

        LinkNode<LinkImpl> lnk = _linkHead;
        while (lnk != null) {
            put(Event.Type.LINK_INIT, lnk.getValue());
            lnk = lnk.getNext();
        }
    }

    @Override
    protected void localOpen()
    {
        put(Event.Type.CONNECTION_LOCAL_OPEN, this);
    }

    @Override
    protected void localClose()
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
