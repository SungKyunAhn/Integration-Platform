/**
 * (@)# BypassGDFactory.java
 *
 * 2016. 4. 15.
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
package com.aimir.fep.bypass.decofactory.protocolfactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aimir.fep.bypass.actions.moe.DayProfileTable;
import com.aimir.fep.bypass.actions.moe.SeasonProfileTable;
import com.aimir.fep.bypass.actions.moe.WeekProfileTable;
import com.aimir.fep.bypass.decofactory.consts.DlmsConstantsForMOE.ActionResult;
import com.aimir.fep.bypass.decofactory.consts.DlmsConstantsForMOE.DataAccessResult;
import com.aimir.fep.bypass.decofactory.consts.DlmsConstantsForMOE.ImageTransferStatus;
import com.aimir.fep.bypass.decofactory.consts.DlmsConstantsForMOE.TOUInfoBlockType;
import com.aimir.fep.bypass.decofactory.consts.HdlcConstants.HdlcObjectType;
import com.aimir.fep.bypass.decofactory.decoframe.GD_DLMSFrame;
import com.aimir.fep.bypass.decofactory.decoframe.INestedFrame;
import com.aimir.fep.bypass.decofactory.decorator.NestedDLMSDecoratorForMOE;
import com.aimir.fep.bypass.decofactory.decorator.NestedHDLCDecorator;
import com.aimir.fep.protocol.nip.client.multisession.MultiSession;
import com.aimir.fep.util.DataUtil;
import com.aimir.fep.util.Hex;
import com.aimir.util.DateTimeUtil;

/**
 * @author simhanger
 *
 */
public class BypassGDFactory extends BypassFrameFactory {
	private static Logger logger = LoggerFactory.getLogger(BypassGDFactory.class);
	private INestedFrame frame;
	private String command;
	private Procedure step;
	private IoSession session;
	private HashMap<String, Object> params;
	private BypassFrameResult bypassFrameResult = new BypassFrameResult();

	/*
	 * Meter F/W OTA용
	 */
	private String imageIdentifier;
	private int fwSize;
	private int packetSize;
	private int offset;
	private byte[] fwImgArray;
	private int totalImageBlockNumber;
	private int imageBlockNumber;
	private int remainPackateLength;

	private int verificationRetryCount;
	private boolean isTakeOverMode;

	private Timer blockTransferRetryTimer = new Timer();
	private NeedImangeBlockTransferRetry blockTransferRetryTask;
	//private RetrySendTimerTask retrySendTimerTask;
	private final int NEED_IMAGE_BLOCK_TRANSFER_MAX_RETRY_COUNT = 10;
	private final int VERIFICATION_MAX_RETRY_COUNT = 10;

	private long procedureStartTime;

	/*
	 * TOU Set용
	 */
	private Procedure[] touProcedure;
	private int touProcedurePos = 0;
	private int sendHdlcPacketMaxSize = 0;
	private TOUInfoBlockType infoBlockType;

	private byte[] touExcuteTOUProfileArray;
	private int remainExcuteTOUPacketLength = -1;
	private int touExcuteTOUProfileOffset;

	private byte[] touSeasonInfoArray;
	private byte[] touWeekInfoArray;
	private byte[] touDayInfoArray;

	private int touBlockNumber = 1;
	private int middleBlockSize;

	/*
	 * ACTION_IMAGE_BLOCK_TRANSFER 상태 플래그
	 * TRUE = SEND 했음
	 * FALSE = RECEIVE 했음.
	 */
	private boolean needImangeBlockTransferRetry = false;
	private boolean RetrySendTaskFlag = false;

	public BypassGDFactory(String command) {
		this.command = command;
		frame = new NestedHDLCDecorator(new NestedDLMSDecoratorForMOE(new GD_DLMSFrame()));
		params = new LinkedHashMap<String, Object>();
	}

	@Override
	public void setParam(HashMap<String, Object> param) {
		params = param;
	}

	@Override
	public boolean start(IoSession session, Object type) {
		boolean result = false;
		this.session = session;
		step = Procedure.HDLC_SNRM;

		/*
		 * COMMAND : cmdMeterOTAStart 초기화
		 */
		if (command.equals("cmdMeterOTAStart")) {
			procedureStartTime = System.currentTimeMillis();
			logger.debug("## [cmdMeterOTAStart] Start : {}", DateTimeUtil.getDateString(procedureStartTime));

			if (params != null && params.get("takeover") != null) {
				isTakeOverMode = Boolean.parseBoolean(String.valueOf(params.get("takeover")));
			}

			if (params != null && params.get("image") != null) {
				imageIdentifier = (String) params.get("image_identifier");

				/*
				 *  Image Key : 6자리
				 */
				//				imageIdentifier = imageIdentifier.substring(0, 6);

				fwImgArray = (byte[]) params.get("image");
				fwSize = fwImgArray.length;
				remainPackateLength = fwSize;

				result = sendBypass();
			}
		} else if (command.equals("cmdMeterParamSet")) {

		} else if (command.equals("cmdMeterParamGet")) {

		} else if (command.equals("cmdGetMeterFWVersion") || command.equals("cmdAlarmReset") 
				|| command.equals("cmdGetBillingCycle") || command.equals("cmdSetBillingCycle") 
				|| command.equals("cmdDemandPeriod") || command.equals("cmdGetDemandPeriod")
				|| command.equals("cmdGetCurrentLoadLimit") || command.equals("cmdSetCurrentLoadLimit")) {
			result = sendBypass();
		} else if (command.equals("cmdTOUSet")) {
			infoBlockType = TOUInfoBlockType.FIRST_BLOCK;

			// Procedure 설정
			int touProcedureIndex = 0;
			touProcedure = new Procedure[params.size()];

			Set<String> set = params.keySet();
			Iterator<String> it = set.iterator();
			while (it.hasNext()) {
				String key = it.next();

				if (key.equals("calendarNamePassive")) {
					touProcedure[touProcedureIndex] = Procedure.SET_CALENDAR_NAME_PASSIVE;
				} else if (key.equals("seasonProfileTable")) {
					touProcedure[touProcedureIndex] = Procedure.SET_SEASON_PROFILE;
					touSeasonInfoArray = ((SeasonProfileTable) params.get("seasonProfileTable")).getBytes();

					logger.debug("## Season Profile Table size = {}, {}", touSeasonInfoArray.length, Hex.decode(touSeasonInfoArray));
				} else if (key.equals("weekProfileTable")) {
					touProcedure[touProcedureIndex] = Procedure.SET_WEEK_PROFILE;
					touWeekInfoArray = ((WeekProfileTable) params.get("weekProfileTable")).getBytes();

					logger.debug("## Week Profile Table size = {}, {}", touWeekInfoArray.length, Hex.decode(touWeekInfoArray));
				} else if (key.equals("dayProfileTable")) {
					touProcedure[touProcedureIndex] = Procedure.SET_DAY_PROFILE;
					touDayInfoArray = ((DayProfileTable) params.get("dayProfileTable")).getBytes();

					logger.debug("## Day Profile Table size = {}, {}", touDayInfoArray.length, Hex.decode(touDayInfoArray));
				} else if (key.equals("startingDate")) {
					touProcedure[touProcedureIndex] = Procedure.SET_STARTING_DATE;
				}

				touProcedureIndex++;
			}

			logger.debug("TOU_PROCEDURE QUEUE ==> {}", Arrays.toString(touProcedure));

			result = sendBypass();
		}

		return result;
	}

