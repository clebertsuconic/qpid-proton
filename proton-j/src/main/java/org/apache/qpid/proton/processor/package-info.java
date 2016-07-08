/**
 * This has the bare bones to just process the AMQP Protocol itself.
 * No Framing, No SASL, just pure AMQP processing.
 * Sub classes will be either controlling framing directly or using some Protocols Handlers such as Netty.
 */
package org.apache.qpid.proton.processor;
