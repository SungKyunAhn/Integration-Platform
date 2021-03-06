package com.aimir.fep.iot.service.action;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import com.aimir.fep.iot.domain.resources.CSEBase;
import com.aimir.fep.iot.domain.resources.Container;
import com.aimir.fep.iot.domain.resources.ContentInstance;
import com.aimir.fep.iot.domain.resources.RemoteCSE;
import com.aimir.fep.iot.request.ReqContentInstance;
import com.aimir.fep.iot.request.Request;
import com.aimir.fep.iot.service.CSEBaseService;
import com.aimir.fep.iot.service.ContainerService;
import com.aimir.fep.iot.service.ContentInstanceService;
import com.aimir.fep.iot.service.RemoteCseService;
import com.aimir.fep.iot.utils.CommonCode.RSC;
import com.aimir.fep.protocol.fmp.processor.ProcessorHandler;
import com.aimir.fep.iot.utils.CommonUtil;
import com.aimir.fep.iot.utils.MessageProperties;

@Service
@EnableAsync
@Scope("prototype")
public class ContentInstanceAction implements ActionService {

	private static Log logger = LogFactory.getLog(ContentInstanceAction.class);
	
	public static String LATEST_LABELTAG = "latest";
	public static String OLDEST_LABELTAG = "oldest";
	
	@Autowired
	CSEBaseService cseBaseService;
	
	@Autowired
	RemoteCseService remoteCseService;
	
	@Autowired
	ContainerService containerService;
	
	@Autowired
	ContentInstanceService contentInstanceService;
	
	@Autowired
	ProcessorHandler processorHandler;
	
