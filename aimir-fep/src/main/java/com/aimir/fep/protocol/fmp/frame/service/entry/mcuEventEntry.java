package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlTransient;

import com.aimir.fep.protocol.fmp.datatype.*;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * mcuEventEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 */
public class mcuEventEntry extends Entry {

	public TIMESTAMP mcuEvnetDate = 
			new TIMESTAMP(7);

	public OCTET mcuEventFileName = 
			new OCTET(32);

	public UINT mcuEventFileSize = 
			new UINT();

	@XmlTransient
	public TIMESTAMP getMcuEvnetDate()
	{
		 return this.mcuEvnetDate;
	}

	public void setMcuEvnetDate(TIMESTAMP mcuEvnetDate)
	{
		 this.mcuEvnetDate=mcuEvnetDate;
	}

	@XmlTransient
	public OCTET getMcuEventFileName()
	{
		 return this.mcuEventFileName;
	}

	public void setMcuEventFileName(OCTET mcuEventFileName)
	{
		 this.mcuEventFileName=mcuEventFileName;
	}

	@XmlTransient
	public UINT getMcuEventFileSize()
	{
		 return this.mcuEventFileSize;
	}

	public void setMcuEventFileSize(UINT mcuEventFileSize)
	{
		 this.mcuEventFileSize=mcuEventFileSize;
	}


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

		sb.append("CLASS["+this.getClass().getName()+"]\n");
		sb.append("mcuEvnetDate: " + mcuEvnetDate + "\n");
		sb.append("mcuEventFileName: " + mcuEventFileName + "\n");
		sb.append("mcuEventFileSize: " + mcuEventFileSize + "\n");


        return sb.toString();
    }
}
