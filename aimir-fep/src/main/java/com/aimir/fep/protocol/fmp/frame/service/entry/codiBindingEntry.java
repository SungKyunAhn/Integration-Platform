package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.aimir.fep.protocol.fmp.datatype.BYTE;
import com.aimir.fep.protocol.fmp.datatype.HEX;
import com.aimir.fep.protocol.fmp.datatype.WORD;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * codiBindingEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2007-10-03 15:59:15 +0900 $,
 * <pre>
 * &lt;complexType name="codiBindingEntry">
 *   &lt;complexContent>
 *     &lt;extension base="{http://server.ws.command.fep.aimir.com/}entry">
 *       &lt;sequence>
 *         &lt;element name="codiBindIndex" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiBindType" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiBindLocal" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiBindRemote" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiBindID" type="{http://server.ws.command.fep.aimir.com/}hex" minOccurs="0"/>
 *         &lt;element name="codiLastHeard" type="{http://server.ws.command.fep.aimir.com/}word" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "codiBindingEntry", propOrder = {
    "codiBindIndex",
    "codiBindType",
    "codiBindLocal",
    "codiBindRemote",
    "codiBindID",
    "codiLastHeard"
})
public class codiBindingEntry extends Entry {
    public BYTE codiBindIndex = 
            new BYTE();
    public BYTE codiBindType = 
            new BYTE();
    public BYTE codiBindLocal = 
            new BYTE();
    public BYTE codiBindRemote = 
            new BYTE();
    public HEX codiBindID = 
        new HEX(8);
    public WORD codiLastHeard = 
        new WORD();

    @XmlTransient
    public BYTE getCodiBindIndex()
    {
        return this.codiBindIndex;
    } 

    public void setCodiBindIndex(BYTE codiBindIndex)
    {
        this.codiBindIndex = codiBindIndex;
    }

    @XmlTransient
    public BYTE getCodiBindType()
    {
        return this.codiBindType;
    } 

    public void setCodiBindType(BYTE codiBindType)
    {
        this.codiBindType = codiBindType;
    }

    @XmlTransient
    public BYTE getCodiBindLocal()
    {
        return this.codiBindLocal;
    } 

    public void setCodiBindLocal(BYTE codiBindLocal)
    {
        this.codiBindLocal = codiBindLocal;
    }

    @XmlTransient
    public BYTE getCodiBindRemote()
    {
        return this.codiBindRemote;
    } 

    public void setCodiBindRemote(BYTE codiBindRemote)
    {
        this.codiBindRemote = codiBindRemote;
    }
    
    @XmlTransient
    public HEX getCodiBindID()
    {
        return this.codiBindID;
    } 

    public void setCodiBindID(HEX codiBindID)
    {
        this.codiBindID = codiBindID;
    }

    @XmlTransient
    public WORD getCodiLastHeard()
    {
        return this.codiLastHeard;
    } 

    public void setCodiLastHeard(WORD codiLastHeard)
    {
        this.codiLastHeard = codiLastHeard;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("CLASS["+this.getClass().getName()+"]\n");
        sb.append("codiBindIndex: " + codiBindIndex + "\n");
        sb.append("codiBindType: " + codiBindType + "\n");
        sb.append("codiBindLocal: " + codiBindLocal + "\n");
        sb.append("codiBindRemote: " + codiBindRemote + "\n");
        sb.append("codiBindID: " + codiBindID + "\n");
        sb.append("codiLastHeard: " + codiLastHeard + "\n");

        return sb.toString();
    }
}
