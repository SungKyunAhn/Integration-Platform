package com.aimir.fep.protocol.fmp.gateway.circuit.relay;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Decodes Cirucuit Data Stream
 * 
 * @author D.J Park (dong7603@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2006-10-16 15:59:15 +0900 $,
 */
public class RelayDecoder implements ProtocolDecoder
{
    private static Log log = LogFactory.getLog(RelayDecoder.class);

    /**
     * decode input stream
     *
     * @param session <code>IoSession</code> session
     * @param in <code>IoBuffer</code> input stream
     * @param out <code>ProtocolDecoderOutput</code> save decoding frame 
     * @throws  ProtocolViolationException
     */
    public void decode(IoSession session, IoBuffer in,
                       ProtocolDecoderOutput out )
    throws ProtocolDecoderException
    {
        try
        { 
            log.debug("Received [" + session.getRemoteAddress()
                    + "] : "+in.limit()+" :"+in.getHexDump());
            out.write(in);
        } catch(Exception ex)
        {
            log.error("RelayDecoder::decode failed : ", ex);
            throw new ProtocolDecoderException(ex.getMessage());
        }
    }

    public void dispose(IoSession session) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception
    {
        // TODO Auto-generated method stub
        
    }
}