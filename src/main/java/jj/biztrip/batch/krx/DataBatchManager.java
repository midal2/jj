package jj.biztrip.batch.krx;

import jj.biztrip.batch.krx.model.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class DataBatchManager {

    protected Logger logger;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataGatherDAO dataGatherDAO;

    @Value("${krx.poolSize}")
    private int iPoolSize;

    @Value("${krx.groupSize}")
    private int iGroupSize;

    @Value("${krx.batchInterval}")
    private int iBatchInterval;

    @Autowired
    private Environment env;

    public DataBatchManager(){
        super();
        logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    private ExceptionHandlingScheduledExecutor executor;

    @EventListener
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent.getClass() == ApplicationReadyEvent.class){
            try {

                if (canStartBatch()) {
                    startup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean canStartBatch() {
        boolean result = false;

        String[] profiles = env.getActiveProfiles();
        for(String selecteProfile:profiles){
            if("real".equals(selecteProfile) || "local".equals(selecteProfile)){
                result = true;
                break;
            }
        }

        return  result;
    }

    private void startup() {
        Step("#GET STOCK_LIST FROM DB");
        List<StockInfo> listCode = getStockCodeList();

        Step("#CREATE BATCH_LIST FROM STOCK_LIST");
        List<DataGather> batchList = getBatchList(listCode, iGroupSize);


        Step("#BATCH_START");
        executor = new ExceptionHandlingScheduledExecutor(iPoolSize, (final Thread t, final Throwable e) ->{
                        logger.error("[##MONITOR##][ExceptionHandlingScheduledExecutor]" + e.getMessage());
                        e.printStackTrace();
                        });
        executor.setRejectedExecutionHandler(
                (Runnable r, ThreadPoolExecutor e) ->{
                    DataGather dataGather = (DataGather)r;
                    logger.error("[##MONITOR##]REJECTED_EXECUTION OCCURED[" + dataGather.getThreadNo() + "/" + dataGather.getCodeList() + "]");
                }
        );

        List<ScheduledFuture> scheduledFutures =  new LinkedList<>();
        for(DataGather selectedDataGather:batchList){
            scheduledFutures.add(executor.scheduleWithFixedDelay(selectedDataGather, 1000, iBatchInterval, TimeUnit.MILLISECONDS));
        }

        Step("#BATCH_MONITORING_START");
        MonitorThread monitor = new MonitorThread(executor, scheduledFutures, 3);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();
    }

    private List<DataGather> getBatchList(List<StockInfo> listCode, int iGroupSize) {
        List<DataGather> batchList = new LinkedList<>();

        if (listCode == null || listCode.size() == 0){
            return batchList;
        }

        int iDataGather = 0;
        DataGather dataGather = applicationContext.getBean(DataGather.class);
        for(StockInfo stockInfo:listCode){
            dataGather.addCode(stockInfo.getStockCd());

            if (dataGather.getCodeList().size() % iGroupSize == 0) {
                batchList.add(dataGather);
                ++iDataGather;
                dataGather = applicationContext.getBean(DataGather.class);
                dataGather.setThreadNo(Integer.toString(iDataGather));
            }
        }

        if (dataGather != null){
            batchList.add(dataGather);
        }

        return batchList;
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
        logger.info("Step[" + str + "]");

    }
}

class MonitorThread implements Runnable{
    private ThreadPoolExecutor executor;
    private int seconds;
    private List<ScheduledFuture> scheduledFuture;
    private Boolean run = true;

    public MonitorThread(ThreadPoolExecutor executor, List<ScheduledFuture> scheduledFutures, int seconds) {
        super();
        this.executor = executor;
        this.seconds = seconds;
        this.scheduledFuture = scheduledFutures;
    }

    public void shutDown() {
        this.run = false;
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass().getName());
        while (run) {

            int iCancelTaskCnt = 0;
            int iDoneTaskCnt = 0;
            for (ScheduledFuture scheduledFuture : scheduledFuture) {

                if (scheduledFuture.isCancelled()){
                    ++iCancelTaskCnt;
                }
                if (scheduledFuture.isDone()){
                    ++iDoneTaskCnt;
                }
            }

            logger.info(
                    String.format("[##MONITOR##] [%d/%d] Active: %d, Done: %d, Ccl: %d, Cpl: %d, Task: %d, QueueSize : %d",
                            this.executor.getPoolSize(),
                            this.executor.getCorePoolSize(),
                            this.executor.getActiveCount(),
                            iDoneTaskCnt,
                            iCancelTaskCnt,
                            this.executor.getCompletedTaskCount(),
                            this.executor.getTaskCount(),
                            this.executor.getQueue().size()
                            ));
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
