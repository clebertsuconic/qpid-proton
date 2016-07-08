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


/**
 * Session
 *
 * Note that session level flow control is handled internally by Proton.
 */
public interface Session extends Endpoint
{
    /**
     * Returns a newly created sender endpoint
     */
    Sender sender(String name);

    /**
     * Returns a newly created receiver endpoint
     */
    Receiver receiver(String name);

    Session next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote);

    Connection getConnection();

    int getIncomingCapacity();

    void setIncomingCapacity(int bytes);

    int getIncomingBytes();

    int getOutgoingBytes();

    long getOutgoingWindow();

    /**
     * Sets the outgoing window size.
     *
     * @param outgoingWindowSize the outgoing window size
     */
    void setOutgoingWindow(long outgoingWindowSize);
}
