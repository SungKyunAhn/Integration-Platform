package com.aimir.fep.iot.service;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.aimir.dao.iot.DcuDao;
import com.aimir.dao.iot.EventDao;
import com.aimir.dao.iot.LocDao;
import com.aimir.dao.iot.SensorDao;
import com.aimir.dao.mvm.MeasurementHistoryDao;
import com.aimir.fep.iot.dao.CntrDAO;
import com.aimir.fep.iot.dao.CustomerDAO;
import com.aimir.fep.iot.dao.EventLogDAO;
import com.aimir.fep.iot.dao.LocDAO;
import com.aimir.fep.iot.dao.RssiDAO;
import com.aimir.fep.iot.domain.common.RSCException;
import com.aimir.fep.iot.domain.resources.ContentInstance;
import com.aimir.fep.iot.domain.resources.Resource;
import com.aimir.fep.iot.model.vo.BeaconVO;
import com.aimir.fep.iot.model.vo.DCUVO;
import com.aimir.fep.iot.model.vo.RSSIVO;
import com.aimir.fep.iot.model.vo.SensorVO;
import com.aimir.fep.iot.notification.NotificationService;
import com.aimir.fep.iot.saver.ContentInstanceDataSaver;
import com.aimir.fep.iot.service.action.ContentInstanceAction;
import com.aimir.fep.iot.service.dao.ContentInstanceDao;
import com.aimir.fep.iot.service.dao.RemoteCseDao;
import com.aimir.fep.iot.utils.CommonCode;
import com.aimir.fep.iot.utils.CommonCode.CNTR_TYPE;
import com.aimir.fep.iot.utils.CommonCode.DEVICE_TYPE;
import com.aimir.fep.iot.utils.CommonCode.RESOURCE_TYPE;
import com.aimir.fep.iot.utils.CommonUtil;
import com.aimir.fep.iot.utils.SequenceUtils.SEQ_PREFIX;
import com.aimir.fep.protocol.fmp.processor.ProcessorHandler;
import com.aimir.fep.util.FMPProperty;
import com.aimir.model.device.MeasurementHistory;
import com.aimir.model.iot.DcuTbl;
import com.aimir.model.iot.EventTbl;
import com.aimir.model.iot.HeartBeatTbl;
import com.aimir.model.iot.LocTbl;
import com.aimir.model.iot.PamperTbl;
import com.aimir.model.iot.SensorTbl;
import com.aimir.util.DateTimeUtil;

@Service
public class ContentInstanceService {
	
	@Autowired
	SensorPeriodService sensorPeriodService;
	
	private static final Log logger = LogFactory.getLog(ContentInstanceService.class);
	private static final String wsAddr = FMPProperty.getProperty("websocket.server.location.addr");
	private static final String SENSOR_CONF="sensorCnf";
	private static final String WD_CONF="wdCnf";
	private static final String HRM ="hrm";
	private static final String RSSI = "rssi";
	private static final String GPS = "gps";
	private static final String PERIOD = "period";
	//추가
	//2016 11 28 차병준
	private static final String TIME = "time";
	private static final String STATUS = "status";
	private static final String TRD = "trd";
	
	//추가
	//2016 12 06 차병준
	private static final String BEACON_CONF = "beaconCnf";
	private static final String MSG_TYPE = "msgType";
	private static final String PAIR_ID = "pairId";
	
	//2017 04 26 차병준
	//변압기
	private static final String VIB = "vib";
	private static final String DEGREE = "degree";
	private static final String NOISE = "noise";
	private static final String HUM = "hum";
	private static final String E_PERIOD = "period";
	private static final String TEMP = "temp";
	
	private Node e_periodNode;
	private String e_periodInfo [] ={"0","0","0","0","0"};
	
	private String smsKey = "";
	private String empNo = "";
	private String recvphone = "";
	private boolean isSos = false;
	private String eventMsg = "";
	private String sosMsg = "";
	//추가
	//2017 03 07 차병준
	//노드가 없을떄 예외처리
	private Node sInfoNode; 
//	private String	sensorInfo [] = {"0","0","0","0","0"};
	private String	sensorInfo [] = {"0","0","0","0"};

	private Node gpsNode;
	private String locInfo [] = {"00:00:00","0","0"};
	
	private Node periodNode;
	//private String periodInfo [] ={"0","0","0","0","0","0","0","0"};
	private String periodInfo [] ={"0","0","0","0"};
	
	private Node threedNode; 
	private String threedInfo[] = {"00:00:00","0","0","0","0"};

	private Node statusNode;
	private String status [] = {"TO"};

	private Node timeNode;
	private String sensorTime [] = {"0000/00/00 00:00:00"};
	
	private Node node;
	private String hrmInfo [] = {"00:00:00","0"};
	
	//2017 04 26 차병준
	private Node vibNode;
	private String vibInfo [] = {"00:00:00","0"};
	
	private Node degreeNode;
	private String degreeInfo [] = {"00:00:00","0","0"};
	
	private Node noiseNode;
	private String noiseInfo [] = {"00:00:00","0"};
	
	private Node humNode;
	private String humInfo [] = {"00:00:00","0"};
	
	private Node tempNode;
	private String tempInfo [] = {"00:00:00","0","0"};
	
	String splitStr = ",";
	String splitStrC = "{";

    @Autowired
    CntrDAO dao;
    
    @Autowired
    RemoteCseDao remoteCSEDao;
    
    /*@Autowired
    SensorDAO sDAO;*/
    
    @Autowired
    LocDAO locDAO;

    @Autowired
    RssiDAO rssiDAO;
    
    @Autowired
    CustomerDAO custDao;
    
    @Autowired
    EventLogDAO eventLogDAO;
    
    @Autowired
    NotificationService notiService;
    
    
    /**
     * IoT (by ask) 
     * @Autowired
     */
    
    @Autowired
    private ContentInstanceDataSaver saver;
    
    @Autowired
    DcuDao dcuDao;
    
    @Autowired
    SensorDao sensorDao;
    
    @Autowired
    LocDao locDao;
    
    @Autowired
    EventDao eventDao;
    
	@Autowired
	MeasurementHistoryDao measurementHistoryDao;
	
