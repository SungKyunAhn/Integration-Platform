package com.aimir.fep.iot.service.action;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.aimir.fep.iot.domain.resources.CSEBase;
import com.aimir.fep.iot.domain.resources.RemoteCSE;
import com.aimir.fep.iot.request.ReqRemoteCse;
import com.aimir.fep.iot.request.Request;
import com.aimir.fep.iot.service.CSEBaseService;
import com.aimir.fep.iot.service.NodeService;
import com.aimir.fep.iot.service.RemoteCseService;
import com.aimir.fep.iot.utils.CommonCode.RSC;
import com.aimir.fep.iot.utils.CommonUtil;
import com.aimir.fep.iot.utils.MessageProperties;
import com.aimir.fep.protocol.fmp.processor.ProcessorHandler;

/* 
 * mobius 1.0과 oasis에서 remotecse의 저장 방식 및 query 구조가 다르다.
 * oasis의 샘플코드에는 csr에 '/' 항목이 추가되어 있지만 mobius에서는 '/' 문자가 틀어가면 find가 되지 않는다.
 * 현재 2017.10.13은 호환성이 고려되지 않았으며 mobius 1.0기반으로 작성되어 있다. 
 */	

@Service
@Scope("prototype")
public class RemoteCSEAction implements ActionService {

	private static Log logger = LogFactory.getLog(RemoteCSEAction.class);

	@Autowired
	CSEBaseService cseBaseService;
	
	@Autowired
	RemoteCseService remoteCseService;

	@Autowired
	NodeService nodeService;
	
	@Autowired
	ProcessorHandler processorHandler;
	
	@Override
	public Response create(Request req) throws Exception {
		RemoteCSE reqRemoteCSE = null;
		String message = null;
		
		OperationUtilAction operUtil = req.getOperUtil();
		HttpServletRequest request = req.getRequest();
		String seq = req.getSeq();
		String rootCseBase = ((ReqRemoteCse)req).getCseBase();
		
		logger.debug("### ["+seq+"] RemoteCSE Create start! ###");
		
		reqRemoteCSE = (RemoteCSE)operUtil.getBodyObject(seq, request, RemoteCSE.class);
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String passCode	= CommonUtil.nvl(request.getHeader("passCode"), "");
		
		//boolean checkMccP = mCommonService.getCheckMccP(from);
		//boolean isError = false;
		
		//1. 입력 파라미터 유효성 체크
		if(CommonUtil.isEmpty(from)) {
			message = MessageProperties.getProperty("aimir.iot.origin.empty");
		} else if(CommonUtil.isEmpty(requestID)) {
			message = MessageProperties.getProperty("aimir.iot.ri.empty");
		} else if(CommonUtil.isEmpty(passCode)) {
			message = MessageProperties.getProperty("aimir.iot.passcode.empty");
		} else if(CommonUtil.isEmpty(rootCseBase)) {
			message = MessageProperties.getProperty("aimir.iot.cseId.empty");
		} else if(CommonUtil.isEmpty(reqRemoteCSE)) {
			message = MessageProperties.getProperty("aimir.iot.csr.empty");
		} else if(CommonUtil.isEmpty(reqRemoteCSE.getCSEID())) {
			message = MessageProperties.getProperty("aimir.iot.csrId.empty");
		} else if(CommonUtil.isEmpty(reqRemoteCSE.isRequestReachability())) {
			message = MessageProperties.getProperty("aimir.iot.rr.empty");
		} else if(!CommonUtil.getExpirationTimeValidation(reqRemoteCSE.getExpirationTime())) {
			message = MessageProperties.getProperty("aimir.iot.expirationTime.fail");
		}
		
		if(message != null) {
			logger.debug("["+seq+"] requeset parameter error!");
			return operUtil.setResponse(request, Response.Status.BAD_REQUEST, message, RSC.BAD_REQUEST, null);
		}
		
		//2.cseBase DB 유효성 체크
		CSEBase cseBase = cseBaseService.findOneCSEBaseByCSEID(rootCseBase);
		if(CommonUtil.isEmpty(cseBase)) {
			message = rootCseBase +" "+MessageProperties.getProperty("aimir.iot.cse.empty");
			logger.debug("["+seq+"] CSEBase emtpy!");
			return operUtil.setResponse(request, Response.Status.NOT_FOUND, message, RSC.NOT_FOUND, null);
		}
		
		//3.remoteCSE 유효성 체크
		RemoteCSE remoteCse = remoteCseService.findOneRemoteCSEByResourceName(cseBase.getResourceID(), reqRemoteCSE.getCSEID());
		if(!CommonUtil.isEmpty(remoteCse)) {
			//등록된 디바이스 등록
			logger.debug("["+seq+"] device already register!");
			processorHandler.putServiceData(ProcessorHandler.SERVICE_IOT_MDDATA, reqRemoteCSE);
			return operUtil.setResponse(request, Response.Status.FORBIDDEN, operUtil.getObject2Txt(request, remoteCse), RSC.ALREADY_EXISTS, remoteCse);
		}
		
		//4.디바이스 등록
		String dkey = operUtil.createDeviceKey(reqRemoteCSE.getResourceID());
		String url = CommonUtil.getURL(request);
		
		reqRemoteCSE.setResourceName(reqRemoteCSE.getCSEID());
		reqRemoteCSE.setCSEID(reqRemoteCSE.getCSEID());
		reqRemoteCSE.setParentID(cseBase.getResourceID());
		reqRemoteCSE.setCSEBase(cseBase.getResourceID());
		reqRemoteCSE.setPassCode(operUtil.createPasscode(passCode));
		reqRemoteCSE.setMappingYn("N");
		reqRemoteCSE.setDKey(dkey);

		//remotecse crate;
		reqRemoteCSE = remoteCseService.insert(seq, reqRemoteCSE);
		
		//acp create;
		
		//node create;
		nodeService.insert(seq, reqRemoteCSE);
		
		reqRemoteCSE.setSeq(seq);
		reqRemoteCSE.setParentID(from);
		
		//MDMS log create		
		processorHandler.putServiceIoTData(ProcessorHandler.SERVICE_H_MDMSData, reqRemoteCSE);
		
		//FepD send Message
		processorHandler.putServiceIoTData(ProcessorHandler.SERVICE_IOT_MDDATA, reqRemoteCSE);
		
		logger.info("### ["+seq+"] remotecse register success! ###");
		return operUtil.setResponse(request, Response.Status.CREATED, operUtil.getObject2Txt(request, reqRemoteCSE), RSC.CREATED, reqRemoteCSE);
	}

	@Override
	public Response retrive(Request req) throws Exception {
		String message = null;
		OperationUtilAction operUtil = req.getOperUtil();
		HttpServletRequest request = req.getRequest();
		
		String seq = req.getSeq();
		String cseBaseId = ((ReqRemoteCse)req).getCseBase();
		String remoteCSEName = ((ReqRemoteCse)req).getRemoteCseName();
		
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
		
		logger.info("["+seq+"] remotecse retrive success!");
		return operUtil.setResponse(request, Response.Status.OK, operUtil.getObject2Txt(request, remoteCSE), RSC.OK, remoteCSE);
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
