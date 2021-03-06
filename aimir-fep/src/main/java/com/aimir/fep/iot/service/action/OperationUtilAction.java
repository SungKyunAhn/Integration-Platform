package com.aimir.fep.iot.service.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.aimir.fep.iot.domain.common.RSCException;
import com.aimir.fep.iot.domain.resources.AE;
import com.aimir.fep.iot.domain.resources.Battery;
import com.aimir.fep.iot.domain.resources.CSEBase;
import com.aimir.fep.iot.domain.resources.Container;
import com.aimir.fep.iot.domain.resources.ContentInstance;
import com.aimir.fep.iot.domain.resources.DeviceInfo;
import com.aimir.fep.iot.domain.resources.Firmware;
import com.aimir.fep.iot.domain.resources.Group;
import com.aimir.fep.iot.domain.resources.LocationPolicy;
import com.aimir.fep.iot.domain.resources.Memory;
import com.aimir.fep.iot.domain.resources.MgmtCmd;
import com.aimir.fep.iot.domain.resources.MgmtResource;
import com.aimir.fep.iot.domain.resources.Reboot;
import com.aimir.fep.iot.domain.resources.RemoteCSE;
import com.aimir.fep.iot.domain.resources.Resource;
import com.aimir.fep.iot.domain.resources.Software;
import com.aimir.fep.iot.domain.resources.Subscription;
import com.aimir.fep.iot.domain.resources.URIList;
import com.aimir.fep.iot.service.snowflake.BasicEntityIdGenerator;
import com.aimir.fep.iot.utils.CommonCode.FILTER_USAGE;
import com.aimir.fep.iot.utils.CommonCode.MGMT_DEFINITION;
import com.aimir.fep.iot.utils.CommonCode.RESOURCE_TYPE;
import com.aimir.fep.iot.utils.CommonCode.RESULT_CONTENT;
import com.aimir.fep.iot.utils.CommonCode.RSC;
import com.aimir.fep.iot.utils.CommonUtil;
import com.aimir.fep.iot.utils.MediaTypeOneM2M;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

@Service
@Scope("prototype")
public class OperationUtilAction {

	private static Log logger = LogFactory.getLog(OperationUtilAction.class);
	
	static final JAXBContext context = initContext();    
    private static JAXBContext initContext() {     
    	JAXBContext jAXBContext = null;
        try {
        	jAXBContext = JAXBContext.newInstance(CSEBase.class,AE.class,Battery.class,Container.class,ContentInstance.class,DeviceInfo.class,Firmware.class,Group.class,LocationPolicy.class,Memory.class,MgmtCmd.class,Reboot.class,RemoteCSE.class,Software.class,Subscription.class,URIList.class);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return jAXBContext;
    }
	
	public Object getBodyObject(String seq, HttpServletRequest request, Class cls) {
		Object profile = null;
        String body = null;
        try {
        	
			body = getBody(request);
			logger.debug("### ["+seq+"] Request Body [ "+body+" ] ###");
			
	        if (isContentTypeByXml(request)) 
	        	profile = unMarshalFromXmlString(body);
	        else
	        	profile = unMarshalFromJsonString(cls, body);
	        		        
		} catch (RSCException e) {
			e.printStackTrace();
		}
        
        return profile;
	}
	
	/**
	 * unMarshalFromJsonString
	 * @param cls
	 * @param in
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Object unMarshalFromJsonString(Class cls, String in){
		if (CommonUtil.isEmpty(in)) return null;
		
		Object obj = null;
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			//mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
			mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE,true);		
			JaxbAnnotationModule module = new JaxbAnnotationModule();
			mapper.registerModule(module);
			mapper.setSerializationInclusion(Include.NON_NULL);
			obj = mapper.readValue(in, cls);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return obj;
	}
	
	/**
	 * unMarshalFromXmlString
	 * @param in
	 * @return
	 */
	public Object unMarshalFromXmlString(String in){
		if (CommonUtil.isEmpty(in)) 
			return null;
		
		Object obj = null;
		StringReader sr = new StringReader(in);
		
		try {
			Unmarshaller msh =  context.createUnmarshaller();
			obj = msh.unmarshal(sr);
			
		} catch (Exception e) {
			return null;
		}
		
		return obj;
	}
	
    /**
     * isContentTypeByXml
     * @param request
     * @return
     */
    public boolean isContentTypeByXml(HttpServletRequest request) {
   	 
        boolean isXml = false;
        String contentType 	= CommonUtil.nvl(request.getHeader("Content-Type"), "");
        
        if (contentType.contains("xml")) isXml = true;
        
        return isXml;
    }
    
	
	/**
     * getBody
     * @param request
     * @return
     * @throws RSCException
     */
    public String getBody(HttpServletRequest request) throws RSCException {
    	 
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
 
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
        	throw new RSCException(RSC.BAD_REQUEST, ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                	throw new RSCException(RSC.BAD_REQUEST, ex.getMessage());
                }
            }
        }
 
