package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.aimir.fep.protocol.fmp.datatype.*;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * commLogEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "commLogEntry", propOrder = {
        "commLogDate",
        "commLogFileName",
        "commLogFileSize"
})
public class commLogEntry extends Entry {

	public TIMESTAMP commLogDate = 
			new TIMESTAMP(7);

	public OCTET commLogFileName = 
			new OCTET(32);

	public UINT commLogFileSize = 
			new UINT();

	@XmlTransient
	public TIMESTAMP getCommLogDate()
	{
		 return this.commLogDate;
	}

	public void setCommLogDate(TIMESTAMP commLogDate)
	{
		 this.commLogDate=commLogDate;
	}

	@XmlTransient
	public OCTET getCommLogFileName()
	{
		 return this.commLogFileName;
	}

	public void setCommLogFileName(OCTET commLogFileName)
	{
		 this.commLogFileName=commLogFileName;
	}

	@XmlTransient
	public UINT getCommLogFileSize()
	{
		 return this.commLogFileSize;
	}

	public void setCommLogFileSize(UINT commLogFileSize)
	{
		 this.commLogFileSize=commLogFileSize;
	}


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

		sb.append("CLASS["+this.getClass().getName()+"]\n");
		sb.append("commLogDate: " + commLogDate + "\n");
		sb.append("commLogFileName: " + commLogFileName + "\n");
		sb.append("commLogFileSize: " + commLogFileSize + "\n");


        return sb.toString();
    }
}