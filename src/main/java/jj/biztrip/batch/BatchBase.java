package jj.biztrip.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BatchBase implements Runnable{
    protected Logger logger;

    public BatchBase(){
        logger = LoggerFactory.getLogger(this.getClass().getName());
    }
}
