/**
 * This is where the State of the protocol is processed.
 * No Framing, No SASL, just pure AMQP processing.
 * Sub classes will be either controlling framing directly or using some Protocols Handlers such as Netty.
 */
package org.apache.qpid.proton.machine;
