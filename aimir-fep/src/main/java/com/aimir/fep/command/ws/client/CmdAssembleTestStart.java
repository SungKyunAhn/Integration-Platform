
package com.aimir.fep.command.ws.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for cmdAssembleTestStart complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="cmdAssembleTestStart">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="McuId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cmdAssembleTestStart", propOrder = {
	    "mcuId"
})
public class CmdAssembleTestStart {

    @XmlElement(name = "McuId")
    protected String mcuId;

    
	public String getMcuId() {
		return mcuId;
	}

	public void setMcuId(String mcuId) {
		this.mcuId = mcuId;
	}
}
