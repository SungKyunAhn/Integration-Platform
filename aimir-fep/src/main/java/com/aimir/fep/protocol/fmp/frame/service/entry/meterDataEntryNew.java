package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlTransient;

import com.aimir.fep.protocol.fmp.datatype.*;
import com.aimir.fep.protocol.fmp.frame.service.Entry;
import com.aimir.fep.util.Hex;

/**
 * meterDataEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 */
public class meterDataEntryNew extends Entry {

    public OCTET mdID = 
            new OCTET(8);

    public OCTET mdSerial = 
            new OCTET(20);

    public BYTE mdType = 
            new BYTE();

    public BYTE mdServiceType = 
        new BYTE();
    
    public BYTE mdVendor = 
            new BYTE();    
  
    public WORD dataCount = new WORD(1);

    //decode할때 length 가 먼저 나와야 해서 추가.
    //public WORD dataLen = new WORD();
    
    //public TIMESTAMP mdTime = 
    //        new TIMESTAMP(7);

    public OCTET mdData = 
            new OCTET();

    @XmlTransient
    public OCTET getMdID()
    {
         return this.mdID;
    }

    @XmlTransient
    public WORD getDataCount() {
		return dataCount;
	}

	public void setDataCount(int value) {
		this.dataCount.setValue(value);
	}

	public void setMdID(OCTET mdID)
    {
         this.mdID=mdID;
    }

	@XmlTransient
    public OCTET getMdSerial()
    {
         return this.mdSerial;
    }

    public void setMdSerial(OCTET mdSerial)
    {
         this.mdSerial=mdSerial;
    }

    @XmlTransient
    public BYTE getMdType()
    {
         return this.mdType;
    }

    public void setMdType(BYTE mdType)
    {
         this.mdType=mdType;
    }

    @XmlTransient
    public BYTE getMdVendor()
    {
         return this.mdVendor;
    }

    public void setMdVendor(BYTE mdVendor)
    {
         this.mdVendor=mdVendor;
    }

    @XmlTransient
    public BYTE getMdServiceType()
    {
         return this.mdServiceType;
    }

    public void setMdServiceType(BYTE mdServiceType)
    {
         this.mdServiceType=mdServiceType;
    }

    @XmlTransient
    public OCTET getMdData()
    {
         return this.mdData;
    }

    public void setMdData(OCTET mdData)
    {
		this.mdData = mdData;
	}


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("CLASS["+this.getClass().getName()+"]\n");
        sb.append("mdID: " + mdID.toHexString() + "\n");
        sb.append("mdSerial: " + mdSerial + "\n");
        sb.append("mdType: " + mdType + "\n");
        sb.append("mdServiceType: " + mdServiceType + "\n");
        sb.append("dataCount: " + dataCount.getValue() + "\n");
        
        if(mdData != null && mdData.getValue().length > 0){
        	sb.append("mdData: " + Hex.decode(mdData.getValue()) + "\n");
        }else{
        	sb.append("mdData: " + "null" + "\n");
        }
        return sb.toString();
    }
}
