package com.aimir.fep.command.ws.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for CmdGetControlLog complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="cmdGetControlLog">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;element name="ModemId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;element name="StartDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;element name="FromDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cmdGetControlLog", propOrder = { "modemId", "fromDate", "toDate"})
public class CmdGetControlLog {
	@XmlElement(name = "ModemId")
	protected String modemId;

	@XmlElement(name = "FromDate")
	protected String fromDate;
	
	@XmlElement(name = "ToDate")
	protected String toDate;
	
	/**
	 * Gets StandardEventLog
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getModemId() {
		return modemId;
	}

	/**
	 * Gets the value of the fromDate property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getFromDate() {
		return fromDate;
	}

	/**
	 * Gets the value of the fromDate property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getToDate() {
		return toDate;
	}

}
