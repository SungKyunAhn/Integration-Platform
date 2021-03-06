
package com.aimir.fep.command.ws.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for cmdBypassSensor1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="cmdBypassSensor1">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="McuId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ModemId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="IsLinkSkip" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="DataStream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cmdBypassSensor1", propOrder = {
    "mcuId",
    "modemId",
    "isLinkSkip",
    "dataStream"
})
public class CmdBypassSensor1 {

    @XmlElement(name = "McuId")
    protected String mcuId;
    @XmlElement(name = "ModemId")
    protected String modemId;
    @XmlElement(name = "IsLinkSkip")
    protected boolean isLinkSkip;
    @XmlElement(name = "DataStream")
    protected byte[] dataStream;

    /**
     * Gets the value of the mcuId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMcuId() {
        return mcuId;
    }

    /**
     * Sets the value of the mcuId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMcuId(String value) {
        this.mcuId = value;
    }

    /**
     * Gets the value of the modemId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModemId() {
        return modemId;
    }

    /**
     * Sets the value of the modemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModemId(String value) {
        this.modemId = value;
    }

    /**
     * Gets the value of the isLinkSkip property.
     * 
     */
    public boolean isIsLinkSkip() {
        return isLinkSkip;
    }

    /**
     * Sets the value of the isLinkSkip property.
     * 
     */
    public void setIsLinkSkip(boolean value) {
        this.isLinkSkip = value;
    }

    /**
     * Gets the value of the dataStream property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getDataStream() {
        return dataStream;
    }

    /**
     * Sets the value of the dataStream property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setDataStream(byte[] value) {
        this.dataStream = value;
    }

}
