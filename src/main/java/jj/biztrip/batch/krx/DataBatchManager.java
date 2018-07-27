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
import org.springframework.stereotype.Component;

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

    public DataBatchManager(){
        super();
        logger = LoggerFactory.getLogger(this.getClass().getName());
    }

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
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(iPoolSize);
        Step("주식정보대상 종목코드를 가져온다. " +
                "PoolSize[" + executor.getPoolSize() + "]/ " +
                "CorePoolSize[" + executor.getCorePoolSize() + "/" +
                "MaximumPoolSize[" + executor.getMaximumPoolSize() + "/" +
                "]");
        List<StockInfo> listCode = getStockCodeList();

        Step("종목코드별로 스케쥴내역을 등록한다");
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler(){
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                DataGather dataGather = (DataGather)r;
                logger.error("rejectedExecution Occured!!![" + dataGather.getThreadNo() + "/" + dataGather.getCodeList() + "]");
            }
        });

        MonitorThread monitor = new MonitorThread(executor, 3);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        DataGather dataGather = null;
        int iDataGather = 0;
        for(StockInfo stockInfo:listCode){
            if (dataGather == null){
                dataGather = applicationContext.getBean(DataGather.class);
                ++iDataGather;
                dataGather.setThreadNo(Integer.toString(iDataGather));
            }

            dataGather.addCode(stockInfo.getStockCd());

            if (dataGather.getCodeList().size() % iGroupSize == 0){
                executor.scheduleWithFixedDelay(dataGather, 1000, iBatchInterval, TimeUnit.MILLISECONDS);
                dataGather = null;
            }else{
                continue;
            }
        }

        if (dataGather != null){
            executor.scheduleWithFixedDelay(dataGather, 1000, iBatchInterval, TimeUnit.MILLISECONDS);
        }

        logger.info("[TOTAL_DATA_GATHER_CNT][" + iDataGather + "]");


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
    private Boolean run = true;
    public MonitorThread(ThreadPoolExecutor executor, int seconds) {
        super();
        this.executor = executor;
        this.seconds = seconds;
    }

    public void shutDown() {
        this.run = false;
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass().getName());
        while (run) {
            logger.info(
                    String.format("[##MONITOR##] [%d/%d] Active: %d, Completed: %d, Task: %d, QueueSize : %d, isShutdown: %s, isTerminated: %s",
                            this.executor.getPoolSize(),
                            this.executor.getCorePoolSize(),
                            this.executor.getActiveCount(),
                            this.executor.getCompletedTaskCount(),
                            this.executor.getTaskCount(),
                            this.executor.getQueue().size(),
                            this.executor.isShutdown(),
                            this.executor.isTerminated()));
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
