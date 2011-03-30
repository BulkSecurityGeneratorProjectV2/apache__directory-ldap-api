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

package org.apache.directory.shared.dsmlv2.engine;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.dsmlv2.DsmlDecorator;
import org.apache.directory.shared.dsmlv2.Dsmlv2Parser;
import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.dsmlv2.reponse.AddResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.BatchResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.BindResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.CompareResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.DelResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.ErrorResponse;
import org.apache.directory.shared.dsmlv2.reponse.ErrorResponse.ErrorResponseType;
import org.apache.directory.shared.dsmlv2.reponse.ExtendedResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.ModDNResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.ModifyResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.SearchResponseDsml;
import org.apache.directory.shared.dsmlv2.reponse.SearchResultDoneDsml;
import org.apache.directory.shared.dsmlv2.reponse.SearchResultEntryDsml;
import org.apache.directory.shared.dsmlv2.reponse.SearchResultReferenceDsml;
import org.apache.directory.shared.dsmlv2.request.BatchRequestDsml;
import org.apache.directory.shared.dsmlv2.request.BatchRequestDsml.OnError;
import org.apache.directory.shared.dsmlv2.request.BatchRequestDsml.Processing;
import org.apache.directory.shared.dsmlv2.request.BatchRequestDsml.ResponseOrder;
import org.apache.directory.shared.dsmlv2.request.Dsmlv2Grammar;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.SearchCursor;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.AbandonRequest;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.BindRequestImpl;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.CompareResponse;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.DeleteResponse;
import org.apache.directory.shared.ldap.model.message.ExtendedRequest;
import org.apache.directory.shared.ldap.model.message.ExtendedResponse;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.Request;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.Strings;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;


