package jj.biztrip.batch.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ScheduleTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Scheduled(fixedDelay = 2000)
    public void testSchedule(){
        logger.debug("testSchedule : " + new Date());
    }

}
