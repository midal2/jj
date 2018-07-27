package jj.biztrip.batch.krx.model;

import org.springframework.transaction.annotation.Transactional;

public interface IDataGather {

    @Transactional
    void processStockInfo(int i, String strCode);
}
