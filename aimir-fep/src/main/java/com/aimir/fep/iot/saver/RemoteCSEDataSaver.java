package com.aimir.fep.iot.saver;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import com.aimir.dao.iot.DcuDao;
import com.aimir.fep.iot.domain.resources.DeviceInfo;
import com.aimir.fep.iot.domain.resources.Firmware;
import com.aimir.fep.iot.domain.resources.RemoteCSE;
import com.aimir.fep.iot.utils.CommonCode.DCU_KIND;
import com.aimir.fep.iot.utils.CommonCode.DCU_TYPE;
import com.aimir.fep.iot.utils.CommonCode.DEVICE_TYPE;
import com.aimir.fep.iot.utils.DateUtil;
import com.aimir.model.iot.DcuTbl;

@Service
public class RemoteCSEDataSaver {
	
	private static final Log logger = LogFactory.getLog(RemoteCSEDataSaver.class);

	private String currentTime = DateUtil.getCurrentDateTime();
	
    @Autowired
    DcuDao dcuDao;
	
	@Resource(name="transactionManager")
    JpaTransactionManager txmanager;
    
	
	public void DeviceInfoDataUpdate(String seq, DeviceInfo data) {
		logger.debug("### ["+seq+"] DeviceInfoData Update start ~~ ");
		
		TransactionStatus txstatus = null;
	   	try {	
	   		txstatus = txmanager.getTransaction(null);

	        logger.debug("### ["+seq+"] DeviceInfo data : "+ data);
	        logger.debug("### ["+seq+"] DeviceInfo dcu_id : "+ data.getParentID());
	        
		    //DcuTbl dcu = dcuDao.get(cnts[0]);
	        DcuTbl dcu = new DcuTbl();
	   		if (dcu != null){
	   			//dcu.setDcuId(data.get());
	   			dcu.setTypeCd(DCU_TYPE.OUT_DOOR.getValue());
	   			dcu.setKindCd(DCU_KIND.SUBGIGA.getValue());
	   			//dcu.setDkey(data.getdKey());
	   			dcu.setParentDeviceType(DEVICE_TYPE.SERVER.getValue());
	   			dcu.setParentDeviceId(data.getParentID());
	   			dcu.setCreateDt(data.getCreationTime());
	   			dcu.setLastCommDt(data.getCreationTime());
	   			dcu.setModifyDt(currentTime);
	   			
	   			dcu.setName(data.getDeviceLabel());
	   			dcu.setFwVer(data.getFwVersion());
	   			dcu.setHwVer(data.getHwVersion());
	   			dcu.setVendor(data.getManufacturer());
	   			dcu.setModel(data.getModel());
	   			
	   			dcuDao.update(dcu);
	   			dcuDao.flushAndClear();
	   		} else {
	   			logger.error("["+seq+"] DeviceInfoDataUpdate dcu is null ~~");
	   		}
	   		
	   		logger.debug("### ["+seq+"] DeviceInfoData Update end ~~ ");
			txmanager.commit(txstatus);
		} catch (Exception e) {
			logger.error("DeviceInfoDataUpdate is error :", e);
			if (txstatus != null) txmanager.rollback(txstatus);
        	throw e;
		}
	}
	
	public void FirmwareDataUpdate(String seq, Firmware data) {
		logger.debug("### ["+seq+"] RemoteCSEFWDataSave Save start ~~ ");
		
		TransactionStatus txstatus = null;
	   	try {	
	   		txstatus = txmanager.getTransaction(null);

	        logger.debug("### ["+seq+"] RemoteCSE-FW data : "+ data);
	        logger.debug("### ["+seq+"] RemoteCSE-FW dcu_id : "+ data.getParentID());

		    //DcuTbl dcu = dcuDao.get(cnts[0]);
	        DcuTbl dcu = new DcuTbl();
	   		if (dcu != null){
	   			//dcu.setDcuId(data.get());
	   			dcu.setTypeCd(DCU_TYPE.OUT_DOOR.getValue());
	   			dcu.setKindCd(DCU_KIND.SUBGIGA.getValue());
	   			//dcu.setDkey(data.getDKey());
	   			dcu.setParentDeviceType(DEVICE_TYPE.SERVER.getValue());
	   			dcu.setParentDeviceId(data.getParentID());
	   			dcu.setCreateDt(data.getCreationTime());
	   			dcu.setLastCommDt(data.getCreationTime());
	   			dcu.setModifyDt(currentTime);
	   			
	   			dcu.setFwName(data.getName());
	   			dcu.setFwVer(data.getVersion());
	   			dcu.setFwUrl(data.getURL());
	   			dcu.setFwStatus(data.getUpdateStatus().getStatus().toString());
		   		
	   			dcuDao.update(dcu);
	   			dcuDao.flushAndClear();
	   		} else {
	   			logger.error("["+seq+"] FirmwareDataUpdate dcu is null ~~");
	   		}
	   		
	   		logger.debug("### ["+seq+"] RemoteCSE-FWData Save end ~~ ");
			txmanager.commit(txstatus);
		} catch (Exception e) {
			logger.error("RemoteCSEDataSave is error :", e);
			if (txstatus != null) txmanager.rollback(txstatus);
        	throw e;
		}
	}
	
	public void RemoteCSEDataSave(String seq, RemoteCSE data) {
		logger.debug("### ["+seq+"] RemoteCSEData Save start ~~ ");
		
		TransactionStatus txstatus = null;
	   	try {	
	   		txstatus = txmanager.getTransaction(null);

	        logger.debug("### ["+seq+"] RemoteCSE data : "+ data);
	   		DcuTbl dcu = new DcuTbl();
	   		if (data != null){
	   			dcu.setDcuId(data.getCSEID());
	   			dcu.setTypeCd(DCU_TYPE.OUT_DOOR.getValue());
	   			dcu.setKindCd(DCU_KIND.SUBGIGA.getValue());
	   			dcu.setDkey(data.getDKey());
	   			dcu.setParentDeviceType(DEVICE_TYPE.SERVER.getValue());
	   			dcu.setParentDeviceId(data.getParentID());
	   			dcu.setCreateDt(data.getCreationTime());
	   			dcu.setLastCommDt(data.getCreationTime());
	   			dcu.setModifyDt(currentTime);
		   		
	   			dcuDao.saveOrUpdate(dcu);
	   			dcuDao.flushAndClear();
	   		}
	   		
	   		logger.debug("### ["+seq+"] RemoteCSEData Save end ~~ ");
			txmanager.commit(txstatus);
		} catch (Exception e) {
			logger.error("RemoteCSEDataSave is error :", e);
			if (txstatus != null) txmanager.rollback(txstatus);
        	throw e;
		}
	}
}
