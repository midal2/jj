package jj.biztrip.batch.krx;

import jj.biztrip.batch.BatchBase;
import jj.biztrip.comm.BizService;
import jj.biztrip.comm.BizServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class DataGather extends BatchBase{

    @Autowired
    private BizService<Map<String, Object>> bizService;

    @Value("${krx.url}")
    private String strKrxUrl;

    @Autowired
    private DataGatherDAO dataGatherDAO;

    public void batch(){
        Step("증권코드 설정");
        String strCode = "067160"; //아프리카TV

        Step("증권코드 결과가져오기");
        Map<String, Object> resultMap =  bizService.send(strKrxUrl+strCode,"", BizServiceType.XML, "");

        Step("TimeConclude(시간별 거래내역) 항목 가져오기");
        List<Map> mapTimeConclude = getTimeConclude(resultMap, strCode);
        for(Map<String, Object> selectedMap : mapTimeConclude){
            dataGatherDAO.insertTimeConclude(selectedMap);
        }

    }

    /**
     * TimeConclude(시간별 거래내역) 항목 가져오기
     * @param resultMap
     * @param strCode
     * @return
     */
    private List<Map> getTimeConclude(Map<String, Object> resultMap, String strCode) {
        List<Map<String,Object>> list = new LinkedList<>();

        Object obj = resultMap.get("TBL_TimeConclude");
        Map mapObj = (Map)obj;
        obj = mapObj.get("TBL_TimeConclude");
        List<Map> listObj = (List<Map>)obj;

        for(Map selectedMap:listObj){
            selectedMap.put("item_cd", strCode);
            selectedMap.put("negoprice", selectedMap.get("negoprice").toString().replaceAll(",",""));
            selectedMap.put("sellprice", selectedMap.get("sellprice").toString().replaceAll(",",""));
            selectedMap.put("buyprice", selectedMap.get("buyprice").toString().replaceAll(",",""));
            selectedMap.put("dungrak", selectedMap.get("Dungrak"));
            selectedMap.put("debi", selectedMap.get("Debi"));
        }

        return listObj;
    }


    /**
     * 진행상태용
     * @param str
     */
    public void Step(String str){
        logger.debug("STEP[" + str + "]");
    }

}
