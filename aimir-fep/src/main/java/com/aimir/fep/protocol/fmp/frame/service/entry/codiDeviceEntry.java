package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.aimir.fep.protocol.fmp.datatype.BYTE;
import com.aimir.fep.protocol.fmp.datatype.OCTET;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * codiDeviceEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="codiDeviceEntry">
 *   &lt;complexContent>
 *     &lt;extension base="{http://server.ws.command.fep.aimir.com/}entry">
 *       &lt;sequence>
 *         &lt;element name="codiDevice" type="{http://server.ws.command.fep.aimir.com/}octet" minOccurs="0"/>
 *         &lt;element name="codiBaudRate" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiParityBit" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiDataBit" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiStopBit" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiRtsCts" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "codiDeviceEntry", propOrder = {
    "codiDevice",
    "codiBaudRate",
    "codiParityBit",
    "codiDataBit",
    "codiStopBit",
    "codiRtsCts"
})
public class codiDeviceEntry extends Entry {
    public OCTET codiDevice = 
            new OCTET(16);
    public BYTE codiBaudRate = 
            new BYTE();
    public BYTE codiParityBit = 
            new BYTE();
    public BYTE codiDataBit = 
            new BYTE();
    public BYTE codiStopBit = 
            new BYTE();
    public BYTE codiRtsCts = 
            new BYTE();

    @XmlTransient
    public OCTET getCodiDevice()
    {
        return this.codiDevice;
    } 

    public void setCodiDevice(OCTET codiDevice)
    {
        this.codiDevice = codiDevice;
    }

    @XmlTransient
    public BYTE getCodiBaudRate()
    {
        return this.codiBaudRate;
    } 

    public void setCodiBaudRate(BYTE codiBaudRate)
    {
        this.codiBaudRate = codiBaudRate;
    } 

    @XmlTransient
    public BYTE getCodiParityBit()
    {
        return this.codiParityBit;
    } 

    public void setCodiParityBit(BYTE codiParityBit)
    {
        this.codiParityBit = codiParityBit;
    }

    @XmlTransient
    public BYTE getCodiDataBit()
    {
        return this.codiDataBit;
    } 

    public void setCodiDataBit(BYTE codiDataBit)
    {
        this.codiDataBit = codiDataBit;
    }

    @XmlTransient
    public BYTE getCodiStopBit()
    {
        return this.codiStopBit;
    } 

    public void setCodiStopBit(BYTE codiStopBit)
    {
        this.codiStopBit = codiStopBit;
    }

    @XmlTransient
    public BYTE getCodiRtsCts()
    {
        return this.codiRtsCts;
    } 

    public void setCodiRtsCts(BYTE codiRtsCts)
    {
        this.codiRtsCts = codiRtsCts;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("CLASS["+this.getClass().getName()+"]\n");
        sb.append("codiDevice: " + codiDevice + "\n");
        sb.append("codiBaudRate: " + codiBaudRate + "\n");
        sb.append("codiParityBit: " + codiParityBit + "\n");
        sb.append("codiDataBit: " + codiDataBit + "\n");
        sb.append("codiStopBit: " + codiStopBit + "\n");
        sb.append("codiRtsCts: " + codiRtsCts + "\n");

        return sb.toString();
    }

}