	@Override
	public Response create(Request req) throws Exception {
		ContentInstance reqConteentInstance = null;
		String message = null;
		
		OperationUtilAction operUtil = req.getOperUtil();
		HttpServletRequest request = req.getRequest();
		String seq = req.getSeq();
		
		String cseBaseId = ((ReqContentInstance)req).getCseBase();
		String remoteCSEName = ((ReqContentInstance)req).getRemoteCSEName();
		String containerName = ((ReqContentInstance)req).getContainerName();
		
		logger.debug("### ["+seq+"] ContentInstance Create start! ###");
		reqConteentInstance = (ContentInstance)operUtil.getBodyObject(seq, request, ContentInstance.class);
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String passCode	= CommonUtil.nvl(request.getHeader("passCode"), "");
		
		if(CommonUtil.isEmpty(from)) {
			message = MessageProperties.getProperty("aimir.iot.origin.empty");
		} else if(CommonUtil.isEmpty(requestID)) {
			message = MessageProperties.getProperty("aimir.iot.ri.empty");
		} else if(CommonUtil.isEmpty(cseBaseId)) {
			message = MessageProperties.getProperty("aimir.iot.cseId.empty");
		} else if (CommonUtil.isEmpty(remoteCSEName)) {
			message = MessageProperties.getProperty("aimir.iot.csr.name.empty");
		} else if (CommonUtil.isEmpty(containerName)) {
			message = MessageProperties.getProperty("aimir.iot.container.empty");
		} else if (CommonUtil.isEmpty(reqConteentInstance)) {
			message = MessageProperties.getProperty("aimir.iot.contentInstance.empty");
		} else if(CommonUtil.isEmpty(reqConteentInstance.getContent())){
			message = MessageProperties.getProperty("aimir.iot.contentInstance.content.empty");
		} else if(!CommonUtil.getExpirationTimeValidation(reqConteentInstance.getExpirationTime())) {
			message = MessageProperties.getProperty("aimir.iot.expirationTime.fail");
		}
		
		if(message != null) {
			logger.debug("["+seq+"] requeset parameter error!");
			return operUtil.setResponse(request, Response.Status.BAD_REQUEST, message, RSC.BAD_REQUEST, null);
		} 
		
		//2. 유효성 체크
		//2-1 cseBase Resource 유효성 체크
		CSEBase cseBase = cseBaseService.findOneCSEBaseByCSEID(cseBaseId);
		if(CommonUtil.isEmpty(cseBase)) {
			logger.debug("["+seq+"] CSEBase emtpy!");
			message = cseBaseId + " " + MessageProperties.getProperty("aimir.iot.cse.empty");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		//2-2 remoteCSE Resource 유효성 체크
		RemoteCSE remoteCSE = remoteCseService.findOneRemoteCSEByResoureName(remoteCSEName);
		if(CommonUtil.isEmpty(remoteCSE)) {
			logger.debug("["+seq+"] RemoteCSE emtpy!");
			message = cseBaseId+ " " + MessageProperties.getProperty("aimir.iot.csr.empty");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		//2-3 container Resource 유효성 체크
		/* 2017.10.26
		 * 현재 사회안전망 nCube는 Container가 동작하지 않으며, 은미애 부장님에게 문의 결과 데이터 요청과 검증을 줄이기 위해 그랬다는 답변
		 * 사회안전망에서는 Gateway을 Dcu로 정의하며, 연산과 조건을 줄이기 위해 변하지 않는 데이터의 항목을 줄인것으로 판단됨
		 */
//		Container container = containerService.findOneContainerByResourceName(remoteCSE.getResourceID(), containerName);
//		if(CommonUtil.isEmpty(container)) {
//			logger.debug("["+seq+"] Container emtpy!");
//			message = name + MessageProperties.getProperty("aimir.iot.container.empty");
//			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
//		}
				
//		reqConteentInstance.setResourceName(name);
		contentInstanceService.insert(seq, remoteCSE, reqConteentInstance);
		logger.info("### ["+seq+"] device contentInstance register success! ###");
				
		//MDMS log create		
		reqConteentInstance.setSeq(seq);
		reqConteentInstance.setParentID(from);
		reqConteentInstance.setResourceName(containerName);//실제 게이트웨이 데이터와 안맞아 수정 (by ask)
		processorHandler.putServiceIoTData(ProcessorHandler.SERVICE_H_MDMSData, reqConteentInstance);
		
		//FepD send Message
		processorHandler.putServiceIoTData(ProcessorHandler.SERVICE_IOT_MDDATA, reqConteentInstance);
		
		return operUtil.setResponse(request, Response.Status.CREATED, operUtil.getObject2Txt(request, reqConteentInstance), RSC.CREATED, reqConteentInstance);
	}

	@Override
	public Response retrive(Request req) throws Exception {
		ContentInstance contentInstance = null;
		String message = null;
		
		OperationUtilAction operUtil = req.getOperUtil();
		HttpServletRequest request = req.getRequest();
		String seq = req.getSeq();
		String cseBaseId = ((ReqContentInstance)req).getCseBase();
		String remoteCSEName = ((ReqContentInstance)req).getRemoteCSEName();
		String containerName = ((ReqContentInstance)req).getContainerName();
		String contentInstanceName = ((ReqContentInstance)req).getContentInstanceName();
		String labelTag = ((ReqContentInstance)req).getLabelTag();
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		
		if(CommonUtil.isEmpty(from)) {
			message = MessageProperties.getProperty("aimir.iot.origin.empty");
		} else if(CommonUtil.isEmpty(requestID)) {
			message = MessageProperties.getProperty("aimir.iot.ri.empty");
		} else if(CommonUtil.isEmpty(cseBaseId)) {
			message = MessageProperties.getProperty("aimir.iot.cseId.empty");
		} else if(CommonUtil.isEmpty(remoteCSEName)) {
			message = MessageProperties.getProperty("aimir.iot.csr.name.empty");
		} else if(CommonUtil.isEmpty(containerName)) {
			message = MessageProperties.getProperty("aimir.iot.container.name.empty");
		} else if(CommonUtil.isEmpty(contentInstanceName) && CommonUtil.isEmpty(labelTag)) {
			message = MessageProperties.getProperty("aimir.iot.contentInstance.name.empty");
		}
		
		if(message != null) {
			logger.debug("["+seq+"] requeset parameter error!");
			return operUtil.setResponse(request, Response.Status.BAD_REQUEST, message, RSC.BAD_REQUEST, null);
		}
		
		//cseBase 유효성 체크
		CSEBase cseBase = cseBaseService.findOneCSEBaseByCSEID(cseBaseId);
		if(CommonUtil.isEmpty(cseBase)) {
			logger.debug("["+seq+"] CSEBase emtpy!");
			message = MessageProperties.getProperty("aimir.iot.cseId.empty");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		//remoteCSE 유효성 체크
		RemoteCSE remoteCSE = remoteCseService.findOneRemoteCSEByResoureName(remoteCSEName);
		if(CommonUtil.isEmpty(remoteCSE)) {
			logger.debug("["+seq+"] RemoteCSE emtpy!");
			message = MessageProperties.getProperty("aimir.iot.csr.empty");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		//container 유효성 체크
		Container container = containerService.findOneContainerByResourceName(remoteCSE.getResourceID(), containerName);
		if(CommonUtil.isEmpty(container)) {
			logger.debug("["+seq+"] Container emtpy!");
			message = MessageProperties.getProperty("aimir.iot.container.empty");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		if(CommonUtil.isEmpty(labelTag)) {
			//contentInstance 유효성 체크
			contentInstance = contentInstanceService.findOneContentInstanceByResourceName(seq, container.getResourceID(), contentInstanceName);
		} else if(LATEST_LABELTAG.equalsIgnoreCase(labelTag)) {
			contentInstance = contentInstanceService.findOneContentInstanceAtLabelTag(seq, container.getResourceID(), LATEST_LABELTAG);
		} else if(OLDEST_LABELTAG.equalsIgnoreCase(labelTag)) {
			contentInstance = contentInstanceService.findOneContentInstanceAtLabelTag(seq, container.getResourceID(), OLDEST_LABELTAG);
		} else {
			logger.debug("["+seq+"] ContentInstance get emtpy!");
			message = MessageProperties.getProperty("aimir.iot.contentInstance.labelTag.fail");
			return operUtil.setResponse(request, Response.Status.BAD_REQUEST, message, RSC.BAD_REQUEST, null);
		}
		
		if(CommonUtil.isEmpty(contentInstance)) {
			logger.debug("["+seq+"] ContentInstance get emtpy!");
			message = MessageProperties.getProperty("aimir.iot.contentInstance.empty");
			return operUtil.setResponse(request, Response.Status.BAD_REQUEST, message, RSC.BAD_REQUEST, null);
		}
		
		logger.info("["+seq+"] contentInstance retrive success!");
		return operUtil.setResponse(request, Response.Status.OK, operUtil.getObject2Txt(request, contentInstance), RSC.OK, contentInstance);
	}

	@Override
	public Response update(Request req) throws Exception {
		return null;
	}

	@Override
	public Response delete(Request req) throws Exception {
		return null;
	}

}
