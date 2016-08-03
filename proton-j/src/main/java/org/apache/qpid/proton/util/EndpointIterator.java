/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.qpid.proton.util;

import java.util.Iterator;

import org.apache.qpid.proton.engine.Endpoint;

public class EndpointIterator<T extends Endpoint> implements Iterator<T>{

   private final Iterator<T> delegatedIterator;
   private final EndpointQuery query;
   private T cachedNext;

   public EndpointIterator(Iterator<T> delegatedIterator, EndpointQuery query) {
      this.delegatedIterator = delegatedIterator;
      this.query = query;
   }

   @Override
   public boolean hasNext() {

      if (cachedNext != null) {
         return true;
      }
      else {
         cachedNext = findNext();
      }

      return cachedNext != null;
   }

   private T findNext() {
      while (delegatedIterator.hasNext()) {
         T nextEl = delegatedIterator.next();
         if (query.matches(nextEl)) {
            return nextEl;
         }
      }
      return null;
   }

   @Override
   public T next() {
      if (cachedNext == null) {
         cachedNext = findNext();
      }

      T returnValue = cachedNext;
      cachedNext = null;

      return returnValue;
   }
}
