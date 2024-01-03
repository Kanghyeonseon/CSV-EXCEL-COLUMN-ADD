package ehojo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.indigo.util.EncryptionUtils;

import net.sf.json.JSONArray;

import net.sf.json.JSONObject;
//import org.json.simple.JSONObject;

public class client {

	public static void main(String[] args) throws Exception {
		
		Util util = new Util();
		Properties prop = util.readProperties("config/ehojo.properties");
		//인터페이스명
		String ifname =args[0];
		//파일저장경로
		String filePath =args[1];
		
		/*
		//현재페이지
		String curPage =args[2];
		//페이지사이즈
		String pageSize =args[3];
		//변수1
		String args1 = args[4];
		//변수2
		String args2 = args[5];
		//변수3
		String args3 = args[6];
		*/
		
		/**********************************************************************
    	 * 필수 값 설정
    	 *********************************************************************/
    	//apiKey 설정
    	String apiKey = prop.getProperty("apiKey");

    	//암복호화 key 설정
    	String encKey = prop.getProperty("encKey");

        // 호출 url 설정
        String url = prop.getProperty("url")+ifname;
        
        //인터페이스 ID
        String ifId = prop.getProperty(ifname);
        
        //송신 기관 코드
        String lafCode = prop.getProperty("lafCode");
        
    	/**********************************************************************
    	 * 필수 값 설정 끝
    	 *********************************************************************/
        
        // HTTP Client
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        HttpClient httpClient = httpClientBuilder.build();
        // HTTP Method
        HttpPost httpPost = new HttpPost(url);
        // HTTP Header
        // API 인증키 (외부기관에서 요청 시 필요)
        httpPost.setHeader("API_KEY", apiKey); 
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        // JSON 요청 메시지
        JSONObject jsonReq = new JSONObject();
        // Header
        JSONObject header = new JSONObject();
        // body
        JSONObject body = new JSONObject();

        // Header 설정
        header.put("ifId", ifId); //  인터페이스 아이디
        header.put("tranId", EncryptionUtils.makeTxId(ifId)); // 트랜잭션 아이디
        header.put("transfGramNo ", ""); // 거래일련번호
        header.put("trnmtInstCd", lafCode); // 송신 기관코드
        header.put("rcptnInstCd", "MOI"); // 수신 기관코드
        header.put("trnmtInstSysCd", "EHJ"); // 송신 기관 시스템 코드
        header.put("rcptnInstSysCd", "EHJ"); // 수신 기관 시스템 코드
        
        //어제날짜 yyyyMMdd
        String yesterday = util.getDate(-1);
    	/**********************************************************************
    	 * 조회 조건 필수 값 설정
    	 *********************************************************************/
        // body 설정
        //현재 페이지
        body.put("fyr", yesterday.substring(0, 4));
        //최종수정시작일자
        body.put("lastMdfcnBgngYmd", yesterday);
        //최종수정종료일자
        body.put("lastMdfcnEndYmd", yesterday);
        body.put("curPage", "1");
        
        //페이지 사이즈(최대 3000)
        body.put("pageSize", "100");
        
        
    	/**********************************************************************
    	 * 인터페이스 정의서를 참고하여 추가로 필요한 항목 설정 추가
    	 * 
    	 * 데이터가 많은경우 loop 처리하여 페이지 순차 호출
    	 * 
    	 * 조회 조건 필수 값 설정 끝
    	 *********************************************************************/

        // JSON 요청 메시지 설정
        jsonReq.put("header", header);
        // body 암호화 (외부기관에서 요청 시 필요)
        jsonReq.put("body", EncryptionUtils.encryptStringAria(body.toString(), encKey)); 
        System.out.println("요청 전문" + jsonReq.toString());
        
        // JSON 오브젝트 등록
		
		JSONArray jsonArray = null;	
        
        // Request → Response
        httpPost.setEntity(new StringEntity(jsonReq.toString(), "UTF-8"));
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        // 정상 응답
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            // 응답 메시지
            String strRes = EntityUtils.toString(httpEntity, "UTF-8");
            // JSON Parsing
            JSONObject jsonRes = JSONObject.fromObject(strRes);
            // 요청결과
            JSONObject resResult = (JSONObject)jsonRes.get("result");
            
            // body 복호화 (외부기관에서 요청 시 필요)
            JSONObject resBody = JSONObject.fromObject(EncryptionUtils.decryptStringAria(jsonRes.getString("body"), encKey));
            jsonRes.put("body", resBody);
            System.out.println("요청 결과 전문 :: " + jsonRes.toString());
            
            
            String [] depthList = "body>data".split(">");
    		
    		// 데이터 리스트를 담을 객체( 한라인씩)
    		List<HashMap<String, Object>> apiDataList = new ArrayList<HashMap<String, Object>>();
    		// 구분자에따라 JosnArray에 할당 
    		if(jsonArray == null) {
    			// jsonObject to HashMap
    			Map<String, Object> resultMap = new HashMap<String, Object>();
    			resultMap = util.jsonObjectToMap(jsonRes.toString());
    		
    			for(String depth : depthList) {
    				depth = depth.trim();
    				Object temp = (Object)resultMap.get(depth);
    				// 리스트 판별
    				if(temp instanceof List) {
    					apiDataList = (ArrayList<HashMap<String, Object>>) temp;
    				}else {
    					resultMap = (HashMap<String, Object>) temp;	
    				}
    			}
    		}
         	
    		util.makeCsv(filePath, apiDataList);
            
      
        } else {
            // 이상 응답
            System.out.println("에러 코드 :: " + httpResponse.getStatusLine().getStatusCode());
        }
        /*
        String jsonStr = "{\"header\":{\"ifId\":\"LR_FE_LAF_SA_00077\",\"tranId\":\"LR_FE_LAF_SA_00077_20230131124050721_00001\",\"transfGramNo\":null,\"trnmtInstCd\":\"CHN\",\"rcptnInstCd\":\"MOI\",\"trnmtInstSysCd\":\"EHJ\",\"rcptnInstSysCd\":\"EHJ\",\"userDeptCode\":null,\"userName\":null},\"result\":{\"rsltCd\":\"0000\",\"rsltMsg\":\"성공\"},\"body\":{\"data\":[{\"rn\":\"1\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000006\",\"deptCd\":\"1067002\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"1549930\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230103\",\"pmodCn\":\"임시일상경비 지출원 임명 및 지출 송금의뢰\",\"pmodRgstrNo\":\"00000001\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-03T15:52:14+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301039000000395\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-03T15:52:14+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230103155122\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230103155214\"},{\"rn\":\"2\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000007\",\"deptCd\":\"1105035\",\"ratLafCd\":null,\"gofCd\":\"0036\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"3078055\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"공공운영비 지출(2022년 12월 청구분 청사 등 전기요금)\",\"pmodRgstrNo\":\"00000001\",\"cmdAprvUsrId\":\"64400002007030000800\",\"cmdAprvDt\":\"2023-01-04T09:45:23+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030000800\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000013\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030000800\",\"mdfcnDt\":\"2023-01-04T09:45:23+09\",\"frstRgstrUsrId\":\"64400002021010000036\",\"frstRgstrDt\":\"20230104085749\",\"lastMdfcnUsrId\":\"64400002007030000800\",\"lastMdfcnDt\":\"20230104094523\"},{\"rn\":\"3\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000008\",\"deptCd\":\"1125030\",\"ratLafCd\":null,\"gofCd\":\"0049\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"2400000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"19\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"12월분 관리과 및 임업시험과 당직수당 지급결의(40명)\",\"pmodRgstrNo\":\"00000001\",\"cmdAprvUsrId\":\"64400002007030002289\",\"cmdAprvDt\":\"2023-01-04T13:13:50+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030002289\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000019\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030002289\",\"mdfcnDt\":\"2023-01-04T13:13:50+09\",\"frstRgstrUsrId\":\"64400002021010000035\",\"frstRgstrDt\":\"20230104085911\",\"lastMdfcnUsrId\":\"64400002007030002289\",\"lastMdfcnDt\":\"20230104131350\"},{\"rn\":\"4\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000009\",\"deptCd\":\"1125030\",\"ratLafCd\":null,\"gofCd\":\"0049\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"297000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"1월 본소 무인경비시스템 경비이용료\",\"pmodRgstrNo\":\"00000002\",\"cmdAprvUsrId\":\"64400002007030002289\",\"cmdAprvDt\":\"2023-01-04T13:13:50+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030002289\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000022\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030002289\",\"mdfcnDt\":\"2023-01-04T13:13:50+09\",\"frstRgstrUsrId\":\"64400002021010000035\",\"frstRgstrDt\":\"20230104085947\",\"lastMdfcnUsrId\":\"64400002007030002289\",\"lastMdfcnDt\":\"20230104131350\"},{\"rn\":\"5\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000011\",\"deptCd\":\"1040062\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"20\",\"pmodSumAmt\":\"1950000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"2023년 학교급식지원시스템 운영 및 유지관리 기술평가위원수당\",\"pmodRgstrNo\":\"00000002\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000132\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104104749\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"6\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000012\",\"deptCd\":\"1009002\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"270000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"1월 자치경찰협력과 일상경비(203-04 부서운영업무추진비)\",\"pmodRgstrNo\":\"00000003\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000134\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104104845\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"7\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000013\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"2805000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 기본경비 사무관리비 일상경비 교부\",\"pmodRgstrNo\":\"00000004\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000136\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104104926\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"8\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000014\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"315000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 기본경비 부서운영업무추진비 일상경비 교부\",\"pmodRgstrNo\":\"00000005\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000137\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104104953\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"9\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000015\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"2805000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 기본경비 국내여비 일상경비 교부\",\"pmodRgstrNo\":\"00000006\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000143\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105303\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"10\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000016\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"4000000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 지방세운영 사무관리비 일상경비 교부\",\"pmodRgstrNo\":\"00000007\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000146\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105331\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"11\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000017\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"222000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 지방세운영 시책추진업무추진비 일상경비 교부\",\"pmodRgstrNo\":\"00000008\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000150\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105412\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"12\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000018\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"3424000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 공유재산관리 사무관리비 일상경비 교부\",\"pmodRgstrNo\":\"00000009\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000151\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105432\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"13\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000019\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"11000000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 공유재산관리 공공운영비 일상경비 교부\",\"pmodRgstrNo\":\"00000010\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000152\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105451\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"14\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000020\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"574000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 공유재산관리 국내여비 일상경비 교부\",\"pmodRgstrNo\":\"00000011\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000154\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105507\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"15\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000021\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"1174000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 지방세운영 국내여비 일상경비 교부\",\"pmodRgstrNo\":\"00000012\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000156\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105526\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"16\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000022\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"2400000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 세외수입업무 사무관리비 일상경비 교부\",\"pmodRgstrNo\":\"00000013\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000157\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105547\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"17\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000023\",\"deptCd\":\"1030041\",\"ratLafCd\":null,\"gofCd\":\"0010\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"500000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"1\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"세정과 1월 세외수입업무 국내여비 일상경비 교부\",\"pmodRgstrNo\":\"00000014\",\"cmdAprvUsrId\":\"64400002007030001715\",\"cmdAprvDt\":\"2023-01-04T11:03:24+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"N\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030001715\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000160\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030001715\",\"mdfcnDt\":\"2023-01-04T11:03:24+09\",\"frstRgstrUsrId\":\"64400002022100000021\",\"frstRgstrDt\":\"20230104105608\",\"lastMdfcnUsrId\":\"64400002007030001715\",\"lastMdfcnDt\":\"20230104110324\"},{\"rn\":\"18\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000024\",\"deptCd\":\"1110021\",\"ratLafCd\":null,\"gofCd\":\"0043\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"2025000\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"4\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"2023년 1월 복리후생비 - 대민활동비\",\"pmodRgstrNo\":\"00000001\",\"cmdAprvUsrId\":\"64400002007030002499\",\"cmdAprvDt\":\"2023-01-04T11:15:49+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"I\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030002499\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000162\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030002499\",\"mdfcnDt\":\"2023-01-04T11:15:49+09\",\"frstRgstrUsrId\":\"64400002021090000001\",\"frstRgstrDt\":\"20230104105647\",\"lastMdfcnUsrId\":\"64400002007030002499\",\"lastMdfcnDt\":\"20230104111549\"},{\"rn\":\"19\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000025\",\"deptCd\":\"1110021\",\"ratLafCd\":null,\"gofCd\":\"0043\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"1493750\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"30\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"2023년 1월 복리후생비 - 대민활동비\",\"pmodRgstrNo\":\"00000002\",\"cmdAprvUsrId\":\"64400002007030002499\",\"cmdAprvDt\":\"2023-01-04T11:15:49+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"I\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030002499\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000164\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030002499\",\"mdfcnDt\":\"2023-01-04T11:15:49+09\",\"frstRgstrUsrId\":\"64400002021090000001\",\"frstRgstrDt\":\"20230104105659\",\"lastMdfcnUsrId\":\"64400002007030002499\",\"lastMdfcnDt\":\"20230104111549\"},{\"rn\":\"20\",\"lafCd\":\"6440000\",\"fyr\":\"2023\",\"acntDvCd\":\"100\",\"expsDvCd\":\"10\",\"pmodNo\":\"000026\",\"deptCd\":\"1110021\",\"ratLafCd\":null,\"gofCd\":\"0043\",\"pmodStatCd\":\"50\",\"pmodDvCd\":\"10\",\"pmodSumAmt\":\"7505620\",\"fdTrsfrSumAmt\":\"0\",\"trgtSumNum\":\"35\",\"fdTrsfrSumNum\":\"0\",\"pmodYmd\":\"20230104\",\"pmodCn\":\"2023년 1월 복리후생비 - 직급보조비\",\"pmodRgstrNo\":\"00000003\",\"cmdAprvUsrId\":\"64400002007030002499\",\"cmdAprvDt\":\"2023-01-04T11:15:49+09\",\"slrySnum\":null,\"macCrtCnt\":\"0\",\"linkDvCd\":\"I\",\"ratYn\":\"N\",\"sndgTelno\":null,\"wtnncDvCd\":null,\"smsSndYn\":\"N\",\"pmodSnum\":null,\"pmodDutSnum1\":null,\"pmodDutSnum2\":null,\"owrSnum\":null,\"aprvTrgtUsrId\":\"64400002007030002499\",\"cardLinkYn\":\"N\",\"intrAtrzDocNo\":\"202301049000000165\",\"pmodHistNo\":null,\"ebnkAtrzYn\":\"Y\",\"atflGrpId\":null,\"mdfcnUsrId\":\"64400002007030002499\",\"mdfcnDt\":\"2023-01-04T11:15:49+09\",\"frstRgstrUsrId\":\"64400002021090000001\",\"frstRgstrDt\":\"20230104105712\",\"lastMdfcnUsrId\":\"64400002007030002499\",\"lastMdfcnDt\":\"20230104111549\"}],\"curPage\":1,\"pageRow\":20,\"totalCnt\":5302}}";
        
        // JSON 오브젝트 등록
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		JSONArray jsonArray = null;		
		jsonObject = (JSONObject) parser.parse(jsonStr);
		String [] depthList = "body>data".split(">");
		
		// 데이터 리스트를 담을 객체( 한라인씩)
		List<HashMap<String, Object>> apiDataList = new ArrayList<HashMap<String, Object>>();
		// 구분자에따라 JosnArray에 할당 
		if(jsonArray == null) {
			// jsonObject to HashMap
			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap = jsonObjectToMap(jsonObject);
		
			for(String depth : depthList) {
				depth = depth.trim();
				Object temp = (Object)resultMap.get(depth);
				// 리스트 판별
				if(temp instanceof List) {
					apiDataList = (ArrayList<HashMap<String, Object>>) temp;
				}else {
					resultMap = (HashMap<String, Object>) temp;	
				}
			}
		}
     	
       
        makeCsv("test.csv", apiDataList);
		*/
	}
	

	
}
