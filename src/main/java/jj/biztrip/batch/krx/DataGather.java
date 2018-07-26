package jj.biztrip.batch.krx;

import jj.biztrip.batch.BatchBase;
import jj.biztrip.batch.krx.model.DailyStock;
import jj.biztrip.batch.krx.model.StockInfo;
import jj.biztrip.batch.krx.model.TimeConclude;
import jj.biztrip.comm.BizService;
import jj.biztrip.comm.BizServiceType;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static jj.biztrip.comm.BizUtil.cLong;

@Component
@Scope("prototype")
@Data
public class DataGather extends BatchBase{

    @Autowired
    private BizService<Map<String, Object>> bizService;

    @Value("${krx.url}")
    private String strKrxUrl;

    private String threadNo;

    private String strStepMsg;

    private List<String> codeList; //종목코드

    @Autowired
    private DataGatherDAO dataGatherDAO;

    public DataGather(){
        super();
        codeList = new LinkedList<>();
    }

    public void addCode(String strCode){
        codeList.add(strCode);
    }

    @Override
    public void run() {

        int i=0;
        for(String strCode:codeList){
            ++i;
            try {
                processStockInfo(i, strCode);
            }catch (Exception e){
                logger.error("처리중오류 발생[" + strCode + "][" + strStepMsg + "][" + e.getMessage() + "]");
            }
        }
    }

    private void processStockInfo(int i, String strCode) {
        if (strCode == null || "".equals(strCode)){
            logger.error("증권코드가 없음");
            return;
        }

        SimpleDateFormat sd = new SimpleDateFormat("HH:mm:ss");

        Step("증권코드 결과가져오기");
        Map<String, Object> resultMap =  bizService.send(strKrxUrl+strCode,"", BizServiceType.XML, "");

        Step("StockInfo(주가정보) 기록하기");
        StockInfo stockInfo = getStockInfo(resultMap, strCode);
        int infoCnt = 0;
        infoCnt = dataGatherDAO.updateTBL_StockInfo(stockInfo);
        Date dtInfo = new Date();

        Step("TimeConclude(시간별 체결가) 기록하기");
        List<TimeConclude> mapTimeConclude = getTimeConclude(resultMap, strCode);
        int timeCnt = 0;
        for(TimeConclude selectedItem : mapTimeConclude){
            timeCnt = timeCnt + dataGatherDAO.insertTBL_TimeConclude(selectedItem);
        }
        Date dtTime = new Date();

        Step("DailyStock(일자별 체결가) 기록하기");
        int dayCnt = 0;
        List<DailyStock> listDailyStock = getDailyStock(resultMap, strCode);
        for(DailyStock selectedItem : listDailyStock){
            dayCnt = dayCnt + dataGatherDAO.insertTBL_DailyStock(selectedItem);
        }
        Date dtDay = new Date();

        logger.info("THREAD_NO[" + threadNo +  "][" + i + "/" + codeList.size() + "]" + "종목코드[" + strCode + "]["
                + infoCnt + "-" + sd.format(dtInfo) + "/"
                + timeCnt + "-" + sd.format(dtTime) + "/"
                + dayCnt + "-" + sd.format(dtDay)
                + "]Process");
    }

    /**
     * 일자별 체결가 기록하기
     * @param resultMap
     * @param strCode
     * @return
     */
    private List<DailyStock> getDailyStock(Map<String,Object> resultMap, String strCode) {
        List<DailyStock> list = new LinkedList<>();

        Object obj = resultMap.get("TBL_DailyStock");
        Map mapObj = (Map)obj;
        obj = mapObj.get("DailyStock");

        if (obj instanceof  Map){
            Map selectedMap = (Map)obj;
            DailyStock dailyStock = new DailyStock();

            dailyStock.setStockCd(strCode);
            dailyStock.setDay_Date(selectedMap.get("day_Date").toString());
            dailyStock.setDay_EndPrice(cLong(selectedMap.get("day_EndPrice")));
            dailyStock.setDay_Debi(cLong(selectedMap.get("day_Debi")));
            dailyStock.setDay_Dungrak(selectedMap.get("day_Dungrak").toString());
            dailyStock.setDay_Start(cLong(selectedMap.get("day_Start")));
            dailyStock.setDay_High(cLong(selectedMap.get("day_High")));
            dailyStock.setDay_Low(cLong(selectedMap.get("day_Low")));
            dailyStock.setDay_Volume(cLong(selectedMap.get("day_Volume")));
            dailyStock.setDay_getAmount(cLong(selectedMap.get("day_getAmount")));

            list.add(dailyStock);
        }else {
            List<Map> listObj = (List<Map>) obj;
            for (Map selectedMap : listObj) {
                DailyStock dailyStock = new DailyStock();

                dailyStock.setStockCd(strCode);
                dailyStock.setDay_Date(selectedMap.get("day_Date").toString());
                dailyStock.setDay_EndPrice(cLong(selectedMap.get("day_EndPrice")));
                dailyStock.setDay_Debi(cLong(selectedMap.get("day_Debi")));
                dailyStock.setDay_Dungrak(selectedMap.get("day_Dungrak").toString());
                dailyStock.setDay_Start(cLong(selectedMap.get("day_Start")));
                dailyStock.setDay_High(cLong(selectedMap.get("day_High")));
                dailyStock.setDay_Low(cLong(selectedMap.get("day_Low")));
                dailyStock.setDay_Volume(cLong(selectedMap.get("day_Volume")));
                dailyStock.setDay_getAmount(cLong(selectedMap.get("day_getAmount")));

                list.add(dailyStock);
            }
        }

        return list;

    }

