package jj.biztrip.batch.krx;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface DataGatherDAO {
    int insertTimeConclude(Map map);
}