	@Autowired
	ProcessorHandler processorHandler;

	@Autowired
	ContentInstanceDao contentInstanceDao;
	
	
	/**
	 * ContentInstance data parsing & saver
	 * @param cntIns
	 * @return
	 * 				... modified by ask
	 */
    public boolean insertContentInstance(String seq, ContentInstance cntIns){
    	logger.debug("###insertContentInstance : [" + cntIns.getResourceName() + "]");
    	
    	String cntrType = cntIns.getResourceName(); //containerName
    	String content = cntIns.getContent();
    	String rplStrBS = "\\[";
    	String rplStrBE = "\\]";
    	String rplStrAS = "<";
    	String rplStrAE = ">";
    	//2016 11 29 차병준
    	//xml 파싱할때 엘레먼트가명에 숫자가 앞에 있으면 규칙에 안맞아서 파싱이 안됨
    	// 그래서 변경 필요헤사 추가
    	String rplStr3dS = "3d";
    	String rplStr3dE = "trd";
    	
    	String cnt = content.replaceAll(rplStrBS, rplStrAS);
		String xmlStr =cnt.replaceAll(rplStrBE, rplStrAE);
    	
		xmlStr = xmlStr.replaceAll(rplStr3dS, rplStr3dE);
		Document doc = this.parsingContent("<root>"+xmlStr+"</root>");

    	boolean rst = true;
    	if(content == null || content.length() == 0){
    		return false;
    	}
    	logger.debug("### ["+seq+"] ContentInstance Saver : "+ CNTR_TYPE.getCntrType(cntrType) +"[ "+doc+" ]");

    	switch (CNTR_TYPE.getCntrType(cntrType)) {
    		case DCU_CONF:
    			logger.debug("### ["+seq+"] DCU_CONF Update start : "+ CNTR_TYPE.getCntrType(cntrType));
    			rst = this.updateRemoteCSEInfo(seq, cntIns, content.split(splitStr));
    			
    			logger.debug("### ["+seq+"] DCU_CONF Update end ["+rst+"]");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
    			break;
    		case SENSOR_INFO:
    			logger.debug("### ["+seq+"] SENSOR_INFO Saver start : "+ CNTR_TYPE.getCntrType(cntrType));
    			
    			if(content == null || content.isEmpty()){
        			logger.error("["+seq+"] SENSOR_INFO ERROR : Invalid conetent " );
        			return false;
        		}
    			
        		rst = this.processSensorElectricInfo(seq, cntIns, doc.getElementsByTagName(SENSOR_CONF), doc.getElementsByTagName(VIB), doc.getElementsByTagName(DEGREE),doc.getElementsByTagName(NOISE),doc.getElementsByTagName(HUM),doc.getElementsByTagName(STATUS),doc.getElementsByTagName(TIME),doc.getElementsByTagName(E_PERIOD),doc.getElementsByTagName(TEMP));
    			
        		logger.debug("### ["+seq+"] SENSOR_INFO Saver end ["+rst+"]");
        		logger.debug("##########################################");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
        		break;
    		case WD_INFO:
    			logger.debug("### ["+seq+"] WD_INFO Saver start : "+ CNTR_TYPE.getCntrType(cntrType)+" ["+cntIns+"]");

        		// 수집센서 G/w 통신 시간 업데이트
        		dcuDao.updateLastCommDt(cntIns.getParentID());
        		logger.debug("## Dcu LastCommDt is Update : ["+ cntIns.getParentID() +"]");
        		
        		if(content == null || content.isEmpty()){
        			logger.error("["+seq+"] WD_INFO ERROR : Invalid conetent");
        			return false;
        		}
        		
    			//센서 정보 갱신
    			rst = this.processSensorGPSInfo(seq, cntIns, doc.getElementsByTagName(WD_CONF), doc.getElementsByTagName(GPS), doc.getElementsByTagName(PERIOD),doc.getElementsByTagName(TIME),doc.getElementsByTagName(STATUS),doc.getElementsByTagName(TRD));
    			//심박수 등록
    			rst = this.insertHeartR(seq, cntIns, doc.getElementsByTagName(HRM),doc.getElementsByTagName(STATUS));
    			//만보기 이력
    			rst = this.insertPamper(seq, cntIns, doc.getElementsByTagName(TRD),doc.getElementsByTagName(STATUS));
    			
    			logger.debug("### ["+seq+"] WD_INFO Saver end ["+rst+"]");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
    			break;	
    		case BLE_INFO:
    			logger.debug("### ["+seq+"] BLE_INFO Saver start : "+ CNTR_TYPE.getCntrType(cntrType));
    			
    			// 수집센서 G/w 통신 시간 업데이트
        		dcuDao.updateLastCommDt(cntIns.getParentID());
        		
        		if(content == null || content.isEmpty()){
        			logger.error("### ["+seq+"] BLE_INFO ERROR : Invalid conetent");
        			return false;
        		}
        		
        		rst = this.bleInfo(seq, cntIns, doc.getElementsByTagName(BEACON_CONF), doc.getElementsByTagName(MSG_TYPE), doc.getElementsByTagName(PAIR_ID),doc.getElementsByTagName(TIME));
        		
        		logger.debug("### ["+seq+"] BLE_INFO Saver end ["+rst+"]");
        		logger.debug("##########################################");
    			logger.debug("##########################################");
    			logger.debug("##########################################");
    			break;
    		default:
    			rst = false;
        }

    	/* 사회안전망에 쓰지 않는 데이터 구분이라 주석처리
    	 * 						... by ask
    	 * if("dcuInfo".equals(cntrType)){
    		remoteCSEDao.updateLastCommDt(cntIns.getParentID());
    		// 수집센서의 통신 시간 및  TAS, Coordi변경
    		rst = this.updateEtcFWver(cntIns.getParentID(), content.split(splitStr), cntIns.getCreationTime());

    	}else if("cmrInfo".equals(cntrType)){
    		// 서버로 직통
    		System.out.println("cmrInfocmrInfocmrInfocmrInfo");
    		rst = this.insertCmrLog(cntIns);
    		
    	}else if("cuttingareaInfo".equals(cntrType)){
    		// 수집센서 통신 시간 변경
    		remoteCSEDao.updateLastCommDt(cntIns.getParentID());
    		if(content == null || content.isEmpty()){
    			logger.error("ERROR : Invalid conetent " );
    			return false;
    		}
    		rst = this.processSensorAngleInfo(cntIns, doc.getElementsByTagName(SENSOR_CONF), doc.getElementsByTagName(GPS), doc.getElementsByTagName(PERIOD));
    	}*/
		return rst;
	}
    
