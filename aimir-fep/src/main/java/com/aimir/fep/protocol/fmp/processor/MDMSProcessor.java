package com.aimir.fep.protocol.fmp.processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aimir.fep.iot.domain.resources.Container;
import com.aimir.fep.iot.domain.resources.ContentInstance;
import com.aimir.fep.iot.domain.resources.DeviceInfo;
import com.aimir.fep.iot.domain.resources.Firmware;
import com.aimir.fep.iot.domain.resources.MgmtCmd;
import com.aimir.fep.iot.domain.resources.MgmtResource;
import com.aimir.fep.iot.domain.resources.RemoteCSE;
import com.aimir.fep.iot.domain.resources.Resource;
import com.aimir.fep.iot.service.action.OperationUtilAction;
import com.aimir.fep.iot.service.utils.filter.DatFileFilter;
import com.aimir.fep.iot.utils.CommonCode.MGMT_DEFINITION;
import com.aimir.fep.iot.utils.CommonCode.RESOURCE_TYPE;
import com.aimir.fep.iot.utils.CommonUtil;
import com.aimir.fep.util.FMPProperty;
import com.aimir.util.DateTimeUtil;

/*
 * 2017.10.27
 * 1. 파일 경로는 동적으로 java 또는 properties에 동작할 수 있도록 수정
 * 2. 파일이름을 변경할 수 있도록
 * 3. message queue 전송 주기를 변경
 * 
 */
@Component
public class MDMSProcessor extends Processor {
	private static Log logger = LogFactory.getLog(MDMSProcessor.class);
	
	@Autowired
	OperationUtilAction operationUtilAction;
	
	private final String HEAD_NAME = "IOT_CI_HE";
	private final String EXTENSION = ".DAT";
	private final String T_EXTENSION = ".TMP";
	private final String NEXT = "NEXT";

	private String now = null;
	private String serverNum = null;
	
	//private final String parentPath = "db/iot";
	//private final String parentPath = "D:\\data";
	private final String parentPath = FMPProperty.getProperty("mdms.file.feph.path");
	
	private String seq = null; 
	private static int rowCount = 0;

	private int maxRows = 0;
	private int maxFileSize = 0;
	
	protected DatFileFilter fileFilter = new DatFileFilter();
	
	public MDMSProcessor() throws Exception {
		//this.now = DateTimeUtil.getDateString(new Date());
		this.serverNum = System.getProperty("iot.fep.h.seq", "01");
		this.maxRows = Integer.parseInt(FMPProperty.getProperty("mdms.file.count", "0"));
		this.maxFileSize = Integer.parseInt(FMPProperty.getProperty("mdms.file.size", "0"));
		
		File dir = new File(parentPath);
		if(!dir.exists()) {
			try {
				dir.mkdirs();
			}catch(Exception e) {
				logger.error("Exception Msg : "+e.getMessage());
				logger.error(e, e);
			}
		}
		
		if(rowCount == 0) {
			File file = getFindFile();
			if(!CommonUtil.isEmpty(file)) {
				rowCount = getFileRow(file);
			}
		}
		
		logger.debug("serverNum:"+serverNum+"|maxRows:"+maxRows+"|maxFileSize:"+maxFileSize+"|rowCount:"+rowCount);
	}
	
	@Override
	public int processing(Object obj) throws Exception {
		if(obj instanceof Resource) {	
			Resource resource = (Resource)obj;
			seq = resource.getSeq();
			writeFile(resource);	
		}
		
		return 0;
	}

	@Override
	public void restore() throws Exception {
		
	}
	
	public synchronized void writeFile(Resource resource) throws Exception {
		String objString = getResourceString(resource);
		File file = getFindFile();
		if(CommonUtil.isEmpty(objString)) {
			return;
		}
		
		if(NEXT.equals(objString)) {
			sendQueueTimer(seq, file);
			return;
		}		
		
		if(CommonUtil.isEmpty(file)) {
			String mFileName = getCreateFileName(T_EXTENSION, -1);
			file = new File(parentPath, mFileName);
			logger.debug("["+seq+"] Create MDMS Log File // "+mFileName);
		}
		
		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter(file, true));
			objString = objString.replaceAll(System.getProperty("line.separator"), "");
			objString += System.getProperty("line.separator");
			
			fw.write(objString);
			fw.flush();
			fw.close();
			
			rowCount++;
			long fileMB = (file.length() / 1024) / 1024; 
			
