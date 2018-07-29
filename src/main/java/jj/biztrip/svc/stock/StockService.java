package jj.biztrip.svc.stock;

import jj.biztrip.batch.krx.model.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("stock/")
public class StockService {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired
    private StockDAO stockDAO;

    @RequestMapping("/getAllInfo")
    public List<StockInfo> getAllInfo() {
        List<StockInfo> stockInfos = new LinkedList<>();

        stockInfos = stockDAO.getAllStockInfo();

        return stockInfos;
    }
}
