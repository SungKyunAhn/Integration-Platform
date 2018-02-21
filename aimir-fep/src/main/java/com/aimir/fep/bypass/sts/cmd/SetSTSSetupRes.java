package com.aimir.fep.bypass.sts.cmd;

import com.aimir.fep.bypass.sts.STSDataFrame;
import com.aimir.fep.bypass.sts.STSException;
import com.aimir.fep.util.DataUtil;

public class SetSTSSetupRes extends STSDataFrame {
    
    public SetSTSSetupRes(byte[] bx) throws Exception {
        super.decode(bx);
    }

    public int getResult() throws Exception {
    	if (res.getResult() == 0x00)
            return DataUtil.getIntToBytes(res.getRdata());
        else
            throw new STSException(res.getRdata());
    }
}
