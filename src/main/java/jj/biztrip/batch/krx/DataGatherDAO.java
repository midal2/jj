package jj.biztrip.batch.krx;

import jj.biztrip.batch.krx.model.DailyStock;
import jj.biztrip.batch.krx.model.StockInfo;
import jj.biztrip.batch.krx.model.TimeConclude;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface DataGatherDAO {
    int insertTBL_TimeConclude(TimeConclude timeConclude);
    int updateTBL_StockInfo(StockInfo stockInfo);
    int insertTBL_DailyStock(DailyStock dailyStock);
    List<StockInfo> selectStockCdList();
}
