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
package org.apache.directory.shared.ldap.codec.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize the AND filter
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InitAndFilterAction extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( InitAndFilterAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new init AND filter action.
     */
    public InitAndFilterAction()
    {
        super( "Initialize AND filter" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( Asn1Container container ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

        TLV tlv = ldapMessageContainer.getCurrentTLV();

        if ( tlv.getLength() == 0 )
        {
            String msg = I18n.err( I18n.ERR_04006 );
            LOG.error( msg );
            throw new DecoderException( msg );
        }

        SearchRequest searchRequest = ldapMessageContainer.getSearchRequest();

        // We can allocate the SearchRequest
        Filter andFilter = new AndFilter( ldapMessageContainer.getTlvId() );

        // Set the filter
        ( ( SearchRequestImpl ) searchRequest ).addCurrentFilter( andFilter );

        if ( IS_DEBUG )
        {
            LOG.debug( "Initialize AND filter" );
        }
    }
}