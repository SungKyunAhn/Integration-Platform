package com.aimir.fep.bypass.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.session.IoSession;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.aimir.constants.CommonConstants.TR_STATE;
import com.aimir.constants.CommonConstants.TargetClass;
import com.aimir.dao.device.AsyncCommandLogDao;
import com.aimir.dao.device.AsyncCommandParamDao;
import com.aimir.dao.device.AsyncCommandResultDao;
import com.aimir.dao.device.MeterDao;
import com.aimir.dao.device.ModemDao;
import com.aimir.fep.bypass.BypassDevice;
import com.aimir.fep.bypass.actions.Constant_GV.ActionResultStatus;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode_BOOT_UP;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode_CLI_STATUS;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode_MODEM_RESET;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode_OTA_STATUS;
import com.aimir.fep.bypass.actions.Constant_GV.MIUEventCode_PPP_STATUS;
import com.aimir.fep.bypass.actions.evn.DayProfileTable;
import com.aimir.fep.bypass.actions.evn.DayProfileTableFactory;
import com.aimir.fep.bypass.actions.evn.SeasonProfileTable;
import com.aimir.fep.bypass.actions.evn.SeasonProfileTableFactory;
import com.aimir.fep.bypass.actions.evn.WeekProfileTable;
import com.aimir.fep.bypass.actions.evn.WeekProfileTableFactory;
import com.aimir.fep.bypass.decofactory.protocolfactory.BypassEVNFactory;
import com.aimir.fep.bypass.decofactory.protocolfactory.BypassFrameFactory;
import com.aimir.fep.bypass.decofactory.protocolfactory.BypassFrameResult;
import com.aimir.fep.bypass.decofactory.protocolfactory.BypassFrameFactory.Procedure;
import com.aimir.fep.meter.parser.DLMSNamjunTable.DLMSTable;
import com.aimir.fep.protocol.fmp.client.Client;
import com.aimir.fep.protocol.fmp.client.bypass.BYPASSClient;
import com.aimir.fep.protocol.fmp.client.bypass.BypassClientHandler;
import com.aimir.fep.protocol.fmp.datatype.BYTE;
import com.aimir.fep.protocol.fmp.datatype.FMPVariable;
import com.aimir.fep.protocol.fmp.datatype.OCTET;
import com.aimir.fep.protocol.fmp.datatype.OID;
import com.aimir.fep.protocol.fmp.datatype.SMIValue;
import com.aimir.fep.protocol.fmp.datatype.STREAM;
import com.aimir.fep.protocol.fmp.datatype.UINT;
import com.aimir.fep.protocol.fmp.frame.ControlDataConstants;
import com.aimir.fep.protocol.fmp.frame.ControlDataFrame;
import com.aimir.fep.protocol.fmp.frame.service.CommandData;
import com.aimir.fep.protocol.fmp.frame.service.EventData_1_2;
import com.aimir.fep.protocol.fmp.frame.service.ServiceData;
import com.aimir.fep.protocol.fmp.processor.EventProcessor_1_2;
import com.aimir.fep.trap.actions.GV.EV_GV_evtOTADownloadEnd_Action;
import com.aimir.fep.trap.actions.GV.EV_GV_evtOTADownloadResult_Action;
import com.aimir.fep.trap.common.EV_Action.OTA_UPGRADE_RESULT_CODE;
import com.aimir.fep.util.CRCUtil;
import com.aimir.fep.util.CodecUtil;
import com.aimir.fep.util.DataUtil;
import com.aimir.fep.util.EventUtil;
import com.aimir.fep.util.Hex;
import com.aimir.model.device.AsyncCommandLog;
import com.aimir.model.device.AsyncCommandParam;
import com.aimir.model.device.AsyncCommandResult;
import com.aimir.model.device.CommLog;
import com.aimir.model.device.Device.DeviceType;
import com.aimir.model.device.EventAlertLog;
import com.aimir.model.device.Meter;
import com.aimir.model.device.Modem;
import com.aimir.util.Condition;
import com.aimir.util.Condition.Restriction;
import com.aimir.util.DateTimeUtil;

/*   Vietnam EVN 
cmdResetModem,100.1.1,Reset Modem
cmdUploadMeteringData,100.2.1,Upload Metering Data
cmdFactorySetting,100.3.1,Factory Setting
cmdReadModemConfiguration,101.1.1,Read Modem Configuration
cmdSetTime,102.1.1,Set Time
cmdModemResetInterval,102.2.1,Set Modem Reset Interval
cmdSetMeteringIntervla,102.3.1,Set Metering Interval
cmdSetServerIpPort,103.1.1,Set Server IP and Port
cmdSetApn,103.2.1 Set APN Information
cmdOBISListUp,104.1.1,OBIS list up
cmdOBISAdd,104.2.1,OBIS add
cmdOBISRemove,104.3.1,OBIS remove
cmdOBISListChange,104.4.1,OBIS list change
cmdSetBypassStart,105.1.1,Null Bypass Start
cmdOTAStart,106.1.1,Upgrade Start
cmdSendImage,106.2.1,Send Image
cmdOTAEnd,106.3.1,Upgrade End
*/
public class CommandAction_GV extends CommandAction {
	private static Log log = LogFactory.getLog(CommandAction_GV.class);

