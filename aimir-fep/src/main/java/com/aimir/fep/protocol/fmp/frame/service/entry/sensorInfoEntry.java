package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlTransient;

import com.aimir.fep.protocol.fmp.datatype.*;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * sensorInfoEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 */
public class sensorInfoEntry extends Entry {

	public BYTE sensorGroup = 
			new BYTE();

	public BYTE sensorMember = 
			new BYTE();

    public HEX  sensorID =
            new HEX(8);

	public OCTET sensorSerial = 
			new OCTET(18);

	public TIMESTAMP sensorLastConnect = 
			new TIMESTAMP(7);

	public TIMESTAMP sensorInstallDate = 
			new TIMESTAMP(7);
	
	public BYTE sensorAttr = new BYTE();
	
	public BYTE sensorState = new BYTE();

	@XmlTransient
	public BYTE getSensorGroup()
	{
		 return this.sensorGroup;
	}

	public void setSensorGroup(BYTE sensorGroup)
	{
		 this.sensorGroup=sensorGroup;
	}

	@XmlTransient
	public BYTE getSensorMember()
	{
		 return this.sensorMember;
	}

	public void setSensorMember(BYTE sensorMember)
	{
		 this.sensorMember=sensorMember;
	}

	@XmlTransient
	public HEX getSensorID()
	{
		 return this.sensorID;
	}

	public void setSensorID(HEX sensorID)
	{
		 this.sensorID=sensorID;
	}

	@XmlTransient
	public OCTET getSensorSerial()
	{
		 return this.sensorSerial;
	}

	public void setSensorSerial(OCTET sensorSerial)
	{
		 this.sensorSerial=sensorSerial;
	}

	@XmlTransient
	public TIMESTAMP getSensorLastConnect()
	{
		 return this.sensorLastConnect;
	}

	public void setSensorLastConnect(TIMESTAMP sensorLastConnect)
	{
		 this.sensorLastConnect=sensorLastConnect;
	}

	@XmlTransient
	public TIMESTAMP getSensorInstallDate()
	{
		 return this.sensorInstallDate;
	}

	public void setSensorInstallDate(TIMESTAMP sensorInstallDate)
	{
		 this.sensorInstallDate=sensorInstallDate;
	}

	@XmlTransient
    public BYTE getSensorAttr()
    {
        return sensorAttr;
    }

    public void setSensorAttr(BYTE sensorAttr)
    {
        this.sensorAttr = sensorAttr;
    }

    @XmlTransient
    public BYTE getSensorState()
    {
        return sensorState;
    }

    public void setSensorState(BYTE sensorState)
    {
        this.sensorState = sensorState;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

		sb.append("CLASS["+this.getClass().getName()+"]\n");
		sb.append("sensorGroup: " + sensorGroup + "\n");
		sb.append("sensorMember: " + sensorMember + "\n");
		sb.append("sensorID: " + sensorID + "\n");
		sb.append("sensorSerial: " + sensorSerial + "\n");
		sb.append("sensorLastConnect: " + sensorLastConnect + "\n");
		sb.append("sensorInstallDate: " + sensorInstallDate + "\n");
		sb.append("sensorAttr: " + sensorAttr + "\n");
		sb.append("sensorState: " + sensorState + "\n");

        return sb.toString();
    }
}