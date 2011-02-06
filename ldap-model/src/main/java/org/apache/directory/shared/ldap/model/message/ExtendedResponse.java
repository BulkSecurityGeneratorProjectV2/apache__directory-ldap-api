/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.model.message;


/**
 * Extended protocol response message used to confirm the results of a extended
 * request message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ExtendedResponse extends ResultResponse, javax.naming.ldap.ExtendedResponse
{
    /** Extended response message type enumeration value */
    MessageTypeEnum TYPE = MessageTypeEnum.EXTENDED_RESPONSE;


    /**
     * Gets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     * 
     * @return the OID of the extended response type.
     */
    String getResponseName();


    /**
     * Sets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     * 
     * @param oidv the OID of the extended response type.
     */
    void setResponseName( String oid );


    /**
     * Gets the response OID specific encoded response values.
     * 
     * @return the response specific encoded response values.
     */
    byte[] getResponseValue();


    /**
     * Sets the response OID specific encoded response values.
     * 
     * @param responseValue the response specific encoded response values.
     */
    void setResponseValue( byte[] responseValue );
}
