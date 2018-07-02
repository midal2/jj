package jj.biztrip.batch.krx;

import jj.biztrip.batch.BatchBase;
import jj.biztrip.comm.BizService;
import jj.biztrip.comm.BizServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class DataGather extends BatchBase{

    @Autowired
    private BizService<Map<String, Object>> bizService;

    @Value("${krx.url}")
    private String strKrxUrl;

    public void batch(){
        Step("증권코드 설정");
        String strCode = "067160"; //아프리카TV

        Step("증권코드 결과가져오기");
        Map<String, Object> resultMap =  bizService.send(strKrxUrl+strCode,"", BizServiceType.XML, "TBL_DailyStock");

        Step("응답결과 확인하기");
        Object obj = resultMap.get("TBL_DailyStock");
        Map mapObj = (Map)obj;
        obj = mapObj.get("DailyStock");
        List<Map> listObj = (List<Map>)obj;

        for(Map selectedMap : listObj){
            logger.debug(selectedMap.toString());
            logger.debug("------------------------------");
        }

    }


    /**
     * 진행상태용
     * @param str
     */
    public void Step(String str){
    }

}
