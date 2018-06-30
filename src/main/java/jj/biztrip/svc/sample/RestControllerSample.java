package jj.biztrip.svc.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("test/")
public class RestControllerSample {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @RequestMapping("/")
    public String index() {
        logger.debug("로그기록!!!!!");
        return "테스트 스프링 실행444";
    }
}