			if((maxRows != 0 && rowCount >= maxRows) || (maxFileSize != 0 && fileMB >= maxFileSize)) {
				sendQueueTimer(seq, file);
			}
			
			logger.debug("["+seq+"] Write MDMS log Data");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getCreateFileName(String ext, int cnt) {
		StringBuilder builder = new StringBuilder();
		now = DateTimeUtil.getDateString(new Date());
				
		builder.append(HEAD_NAME).append(serverNum).append("_").append(now);
		
		if(cnt > -1)
			builder.append("_").append(cnt);
		
		builder.append(ext);
		
		return builder.toString();
	}
	
	public File getFindFile() {
		//RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(resource.getResourceType());
		//타입에 따라 파일명이 바뀐다면 해당 문에 switch~case을 넣어 수정해야 한다.
		
		File file = new File(parentPath);
		if(!file.exists()) {
			logger.info("["+seq+"] Folder not Fount!! // Path:"+file.getAbsolutePath());
			file.mkdir();
		}
		
		File[] files = file.listFiles(fileFilter);
		if(CommonUtil.isEmpty(files)) {
			return null;
		}
		
		return files[0];
	}

	public String getResourceString(Resource resource) throws Exception {
		RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(resource.getResourceType());
		
		switch(resourceType) {
		case UNKNOW:
			return NEXT;
		case CONTAINER:
			return operationUtilAction.getObject2Txt(null, (Container)resource);
		case CONTENT_INSTANCE:
			return operationUtilAction.getObject2Txt(null, (ContentInstance)resource);
		case REMOTE_CSE:
			return operationUtilAction.getObject2Txt(null, (RemoteCSE)resource);
		case MGMT_CMD:
			return operationUtilAction.getObject2Txt(null, (MgmtCmd)resource);
		case MGMT_OBJ:
			return getMgmtString((MgmtResource)resource);
		default:
			return null;
		}
	}
	
	public String getMgmtString(MgmtResource mgmtResource) throws Exception {
		MGMT_DEFINITION mgmtDefinition = MGMT_DEFINITION.getThis(mgmtResource.getMgmtDefinition());
		
		switch(mgmtDefinition) {
		case FIRMWARE:
			return operationUtilAction.getObject2Txt(null, (Firmware)mgmtResource);
		case DEVICE_INFO:
			return operationUtilAction.getObject2Txt(null, (DeviceInfo)mgmtResource);
		default:
			return null;
		}
	}
	
	
	/**
	 * 정해진 시간(2분)에 도달한 Message가 도달할 경우
	 * 파일의 이름을 변경한 후 다시 2분짜리 Message을 전송한다.
	 */
	public void sendQueueTimer(String seq, File file) throws Exception {
		if(CommonUtil.isEmpty(file)) {
			String mFileName = getCreateFileName(EXTENSION, 0);
			new File(parentPath, mFileName).createNewFile();
			
			logger.debug("["+seq+"] data is empty! create empty file["+mFileName+"]");
			return;
		}
		
		/*
		BufferedReader bufferedReader = null;
		int cnt = 0;
		
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String str = bufferedReader.readLine();
			
			while(str != null) {
				cnt++;
				str = bufferedReader.readLine();
			}
		}finally {
			rowCount = 0;
			if(bufferedReader != null){
				bufferedReader.close();
			}
		}
		*/
		
		int cnt = getFileRow(file);
		this.rowCount = 0;
		
		setRename(seq, file, cnt);
	}
	
	public void setRename(String seq, File file, int count) throws Exception {
		//이름변경
		if(CommonUtil.isEmpty(file)) {
			logger.debug("["+seq+"] File not Found!");
			return;
		}
		
		String name = new StringBuilder().append(FilenameUtils.removeExtension(file.getName())).append("_").append(count).append(EXTENSION).toString();
		file.renameTo(new File(parentPath, name));
	}
	
	private int getFileRow(File file) throws Exception {
		if(CommonUtil.isEmpty(file)) {
			return 0;
		}
		
		int count = 0;
		BufferedReader bufferedReader = null;
		
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String str = bufferedReader.readLine();
			
			while(str != null) {
				count ++;
				str = bufferedReader.readLine();
			}
		}catch(Exception e) {
			e.printStackTrace();
			throw e;
		}finally {
			if(bufferedReader != null){
				bufferedReader.close();
			}
		}
		
		return count;
	}
	
	public void init() {
		//서버를 재구동(첫구동)되었을 때
		//해당 폴더의 .TMP 파일을 읽어
		//fileName에 값을 입력시켜주어야 한다.
	}
}
