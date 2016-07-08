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

import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.engine.impl.TransportDelivery;

/**
 * A delivery of a message on a particular link.
 *
 * Whilst a message is logically a long-lived object, a delivery is short-lived - it
 * is only intended to be used by the application until it is settled and all its data has been read.
 */
public interface Delivery extends Extendable
{

    byte[] getTag();

    Link getLink();

    DeliveryState getLocalState();

    DeliveryState getRemoteState();

    /**
     * updates the state of the delivery
     *
     * @param state the new delivery state
     */
    void disposition(DeliveryState state);

    /**
     * Settles this delivery.
     *
     * Causes the delivery to be removed from the connection's work list (see {@link Connection#getWorkHead()}).
     * If this delivery is its link's current delivery, the link's current delivery pointer is advanced.
     */
    void settle();

    /**
     * Returns whether this delivery has been settled.
     *
     * TODO proton-j and proton-c return the local and remote statuses respectively. Resolve this ambiguity.
     *
     * @see #settle()
     */
    boolean isSettled();

    boolean remotelySettled();

    /**
     * TODO When does an application call this method?  Do we really need this?
     */
    void free();

    /**
     * TODO-now: remove this, use collections
     * @see Connection#getWorkHead()
     */
    Delivery getWorkNext();

    // TODO-now: remove this, use collections
    Delivery getWorkPrev();

    // TODO-now: remove this, use collections
    Delivery next();

    boolean isWritable();

    /**
     * Returns whether this delivery has data ready to be received.
     *
     * @see Receiver#recv(byte[], int, int)
     */
    boolean isReadable();

    void setContext(Object o);

    Object getContext();

    /**
     * Returns whether this delivery's state or settled flag has ever remotely changed.
     *
     * TODO what is the main intended use case for calling this method?
     */
    boolean isUpdated();

    void clear();

    boolean isPartial();

    int pending();

    boolean isBuffered();

    /**
     * Configures a default DeliveryState to be used if a
     * received delivery is settled/freed without any disposition
     * state having been previously applied.
     *
     * @param state the default delivery state
     */
    void setDefaultDeliveryState(DeliveryState state);

    DeliveryState getDefaultDeliveryState();

    /**
     * Sets the message-format for this Delivery, representing the 32bit value using an int.
     *
     * The default value is 0 as per the message format defined in the core AMQP 1.0 specification.<p>
     *
     * See the following for more details:<br>
     * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html#type-transfer">
     *          http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html#type-transfer</a><br>
     * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html#type-message-format">
     *          http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html#type-message-format</a><br>
     * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#section-message-format">
     *          http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#section-message-format</a><br>
     * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#definition-MESSAGE-FORMAT">
     *          http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#definition-MESSAGE-FORMAT</a><br>
     *
     * @param messageFormat the message format
     */
    void setMessageFormat(int messageFormat);

    /**
     * Gets the message-format for this Delivery, representing the 32bit value using an int.
     *
     * @return the message-format
     * @see #setMessageFormat(int)
     */
    int getMessageFormat();

    // TODO-now: use collections
    void setWorkNext(Delivery workNext);

    // TODO-now: use collections
    void setWorkPrev(Delivery workPrev);

    // clebert: added to abstract
    boolean isWork();

    // clebert: added to abstract
    void setWork(boolean _work);

    // clebert: added to abstract
    boolean isTransportWork();

    // clebert: added to abstract
    void setTransportWork(boolean work);

    // clebert: added to abstract
    void updateWork();

    // clebert: added to abstract
    TransportDelivery getTransportDelivery();

    // clebert: added to abstract
    void setTransportDelivery(TransportDelivery transportDelivery);

    // clebert: added to abstract
    void setDone();

    // clebert: added to abstract
    boolean isDone();

    // clebert: added to abstract
    int getDataOffset();

    // clebert: added to abstract
    int getDataLength();

    // clebert: added to abstract
    void setData(byte[] data);

    // clebert: added to abstract
    void setDataLength(int length);

    // clebert: added to abstract
    void setDataOffset(int arrayOffset);

    // clebert: added to abstract
    void setComplete();

    // clebert: added to abstract
    void setRemoteDeliveryState(DeliveryState remoteDeliveryState);

    // clebert: added to abstract
    void setRemoteSettled(boolean remoteSettled);

    byte[] getData();

    // TODO-now: use collections
    void setTransportWorkNext(Delivery transportWorkNext);

    // TODO-now: use collections
    void setTransportWorkPrev(Delivery transportWorkPrev);


    Delivery clearTransportWork();

    // TODO-now: use collection
    Delivery getTransportWorkNext();

    // TODO-now: use collection
    Delivery getTransportWorkPrev();





}