	@Override
	public BypassFrameResult receiveBypass(IoSession session, byte[] rawFrame) {
		boolean result = false;
		bypassFrameResult.clean();

		if (frame.decode(session, rawFrame, this.step)) {
			try {
				/**
				 * 공통
				 */
				switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
				case UA:
					if (this.step == Procedure.HDLC_SNRM) {

						// 결과 확인
						sendHdlcPacketMaxSize = Integer.parseInt(String.valueOf(frame.getResultData()));
						logger.debug("### Send HDLC Packet Max Size = {}", sendHdlcPacketMaxSize);

						this.step = Procedure.HDLC_AARQ;
						result = sendBypass();
					} else if (this.step == Procedure.HDLC_DISC) {
						logger.info("### HDLC DISC !!");
						session.closeNow();
						result = true;
					}
					break;
				case AARE:
					if (this.step == Procedure.HDLC_AARQ) {
						// 결과 확인
						boolean param = Boolean.valueOf(String.valueOf(frame.getResultData()));
						logger.debug("## HDLC_AARQ Result => {}", param);
						if (param) {
							this.step = Procedure.HDLC_ASSOCIATION_LN;
							result = sendBypass();
						}
					}
					break;
				default:
					break;

				}

				/*************************
				 * MOE GPRS Modem / Meter F/W OTA
				 */
				if (command.equals("cmdMeterOTAStart")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_IMAGE_TRANSFER_ENABLE;
								result = sendBypass();
							}
						} else if (this.step == Procedure.ACTION_IMAGE_TRANSFER_INIT) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## ACTION_IMAGE_TRANSFER_INIT => {}", param.name());

							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_IMAGE_TRANSFER_STATUS;
								result = sendBypass();
							}
						} else if (this.step == Procedure.ACTION_IMAGE_BLOCK_TRANSFER) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## ACTION_IMAGE_BLOCK_TRANSFER => {}", param.name());

							needImangeBlockTransferRetry = false;
							blockTransferRetryTask.cancel();
							int temp = blockTransferRetryTimer.purge();
							logger.debug("##퍼지 됬음.  ==>> {}", temp);

							if (param == ActionResult.SUCCESS) {
								/*
								 * 더 보낼 Block이 없을때 처리
								 */
								if (remainPackateLength <= 0) {
									logger.info("[ACTION_IMAGE_BLOCK_TRANSFER] Finished !! Image Block Count={}/{}, RemainPacket Size={}", imageBlockNumber, totalImageBlockNumber, (remainPackateLength - packetSize));
									this.step = Procedure.GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER;

									// 다 보내면 타이머 해지
									stopTransferImageTimer();
									logger.debug("## Timer 다보낸뒤 해지~! ==> needImangeBlockTransferRetry={}", needImangeBlockTransferRetry);
								}

								result = sendBypass();
							} else { // 실패시 타이머 해지
								stopTransferImageTimer();
								logger.debug("## Timer 실패시 해지~! ==> needImangeBlockTransferRetry={}", needImangeBlockTransferRetry);
							}
						} else if (this.step == Procedure.ACTION_IMAGE_VERIFY) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## ACTION_IMAGE_VERIFY => {}", param.name());

							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_IMAGE_TO_ACTIVATE_INFO;
								result = sendBypass();
							} else if (param == ActionResult.TEMPORARY_FAIL) {
								/*
								 * Image transfer status 체크
								 * Procedure.ACTION_IMAGE_VERIFY를 유지한체 IMAGE_TRANSFER_STATUS 검증 => GET으로 받기때문에 밑에서 처리
								 */
								byte[] req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_STATUS, null);
								if (req != null) {
									logger.debug("### [ACTION_IMAGE_VERIFY][TEMPORARY_FAIL] HDLC_REQUEST => {}", Hex.decode(req));

									this.session.write(req);
									result = true;
								}
								break;
							} else {
								// 나머지는 다 에러.
							}
						} else if (this.step == Procedure.ACTION_IMAGE_ACTIVATE) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## ACTION_IMAGE_ACTIVATE => {}", param.name());

							if (param == ActionResult.SUCCESS) {
								logger.debug("### Meter F/W OTA Successful. ###");
								logger.debug("### Meter F/W OTA Successful. ###");
								logger.debug("### Meter F/W OTA Successful. ###");

								bypassFrameResult.setLastProcedure(Procedure.ACTION_IMAGE_ACTIVATE);
								bypassFrameResult.setResultValue("success");

								/*
								 * 종료처리
								 */
								//this.step = Procedure.HDLC_DISC;

								/*
								 * 미터 F/W 버전 갱신
								 */
								Thread.sleep(40000);
								this.step = Procedure.GET_FIRMWARE_VERSION;

								result = sendBypass();
							} else if (param == ActionResult.TEMPORARY_FAIL) {
								/*
								 * Image transfer status 체크
								 * Procedure.ACTION_IMAGE_ACTIVATE를 유지한체 IMAGE_TRANSFER_STATUS 검증 => GET으로 받기때문에 밑에서 처리
								 */
								byte[] req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_STATUS, null);
								if (req != null) {
									logger.debug("### [ACTION_IMAGE_ACTIVATE][TEMPORARY_FAIL] HDLC_REQUEST => {}", Hex.decode(req));

									this.session.write(req);
									result = true;
								}
								break;
							} else {
								// 나머지는 다 에러.
							}
						}
						break;
					case GET_RES:
						// 결과 확인
						Object param = frame.getResultData();

						if (param instanceof DataAccessResult) {
							result = false;
							logger.debug("## [{}]GET_RES_DataAccessResult => {}", step.name(), ((DataAccessResult) param).name());
						} else {
							if (this.step == Procedure.GET_IMAGE_TRANSFER_ENABLE) {
								// 결과 확인
								boolean resultData = Boolean.parseBoolean(String.valueOf(param));
								if (resultData) {
									this.step = Procedure.GET_IMAGE_BLOCK_SIZE;
									logger.debug("## GET_IMAGE_TRANSFER_ENABLE => {}", resultData);
									result = sendBypass();
								}
							} else if (this.step == Procedure.GET_IMAGE_BLOCK_SIZE) {
								// 결과 확인
								long IMAGE_TRANSFER_BLOCK_SIZE = (Long) param;
								packetSize = (int) IMAGE_TRANSFER_BLOCK_SIZE;
								if ((long) packetSize != IMAGE_TRANSFER_BLOCK_SIZE) {
									logger.error("IMAGE_TRANSFER_BLOCK_SIZE Casting Error to Integer. => {}", IMAGE_TRANSFER_BLOCK_SIZE);
									result = false;
								} else {
									totalImageBlockNumber = fwSize / packetSize;
									if (0 < (fwSize % packetSize)) {
										totalImageBlockNumber++;
									}

									if (isTakeOverMode) {
										this.step = Procedure.GET_IMAGE_TRANSFER_STATUS;

									} else {
										this.step = Procedure.ACTION_IMAGE_TRANSFER_INIT;
									}

									logger.debug("## TAKEOVER_MODE => {}, IMAGE_TRANSFER_BLOCK_SIZE => {}", isTakeOverMode, IMAGE_TRANSFER_BLOCK_SIZE);
									result = sendBypass();
								}
							} else if (this.step == Procedure.GET_IMAGE_TRANSFER_STATUS) {
								// 결과 확인
								Integer resultData = (Integer) param;
								logger.debug("## GET_IMAGE_TRANSFER_STATUS => {}", ImageTransferStatus.getItem(resultData));

								if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_TRANSFER_INITIATED) {
									this.step = Procedure.GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER;
									result = sendBypass();
								} else if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_TRANSFER_NOT_INITIATED) {
									this.step = Procedure.ACTION_IMAGE_TRANSFER_INIT;
									result = sendBypass();
								} else if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_VERIFICATION_INITIATED) {
									this.step = Procedure.ACTION_IMAGE_VERIFY;
									result = verificationCheckRetry();
								} else if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_VERIFICATION_SUCCESSFUL) {
									this.step = Procedure.GET_IMAGE_TO_ACTIVATE_INFO;
									result = sendBypass();
								} else if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_ACTIVATION_INITIATED) {
									this.step = Procedure.ACTION_IMAGE_ACTIVATE;
									result = activationCheckRetry();
								} else if (ImageTransferStatus.getItem(resultData) == ImageTransferStatus.IMAGE_ACTIVATION_SUCCESSFUL) {
									this.step = Procedure.HDLC_DISC;
									result = sendBypass();
								}

							} else if (this.step == Procedure.GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER) {
								/*
								 * 마지막 블럭인지 확인
								 */
								// 결과 확인
								long resultData = (Long) param;
								int firstNotTransferredBlockNumber = (int) resultData;
								if ((long) firstNotTransferredBlockNumber != resultData) {
									logger.error("IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER Casting Error to Integer. => {}", resultData);
									result = false;
								} else {
									if (totalImageBlockNumber <= firstNotTransferredBlockNumber) {
										this.step = Procedure.ACTION_IMAGE_VERIFY;
										logger.debug("## GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER : Last block = {}, Not transferred block = {}", imageBlockNumber, firstNotTransferredBlockNumber);

										result = sendBypass();
									} else {
										/**
										 * firstNotTransferredBlockNumber 확인시
										 * 미전송 블럭부터 전송.
										 */
										this.step = Procedure.ACTION_IMAGE_BLOCK_TRANSFER;

										imageBlockNumber = firstNotTransferredBlockNumber;
										offset = firstNotTransferredBlockNumber * packetSize;
										remainPackateLength = fwSize - offset;

										logger.warn("###### Image not transferred block is exist  ==> totalImageBlockNumber={}, firstNotTransferredBlockNumber={}, packetSize={}, offset={}" + ",  remainPackateLength={}", totalImageBlockNumber, firstNotTransferredBlockNumber, packetSize, offset, remainPackateLength);

										result = sendBypass();
									}
								}
							} else if (this.step == Procedure.ACTION_IMAGE_VERIFY) {
								// 결과 확인
								Integer resultData = (Integer) param;
								ImageTransferStatus status = ImageTransferStatus.getItem(resultData);
								logger.debug("## GET_IMAGE_TRANSFER_STATUS => {}", status);

								if (status == ImageTransferStatus.IMAGE_VERIFICATION_SUCCESSFUL) {
									this.step = Procedure.GET_IMAGE_TO_ACTIVATE_INFO;
									result = sendBypass();
								}
								/*
								 * 초기화중... 30초간 대기후 재시도. 3회 실시.
								 */
								else if (status == ImageTransferStatus.IMAGE_VERIFICATION_INITIATED && verificationRetryCount < VERIFICATION_MAX_RETRY_COUNT) {
									result = verificationCheckRetry();
								}
								/*
								 * status == ImageTransferStatus.IMAGE_VERIFICATION_FAILED)
								 */
								else {
									// Image Verify 실패
									logger.debug("ACTION_IMAGE_VERIFY 검증 실패");
									logger.debug("ACTION_IMAGE_VERIFY 검증 실패");
									logger.debug("ACTION_IMAGE_VERIFY 검증 실패");
								}
							} else if (this.step == Procedure.ACTION_IMAGE_ACTIVATE) {
								// 결과 확인
								Integer resultData = (Integer) param;
								ImageTransferStatus status = ImageTransferStatus.getItem(resultData);
								logger.debug("## GET_IMAGE_TRANSFER_STATUS => {}", status);

								if (status == ImageTransferStatus.IMAGE_ACTIVATION_SUCCESSFUL) {
									logger.debug("### Meter F/W OTA Successful. ###");
									logger.debug("### Meter F/W OTA Successful. ###");
									logger.debug("### Meter F/W OTA Successful. ###");

									bypassFrameResult.setLastProcedure(Procedure.ACTION_IMAGE_ACTIVATE);
									bypassFrameResult.setResultValue("success");

									/*
									 * 종료처리
									 */
									//this.step = Procedure.HDLC_DISC;

									/*
									 * 미터 F/W 버전 갱신
									 */
									Thread.sleep(50000);
									this.step = Procedure.GET_FIRMWARE_VERSION;

									result = sendBypass();
								}
								/*
								 * 초기화중... 30초간 대기후 재시도. 3회 실시.
								 */
								else if (status == ImageTransferStatus.IMAGE_ACTIVATION_INITIATED && verificationRetryCount < VERIFICATION_MAX_RETRY_COUNT) {
									result = activationCheckRetry();
								}
								/*
								 * status == ImageTransferStatus.IMAGE_ACTIVATION_FAILED)
								 */
								else {
									// Image Activation 실패
									logger.debug("ACTION_IMAGE_ACTIVATE 검증 실패");
								}
							} else if (this.step == Procedure.GET_IMAGE_TO_ACTIVATE_INFO) {
								// 결과 확인
								@SuppressWarnings("unchecked")
								HashMap<String, Object> resultData = (HashMap<String, Object>) param;

								if (resultData != null && 0 < resultData.size()) {
									long image_to_activate_size = Long.parseLong(String.valueOf(resultData.get("image_to_activate_size")));
									byte[] image_to_activate_identification = (byte[]) resultData.get("image_to_activate_identification");

									logger.debug("## GET_IMAGE_TO_ACTIVATE_INFO => image_to_activate_size - Send = {}, Receive = {}", fwSize, image_to_activate_size);
									logger.debug("## GET_IMAGE_TO_ACTIVATE_INFO => image_to_activate_identification - Send = {}, Receive = {}", imageIdentifier, DataUtil.getString(image_to_activate_identification));

									if (resultData.containsKey("image_to_activate_signature")) {
										byte[] image_to_activate_signature = (byte[]) resultData.get("image_to_activate_signature");
										logger.debug("## GET_IMAGE_TO_ACTIVATE_INFO => image_to_activate_signature - {}", Hex.decode(image_to_activate_signature));
									}

									/*
									 * 검증
									 */
									if (image_to_activate_size == fwSize && imageIdentifier.equals(DataUtil.getString(image_to_activate_identification))) {
										this.step = Procedure.ACTION_IMAGE_ACTIVATE;

										result = sendBypass();
									} else {
										logger.debug("## IMAGE_TO_ACTIVATE _INFO - Validation Fail.");
									}
								}
							} else if (this.step == Procedure.GET_FIRMWARE_VERSION) {
								// 결과 확인
								String resultData = (String) param;
								if (!resultData.equals("")) {
									logger.debug("## GET_FIRMWARE_VERSION => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_FIRMWARE_VERSION);
									bypassFrameResult.setResultValue(resultData);

									result = true;
								}

								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}

						}

						break;
					default:
						break;
					}

				} else if (command.equals("cmdMeterParamSet")) {

				} else if (command.equals("cmdMeterParamGet")) {

				} else if (command.equals("cmdGetMeterFWVersion")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_FIRMWARE_VERSION;
								result = sendBypass();
							}
						}
						break;
					case GET_RES:
						// 결과 확인
						Object param = frame.getResultData();

						if (param instanceof DataAccessResult) {
							result = false;
							logger.debug("## [{}]GET_RES_DataAccessResult => {}", step.name(), ((DataAccessResult) param).name());
						} else {
							if (this.step == Procedure.GET_FIRMWARE_VERSION) {
								// 결과 확인
								String resultData = (String) param;
								if (!resultData.equals("")) {
									logger.debug("## GET_FIRMWARE_VERSION => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_FIRMWARE_VERSION);
									bypassFrameResult.setResultValue(resultData);

									result = true;
								}

								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}
						}
						break;
					default:
						break;
					}
				} else if (command.equals("cmdAlarmReset")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.ACTION_METER_ALARM_RESET;
								result = sendBypass();
							}
						} else if (this.step == Procedure.ACTION_METER_ALARM_RESET) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## ACTION_METER_ALARM_RESET => {}", param.name());

							if (param == ActionResult.SUCCESS) {
								bypassFrameResult.setLastProcedure(Procedure.ACTION_METER_ALARM_RESET);
								bypassFrameResult.setResultValue("Success");
								result = true;

								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}
						}
						break;
					default:
						break;
					}
				} else if (command.equals("cmdGetBillingCycle")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_BILLING_CYCLE;
								result = sendBypass();
							}
						}
						break;
					case GET_RES:
						// 결과 확인
						Object param = frame.getResultData();
						if (param instanceof DataAccessResult) {
							result = false;
							logger.debug("## [{}]GET_RES_DataAccessResult => {}", step.name(), ((DataAccessResult) param).name());
						} else {
							if (this.step == Procedure.GET_BILLING_CYCLE) {
								// 결과 확인
								@SuppressWarnings("unchecked")
								HashMap<String, String> resultData = (HashMap<String, String>) param;
								if (!resultData.equals("")) {
									logger.debug("## GET_BILLING_CYCLE => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_BILLING_CYCLE);
									bypassFrameResult.setResultValue(resultData);

									result = true;
								}

								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}
						}
						break;
					default:
						break;
					}
				} else if (command.equals("cmdSetBillingCycle")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.SET_BILLING_CYCLE;
								result = sendBypass();
							}
						}
						break;
					case SET_RES:
						// 결과 확인
						DataAccessResult param = (DataAccessResult) frame.getResultData();
						logger.debug("## SET_BILLING_CYCLE => {}", param.name());

						if (param == DataAccessResult.SUCCESS) {
							bypassFrameResult.setLastProcedure(Procedure.SET_BILLING_CYCLE);
							bypassFrameResult.setResultValue("Success");
							result = true;

							this.step = Procedure.HDLC_DISC;
							result = sendBypass();
						}
						break;
					default:
						break;
					}

				} else if (command.equals("cmdDemandPeriod")) {

					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.SET_DEMAND_PLUS_A_PERIOD;
								result = sendBypass();
							}
						}
						break;
					case SET_RES:
						// 결과 확인
						DataAccessResult param = (DataAccessResult) frame.getResultData();
						logger.debug("## [" + this.step + "]SET_BILLING_CYCLE => {}", param.name());

						if (param == DataAccessResult.SUCCESS) {
							if (this.step == Procedure.SET_DEMAND_PLUS_A_PERIOD) {
								this.step = Procedure.SET_DEMAND_PLUS_A_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_PLUS_A_NUMBER) {
								this.step = Procedure.SET_DEMAND_MINUS_A_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_A_PERIOD) {
								this.step = Procedure.SET_DEMAND_MINUS_A_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_A_NUMBER) {
								this.step = Procedure.SET_DEMAND_PLUS_R_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_PLUS_R_PERIOD) {
								this.step = Procedure.SET_DEMAND_PLUS_R_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_PLUS_R_NUMBER) {
								this.step = Procedure.SET_DEMAND_MINUS_R_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_R_PERIOD) {
								this.step = Procedure.SET_DEMAND_MINUS_R_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_R_NUMBER) {
								this.step = Procedure.SET_DEMAND_R_QI_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_R_QI_PERIOD) {
								this.step = Procedure.SET_DEMAND_R_QI_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_R_QI_NUMBER) {
								this.step = Procedure.SET_DEMAND_R_QIV_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_R_QIV_PERIOD) {
								this.step = Procedure.SET_DEMAND_R_QIV_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_R_QIV_NUMBER) {
								this.step = Procedure.SET_DEMAND_PLUS_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_PLUS_PERIOD) {
								this.step = Procedure.SET_DEMAND_PLUS_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_PLUS_NUMBER) {
								this.step = Procedure.SET_DEMAND_MINUS_PERIOD;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_PERIOD) {
								this.step = Procedure.SET_DEMAND_MINUS_NUMBER;
								result = sendBypass();
							} else if (this.step == Procedure.SET_DEMAND_MINUS_NUMBER) {

								bypassFrameResult.setLastProcedure(Procedure.SET_DEMAND_MINUS_NUMBER);
								bypassFrameResult.setResultValue("Success");
								result = true;

								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}
						} else {

						}
						break;
					default:
						break;
					}

				} else if(command.equals("cmdGetDemandPeriod")) {
					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = Procedure.GET_DEMAND_PLUS_A_PERIOD;
								result = sendBypass();
							}
						}
						break;
					case GET_RES:
						// 결과 확인
						Object param = frame.getResultData();
						if(param == null) {
							
						}
						else if (param instanceof DataAccessResult) {
							result = false;
							logger.debug("## [{}]GET_RES_DataAccessResult => {}", step.name(), ((DataAccessResult) param).name());
						}
						else {
							if(this.step == Procedure.GET_DEMAND_PLUS_A_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_A_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_A_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_A_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_PLUS_A_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_PLUS_A_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_A_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_A_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_A_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_A_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_A_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_A_PERIOD => {}", resultData);
									
									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_A_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_A_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_A_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_A_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_A_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_A_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_A_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_PLUS_R_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_PLUS_R_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_R_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_R_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_R_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_PLUS_R_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_PLUS_R_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_R_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_R_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_R_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_R_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_R_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_R_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_R_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_R_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_R_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_R_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_R_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_R_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_R_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_R_QI_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_R_QI_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_R_QI_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_R_QI_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_R_QI_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_R_QI_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_R_QI_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_R_QI_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_R_QI_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_R_QI_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_R_QIV_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_R_QIV_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_R_QIV_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_R_QIV_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_R_QIV_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_R_QIV_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_R_QIV_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_R_QIV_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_R_QIV_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_R_QIV_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_PLUS_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_PLUS_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_PLUS_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_PLUS_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_PLUS_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_PLUS_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_PLUS_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_PERIOD;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_PERIOD) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_PERIOD => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_PERIOD);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_PERIOD.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.GET_DEMAND_MINUS_NUMBER;
								result = sendBypass();
							}else if(this.step == Procedure.GET_DEMAND_MINUS_NUMBER) {
								Long resultData = (Long) param;
								
								if (0 <= resultData) {
									logger.debug("## GET_DEMAND_MINUS_NUMBER => {}", resultData);

									bypassFrameResult.setLastProcedure(Procedure.GET_DEMAND_MINUS_NUMBER);
									bypassFrameResult.addResultValue(Procedure.GET_DEMAND_MINUS_NUMBER.name(), resultData);
									//bypassFrameResult.setResultValue(resultData);

									result = true;
								}
								
								this.step = Procedure.HDLC_DISC;
								result = sendBypass();
							}
						}
						
						break;
					default:
							break;
					}
				} else if (command.equals("cmdTOUSet")) {

					switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
					case ACTION_RES:
						if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
							// 결과 확인
							ActionResult param = (ActionResult) frame.getResultData();
							logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
							if (param == ActionResult.SUCCESS) {
								this.step = touProcedure[touProcedurePos];
								result = sendBypass();
							}
						}
						break;
					case SET_RES:
						// 결과 확인
						Object param = frame.getResultData();

						if (param instanceof DataAccessResult) {
							DataAccessResult daResult = (DataAccessResult) frame.getResultData();
							logger.debug("## TOU_SET [" + this.step + "] => {}", daResult.name());

							if (daResult == DataAccessResult.SUCCESS) {
								if (this.step == touProcedure[touProcedurePos]) {
									touProcedurePos++;

									if (touProcedurePos == touProcedure.length) { // 마지막 Procedure 일경우
										bypassFrameResult.setLastProcedure(this.step);
										bypassFrameResult.setResultValue("Success");
										result = true;

										this.step = Procedure.HDLC_DISC;
										result = sendBypass();
									} else {
										this.step = touProcedure[touProcedurePos];
										result = sendBypass();
									}
								}
							} else {

							}

						} else if (param instanceof Integer) {
							// 결과 확인
							int blockNumber = Integer.parseInt(String.valueOf(frame.getResultData()));
							logger.debug("## {} => {}", this.step, blockNumber);

							if (blockNumber == 1 && middleBlockSize < remainExcuteTOUPacketLength) {
								infoBlockType = TOUInfoBlockType.MIDDLE_BLOCK;
							} else if (middleBlockSize > remainExcuteTOUPacketLength) {
								infoBlockType = TOUInfoBlockType.LAST_BLOCK;
							}

							result = sendBypass();

						}

						break;
					default:
						break;
					}
				}
				else if(command.equals("cmdGetCurrentLoadLimit")) {
						switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
						case ACTION_RES:
							if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
								// 결과 확인
								ActionResult param = (ActionResult) frame.getResultData();
								logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
								if (param == ActionResult.SUCCESS) {
									this.step = Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME;
									result = sendBypass();
								}
							}
							break;
						case GET_RES:
							// 결과 확인
							Object param = frame.getResultData();
							if(param == null) {
								
							}
							else if (param instanceof DataAccessResult) {
								result = false;
								logger.debug("## [{}]GET_RES_DataAccessResult => {}", step.name(), ((DataAccessResult) param).name());
							}
							else {
								if(this.step == Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME) {
									Long resultData = (Long) param;
									
									if (0 <= resultData) {
										logger.debug("## GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME => {}", resultData);

										bypassFrameResult.setLastProcedure(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME);
										bypassFrameResult.addResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME.name(), resultData);

										result = true;
									}
									
									this.step = Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD;
									result = sendBypass();
								}else if(this.step == Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD) {
									Long resultData = (Long) param;
									
									if (!resultData.equals("")) {
										logger.debug("## GET_CURRENT_LOAD_LIMIT_THRESHOLD => {}", resultData);

										bypassFrameResult.setLastProcedure(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD);
										bypassFrameResult.addResultValue(Procedure.GET_CURRENT_LOAD_LIMIT_THRESHOLD.name(), resultData);

										result = true;
									}
									
									this.step = Procedure.HDLC_DISC;
									result = sendBypass();
								}
							}
							break;
						default:
								break;
						}
					}else if (command.equals("cmdSetCurrentLoadLimit")) {

						switch (HdlcObjectType.getItem(DataUtil.getByteToInt(frame.getType()))) {
						case ACTION_RES:
							if (this.step == Procedure.HDLC_ASSOCIATION_LN) {
								// 결과 확인
								ActionResult param = (ActionResult) frame.getResultData();
								logger.debug("## HDLC_ASSOCIATION_LN Result => {}", param.name());
								if (param == ActionResult.SUCCESS) {
									this.step = Procedure.SET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME;
									result = sendBypass();
								}
							}
							break;
						case SET_RES:
							// 결과 확인
							DataAccessResult param = (DataAccessResult) frame.getResultData();
							logger.debug("## [" + this.step + "]SET_CURRENT_LOAD_LIMIT => {}", param.name());

							if (param == DataAccessResult.SUCCESS) {
								if (this.step == Procedure.SET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME) {
									this.step = Procedure.SET_CURRENT_LOAD_LIMIT_THRESHOLD;
									result = sendBypass();
								} else if (this.step == Procedure.SET_CURRENT_LOAD_LIMIT_THRESHOLD) {

									bypassFrameResult.setLastProcedure(Procedure.SET_CURRENT_LOAD_LIMIT_THRESHOLD);
									bypassFrameResult.setResultValue("Success");
									result = true;

									this.step = Procedure.HDLC_DISC;
									result = sendBypass();
								}
							} else {

							}
							break;
						default:
							break;
						}

					}
				
			} catch (Exception e) {
				logger.error("BYPASS_GD RECEIVE ERROR - {}", e);
				result = false;
			}
		}

		bypassFrameResult.setStep(this.step);
		bypassFrameResult.setResultState(result);
		return bypassFrameResult;

	}

	private boolean verificationCheckRetry() {
		boolean result = false;
		/*
		 * Image transfer status 체크
		 */
		try {
			Thread.sleep(30000);

			byte[] req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_STATUS, null);
			if (req != null) {
				verificationRetryCount++; // 재시도 횟수 카운팅
				logger.debug("### [ACTION_IMAGE_VERIFY][IMAGE_VERIFICATION_INITIATED] Count={} HDLC_REQUEST => {}", verificationRetryCount, Hex.decode(req));

				session.write(req);
				result = true;
			}
		} catch (Exception e) {
			logger.equals("verificationCheckRetry Error - " + e);
		}
		return result;
	}

	private boolean activationCheckRetry() {
		boolean result = false;

		/*
		 * Image transfer status 체크
		 */
		try {
			Thread.sleep(30000);

			byte[] req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_STATUS, null);
			if (req != null) {
				verificationRetryCount++; // 재시도 횟수 카운팅
				logger.debug("### [ACTION_IMAGE_ACTIVATE][IMAGE_ACTIVATION_INITIATED] Count={} HDLC_REQUEST => {}", verificationRetryCount, Hex.decode(req));

				session.write(req);
				result = true;
			}
		} catch (Exception e) {
			logger.equals("activationCheckRetry Error - " + e);
		}

		return result;
	}

	private boolean sendBypass() {
		boolean result = false;
		byte[] req = null;
		HashMap<String, Object> initParam = null;

		try {
			switch (this.step) {
			case HDLC_SNRM:
				req = frame.encode(HdlcObjectType.SNRM, null, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case HDLC_AARQ:
				req = frame.encode(HdlcObjectType.AARQ, null, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case HDLC_ASSOCIATION_LN:
				req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.HDLC_ASSOCIATION_LN, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_IMAGE_TRANSFER_ENABLE:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_ENABLE, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_IMAGE_BLOCK_SIZE:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_BLOCK_SIZE, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case ACTION_IMAGE_TRANSFER_INIT:
				initParam = new HashMap<String, Object>();
				initParam.put("image_identifier", imageIdentifier);
				initParam.put("image_size", String.valueOf(fwSize));

				req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.ACTION_IMAGE_TRANSFER_INIT, initParam);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => Image={}, size={} / {}", this.step.name(), initParam.get("image_identifier"), initParam.get("image_size"), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_IMAGE_TRANSFER_STATUS:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TRANSFER_STATUS, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case ACTION_IMAGE_BLOCK_TRANSFER:

				byte[] sendPacket = null;

				if (0 < remainPackateLength) {

					// 전송실패시 어디까지 보냈는지 저장하기위함.
					//resultParam.put("RESULT_VALUE", imageBlockNumber + "/" + totalImageBlockNumber);
					bypassFrameResult.setResultValue(imageBlockNumber + "/" + totalImageBlockNumber);

					if (packetSize < remainPackateLength) {
						sendPacket = new byte[packetSize];
					} else {
						sendPacket = new byte[remainPackateLength];
					}
					System.arraycopy(fwImgArray, offset, sendPacket, 0, sendPacket.length);

					initParam = new HashMap<String, Object>();
					initParam.put("image_block_number", imageBlockNumber);
					initParam.put("image_block_value", sendPacket);

					req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.ACTION_IMAGE_BLOCK_TRANSFER, initParam);

					if (req != null) {
						logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

						this.session.write(req);
						result = true;
					} else {
						needImangeBlockTransferRetry = false;
						throw new Exception("ACTION_IMAGE_BLOCK_TRANSFER Encoding Error");
					}

					logger.info("[ACTION_IMAGE_BLOCK_TRANSFER] Sended Image Block Number={}/{}, Packet Size={}, RemainPacket Size={}", imageBlockNumber, totalImageBlockNumber, sendPacket.length, (remainPackateLength - packetSize));

					remainPackateLength -= packetSize;
					offset += sendPacket.length;
					imageBlockNumber++;

					/*
					 *  재전송해야할 필요가 있는지 체크하는 타이머
					 *  10초뒤에 실행, 10초 간격으로 NEED_IMAGE_BLOCK_TRANSFER_MAX_RETRY_COUNT 만큼 재실행
					 */
					needImangeBlockTransferRetry = true;
					blockTransferRetryTask = new NeedImangeBlockTransferRetry(this.session, req, NEED_IMAGE_BLOCK_TRANSFER_MAX_RETRY_COUNT);
					blockTransferRetryTimer.scheduleAtFixedRate(blockTransferRetryTask, 10000, 10000);
				} else {
					stopTransferImageTimer();
					//logger.debug("## Timer 중지!! ==> needImangeBlockTransferRetry ={}", needImangeBlockTransferRetry);
				}

				break;
			case GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_FIRST_NOT_TRANSFERRED_BLOCK_NUMBER, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case ACTION_IMAGE_VERIFY:
				initParam = new HashMap<String, Object>();
				initParam.put("image_verify_data", (byte) 0x00);

				req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.ACTION_IMAGE_VERIFY, initParam);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_IMAGE_TO_ACTIVATE_INFO:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_IMAGE_TO_ACTIVATE_INFO, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case ACTION_IMAGE_ACTIVATE:
				initParam = new HashMap<String, Object>();
				initParam.put("image_activate_data", (byte) 0x00);

				req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.ACTION_IMAGE_ACTIVATE, initParam);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_FIRMWARE_VERSION:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_FIRMWARE_VERSION, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case HDLC_DISC:
				req = frame.encode(HdlcObjectType.DISC, null, null);
				bypassFrameResult.setFinished(true);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case ACTION_METER_ALARM_RESET:
				req = frame.encode(HdlcObjectType.ACTION_REQ, Procedure.ACTION_METER_ALARM_RESET, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case GET_BILLING_CYCLE:
				req = frame.encode(HdlcObjectType.GET_REQ, Procedure.GET_BILLING_CYCLE, null);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case SET_BILLING_CYCLE:
				req = frame.encode(HdlcObjectType.SET_REQ, Procedure.SET_BILLING_CYCLE, params);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case SET_DEMAND_PLUS_A_PERIOD:
			case SET_DEMAND_PLUS_A_NUMBER:
			case SET_DEMAND_MINUS_A_PERIOD:
			case SET_DEMAND_MINUS_A_NUMBER:
			case SET_DEMAND_PLUS_R_PERIOD:
			case SET_DEMAND_PLUS_R_NUMBER:
			case SET_DEMAND_MINUS_R_PERIOD:
			case SET_DEMAND_MINUS_R_NUMBER:
			case SET_DEMAND_R_QI_PERIOD:
			case SET_DEMAND_R_QI_NUMBER:
			case SET_DEMAND_R_QIV_PERIOD:
			case SET_DEMAND_R_QIV_NUMBER:
			case SET_DEMAND_PLUS_PERIOD:
			case SET_DEMAND_PLUS_NUMBER:
			case SET_DEMAND_MINUS_PERIOD:
			case SET_DEMAND_MINUS_NUMBER:
				req = frame.encode(HdlcObjectType.SET_REQ, this.step, params);
				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));
					this.session.write(req);
					result = true;
				}
				break;
			case GET_DEMAND_PLUS_A_PERIOD: 
			case GET_DEMAND_PLUS_A_NUMBER:
			case GET_DEMAND_MINUS_A_PERIOD: 
			case GET_DEMAND_MINUS_A_NUMBER:
			case GET_DEMAND_PLUS_R_PERIOD: 
			case GET_DEMAND_PLUS_R_NUMBER:
			case GET_DEMAND_MINUS_R_PERIOD: 
			case GET_DEMAND_MINUS_R_NUMBER:
			case GET_DEMAND_R_QI_PERIOD: 
			case GET_DEMAND_R_QI_NUMBER:
			case GET_DEMAND_R_QIV_PERIOD: 
			case GET_DEMAND_R_QIV_NUMBER:
			case GET_DEMAND_PLUS_PERIOD:
			case GET_DEMAND_PLUS_NUMBER: 
			case GET_DEMAND_MINUS_PERIOD:
			case GET_DEMAND_MINUS_NUMBER:
				req = frame.encode(HdlcObjectType.GET_REQ, this.step, null);
				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case SET_CALENDAR_NAME_PASSIVE:
				req = frame.encode(HdlcObjectType.SET_REQ, this.step, params);
				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));
					this.session.write(req);
					result = true;
				}
				break;
			case SET_SEASON_PROFILE:
				touExcuteTOUProfileArray = touSeasonInfoArray;

				if (remainExcuteTOUPacketLength == -1) { // 초기에 한번만 넣기위함.
					remainExcuteTOUPacketLength = touExcuteTOUProfileArray.length;
				}

				result = setProcedureExcute();
				break;
			case SET_WEEK_PROFILE:
				touExcuteTOUProfileArray = touWeekInfoArray;

				if (remainExcuteTOUPacketLength == -1) { // 초기에 한번만 넣기위함.
					remainExcuteTOUPacketLength = touExcuteTOUProfileArray.length;
				}

				result = setProcedureExcute();
				break;
			case SET_DAY_PROFILE:
				touExcuteTOUProfileArray = touDayInfoArray;

				if (remainExcuteTOUPacketLength == -1) { // 초기에 한번만 넣기위함.
					remainExcuteTOUPacketLength = touExcuteTOUProfileArray.length;
				}

				result = setProcedureExcute();
				break;
			case SET_STARTING_DATE:
				req = frame.encode(HdlcObjectType.SET_REQ, this.step, params);
				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));
					this.session.write(req);
					result = true;
				}
				break;
			case GET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME:
			case GET_CURRENT_LOAD_LIMIT_THRESHOLD:
				req = frame.encode(HdlcObjectType.GET_REQ, this.step, null);
				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			case SET_CURRENT_LOAD_LIMIT_DURATION_JUDGE_TIME:
			case SET_CURRENT_LOAD_LIMIT_THRESHOLD:
				req = frame.encode(HdlcObjectType.SET_REQ, this.step, params);

				if (req != null) {
					logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

					this.session.write(req);
					result = true;
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error("BYPASS_GD SEND ERROR - {}", e);
			result = false;
		}

		return result;
	}

	/*
	 * TOU profile Set처리
	 */
	private boolean setProcedureExcute() throws Exception {
		boolean result = false;
		HashMap<String, Object> initParam = new HashMap<String, Object>();
		initParam.put("infoBlockType", infoBlockType);
		logger.debug("Command = {}, Procedure = {}, BlockType = {}", command, step, infoBlockType);

		if (infoBlockType == TOUInfoBlockType.FIRST_BLOCK) {
			int firstBlockSize = sendHdlcPacketMaxSize - 23;
			middleBlockSize = sendHdlcPacketMaxSize - 13;

			//int aSize = firstBlockSize + 16;
			int aSize = sendHdlcPacketMaxSize - 16;
			if (aSize < touExcuteTOUProfileArray.length) { // Multi모드로 전송
				initParam.put("isLastBlock", false);
				initParam.put("blockNumber", touBlockNumber);
				initParam.put("blockLength", firstBlockSize);

				byte[] sendTouInfoPacket = new byte[firstBlockSize];
				System.arraycopy(touExcuteTOUProfileArray, touExcuteTOUProfileOffset, sendTouInfoPacket, 0, sendTouInfoPacket.length);

				initParam.put("blockValue", sendTouInfoPacket);

				remainExcuteTOUPacketLength -= firstBlockSize;
				touExcuteTOUProfileOffset += sendTouInfoPacket.length;
				logger.debug("## TOUInfoBlockType.FIRST_BLOCK Multi Mode.");
			} else { //Single모드 전송
				initParam.put("blockValue", touExcuteTOUProfileArray);
				remainExcuteTOUPacketLength -= touExcuteTOUProfileArray.length;
				logger.debug("## TOUInfoBlockType.FIRST_BLOCK Single Mode.");
			}
		} else if (infoBlockType == TOUInfoBlockType.MIDDLE_BLOCK) {

			byte[] sendTouInfoPacket = null;

			if (0 < remainExcuteTOUPacketLength) {
				sendTouInfoPacket = new byte[middleBlockSize];
				initParam.put("isLastBlock", false);

				System.arraycopy(touExcuteTOUProfileArray, touExcuteTOUProfileOffset, sendTouInfoPacket, 0, sendTouInfoPacket.length);

				initParam.put("blockNumber", touBlockNumber);
				initParam.put("blockLength", sendTouInfoPacket.length);
				initParam.put("blockValue", sendTouInfoPacket);

				remainExcuteTOUPacketLength -= middleBlockSize;
				touExcuteTOUProfileOffset += sendTouInfoPacket.length;
			}
		} else if (infoBlockType == TOUInfoBlockType.LAST_BLOCK) {
			byte[] sendTouInfoPacket = new byte[remainExcuteTOUPacketLength];
			initParam.put("isLastBlock", true);

			System.arraycopy(touExcuteTOUProfileArray, touExcuteTOUProfileOffset, sendTouInfoPacket, 0, sendTouInfoPacket.length);

			initParam.put("blockNumber", touBlockNumber);
			initParam.put("blockLength", sendTouInfoPacket.length);
			initParam.put("blockValue", sendTouInfoPacket);

			remainExcuteTOUPacketLength -= sendTouInfoPacket.length;
		}

		byte[] req = frame.encode(HdlcObjectType.SET_REQ, this.step, initParam);

		logger.info("[{}] Sended TOU Block Number={}, Packet Size={}, RemainPacket Size={}/{}", this.step, touBlockNumber, ((byte[]) initParam.get("blockValue")).length, remainExcuteTOUPacketLength, touExcuteTOUProfileArray.length);

		if (req != null) {
			logger.debug("### [{}] HDLC_REQUEST => {}", this.step.name(), Hex.decode(req));

			this.session.write(req);
			result = true;
		} else {
			throw new Exception("SET_DAY_PROFILE Encoding Error");
		}

		//초기화 : 마지막 블럭이거나, Single블럭일경우
		if (infoBlockType == TOUInfoBlockType.LAST_BLOCK || (infoBlockType == TOUInfoBlockType.FIRST_BLOCK && remainExcuteTOUPacketLength < 1)) {
			touExcuteTOUProfileOffset = 0;
			remainExcuteTOUPacketLength = -1;
			touBlockNumber = 1;

			infoBlockType = TOUInfoBlockType.FIRST_BLOCK;
		} else {
			touBlockNumber++;
		}

		return result;
	}

	@Override
	public void stop(IoSession session) {
		byte[] frameArray = frame.encode(HdlcObjectType.DISC, null, null);

		if (frameArray != null) {
			this.step = Procedure.HDLC_DISC;

			logger.debug("### Stop Bypass ~ !! => {}", Hex.decode(frameArray));
			logger.debug("### Stop Bypass ~ !! => {}", Hex.decode(frameArray));
			logger.debug("### Stop Bypass ~ !! => {}", Hex.decode(frameArray));

			this.session.write(frameArray);
		}

	}

	/**
	 * Timer, Timer Task cancel
	 */
	private void stopTransferImageTimer() {
		needImangeBlockTransferRetry = false;
		blockTransferRetryTask.cancel();
		blockTransferRetryTimer.cancel();
		blockTransferRetryTimer = null;
		logger.debug("## Timer Task Stop.");
	}

	/**
	 * Image Block Transfer를 반복 실행하는 TimerTask
	 * 
	 * @author simhanger
	 *
	 */
	protected class NeedImangeBlockTransferRetry extends TimerTask {
		private IoSession session;
		private byte[] req;
		private int maxRetryCount;
		private int retryCount;

		public NeedImangeBlockTransferRetry(IoSession session, byte[] req, int maxRetryCount) {
			this.session = session;
			this.req = req;
			this.maxRetryCount = maxRetryCount;
		}

		@Override
		public void run() {
			if (needImangeBlockTransferRetry == true && this.retryCount < this.maxRetryCount) {
				logger.info("[ACTION_IMAGE_BLOCK_TRANSFER] Retry={} Sended Image Block Number={}/{}", retryCount + 1, imageBlockNumber, totalImageBlockNumber);
				this.session.write(this.req);
				this.retryCount++;
			} else {
				this.cancel();
				logger.debug("NeedImangeBlockTransferRetry cancle!!");
				stop(session);
			}

		}

	}

    @Override
    public BypassFrameResult receiveBypass(MultiSession session, byte[] rawFrame) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean start(MultiSession session, Object type) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void stop(MultiSession session) {
        // TODO Auto-generated method stub
        
    }

	//	protected class RetrySendTimerTask extends TimerTask{
	//		private IoSession session;
	//		private byte[] req;
	//		private int maxRetryCount;
	//		private int retryCount;
	//		
	//		public RetrySendTimerTask(IoSession session, byte[] req, int maxRetryCount) {
	//			this.session = session;
	//			this.req = req;
	//			this.maxRetryCount = maxRetryCount;
	//		}
	//
	//		@Override
	//		public void run() {
	//			if(RetrySendTaskFlag == true && this.retryCount < this.maxRetryCount){
	//				this.session.write(this.req);
	//				logger.info("[Retry Send Timer Task] Retry={}/{}", this.retryCount+1, this.maxRetryCount);
	//				
	//				this.retryCount++;
	//			}else{
	//				this.cancel();
	//				logger.debug("RetrySendTimerTask cancle!!");
	//				stop(session);
	//			}
	//			
	//		}
	//		
	//	}
}