	// 만보기
	private boolean insertPamper(String seq, ContentInstance cntIns, NodeList nodes, NodeList statusNodes){
		int max = 95;
		int min = 50;
		boolean hrmChk = false;
		
		logger.debug("## ["+seq+"] Pamper Data Saver is start ~~ ");
		/*
		 * 2017 03 08 차병준
		 * hrm 노드 체크
		 * 노드가 없을때 프로세스 진행시 에러 발생을 하여 추가 하였음.
		 */
		if(nodes.item(0) != null){
			threedNode  = nodes.item(0);
			String cnts = threedNode .getTextContent();
			threedInfo  = cnts.split(splitStr);
			hrmChk = true;
		}
		
	  	if(statusNodes.item(0) != null){
	  		statusNode = statusNodes.item(0);
	  		status = statusNode.getTextContent().split(splitStr);
	  	}
	  	String statusInfo = statusNode.getTextContent();
	  	
		//2016 11 28 차병준
		String hrmTm = threedInfo[0].replaceAll(":", "");
		Double hrmRate = Double.valueOf(threedInfo[4]);
		Double hrmMin = Double.valueOf(FMPProperty.getProperty("hrm.min"));
		Double hrmMax = Double.valueOf(FMPProperty.getProperty("hrm.max"));
		
		PamperTbl pamper = new PamperTbl();
		EventTbl event = new EventTbl();
		
		//heartBeat event save
		event = saver.CiPamperEventDataSave(cntIns, pamper, event, nodes, sensorInfo, hrmRate, hrmMin, hrmMax, hrmTm);
		logger.debug("## ["+seq+"] 1. Pamper Event save end ~~ ");
		
		//heartBeat data save
		pamper = saver.CiPamperDataSave(pamper, cntIns, sensorInfo, hrmRate, hrmMin, hrmMax, hrmTm, statusInfo);
		logger.debug("## ["+seq+"] 2. Pamper save end ~~ ");
		
		/*중복제거
		 * HeartBeatVO vo = hbDAO.selectPamper(hbVO); by ask*/
		
		/*if(vo != null){
			logger.error(" 동일 만보기 존재" );
			return false;
		}else{
			if(hrmChk == true){
				hbDAO.insertPamper(hbVO);
			}
		}by ask*/
		return true;
	}
   
    /**
     * BeaCon 데이터 저장단
     * @param seq
     * @param cntIns
     * @param binfoNodes
     * @param msgNodes
     * @param pairNodes
     * @param timeNodes
     * @return
     * 					... modified by ask
     */
	private boolean bleInfo(String seq, ContentInstance cntIns, NodeList binfoNodes, NodeList msgNodes, NodeList pairNodes,NodeList timeNodes) {
		// TODO Auto-generated method stub
		Node bInfoNode = binfoNodes.item(0);
    	String bleInfo [] = bInfoNode.getTextContent().split(splitStr);
    	
    	Node msgNode = msgNodes.item(0);
    	String msgInfo [] = msgNode.getTextContent().split(splitStr);
    	
    	Node pairNode = pairNodes.item(0);
    	String pairId [] = pairNode.getTextContent().split(splitStr);
    	
      	if(timeNodes.item(0) != null){
	  		timeNode = timeNodes.item(0);
	  		sensorTime = timeNode.getTextContent().split(splitStr);
	  	}
    	
	  	String sensorTm = sensorTime[0].replaceAll("/", "");
	  	sensorTm = sensorTm.replaceAll(":", "");
	  	sensorTm = sensorTm.replaceAll(" ", "");
	  	cntIns.setCreationTime(sensorTm);
	  	
	  	saver.CiBleInfoDataSave(cntIns, bleInfo, msgInfo, pairId);

	  	
    	 /** 2017년 03월 03일 차병준
    	 * ble msgType값 확인ㄴ
    	 * msgType value : PAIR이면 G/W에 G/W끄는 명령어 보냄
    	 * "이설정에 의해 GPS, HRM, 3D 정보 수집 여부가 결정된다. 각 정보는 Bit mask로 구성되며 다음과 같은 값을 가진다.
            - 0x01: GPS, 0x02: HRM, 0x04: 3D, 0x08: Temperature, 0x10: Humidity, 0x20: Noise"
            
            pair이면 02 + 04
            unpair 01 + 02 + 04*/
 
		//	if(msgInfo[0].equals("PAIR")){
	    //	   sensorPeriodService.sendWearableInfoCmd(pairIdTemp,"0x06");
		//	}else{
		//		sensorPeriodService.sendWearableInfoCmd(pairIdTemp,"0x07");	
		//	}
    	return true;
	}

