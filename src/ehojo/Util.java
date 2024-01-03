package ehojo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Util {
	
	/**
	 * 리스트를 CSV파일로 만들기
	 * @param filePath
	 * @param apiDataList
	 */
	public void makeCsv(String filePath, List<HashMap<String, Object>> apiDataList) {
		Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Map<String, String>>>() {}.getType();
		// 저장 파일 생성 
		File file = new File(filePath);	
		OutputStreamWriter fw = null;	
		List<String> keyList = new ArrayList();
		
		int apiDataListIndex=0;
		int apiDataSize=0;
		// 맵 엔트리 사이즈 
		int dataSize = apiDataList.get(apiDataListIndex).entrySet().size();
		// 맵 반복문 카운트 체크
		
		int tempCount = 0;
		for(int i=0; i<apiDataList.size(); i++) {
			if(apiDataList.get(i).size()>apiDataSize) {
				apiDataSize=apiDataList.get(i).size();
				apiDataListIndex=i;
			}
		}
		// 파일 생성 
		try {
			fw = new OutputStreamWriter(new FileOutputStream(file));

			// 제목 
			for (Entry<String, Object> entry : apiDataList.get(apiDataListIndex).entrySet()) 
			{
				tempCount++;
				keyList.add(entry.getKey());
				// 헤더 내용 제외
				/*
						if(tempCount == dataSize) {
						   fw.write(entry.getKey()+"\r\n");
						}else {
						   fw.write(entry.getKey() + ",");
						}				
				 */
			}			
			// 데이터 
			for(HashMap<String, Object> temp : apiDataList ) {
				tempCount = 0;
				for(int i=0; i<keyList.size(); i++) {

					if(i+1 == dataSize) {						
						fw.write(temp.get(keyList.get(i))+"\r\n");
					}else {					
						fw.write(temp.get(keyList.get(i))+",");
					}	
				}

			}

			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			 
		} finally {
			if(fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * json오브젝트를 맵으로 변환
	 * @param jsonStr
	 * @return
	 */
	public Map<String, Object> jsonObjectToMap( String jsonStr ) {
        Map<String, Object> map = null;
        try {
            map = new ObjectMapper().readValue(jsonStr, Map.class) ;            	
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
	
	public Properties readProperties(String propFileName) {
	    Properties prop = new Properties();
	    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
	    
	    try {
	        if (inputStream != null) {
	            prop.load(inputStream);
	            return prop;
	        } else {
	            throw new FileNotFoundException("프로퍼티 파일 '" + propFileName + "'을 resource에서 찾을 수 없습니다.");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	/**
	 * 입력한 변수만큼 뺀 날짜 가져오기
	 * @param day
	 * @return
	 */
	public String getDate(int day) {
		Calendar calendar = new GregorianCalendar();
		SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
		
		if (day != 0) {
			calendar.add(Calendar.DATE, day);
		}
		
		String chkDate = SDF.format(calendar.getTime());		
		
		return chkDate;
	}

}
