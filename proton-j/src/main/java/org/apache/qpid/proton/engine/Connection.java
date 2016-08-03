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
package org.apache.qpid.proton.engine;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.impl.ConnectionImpl;
import org.apache.qpid.proton.engine.impl.LinkNode;
import org.apache.qpid.proton.reactor.Reactor;
import org.apache.qpid.proton.reactor.ReactorChild;


/**
 * Maintains lists of sessions, links and deliveries in a state
 * that is interesting to the application.
 *
 * These are exposed by returning the head of those lists via
 * {@link #sessionHead(EnumSet, EnumSet)}, {@link #linkHead(EnumSet, EnumSet)}
 * {@link #getWorkHead()} respectively.
 */
public interface Connection extends Endpoint, ReactorChild
{

    final class Factory
    {
        public static Connection create() {
            return new ConnectionImpl();
        }
    }

    /**
     * Returns a newly created session
     *
     * TODO does the Connection's channel-max property limit how many sessions can be created,
     * or opened, or neither?
     */
    Session session();

    /**
     * Returns the head of the list of sessions in the specified states.
     *
     * Typically used to discover sessions whose remote state has acquired
     * particular values, e.g. sessions that have been remotely opened or closed.
     *
     * TODO what ordering guarantees on the returned "linked list" are provided?
     *
     * @see Session#next(EnumSet, EnumSet)
     */
    Session sessionHead(EnumSet<EndpointState> local, EnumSet<EndpointState> remote);

    /**
     * Returns the head of the list of links in the specified states.
     *
     * Typically used to discover links whose remote state has acquired
     * particular values, e.g. links that have been remotely opened or closed.
     *
     * @see Link#next(EnumSet, EnumSet)
     */
    Link linkHead(EnumSet<EndpointState> local, EnumSet<EndpointState> remote);

    /**
     * Returns the head of the delivery work list. The delivery work list consists of
     * unsettled deliveries whose state has been changed by the other container
     * and not yet locally processed.
     *
     * @see Receiver#recv(byte[], int, int)
     * @see Delivery#settle()
     * @see Delivery#getWorkNext()
     */
    Delivery getWorkHead();

    void setContainer(String container);

    String getContainer();

    /**
     * Set the name of the host (either fully qualified or relative) to which
     * this connection is connecting to.  This information may be used by the
     * remote peer to determine the correct back-end service to connect the
     * client to.  This value will be sent in the Open performative.
     *
     * <b>Note that it is illegal to set the hostname to a numeric IP
     * address or include a port number.</b>
     *
     * @param hostname the RFC1035 compliant host name.
     */
    void setHostname(String hostname);

    String getHostname();

    String getRemoteContainer();

    String getRemoteHostname();

    void setOfferedCapabilities(Symbol[] capabilities);

    void setDesiredCapabilities(Symbol[] capabilities);

    Symbol[] getRemoteOfferedCapabilities();

    Symbol[] getRemoteDesiredCapabilities();

    Map<Symbol,Object> getRemoteProperties();

    void setProperties(Map<Symbol,Object> properties);

    @Override
    Object getContext();

    @Override
    void setContext(Object context);

    void collect(Collector collector);

    Transport getTransport();

    Reactor getReactor();

    // Clebert: Added to abstract
    Event put(Event.Type type, Object context);

    // Clebert: Added to abstract
    void addModified(Endpoint endpoint);

    // Clebert: Added to abstract
    void removeModified(Endpoint endpoint);

    // Clebert: Added to abstract
    LinkNode<Link> addLinkEndpoint(Link endpoint);

    // Clebert: Added to abstract
    void removeLinkEndpoint(LinkNode<Link> node);

    // TODO-now: collection candidate
    void removeTransportWork(Delivery delivery);

    // TODO-now: collection candidate
    void addTransportWork(Delivery delivery);

    // Cebert: Added to make it abstract
    void removeWork(Delivery delivery);

    // Cebert: Added to make it abstract
    void addWork(Delivery delivery);

    // Clebert: Added to make it abstract
    void workUpdate(Delivery delivery);

    // TODO-now: remove this, use iterator
    Iterator<Delivery> getWorkSequence();

    void freeSession(Session session);

    Iterable<Session> sessions(EnumSet<EndpointState> local, EnumSet<EndpointState> remote);

}