	private boolean processSensorAngleInfo(ContentInstance cntIns, NodeList sinfoNodes, NodeList gpsNodes, NodeList periodNodes) {
		// TODO Auto-generated method stub
		Node sInfoNode = sinfoNodes.item(0);
    	String sensorInfo [] = sInfoNode.getTextContent().split(splitStr);
    	Node gpsNode = gpsNodes.item(0);
	  	String locInfo [] = gpsNode.getTextContent().split(splitStr);
	  	Node periodNode = periodNodes.item(0);
	  	String periodInfo [] = periodNode.getTextContent().split(splitStr);
	  	Double x=0.0, y =0.0, z=0.0, resultDegrees=0.0;
	  	
	  	if(locInfo!=null && locInfo.length>2){
	  		
		  	 x = Double.parseDouble(locInfo[0]);
	    	 y = Double.parseDouble(locInfo[1]);
	    	 z = Double.parseDouble(locInfo[2]);
	    	 resultDegrees = Math.atan2(x,y*y + z*z ) * 57.2958 ; //test용. 실제 값 받아와서 쓰는건 아랫줄 쓰기
	 	  	
	  	}
    	if(sensorInfo.length != 5) {
			logger.error(sensorInfo.length + " ERROR " );
			return false;
		}

	  	// 사이즈 체크, GPS정보가 없을 경우는 에러 발생
	  	if(CommonUtil.isEmpty(cntIns.getCreationTime()) ) {
	  		
			logger.error( " ERROR : Invalid location info " );
			return false;
		}
	  	
	  	/*DetSnrVO sVO = sDAO.selectDSensor(cntIns.getCreator());by ask*/
    	// 센서 신규 등록
	
    	/*if(sVO == null){
    		sVO.setSID(cntIns.getCreator());
    		sVO.setPARENT_DEVICE_ID(cntIns.getParentID());
    		sVO.setPARENT_DEVICE_TYPE("2");
    		sVO.setGPIOX(x);
    		sVO.setGPIOY(y);
    		sVO.setGPIOZ(z);
    		sVO.setINCLINEDEG(resultDegrees);
    		sVO.setWAKEUP_PERIOD(periodInfo[0]);
    		sVO.setGPS_PERIOD(periodInfo[1]);
    		sVO.setHRM_PERIOD(periodInfo[2]);
    		sVO.setBEACON_PERIOD(periodInfo[3]);
    		sVO.setFW_VER(sensorInfo[1]);
    		sVO.setHW_VER(sensorInfo[2]);
    		sVO.setVENDOR(sensorInfo[3]);
    		sVO.setMODEL(sensorInfo[4]);
    		
    		if(sVO.getMIN_THRESHOLD()!=null && sVO.getMAX_THRESHOLD()!=null && sVO.getINCLINEDEG()!=null){
        		if (sVO.getMIN_THRESHOLD()>=sVO.getINCLINEDEG()){
        			sVO.setSTATUS_CD("2");
        		}else if (sVO.getINCLINEDEG()>=sVO.getMAX_THRESHOLD()){
        			sVO.setSTATUS_CD("3");
        		}else{
        			sVO.setSTATUS_CD("1");
        		}
    		}else{
    			sVO.setSTATUS_CD("0"); //미측정
    		}
        	sDAO.insertDSensor(sVO); 
    	} else { // 센서및 GPS정보 업그레이드
    		sVO.setSID(cntIns.getCreator());
    		sVO.setPARENT_DEVICE_ID(cntIns.getParentID());
    		sVO.setPARENT_DEVICE_TYPE("2");
    		sVO.setGPIOX(x);
    		sVO.setGPIOY(y);
    		sVO.setGPIOZ(z);
    		sVO.setINCLINEDEG(resultDegrees);
    		sVO.setWAKEUP_PERIOD(periodInfo[0]);
    		sVO.setGPS_PERIOD(periodInfo[1]);
    		sVO.setHRM_PERIOD(periodInfo[2]);
    		sVO.setBEACON_PERIOD(periodInfo[3]);
    		sVO.setFW_VER(sensorInfo[1]);
    		sVO.setHW_VER(sensorInfo[2]);
    		sVO.setVENDOR(sensorInfo[3]);
    		sVO.setMODEL(sensorInfo[4]);
    		
    		if(sVO.getMIN_THRESHOLD()!=null && sVO.getMAX_THRESHOLD()!=null && sVO.getINCLINEDEG()!=null){
        		 if (sVO.getMIN_THRESHOLD()>=sVO.getINCLINEDEG()){
        			sVO.setSTATUS_CD("2");
        		}else if (sVO.getINCLINEDEG()>=sVO.getMAX_THRESHOLD()){
        			sVO.setSTATUS_CD("3");
        		}else{
        			sVO.setSTATUS_CD("1");
        		}
    		}else{
    			sVO.setSTATUS_CD("0"); //미측정
    		}
    		sDAO.updateDSensor(sVO);
    	} by ask*/
    	
		HashMap<String, String> map = new HashMap<String, String>();
    	map.put("SID", cntIns.getCreator());
    	map.put("PARENT_DEVICE_TYPE", cntIns.getParentID());
    	map.put("PARENT_DEVICE_ID", "2");
    	map.put("GPIOX", locInfo[0]);
    	map.put("GPIOY", locInfo[1]);
    	map.put("GPIOZ", locInfo[2]);
    	map.put("INCLINEDEG", Double.toString(resultDegrees));
    	map.put("YEAR", cntIns.getCreationTime().substring(0, 4));
    	map.put("MONTH", cntIns.getCreationTime().substring(4, 6));
    	map.put("DAY", cntIns.getCreationTime().substring(6, 8));
    	map.put("HHMMSS", cntIns.getCreationTime().substring(8, 14));
    	map.put("YYYYMMDDHHMMSS", cntIns.getCreationTime());
		
    	/*eventLogDAO.insertDSensorLog(map); by ask*/
		return false;
	}
	
	
	private boolean insertCmrLog(ContentInstance cntIns) {
		// TODO Auto-generated method stub
		StringTokenizer st = new StringTokenizer(cntIns.getContent(), ",");
		List<String> content = new ArrayList<String>();
		while(st.hasMoreTokens()){
			content.add(st.nextToken());
		}

		HashMap<String, String> cmrInfo = new HashMap<String, String>();
		cmrInfo.put("CMR_ID", cntIns.getCreator());
		cmrInfo.put("CMR_IP", content.get(0));
		cmrInfo.put("EVENT_CD", content.get(1));
		cmrInfo.put("FILENAME", content.get(2));
		cmrInfo.put("CMR_DT",cntIns.getCreationTime());
		/*if(eventLogDAO.selectCmr(content.get(0))!=null){
			eventLogDAO.updateCmrInfo(cmrInfo);
		}else{
			eventLogDAO.insertCmrInfo(cmrInfo);
		}
		
		eventLogDAO.insertCmrLog(cmrInfo); by ask*/
		return false;
	}

