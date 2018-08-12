package jj.biztrip.batch.krx;

import com.zaxxer.hikari.HikariDataSource;
import jj.biztrip.batch.BatchBase;
import jj.biztrip.batch.krx.model.DailyStock;
import jj.biztrip.batch.krx.model.IDataGather;
import jj.biztrip.batch.krx.model.StockInfo;
import jj.biztrip.batch.krx.model.TimeConclude;
import jj.biztrip.comm.BizService;
import jj.biztrip.comm.BizServiceType;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static jj.biztrip.comm.BizUtil.cLong;

@Component
@Scope("prototype")
@Data
public class DataGather extends BatchBase implements IDataGather {

    @Autowired
    private BizService<Map<String, Object>> bizService;

    @Value("${krx.url}")
    private String strKrxUrl;

    private String threadNo;

    private String strStepMsg;

    private List<String> codeList; //종목코드

    private SimpleDateFormat sd;

    @Autowired
    private DataGatherDAO dataGatherDAO;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    HikariDataSource dataSource;

    public DataGather(){
        super();
        codeList = new LinkedList<>();
        sd = new SimpleDateFormat("HH:mm:ss");
    }

    public void addCode(String strCode){
        codeList.add(strCode);
    }

    @Override
    public void run() {
        Date startDt = new Date();

        try {
            int i = 0;
            TransactionStatus transactionStatus = null;
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
            for (String strCode : codeList) {
                /*
                @Transaction 어노테이션을 사용안함
                - 사용할 경우 초기 SpringContainer 생성시 AOP로 인한 bean 생성속도가 느림.
                - 배치를 위한 Bean이 1000개 이상생성되므로 초기 SpringContainer 구성시 Transaction AOP가 부적합함ㅣ
                - AOP 적용으로 인해 bean속도가 느려서 수동으로 구성.

                ##@Transaction 구성시 AOP로 인한 추가 생성된 로직에서 오류처리시
                UncaughtException의 발생이 가능하므로 오류보완처리에 대한 전략필요
                */
                try {
                    transactionStatus = transactionManager.getTransaction(transactionDefinition);

                    processStockInfo(++i, strCode);

                    transactionManager.commit(transactionStatus);
                } catch (Exception e) {
                    logger.error("처리중오류 발생[" + strCode + "][" + strStepMsg + "][" + e.getMessage() + "]");
                    if (transactionStatus != null) {
                        transactionManager.rollback(transactionStatus);
                    }
                }
            }
        }catch (Throwable e) {
            logger.error("[THREAD_NO][" + threadNo + "]" + getCodeList() + " 중 오류발생[" + e.getMessage() + "]");
        }

        Date endDt = new Date();

        logger.info(
                String.format("[THREAD_NO][%s]%s stTime: %s, edTime: %s, runTime %d second",
                        threadNo,
                        getCodeList(),
                        sd.format(startDt),
                        sd.format(endDt),
                        ((endDt.getTime() - startDt.getTime()) / 1000)
                )
        );
    }

    @Override
    public void processStockInfo(int i, String strCode) {
        if (strCode == null || "".equals(strCode)){
            logger.error("증권코드가 없음");
            return;
        }

        SimpleDateFormat sd = new SimpleDateFormat("HH:mm:ss");

        Step("증권코드 결과가져오기");
        Map<String, Object> resultMap =  bizService.send(strKrxUrl+strCode,"", BizServiceType.XML, "");

        Step("StockInfo(주가정보) 기록하기");
        StockInfo stockInfo = getStockInfo(resultMap, strCode);
        int infoCnt;
        infoCnt = dataGatherDAO.updateTBL_StockInfo(stockInfo);

        Step("TimeConclude(시간별 체결가) 기록하기");
        List<TimeConclude> mapTimeConclude = getTimeConclude(resultMap, strCode);
        int timeCnt = 0;
        for(TimeConclude selectedItem : mapTimeConclude){
            timeCnt = timeCnt + dataGatherDAO.insertTBL_TimeConclude(selectedItem);
        }

        Step("DailyStock(일자별 체결가) 기록하기");
        int dayCnt = 0;
        List<DailyStock> listDailyStock = getDailyStock(resultMap, strCode);
        for(DailyStock selectedItem : listDailyStock){
            dayCnt = dayCnt + dataGatherDAO.insertTBL_DailyStock(selectedItem);
        }

        logger.info(
                String.format("    [STOCK_INFO] %s - %s[%d], InfoDbCnt: %d, TimeConcludeDbCnt: %d, DailyDbCnt: %d]",
                    strCode, threadNo, codeList.size(),
                    infoCnt,
                    timeCnt,
                    dayCnt)
        );
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
            timeConclude.setSellprice(cLong(selectedMap.get("sellprice")));
            timeConclude.setBuyprice(cLong(selectedMap.get("buyprice")));
            timeConclude.setAmount(cLong(selectedMap.get("amount")));

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
                timeConclude.setSellprice(cLong(selectedMap.get("sellprice")));
                timeConclude.setBuyprice(cLong(selectedMap.get("buyprice")));
                timeConclude.setAmount(cLong(selectedMap.get("amount")));

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