	@Override
	public void execute(String cmd, SMIValue[] smiValues, IoSession session) throws Exception {
		try {
			/*
			 * EVN GPRS Modem Command List
			 * 
			 * 1. cmdResetModem 
			 * 2. cmdUploadMeteringData -- 검침서버로 Upload 
			 * 3. cmdFactorySetting 
			 * 4. cmdReadModemConfiguration -- 검침서버로 Upload 
			 * 5. cmdIdentifyDevice -- 모뎀 식별용으로만 사용 
			 * 6. cmdSetTime 
			 * 7. cmdSetModemResetInterval 
			 * 8. cmdSetMeteringInterval 
			 * 9. cmdSetServerIpPort 
			 * 10. cmdSetApn 
			 * 11. cmdOTAStart 
			 * 12. cmdCurrentValuesMetering -- 검침서버로 Upload X. 지원하지 않음. 
			 * 13. cmdReadModemEventLog 
			 * 14. cmdGetMeterStatus 
			 * 15. cmdOndemandMetering -- 미터쪽 Command 검침서버로 Upload 
			 * 16. cmdRelayDisconnect -- 미터쪽 Command 
			 * 17. cmdRelayReconnect -- 미터쪽 Command 
			 * 18. cmdSetMeterTime -- 미터쪽 Command (Meter Timesync) 
			 * 19. cmdRelayStatus -- 미터쪽 Command (Get Relay Status) 
			 * 20. cmdMeterOTAStart 
			 * 21. cmdGetMeterFWVersion
			 * 22. cmdGetDemandPeriod
			 */
			
			BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
			JpaTransactionManager txmanager = (JpaTransactionManager) DataUtil.getBean("transactionManager");
			TransactionStatus txstatus = null;

			AsyncCommandLogDao acld = null;
			Set<Condition> condition = null;

			String resultParams = "";

			if (0 < smiValues.length) {
				for (SMIValue value : smiValues) {
					resultParams += value.getVariable().toString() + ", ";
				}

				resultParams = resultParams.substring(0, resultParams.length() - 2);
				log.debug("### [" + cmd + "] Command Response params = " + resultParams);
			} else {
				log.debug("### [" + cmd + "] Command Response params is empty.");
			}

			/***********************************
			 * Response 처리
			 ***********************************/
			//GV 101.2.1 모뎀/미터 식별 응답이 오면 커맨드를 실행한다.
			if (cmd.equals("cmdIdentifyDevice")) { // 모뎀 식별용 Command.
				bd.setModemId(smiValues[0].getVariable().toString());
				bd.setMeterId(smiValues[1].getVariable().toString());

				/*
				 * 아래와 같은 의미로 사용한다. 
				 * TR_STATE.Success (0) : Command 수행 성공및 정상종료 상태 
				 * TR_STATE.Waiting (1) : 서버측에서 SMS전송후 모뎀의 접속을 기다리는 상태
				 * TR_STATE.Running (2) : Command 수행중인 상태 
				 * TR_STATE.Terminate (4) : Command 가 성공하지 못한 상태에서 종료된 상태 (ex. crc error)
				 * TR_STATE.Delete (8) : 다른 Tranasction의 동일 Command가 실행되어(단지 실행) 삭제처리된 상태 
				 * TR_STATE.Unknown (255) : 아몰랑.
				 */
				// 비동기 내역을 조회한다.
				List<AsyncCommandLog> acllist = null;
				acld = DataUtil.getBean(AsyncCommandLogDao.class);

				try {
					txstatus = txmanager.getTransaction(null);

					// 미터 시리얼 번호로 조회
					/*				Set<Condition> condition = new HashSet<Condition>();
									condition.add(new Condition("deviceId", new Object[] { bd.getMeterId() }, null, Restriction.EQ));
									condition.add(new Condition("state", new Object[] { TR_STATE.Waiting.getCode() }, null, Restriction.EQ));
									List<AsyncCommandLog> acllist = acld.findByConditions(condition);

									// 모뎀 번호로 조회
									condition = new HashSet<Condition>();
									condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
									condition.add(new Condition("state", new Object[] { TR_STATE.Waiting.getCode() }, null, Restriction.EQ));
									acllist.addAll(acld.findByConditions(condition));*/

					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					//condition.add(new Condition("state", new Object[] { TR_STATE.Waiting.getCode(), TR_STATE.Running.getCode() }, null, Restriction.IN));
					condition.add(new Condition("state", new Object[] { TR_STATE.Waiting.getCode() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", null, null, Restriction.ORDERBY));
					acllist = acld.findByConditions(condition);

					log.debug("ModemID = " + bd.getModemId() + ", TransactionId = " + bd.getTransactionId() + " ASYNC_SIZE[" + acllist.size() + "]");

					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

				if (acllist.size() > 0) {
					/*
					 * 기존에 수행하다 문제가 접속이 끊겨서 멈춘것들을 모두 Delete하고
					 * 마지막(가장최근) 트렌젝션으로 수행한다.
					 */
					AsyncCommandLog acl = null;
					List<AsyncCommandParam> acplist = null;
					try {
						txstatus = txmanager.getTransaction(null);

						for (int i = 0; i < acllist.size(); i++) {
							acl = acllist.get(i);
							if (i == acllist.size() - 1)
								acl.setState(TR_STATE.Running.getCode());
							else
								acl.setState(TR_STATE.Delete.getCode());
							acld.update(acl);
						}

						bd.setTrState(TR_STATE.Running);

						// 마지막 커맨드를 실행한다.
						acl = acllist.get(acllist.size() - 1);
						bd.setTransactionId(acl.getTrId());

						condition = new HashSet<Condition>();
						condition.add(new Condition("id.trId", new Object[] { acl.getTrId() }, null, Restriction.EQ));
						condition.add(new Condition("id.mcuId", new Object[] { acl.getMcuId() }, null, Restriction.EQ));
						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
						acplist = acpd.findByConditions(condition);

						txmanager.commit(txstatus);
					} catch (Exception e) {
						if (txstatus != null)
							txmanager.rollback(txstatus);
					}

					HashMap<String, Object> map = new HashMap<String, Object>();
					for (AsyncCommandParam param : acplist) {
						map.put(param.getParamType(), param.getParamValue());
					}
					bd.setArgMap(map);

					/**********************
					 * REQUEST ~!!
					 */

					String excuteCommandName = acl.getCommand();
					bd.setCommand(excuteCommandName);

					log.debug("EXCUTE CommandName => " + excuteCommandName);
					Method method = this.getClass().getMethod(excuteCommandName, IoSession.class);
					method.invoke(this, session);

					/**
					 * 아래 Command들은 리턴값이 없어서 상태를 체크할 수가 없음. 임의로 Command실행시
					 * 성공한것으로 처리함. 추후 리턴값을 받아서 처리 할경우 상태변경을 아래 로직에서 하지 말고 각
					 * Command Response 처리하는 부분에서 진행할것.
					 */
					if (acl.getCommand().equals("cmdResetModem") || acl.getCommand().equals("cmdFactorySetting") || acl.getCommand().equals("cmdSetTime") || acl.getCommand().equals("cmdSetModemResetInterval") || acl.getCommand().equals("cmdSetMeteringInterval") || acl.getCommand().equals("cmdSetServerIpPort") || acl.getCommand().equals("cmdSetApn")
					// 검침서버로 Upload 하는 Command들
							|| acl.getCommand().equals("cmdReadModemConfiguration") || acl.getCommand().equals("cmdUploadMeteringData") || acl.getCommand().equals("cmdOndemandMetering")
							// cmdCurrentValuesMetering 기능은 OBIS코드 문제로 현재 제공하지 않기로함 향후 필요시 사용할것. (미터 OBIS 코드 정의 필요함.)
							|| acl.getCommand().equals("cmdCurrentValuesMetering")) {

						//AsyncCommandLogDao acld = DataUtil.getBean(AsyncCommandLogDao.class);
						try {
							txstatus = txmanager.getTransaction(null);

							//acld = DataUtil.getBean(AsyncCommandLogDao.class);
							condition = new HashSet<Condition>();
							condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
							condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
							condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
							// orderby 날짜로
							List<AsyncCommandLog> acLogList = acld.findByConditions(condition);

							log.debug("TEMP ASYNC_SIZE[" + acLogList.size() + "]");

							if (acLogList.size() > 0) {
								for (int i = 0; i < acLogList.size(); i++) {
									AsyncCommandLog sacl = acLogList.get(i);
									sacl.setState(TR_STATE.Success.getCode());
									
									log.debug("Command=" + sacl.getCommand() + "(TransactionID=" + sacl.getTrId()+ ", ReqTime="+ sacl.getRequestTime() +") AsyncCommandLog state change (before = " + TR_STATE.valueOf(sacl.getState()) + ", after=" + TR_STATE.Success.name());
									
									acld.update(sacl);
								}
								bd.setTrState(TR_STATE.Success);
							}
							
							
							AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
							int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
							
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Async Command excute ok");
							acpd.add(asyncCommandResult);

							log.debug("AsyncCommand Result => " + asyncCommandResult.toJSONString());

							txmanager.commit(txstatus);
						} catch (Exception e) {
							if (txstatus != null)
								txmanager.rollback(txstatus);
						}

						log.debug("### [Command=" + acl.getCommand() + "] [Modem=" + bd.getModemId() + ", Meter=" + bd.getMeterId() + "] Command excute complete. bye bye~");
					}

				} else {
					log.debug("### [Modem=" + bd.getModemId() + ", Meter=" + bd.getMeterId() + "] have no command. bye bye~");
					
					// 실행할 명령이 없으면 EOT 호출하고 종료
					session.write(new ControlDataFrame(ControlDataConstants.CODE_EOT));

					/**
					 * TEST Code
					 */
					//					HashMap<String, Object> map = new HashMap<String, Object>();
					//					bd.setArgMap(map);
					//					cmdReadModemEventLog(session);
				}
			} // cmd.equals("cmdIdentifyDevice") END.
			/**
			 * Modem으로부터 리턴결과가 없는 Command들 처리. : 이러한 Command는 현재 모뎀으로부터
			 * Response가 따로없이 명령수행후 바로 끊기게된다. 추후 Modem으로부터 결과 Response를 받아서 처리하는
			 * 방식으로 로직이 변경될경우 아래같은 형태로 수정하여 사용할것.
			 */
			else if (cmd.equals("cmdResetModem") || cmd.equals("cmdFactorySetting") || cmd.equals("cmdSetTime") || cmd.equals("cmdSetModemResetInterval") || cmd.equals("cmdSetMeteringInterval") || cmd.equals("cmdSetServerIpPort") || cmd.equals("cmdSetApn")
			// 검침서버로 Upload 하는 Command들
					|| cmd.equals("cmdReadModemConfiguration") || cmd.equals("cmdUploadMeteringData") || cmd.equals("cmdOndemandMetering")
					// cmdCurrentValuesMetering 기능은 OBIS코드 문제로 현재 제공하지 않기로함 향후 필요시 사용할것. (미터 OBIS 코드 정의 필요함.)
					|| cmd.equals("cmdCurrentValuesMetering")) {

				log.debug("#########  여기 실행 됬어 !!! @@@@@@@ ");
				log.debug("#########  여기 실행 됬어 !!! @@@@@@@ ");
				log.debug("#########  여기 실행 됬어 !!! @@@@@@@ ");
			}

			/*
			 * OTA 진행
			 */
			else if (cmd.equals("cmdOTAStart")) {
				log.info("### [" + cmd + "] Response params => " + smiValues.toString());
				log.debug("modemId[" + bd.getModemId() + "] meterId[" + bd.getMeterId() + "]");
				bd.setModemModel(smiValues[0].getVariable().toString());
				bd.setFwVersion(smiValues[1].getVariable().toString());
				bd.setBuildno(smiValues[2].getVariable().toString());
				bd.setHwVersion(smiValues[3].getVariable().toString());
				bd.setPacket_size(Integer.parseInt(smiValues[4].getVariable().toString()));

				cmdSendImage(session);
			} else if (cmd.equals("cmdSendImage")) {
				cmdSendImage(session);
			} else if (cmd.equals("cmdOTAEnd")) {
				long endTime = System.currentTimeMillis();
				ModemDao modemDao = DataUtil.getBean(ModemDao.class);
				Modem modem = modemDao.get(bd.getModemId());
				TargetClass tClass = TargetClass.valueOf(modem.getModemType().name());

				// 상태값을 받아서 실패하면 다시 시도하도록 한다.
				int status = Integer.parseInt(smiValues[0].getVariable().toString());

				log.debug("#### OTA 결과는 => " + (status == 0 ? "SUCCESS" : "ERROR" + ", ModemId=" + bd.getModemId() + ", TransactionID=" + bd.getTransactionId()));

				// 비동기 내역 수정
				List<AsyncCommandLog> acLogList = null;
				try {
					txstatus = txmanager.getTransaction(null);
					//acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					acLogList = acld.findByConditions(condition);

					log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}
						
						AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
						int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
						
						// Return값 저장
						AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(nextNum);
						asyncCommandResult.setResultType("RESULT_OTA");
						asyncCommandResult.setResultValue(status == 0 ? "SUCCESS" : "ERROR");
						acpd.add(asyncCommandResult);

						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_OTA_START");
						asyncCommandResult.setResultValue(DateTimeUtil.getDateString(bd.getStartOTATime()));
						acpd.add(asyncCommandResult);
						
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_OTA_END");
						asyncCommandResult.setResultValue(DateTimeUtil.getDateString(endTime));
						acpd.add(asyncCommandResult);

						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_OTA_ELAPSE");
						asyncCommandResult.setResultValue((endTime - bd.getStartOTATime()) / 1000.0f + "s");
						acpd.add(asyncCommandResult);
						

//						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
//
//						// Return값 저장
//						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
//						asyncCommandParam.setMcuId(bd.getModemId());
//						asyncCommandParam.setNum(7); // 1.fw_crc, 2.fw_size, 3.fw_model_name, 4.fw_path, 5.oid, 6.fw_version, 7.RESULT_OTA
//						asyncCommandParam.setParamType("RESULT_OTA");
//						asyncCommandParam.setParamValue(status == 0 ? "SUCCESS" : "ERROR");
//						asyncCommandParam.setTrId(bd.getTransactionId());
//						acpd.add(asyncCommandParam);
//
//						asyncCommandParam.setNum(8); // 8.RESULT_OTA_START
//						asyncCommandParam.setParamType("RESULT_OTA_START");
//						asyncCommandParam.setParamValue(DateTimeUtil.getDateString(bd.getStartOTATime()));
//						acpd.add(asyncCommandParam);
//
//						asyncCommandParam.setNum(9); // 9.RESULT_OTA_END
//						asyncCommandParam.setParamType("RESULT_OTA_END");
//						asyncCommandParam.setParamValue(DateTimeUtil.getDateString(endTime));
//						asyncCommandParam.setTrId(bd.getTransactionId());
//						acpd.add(asyncCommandParam);
//
//						asyncCommandParam.setNum(10); // 10.RESULT_OTA_ELAPSE
//						asyncCommandParam.setParamType("RESULT_OTA_ELAPSE");
//						asyncCommandParam.setParamValue((endTime - bd.getStartOTATime()) / 1000.0f + "s");
//						asyncCommandParam.setTrId(bd.getTransactionId());
//						acpd.add(asyncCommandParam);
						bd.setTrState(TR_STATE.Success);
					}

//					/*
//					 * Modem 정보 갱신
//					 */
//					if (status == 0) {
//						bd.setModemModel(smiValues[0].getVariable().toString());
//						bd.setFwVersion(smiValues[1].getVariable().toString());
//						bd.setBuildno(smiValues[2].getVariable().toString());
//						bd.setHwVersion(smiValues[3].getVariable().toString());
//						bd.setPacket_size(Integer.parseInt(smiValues[4].getVariable().toString()));
//
//						modem.setFwVer(bd.getFwVersion());
//						modem.setHwVer(bd.getHwVersion());
//					}
//
//					/*
//					 * OTA 종료후 Event 저장
//					 */
//					//long endTime = System.currentTimeMillis();
//					String elapseTime = DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime()));
//
//					String openTime = DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss");
//					EV_GV_evtOTADownloadEnd_Action action1 = new EV_GV_evtOTADownloadEnd_Action();
//					action1.makeEvent(tClass, bd.getModemId(), tClass, openTime, elapseTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]", "HES");
//					action1.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]");
//
//					EV_GV_evtOTADownloadResult_Action action2 = new EV_GV_evtOTADownloadResult_Action();
//					action2.makeEvent(tClass, bd.getModemId(), tClass, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, null, "HES");
//					action2.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR);
//
//					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
//					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
//					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
//					
					txmanager.commit(txstatus);
				} catch (Exception e) {
					log.error("cmdOTAEnd Error - " + e, e);
					
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}
				
				/*
				 * Modem 정보 갱신
				 */
				if (status == 0) {
					bd.setModemModel(smiValues[0].getVariable().toString());
					bd.setFwVersion(smiValues[1].getVariable().toString());
					bd.setBuildno(smiValues[2].getVariable().toString());
					bd.setHwVersion(smiValues[3].getVariable().toString());
					bd.setPacket_size(Integer.parseInt(smiValues[4].getVariable().toString()));

					modem.setFwVer(bd.getFwVersion());
					modem.setHwVer(bd.getHwVersion());
					
					/*
					 * OTA 종료후 Event 저장
					 */
					//long endTime = System.currentTimeMillis();
					String elapseTime = DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime()));

					String openTime = DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss");
					EV_GV_evtOTADownloadEnd_Action action1 = new EV_GV_evtOTADownloadEnd_Action();
					action1.makeEvent(tClass, bd.getModemId(), tClass, openTime, elapseTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]", "HES");
					action1.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]");

					EV_GV_evtOTADownloadResult_Action action2 = new EV_GV_evtOTADownloadResult_Action();
					action2.makeEvent(tClass, bd.getModemId(), tClass, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, null, "HES");
					action2.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR);
				}else{
					/*
					 * OTA 종료후 Event 저장
					 */
					//long endTime = System.currentTimeMillis();
					String elapseTime = DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime()));

