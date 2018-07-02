package jj.biztrip.comm;

public interface BizService<T> {
    T send(String url, String sendStr);
    T send(String url, String sendStr, BizServiceType type, String etcInfo);
}
