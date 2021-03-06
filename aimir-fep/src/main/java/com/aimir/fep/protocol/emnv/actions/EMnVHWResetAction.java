/**
 * (@)# EMnVHWResetAction.java
 *
 * 2015. 7. 28.
 *
 * Copyright (c) 2013 NURITELECOM, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of 
 * NURITELECOM, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with NURITELECOM, Inc.
 *
 * For more information on this product, please see
 * http://www.nuritelecom.co.kr
 *
 */
package com.aimir.fep.protocol.emnv.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aimir.constants.CommonConstants.TR_STATE;
import com.aimir.dao.device.AsyncCommandLogDao;
import com.aimir.dao.device.AsyncCommandParamDao;
import com.aimir.fep.protocol.emnv.frame.EMnVConstants.EMnVCommandSubFrameType;
import com.aimir.fep.protocol.emnv.frame.EMnVGeneralDataFrame;
import com.aimir.fep.protocol.emnv.frame.payload.EMnVCommandFramePayLoad;
import com.aimir.fep.protocol.emnv.frame.payload.EMnVGeneralFramePayLoad;
import com.aimir.fep.util.DataUtil;
import com.aimir.model.device.AsyncCommandLog;
import com.aimir.model.device.AsyncCommandParam;
import com.aimir.util.Condition;
import com.aimir.util.Condition.Restriction;

/**
 * @author nuri
 *
 */
public class EMnVHWResetAction extends EMnVActions {
	private static Logger logger = LoggerFactory.getLogger(EMnVHWResetAction.class);
	private EMnVCommandSubFrameType commandType;
	private RWSType rwsType;
	private ActionCommandType actionCommandType = ActionCommandType.HW_RESET;

	public EMnVHWResetAction(EMnVCommandSubFrameType commandType) {
		this.commandType = commandType;
	}
	
