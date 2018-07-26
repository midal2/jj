package jj.biztrip.batch.krx.model;

import lombok.Data;

import java.util.Date;

@Data
public class TimeConclude {
   private int idx;
   private String StockDt;
   private String StockCd;
   private String time;
   private long negoprice;
   private long Debi;
   private String Dungrak;
   private long Sellprice;
   private long Buyprice;
   private long Amount;
   private Date CreateDt;
   private Date UpdateDt;
}
