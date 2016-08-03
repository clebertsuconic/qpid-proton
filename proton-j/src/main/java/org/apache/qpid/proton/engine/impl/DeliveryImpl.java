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

import java.util.Arrays;

import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.amqp.transport.DeliveryState;

public class DeliveryImpl implements Delivery
{
    public static final int DEFAULT_MESSAGE_FORMAT = 0;

    private DeliveryImpl _linkPrevious;
    private DeliveryImpl _linkNext;

    boolean _work;
    boolean _transportWork;

    private Record _attachments;
    private Object _context;

    private final byte[] _tag;
    private final LinkImpl _link;
    private DeliveryState _deliveryState;
    private boolean _settled;
    private boolean _remoteSettled;
    private DeliveryState _remoteDeliveryState;
    private DeliveryState _defaultDeliveryState = null;
    private int _messageFormat = DEFAULT_MESSAGE_FORMAT;

    /**
     * A bit-mask representing the outstanding work on this delivery received from the transport layer
     * that has not yet been processed by the application.
     */
    private int _flags = (byte) 0;

    private TransportDelivery _transportDelivery;
    private byte[] _data;
    private int _dataSize;
    private boolean _complete;
    private boolean _updated;
    private boolean _done;
    private int _offset;

    DeliveryImpl(final byte[] tag, final LinkImpl link, DeliveryImpl previous)
    {
        _tag = tag;
        _link = link;
        _link.incrementUnsettled();
        _linkPrevious = previous;
        if(previous != null)
        {
            previous._linkNext = this;
        }
    }


    // clebert: added to abstract
    public boolean isWork() {
        return _work;
    }

    // clebert: added to abstract
    public void setWork(boolean work) {
        this._work = work;
    }


    // clebert: added to abstract
    public boolean isTransportWork() {
        return _transportWork;
    }

    // clebert: added to abstract
    public void setTransportWork(boolean work) {
        this._transportWork = work;
    }



    public byte[] getTag()
    {
        return _tag;
    }

    public LinkImpl getLink()
    {
        return _link;
    }

    public DeliveryState getLocalState()
    {
        return _deliveryState;
    }

    public DeliveryState getRemoteState()
    {
        return _remoteDeliveryState;
    }

    public boolean remotelySettled()
    {
        return _remoteSettled;
    }

    @Override
    public void setMessageFormat(int messageFormat)
    {
        _messageFormat = messageFormat;
    }

    @Override
    public int getMessageFormat()
    {
        return _messageFormat;
    }

    public void disposition(final DeliveryState state)
    {
        _deliveryState = state;
        if(!_remoteSettled)
        {
            addToTransportWorkList();
        }
    }

    public void settle()
    {
        if (_settled) {
            return;
        }

        _settled = true;
        _link.decrementUnsettled();
        if(!_remoteSettled)
        {
            addToTransportWorkList();
        }
        else
        {
            _transportDelivery.settled();
        }
        if(_link.current() == this)
        {
            _link.advance();
        }

        _link.remove(this);
        if(_linkPrevious != null)
        {
            _linkPrevious._linkNext = _linkNext;
        }
        if(_linkNext != null)
        {
            _linkNext._linkPrevious = _linkPrevious;
        }
        updateWork();
    }

    DeliveryImpl getLinkNext()
    {
        return _linkNext;
    }

    public DeliveryImpl next()
    {
        return getLinkNext();
    }

    public void free()
    {
        settle();
    }

    DeliveryImpl getLinkPrevious()
    {
        return _linkPrevious;
    }


    int recv(byte[] bytes, int offset, int size)
    {

        final int consumed;
        if(_data != null)
        {
            //TODO - should only be if no bytes left
            consumed = Math.min(size, _dataSize);

            System.arraycopy(_data, _offset, bytes, offset, consumed);
            _offset += consumed;
            _dataSize -= consumed;
        }
        else
        {
            _dataSize =  consumed = 0;
        }
        return (_complete && consumed == 0) ? Transport.END_OF_STREAM : consumed;  //TODO - Implement
    }

    // TODO: this is better on Transport or ConnectionImpl
    public void updateWork()
    {
        getLink().getConnectionImpl().workUpdate(this);
    }

