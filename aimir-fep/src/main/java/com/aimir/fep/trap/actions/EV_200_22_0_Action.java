package com.aimir.fep.trap.actions;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import com.aimir.dao.device.MCUDao;
import com.aimir.fep.trap.common.EV_Action;
import com.aimir.fep.trap.common.ReqConstants;
import com.aimir.fep.util.EventUtil;
import com.aimir.model.device.EventAlertLog;
import com.aimir.model.device.MCU;
import com.aimir.notification.FMPTrap;
import com.aimir.util.TimeUtil;

/**
 * Event ID : 200.22.0 Processing Class
 *
 * @author Y.S Kim
 * @version $Rev: 1 $, $Date: 2005-12-13 15:59:15 +0900 $,
 */
@Component
public class EV_200_22_0_Action implements EV_Action
{
    private static Log log = LogFactory.getLog(EV_200_22_0_Action.class);
    
    @Resource(name="transactionManager")
    JpaTransactionManager txmanager;
    
    @Autowired
    MCUDao mcuDao;
    /**
     * execute event action
     *
     * @param trap - FMP Trap(MCU Event)
     * @param event - Event Data
     */
    public void execute(FMPTrap trap, EventAlertLog event) throws Exception
    {
        log.debug("EventCode[" + trap.getCode()
                +"] MCU["+trap.getMcuId()+"]");

        TransactionStatus txstatus = null;
        
        try {
            txstatus = txmanager.getTransaction(null);
            
            String mcuId = trap.getMcuId();
            MCU mcu = mcuDao.get(mcuId);
            if (mcu == null)
            {
                log.debug("no mcu intance exist mcu["+trap.getMcuId()+"]");
                return;
            }

            String req = event.getEventAttrValue("byteEntry");
            event.remove("byteEntry");
            event.append(EventUtil.makeEventAlertAttr("request",
                    "java.lang.String", 
                    ReqConstants.getTypeString(Integer.parseInt(req))));
            String ip = event.getEventAttrValue("ipEntry");
            event.remove("ipEntry");
            event.append(EventUtil.makeEventAlertAttr("ip",
                    "java.lang.String",ip));
            String filename = event.getEventAttrValue("stringEntry");
            event.remove("stringEntry");
            event.append(EventUtil.makeEventAlertAttr("filename",
                    "java.lang.String",filename));
            mcu.setLastswUpdateDate(TimeUtil.getCurrentTime());
            mcuDao.update(mcu);
        }
        catch (Exception e)
        {
            log.error(e,e);
        }
        finally {
            if (txstatus != null) txmanager.commit(txstatus);
        }
    }
}