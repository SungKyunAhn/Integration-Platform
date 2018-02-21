package com.aimir.fep;

import org.junit.Test;

import com.aimir.fep.util.DataUtil;
import com.aimir.fep.util.FrameUtil;

public class MultiFrameEncodeTest {
	@Test
	public void mTest(){
		String str = "5E00004E050000430A0000006D02020681003800010B0006006661696C5231010300010000010B000A0033302038202A202A202A010B0037006D65746572696E67206F7074696F6E3D302C206F66667365743D33322C20636F756E743D32352C2066696C7465722D6D74723D6661696C010B0006006661696C5232010300010000010B000B003330203132202A202A202A010B0037006D65746572696E67206F7074696F6E3D302C206F66667365743D33362C20636F756E743D32352C2066696C7465722D6D74723D6661696C010B0006006661696C5233010300010000010B000B003330203134202A202A202A010B0037006D65746572696E67206F7074696F6E3D302C206F66667365743D33382C20636F756E743D32352C2066696C7465722D6D74723D6661696C010B0002006D31010300010000010B000B003220302D35202A202A202A010B0055006D65746572696E67206F7074696F6E3D302C206F66667365743D32392C20636F756E743D33302C20737563632D6E6578743D6D3172312C2064656C61792D706F7765723D33302C2062616E2D73746172743D333630010B0004006D317231010300010000010B000900782078207820782078010B0047006D65746572696E67206F7074696F6E3D302C206F66667365743D32392C20636F756E743D33302C2066696C7465722D6D74723D6661696C2C20737563632D6E6578743D6D317232010B0004006D317232010300010000010B000900782078207820782078010B0047006D65746572696E67206F7074696F6E3D302C206F66667365743D32392C20636F756E743D33302C2066696C7465722D6D74723D6661696C2C20737563632D6E6578743D6D317233010B0004006D317233010300010000010B000900782078207820782078010B0037006D65746572696E67206F7074696F6E3D302C206F66667365743D32392C20636F756E743D33302C2066696C7465722D6D74723D6661696C010B0002006D32010300010000010B000D00322032302D3232202A202A202A010B0055006D65746572696E67206F7074696F6E3D302C206F66667365743D34362C20636F756E743D32372C20737563632D6E6578743D6D3272312C2064656C61792D706F7765723D33302C2062616E2D73746172743D333630010B0004006D327231010300010000010B000900782078207820782078010B0047006D65746572696E67206F7074696F6E3D302C206F66667365743D34362C20636F756E743D32372C2066696C7465722D6D74723D6661696C2C20737563632D6E6578743D6D327232010B0004006D327232010300010000010B000900782078207820782078010B0047006D65746572696E67206F7074696F6E3D302C206F66667365743D34362C20636F756E743D32372C2066696C7465722D6D74723D6661696C2C20737563632D6E6578743D6D327233010B0004006D327233010300010000010B000900782078207820782078010B0037006D65746572696E67206F7074696F6E3D302C206F66667365743D34362C20636F756E743D32372C2066696C7465722D6D74723D6661696C010B00070072656672657368010300010000010B002700343520312C332C352C372C392C31312C31332C31352C31372C31392C32312C3233202A202A202A010B00100072656672657368207461726765743D32010B00070075706772616465010300010000010B001200333020302C362C31362C3232202A202A202A010B003A00757067726164652075726C3D687474703A2F2F757067726164652E6E75726974656C65636F6D2E636F6D2C2062616E2D73746172743D31343330010B00060075706C6F6164010300010000010B000A003535202A202A202A202A010B00060075706C6F6164";
		//String str = "5E00000C000000430A0000006D02010181000000";
		
		FrameUtil.makeMultiEncodedFrame(DataUtil.readByteString(str), null);
	}
}