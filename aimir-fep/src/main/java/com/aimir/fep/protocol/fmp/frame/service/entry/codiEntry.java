package com.aimir.fep.protocol.fmp.frame.service.entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.aimir.fep.protocol.fmp.datatype.*;
import com.aimir.fep.protocol.fmp.frame.service.Entry;

/**
 * codiEntry
 * generated by MIB Tool, Do not modify
 *
 * @author Y.S Kim (sorimo@nuritelecom.com)
 * @version $Rev: 1 $, $Date: 2005-11-21 15:59:15 +0900 $,
 * <pre>
 * &lt;complexType name="codiEntry">
 *   &lt;complexContent>
 *     &lt;extension base="{http://server.ws.command.fep.aimir.com/}entry">
 *       &lt;sequence>
 *         &lt;element name="codiMask" type="{http://server.ws.command.fep.aimir.com/}word" minOccurs="0"/>
 *         &lt;element name="codiIndex" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiID" type="{http://server.ws.command.fep.aimir.com/}hex" minOccurs="0"/>
 *         &lt;element name="codiType" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiShortID" type="{http://server.ws.command.fep.aimir.com/}word" minOccurs="0"/>
 *         &lt;element name="codiFwVer" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiHwVer" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiZAIfVer" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiZZIfVer" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiFwBuild" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiResetKind" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiAutoSetting" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiChannel" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiPanID" type="{http://server.ws.command.fep.aimir.com/}word" minOccurs="0"/>
 *         &lt;element name="codiExtPanID" type="{http://server.ws.command.fep.aimir.com/}octet" minOccurs="0"/>
 *         &lt;element name="codiRfPower" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiTxPowerMode" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiPermit" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiEnableEncrypt" type="{http://server.ws.command.fep.aimir.com/}byte" minOccurs="0"/>
 *         &lt;element name="codiLineKey" type="{http://server.ws.command.fep.aimir.com/}octet" minOccurs="0"/>
 *         &lt;element name="codiNetworkKey" type="{http://server.ws.command.fep.aimir.com/}octet" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "codiEntry", propOrder = {
    "codiMask",
    "codiIndex",
    "codiID",
    "codiType",
    "codiShortID",
    "codiFwVer",
    "codiHwVer",
    "codiZAIfVer",
    "codiZZIfVer",
    "codiFwBuild",
    "codiResetKind",
    "codiAutoSetting",
    "codiChannel",
    "codiPanID",
    "codiExtPanID",
    "codiRfPower",
    "codiTxPowerMode",
    "codiPermit",
    "codiEnableEncrypt",
    "codiLineKey",
    "codiNetworkKey"
})
public class codiEntry extends Entry {
    public WORD codiMask = 
            new WORD();
    public BYTE codiIndex = 
            new BYTE();
    public HEX codiID = 
            new HEX(8);
    public BYTE codiType = 
            new BYTE();
    public WORD codiShortID = 
            new WORD();
    public BYTE codiFwVer = 
            new BYTE();
    public BYTE codiHwVer = 
            new BYTE();
    public BYTE codiZAIfVer = 
            new BYTE();
    public BYTE codiZZIfVer = 
            new BYTE();
    public BYTE codiFwBuild = 
            new BYTE();
    public BYTE codiResetKind = 
            new BYTE();
    public BYTE codiAutoSetting = 
            new BYTE();
    public BYTE codiChannel = 
            new BYTE();
    public WORD codiPanID = 
            new WORD();
    public OCTET codiExtPanID =  
            new OCTET(8);
    public BYTE codiRfPower = 
            new BYTE();
    public BYTE codiTxPowerMode = 
            new BYTE();
    public BYTE codiPermit = 
            new BYTE();
    public BYTE codiEnableEncrypt = 
            new BYTE();
    public OCTET codiLineKey = 
            new OCTET(16);
    public OCTET codiNetworkKey = 
            new OCTET(16);

    @XmlTransient
    public WORD getCodiMask()
    {
        return this.codiMask;
    } 

    public void setCodiMask(WORD codiMask)
    {
        this.codiMask = codiMask;
    } 

    @XmlTransient
    public BYTE getCodiIndex()
    {
        return this.codiIndex;
    } 
    
    public void setCodiIndex(BYTE codiIndex)
    {
        this.codiIndex = codiIndex;
    } 
    
    @XmlTransient
    public HEX getCodiID()
    {
        return this.codiID;
    } 
    
    public void setCodiID(HEX codiID)
    {
        this.codiID = codiID;
    }
    
    @XmlTransient
    public BYTE getCodiType()
    {
        return this.codiType;
    } 
    
    public void setCodiType(BYTE codiType)
    {
        this.codiType = codiType;
    }
    
    @XmlTransient
    public WORD getCodiShortID()
    {
        return this.codiShortID;
    } 
    
    public void setCodiShortID(WORD codiShortID)
    {
        this.codiShortID = codiShortID;
    }
    
    @XmlTransient
    public BYTE getCodiFwVer()
    {
        return this.codiFwVer;
    } 
    
    public void setCodiFwVer(BYTE codiFwVer)
    {
        this.codiFwVer = codiFwVer;
    }
    
    @XmlTransient
    public BYTE getCodiHwVer()
    {
        return this.codiHwVer;
    } 
    
    public void setCodiHwVer(BYTE codiHwVer)
    {
        this.codiHwVer = codiHwVer;
    }
    
    @XmlTransient
    public BYTE getCodiZAIfVer()
    {
        return this.codiZAIfVer;
    } 
    
    public void setCodiZAIfVer(BYTE codiZAIfVer)
    {
        this.codiZAIfVer = codiZAIfVer;
    }
    
    @XmlTransient
    public BYTE getCodiZZIfVer()
    {
        return this.codiZZIfVer;
    } 
    
    public void setCodiZZIfVer(BYTE codiZZIfVer)
    {
        this.codiZZIfVer = codiZZIfVer;
    }
    
    @XmlTransient
    public BYTE getCodiFwBuild()
    {
        return this.codiFwBuild;
    } 
    
    public void setCodiFwBuild(BYTE codiFwBuild)
    {
        this.codiFwBuild = codiFwBuild;
    } 
    
    @XmlTransient
    public BYTE getCodiResetKind()
    {
        return this.codiResetKind;
    } 
    
    public void setCodiResetKind(BYTE codiResetKind)
    {
        this.codiResetKind = codiResetKind;
    } 
    
    @XmlTransient
    public BYTE getCodiAutoSetting()
    {
        return this.codiAutoSetting;
    } 
    
    public void setCodiAutoSetting(BYTE codiAutoSetting)
    {
        this.codiAutoSetting = codiAutoSetting;
    }
    
    @XmlTransient
    public BYTE getCodiChannel()
    {
        return this.codiChannel;
    } 
    
    public void setCodiChannel(BYTE codiChannel)
    {
        this.codiChannel = codiChannel;
    }
    
    @XmlTransient
    public WORD getCodiPanID()
    {
        return this.codiPanID;
    } 
    
    public void setCodiPanID(WORD codiPanID)
    {
        this.codiPanID = codiPanID;
    }
    
    @XmlTransient
    public OCTET getCodiExtPanID()
    {
        return this.codiExtPanID;
    } 
    
    public void setCodiExtPanID(OCTET codiExtPanID)
    {
        this.codiExtPanID = codiExtPanID;
    } 
    
    @XmlTransient
    public BYTE getCodiRfPower()
    {
        return this.codiRfPower;
    } 
    
    public void setCodiRfPower(BYTE codiRfPower)
    {
        this.codiRfPower = codiRfPower;
    } 
    
    @XmlTransient
    public BYTE getCodiTxPowerMode()
    {
        return this.codiTxPowerMode;
    } 
    
    public void setCodiTxPowerMode(BYTE codiTxPowerMode)
    {
        this.codiTxPowerMode = codiTxPowerMode;
    } 
    
    @XmlTransient
    public BYTE getCodiPermit()
    {
        return this.codiPermit;
    } 
    
    public void setCodiPermit(BYTE codiPermit)
    {
        this.codiPermit = codiPermit;
    } 
    
    @XmlTransient
    public BYTE getCodiEnableEncrypt()
    {
        return this.codiEnableEncrypt;
    } 
    
    public void setCodiEnableEncrypt(BYTE codiEnableEncrypt)
    {
        this.codiEnableEncrypt = codiEnableEncrypt;
    }
    
    @XmlTransient
    public OCTET getCodiLineKey()
    {
        return this.codiLineKey;
    } 
    
    public void setCodiLineKey(OCTET codiLineKey)
    {
        this.codiLineKey = codiLineKey;
    }
    
    @XmlTransient
    public OCTET getCodiNetworkKey()
    {
        return this.codiNetworkKey;
    } 
    
    public void setCodiNetworkKey(OCTET codiNetworkKey)
    {
        this.codiNetworkKey = codiNetworkKey;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

		sb.append("CLASS["+this.getClass().getName()+"]\n");
        sb.append("codiMask: " + codiMask + "\n");
        sb.append("codiIndex: " + codiIndex + "\n");
        sb.append("codiID: " + codiID + "\n");
        sb.append("codiType: " + codiType + "\n");
        sb.append("codiShortID: " + codiShortID + "\n");
        sb.append("codiFwVer: " + codiFwVer + "\n");
        sb.append("codiHwVer: " + codiHwVer + "\n");
        sb.append("codiZAIfVer: " + codiZAIfVer + "\n");
        sb.append("codiZZIfVer: " + codiZZIfVer + "\n");
        sb.append("codiFwBuild: " + codiFwBuild + "\n");
        sb.append("codiResetKind: " + codiResetKind + "\n");
        sb.append("codiAutoSetting: " + codiAutoSetting + "\n");
        sb.append("codiChannel: " + codiChannel + "\n");
        sb.append("codiPanID: " + codiPanID + "\n");
        sb.append("codiExtPanID: " + codiExtPanID + "\n");
        sb.append("codiRfPower: " + codiRfPower + "\n");
        sb.append("codiTxPowerMode: " + codiTxPowerMode + "\n");
        sb.append("codiPermit: " + codiPermit + "\n");
        sb.append("codiEnableEncrypt: " + codiEnableEncrypt + "\n");
        sb.append("codiLineKey: " + codiLineKey + "\n");
        sb.append("codiNetworkKey: " + codiNetworkKey + "\n");

        return sb.toString();
    }
}