	/**
	 * 사회안전망 G/W 저장단
	 * @param seq
	 * @param cnts
	 * @param ct
	 * @return
	 * 			....  by ask
	 */
    private boolean updateRemoteCSEInfo(String seq, ContentInstance cntIns, String[] cnts){
    	if(cnts.length != 5) {
			logger.error(cnts.length + " ERROR " );
			return false;
		}
    	//DCU_CONF : cntIns createtime이 이상하게 들오옴 확인 요청 (by ask)
    	String ct = cntIns.getCreationTime().replaceAll(":", "");
    	ct = ct.replaceAll("T", "");
    	ct = ct.replaceAll("-", "");
    	ct = ct.substring(0, 14);
    	cntIns.setCreationTime(ct);
    	
    	saver.CiDcuConfDataSave(seq, cnts, ct);

    	return true;
    }
    
   /**
    * Electrict Sensor 저장단
    * @param seq
    * @param cntIns
    * @param sinfoNodes
    * @param vibNodes
    * @param degreeNodes
    * @param noiseNodes
    * @param humNodes
    * @param statusNodes
    * @param timeNodes
    * @param periodNodes
    * @param tempNodes
    * @return
    * 				....  by ask
    */
   private boolean processSensorElectricInfo(String seq, ContentInstance cntIns, NodeList sinfoNodes, NodeList vibNodes, NodeList degreeNodes, NodeList noiseNodes, NodeList humNodes, NodeList statusNodes, NodeList timeNodes, NodeList periodNodes, NodeList tempNodes){
	 	/*
    	 * 2017 03 07 차병준
    	 * 노드에 값이 있는지 체크, 값이 없으면 기본값 셋팅되게 수정
    	 */
	   	//	String sensorInfo [] = {"0","0","0","0","0"};
    	if(sinfoNodes.item(0) != null){
    		sInfoNode = sinfoNodes.item(0);
    	    sensorInfo = sInfoNode.getTextContent().split(splitStr);
    	}
    	if(vibNodes.item(0) != null){
    		vibNode = vibNodes.item(0);
    		vibInfo = vibNode.getTextContent().split(splitStr);
    	}
     	//3d 시간
    	// 	String vibTm = threedInfo[0].replaceAll(":", "");
    	
    	if(periodNodes.item(0) != null){
    		e_periodNode = periodNodes.item(0);
    		e_periodInfo  = e_periodNode.getTextContent().split(splitStr);
    	}
    	// 	Double locX=0.0, locY =0.0;

	  	if(degreeNodes.item(0) != null){
	  		degreeNode = degreeNodes.item(0);
	  		degreeInfo = degreeNode.getTextContent().split(splitStr);
	  	}

	  	//status
	  	if(statusNodes.item(0) != null){
	  		statusNode = statusNodes.item(0);
	  		status = statusNode.getTextContent().split(splitStr);
	  	}
	  	String statusInfo = statusNode.getTextContent();
	  	
	  	if(tempNodes.item(0) != null){
	  		tempNode = tempNodes.item(0);
	  		tempInfo = tempNode.getTextContent().split(splitStr);
	  	}
	  	
	  	if(noiseNodes.item(0) != null){
	  		noiseNode = noiseNodes.item(0);
	  		noiseInfo = noiseNode.getTextContent().split(splitStr);
	  	}
	  	
	  	if(humNodes.item(0) != null){
	  		humNode = humNodes.item(0);
	  		humInfo = humNode.getTextContent().split(splitStr);
	  	}

	  	if(timeNodes.item(0) != null){
	  		timeNode = timeNodes.item(0);
	  		sensorTime = timeNode.getTextContent().split(splitStr);
	  	}
	  	String sensorTm = sensorTime[0].replaceAll("/", "");
	  	sensorTm = sensorTm.replaceAll(":", "");
	  	sensorTm = sensorTm.replaceAll(" ", "");
	  	cntIns.setCreationTime(sensorTm);
	  	
	  	//Sensor_Info saver
	  	saver.CiSensorInfoDataSave(cntIns, sensorInfo, vibInfo, statusInfo, degreeInfo, noiseInfo, tempInfo, humInfo, e_periodInfo);

	return true;
    }

