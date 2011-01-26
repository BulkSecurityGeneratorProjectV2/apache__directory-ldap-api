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
package org.apache.directory.shared.ldap.codec.search.controls.pagedSearch;


import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.ControlDecorator;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;


/**
 * A codec decorator for the {@link PagedResults}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PagedResultsDecorator extends ControlDecorator
{

    /** The number of entries to return, or returned */
    private int size;

    /** The exchanged cookie */
    private byte[] cookie;

    /** The entry change global length */
    private int pscSeqLength;


    /**
     * 
     * Creates a new instance of PagedResultsDecorator.
     *
     */
    public PagedResultsDecorator()
    {
        super( new PagedResults(), new PagedResultsDecoder() );
        cookie = StringConstants.EMPTY_BYTES;
    }


    /**
     * Compute the PagedSearchControl length, which is the sum
     * of the control length and the value length.
     * 
     * <pre>
     * PagedSearchControl value length :
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^63-1] (size) 
     *   +--> 0x04 L2 (cookie)
     * </pre> 
     */
    public int computeLength()
    {
        int sizeLength = 1 + 1 + Value.getNbBytes( size );

        int cookieLength;

        if ( cookie != null )
        {
            cookieLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
        }
        else
        {
            cookieLength = 1 + 1;
        }

        pscSeqLength = sizeLength + cookieLength;
        valueLength = 1 + TLV.getNbBytes( pscSeqLength ) + pscSeqLength;

        // Call the super class to compute the global control length
        return super.computeLength( valueLength );
    }


    /**
     * Encodes the paged search control.
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        // Encode the Control envelop
        super.encode( buffer );

        // Encode the OCTET_STRING tag
        buffer.put( UniversalTag.OCTET_STRING.getValue() );
        buffer.put( TLV.getBytes( valueLength ) );

        // Now encode the PagedSearch specific part
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( TLV.getBytes( pscSeqLength ) );

        Value.encode( buffer, size );
        Value.encode( buffer, cookie );

        return buffer;
    }


    /**
     * {@inheritDoc}
     */
    public byte[] getValue()
    {
        if ( getDecorated().getValue() == null )
        {
            try
            {
                computeLength();
                ByteBuffer buffer = ByteBuffer.allocate( valueLength );

                // Now encode the PagedSearch specific part
                buffer.put( UniversalTag.SEQUENCE.getValue() );
                buffer.put( TLV.getBytes( pscSeqLength ) );

                Value.encode( buffer, size );
                Value.encode( buffer, cookie );

                getDecorated().setValue( buffer.array() );
            }
            catch ( Exception e )
            {
                return null;
            }
        }

        return getDecorated().getValue();
    }


    /**
     * @return The requested or returned number of entries
     */
    public int getSize()
    {
        return size;
    }


    /**
     * Set the number of entry requested or returned
     *
     * @param size The number of entries 
     */
    public void setSize( int size )
    {
        this.size = size;
    }


    /**
     * @return The stored cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * Set the cookie
     *
     * @param cookie The cookie to store in this control
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * @return The integer value for the current cookie
     */
    public int getCookieValue()
    {
        int value = 0;

        switch ( cookie.length )
        {
            case 1:
                value = cookie[0] & 0x00FF;
                break;

            case 2:
                value = ( ( cookie[0] & 0x00FF ) << 8 ) + ( cookie[1] & 0x00FF );
                break;

            case 3:
                value = ( ( cookie[0] & 0x00FF ) << 16 ) + ( ( cookie[1] & 0x00FF ) << 8 ) + ( cookie[2] & 0x00FF );
                break;

            case 4:
                value = ( ( cookie[0] & 0x00FF ) << 24 ) + ( ( cookie[1] & 0x00FF ) << 16 )
                    + ( ( cookie[2] & 0x00FF ) << 8 ) + ( cookie[3] & 0x00FF );
                break;

        }

        return value;
    }


    /**
     * @see Object#equals(Object)
     */
    @SuppressWarnings( { "EqualsWhichDoesntCheckParameterClass" } )
    @Override
    public boolean equals( Object o )
    {
        if ( !super.equals( o ) )
        {
            return false;
        }

        PagedResultsDecorator otherControl = ( PagedResultsDecorator ) o;

        return ( size == otherControl.size ) && Arrays.equals( cookie, otherControl.cookie );
    }


    /**
     * Return a String representing this PagedSearchControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Paged Search Control\n" );
        sb.append( "        oid : " ).append( getOid() ).append( '\n' );
        sb.append( "        critical : " ).append( isCritical() ).append( '\n' );
        sb.append( "        size   : '" ).append( size ).append( "'\n" );
        sb.append( "        cookie   : '" ).append( Strings.dumpBytes(cookie) ).append( "'\n" );

        return sb.toString();
    }
}