/**
 * This is the DSMLv2Engine. It can be use to execute operations on a LDAP Server and get the results of these operations.
 * The format used for request and responses is the DSMLv2 format.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Dsmlv2Engine
{
    /** The user. */
    protected String user;

    /** The password. */
    protected String password;

    /** The LDAP connection */
    protected LdapConnection connection;

    /** The DSVMv2 parser. */
    protected Dsmlv2Parser parser;

    /** The continue on error flag. */
    protected boolean continueOnError;

    /** The exit flag. */
    protected boolean exit = false;

    /** The batch request. */
    protected BatchRequestDsml batchRequest;

    /** The batch response. */
    protected BatchResponseDsml batchResponse = new BatchResponseDsml();
    
    protected Dsmlv2Grammar grammar = new Dsmlv2Grammar();

    /** flag to indicate to generate the response in a SOAP envelope */
    protected boolean generateSoapResp = false;

    private static final Logger LOG = LoggerFactory.getLogger( Dsmlv2Engine.class );

    /**
     * Creates a new instance of Dsmlv2Engine.
     * 
     * @param host the server host
     * @param port the server port
     * @param user the server admin Dn
     * @param password the server admin's password
     */
    public Dsmlv2Engine( String host, int port, String user, String password )
    {
        this.user = user;
        this.password = password;

        connection = new LdapNetworkConnection( host, port );
    }


    /**
     * 
     * Creates a new instance of Dsmlv2Engine.
     *
     * @param connection an unbound active connection 
     * @param user the user name to be used to bind this connection to the server
     * @param password user's credentials
     */
    public Dsmlv2Engine( LdapConnection connection, String user, String password )
    {
        this.user = user;
        this.password = password;

        this.connection = connection;
    }

    
    /**
     * Processes the file given and return the result of the operations
     * 
     * @param dsmlInput 
     *      the DSMLv2 formatted request input
     * @return
     *      the XML response in DSMLv2 Format
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    public String processDSML( String dsmlInput ) throws XmlPullParserException
    {
        parser = new Dsmlv2Parser( grammar );
        parser.setInput( dsmlInput );

        return processDSML();
    }


    /**
     * Processes the file given and return the result of the operations
     * 
     * @param fileName 
     *      the path to the file
     * @return 
     *      the XML response in DSMLv2 Format
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     * @throws FileNotFoundException
     *      if the file does not exist
     */
    public String processDSMLFile( String fileName ) throws XmlPullParserException, FileNotFoundException
    {
        parser = new Dsmlv2Parser( grammar );
        parser.setInputFile( fileName );

        return processDSML();
    }


    /**
     * process the given file and optionally writing the output to the
     * output stream(if not null)
     *
     * @param file the DSML file
     * @param respStream the output stream to which response will be written, skipped if null
     * @throws Exception
     */
    public void processDSMLFile( File file, OutputStream respStream ) throws Exception
    {
        parser = new Dsmlv2Parser();
        parser.setInputFile( file.getAbsolutePath() );

        processDSML( respStream );
    }


    /**
     * uses the default UTF-8 encoding for processing the DSML
     * 
     * @see #processDSML(InputStream, String, OutputStream) 
     */
    public void processDSML( InputStream inputStream, OutputStream out ) throws Exception
    {
        processDSML( inputStream, "UTF-8", out );
    }


    /**
     * process the DSML request(s) from the given input stream with the specified encoding 
     * and writes the response to the output stream
     * 
     * @param inputStream the input stream for DSML batch request
     * @param inputEncoding encoding to be used while reading the DSML request data
     * @param out the output stream to which DSML response will be written
     * @throws Exception
     */
    public void processDSML( InputStream inputStream, String inputEncoding, OutputStream out ) throws Exception
    {
        parser = new Dsmlv2Parser();
        parser.setInput( inputStream, inputEncoding );
        processDSML( out );
    }

    
    /**
     * Processes the Request document
     * 
     * @return the XML response in DSMLv2 Format
     */
    private String processDSML()
    {
        try
        {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            processDSML( byteOut );
            return new String( byteOut.toByteArray(), "UTF-8" );
        }
        catch( IOException e )
        {
            LOG.error( "Failed to process the DSML", e );
        }
        
        return null;
    }


    /**
     * processes the DSML batch request and writes the response of each operation will be
     * written to the given response stream if it is not null
     *
     * @param outStream the stream to which the responses will be written, can be null
     * @throws IOException
     */
    protected void processDSML( OutputStream outStream ) throws IOException
    {
        BufferedWriter respWriter = null;
       
        if ( outStream != null )
        {
            respWriter = new BufferedWriter( new OutputStreamWriter( outStream ) );

            if ( generateSoapResp )
            {
                respWriter.write( "<Envelope " );
                
                Namespace soapNs = new Namespace( null, "http://www.w3.org/2001/12/soap-envelope" );
                soapNs.write( respWriter );

                respWriter.write( "><Body>" );
            }
            
            respWriter.write( "<batchResponse " );
            
            ParserUtils.DSML_NAMESPACE.write( respWriter );
            
            respWriter.write( " " ); // a space to separate the namespace declarations
            
            ParserUtils.XSD_NAMESPACE.write( respWriter );
            
            respWriter.write( " " ); // a space to separate the namespace declarations
            
            ParserUtils.XSI_NAMESPACE.write( respWriter );
            
            respWriter.write( '>' ); // the end tag
        }

        // Binding to LDAP Server
        try
        {
            bind( 1 );
        }
        catch ( Exception e )
        {
            // Unable to connect to server
            // We create a new ErrorResponse and return the XML response.
            ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.COULD_NOT_CONNECT, e
                .getLocalizedMessage() );
            if ( respWriter != null )
            {
                writeResponse( respWriter, errorResponse );
                respWriter.write( "</batchResponse>" );
            }
            else
            {
                batchResponse.addResponse( errorResponse );
            }
            
            return;
        }

        // Processing BatchRequest:
        //    - Parsing and Getting BatchRequest
        //    - Getting and registering options from BatchRequest
        try
        {
            processBatchRequest();
        }
        catch ( XmlPullParserException e )
        {
            // We create a new ErrorResponse and return the XML response.
            ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.MALFORMED_REQUEST, I18n.err(
                I18n.ERR_03001, e.getLocalizedMessage(), e.getLineNumber(), e.getColumnNumber() ) );

            if ( respWriter != null )
            {
                writeResponse( respWriter, errorResponse );
                respWriter.write( "</batchResponse>" );
            }
            else
            {
                batchResponse.addResponse( errorResponse );
            }
            
            return;
        }

        // Processing each request:
        //    - Getting a new request
        //    - Checking if the request is well formed
        //    - Sending the request to the server
        //    - Getting and converting reponse(s) as XML
        //    - Looping until last request
        DsmlDecorator<? extends Request> request = null;

        try
        {
            request = parser.getNextRequest();
        }
        catch ( XmlPullParserException e )
        {
            // We create a new ErrorResponse and return the XML response.
            ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.MALFORMED_REQUEST, I18n.err(
                I18n.ERR_03001, e.getLocalizedMessage(), e.getLineNumber(), e.getColumnNumber() ) );
            if ( respWriter != null )
            {
                writeResponse( respWriter, errorResponse );
                respWriter.write( "</batchResponse>" );
            }
            else
            {
                batchResponse.addResponse( errorResponse );
            }
            
            return;
        }

        while ( request != null ) // (Request == null when there's no more request to process)
        {
            // Checking the request has a requestID attribute if Processing = Parallel and ResponseOrder = Unordered
            if ( ( batchRequest.getProcessing().equals( Processing.PARALLEL ) )
                && ( batchRequest.getResponseOrder().equals( ResponseOrder.UNORDERED ) )
                && ( request.getDecorated().getMessageId() <= 0 ) )
            {
                // Then we have to send an errorResponse
                ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.MALFORMED_REQUEST, I18n
                    .err( I18n.ERR_03002 ) );

                if ( respWriter != null )
                {
                    writeResponse( respWriter, errorResponse );
                }
                else
                {
                    batchResponse.addResponse( errorResponse );
                }
                
                break;
            }

            try
            {
                processRequest( request, respWriter );
            }
            catch ( Exception e )
            {
                // We create a new ErrorResponse and return the XML response.
                ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.GATEWAY_INTERNAL_ERROR, I18n.err(
                    I18n.ERR_03003, e.getMessage() ) );
                if ( respWriter != null )
                {
                    writeResponse( respWriter, errorResponse );
                }
                else
                {
                    batchResponse.addResponse( errorResponse );
                }
                
                break;
            }

            // Checking if we need to exit processing (if an error has occurred if onError == Exit)
            if ( exit )
            {
                break;
            }

            // Getting next request
            try
            {
                request = parser.getNextRequest();
            }
            catch ( XmlPullParserException e )
            {
                // We create a new ErrorResponse and return the XML response.
                ErrorResponse errorResponse = new ErrorResponse( 0, ErrorResponseType.MALFORMED_REQUEST, I18n.err(
                    I18n.ERR_03001, e.getLocalizedMessage(), e.getLineNumber(), e.getColumnNumber() ) );
                if ( respWriter != null )
                {
                    writeResponse( respWriter, errorResponse );
                }
                else
                {
                    batchResponse.addResponse( errorResponse );
                }
                
                break;
            }
        }

        if ( respWriter != null )
        {
            respWriter.write( "</batchResponse>" );
            
            if ( generateSoapResp )
            {
                respWriter.write( "</Body>" );
                respWriter.write( "</Envelope>" );
            }

            respWriter.flush();
        }
    }
    
    
    /**
     * write the response to the writer of the underlying output stream
     * @param respWriter
     * @param respDsml
     * @throws IOException
     */
    protected void writeResponse( BufferedWriter respWriter, DsmlDecorator respDsml ) throws IOException
    {
        if( respWriter != null )
        {
            Element xml = respDsml.toDsml( null );
            xml.write( respWriter );
        }
    }
    

    /**
     * @return the generateSoapResp
     */
    public boolean isGenerateSoapResp()
    {
        return generateSoapResp;
    }


    /**
     * @param generateSoapResp the generateSoapResp to set
     */
    public void setGenerateSoapResp( boolean generateSoapResp )
    {
        this.generateSoapResp = generateSoapResp;
    }

    
    /**
     * @return the batchResponse
     */
    public BatchResponseDsml getBatchResponse()
    {
        return batchResponse;
    }

    
    /**
     * @return the connection
     */
    public LdapConnection getConnection()
    {
        return connection;
    }

    
    /**
     * Processes a single request
     * 
     * @param request the request to process
     */
    protected void processRequest( DsmlDecorator<? extends Request> request, BufferedWriter respWriter  ) throws Exception
    {
        ResultCodeEnum resultCode = null;

        switch ( request.getDecorated().getType() )
        {
            case ABANDON_REQUEST:
                connection.abandon( ( AbandonRequest ) request );
                return;

            case ADD_REQUEST:
                AddResponse response = connection.add( ( AddRequest ) request );
                resultCode = response.getLdapResult().getResultCode();
                AddResponseDsml addResponseDsml = new AddResponseDsml( connection.getCodecService(), response );
                writeResponse( respWriter, addResponseDsml );

                break;

            case BIND_REQUEST:
                BindResponse bindResponse = connection.bind( ( BindRequest ) request );
                resultCode = bindResponse.getLdapResult().getResultCode();
                BindResponseDsml authResponseDsml = new BindResponseDsml( connection.getCodecService(), bindResponse );
                writeResponse( respWriter, authResponseDsml );

                break;

            case COMPARE_REQUEST:
                CompareResponse compareResponse = connection.compare( ( CompareRequest ) request );
                resultCode = compareResponse.getLdapResult().getResultCode();
                CompareResponseDsml compareResponseDsml = new CompareResponseDsml( connection.getCodecService(), compareResponse );
                writeResponse( respWriter, compareResponseDsml );

                break;

            case DEL_REQUEST:
                DeleteResponse delResponse = connection.delete( ( DeleteRequest ) request );
                resultCode = delResponse.getLdapResult().getResultCode();
                DelResponseDsml delResponseDsml = new DelResponseDsml( connection.getCodecService(), delResponse );
                writeResponse( respWriter, delResponseDsml );

                break;

            case EXTENDED_REQUEST:
                ExtendedResponse extendedResponse = connection.extended( ( ExtendedRequest<?> ) request );
                resultCode = extendedResponse.getLdapResult().getResultCode();
                ExtendedResponseDsml extendedResponseDsml = new ExtendedResponseDsml( connection.getCodecService(), extendedResponse );
                writeResponse( respWriter, extendedResponseDsml );

                break;

            case MODIFY_REQUEST:
                ModifyResponse modifyResponse = connection.modify( ( ModifyRequest ) request );
                resultCode = modifyResponse.getLdapResult().getResultCode();
                ModifyResponseDsml modifyResponseDsml = new ModifyResponseDsml( connection.getCodecService(), modifyResponse );
                writeResponse( respWriter, modifyResponseDsml );
                
                break;

            case MODIFYDN_REQUEST:
                ModifyDnResponse modifyDnResponse = connection.modifyDn( ( ModifyDnRequest ) request );
                resultCode = modifyDnResponse.getLdapResult().getResultCode();
                ModDNResponseDsml modDNResponseDsml = new ModDNResponseDsml( connection.getCodecService(), modifyDnResponse );
                writeResponse( respWriter, modDNResponseDsml );
                
                break;

            case SEARCH_REQUEST:
                SearchCursor searchResponses = connection.search( ( SearchRequest ) request );
                
                if ( respWriter != null )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append( "<searchResponse" );
                    
                    if ( request.getDecorated().getMessageId() > 0 )
                    {
                        sb.append( " requestID=\"" );
                        sb.append( request.getDecorated().getMessageId() );
                        sb.append( '"' );
                    }
                    
                    sb.append( '>' );
                    
                    respWriter.write( sb.toString() );
                }

                SearchResponseDsml searchResponseDsml = new SearchResponseDsml( connection.getCodecService() );
                searchResponseDsml.setMessageId( request.getDecorated().getMessageId() );
                
                while ( searchResponses.next() )
                {
                    Response searchResponse = searchResponses.get();

                    if ( searchResponse.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY )
                    {
                        SearchResultEntry searchResultEntry = ( SearchResultEntry ) searchResponse;

                        SearchResultEntryDsml searchResultEntryDsml = new SearchResultEntryDsml( 
                            connection.getCodecService(), searchResultEntry );
                        searchResponseDsml = new SearchResponseDsml( connection.getCodecService(), searchResultEntryDsml );

                        if ( respWriter != null )
                        {
                            writeResponse( respWriter, searchResultEntryDsml );
                        }
                        else
                        {
                            searchResponseDsml.addResponse( searchResultEntryDsml );
                        }
                    }
                    else if ( searchResponse.getType() == MessageTypeEnum.SEARCH_RESULT_REFERENCE )
                    {
                        SearchResultReference searchResultReference = ( SearchResultReference ) searchResponse;

                        SearchResultReferenceDsml searchResultReferenceDsml = new SearchResultReferenceDsml(
                            connection.getCodecService(), searchResultReference );
                        searchResponseDsml = new SearchResponseDsml( connection.getCodecService(), searchResultReferenceDsml );

                        if ( respWriter != null )
                        {
                            writeResponse( respWriter, searchResultReferenceDsml );
                        }
                        else
                        {
                            searchResponseDsml.addResponse( searchResultReferenceDsml );
                        }
                    }
                }

                SearchResultDone srDone = searchResponses.getSearchResultDone();
                resultCode = srDone.getLdapResult().getResultCode();
                
                SearchResultDoneDsml srdDsml = new SearchResultDoneDsml( connection.getCodecService(), srDone );
                writeResponse( respWriter, srdDsml);

                if ( respWriter != null )
                {
                    respWriter.write( "</searchResponse>" );
                }
                else
                {
                    batchResponse.addResponse( searchResponseDsml );
                }
                
                break;

            case UNBIND_REQUEST:
                connection.unBind();
                break;

            default:
                throw new IllegalStateException( "Unexpected request tpye " + request.getDecorated().getType() );
        }

        if ( ( !continueOnError ) && ( resultCode != null ) && ( resultCode != ResultCodeEnum.SUCCESS )
            && ( resultCode != ResultCodeEnum.COMPARE_TRUE ) && ( resultCode != ResultCodeEnum.COMPARE_FALSE )
            && ( resultCode != ResultCodeEnum.REFERRAL ) )
        {
            // Turning on Exit flag
            exit = true;
        }
    }


    /**
     * Processes the BatchRequest
     * <ul>
     *     <li>Parsing and Getting BatchRequest</li>
     *     <li>Getting and registering options from BatchRequest</li>
     * </ul>
     *     
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    protected void processBatchRequest() throws XmlPullParserException
    {
        // Parsing BatchRequest
        parser.parseBatchRequest();

        // Getting BatchRequest
        batchRequest = parser.getBatchRequest();

        if ( OnError.RESUME.equals( batchRequest.getOnError() ) )
        {
            continueOnError = true;
        }
        else if ( OnError.EXIT.equals( batchRequest.getOnError() ) )
        {
            continueOnError = false;
        }

        if ( batchRequest.getRequestID() != 0 )
        {
            if ( batchResponse != null )
            {
                batchResponse.setRequestID( batchRequest.getRequestID() );
            }
        }
    }


    /**
     * Binds to the ldap server
     * 
     * @param messageId the message Id
     * @throws EncoderException
     * @throws DecoderException
     * @throws IOException
     */
    protected void bind( int messageId ) throws LdapException, EncoderException, DecoderException, IOException
    {
        if ( ( connection != null ) && connection.isAuthenticated() )
        {
            return;
        }
        
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setSimple( true );
        bindRequest.setCredentials( Strings.getBytesUtf8(password) );
        bindRequest.setName( new Dn( user ) );
        bindRequest.setVersion3( true );
        bindRequest.setMessageId( messageId );

        BindResponse bindResponse = connection.bind( bindRequest );

        if ( bindResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
        {
            LOG.warn( "Error : {}", bindResponse.getLdapResult().getErrorMessage() );
        }
    }
}