    /**
     * ContentInstance 저장단
     * @param cntIns
     * @param sinfoNodes
     * @param gpsNodes
     * @param periodNodes
     * @param timeNodes
     * @param statusNodes
     * @param threedNodes
     *                ....  by ask
     * @return
     */
    private boolean processSensorGPSInfo(String seq, ContentInstance cntIns, NodeList sinfoNodes, NodeList gpsNodes, NodeList periodNodes, NodeList timeNodes, NodeList statusNodes, NodeList threedNodes){

    	/*
    	 * 2017 03 07 차병준
    	 * 노드에 값이 있는지 체크, 값이 없으면 기본값 셋팅되게 수정
    	 */
    	if(sinfoNodes.item(0) != null){
    		sInfoNode = sinfoNodes.item(0);
    	    sensorInfo = sInfoNode.getTextContent().split(splitStr);
    	}
    	if(gpsNodes.item(0) != null){
    		gpsNode = gpsNodes.item(0);
    		locInfo = gpsNode.getTextContent().split(splitStr);
    	}
    	if(periodNodes.item(0) != null){
    		periodNode = periodNodes.item(0);
    		periodInfo  = periodNode.getTextContent().split(splitStr);
    	}
	  	Double locX=0.0, locY =0.0;
	  	
	  	//추가
	  	//2016 11 28 차병준
	  	if(threedNodes.item(0) != null){
	  		threedNode = threedNodes.item(0);
	  		threedInfo = threedNode.getTextContent().split(splitStr);
	  	}
	  	//3d 시간
	  	String threedTm = threedInfo[0].replaceAll(":", "");
	  	//status
	  	if(statusNodes.item(0) != null){
	  		statusNode = statusNodes.item(0);
	  		status = statusNode.getTextContent().split(splitStr);
	  	}
	  	String statusInfo = statusNode.getTextContent();
	  	
	  	if(timeNodes.item(0) != null){
	  		timeNode = timeNodes.item(0);
	  		sensorTime = timeNode.getTextContent().split(splitStr);
	  	}
	  	String sensorTm = sensorTime[0].replaceAll("/", "");
	  	sensorTm = sensorTm.replaceAll(":", "");
	  	sensorTm = sensorTm.replaceAll(" ", "");
	  	cntIns.setCreationTime(sensorTm);
	  	
	  	//gps 시간
	  	String gpiTm = locInfo[0].replaceAll(":", "");
	  	String loc1 = locInfo[1].toString();
	  	locX = Double.valueOf(loc1);
	  	logger.debug("GPS locX1 : " + gpiTm+ " / " +locX);
	  	
	  	String loc2 = locInfo[2].toString();
	  	locY = Double.valueOf(loc2);
	  	logger.debug("GPS locY1 : " + locY);

    	SensorVO vo = new SensorVO();//by ask
       
       /**
        * IoT Data saver (by ask)
        */
       SensorTbl sensorLoc = sensorDao.get(sensorInfo[0]);
 	   //DcuTbl dcu = dcuDao.get(cntIns.getParentID());
 	  
	   // GPS 정보가 없을 경우는, 마지막 통신한 수집센서의 좌표를 설정한다. modify by eunmiae. 2016-02-23      
       //if(CommonUtil.isEmpty(locInfo[1]) || CommonUtil.isEmpty(locInfo[2]) || loc1.equals("0") || loc2.equals("0") ){
       if(locX == null || locY == null || loc1.equals("0") || loc2.equals("0")) {//gps 좌표가 null이거나 0으로 올 경우
			logger.debug("GPS values is null || zero : ["+locX+" / "+locY+"]");
	
	    	if(sensorLoc != null) {
	    		locX =  sensorLoc.getGpiox();
	    		locY =  sensorLoc.getGpioy();
	    	} else {
	    		locX = 0.0;
	    		locY = 0.0;
	    	}
	   }
       
       //location addr add
       //String addr = locDao.getAddr(loc);
       //logger.debug("### addr : [" + loc + "] [" +addr+ "]");
       
       //#1. Sensor Data save
       SensorTbl sensor = new SensorTbl();
       sensor = saver.CiSensorDataSave(sensor, cntIns, /*addr,*/ gpiTm, sensorInfo, periodInfo, sensorTm, statusInfo, threedTm, threedInfo, locX, locY);
	   logger.debug("## ["+seq+"] 1. sensorDataSave end ~~");
       
	   //#2. Location Data save
       LocTbl loc = new LocTbl();
	   loc = saver.CiLocationDataSave(loc, cntIns, /*addr,*/ sensorInfo, locX, locY);
	   logger.debug("## ["+seq+"] 2. locationDataSave end ~~");
	   
	   //#3. Event Data save
	   EventTbl event = new EventTbl();
	   event = saver.CiEventDataSave(event, cntIns, status, sensorInfo, loc, sensor);
	   logger.debug("## ["+seq+"] 3. eventDataSave end ~~");
	   
	return true;
    }

//  <content>
//	[tasInfo]v1.0.0[/tasInfo]
//	[coordi]1.0.0[/coordi]
//  </content>    
    private boolean updateEtcFWver(String dcuId, String[] cnts, String ct) {
		// TODO Auto-generated method stub
    	if(cnts.length != 2) {
			logger.error(cnts.length + " ERROR " );
			return false;
		}
    	DCUVO dcuVO = new DCUVO();
    	dcuVO.setMODIFY_DT(ct);
    	dcuVO.setDCU_ID(dcuId);
    	dcuVO.setTAS_FW_VER(cnts[0]);
    	dcuVO.setCOORDI_FW_VER(cnts[1]);
    	
    	// TODO dcu의 parent 설정할것
    	/*remoteCSEDao.updateEtcFWver(dcuVO); by ask*/
    	return true;

	}
    
    
	// 심박수			
	private boolean insertHeartR(String seq, ContentInstance cntIns, NodeList nodes, NodeList statusNodes){
		int max = 95;
		int min = 50;
		boolean hrmChk = false;
		
		logger.debug("## ["+seq+"] HeartBeat Data Saver is start ~~ ");
		/*
		 * 2017 03 08 차병준
		 * hrm 노드 체크
		 * 노드가 없을때 프로세스 진행시 에러 발생을 하여 추가 하였음.
		 */
		if(nodes.item(0) != null){
			node = nodes.item(0);
			String cnts = node.getTextContent();
			hrmInfo = cnts.split(splitStr);
			hrmChk = true;
		}
		
	  	if(statusNodes.item(0) != null){
	  		statusNode = statusNodes.item(0);
	  		status = statusNode.getTextContent().split(splitStr);
	  	}
	  	String statusInfo = statusNode.getTextContent();

		String hrmTm = hrmInfo[0].replaceAll(":", "");
		Double hrmRate = Double.valueOf(hrmInfo[1]);
		Double hrmMin = Double.valueOf(FMPProperty.getProperty("hrm.min"));
		Double hrmMax = Double.valueOf(FMPProperty.getProperty("hrm.max"));
		
		HeartBeatTbl heartBeat = new HeartBeatTbl();
		EventTbl event = new EventTbl();
		
		//heartBeat event save
		event = saver.CiHeartBeatEventDataSave(cntIns, heartBeat, event, nodes, sensorInfo, hrmRate, hrmMin, hrmMax, hrmTm);
		logger.debug("## ["+seq+"] 1. HeartBeat Event save end ~~ ");
		
		//heartBeat data save
		heartBeat = saver.CiHeartBeatDataSave(heartBeat, cntIns, sensorInfo, hrmRate, hrmMin, hrmMax, hrmTm, statusInfo);
		logger.debug("## ["+seq+"] 2. HeartBeat save end ~~ ");

		/*심박수 중복 제거 차후 수정 (by ask)
		 * HeartBeatVO vo = hbDAO.selectHeartbeat(hbVO);
		if(vo != null){
			logger.error(" 동일 심박수 존재" );
			return false;
		}else{
			if(hrmChk == true){
				hbDAO.insertHeartbeat(hbVO);
			}
		}*/
		return true;
	}



