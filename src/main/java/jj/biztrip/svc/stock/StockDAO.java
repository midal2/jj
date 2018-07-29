package jj.biztrip.svc.stock;

import jj.biztrip.batch.krx.model.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StockDAO {
    /**
     * 전체 주식정보를 가져온다
     * @return
     */
    List<StockInfo> getAllStockInfo();
}
