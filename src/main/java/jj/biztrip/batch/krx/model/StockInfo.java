package jj.biztrip.batch.krx.model;

import lombok.Data;

@Data
public class StockInfo {
    private long idx;
    private String StockCd      ;
    private String JongName     ;
    private long CurJuka      ;
    private long Debi         ;
    private String DungRak      ;
    private long PrevJuka     ;
    private long Volume       ;
    private long Money        ;
    private long StartJuka    ;
    private long HighJuka     ;
    private long LowJuka      ;
    private long High52       ;
    private long Low52        ;
    private long UpJuka       ;
    private long DownJuka     ;
    private String Per        ;
    private long Amount       ;
    private long FaceJuka     ;
    private String CreateDt   ;
    private String UpdateDt   ;
}