	// yyyymmddhhmmss,rssi			
	private boolean insertRssi(ContentInstance cntIns,NodeList nodes){
		Node node = nodes.item(0);
	  	String cnts = node.getTextContent();
	  	String rssiInfo [] = cnts.split(splitStr);
	  	
		if(rssiInfo.length != 2 || CommonUtil.isEmpty(rssiInfo[0]) || rssiInfo[0].length() != 14) {
			logger.error(" ERROR : Invalid RSSI Info " );
			return false;
		}
		RSSIVO rssiVO = new RSSIVO();
		rssiVO.setYEAR(rssiInfo[0].substring(0, 4));
		rssiVO.setMONTH(rssiInfo[0].substring(4, 6));
		rssiVO.setDAY(rssiInfo[0].substring(6, 8));
		rssiVO.setHHMMSS(rssiInfo[0].substring(8, 14));
		rssiVO.setYYYYMMDDHHMMSS(rssiInfo[0]);
		rssiVO.setDEVICE_NO(cntIns.getCreator()); // 웨어러블 정보
		rssiVO.setRSSI(Double.valueOf(rssiInfo[1]));
		rssiVO.setPARENT_DEVICE_TYPE(DEVICE_TYPE.DCU.getValue());
		rssiVO.setPARENT_DEVICE_ID(cntIns.getParentID()); 
		/*rssiDAO.insertRssi(rssiVO); by ask*/
		return true;
	}

    private Document parsingContent(String content){
    	DocumentBuilderFactory t_dbf = null;
        DocumentBuilder t_db = null;
        Document t_doc = null;
//        NodeList t_nodes = null;
//        Node t_node = null;
//        Element t_element = null;
        InputSource t_is = new InputSource();
        
        try
        {
        	//1. 문서를 읽기위한 공장 	
            t_dbf = DocumentBuilderFactory.newInstance();
            //2. 빌더 생성
            t_db = t_dbf.newDocumentBuilder();
            //2.1 XML 가공    
            t_is = new InputSource();
            t_is.setCharacterStream(new StringReader(content));
            //3.생성된 빌더를 통해서 xml 문서를 Document객체로 파싱해서 가져온다.
            t_doc = t_db.parse(t_is);
        }
        catch (Exception e)
        {
        	logger.error(e);
        } 
        
        return t_doc;
    }
    
    public static void main(String[] args){
    	System.out.println((Math.random() * (100 - 50 + 1)) + 50);
    	Double d = (Math.random() * (100 - 50 + 1)) + 50;
       int ii = d.intValue();
       double dou = (double)ii;
    	String str = "[sensorCnf]20150911221500,wd000001, fw02, hw03, nuritelecom, wd0001[/sensorCnf][gps]0150911221500,10, 20[/gps][rssi]20150911221500,5[/rssi][hrm]20150911221500,100[/hrm]";
    	str.replaceAll("\\[", "<");
    	DocumentBuilderFactory t_dbf = null;
        DocumentBuilder t_db = null;
        Document t_doc = null;
        NodeList t_nodes = null;
        Node t_node = null;
        Element t_element = null;
        InputSource t_is = new InputSource();
        String content ="<root>	<sensorCnf>20150911221500,wd000001, fw02, hw03, nuritelecom, wd0001</sensorCnf>"
        		+ "<gps>0150911221500,10, 20</gps>	<rssi>20150911221500,5</rssi>" +
        		"<hrm>20150911221500,100</hrm></root>";

        try
        {
            t_dbf = DocumentBuilderFactory.newInstance();
            t_db = t_dbf.newDocumentBuilder();
            t_is = new InputSource();
            t_is.setCharacterStream(new StringReader(content));
            t_doc = t_db.parse(t_is);
            t_nodes = t_doc.getElementsByTagName("new");
            Node node = t_nodes.item(0);
        	String cnts = node.getTextContent();
            for (int i = 0, t_len = t_nodes.getLength();
                    i < t_len; i ++)
            {
                t_element = (Element)t_nodes.item(i);
                System.out.println(t_element.getAttribute("value"));
            }
            
            //return t_nodes;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } 
        
       // return t_doc;
    }
    
    /* by ask modified
     * private boolean isOutLoc(double x, double y, String devId){
    	HashMap<String, String> locInfo = custDao.selectScope(devId);  //SCOPE, GPIOX(longitude), GPIOY(latitude)
    	if(locInfo!=null && locInfo.get("SCOPE")!=null){
    		double scope = Double.parseDouble(locInfo.get("SCOPE").toString()); //반경=반지름
    		double gpiox = Double.parseDouble(locInfo.get("ADDR3").toString());
    		double gpioy = Double.parseDouble(locInfo.get("ADDR4").toString());
    	
    		double distance= calDistance( y, x, gpioy, gpiox);
    	
    		if(scope - distance<=0){  //반지름 - 중심에서 현재위치까지의 거리가 음수이면 반경 이상(r-d<=0) 
    			return true;
    		}else{
    			return false;
    		}
    	}else{ //유저가 반경설정안함
    		return false;
    	}
    	return false;
    }
    
    public double calDistance(double lat1, double lon1, double lat2, double lon2){  
        
        double theta, dist;  
        theta = lon1 - lon2;  
        dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))   
              * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));  
        dist = Math.acos(dist);  
        dist = rad2deg(dist);  
          
        dist = dist * 60 * 1.1515;   
        dist = dist * 1.609344;    // 단위 mile 에서 km 변환.  
        dist = dist * 1000.0;      // 단위  km 에서 m 로 변환  
      
        return dist;  
    } 
    
    // 주어진 도(degree) 값을 라디언으로 변환  
    private double deg2rad(double deg){  
    	return (double)(deg * Math.PI / (double)180d);  
    }  
  
    // 주어진 라디언(radian) 값을 도(degree) 값으로 변환  
    private double rad2deg(double rad){  
    	return (double)(rad * (double)180d / Math.PI);  
    }*/
    

	public void increaseReceiveSensingCnt(int rCount) {
		// TODO Auto-generated method stub
		/*dao.increaseReceiveSensingCnt(rCount); by ask*/
	}



	public void increaseSendSensingCnt(int sCount) {
		// TODO Auto-generated method stub
		/*dao.increaseSendSensingCnt(sCount);by ask*/
	}
	
	public ContentInstance insert(String seq, Resource parent, ContentInstance contentInstance) throws RSCException, UnsupportedEncodingException {
		
		String resourceId = CommonUtil.seqIDToResourceID(SEQ_PREFIX.CONTENT_INSTANCE.getValue(), seq);
		String currentTime = CommonUtil.getNowTimestamp();
		
		contentInstance.setParentID(parent.getResourceID());
		contentInstance.setCreator(parent.getResourceID());
		contentInstance.setResourceType(RESOURCE_TYPE.CONTENT_INSTANCE.getValue());
		contentInstance.setResourceID(resourceId);
		contentInstance.setExpirationDate(CommonUtil.timestampToDate(contentInstance.getExpirationTime()));
		contentInstance.setCreationTime(currentTime);
		contentInstance.setLastModifiedTime(currentTime);
		contentInstance.setResourceName(CommonUtil.isEmpty(contentInstance.getResourceName()) ? resourceId : contentInstance.getResourceName());
		contentInstance.setContentSize(new BigInteger(Integer.toString(contentInstance.getContent().getBytes("UTF-8").length)));
		
		//contentInstanceItem.getLabels().addAll(contentInstanceProfile.getLabels());
		//contentInstanceItem.getAnnounceTo().addAll(contentInstanceProfile.getAnnounceTo());
		//contentInstanceItem.getAnnouncedAttribute().addAll(contentInstanceProfile.getAnnouncedAttribute());
		//contentInstanceItem.setContentInfo(contentInstanceProfile.getContentInfo());
		//contentInstanceItem.setOntologyRef(contentInstanceProfile.getOntologyRef());
		//contentInstanceItem.setContent(contentInstanceProfile.getContent());
		//contentInstanceItem.setLinkType(contentInstanceProfile.getLinkType());
		//contentInstanceItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, contentInstanceItem), contentInstanceItem.getResourceName(), RESOURCE_TYPE.CONTENT_INSTANCE.getValue(), contentInstanceItem.getResourceID(), null));
		
		try {
			contentInstanceDao.insert(contentInstance);
			return contentInstance;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("aimir.iot.container.create.fail"));
		}
	}
	
