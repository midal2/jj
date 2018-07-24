package jj.biztrip.batch.krx.model;

import lombok.Data;

import java.util.Date;

@Data
public class DailyStock {
    private int idx              ;
    private String StockCd          ;
    private String day_Date         ;
    private long day_EndPrice     ;
    private long day_Debi         ;
    private String day_Dungrak      ;
    private long day_Start        ;
    private long day_High         ;
    private long day_Low          ;
    private long day_Volume       ;
    private long day_getAmount    ;
    private Date CreateDt         ;
    private Date UpdateDt         ;
}