        body = stringBuilder.toString();
        return body;
    }
    
    public Response setResponse(HttpServletRequest request, Status status, String resMessage, RSC resCode, Resource responseObj) {
		int iContentLength 		= 0;
		String contentLocation 	= null;
		String contentLength	= null;
		String accept 			= CommonUtil.nvl(request.getHeader("Accept"), "");
		String xM2MRI 			= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		
		BigInteger filterUsage	= new BigInteger(CommonUtil.nvl(request.getParameter("fu"), "-1"));
		BigInteger resultContent= new BigInteger(CommonUtil.nvl(request.getParameter("rcn"), "-1"));
		String dKey				= null;
		String aKey				= null;
		ResponseBuilder responseBuilder = Response.status(status);
		
		if(responseObj != null) {
			switch(request.getMethod()) {
			case "POST":
				String url = CommonUtil.getURL(request);
				contentLocation = getContentLocation(url, responseObj);
				
				if (!CommonUtil.isEmpty(responseObj)) {
					RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(((Resource)responseObj).getResourceType());
					switch(resourceType) {
					case REMOTE_CSE:
						dKey = ((RemoteCSE)responseObj).getDKey();
						break;
					case AE:
						aKey = ((AE)responseObj).getAKey();
						break;
					default:
						break;
					}
				}
				break;
			default:
				break;
			} 
		}
		/*
		if (!CommonUtil.isEmpty(resMessage)) 
			iContentLength = resMessage.getBytes("UTF-8").length;
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(resultContent)) 
			contentLength = Integer.toString(0);
		else 
			contentLength = Integer.toString(iContentLength > 0 ? iContentLength : 0);
		*/
		
		if (FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterUsage)
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(resultContent)
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(resultContent)
				 || RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(resultContent)
				 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(resultContent)) {
			if (accept.contains("xml")) 
				responseBuilder.type(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_PRSP_XML);
			else 						
				responseBuilder.type(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_PRSP_JSON);
		} else {
			if (accept.contains("xml")) 
				responseBuilder.type(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_RES_XML);
			else 						
				responseBuilder.type(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_RES_JSON);
		}
		
		if (!CommonUtil.isEmpty(xM2MRI)) 			responseBuilder.header("X-M2M-RI", xM2MRI);
		if (!CommonUtil.isEmpty(resCode)) 			responseBuilder.header("X-M2M-RSC", resCode.getValue());
		//if (!CommonUtil.isEmpty(rsm)) 				responseHeaders.set("X-M2M-RSM", rsm);
		if (!CommonUtil.isEmpty(contentLocation)) 	responseBuilder.header("Content-Location", contentLocation);
		//if (!CommonUtil.isEmpty(contentLength)) 	responseBuilder.setContentLength(Long.parseLong(contentLength));
		if (!CommonUtil.isEmpty(dKey)) 				responseBuilder.header("dKey", dKey);
		if (!CommonUtil.isEmpty(aKey)) 				responseBuilder.header("aKey", aKey);
		
		
		responseBuilder.entity(resMessage);
		return responseBuilder.build();
	}
   
    public String getObject2Txt(HttpServletRequest request, Object obj) {
    	
        String txt = null;
        
        if(request == null) {
        	return marshalToXmlString(obj);
        }
        
        if (isAcceptByXml(request)) 
        	txt = marshalToXmlString(obj);
        else 
        	txt = marshalToJsonString(obj);
        
        return txt;
    }
    
    public boolean isAcceptByXml(HttpServletRequest request) {
      	 
        boolean isXml = false;
        String accept 	= CommonUtil.nvl(request.getHeader("Accept"), "");
        String contentType = CommonUtil.nvl(request.getHeader("Content-Type"), "");
                
        if (accept.contains("xml") || contentType.contains("xml")) {
        	isXml = true;
        }
        
        return isXml;
    }
    
	public String marshalToXmlString(Object obj){
		if (CommonUtil.isEmpty(obj)) return null;
		
		StringWriter sw = new StringWriter();
		
		try {
			Marshaller msh = context.createMarshaller();
			msh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			msh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
			msh.setProperty(Marshaller.JAXB_FRAGMENT, false);
			msh.marshal(obj, sw);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return sw.toString();
	}
	
	public String marshalToJsonString(Object obj){
		if (CommonUtil.isEmpty(obj)) return null;
		
		String jsonStr = null;
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true); 
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		try {
			jsonStr = mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return jsonStr;
	}
	
	public String getContentLocation(String url, Object obj) {
		String contentLocation = null;
		
		if (!CommonUtil.isEmpty(obj)) {
			
			RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(((Resource)obj).getResourceType());
			String resourceName = ((Resource)obj).getResourceName();
			
			switch(resourceType) {
			case REMOTE_CSE:
				contentLocation = url + "/" + RESOURCE_TYPE.REMOTE_CSE.getName() + "-" + resourceName;
				break;
			case AE:
				contentLocation = url + "/" + RESOURCE_TYPE.AE.getName() + "-" + resourceName;
				break;
			case CONTAINER:
				contentLocation = url + "/" + RESOURCE_TYPE.CONTAINER.getName() + "-" + resourceName;
				break;
			case CONTENT_INSTANCE:
				contentLocation = url + "/" + RESOURCE_TYPE.CONTENT_INSTANCE.getName() + "-" + resourceName;
				break;
			case SUBSCRIPTION:
				contentLocation = url + "/" + RESOURCE_TYPE.SUBSCRIPTION.getName() + "-" + resourceName;
				break;
			case LOCATION_POLICY:
				contentLocation = url + "/" + RESOURCE_TYPE.LOCATION_POLICY.getName() + "-" + resourceName;
				break;
			case GROUP:
				contentLocation = url + "/" + RESOURCE_TYPE.REAL_GROUP.getName() + "-" + resourceName;
				break;
			case MGMT_CMD:
				contentLocation = url + "/" + RESOURCE_TYPE.MGMT_CMD.getName() + "-" + resourceName;
				break;
			case EXEC_INSTANCE:
				contentLocation = url + "/" + RESOURCE_TYPE.EXEC_INSTANCE.getName() + "-" + resourceName;
				break;
			case NODE:
				contentLocation = url + "/" + RESOURCE_TYPE.NODE.getName() + "-" + resourceName;
				break;
			case MGMT_OBJ:
				
				try {
					MgmtResource mgmtResource = (MgmtResource)obj;
					
					if (!CommonUtil.isEmpty(mgmtResource.getMgmtDefinition())) {
						MGMT_DEFINITION mgmtDefinition = MGMT_DEFINITION.getThis(mgmtResource.getMgmtDefinition());
						
						switch(mgmtDefinition) {
						case FIRMWARE:
							contentLocation = url + "/" + MGMT_DEFINITION.FIRMWARE.getName() + "-" + resourceName;
							break;
						case SOFTWARE:
							contentLocation = url + "/" + MGMT_DEFINITION.SOFTWARE.getName() + "-" + resourceName;
							break;
						case MEMORY:
							contentLocation = url + "/" + MGMT_DEFINITION.MEMORY.getName() + "-" + resourceName;
							break;
						case BATTERY:
							contentLocation = url + "/" + MGMT_DEFINITION.BATTERY.getName() + "-" + resourceName;
							break;
						case DEVICE_INFO:
							contentLocation = url + "/" + MGMT_DEFINITION.DEVICE_INFO.getName() + "-" + resourceName;
							break;
						case REBOOT:
							contentLocation = url + "/" + MGMT_DEFINITION.REBOOT.getName() + "-" + resourceName;
							break;
						default:
							break;
						}
					}
				} catch (Exception e) {
				}
				
				break;
			default:
				break;
			}
		}
		
		return contentLocation;
	}
	
    public String createDeviceKey(String resoureId) throws Exception {
    	return BasicEntityIdGenerator.getInstance().generateLongId();    	
    }
    
    public String createPasscode(String target) throws Exception  {
		MessageDigest sh = MessageDigest.getInstance("SHA-256"); 
		sh.update(target.getBytes());
		byte byteData[] = sh.digest();
		StringBuffer sb = new StringBuffer(); 
		for(int i = 0 ; i < byteData.length ; i++){
			sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
    }
}