	public ContentInstance findOneContentInstanceByResourceName(String seq, String parentId, String resourceName) throws RSCException {
		ContentInstance contentInstance = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentId));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			contentInstance = (ContentInstance)contentInstanceDao.findOne(query);
		}catch(Exception e) {
			e.printStackTrace();
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("aimir.iot.container.find.fail"));
		}
		
		return contentInstance;
	}
	
	public ContentInstance findOneContentInstanceAtLabelTag(String seq, String parentId, String labelTag) throws RSCException {
		ContentInstance contentInstance = null;
		final int maxRow = 1;
		final String sortColumn = "creationTime";
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentId));
		
		if(labelTag.equals(ContentInstanceAction.LATEST_LABELTAG)) {
			query.with(new Sort(Sort.Direction.DESC, sortColumn)).limit(maxRow);
		} else {
			query.with(new Sort(Sort.Direction.ASC, sortColumn)).limit(maxRow);
		}
		
		try {
			contentInstance = (ContentInstance)contentInstanceDao.findOne(query);
		}catch(Exception e) {
			e.printStackTrace();
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("aimir.iot.container.find.fail"));
		}
		
		return contentInstance;
	}
	
	/**
	 * duplication & rawdata saver
	 * @param data
	 * @return
	 * 						... by ask
	 */
	public boolean rawDataSave(String seq, Resource data){
		
		if (data != null) {
			
			Map<String, String> rawConditionMap = new HashMap<String, String>();
    		rawConditionMap.put("page", "0");
    		rawConditionMap.put("pageSize", "10");
    		rawConditionMap.put("deviceId", data.getParentID());
    		rawConditionMap.put("deviceType", data.getResourceName());
    		rawConditionMap.put("creationTime", data.getCreationTime());
    		rawConditionMap.put("writeDate", DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss"));
			List<MeasurementHistory> rawList = measurementHistoryDao.getGridData(rawConditionMap);
			
			int rawSize =0;
			try {
				if(rawList != null && rawList.size() >= 1) {
					rawSize = rawList.size()+1;
					logger.error("### Duplicate data fields are not allowed ~~~");
					addMeasurementHistory(rawSize, data);
					return false;
				} else {
					rawSize = 1;
					logger.debug("### ["+seq+"] duplication is not ~~~~");
					addMeasurementHistory(rawSize, data);
					
					//MDMS_BE File Log Send
					logger.debug("### ["+seq+"] MdMs Be Log File putServiceIoTData ~~~~");
					processorHandler.putServiceIoTData(ProcessorHandler.SERVICE_B_MDMSData, data);
					return true;
				}
			} catch(Exception ex) {
				ex.getStackTrace();
				logger.error( "[Exception] msg: "+ex.getMessage());
				return false;
			}
		} else {//TimeSync#1 && TimeSync#2 && interrupt
			logger.error("### rawDataSave data is Null!!!");
			return false;
		}
	}
	
	public void addMeasurementHistory(int rawSize, Resource data) {
		String ct = data.getCreationTime();
		
   		try {			
   			MeasurementHistory mh = new MeasurementHistory();
   			mh.setDataCount(rawSize);
	        mh.setDeviceId(data.getParentID());
	        mh.setDeviceType(data.getResourceName());
	             
	        mh.setWriteDate(DateTimeUtil.getCurrentDateTimeByFormat("yyyyMMddHHmmss"));
	        mh.setYyyymmdd(ct.substring(0, 4)+ct.substring(5, 7)+ct.substring(8, 10));
	        mh.setHhmmss(ct.substring(11, 13)+ct.substring(14, 16)+ct.substring(17, 19));
	        
	        if(data != null) {
	        	ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
	        	ObjectOutput out = new ObjectOutputStream(bos);   
	        	out.writeObject(data);
	        	byte[] byteContent = bos.toByteArray();
		        mh.setRawData(byteContent);
	        }
	        measurementHistoryDao.add(mh);
   		}catch(Exception ex){
   			logger.error( "[Exception] addMeasurementHistory error : "+ex.getMessage());
   		}
	}

}