    /**
     * 주가정보를 가져오기
     * @param resultMap
     * @param strCode
     * @return
     */
    private StockInfo getStockInfo(Map<String,Object> resultMap, String strCode) {
        StockInfo stockInfo = new StockInfo();

        Object obj = resultMap.get("TBL_StockInfo");
        Map mapObj = (Map)obj;

        stockInfo.setStockCd(strCode);
        stockInfo.setJongName(mapObj.get("JongName").toString());
        stockInfo.setCurJuka(cLong(mapObj.get("CurJuka")));
        stockInfo.setDebi(cLong(mapObj.get("Debi")));
        stockInfo.setDungRak(mapObj.get("DungRak").toString());
        stockInfo.setPrevJuka(cLong(mapObj.get("PrevJuka")));
        stockInfo.setVolume(cLong(mapObj.get("Volume")));
        stockInfo.setMoney(cLong(mapObj.get("Money")));
        stockInfo.setStartJuka(cLong(mapObj.get("StartJuka")));
        stockInfo.setHighJuka(cLong(mapObj.get("HighJuka")));
        stockInfo.setLowJuka(cLong(mapObj.get("LowJuka")));
        stockInfo.setHigh52(cLong(mapObj.get("High52")));
        stockInfo.setLow52(cLong(mapObj.get("Low52")));
        stockInfo.setUpJuka(cLong(mapObj.get("UpJuka")));
        stockInfo.setDownJuka(cLong(mapObj.get("DownJuka")));
        stockInfo.setPer(mapObj.get("Per").toString());
        stockInfo.setAmount(cLong(mapObj.get("Amount")));
        stockInfo.setFaceJuka(cLong(mapObj.get("FaceJuka")));

        return stockInfo;
    }

    /**
     * TimeConclude(시간별 거래내역) 항목 가져오기
     * @param resultMap
     * @param strCode
     * @return
     */
    private List<TimeConclude> getTimeConclude(Map<String, Object> resultMap, String strCode) {
        List<TimeConclude> list = new LinkedList<>();

        SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
        String nowDt = sd.format(new Date());

        Object obj = resultMap.get("TBL_TimeConclude");
        Map mapObj = (Map)obj;
        obj = mapObj.get("TBL_TimeConclude");
        if (obj instanceof  Map){
            Map selectedMap = (Map)obj;
            TimeConclude timeConclude = new TimeConclude();

            timeConclude.setStockCd(strCode);
            timeConclude.setStockDt(nowDt);
            timeConclude.setTime(selectedMap.get("time").toString());
            timeConclude.setNegoprice(cLong(selectedMap.get("negoprice").toString()));
            timeConclude.setDebi(cLong(selectedMap.get("Debi")));
            timeConclude.setDungrak(selectedMap.get("Dungrak").toString());
            timeConclude.setSellprice(cLong(selectedMap.get("Sellprice")));
            timeConclude.setBuyprice(cLong(selectedMap.get("Buyprice")));
            timeConclude.setAmount(cLong(selectedMap.get("Amount")));

            list.add(timeConclude);
        }else {
            List<Map> listObj = (List<Map>) obj;
            for (Map selectedMap : listObj) {
                TimeConclude timeConclude = new TimeConclude();

                timeConclude.setStockCd(strCode);
                timeConclude.setStockDt(nowDt);
                timeConclude.setTime(selectedMap.get("time").toString());
                timeConclude.setNegoprice(cLong(selectedMap.get("negoprice").toString()));
                timeConclude.setDebi(cLong(selectedMap.get("Debi")));
                timeConclude.setDungrak(selectedMap.get("Dungrak").toString());
                timeConclude.setSellprice(cLong(selectedMap.get("Sellprice")));
                timeConclude.setBuyprice(cLong(selectedMap.get("Buyprice")));
                timeConclude.setAmount(cLong(selectedMap.get("Amount")));

                list.add(timeConclude);
            }
        }

        return list;
    }


    /**
     * 진행상태용
     * @param str
     */
    public void Step(String str){
        strStepMsg = str;
        logger.debug("STEP[" + str + "]");
    }

}