    // TODO: this is better on Transport or ConnectionImpl
    public void clearTransportWork()
    {
    }

    void addToTransportWorkList()
    {
        getLink().getConnectionImpl().addTransportWork(this);
    }

    public TransportDelivery getTransportDelivery()
    {
        return _transportDelivery;
    }

    public void setTransportDelivery(TransportDelivery transportDelivery)
    {
        _transportDelivery = transportDelivery;
    }

    public boolean isSettled()
    {
        return _settled;
    }

    int send(byte[] bytes, int offset, int length)
    {
        if(_data == null)
        {
            _data = new byte[length];
        }
        else if(_data.length - _dataSize < length)
        {
            byte[] oldData = _data;
            _data = new byte[oldData.length + _dataSize];
            System.arraycopy(oldData,_offset,_data,0,_dataSize);
            _offset = 0;
        }
        System.arraycopy(bytes,offset,_data,_dataSize+_offset,length);
        _dataSize+=length;
        addToTransportWorkList();
        return length;  //TODO - Implement.
    }

    public byte[] getData()
    {
        return _data;
    }

    @Override
    public int getDataOffset()
    {
        return _offset;
    }

    @Override
    public int getDataLength()
    {
        return _dataSize;  //TODO - Implement.
    }

    @Override
    public void setData(byte[] data)
    {
        _data = data;
    }

    @Override
    public void setDataLength(int length)
    {
        _dataSize = length;
    }

    @Override
    public void setDataOffset(int arrayOffset)
    {
        _offset = arrayOffset;
    }

    public boolean isWritable()
    {
        return getLink() instanceof SenderImpl
                && getLink().current() == this
                && ((SenderImpl) getLink()).hasCredit();
    }

    public boolean isReadable()
    {
        return getLink() instanceof ReceiverImpl
            && getLink().current() == this;
    }

    public void setComplete()
    {
        _complete = true;
    }

    public boolean isPartial()
    {
        return !_complete;
    }

    public void setRemoteDeliveryState(DeliveryState remoteDeliveryState)
    {
        _remoteDeliveryState = remoteDeliveryState;
        _updated = true;
    }

    public boolean isUpdated()
    {
        return _updated;
    }

    public void clear()
    {
        _updated = false;
        getLink().getConnectionImpl().workUpdate(this);
    }


    @Override
    public void setDone()
    {
        _done = true;
    }

    @Override
    public boolean isDone()
    {
        return _done;
    }

    public void setRemoteSettled(boolean remoteSettled)
    {
        _remoteSettled = remoteSettled;
        _updated = true;
    }

    public boolean isBuffered()
    {
        if (_remoteSettled) return false;
        if (getLink() instanceof SenderImpl) {
            if (isDone()) {
                return false;
            } else {
                return _complete || _dataSize > 0;
            }
        } else {
            return false;
        }
    }

    public Object getContext()
    {
        return _context;
    }

    public void setContext(Object context)
    {
        _context = context;
    }

    public Record attachments()
    {
        if(_attachments == null)
        {
            _attachments = new RecordImpl();
        }

        return _attachments;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("DeliveryImpl [_tag=").append(Arrays.toString(_tag))
            .append(", _link=").append(_link)
            .append(", _deliveryState=").append(_deliveryState)
            .append(", _settled=").append(_settled)
            .append(", _remoteSettled=").append(_remoteSettled)
            .append(", _remoteDeliveryState=").append(_remoteDeliveryState)
            .append(", _flags=").append(_flags)
            .append(", _defaultDeliveryState=").append(_defaultDeliveryState)
            .append(", _transportDelivery=").append(_transportDelivery)
            .append(", _dataSize=").append(_dataSize)
            .append(", _complete=").append(_complete)
            .append(", _updated=").append(_updated)
            .append(", _done=").append(_done)
            .append(", _offset=").append(_offset).append("]");
        return builder.toString();
    }

    public int pending()
    {
        return _dataSize;
    }

    @Override
    public void setDefaultDeliveryState(DeliveryState state)
    {
        _defaultDeliveryState = state;
    }

    @Override
    public DeliveryState getDefaultDeliveryState()
    {
        return _defaultDeliveryState;
    }

}
