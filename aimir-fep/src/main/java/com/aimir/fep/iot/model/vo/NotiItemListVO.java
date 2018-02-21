package com.aimir.fep.iot.model.vo;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"contentId",
		"date",
		"message",
		"type"
})

@XmlRootElement(name = "item", namespace="http://www.onem2m.org/xml/protocols")
public class NotiItemListVO {
	
	@XmlElement(name = "contentId", required = true)
	protected String contentId;
	
	@XmlElement(name = "date", required = true)
	protected  String  date;

	@XmlElement(name = "message", required = true)
	protected String message;
	
	@XmlElement(name = "type", required = true)
	protected String type;

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	
	
}
