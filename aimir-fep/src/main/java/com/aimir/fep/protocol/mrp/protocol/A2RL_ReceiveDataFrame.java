package com.aimir.fep.protocol.mrp.protocol;


import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aimir.fep.protocol.fmp.datatype.OCTET;
import com.aimir.fep.protocol.mrp.command.frame.ReceiveDataFrame;
import com.aimir.fep.util.ByteArray;

/**
 * RequestDataFrame
 * 
 * @author Kang, Soyi
 */
public class A2RL_ReceiveDataFrame extends ReceiveDataFrame
{
    private static Log log = LogFactory.getLog(A2RL_ReceiveDataFrame.class);
    @SuppressWarnings("unused")
    public int cnt;
    @SuppressWarnings("unused")
    private ArrayList<byte[]> list = new ArrayList<byte[]>();

    /**
     * constructor
     */
    public A2RL_ReceiveDataFrame()
    {
    }    

    public void append(byte[] b)
    {
        log.debug("append =>"+new OCTET(b).toHexString());
        list.add(b);
        this.cnt++;
    }

    public byte[] encode() throws Exception
    {
        ByteArray array = new ByteArray();
        Iterator it = list.iterator();
        int idx = 10;
        while(it.hasNext()){
        	log.debug("idx ="+idx);
            byte[] temp = (byte[])it.next();
            array.append(cutHeaderTail(temp, A2RL_DataConstants.HEADER, A2RL_DataConstants.TAIL)); 
            idx++;
        }
        return array.toByteArray();
    }
    
    public byte[] encode(int headerLen, int tailLen) throws Exception
    {
        ByteArray array = new ByteArray();
        Iterator it = list.iterator();
        int idx = 10;
        while(it.hasNext()){
        	log.debug("idx ="+idx);
            byte[] temp = (byte[])it.next();
            array.append(cutHeaderTail(temp, headerLen, tailLen)); 
            idx++;
        }
        return array.toByteArray();
    }
    
    protected byte[] cutHeaderTail(byte[] org, int header, int tail) //classread
    {
        log.debug("org =>"+new OCTET(org).toHexString());
        byte[] ret = null;
        int lenth = org.length-header-tail;
        ret = new byte[lenth];
        System.arraycopy(org, header, ret, 0, lenth);
    
        if(ret!=null)
        	log.debug("org =>"+new OCTET(ret).toHexString());
        return ret;
    }
    
    /**
     * decode
     *
     * @param bytebuffer <code>ByteBuffer</code> bytebuffer
     * @return frame <code>GeneralDataFrame</code> frame
     */
    public static A2RL_ReceiveDataFrame decode(byte[] b) 
        throws Exception
    {
    	A2RL_ReceiveDataFrame frame = null;
        return frame;
    }

    /**
     * get string
     */
    public String toString()
    {
        StringBuffer strbuf = new StringBuffer();
        try
        {        
            strbuf.append("CLASS["+this.getClass().getName()+"]\n");
            strbuf.append("service : " + new OCTET(encode()).toHexString() + "\n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return strbuf.toString();
    }
}
