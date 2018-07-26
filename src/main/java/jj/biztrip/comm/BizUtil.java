package jj.biztrip.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BizUtil {

    public static Logger logger;

    static{
        logger = LoggerFactory.getLogger(BizUtil.class);
    }

    public static long cLong(Object obj){
        long result = 0;

        if (obj == null){
            return result;
        }

        String readStr = obj.toString();
        readStr = readStr.replaceAll(",","");

        try {
            result = Long.parseLong(readStr);
        }catch (Exception e){
            logger.warn("cLong error:" + e.getMessage());
        }

        return result;
    }
}
