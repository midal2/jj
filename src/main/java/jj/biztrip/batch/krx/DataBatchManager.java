package jj.biztrip.batch.krx;

import jj.biztrip.batch.krx.model.StockInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DataBatchManager {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataGatherDAO dataGatherDAO;

    @Value("${krx.poolSize}")
    private int iPoolSize;

    @EventListener
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent.getClass() == ApplicationReadyEvent.class){
            try {
                startup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startup() {
        Step("주식정보대상 종목코드를 가져온다");
        List<StockInfo> listCode = getStockCodeList();

        Step("종목코드별로 스케쥴내역을 등록한다");
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(iPoolSize);
        for(StockInfo stockInfo:listCode){
            DataGather dataGather = applicationContext.getBean(DataGather.class);
            dataGather.setStrCode(stockInfo.getStockCd());
            executor.scheduleWithFixedDelay(dataGather, 1000, 15000, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 주식정보대상 종목코드를 가져온다
     * @return
     */
    private List<StockInfo> getStockCodeList() {
        List<StockInfo> resultList = dataGatherDAO.selectStockCdList();
        return resultList;
    }

    private void Step(String str){

    }
}
