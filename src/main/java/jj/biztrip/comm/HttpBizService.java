package jj.biztrip.comm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jj.biztrip.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Component
public class HttpBizService implements BizService<Map<String, Object>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public Map<String, Object> send(String url, String sendStr) {
        return send(url, sendStr, BizServiceType.JSON, "");
    }

    /**
     * 유형별 응답메세지를 생성한다
     * @param temp
     * @param type
     * @return
     * @throws IOException
     */
    private Map<String,Object> getResultMap(String temp, BizServiceType type, String etcInfo) throws IOException {
        Map<String,Object> resultMap = new HashMap();

        switch(type) {
            case JSON:
                ObjectMapper om = new ObjectMapper();
                resultMap = om.readValue(temp, new TypeReference<Map<String, Object>>(){});
                break;
            case XML:
                XmlMapper xmlMapper = new XmlMapper();
                xmlMapper.setConfig(xmlMapper.getSerializationConfig().withRootName(etcInfo));
                String strXml = temp.replace("<?xml version=\"1.0\" encoding=\"utf-8\" ?>", "");
                resultMap = (Map<String,Object>)xmlMapper.readValue(strXml, GenericObject.class);
                break;
            default:
                break;
        }

        return resultMap;
    }

    @Override
    public Map<String, Object> send(String url, String sendStr, BizServiceType type, String etcInfo) {
        Map<String, Object> resultMap = new HashMap();

        try {
            logger.info("Trying Connect... URL[" + url + "][DATA][" + sendStr+ "]");

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());

            logger.info("SEND[" + url + "?" + sendStr+ "]");
            os.write(sendStr);
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            DataInputStream in;
            if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST){
                in = new DataInputStream(conn.getInputStream());
            }else{
                in = new DataInputStream(conn.getErrorStream());
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            byte[] buf = new byte[2048];

            while (true) {
                int n = in.read(buf);
                if (n == -1) break;
                bout.write(buf, 0, n);
            }
            bout.flush();
            bout.close();

            byte[] resMessage = bout.toByteArray();
            conn.disconnect();

            String temp = new String(resMessage, "utf-8");

            if (responseCode == HttpURLConnection.HTTP_OK){
                logger.info("RECEIVE[" + temp +"]");
                ObjectMapper om = new ObjectMapper();
                resultMap = getResultMap(temp, type, etcInfo);
            }

            resultMap.put("RESPONSE_CODE", Integer.toString(responseCode));
            resultMap.put("RESPONSE_BODY", temp.trim());

        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException();
        }

        return resultMap;
    }
    @JsonDeserialize(using = CustomDeserializer.class)
    public class GenericObject
    {

    }

    public static class CustomDeserializer extends UntypedObjectDeserializer
    {
        private static final long serialVersionUID = -4628994110702279382L;

        protected Object mapObject(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.START_OBJECT)
            {
                t = jp.nextToken();
            }
            // minor optimization; let's handle 1 and 2 entry cases separately
            if (t == JsonToken.END_OBJECT)
            { // and empty one too
                // empty map might work; but caller may want to modify... so better just give small modifiable
                return new LinkedHashMap<String, Object>(2);
            }
            String field1 = jp.getCurrentName();
            jp.nextToken();
            Object value1 = deserialize(jp, ctxt);
            if (jp.nextToken() == JsonToken.END_OBJECT)
            { // single entry; but we want modifiable
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
                value1 = handleMaultipleValue(result, field1, value1);
                result.put(field1, value1);
                return result;
            }
            String field2 = jp.getCurrentName();
            jp.nextToken();
            Object value2 = deserialize(jp, ctxt);
            if (jp.nextToken() == JsonToken.END_OBJECT)
            {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
                value1 = handleMaultipleValue(result, field1, value1);
                result.put(field1, value1);
                value2 = handleMaultipleValue(result, field2, value2);
                result.put(field2, value2);
                return result;
            }
            // And then the general case; default map size is 16
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            value1 = handleMaultipleValue(result, field1, value1);
            result.put(field1, value1);
            value2 = handleMaultipleValue(result, field2, value2);
            result.put(field2, value2);
            do
            {
                String fieldName = jp.getCurrentName();
                jp.nextToken();
                Object value = deserialize(jp, ctxt);
                value = handleMaultipleValue(result, fieldName, value);
                result.put(fieldName, value);
            } while (jp.nextToken() != JsonToken.END_OBJECT);
            return result;
        }

        @SuppressWarnings("unchecked")
        private Object handleMaultipleValue(Map<String, Object> map,
                                            String key,
                                            Object value)
        {
            if (!map.containsKey(key))
            {
                return value;
            }

            Object originalValue = map.get(key);
            if (originalValue instanceof List)
            {
                ((List) originalValue).add(value);
                return originalValue;
            }
            else
            {
                ArrayList newValue = new ArrayList();
                newValue.add(originalValue);
                newValue.add(value);
                return newValue;
            }
        }

    }
}
