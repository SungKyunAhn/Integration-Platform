
package com.aimir.fep.command.ws.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for cmdWriteTable complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="cmdWriteTable">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="McuId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="MeterSerialNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TableNo" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
@XmlType(name = "cmdWriteTable", propOrder = {
    "mcuId",
    "meterSerialNo",
    "tableNo",
    "dataStream"
})
public class CmdWriteTable {

    @XmlElement(name = "McuId")
    protected String mcuId;
    @XmlElement(name = "MeterSerialNo")
    protected String meterSerialNo;
    @XmlElement(name = "TableNo")
    protected int tableNo;
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
     * Gets the value of the meterSerialNo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMeterSerialNo() {
        return meterSerialNo;
    }

    /**
     * Sets the value of the meterSerialNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMeterSerialNo(String value) {
        this.meterSerialNo = value;
    }

    /**
     * Gets the value of the tableNo property.
     * 
     */
    public int getTableNo() {
        return tableNo;
    }

    /**
     * Sets the value of the tableNo property.
     * 
     */
    public void setTableNo(int value) {
        this.tableNo = value;
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
