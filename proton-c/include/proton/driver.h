#ifndef PROTON_DRIVER_H
#define PROTON_DRIVER_H 1

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

#include <proton/error.h>
#include <proton/engine.h>
#include <proton/sasl.h>

#ifdef __cplusplus
extern "C" {
#endif

/** @file
 * API for the Driver Layer.
 *
 * The driver library provides a simple implementation of a driver for
 * the proton engine. A driver is responsible for providing input,
 * output, and tick events to the bottom half of the engine API. See
 * ::pn_input, ::pn_output, and ::pn_tick. The driver also provides an
 * interface for the application to access the top half of the API
 * when the state of the engine may have changed due to I/O or timing
 * events. Additionally the driver incorporates the SASL engine as
 * well in order to provide a complete network stack: AMQP over SASL
 * over TCP.
 *
 */

typedef struct pn_driver_t pn_driver_t;
typedef struct pn_listener_t pn_listener_t;
typedef struct pn_connector_t pn_connector_t;

/** Construct a driver
 *
 *  Call pn_driver_free() to release the driver object.
 *  @return new driver object, NULL if error
 */
pn_driver_t *pn_driver(void);

/** Return the most recent error code.
 *
 * @param[in] d the driver
 *
 * @return the most recent error text for d
 */
int pn_driver_errno(pn_driver_t *d);

/** Return the most recent error text for d.
 *
 * @param[in] d the driver
 *
 * @return the most recent error text for d
 */
const char *pn_driver_error(pn_driver_t *d);

/** Set the tracing level for the given driver.
 *
 * @param[in] driver the driver to trace
 * @param[in] trace the trace level to use.
 * @todo pn_trace_t needs documentation
 */
void pn_driver_trace(pn_driver_t *driver, pn_trace_t trace);

/** Force pn_driver_wait() to return
 *
 * @param[in] driver the driver to wake up
 */
void pn_driver_wakeup(pn_driver_t *driver);

/** Wait for an active connector or listener
 *
 * @param[in] driver the driver to wait on
 * @param[in] timeout maximum time in milliseconds to wait, -1 means
 *                    infinite wait
 */
void pn_driver_wait(pn_driver_t *driver, int timeout);

/** Get the next listener with pending data in the driver.
 *
 * @param[in] driver the driver
 * @return NULL if no active listener available
 */
pn_listener_t *pn_driver_listener(pn_driver_t *driver);

/** Get the next active connector in the driver.
 *
 * Returns the next connector with pending inbound data, available
 * capacity for outbound data, or pending tick.
 *
 * @param[in] driver the driver
 * @return NULL if no active connector available
 */
pn_connector_t *pn_driver_connector(pn_driver_t *driver);

/** Free the driver allocated via pn_driver, and all associated
 *  listeners and connectors.
 *
 * @param[in] driver the driver to free, no longer valid on
 *                   return
 */
void pn_driver_free(pn_driver_t *driver);


/** pn_listener - the server API **/

/** Construct a listener for the given address.
 *
 * @param[in] driver driver that will 'own' this listener
 * @param[in] host local host address to listen on
 * @param[in] port local port to listen on
 * @param[in] context application-supplied, can be accessed via
 *                    pn_listener_context()
 * @return a new listener on the given host:port, NULL if error
 */
pn_listener_t *pn_listener(pn_driver_t *driver, const char *host,
                           const char *port, void* context);

/** Create a listener using the existing file descriptor.
 *
 * @param[in] driver driver that will 'own' this listener
 * @param[in] fd existing file descriptor for listener to listen on
 * @param[in] context application-supplied, can be accessed via
 *                    pn_listener_context()
 * @return a new listener on the given host:port, NULL if error
 */
pn_listener_t *pn_listener_fd(pn_driver_t *driver, int fd, void *context);

/** Access the head listener for a driver.
 *
 * @param[in] driver the driver whose head listener will be returned
 *
 * @return the head listener for driver or NULL if there is none
 */
pn_listener_t *pn_listener_head(pn_driver_t *driver);

/** Access the next listener.
 *
 * @param[in] listener the listener whose next listener will be
 *            returned
 *
 * @return the next listener
 */
pn_listener_t *pn_listener_next(pn_listener_t *listener);

/**
 * @todo pn_listener_trace needs documentation
 */
void pn_listener_trace(pn_listener_t *listener, pn_trace_t trace);

/** Accept a connection that is pending on the listener.
 *
 * @param[in] listener the listener to accept the connection on
 * @return a new connector for the remote, or NULL on error
 */
pn_connector_t *pn_listener_accept(pn_listener_t *listener);

/** Access the application context that is associated with the listener.
 *
 * @param[in] listener the listener whose context is to be returned
 * @return the application context that was passed to pn_listener() or
 *         pn_listener_fd()
 */
void *pn_listener_context(pn_listener_t *listener);

/** Close the socket used by the listener.
 *
 * @param[in] listener the listener whose socket will be closed.
 */
void pn_listener_close(pn_listener_t *listener);

/** Frees the given listener.
 *
 * Assumes the listener's socket has been closed prior to call.
 *
 * @param[in] listener the listener object to free, no longer valid
 *            on return
 */
void pn_listener_free(pn_listener_t *listener);

/** Configure the listener as an SSL server by setting the identifying certificate for the
 * server.
 *
 * This certificate will set the identity for all connectors created from this listener.
 * Setting these parameters configures the pn_listener_t to use SSL/TLS on all connectors
 * created from this listener (see ::pn_listener_accept).  The certificate will be used
 * for authenticating this server to connecting clients and encrypting the data stream.
 *
 * @param[in] listener the listener that will provide this certificate.
 * @param[in] certificate_file path to file containing the identifying certificate.
 * @param[in] private_key_file path to file the private key used to sign the certificate
 * @param[in] password the password used to sign the key, else NULL if key is not protected.
 * @param[in] certificate_db (optional) database of trusted CAs.  Required if client authentication used, or the certificate chain is incomplete.
 *
 * @return 0 on success
 */
int pn_listener_ssl_server_init(pn_listener_t *listener,
                                const char *certificate_file,
                                const char *private_key_file,
                                const char *password,
                                const char *certificate_db);


/** Permit a listener that has been configured to use SSL/TLS to accept connection
 * requests from clients that are not using SSL/TLS.  This configures the listener to
 * "sniff" the incoming client data stream, and dynamically determine whether SSL/TLS is
 * being used on a per-client basis.  This option is disabled by default: only clients
 * using SSL/TLS are accepted.  See ::pn_listener_ssl_server_init.
 *
 * @param[in] listener the listener that will accept client connections.
 * @return 0 on success
 */
int pn_listener_ssl_allow_unsecured_clients(pn_listener_t *listener);



/** pn_connector - the client API **/

/** Construct a connector to the given remote address.
 *
 * @param[in] driver owner of this connection.
 * @param[in] host remote host to connect to.
 * @param[in] port remote port to connect to.
 * @param[in] context application supplied, can be accessed via
 *                    pn_connector_context() @return a new connector
 *                    to the given remote, or NULL on error.
 */
pn_connector_t *pn_connector(pn_driver_t *driver, const char *host,
                             const char *port, void* context);

/** Create a connector using the existing file descriptor.
 *
 * @param[in] driver driver that will 'own' this connector.
 * @param[in] fd existing file descriptor to use for this connector.
 * @param[in] context application-supplied, can be accessed via
 *                    pn_connector_context()
 * @return a new connector to the given host:port, NULL if error.
 */
pn_connector_t *pn_connector_fd(pn_driver_t *driver, int fd, void *context);

/** Access the head connector for a driver.
 *
 * @param[in] driver the driver whose head connector will be returned
 *
 * @return the head connector for driver or NULL if there is none
 */
pn_connector_t *pn_connector_head(pn_driver_t *driver);

/** Access the next connector.
 *
 * @param[in] connector the connector whose next connector will be
 *            returned
 *
 * @return the next connector
 */
pn_connector_t *pn_connector_next(pn_connector_t *connector);

/** Set the tracing level for the given connector.
 *
 * @param[in] connector the connector to trace
 * @param[in] trace the trace level to use.
 */
void pn_connector_trace(pn_connector_t *connector, pn_trace_t trace);

/** Service the given connector.
 *
 * Handle any inbound data, outbound data, or timing events pending on
 * the connector.
 *
 * @param[in] connector the connector to process.
 */
void pn_connector_process(pn_connector_t *connector);

/** Access the listener which opened this connector.
 *
 * @param[in] connector connector whose listener will be returned.
 * @return the listener which created this connector, or NULL if the
 *         connector has no listener (e.g. an outbound client
 *         connection)
 */
pn_listener_t *pn_connector_listener(pn_connector_t *connector);

/** Access the Authentication and Security context of the connector.
 *
 * @param[in] connector connector whose securty context will be
 *                      returned
 * @return the Authentication and Security context for the connector,
 *         or NULL if none
 */
pn_sasl_t *pn_connector_sasl(pn_connector_t *connector);

/** Access the AMQP Connection associated with the connector.
 *
 * @param[in] connector the connector whose connection will be
 *                      returned
 * @return the connection context for the connector, or NULL if none
 */
pn_connection_t *pn_connector_connection(pn_connector_t *connector);

/** Assign the AMQP Connection associated with the connector.
 *
 * @param[in] connector the connector whose connection will be set.
 * @param[in] connection the connection to associate with the
 *                       connector
 */
void pn_connector_set_connection(pn_connector_t *ctor, pn_connection_t *connection);

/** Access the application context that is associated with the
 *  connector.
 *
 * @param[in] connector the connector whose context is to be returned.

 * @return the application context that was passed to pn_connector()
 *         or pn_connector_fd()
 */
void *pn_connector_context(pn_connector_t *connector);

/** Assign a new application context to the connector.
 *
 * @param[in] connector the connector which will hold the context.
 * @param[in] context new application context to associate with the
 *                    connector
 */
void pn_connector_set_context(pn_connector_t *connector, void *context);

/** Close the socket used by the connector.
 *
 * @param[in] connector the connector whose socket will be closed
 */
void pn_connector_close(pn_connector_t *connector);

/** Determine if the connector is closed.
 *
 * @return True if closed, otherwise false
 */
bool pn_connector_closed(pn_connector_t *connector);

/** Destructor for the given connector.
 *
 * Assumes the connector's socket has been closed prior to call.
 *
 * @param[in] connector the connector object to free. No longer
 *                      valid on return
 */
void pn_connector_free(pn_connector_t *connector);

/** Configure the set of trusted certificates for this client.  This causes the connector
 * to use SSL/TLS to authenticate the server and encrypt traffic.  It is intended to be
 * used by a client that is attempting to connect to a trusted server.  See
 * ::pn_driver_connector ::pn_connector ::pn_connector_fd
 *
 * @param[in] connector the connector that will use SSL/TLS
 * @param[in] certificate_db database of trusted CAs, used to authenticate the server.
 *
 * @return 0 on success
 */
int pn_connector_ssl_client_init(pn_connector_t *connector,
                                 const char *certificate_db);

/** Configure the identifying certificate for the connector.  Used for those client
 * connections that will have to authenticate -to- the remote server.
 *
 * @param[in] connector the connector that will advertise the certificate.
 * @param[in] certificate_file path to file containing the certificate.
 * @param[in] private_key_file path to file the private key used to sign the certificate
 * @param[in] password the password used to sign the key, else NULL if key is not protected.
 *
 * @return 0 on success
 */
int pn_connector_ssl_set_client_auth(pn_connector_t *connector,
                                     const char *certificate_file,
                                     const char *private_key_file,
                                     const char *password);

/** Force the peer (client) to authenticate.  This is intended to be used on those
 * connectors that have been created by a listener - it permits the server to force
 * authentication of the connected client.  See ::pn_listener_ssl_set_client_auth
 *
 * @param[in] connector the connector that will require authentication from its peer.
 * @param[in] trusted_CAs_file a file containing certificates of those CA that will be
 *  advertised to the client as trusted CAs.
 *
 * @return 0 on success
 */
int pn_connector_ssl_authenticate_client(pn_connector_t *connector,
                                         const char *trusted_CAs_file);


#ifdef __cplusplus
}
#endif

#endif /* driver.h */