					String openTime = DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss");
					EV_GV_evtOTADownloadEnd_Action action1 = new EV_GV_evtOTADownloadEnd_Action();
					action1.makeEvent(tClass, bd.getModemId(), tClass, openTime, elapseTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, "result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"), "HES");
					action1.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, "result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));

					EV_GV_evtOTADownloadResult_Action action2 = new EV_GV_evtOTADownloadResult_Action();
					action2.makeEvent(tClass, bd.getModemId(), tClass, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, null, "HES");
					action2.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL);
				}

				log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
				log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
				log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));

				log.debug("### [OTA FINISHED] Start = " + DateTimeUtil.getDateString(bd.getStartOTATime()) + ", Finished = " + DateTimeUtil.getDateString(endTime) + ", Elapse = " + (endTime - bd.getStartOTATime()) / 1000.0f + "s");

			} else if (cmd.equals("cmdGetMeterStatus")) {
				log.info("### [" + cmd + "] Response params => " + smiValues[0].toString());
				log.info("### [" + cmd + "] Response params => " + Hex.decode(smiValues[0].getVariableByte()));

				BYTE byteValue = (BYTE) smiValues[0].getVariable();

				/*
				: 1-ON, 0-Off
				LSB
				0 : L1 relay status
				1 : L2 relay status
				2 : L3 relay status
				3 : External relay status
				4 : L1 relay error
				5 : L2 relay error
				6 : L3 relay error
				7 : External relay error
				8 : Open terminal cover
				9 : Open terminal cover in power off
				10: Open top cover
				11: Open top cover in power off
				12: Magnetic detection 1
				13:Magnetic detection 2
				14: Program
				15: Factory status
				MSB
				*/

				boolean status = false;
				log.debug("## [cmdGetMeterStatus] METER STATUS ==> " + Integer.toBinaryString(byteValue.getValue()));

				if ((byteValue.getValue() & 0x01) == 1 || (byteValue.getValue() & 0x02) == 1 || (byteValue.getValue() & 0x04) == 1) {
					status = true; // Relay Close 상태. 전류가 흐르는 상태. 정상
				}
				if ((byteValue.getValue() & 0x01) == 0 && (byteValue.getValue() & 0x02) == 1 && (byteValue.getValue() & 0x04) == 0) {
					status = false; // Relay Open 상태. 전류가 흐르지 않는 상태
				}
				if (byteValue.getValue() == 0) {
					status = false; // Relay Open 상태. 전류가 흐르지 않는 상태
				}
				/**
				 * 결과값 분석
				 */

				String statusString = DLMSTable.getOBIS_FUNCTION_STATUS((OCTET) new OCTET(smiValues[0].encode()));
				log.debug("## [cmdGetMeterStatus] METER STATUS string ==> " + statusString);

				/**
				 * 비동기 내역 수정
				 */
				try {
					txstatus = txmanager.getTransaction(null);

					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ModemID = " + bd.getModemId() + ", TransactionId = " + bd.getTransactionId() + " ASYNC_LOG_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);

						// Return값 저장
						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(2); // setNum 1번은 OID 244.0.0이 들어있음.
						asyncCommandParam.setParamType("RESULT_STATUS");
						asyncCommandParam.setParamValue(status ? "Connected" : "Disconnected");
						asyncCommandParam.setTrId(bd.getTransactionId());
						acpd.add(asyncCommandParam);

						// Return값 저장
						asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(3);
						asyncCommandParam.setParamType("RESULT_STATUS_STRING");
						asyncCommandParam.setParamValue(statusString);
						asyncCommandParam.setTrId(bd.getTransactionId());
						acpd.add(asyncCommandParam);

						bd.setTrState(TR_STATE.Success);
					}

					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

				/**
				 * 미터 상태 변경
				 */
				setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status ? 0 : 1);
			} else if (cmd.equals("cmdReadModemEventLog")) {

				log.info("### [" + cmd + "] Response params => " + smiValues[0].toString());
				log.info("### [" + cmd + "] Response params => " + Hex.decode(smiValues[0].getVariableByte()));

				byte[] bb = smiValues[0].getVariableByte();
				int totalEventCount = 0;

				if (bb != null && 0 < bb.length) {
					/*  Event List Format */
					byte[] count = new byte[2];
					byte[] index = new byte[2];
					byte[] time = new byte[7];
					byte[] code = new byte[2];
					byte[] value = new byte[4];

					int pos = 0;
					System.arraycopy(bb, pos, count, 0, count.length);
					pos += count.length;

					totalEventCount = DataUtil.getIntTo2Byte(count);

					for (int i = 0; i < totalEventCount; i++) {
						System.arraycopy(bb, pos, index, 0, index.length);
						pos += index.length;

						System.arraycopy(bb, pos, time, 0, time.length);
						pos += time.length;

						System.arraycopy(bb, pos, code, 0, code.length);
						pos += code.length;

						System.arraycopy(bb, pos, value, 0, value.length);
						pos += value.length;

						log.debug("EventLogData Total: " + totalEventCount + " Index:" + DataUtil.getIntTo2Byte(index) + " , Time:" + DataUtil.getTimeStamp(time) + ", Code:" + Hex.decode(code) + ", Value:" + Hex.decode(value));

						/*
						 * eventAlertName : 이벤트 알람 명 
						 * activatorType: 이벤트 소스(MCU, Meter, Modem...) 
						 * activatorId : 이벤트 소스 아이디 (MCU 아이디, 계량기 아이디...) 
						 * timestamp : 이벤트 발생 시간 
						 * eventClassId : 이벤트 종류 
						 * attrs : 속성 new String[][] {{"meterID", "2039293293"},{"mcuID", "20392"}}
						 */
						String eventAlertName = Constant_GV.MIUEventCode.getItem(code).name();
						String eventAlertValue = "";

						switch (MIUEventCode.getItem(code)) {
						case BOOT_UP:
							int resetCount = DataUtil.getIntToBytes(new byte[] { value[0], value[1] });
							eventAlertValue = MIUEventCode_BOOT_UP.getItem(new byte[] { value[2], value[3] }).getDesc() + " / Reset Count = " + resetCount;
							break;
						case MODEM_RESET:
							eventAlertValue = MIUEventCode_MODEM_RESET.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
							break;
						case CLI_STATUS:
							eventAlertValue = MIUEventCode_CLI_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
							break;
						case OTA_STATUS:
							eventAlertValue = MIUEventCode_OTA_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
							break;
						case PPP_STATUS:
							eventAlertValue = MIUEventCode_PPP_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
							break;
						default: // Value값이 없는 코드들.
							eventAlertValue = MIUEventCode.getItem(code).getDesc();
							break;
						}

						log.debug("## ModemID = " + bd.getModemId() + ", EventAlertName=" + eventAlertName + ", EventAlertValue=" + eventAlertValue);

						/**
						 * Event Alarm Log 저장.
						 */
						MeterDao meterDao = DataUtil.getBean(MeterDao.class);
						Meter meter = meterDao.get(bd.getMeterId());

						EventAlertLog event = new EventAlertLog();
						event.setActivatorId(bd.getModemId());
						event.setActivatorType(TargetClass.Modem);
						event.append(EventUtil.makeEventAlertAttr("modemID", "java.lang.String", bd.getModemId()));
						event.append(EventUtil.makeEventAlertAttr("message", "java.lang.String", DataUtil.getIntTo2Byte(index) + ". [" + eventAlertName + "]" + eventAlertValue));
						event.setSupplier(meter.getSupplier());

						try {
							EventUtil.sendEvent("Modem Event Log", TargetClass.Modem, bd.getModemId(), DataUtil.getTimeStamp(time), new String[][] {}, event);
						} catch (Exception e) {
							log.error("Send Event Error - " + e);
						}

					}
				}

				// 비동기 내역 수정
				try {
					txstatus = txmanager.getTransaction(null);
					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						// Return값 저장
						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(3);
						asyncCommandParam.setParamType("RESULT_EVENT_LOG_COUNT");
						asyncCommandParam.setParamValue(String.valueOf(totalEventCount));
						asyncCommandParam.setTrId(bd.getTransactionId());

						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
						acpd.add(asyncCommandParam);

						bd.setTrState(TR_STATE.Success);
					}
					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

			}

			/*
			 * 미터가젯쪽 에서 구현되어있는 Command 5개 
			 * 1. cmdOndemandMetering 
			 * 2. cmdRelayDisconnect 
			 * 3. cmdRelayReconnect 
			 * 4. cmdSetMeterTime 
			 * 5. cmdRelayStatus
			 */
			else if (cmd.equals("cmdOndemandMetering")) { /* 검침서버로 Upload */
				// 검침 서버로 Upload하기때문에 구현할 필요 없음.
				log.debug("######### 이거 안찍힐껄?????? ####");
			} else if (cmd.equals("cmdRelayDisconnect")) {
				int status = Integer.parseInt(smiValues[0].getVariable().toString());

				// 비동기 내역 수정
				try {
					txstatus = txmanager.getTransaction(null);
					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						// Return값 저장
						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(1);
						asyncCommandParam.setParamType("RESULT_DISCONNECT");
						asyncCommandParam.setParamValue(ActionResultStatus.getItem(status).name());
						asyncCommandParam.setTrId(bd.getTransactionId());

						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
						acpd.add(asyncCommandParam);

						bd.setTrState(TR_STATE.Success);
					}
					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

				setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status == ActionResultStatus.SUCCESS.getCode() ? 0 : 1);
			} else if (cmd.equals("cmdRelayReconnect")) {
				int status = Integer.parseInt(smiValues[0].getVariable().toString());

				// 비동기 내역 수정
				try {
					txstatus = txmanager.getTransaction(null);
					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						// Return값 저장
						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(1);
						asyncCommandParam.setParamType("RESULT_RECONNECT");
						asyncCommandParam.setParamValue(ActionResultStatus.getItem(status).name());
						asyncCommandParam.setTrId(bd.getTransactionId());

						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
						acpd.add(asyncCommandParam);

						bd.setTrState(TR_STATE.Success);
					}
					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

				setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status == ActionResultStatus.SUCCESS.getCode() ? 0 : 1);
			} else if (cmd.equals("cmdSetMeterTime")) {
				int status = Integer.parseInt(smiValues[0].getVariable().toString());

				// 비동기 내역 수정
				try {
					txstatus = txmanager.getTransaction(null);
					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						// Return값 저장
//						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
//						asyncCommandParam.setMcuId(bd.getModemId());
//						asyncCommandParam.setNum(3); // 이거 몇개 저장하는지 확인해서 수정
//						asyncCommandParam.setParamType("RESULT_METER_TIME");
//						asyncCommandParam.setParamValue(ActionResultStatus.getItem(status).name());
//						asyncCommandParam.setTrId(bd.getTransactionId());
//
//						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
//						acpd.add(asyncCommandParam);
						
						AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
						int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
						
						AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(ActionResultStatus.getItem(status).name());
						acpd.add(asyncCommandResult);

						log.debug("AsyncCommand Result => " + asyncCommandResult.toJSONString());
						
						bd.setTrState(TR_STATE.Success);
					}
					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}
			} else if (cmd.equals("cmdRelayStatus")) { // Get Relay Status
				// True = Connected, False = Disconnected
				boolean status = Boolean.parseBoolean(smiValues[0].getVariable().toString());

				// 비동기 내역 수정
				try {
					txstatus = txmanager.getTransaction(null);
					acld = DataUtil.getBean(AsyncCommandLogDao.class);
					condition = new HashSet<Condition>();
					condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
					condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
					condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
					// orderby 날짜로
					List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
					log.debug("ModemID = " + bd.getModemId() + ", TransactionId = " + bd.getTransactionId() + " ASYNC_LOG_SIZE[" + acLogList.size() + "]");

					if (acLogList.size() > 0) {
						for (int i = 0; i < acLogList.size(); i++) {
							AsyncCommandLog acl = acLogList.get(i);
							acl.setState(TR_STATE.Success.getCode());
							acld.update(acl);
						}

						// Return값 저장
						AsyncCommandParam asyncCommandParam = new AsyncCommandParam();
						asyncCommandParam.setMcuId(bd.getModemId());
						asyncCommandParam.setNum(1);
						asyncCommandParam.setParamType("RESULT_STATUS");
						asyncCommandParam.setParamValue(status ? "Connected" : "Disconnected");
						asyncCommandParam.setTrId(bd.getTransactionId());

						AsyncCommandParamDao acpd = DataUtil.getBean(AsyncCommandParamDao.class);
						acpd.add(asyncCommandParam);

						bd.setTrState(TR_STATE.Success);
					}
					txmanager.commit(txstatus);
				} catch (Exception e) {
					if (txstatus != null)
						txmanager.rollback(txstatus);
				}

				// 미터 상태변경
				setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status ? 0 : 1);
			}

			// 기존 코딩부분.. 추후 필요시 이용할것.
			// else if (cmd.equals("cmdOndemandMetering") || cmd.equals("cmdOnDemandByMeter") || cmd.equals("cmdOnDemandMeter")) {		}
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	@Override
	public void executeBypass(byte[] rawFrame, IoSession session) throws Exception {
		log.debug("#### Receive Bypass Frame ==> " + Hex.decode(rawFrame));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		BypassFrameResult bypassFrameResult = bd.getFrameFactory().receiveBypass(session, rawFrame);

		log.debug("### [" + bd.getCommand() + "] Execute Bypass ==> " + bypassFrameResult.toString());
		JpaTransactionManager txmanager = (JpaTransactionManager) DataUtil.getBean("transactionManager");
		TransactionStatus txstatus = null;

		if (bd.getCommand().equals("cmdMeterOTAStart")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());

					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(++nextNum);
					asyncCommandResult.setResultType("RESULT_VALUE");
					asyncCommandResult.setResultValue("fail");
					acpd.add(asyncCommandResult);
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				} else {
					try {
						log.debug("####여기야 ==> " + bypassFrameResult.toString());
						if (bypassFrameResult.getLastProcedure() == Procedure.ACTION_IMAGE_ACTIVATE) {
							long endTime = System.currentTimeMillis();

							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);
							log.debug("아싸저장1 = > " + asyncCommandResult.toJSONString());

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
							acpd.add(asyncCommandResult);
							log.debug("아싸저장2 = > " + asyncCommandResult.toJSONString());

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum); // RESULT_OTA_START
							asyncCommandResult.setResultType("RESULT_OTA_START");
							asyncCommandResult.setResultValue(DateTimeUtil.getDateString(bd.getStartOTATime()));
							acpd.add(asyncCommandResult);
							log.debug("아싸저장3 = > " + asyncCommandResult.toJSONString());

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum); // RESULT_OTA_END
							asyncCommandResult.setResultType("RESULT_OTA_END");
							asyncCommandResult.setResultValue(DateTimeUtil.getDateString(endTime));
							asyncCommandResult.setTrId(bd.getTransactionId());
							acpd.add(asyncCommandResult);
							log.debug("아싸저장4 = > " + asyncCommandResult.toJSONString());

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum); // RESULT_OTA_ELAPSE
							asyncCommandResult.setResultType("RESULT_OTA_ELAPSE");
							asyncCommandResult.setResultValue(DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime())));
							asyncCommandResult.setTrId(bd.getTransactionId());
							acpd.add(asyncCommandResult);
							log.debug("아싸저장5 = > " + asyncCommandResult.toJSONString());

						} else if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.GET_FIRMWARE_VERSION) {
							/*
							 * 미터 FW정보 UPDATE
							 */
							String fwVersion = (String) bypassFrameResult.getResultValue();
							log.debug("F/W Version = " + fwVersion + " OTA Finished.");

							MeterDao meterDao = DataUtil.getBean(MeterDao.class);
							Meter meter = meterDao.get(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId());
							meter.setSwVersion(fwVersion);
						}
					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}

		} else if (bd.getCommand().equals("cmdGetMeterFWVersion")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
				String fwVersion = (String) bypassFrameResult.getResultValue();

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.GET_FIRMWARE_VERSION) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							if (bypassFrameResult.getResultValue() != null) {
								asyncCommandResult.setResultValue(fwVersion);
							} else {
								asyncCommandResult.setResultValue("FAIL");
							}
							acpd.add(asyncCommandResult);

							/*
							 * 미터 FW정보 UPDATE
							 */
							MeterDao meterDao = DataUtil.getBean(MeterDao.class);
							Meter meter = meterDao.get(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId());
							meter.setSwVersion(fwVersion);

						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}

		} else if (bd.getCommand().equals("cmdAlarmReset")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());
					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.ACTION_METER_ALARM_RESET) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Success");
							acpd.add(asyncCommandResult);

						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		} else if (bd.getCommand().equals("cmdGetBillingCycle")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
				@SuppressWarnings("unchecked")
				HashMap<String, Object> resultValue = (HashMap<String, Object>) bypassFrameResult.getResultValue();

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(++nextNum);
					asyncCommandResult.setResultType("RESULT_VALUE");
					asyncCommandResult.setResultValue("Fail");
					acpd.add(asyncCommandResult);

					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.GET_BILLING_CYCLE) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue(resultValue.get("time") + " / " + resultValue.get("day"));
							acpd.add(asyncCommandResult);

						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		} else if (bd.getCommand().equals("cmdSetBillingCycle")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.SET_BILLING_CYCLE) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Success");
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}
					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		}else if (bd.getCommand().equals("cmdDemandPeriod")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.SET_DEMAND_MINUS_NUMBER) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Success");
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}
					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		}else if(bd.getCommand().equals("cmdGetDemandPeriod")){
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
				
				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(++nextNum);
					asyncCommandResult.setResultType("RESULT_VALUE");
					asyncCommandResult.setResultValue("Fail");
					acpd.add(asyncCommandResult);

					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						StringBuilder builder = new StringBuilder();							
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_PERIOD.name()) != null) {
							builder.append("demand +A Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_NUMBER.name()) != null) {
							builder.append("demand +A Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_PERIOD.name()) != null) {
							builder.append("demand -A Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_NUMBER.name()) != null) {
							builder.append("demand -A Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_PERIOD.name()) != null) {
							builder.append("demand +R Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_NUMBER.name()) != null) {
							builder.append("demand +R Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_PERIOD.name()) != null) {
							builder.append("demand -R Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_NUMBER.name()) != null) {
							builder.append("demand -R Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_PERIOD.name()) != null) {
							builder.append("demand R(QI) Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_NUMBER.name()) != null) {
							builder.append("demand R(QI) Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_PERIOD.name()) != null) {
							builder.append("demand R(QIV) Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_NUMBER.name()) != null) {
							builder.append("demand R(QIV) Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_PERIOD.name()) != null) {
							builder.append("demand+ Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_NUMBER.name()) != null) {
							builder.append("demand+ Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_NUMBER.name()));
							builder.append("\n");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_PERIOD.name()) != null) {
							builder.append("demand- Period : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_PERIOD.name()));
							builder.append(" , ");
						}
						if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_NUMBER.name()) != null) {
							builder.append("demand- Number : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_NUMBER.name()));
							builder.append("\n");
						}
						
						if(session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), builder.toString());
						}else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args+builder.toString());
						}
						
						if (bypassFrameResult.isFinished() == true) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);
							
							log.debug("["+bd.getCommand()+"] , Result["+session.getAttribute(bd.getCommand())+"]");
							
							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue(session.getAttribute(bd.getCommand()).toString());
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}
				
				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
			
		} else if (bd.getCommand().equals("cmdTOUSet")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Success");
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}
					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		} else if (bd.getCommand().equals("cmdTOUGet")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
				
				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(++nextNum);
					asyncCommandResult.setResultType("RESULT_VALUE");
					asyncCommandResult.setResultValue("Fail");
					acpd.add(asyncCommandResult);

					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						StringBuilder builder = new StringBuilder();
						
						if(bypassFrameResult.getResultValue(Procedure.GET_CALENDAR_NAME_PASSIVE.name()) != null) {			
							builder.append("[CALENDAR NAME PASSIVE] - ");
							builder.append((String)bypassFrameResult.getResultValue(Procedure.GET_CALENDAR_NAME_PASSIVE.name()));
							builder.append("\n\n");
						}
						
						if(bypassFrameResult.getResultValue(Procedure.GET_SEASON_PROFILE.name()) != null) {			
							builder.append("[SEASON PROFILE] \n");
							
							List<HashMap<String, Object>> list 
								= (ArrayList<HashMap<String, Object>>)bypassFrameResult.getResultValue(Procedure.GET_SEASON_PROFILE.name()) ;
							
							for(int i = 0; i < list.size(); i++) {
								HashMap<String, Object> map = (HashMap<String, Object>)list.get(i);
								
								if ((String)map.get("SeasonProfileName") != null) {
									builder.append("SeasonProfileName : ");
									builder.append((String)map.get("SeasonProfileName"));
									builder.append(" , ");
								}
								
								if ((String)map.get("SeasonStart") != null) {
									
									builder.append("SeasonStart : ");
									
									String strDate = (String)map.get("SeasonStart");
									String yy = "XXXX";
									String mm = strDate.substring(4, 6);
									String dd = strDate.substring(6, 8);
									String hh = strDate.substring(8, 10);
									String ii = strDate.substring(10, 12);
									String ss = strDate.substring(12, 14);
									String strMaskDate = mm + "-" + dd + "-" + yy + " " + hh + ":" +  ii + ":" + ss;
									
									builder.append(strMaskDate);
									builder.append(" , ");
								}
								
								if ((String)map.get("SeasonWeekName") != null) {
									builder.append("SeasonWeekName : ");
									builder.append((String)map.get("SeasonWeekName"));
									builder.append("\n");
								}
							}
							builder.append("\n");
						}
						
						
						if(bypassFrameResult.getResultValue(Procedure.GET_WEEK_PROFILE.name()) != null) {			
							builder.append("[WEEK PROFILE TABLE] \n");
							
							String[] weekName = {"MON","TUE","WED","THD","FRI","SAT","SUN"};
							
							List<HashMap<String, Object>> list 
								= (ArrayList<HashMap<String, Object>>)bypassFrameResult.getResultValue(Procedure.GET_WEEK_PROFILE.name()) ;
							for(int i = 0; i < list.size(); i++) {
								HashMap<String, Object> map = (HashMap<String, Object>)list.get(i);
								
								if ((String)map.get("WeekProfileName") != null) {
									builder.append("WeekProfileName : ");
									builder.append((String)map.get("WeekProfileName"));					
								}
								
								for(int w = 0; w < weekName.length; w++) {
									if ((String)map.get(weekName[w]) != null) {
										builder.append(" , ");
										builder.append(weekName[w] + " : ");
										builder.append((String)map.get(weekName[w]));						
									}	
								}
								
								builder.append("\n");
							}
							
							builder.append("\n");
						}
						
						if(bypassFrameResult.getResultValue(Procedure.GET_DAY_PROFILE.name()) != null) {			
							
							builder.append("[DAY PROFILE TABLE] \n");
							List<HashMap<String, Object>> list 
								= (ArrayList<HashMap<String, Object>>)bypassFrameResult.getResultValue(Procedure.GET_DAY_PROFILE.name()) ;
							
							for(int i = 0; i < list.size(); i++) {
								
								HashMap<String, Object> map = (HashMap<String, Object>)list.get(i);
								
								if ((String)map.get("dayid") != null) {
									builder.append("day id : ");
									builder.append((String)map.get("dayid"));
									builder.append(" , ");
								}
								
								if ((String)map.get("StartTime") != null) {					
									builder.append("StartTime : ");
									builder.append((String)map.get("StartTime"));
									builder.append(" , ");
								}
								
								if ((String)map.get("ScriptLogicalName") != null) {
									builder.append("ScriptLogicalName : ");
									builder.append((String)map.get("ScriptLogicalName"));
									builder.append(" , ");
								}
								
								if ((String)map.get("timeBucketNo") != null) {
									builder.append("timeBucketNo : ");
									builder.append((String)map.get("timeBucketNo"));
									builder.append("\n");
								}
							}
							
							builder.append("\n");
						}
						
						if(bypassFrameResult.getResultValue(Procedure.GET_STARTING_DATE.name()) != null) {			
							builder.append("[TOU STARTING DATE] - ");
							
							String strDate = (String)bypassFrameResult.getResultValue(Procedure.GET_STARTING_DATE.name());
							String yy = strDate.substring(0, 4);
							String mm = strDate.substring(4, 6);
							String dd = strDate.substring(6, 8);
							String hh = strDate.substring(8, 10);
							String ii = strDate.substring(10, 12);
							String ss = strDate.substring(12, 14);
							String strMaskDate = mm + "-" + dd + "-" + yy + " " + hh + ":" +  ii + ":" + ss;
							
							builder.append(strMaskDate);			
						}
					
						if(session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), builder.toString());
						}else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args+builder.toString());
						}
						
						if (bypassFrameResult.isFinished() == true) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);
							
							log.debug("["+bd.getCommand()+"] , Result["+session.getAttribute(bd.getCommand())+"]");
							
							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue(session.getAttribute(bd.getCommand()).toString());
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}
				
				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		}else if (bd.getCommand().equals("cmdSetCurrentLoadLimit")) {
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());

				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					if (bypassFrameResult.getResultValue() != null) {
						asyncCommandResult = new AsyncCommandResult();
						asyncCommandResult.setMcuId(bd.getModemId());
						asyncCommandResult.setTrId(bd.getTransactionId());
						asyncCommandResult.setNum(++nextNum);
						asyncCommandResult.setResultType("RESULT_VALUE");
						asyncCommandResult.setResultValue(((String) bypassFrameResult.getResultValue()));
						acpd.add(asyncCommandResult);
					}
					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						if (bypassFrameResult.isFinished() == true && bypassFrameResult.getLastProcedure() == Procedure.SET_CURRENT_LOAD_LIMIT_THRESHOLD) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);

							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue("Success");
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}
					bypassTRStateChange(session);
				}

				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		}else if(bd.getCommand().equals("cmdGetCurrentLoadLimit")){
			try {
				txstatus = txmanager.getTransaction(null);

				AsyncCommandResultDao acpd = DataUtil.getBean(AsyncCommandResultDao.class);
				int nextNum = acpd.getMaxNum(bd.getModemId(), bd.getTransactionId());
				
				// 실패시는 모두 저장
				if (!bypassFrameResult.isResultState()) {
					AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(nextNum);
					asyncCommandResult.setResultType("RESULT_STATUS");
					asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure() == null ? bypassFrameResult.getStep().name() : bypassFrameResult.getLastProcedure().name());
					acpd.add(asyncCommandResult);

					asyncCommandResult = new AsyncCommandResult();
					asyncCommandResult.setMcuId(bd.getModemId());
					asyncCommandResult.setTrId(bd.getTransactionId());
					asyncCommandResult.setNum(++nextNum);
					asyncCommandResult.setResultType("RESULT_VALUE");
					asyncCommandResult.setResultValue("Fail");
					acpd.add(asyncCommandResult);

					log.debug("Fail Result => " + asyncCommandResult.toJSONString());

					bypassTRStateChange(session);
				}
				// 성공시에는 DISC 까지 실행했을경우만 저장
				else {
					try {
						StringBuilder builder = new StringBuilder();							
						if(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME.name()) != null) {
							builder.append("Duration judge time : ");
							builder.append(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME.name()));
							builder.append("s, ");
						}

						if(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD.name()) != null) {
							String threshold = String.valueOf(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD.name()));
							builder.append("Threshold : ");
							builder.append(threshold.substring(0, threshold.length() - 2) + "." + threshold.substring(threshold.length() - 2, threshold.length()) + "%");
							builder.append("\n");
						}
						
						if(session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), builder.toString());
						}else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args+builder.toString());
						}
						
						if (bypassFrameResult.isFinished() == true) {
							// Return값 저장
							AsyncCommandResult asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(nextNum);
							asyncCommandResult.setResultType("RESULT_STATUS");
							asyncCommandResult.setResultValue(bypassFrameResult.getLastProcedure().name());
							acpd.add(asyncCommandResult);
							
							log.debug("["+bd.getCommand()+"] , Result["+session.getAttribute(bd.getCommand())+"]");
							
							asyncCommandResult = new AsyncCommandResult();
							asyncCommandResult.setMcuId(bd.getModemId());
							asyncCommandResult.setTrId(bd.getTransactionId());
							asyncCommandResult.setNum(++nextNum);
							asyncCommandResult.setResultType("RESULT_VALUE");
							asyncCommandResult.setResultValue(session.getAttribute(bd.getCommand()).toString());
							acpd.add(asyncCommandResult);
						}

					} catch (Exception e) {
						log.error("Procedure save error -" + e);
					}

					bypassTRStateChange(session);
				}
				
				txmanager.commit(txstatus);
			} catch (Exception e) {
				if (txstatus != null)
					txmanager.rollback(txstatus);
			}
		}

		log.debug("### [" + bd.getCommand() + "] Execute Bypass ok.");

		if (!bypassFrameResult.isResultState()) {
			bd.getFrameFactory().stop(session);
		}
	}

	@Override
	public CommandData executeBypassClient(byte[] frame, IoSession session) throws Exception {
		
		ServiceData sd = (ServiceData)session.getAttribute("ServiceData");
		
		if(sd == null) {
			return executeBypassClientByCommand(frame, session);
		} else {
			return executeBypassClientByDLMS(frame, session);
		}		
	}
	
	public CommandData executeBypassClientByCommand(byte[] frame, IoSession session) {
		
		ServiceData sd = (ServiceData)session.getAttribute("ServiceData");
		Object ns_obj = session.getAttribute("nameSpace");
		String ns = ns_obj != null ? (String) ns_obj : null;

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String cmd = bd.getCommand();
		log.debug("command : " + cmd);
		
		CommandData cd = (CommandData) CodecUtil.unpack(ns, CommandData.class.getName(), frame);
		
		cd.setNS(ns);

		OID oid = new OID("1.11.0");
		SMIValue smiValue = new SMIValue();
		smiValue.setOid(oid);

		FMPVariable mFMPVariable = DataUtil.getFMPVariableObject(ns, oid);
		
		try {
			if(sd instanceof CommandData) { 
				CommandData csd = (CommandData)sd;
				SMIValue[] smiValues = csd.getSMIValue();
				
				if(cmd.equals("cmdResetModem")
						|| cmd.equals("cmdFactorySetting")
						|| cmd.equals("cmdSetTime")
						|| cmd.equals("cmdSetModemResetInterval")
						|| cmd.equals("cmdSetMeteringInterval")
						|| cmd.equals("cmdSetServerIpPort")
						|| cmd.equals("cmdSetApn")
						|| cmd.equals("cmdUploadMeteringData")
						|| cmd.equals("cmdOndemandMetering")
						|| cmd.equals("cmdCurrentValuesMetering")
						) {
					
				} else if(cmd.equals("cmdOTAStart")) {
					log.info("### [" + cmd + "] Response params => " + smiValues.toString());
					log.debug("modemId[" + bd.getModemId() + "] meterId[" + bd.getMeterId() + "]");
					bd.setModemModel(smiValues[0].getVariable().toString());
					bd.setFwVersion(smiValues[1].getVariable().toString());
					bd.setBuildno(smiValues[2].getVariable().toString());
					bd.setHwVersion(smiValues[3].getVariable().toString());
					bd.setPacket_size(Integer.parseInt(smiValues[4].getVariable().toString()));
	
					cmdSendImage(session);
				} else if(cmd.equals("cmdSendImage")) {
					cmdSendImage(session);
				} else if (cmd.equals("cmdOTAEnd")) {
					
					long endTime = System.currentTimeMillis();
					ModemDao modemDao = DataUtil.getBean(ModemDao.class);
					Modem modem = modemDao.get(bd.getModemId());
					TargetClass tClass = TargetClass.valueOf(modem.getModemType().name());

					// 상태값을 받아서 실패하면 다시 시도하도록 한다.
					int status = Integer.parseInt(smiValues[0].getVariable().toString());
					
					log.debug("#### OTA 결과는 => " + (status == 0 ? "SUCCESS" : "ERROR" + ", ModemId=" + bd.getModemId() + ", TransactionID=" + bd.getTransactionId()));
					
					/*
					 * Modem 정보 갱신
					 */
					if (status == 0) {
						
						bd.setModemModel(smiValues[0].getVariable().toString());
						bd.setFwVersion(smiValues[1].getVariable().toString());
						bd.setBuildno(smiValues[2].getVariable().toString());
						bd.setHwVersion(smiValues[3].getVariable().toString());
						bd.setPacket_size(Integer.parseInt(smiValues[4].getVariable().toString()));

						modem.setFwVer(bd.getFwVersion());
						modem.setHwVer(bd.getHwVersion());						
						/*
						 * OTA 종료후 Event 저장
						 */
						//long endTime = System.currentTimeMillis();
						String elapseTime = DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime()));

						String openTime = DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss");
						EV_GV_evtOTADownloadEnd_Action action1 = new EV_GV_evtOTADownloadEnd_Action();
						action1.makeEvent(tClass, bd.getModemId(), tClass, openTime, elapseTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]", "HES");
						action1.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, "F/W Version=[" + bd.getFwVersion() + "]");

						EV_GV_evtOTADownloadResult_Action action2 = new EV_GV_evtOTADownloadResult_Action();
						action2.makeEvent(tClass, bd.getModemId(), tClass, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR, null, "HES");
						action2.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_NOERROR);
						
					} else {
						/*
						 * OTA 종료후 Event 저장
						 */
						//long endTime = System.currentTimeMillis();
						String elapseTime = DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime()));

						String openTime = DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss");
						EV_GV_evtOTADownloadEnd_Action action1 = new EV_GV_evtOTADownloadEnd_Action();
						action1.makeEvent(tClass, bd.getModemId(), tClass, openTime, elapseTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, "result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"), "HES");
						action1.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, "result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));

						EV_GV_evtOTADownloadResult_Action action2 = new EV_GV_evtOTADownloadResult_Action();
						action2.makeEvent(tClass, bd.getModemId(), tClass, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL, null, "HES");
						action2.updateOTAHistory(bd.getModemId(), DeviceType.Modem, openTime, OTA_UPGRADE_RESULT_CODE.OTAERR_TRN_FAIL);
					}

					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
					log.debug("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));

					log.debug("### [OTA FINISHED] Start = " + DateTimeUtil.getDateString(bd.getStartOTATime()) + ", Finished = " + DateTimeUtil.getDateString(endTime) + ", Elapse = " + (endTime - bd.getStartOTATime()) / 1000.0f + "s");
					
					StringBuilder builder = new StringBuilder();
					
					builder.append("#### [Upgrade Fininsed] Meter=" + bd.getMeterId() + ", Modem=" + bd.getModemId() + " F/W Upgrade finished. result = " + (status == 0 ? "SUCCESS" : "ERROR" + "!!!"));
					builder.append("### [OTA FINISHED] Start = " + DateTimeUtil.getDateString(bd.getStartOTATime()) + ", Finished = " + DateTimeUtil.getDateString(endTime) + ", Elapse = " + (endTime - bd.getStartOTATime()) / 1000.0f + "s");
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;

				} else if (cmd.equals("cmdGetMeterStatus")) {
					
					log.info("### [" + cmd + "] Response params => " + smiValues[0].toString());
					log.info("### [" + cmd + "] Response params => " + Hex.decode(smiValues[0].getVariableByte()));

					BYTE byteValue = (BYTE) smiValues[0].getVariable();
					
					/*
					: 1-ON, 0-Off
					LSB
					0 : L1 relay status
					1 : L2 relay status
					2 : L3 relay status
					3 : External relay status
					4 : L1 relay error
					5 : L2 relay error
					6 : L3 relay error
					7 : External relay error
					8 : Open terminal cover
					9 : Open terminal cover in power off
					10: Open top cover
					11: Open top cover in power off
					12: Magnetic detection 1
					13:Magnetic detection 2
					14: Program
					15: Factory status
					MSB
					*/

					boolean status = false;
					log.debug("## [cmdGetMeterStatus] METER STATUS ==> " + Integer.toBinaryString(byteValue.getValue()));

					if ((byteValue.getValue() & 0x01) == 1 || (byteValue.getValue() & 0x02) == 1 || (byteValue.getValue() & 0x04) == 1) {
						status = true; // Relay Close 상태. 전류가 흐르는 상태. 정상
					}
					if ((byteValue.getValue() & 0x01) == 0 && (byteValue.getValue() & 0x02) == 1 && (byteValue.getValue() & 0x04) == 0) {
						status = false; // Relay Open 상태. 전류가 흐르지 않는 상태
					}
					if (byteValue.getValue() == 0) {
						status = false; // Relay Open 상태. 전류가 흐르지 않는 상태
					}
					
					/**
					 * 결과값 분석
					 */
					String statusString = DLMSTable.getOBIS_FUNCTION_STATUS((OCTET) new OCTET(smiValues[0].encode()));
					log.debug("## [cmdGetMeterStatus] METER STATUS string ==> " + statusString);
					
					/**
					 * 미터 상태 변경
					 */
					setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status ? 0 : 1);
					
					StringBuilder builder = new StringBuilder();					
					builder.append("METER STATUS : ");
					builder.append(statusString);
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
					
				} else if (cmd.equals("cmdReadModemEventLog")) {
					
					log.info("### [" + cmd + "] Response params => " + smiValues[0].toString());
					log.info("### [" + cmd + "] Response params => " + Hex.decode(smiValues[0].getVariableByte()));

					byte[] bb = smiValues[0].getVariableByte();
					int totalEventCount = 0;
					StringBuilder builder = new StringBuilder();
					
					if (bb != null && 0 < bb.length) {
						/*  Event List Format */
						byte[] count = new byte[2];
						byte[] index = new byte[2];
						byte[] time = new byte[7];
						byte[] code = new byte[2];
						byte[] value = new byte[4];

						int pos = 0;
						System.arraycopy(bb, pos, count, 0, count.length);
						pos += count.length;

						totalEventCount = DataUtil.getIntTo2Byte(count);

						for (int i = 0; i < totalEventCount; i++) {
							System.arraycopy(bb, pos, index, 0, index.length);
							pos += index.length;

							System.arraycopy(bb, pos, time, 0, time.length);
							pos += time.length;

							System.arraycopy(bb, pos, code, 0, code.length);
							pos += code.length;

							System.arraycopy(bb, pos, value, 0, value.length);
							pos += value.length;

							log.debug("EventLogData Total: " + totalEventCount + " Index:" + DataUtil.getIntTo2Byte(index) + " , Time:" + DataUtil.getTimeStamp(time) + ", Code:" + Hex.decode(code) + ", Value:" + Hex.decode(value));

							/*
							 * eventAlertName : 이벤트 알람 명 
							 * activatorType: 이벤트 소스(MCU, Meter, Modem...) 
							 * activatorId : 이벤트 소스 아이디 (MCU 아이디, 계량기 아이디...) 
							 * timestamp : 이벤트 발생 시간 
							 * eventClassId : 이벤트 종류 
							 * attrs : 속성 new String[][] {{"meterID", "2039293293"},{"mcuID", "20392"}}
							 */
							String eventAlertName = Constant_GV.MIUEventCode.getItem(code).name();
							String eventAlertValue = "";
							
							switch (MIUEventCode.getItem(code)) {
							case BOOT_UP:
								int resetCount = DataUtil.getIntToBytes(new byte[] { value[0], value[1] });
								eventAlertValue = MIUEventCode_BOOT_UP.getItem(new byte[] { value[2], value[3] }).getDesc() + " / Reset Count = " + resetCount;
								break;
							case MODEM_RESET:
								eventAlertValue = MIUEventCode_MODEM_RESET.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
								break;
							case CLI_STATUS:
								eventAlertValue = MIUEventCode_CLI_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
								break;
							case OTA_STATUS:
								eventAlertValue = MIUEventCode_OTA_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
								break;
							case PPP_STATUS:
								eventAlertValue = MIUEventCode_PPP_STATUS.getItem(DataUtil.getIntTo4Byte(value)).getDesc();
								break;
							default: // Value값이 없는 코드들.
								eventAlertValue = MIUEventCode.getItem(code).getDesc();
								break;
							}

							log.debug("## ModemID = " + bd.getModemId() + ", EventAlertName=" + eventAlertName + ", EventAlertValue=" + eventAlertValue);

							/**
							 * Event Alarm Log 저장.
							 */
							MeterDao meterDao = DataUtil.getBean(MeterDao.class);
							Meter meter = meterDao.get(bd.getMeterId());

							EventAlertLog event = new EventAlertLog();
							event.setActivatorId(bd.getModemId());
							event.setActivatorType(TargetClass.Modem);
							event.append(EventUtil.makeEventAlertAttr("modemID", "java.lang.String", bd.getModemId()));
							event.append(EventUtil.makeEventAlertAttr("message", "java.lang.String", DataUtil.getIntTo2Byte(index) + ". [" + eventAlertName + "]" + eventAlertValue));
							event.setSupplier(meter.getSupplier());

							try {
								EventUtil.sendEvent("Modem Event Log", TargetClass.Modem, bd.getModemId(), DataUtil.getTimeStamp(time), new String[][] {}, event);
							} catch (Exception e) {
								log.error("Send Event Error - " + e);
							}
												
							builder.append("modemID : " + bd.getModemId());
							builder.append("message : " + DataUtil.getIntTo2Byte(index) + ". [" + eventAlertName + "]" + eventAlertValue);
							builder.append("\n");

						} // end for
						
						String result = builder.toString();
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
					}
				}
				/*
				 * 미터가젯쪽 에서 구현되어있는 Command 5개 
				 * 1. cmdOndemandMetering 
				 * 2. cmdRelayDisconnect 
				 * 3. cmdRelayReconnect 
				 * 4. cmdSetMeterTime 
				 * 5. cmdRelayStatus
				 */
				else if (cmd.equals("cmdOndemandMetering")) { /* 검침서버로 Upload */
					// 검침 서버로 Upload하기때문에 구현할 필요 없음.
					log.debug("######### 이거 안찍힐껄?????? ####");
					
					StringBuilder builder = new StringBuilder();					
					builder.append("RESULT : ");
					builder.append("Success");
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
					
				} else if (cmd.equals("cmdRelayDisconnect")) {
					
					int status = Integer.parseInt(smiValues[0].getVariable().toString());					
					setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status == ActionResultStatus.SUCCESS.getCode() ? 0 : 1);
					
					StringBuilder builder = new StringBuilder();
					builder.append("MODEM ID : " + bd.getModemId());
					builder.append("RESULT_DISCONNECT : ");
					builder.append(ActionResultStatus.getItem(status).name());
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
					
				} else if (cmd.equals("cmdRelayReconnect")) {
					
					int status = Integer.parseInt(smiValues[0].getVariable().toString());
					setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status == ActionResultStatus.SUCCESS.getCode() ? 0 : 1);
					
					StringBuilder builder = new StringBuilder();
					builder.append("MODEM ID : " + bd.getModemId());
					builder.append("RESULT_RECONNECT : ");
					builder.append(ActionResultStatus.getItem(status).name());
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
					
				} else if (cmd.equals("cmdSetMeterTime")) {
					int status = Integer.parseInt(smiValues[0].getVariable().toString());
					
					setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status == ActionResultStatus.SUCCESS.getCode() ? 0 : 1);
					
					StringBuilder builder = new StringBuilder();
					builder.append("MODEM ID : " + bd.getModemId());
					builder.append("RESULT_VALUE : ");
					builder.append(ActionResultStatus.getItem(status).name());
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
				} else if (cmd.equals("cmdRelayStatus")) { // Get Relay Status
					// True = Connected, False = Disconnected
					boolean status = Boolean.parseBoolean(smiValues[0].getVariable().toString());
					// 미터 상태변경
					setMeterStatus(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId(), status ? 0 : 1);
					
					StringBuilder builder = new StringBuilder();
					builder.append("MODEM ID : " + bd.getModemId());
					builder.append("RESULT_STATUS : ");
					builder.append(status ? "Connected" : "Disconnected");
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
				}		
				
			} else if(sd instanceof EventData_1_2) {
				
				log.debug("#### EventData_1_2");
				
				if(bd.getCommand().equals("cmdReadModemConfiguration")) {
					EventProcessor_1_2 ep_1_2 = DataUtil.getBean(EventProcessor_1_2.class);
					CommLog commLog = new CommLog();
	                ep_1_2.processing(sd, commLog);
	                
	                StringBuilder builder = new StringBuilder();					
					builder.append("RESULT : ");
					builder.append("Success");
					
					String result = builder.toString();
					mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

					smiValue.setVariable(mFMPVariable);
					cd.setErrCode(new BYTE(0));
					cd.append(smiValue);
					
					return cd;
				}				
	        } 
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public CommandData executeBypassClientByDLMS(byte[] frame, IoSession session) {
		
		log.debug("#### Receive Bypass Frame ===> " + Hex.decode(frame));
		
		Object ns_obj = session.getAttribute("nameSpace");
		String ns = ns_obj != null ? (String) ns_obj : null;

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		JpaTransactionManager txmanager = (JpaTransactionManager) DataUtil.getBean("transactionManager");
		TransactionStatus txstatus = null;
		
		try {
			
			BypassFrameResult bypassFrameResult = bd.getFrameFactory().receiveBypass(session, frame);
			log.debug("###[" + bd.getCommand() + "] Execute Bypass ===> " + bypassFrameResult.toString());
			
			if (!bypassFrameResult.isResultState()) {
				log.debug("FAIL : " + bd.getCommand());
				return null;
			}
			
			if (bypassFrameResult.isResultState()) {
				if (bypassFrameResult.isFinished()) {
					CommandData cd = (CommandData) CodecUtil.unpack(ns, CommandData.class.getName(), frame);
					
					cd.setNS(ns);

					OID oid = new OID("1.11.0");
					SMIValue smiValue = new SMIValue();
					smiValue.setOid(oid);

					FMPVariable mFMPVariable = DataUtil.getFMPVariableObject(ns, oid);
					
					if ("cmdMeterOTAStart".equals(bd.getCommand())) {
						
						String args = session.getAttribute(bd.getCommand()).toString();
						String lastArgs = getMeterOTAString(bypassFrameResult, bd);
						log.debug("### cmdMeterOTAStart RESULT[" + args + lastArgs + "]");
						
						if(bypassFrameResult.getLastProcedure() == Procedure.GET_FIRMWARE_VERSION) {
							
							try {
								
								txstatus = txmanager.getTransaction(null);
								
								/*
								 * 미터 FW정보 UPDATE
								 */
								String fwVersion = (String) bypassFrameResult.getResultValue();
								log.debug("F/W Version = " + fwVersion + " OTA Finished.");

								MeterDao meterDao = DataUtil.getBean(MeterDao.class);
								Meter meter = meterDao.get(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId());
								meter.setSwVersion(fwVersion);
								
								txmanager.commit(txstatus);
								
							} catch(Exception e) {
								if (txstatus != null)
									txmanager.rollback(txstatus);
							}
						}

						StringBuilder builder = new StringBuilder();
						builder.append(args);
						builder.append(lastArgs);

						String result = builder.toString();

						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);
						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);

						return cd;
						
					} else if (bd.getCommand().equals("cmdGetMeterFWVersion")) {
						
						String fwVersion = (String) bypassFrameResult.getResultValue();
						
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append(fwVersion);
						
						String result = builder.toString();
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						try {
							txstatus = txmanager.getTransaction(null);
							log.debug("F/W Version = " + fwVersion);

							MeterDao meterDao = DataUtil.getBean(MeterDao.class);
							Meter meter = meterDao.get(((BypassDevice) session.getAttribute(session.getRemoteAddress())).getMeterId());
							meter.setSwVersion(fwVersion);
							
							txmanager.commit(txstatus);
							
						} catch(Exception e) {
							if (txstatus != null)
								txmanager.rollback(txstatus);
						}
						
						return cd;
						
					} else if (bd.getCommand().equals("cmdAlarmReset")) {
						
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();

						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
						
					} else if (bd.getCommand().equals("cmdGetBillingCycle")) {
						
						@SuppressWarnings("unchecked")
						HashMap<String, String> resultValue = (HashMap<String, String>) bypassFrameResult.getResultValue();
						
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append(resultValue.get("time") + " / " + resultValue.get("day"));
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
						
					} else if (bd.getCommand().equals("cmdSetBillingCycle")) {
						
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
					} else if ("cmdGetBillingCycle".equals(bd.getCommand())) {
						@SuppressWarnings("unchecked")
						HashMap<Object, Object> resultMap = (HashMap<Object, Object>) bypassFrameResult.getResultValue();

						StringBuilder builder = new StringBuilder();
						builder.append(resultMap.get("time").toString());
						builder.append(",");
						builder.append(resultMap.get("day").toString());

						String result = builder.toString();

						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);
						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);

						return cd;						
					} else if (bd.getCommand().equals("cmdDemandPeriod")) {
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
						
					} else if (bd.getCommand().equals("cmdGetDemandPeriod")) {

						String args = session.getAttribute(bd.getCommand()).toString();
						String lastArgs = getDemandPeriodString(bypassFrameResult);
						log.debug("### CmdGetDemandPeriod RESULT[" + args + lastArgs + "]");

						StringBuilder builder = new StringBuilder();
						builder.append(args);
						builder.append(lastArgs);

						String result = builder.toString();

						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);
						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);

						return cd;						
					} else if (bd.getCommand().equals("cmdTOUSet")) {
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
						
					} else if (bd.getCommand().equals("cmdTOUGet")) {
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
					} else if (bd.getCommand().equals("cmdSetCurrentLoadLimit")) {
						StringBuilder builder = new StringBuilder();
						builder.append("RESULT_STATUS : ");						
						builder.append(bypassFrameResult.getLastProcedure().name());
						builder.append("RESULT_VALUE : ");						
						builder.append("Success");
						
						String result = builder.toString();
						
						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);

						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);
						
						return cd;
					} else if (bd.getCommand().equals("cmdGetCurrentLoadLimit")) {
						String args = session.getAttribute(bd.getCommand()).toString();
						String lastArgs = getCurrentLoadLimitString(bypassFrameResult);
						log.debug("### cmdGetCurrentLoadLimit RESULT[" + args + lastArgs + "]");

						StringBuilder builder = new StringBuilder();
						builder.append(args);
						builder.append(lastArgs);

						String result = builder.toString();

						mFMPVariable.decode(ns, result.getBytes(), 0, result.getBytes().length);
						smiValue.setVariable(mFMPVariable);
						cd.setErrCode(new BYTE(0));
						cd.append(smiValue);

						return cd;						
					}
					
				} else {
					
					if("cmdMeterOTAStart".equals(bd.getCommand())) {
						if (session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), getMeterOTAString(bypassFrameResult, bd));
						} else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args + getMeterOTAString(bypassFrameResult, bd));
						}
					} else if ("cmdGetDemandPeriod".equals(bd.getCommand())) {

						if (session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), getDemandPeriodString(bypassFrameResult));
						} else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args + getDemandPeriodString(bypassFrameResult));
						}
					} else if ("cmdTOUGet".equals(bd.getCommand())) {
					
						if (session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), getTOUResultString(bypassFrameResult));
						} else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args + getTOUResultString(bypassFrameResult));
						}
					} else if ("cmdGetCurrentLoadLimit".equals(bd.getCommand())) {
						
						if (session.getAttribute(bd.getCommand()) == null) {
							session.setAttribute(bd.getCommand(), getCurrentLoadLimitString(bypassFrameResult));
						} else {
							String args = session.getAttribute(bd.getCommand()).toString();
							session.setAttribute(bd.getCommand(), args + getCurrentLoadLimitString(bypassFrameResult));
						}
					} else {
						return null;
					}
				}
			} // end bypassFrame
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		return null;
	}
	
	public String getDemandPeriodString(BypassFrameResult bypassFrameResult) {
		StringBuilder builder = new StringBuilder();
		
		if (bypassFrameResult.getLastProcedure() == Procedure.ACTION_IMAGE_ACTIVATE) {
			
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_PERIOD.name()) != null) {
				builder.append("demand +A Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_NUMBER.name()) != null) {
				builder.append("demand +A Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_A_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_PERIOD.name()) != null) {
				builder.append("demand -A Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_NUMBER.name()) != null) {
				builder.append("demand -A Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_A_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_PERIOD.name()) != null) {
				builder.append("demand +R Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_NUMBER.name()) != null) {
				builder.append("demand +R Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_R_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_PERIOD.name()) != null) {
				builder.append("demand -R Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_NUMBER.name()) != null) {
				builder.append("demand -R Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_R_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_PERIOD.name()) != null) {
				builder.append("demand R(QI) Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_NUMBER.name()) != null) {
				builder.append("demand R(QI) Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QI_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_PERIOD.name()) != null) {
				builder.append("demand R(QIV) Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_NUMBER.name()) != null) {
				builder.append("demand R(QIV) Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_R_QIV_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_PERIOD.name()) != null) {
				builder.append("demand+ Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_NUMBER.name()) != null) {
				builder.append("demand+ Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_PLUS_NUMBER.name()));
				builder.append("\n");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_PERIOD.name()) != null) {
				builder.append("demand- Period : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_PERIOD.name()));
				builder.append(" , ");
			}
			if(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_NUMBER.name()) != null) {
				builder.append("demand- Number : ");
				builder.append(bypassFrameResult.getResultValue(Procedure.GET_DEMAND_MINUS_NUMBER.name()));
				builder.append("\n");
			}			
		}
		
		return builder.toString();
	}
	
	
	public String getMeterOTAString(BypassFrameResult bypassFrameResult, BypassDevice bd) {
		StringBuilder builder = new StringBuilder();
		
		if (bypassFrameResult.getLastProcedure() == Procedure.ACTION_IMAGE_ACTIVATE) {
			
			long endTime = System.currentTimeMillis();
			
			builder.append("RESULT_STATUS : ");
			builder.append(bypassFrameResult.getLastProcedure().name());
			builder.append(" , ");
			
			builder.append("RESULT_VALUE : ");
			builder.append((String) bypassFrameResult.getResultValue());
			builder.append(" , ");
			
			builder.append("RESULT_OTA_START : ");
			builder.append(DateTimeUtil.getDateString(bd.getStartOTATime()));
			builder.append(" , ");
			
			builder.append("RESULT_OTA_END : ");
			builder.append(bypassFrameResult.getLastProcedure().name());
			builder.append(" , ");
			
			builder.append("RESULT_OTA_ELAPSE : ");
			builder.append(DateTimeUtil.getElapseTimeToString((endTime - bd.getStartOTATime())));
			builder.append("/n");
			
		}
		
		return builder.toString();
	}
	
	public String getTOUResultString(BypassFrameResult bypassFrameResult) {
		
		StringBuilder builder = new StringBuilder();
		
		if(bypassFrameResult.getResultValue(Procedure.GET_CALENDAR_NAME_PASSIVE.name()) != null) {			
			builder.append("[CALENDAR NAME PASSIVE] - ");
			builder.append((String)bypassFrameResult.getResultValue(Procedure.GET_CALENDAR_NAME_PASSIVE.name()));
			builder.append("\n\n");
		}
		
		if(bypassFrameResult.getResultValue(Procedure.GET_SEASON_PROFILE.name()) != null) {			
			builder.append("[SEASON PROFILE] \n");
			
			List<HashMap<String, String>> list 
				= (ArrayList<HashMap<String, String>>)bypassFrameResult.getResultValue(Procedure.GET_SEASON_PROFILE.name()) ;
			
			for(int i = 0; i < list.size(); i++) {
				HashMap<String, String> map = (HashMap<String, String>)list.get(i);
				
				if ((String)map.get("SeasonProfileName") != null) {
					builder.append("SeasonProfileName : ");
					builder.append((String)map.get("SeasonProfileName"));
					builder.append(" , ");
				}
				
				if ((String)map.get("SeasonStart") != null) {
					
					builder.append("SeasonStart : ");
					
					String strDate = (String)map.get("SeasonStart");
					String yy = "XXXX";
					String mm = strDate.substring(4, 6);
					String dd = strDate.substring(6, 8);
					String hh = strDate.substring(8, 10);
					String ii = strDate.substring(10, 12);
					String ss = strDate.substring(12, 14);
					String strMaskDate = mm + "-" + dd + "-" + yy + " " + hh + ":" +  ii + ":" + ss;
					
					builder.append(strMaskDate);
					builder.append(" , ");
				}
				
				if ((String)map.get("SeasonWeekName") != null) {
					builder.append("SeasonWeekName : ");
					builder.append((String)map.get("SeasonWeekName"));
					builder.append("\n");
				}
			}
			builder.append("\n");
		}
		
		
		if(bypassFrameResult.getResultValue(Procedure.GET_WEEK_PROFILE.name()) != null) {			
			builder.append("[WEEK PROFILE TABLE] \n");
			
			String[] weekName = {"MON","TUE","WED","THD","FRI","SAT","SUN"};
			
			List<HashMap<String, String>> list 
				= (ArrayList<HashMap<String, String>>)bypassFrameResult.getResultValue(Procedure.GET_WEEK_PROFILE.name()) ;
			for(int i = 0; i < list.size(); i++) {
				HashMap<String, String> map = (HashMap<String, String>)list.get(i);
				
				if ((String)map.get("WeekProfileName") != null) {
					builder.append("WeekProfileName : ");
					builder.append((String)map.get("WeekProfileName"));					
				}
				
				for(int w = 0; w < weekName.length; w++) {
					if ((String)map.get(weekName[w]) != null) {
						builder.append(" , ");
						builder.append(weekName[w] + " : ");
						builder.append((String)map.get(weekName[w]));						
					}	
				}
				
				builder.append("\n");
			}
			
			builder.append("\n");
		}
		
		if(bypassFrameResult.getResultValue(Procedure.GET_DAY_PROFILE.name()) != null) {			
			
			builder.append("[DAY PROFILE TABLE] \n");
			List<HashMap<String, String>> list 
				= (ArrayList<HashMap<String, String>>)bypassFrameResult.getResultValue(Procedure.GET_DAY_PROFILE.name()) ;
			
			for(int i = 0; i < list.size(); i++) {
				
				HashMap<String, String> map = (HashMap<String, String>)list.get(i);
				
				if ((String)map.get("dayid") != null) {
					builder.append("day id : ");
					builder.append((String)map.get("dayid"));
					builder.append(" , ");
				}
				
				if ((String)map.get("StartTime") != null) {					
					builder.append("StartTime : ");
					builder.append((String)map.get("StartTime"));
					builder.append(" , ");
				}
				
				if ((String)map.get("ScriptLogicalName") != null) {
					builder.append("ScriptLogicalName : ");
					builder.append((String)map.get("ScriptLogicalName"));
					builder.append(" , ");
				}
				
				if ((String)map.get("timeBucketNo") != null) {
					builder.append("timeBucketNo : ");
					builder.append((String)map.get("timeBucketNo"));
					builder.append("\n");
				}
			}
			
			builder.append("\n");
		}
		
		if(bypassFrameResult.getResultValue(Procedure.GET_STARTING_DATE.name()) != null) {			
			builder.append("[TOU STARTING DATE] - ");
			
			String strDate = (String)bypassFrameResult.getResultValue(Procedure.GET_STARTING_DATE.name());
			String yy = strDate.substring(0, 4);
			String mm = strDate.substring(4, 6);
			String dd = strDate.substring(6, 8);
			String hh = strDate.substring(8, 10);
			String ii = strDate.substring(10, 12);
			String ss = strDate.substring(12, 14);
			String strMaskDate = mm + "-" + dd + "-" + yy + " " + hh + ":" +  ii + ":" + ss;
			
			builder.append(strMaskDate);
			builder.append("\n");			
		}
		
		return builder.toString();
	}
	
	public String getCurrentLoadLimitString(BypassFrameResult bypassFrameResult) {
		StringBuilder builder = new StringBuilder();							
		if(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME.name()) != null) {
			builder.append("Duration judge time : ");
			builder.append(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME.name()));
			builder.append("s, ");
		}

		if(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD.name()) != null) {
			String threshold = String.valueOf(bypassFrameResult.getResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD.name()));
			builder.append("Threshold : ");
			builder.append(threshold.substring(0, threshold.length() - 2) + "." + threshold.substring(threshold.length() - 2, threshold.length()) + "%");
			builder.append("\n");
		}
		return builder.toString();
	}
	
	private void bypassTRStateChange(IoSession session) {
		// 비동기 내역 수정
		try {
			AsyncCommandLogDao acld = null;
			Set<Condition> condition = null;
			BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

			//			txstatus = txmanager.getTransaction(null);
			acld = DataUtil.getBean(AsyncCommandLogDao.class);
			condition = new HashSet<Condition>();
			condition.add(new Condition("deviceId", new Object[] { bd.getModemId() }, null, Restriction.EQ));
			condition.add(new Condition("id.trId", new Object[] { bd.getTransactionId() }, null, Restriction.EQ));
			condition.add(new Condition("state", new Object[] { TR_STATE.Running.getCode() }, null, Restriction.EQ));
			// orderby 날짜로
			List<AsyncCommandLog> acLogList = acld.findByConditions(condition);
			log.debug("ASYNC_SIZE[" + acLogList.size() + "]");

			if (acLogList.size() > 0) {
				for (int i = 0; i < acLogList.size(); i++) {
					AsyncCommandLog acl = acLogList.get(i);
					acl.setState(TR_STATE.Success.getCode());
					acld.update(acl);
				}

				bd.setTrState(TR_STATE.Success);
			}
			//			txmanager.commit(txstatus);
		} catch (Exception e) {
			//			if (txstatus != null)
			//				txmanager.rollback(txstatus);
		}
	}

	/*
	 * OTA 시작 명령
	 */
	public void cmdOTAStart(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdOTAStart] Action - Start : " + DateTimeUtil.getDateString(startTime));

		// ota 비동기 이력의 인자에서 파일 경로를 가져온다.
		ByteArrayOutputStream out = null;
		FileInputStream in = null;
		try {
			BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
			bd.setStartOTATime(startTime);
			String fw_path = (String)bd.getArgMap().get("fw_path");
			Path filePath = Paths.get(fw_path);
			File file = filePath.toFile();

			//			
			//			BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
			//			// BypassDevice의 args에서 파일명을 가져와야 한다.
			//			File file = new File((String) bd.getArgs().get(0));
			out = new ByteArrayOutputStream();
			in = new FileInputStream(file);

			int len = 0;
			byte[] b = new byte[1024];
			while ((len = in.read(b)) != -1) {
				out.write(b, 0, len);
			}

			long filelen = file.length();
			// sendImage 에서 사용하기 위해 바이너리를 전역 변수에 넣는다.
			bd.setFw_bin(out.toByteArray());
			// sendImage에서 바이너리를 읽어올 수 있도록 하기 위해 전역 변수에 넣는다.
			bd.setFw_in(new ByteArrayInputStream(bd.getFw_bin(), 0, bd.getFw_bin().length));
			// int crc = DataUtil.getIntTo2Byte(FrameUtil.getCRC(bd.getFw_bin()));
			byte[] crc = CRCUtil.Calculate_ZigBee_Crc(bd.getFw_bin(), (char) 0x0000);
			DataUtil.convertEndian(crc);

			String ns = (String) session.getAttribute("nameSpace");
			List<SMIValue> params = new ArrayList<SMIValue>();

			params.add(DataUtil.getSMIValueByObject(ns, "cmdModemFwImageLength", Long.toString(filelen)));
			params.add(DataUtil.getSMIValueByObject(ns, "cmdModemFwImageCRC", Integer.toString(DataUtil.getIntTo2Byte(crc))));
			sendCommand(session, "cmdOTAStart", params);
		} finally {
			if (out != null)
				out.close();
			if (in != null)
				in.close();
		}
	}

	/*
	 * 펌웨어 바이너리를 보내는 명령
	 * 한 패킷 전송 후 응답을 받고 다음 패킷을 보내야 하므로 offset과 fw_in이 전역 변수로 선언되었다.
	 */
	public void cmdSendImage(IoSession session) throws Exception {
		log.debug("## [cmdSendImage] Action");

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		log.debug("1. packetSize[" + bd.getPacket_size() + "]");
		log.debug("1. offset[" + bd.getOffset() + "]");
		String ns = (String) session.getAttribute("nameSpace");
		byte[] b = new byte[bd.getPacket_size()];
		int len = -1;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if ((len = bd.getFw_in().read(b)) != -1) {
			out.write(b, 0, len);
			log.debug("## Ssend Image ==> " + Hex.decode(b));
			List<SMIValue> params = new ArrayList<SMIValue>();
			params.add(DataUtil.getSMIValueByObject(ns, "cmdImageAddress", Integer.toString(bd.getOffset())));
			params.add(DataUtil.getSMIValueByObject(ns, "cmdImageSize", Integer.toString(len)));
			// 바이트 스트립을 문자열로 변환 후 바이트로 변환시 원본이 손상되어 STREAM을 바로 사용하도록 한다.
			params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdImageData"), new STREAM(out.toByteArray())));
			bd.setOffset(bd.getOffset() + len);
			sendCommand(session, "cmdSendImage", params);
		}
		log.debug("2. offset[" + bd.getOffset() + "] / total = " + bd.getFw_bin().length);
		out.close();

		// 전송이 끝나면 종료 명령을 보낸다.
		if (bd.getOffset() == bd.getFw_bin().length) {
			bd.getFw_in().close();
			sendCommand(session, "cmdOTAEnd", null);
		}
	}

	/*
	 * Meter FW OTA 시작 명령
	 */
	public void cmdMeterOTAStart(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdMeterOTAStart] Action : " + DateTimeUtil.getDateString(startTime));

		/*
		 * F/W Image 준비
		 */
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		bd.setStartOTATime(startTime);
		String fw_path = (String)bd.getArgMap().get("fw_path");
		Path filePath = Paths.get(fw_path);
		byte[] fileArray = Files.readAllBytes(filePath);

		boolean takeover = false;
		if (bd.getArgMap().containsKey("take_over")) {
			try {
				takeover = Boolean.parseBoolean((String)bd.getArgMap().get("take_over"));
			} catch (Exception e) {
			}
		}

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "0");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		HashMap<String, Object> params = new HashMap<String, Object>();
		String fileName = filePath.getFileName().toString();
		params.put("image_identifier", fileName.substring(0, fileName.lastIndexOf(".")));
		params.put("image", fileArray);
		params.put("takeover", takeover);

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdMeterOTAStart");
		gdFactory.setParam(params);

		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);

	}

	/*
	 * Meter FW Version 갱신
	 */
	public void cmdGetMeterFWVersion(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdGetMeterFWVersion] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdGetMeterFWVersion");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}

	/*
	 * Meter Alarm Reset
	 */
	public void cmdAlarmReset(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdAlarmReset] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdAlarmReset");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}

	/*
	 * Billing Cycle Information
	 */
	public void cmdGetBillingCycle(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdGetBillingCycle] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdGetBillingCycle");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}

	/*
	 * Billing Cycle Setting
	 */
	public void cmdSetBillingCycle(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdSetBillingCycle] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String time = (String)bd.getArgMap().get("time");
		String day = (String)bd.getArgMap().get("day");

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("time", time);
		params.put("day", day);

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdSetBillingCycle");
		bd.setFrameFactory(gdFactory);

		gdFactory.setParam(params);
		gdFactory.start(session, null);
	}
	
	/*
	 * Get Current Load Limit
	 */
	public void cmdGetCurrentLoadLimit(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdGetCurrentLoadLimit] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdGetCurrentLoadLimit");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}

	/*
	 * Set Current Load Limit
	 */
	public void cmdSetCurrentLoadLimit(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdSetCurrentLoadLimit] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String judgeTime = (String)bd.getArgMap().get("judgeTime");
		String threshold = (String)bd.getArgMap().get("threshold");

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("judgeTime", judgeTime);
		params.put("threshold", threshold);

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdSetCurrentLoadLimit");
		bd.setFrameFactory(gdFactory);

		gdFactory.setParam(params);
		gdFactory.start(session, null);
	}

	/*
	 * Demand Period
	 */
	public void cmdDemandPeriod(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdDemandPeriod] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String period = (String)bd.getArgMap().get("period");
		String number = (String)bd.getArgMap().get("numberOfPeriod");

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("period", period);
		params.put("number", number);

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdDemandPeriod");
		bd.setFrameFactory(gdFactory);

		gdFactory.setParam(params);
		gdFactory.start(session, null);
	}
	
	/*
	 * TOU 세팅
	 */
	public void cmdTOUSet(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdTOUSet] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String calendarNamePassive = (String)bd.getArgMap().get("calendarNamePassive");
		String startingDate = (String)bd.getArgMap().get("startingDate");
		String seasonFilePath = (String)bd.getArgMap().get("seasonFile");
		String weekFilePath = (String)bd.getArgMap().get("weekFile");
		String dayFilePath = (String)bd.getArgMap().get("dayFile");

		
		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "0");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */
		HashMap<String, Object> params = new HashMap<String, Object>();
		
		/*
		 * Calendar Name Passive
		 */
		if(calendarNamePassive != null && !calendarNamePassive.equals("")){
			params.put("calendarNamePassive", calendarNamePassive);
			log.debug("[Calendar Name Passive] ==> " + calendarNamePassive);
		}
		
		/*
		 * TOU Starting Date
		 */
		if(startingDate != null && !startingDate.equals("")){
			params.put("startingDate", startingDate);
			log.debug("[Starting Date] ==> " + startingDate);
		}
		
		/*
		 * Seasson Profile
		 */
		if(seasonFilePath != null && !seasonFilePath.equals("")){
			try {
				Path path = Paths.get(seasonFilePath);
				Source xmlSource = new StreamSource(path.toFile());
				
				JAXBContext jaxbContext = JAXBContext.newInstance(SeasonProfileTableFactory.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				JAXBElement<SeasonProfileTable> unmarshalledObject = (JAXBElement<SeasonProfileTable>) unmarshaller.unmarshal(xmlSource, SeasonProfileTable.class);
				
				SeasonProfileTable seasonProfileTable = unmarshalledObject.getValue();
				log.debug("[Season Profile Table] ==> " + seasonProfileTable.getRecordList().toString());
				params.put("seasonProfileTable", seasonProfileTable);				
			} catch (JAXBException e) {
				log.error("Season Profile Table Parsing Error - " + e);
			}
		}
		
		/*
		 * Week Profile
		 */
		if(weekFilePath != null && !weekFilePath.equals("")){
			try {
				Path path = Paths.get(weekFilePath);
				Source xmlSource = new StreamSource(path.toFile());
				
				JAXBContext jaxbContext = JAXBContext.newInstance(WeekProfileTableFactory.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				JAXBElement<WeekProfileTable> unmarshalledObject = (JAXBElement<WeekProfileTable>) unmarshaller.unmarshal(xmlSource, WeekProfileTable.class);
				
				WeekProfileTable weekProfileTable = unmarshalledObject.getValue();
				log.debug("[Week Profile Table] ==> " + weekProfileTable.getRecordList().toString());
				params.put("weekProfileTable", weekProfileTable);
			} catch (JAXBException e) {
				log.error("Week Profile Table Parsing Error - " + e);
			}
		}
		
		/*
		 * Day Profile
		 */
		if(dayFilePath != null && !dayFilePath.equals("")){
			try {
				Path path = Paths.get(dayFilePath);
				Source xmlSource = new StreamSource(path.toFile());
				
				JAXBContext jaxbContext = JAXBContext.newInstance(DayProfileTableFactory.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				JAXBElement<DayProfileTable> unmarshalledObject = (JAXBElement<DayProfileTable>) unmarshaller.unmarshal(xmlSource, DayProfileTable.class);
				
				DayProfileTable dayProfileTable = unmarshalledObject.getValue();
				log.debug("[Day Profile Table] ==> " + dayProfileTable.getRecordList().toString());				
				params.put("dayProfileTable", dayProfileTable);
			} catch (JAXBException e) {
				log.error("Day Profile Table Parsing Error - " + e);
			}
		}
		
		if(0 < params.size()){
			BypassFrameFactory gdFactory = new BypassEVNFactory("cmdTOUSet");
			gdFactory.setParam(params);

			bd.setFrameFactory(gdFactory);

			gdFactory.start(session, null);			
		}else{
			log.error("TOU Parameter is null...");
		}
	}
	
	/*
	 * TOU GET
	 */
	public void cmdTOUGet(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdTOUGet] Action : " + DateTimeUtil.getDateString(startTime));

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());

		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);

		/*
		 * 2. HDLC Connction.
		 */

		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdTOUGet");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}
	
	//    public void cmdGetMaxDemand(IoSession session) throws Exception
	//    {
	//        cmd = "cmdGetMaxDemand";
	//        
	//        cmdSetBypassStart(session);
	//        
	//        byte[][] req = KamstrupCIDMeta.getRequest(new String[]{"GetRegister","1326"});
	//        log.debug("REQ[" + Hex.decode(req[0]) + "] VAL[" + Hex.decode(req[1]) + "]");
	//        session.write(KamstrupCIDMeta.makeKmpCmd(req[0], req[1]));
	//        // session.write(new byte[]{(byte)0x80, (byte)0x3F, (byte)0x10, 
	//        //         (byte)0x02, (byte)0x04, (byte)0xBA, (byte)0x00, 
	//        //        (byte)0xC7, (byte)0xD7, (byte)0x07, (byte)0x0D});
	//    }

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdUploadMeteringData(IoSession session) throws Exception {
		log.debug("## [cmdUploadMeteringData] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdUploadMeteringData", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdResetModem(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String delay_time = (String)bd.getArgMap().get("delay_time");
		if (delay_time == null || delay_time.equals("")) {
			delay_time = "0"; // 즉시
		}

		log.debug("## [cmdResetModem] Action : " + delay_time);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdResetModemDelayTime", delay_time));
		sendCommand(session, "cmdResetModem", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdFactorySetting(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String code = (String)bd.getArgMap().get("code");
		if (code == null || code.equals("")) {
			code = "0314"; // 즉시
		}

		log.debug("## [cmdFactorySetting] Action : " + code);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdFactorySettingCode", Integer.toString(DataUtil.getIntTo2Byte(Hex.encode(code)))));
		sendCommand(session, "cmdFactorySetting", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdReadModemConfiguration(IoSession session) throws Exception {
		log.debug("## [cmdReadModemConfiguration] Action");
		sendCommand(session, "cmdReadModemConfiguration", null);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetTime(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String timestamp = (String)bd.getArgMap().get("time");
		if (timestamp == null || timestamp.equals("")) {
			timestamp = DateTimeUtil.getDateString(new Date());
		}

		log.debug("## [cmdSetTime] Action = " + timestamp);

		//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		//        Calendar cal = Calendar.getInstance();
		//        cal.add(Calendar.HOUR, -6);
		//        timestamp = sdf.format(cal.getTime());

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetTimestamp", timestamp));
		sendCommand(session, "cmdSetTime", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetModemResetInterval(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String interval = (String)bd.getArgMap().get("interval");
		if (interval == null || interval.equals("")) {
			interval = "1440";
		}

		log.debug("## [cmdSetModemResetInterval] Action : " + interval);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdModemResetIntervalMinute", interval));
		sendCommand(session, "cmdModemResetInterval", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetMeteringInterval(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String interval = (String)bd.getArgMap().get("interval");
		if (interval == null || interval.equals("")) {
			interval = "15";
		}

		log.debug("## [cmdSetMeteringInterval] Action : " + interval);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetMeteringIntervalMinute", interval));
		sendCommand(session, "cmdSetMeteringInterval", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetServerIpPort(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String server_ip = (String)bd.getArgMap().get("server_ip");
		String server_port = (String)bd.getArgMap().get("server_port");

		if (server_ip == null || server_ip.equals("")) {
			server_ip = "187.1.25.250";
		}

		if (server_port == null || server_port.equals("")) {
			server_port = "8000";
		}

		log.debug("## [cmdSetServerIpPort] Action : " + server_ip + ", " + server_port);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetServerIp", server_ip));
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetServerPort", server_port));
		sendCommand(session, "cmdSetServerIpPort", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetApn(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String apn_address = (String)bd.getArgMap().get("apn_address");
		String apn_id = (String)bd.getArgMap().get("apn_id");
		String apn_password = (String)bd.getArgMap().get("apn_id");

		if (apn_address == null || apn_address.equals("")) {
			apn_address = "net.asiacell.com";
		}

		if (apn_id == null || apn_id.equals("")) {
			apn_id = "";
		}

		if (apn_password == null || apn_password.equals("")) {
			apn_password = "";
		}

		log.debug("## [cmdSetApn] Action : " + apn_address + ", " + apn_id + ", " + apn_password);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetApnAddress", apn_address));
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetApnID", apn_id));
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetApnPassword", apn_password));
		sendCommand(session, "cmdSetApn", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdSetMeterTime(IoSession session) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String currTime = sdf.format(cal.getTime());

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();

		log.debug("## [cmdSetMeterTime] Action : " + currTime);

		byte[] times = new byte[12];
		int year = Integer.parseInt(currTime.substring(0, 4));
		int month = Integer.parseInt(currTime.substring(4, 6));
		int day = Integer.parseInt(currTime.substring(6, 8));
		int hour = Integer.parseInt(currTime.substring(8, 10));
		int min = Integer.parseInt(currTime.substring(10, 12));
		int sec = Integer.parseInt(currTime.substring(12, 14));

		times[0] = (byte) ((year / 0x100) & 0xFF); //year highbyte,
		times[1] = (byte) ((year % 0x100) & 0xFF); //year lowbyte,
		times[2] = (byte) month; //month,
		times[3] = (byte) day; //day of month
		times[4] = (byte) (cal.get(Calendar.DAY_OF_WEEK) - 1); //day of week,
		times[5] = (byte) hour; //hour,
		times[6] = (byte) min; //minute,
		times[7] = (byte) sec; //second,
		times[8] = 0x00; //hundredths of second,
		times[9] = (byte) 0x80; //deviation highbyte,
		times[10] = 0x00; //deviation lowbyte,
		times[11] = 0x00; //clock status   	

		//params.add(DataUtil.getSMIValueByObject(ns, "cmdSetTimestamp", new TIMESTAMP(sdf.format(cal.getTime()))));
		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdMeterTimeSyncTime"), new STREAM(times)));
		//params.add(DataUtil.getSMIValueByObject(ns, "cmdMeterTimeSyncTime", currTime));
		sendCommand(session, "cmdSetMeterTime", params);
	}

	public void cmdSetBypassStart(IoSession session) throws Exception {
		log.debug("## [cmdSetBypassStart] Action ~!!!");

		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		int timeout = 10;

		if (bd.getArgMap() != null && !bd.getArgMap().get("timeout").equals("")) {
			timeout = Integer.parseInt((String)bd.getArgMap().get("timeout"));
		}

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdSetBypassStartTimeout", Integer.toString(timeout)));
		sendCommand(session, "cmdSetBypassStart", params);
	}

	public void cmdRelayDisconnectNoTimeOut(IoSession session) throws Exception {
		log.debug("## [cmdRelayDisconnectNoTimeOut] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdRelayDisconnect", params);
	}

	public void cmdRelayReconnectNoTimeOut(IoSession session) throws Exception {
		log.debug("## [cmdRelayReconnectNoTimeOut] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdRelayReconnect", params);
	}

	public void cmdMeterTimeSync(IoSession session) throws Exception //for dlms meter
	{
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String time = (String)bd.getArgMap().get("time");

		Calendar cal = null;
		if (time != null && !time.equals("")) {
			cal = DateTimeUtil.getCalendar(time);
		} else {
			cal = Calendar.getInstance();
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String currTime = sdf.format(cal.getTime());

		log.debug("## [cmdMeterTimeSync] Action : " + currTime);

		byte[] times = new byte[12];
		int year = Integer.parseInt(currTime.substring(0, 4));
		int month = Integer.parseInt(currTime.substring(4, 6));
		int day = Integer.parseInt(currTime.substring(6, 8));
		int hour = Integer.parseInt(currTime.substring(8, 10));
		int min = Integer.parseInt(currTime.substring(10, 12));
		int sec = Integer.parseInt(currTime.substring(12, 14));

		times[0] = (byte) ((year / 0x100) & 0xFF); //year highbyte,
		times[1] = (byte) ((year % 0x100) & 0xFF); //year lowbyte,
		times[2] = (byte) month; //month,
		times[3] = (byte) day; //day of month
		times[4] = (byte) (cal.get(Calendar.DAY_OF_WEEK) - 1); //day of week,
		times[5] = (byte) hour; //hour,
		times[6] = (byte) min; //minute,
		times[7] = (byte) sec; //second,
		times[8] = 0x00; //hundredths of second,
		times[9] = (byte) 0x80; //deviation highbyte,
		times[10] = 0x00; //deviation lowbyte,
		times[11] = 0x00; //clock status   		

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdMeterTimeSyncTime"), new STREAM(times)));
		sendCommand(session, "cmdMeterTimeSync", params);//cmdSetTime,107.5.1,Set Time
	}

	public void cmdCurrentValuesMetering(IoSession session) throws Exception {
		log.debug("## [cmdCurrentValuesMetering] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdCurrentValuesMetering", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdRelayStatus(IoSession session) throws Exception {
		log.debug("## [cmdRelayStatus] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdRelayStatus", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdRelayDisconnect(IoSession session) throws Exception {
		log.debug("## [cmdRelayDisconnect] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdRelayDisconnect", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdRelayReconnect(IoSession session) throws Exception {
		log.debug("## [cmdRelayReconnect] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdRelayReconnect", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdReadModemEventLog(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String read_cnt = (String)bd.getArgMap().get("read_cnt");

		int readCount = 1;
		if (read_cnt != null && !read_cnt.equals("")) {
			// 최대 200개로 제한
			readCount = 200 < Integer.parseInt(read_cnt) ? 200 : Integer.parseInt(read_cnt);
		}

		log.debug("## [cmdReadModemEventLog] Action : " + readCount);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();
		params.add(DataUtil.getSMIValueByObject(ns, "cmdReadEventLogCount", String.valueOf(readCount)));
		sendCommand(session, "cmdReadModemEventLog", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdGetMeterStatus(IoSession session) throws Exception {
		log.debug("## [cmdGetMeterStatus] Action");

		List<SMIValue> params = new ArrayList<SMIValue>();
		sendCommand(session, "cmdGetMeterStatus", params);
	}

	public void cmdOndemandMetering(IoSession session, int startIndex, int endIndex) throws Exception {
		log.debug("## [cmdOndemandMetering] Action");

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();

		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdOndemandStartIndex"), new UINT(startIndex)));
		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdOndemandEndIndex"), new UINT(endIndex)));

		sendCommand(session, "cmdOndemandMetering", params);
	}

	/**
	 * EVN GPRS Modem Command
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void cmdOndemandMetering(IoSession session) throws Exception {
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		String startIndex = (String)bd.getArgMap().get("start_idx");
		String endIndex = (String)bd.getArgMap().get("stop_idx");

		log.debug("## [cmdOndemandMetering] Action : " + startIndex + "~" + endIndex);

		String ns = (String) session.getAttribute("nameSpace");
		List<SMIValue> params = new ArrayList<SMIValue>();

		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdOndemandStartIndex"), new UINT(Integer.parseInt(startIndex))));
		params.add(new SMIValue(DataUtil.getOIDByMIBName(ns, "cmdOndemandEndIndex"), new UINT(Integer.parseInt(endIndex))));

		sendCommand(session, "cmdOndemandMetering", params);
	}

	public void cmdGetDemandPeriod(IoSession session) throws Exception {
		long startTime = System.currentTimeMillis();
		log.debug("## [cmdGetDemandPeriod] Action : " + DateTimeUtil.getDateString(startTime));
		
		BypassDevice bd = (BypassDevice) session.getAttribute(session.getRemoteAddress());
		
		HashMap<String, Object> argMap = new HashMap<String, Object>();
		argMap.put("timeout", "60");
		bd.setArgMap(argMap);

		/*
		 * 1. Bypass Connection
		 */
		cmdSetBypassStart(session);
		
		/*
		 * 2. HDLC Connction.
		 */
		BypassFrameFactory gdFactory = new BypassEVNFactory("cmdGetDemandPeriod");
		bd.setFrameFactory(gdFactory);

		gdFactory.start(session, null);
	}

	@Override
	public CommandData execute(HashMap<String, String> params, IoSession session, Client client) throws Exception {
		
		BypassDevice bd = (BypassDevice)session.getAttribute(session.getRemoteAddress());
		String cmd = params.get("cmd").toString();		
		
		HashMap<String, Object> map = new HashMap<String, Object>();		
		map.putAll(params); // cast
		
		bd.setCommand(cmd);    
		bd.setClient(client);		
		bd.setArgMap(map);
		
		try {
			session.setAttribute(session.getRemoteAddress(), bd);
			
			Method method = this.getClass().getMethod(cmd, IoSession.class);
			method.invoke(this, session);
			
			BypassClientHandler handler = 
	                (BypassClientHandler)session.getHandler();
			
			ServiceData sd = handler.getResponse(session);
			
			// 응답받고 EOT 전송
			session.write(new ControlDataFrame(ControlDataConstants.CODE_EOT));
			
			return (CommandData)sd;
			
		} catch(Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}

}