	@Override
	public void execute(EMnVGeneralFramePayLoad gFramePayload, IoSession session, EMnVGeneralDataFrame generalDataFrame) throws Exception {
		ActionDevice aDevice = null;
		AsyncCommandLogDao acld = null;
		Set<Condition> condition = null;
		EMnVCommandFramePayLoad payLoad = null;
		
		if(commandType == null){
			throw new Exception("Commnad Type is null.");
		}

		switch (commandType) {
		case COMMAND_REQUEST:     
			aDevice = (ActionDevice)session.getAttribute(session.getRemoteAddress());
			/** KEMCO에서는 아래와 같은 의미로 사용한다.
			 * TR_STATE.Success   (0) : Command 수행 성공및 정상종료 상태
			 * TR_STATE.Waiting   (1) : 서버측에서 SMS전송후 모뎀의 접속을 기다리는 상태
			 * TR_STATE.Running   (2) : Command 수행중인 상태
			 * TR_STATE.Terminate (4) : Command 가 성공하지 못한 상태에서 종료된 상태 (ex. crc error)
			 * TR_STATE.Delete    (8) : 다른 Tranasction의 동일 Command가 실행되어(단지 실행) 삭제처리된 상태
			 * TR_STATE.Unknown (255) : 아몰랑.
			 */
			// 비동기 내역 조회 : 기존에 처리되지 않았던 내역이 있으면 한꺼번에 삭제처리
            acld = DataUtil.getBean(AsyncCommandLogDao.class);
            condition = new HashSet<Condition>();
            condition.add(new Condition("deviceId", new Object[]{aDevice.getModemId()}, null, Restriction.EQ));
            condition.add(new Condition("command", new Object[]{aDevice.getCommand()}, null, Restriction.EQ));
            condition.add(new Condition("state", new Object[]{TR_STATE.Waiting.getCode(), TR_STATE.Running.getCode()}, null, Restriction.IN));
            condition.add(new Condition("id.trId", null, null, Restriction.ORDERBY));

            List<AsyncCommandLog> acllist = acld.findByConditions(condition);
            
            logger.debug("[{}][COMMAND_REQUEST][TRAN_ID={}] - DEVICE={}, COMMAND={}, STATE={}, ASYNC_SIZE={}"
            		, new Object[]{actionCommandType.name(), aDevice.getTransactionId(), aDevice.getModemId(), aDevice.getCommand(), TR_STATE.Waiting.getCode(), acllist.size ()});
            
            if (acllist.size() > 0) {
            	/*
            	 * 기존에 수행하다 문제가 접속이 끊겨서 멈춘것들을 모두 삭제하고
            	 * 마지막(가장최근) 트렌젝션으로 수행한다.
            	 */
                AsyncCommandLog acl = null;
                for (int i = 0; i < acllist.size(); i++) {
                    acl = acllist.get(i);
                    if (i == acllist.size() - 1)
                    	acl.setState(TR_STATE.Running.getCode());
                    else
                    	acl.setState(TR_STATE.Delete.getCode());
                    acld.update(acl);
                }
                aDevice.setTrState(TR_STATE.Running);
                
                payLoad = new EMnVCommandFramePayLoad(EMnVCommandSubFrameType.COMMAND_REQUEST);
                payLoad.setCommandType(actionCommandType);
                payLoad.setRwsType(RWSType.S);
                payLoad.setErrorBitType(ErrorBitType.SUCCESS);
                generalDataFrame.setGeneralFramePayLoad(payLoad);
                
                logger.info("");
                logger.info("#### [{}][COMMAND_REQUEST][TRAN_ID={}][remote={}] RWS_Type={}"
                		, new Object[]{actionCommandType.name(), aDevice.getTransactionId(), session.getRemoteAddress(), rwsType});
                session.write(generalDataFrame);
            } else {
                // 실행할 명령이 없으면 EOT 호출하고 종료            	
                session.write(EMnVActionType.EOT);
            }
			break;
		case COMMAND_RESPONSE:
			aDevice = (ActionDevice)session.getAttribute(session.getRemoteAddress());
			payLoad = (EMnVCommandFramePayLoad) gFramePayload;
			rwsType = payLoad.getRwsType();
			
			// 비동기 내역 수정
            acld = DataUtil.getBean(AsyncCommandLogDao.class);
            condition = new HashSet<Condition>();
            condition.add(new Condition("deviceId", new Object[]{aDevice.getModemId()}, null, Restriction.EQ));
            condition.add(new Condition("id.trId", new Object[]{aDevice.getTransactionId()}, null, Restriction.EQ));
            condition.add(new Condition("state", new Object[]{TR_STATE.Running.getCode()}, null, Restriction.EQ));
            // orderby 날짜로
            List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
            logger.debug("ASYNC_SIZE[" + acLogList.size () + "]");
            
			switch (payLoad.getErrorBitType()) {
			case SUCCESS:
				logger.info("[{}][COMMAND_RESPONSE][{}][TRNX_ID={}][{}] Success !!!"
						, new Object[]{actionCommandType.name(), aDevice.getModemId(), aDevice.getTransactionId(), payLoad.getRwsType().getDesc()});
	            if (acLogList.size() > 0) {
	                for (int i = 0; i < acLogList.size(); i++) {
	                	AsyncCommandLog acl = acLogList.get(i);
	                   	acl.setState(TR_STATE.Success.getCode());
	                    acld.update(acl);
	                }
	                aDevice.setTrState(TR_STATE.Success);
	            }
				break;
			case NOT_SUPPORTED_COMMAND:
				logger.error("[{}][COMMAND_RESPONSE][{}][TRNX_ID={}] Fail !!! - NOT_SUPPORTED_COMMAND " 
						, new Object[]{actionCommandType.name(), aDevice.getModemId(), aDevice.getTransactionId()});
				break;
			case NOT_ACCESS_USER:
				logger.error("[{}][COMMAND_RESPONSE][{}][TRNX_ID={}] Fail !!! - NOT_ACCESS_USER"
						, new Object[]{actionCommandType.name(), aDevice.getModemId(), aDevice.getTransactionId()});
				break;
			case FAIL:
				logger.error("[{}][COMMAND_RESPONSE][{}][TRNX_ID={}] Fail !!!"
						, new Object[]{actionCommandType.name(), aDevice.getModemId(), aDevice.getTransactionId()});
	            if (acLogList.size() > 0) {
	                for (int i = 0; i < acLogList.size(); i++) {
	                	AsyncCommandLog acl = acLogList.get(i);
	                   	acl.setState(TR_STATE.Terminate.getCode());
	                    acld.update(acl);
	                }
	                aDevice.setTrState(TR_STATE.Terminate);
	            }
				break;
			case UNKNOWN:
				
				break;
			default:
				break;
			}
			
            // EOT 호출하고 종료            	
            session.write(EMnVActionType.EOT);
			break;
		default:
			break;
		}
	}

}
