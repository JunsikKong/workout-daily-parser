package com.jskong.com;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class WorkoutDailyParserMain {
	public static final String PATH_IN_TXT = System.getProperty("user.dir") + "\\textfile.txt";
	public static final String PATH_BASE_JSON = System.getProperty("user.dir") + "\\jsonfile.json";
	public static final String PATH_OUT_JSON = System.getProperty("user.dir") + "\\out.json";
	
	public static final String VAR_CURRENT_YEAR = "2023";
	public static final String REG_DATE8 = "(19|20)([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_DATE4 = "(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_NAME_PATTERN = "([가-힣ㄱ-ㅎ]+)(?![가-힣ㄱ-ㅎ]*-)";
	public static final String REG_TITLE = "[ㄱ-ㅎ]+";
	public static final String REG_VALUE = "[^wdf0-9.가-힣ㄱ-ㅎ-\\/\\*\\(\\)]+";
	public static final String REG_WARM_AND_DROP = "w[0-9]*|d[0-9]*";
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		System.out.println(getWorkoutJSON("").toJSONString());
	}
	
	/* [ Delimiter ]	[ Description ]
	 * [   ]			set delimiter
	 * 
	 * [ - ]			weight
	 * [ / ]			rest
	 * [ * ]			set count
	 * [ ( ]			ascending/descending
	 * [ ) ]			ascending/descending
	 * 
	 * [ w(n) ]			warm set (n set)
	 * [ d(n) ]			drop set (n set)
	 * [ f ]			failure point
	 * 
	 * [ c ]			compound set
	 * [ s ]			super set
	 *  
	 * [ ) ]			warming up set (not used)
	 * [ ( ]			drop set (not used)
	 * 
	 * 
	 * ex1)
	 * ㅅㅋㅌ 바벨			◀ WORK_NAME = WORK_TITLE + WORK_OPTION
	 * 60-10 10 10/90	◀ WORK_VALUE
	 * 
	 * 
	 * ex2)
	 * <IN>
	 * ㅅㄹㄹ
	 * 3-100 
	 * ㅁㅍ
	 * ㅅㄹㄹ
	 * w2 20-10 26-10*4
	 * 10-15*5/60
	 *
	 * <OUT>
	 * title :	["ㅅㄹㄹ", "ㅁㅍ;ㅅㄹㄹ"]
	 * part :	["어깨", "어깨;어깨"]
	 * rest :	["0", "60"]
	 * option :	["" , ""]
	 * reps :	["100", "w w 10 10 10 10 10;15 15 15 15 15"]
	 * weight :	["3", "0 0 20 26 26 26 26;10 10 10 10 10"]
	 * 
	 * 
	 * < Json Structure >
	 * ├ DATE (DATE)
	 * └ WORK (ARRAY)
	 *   ├ WORK_SEQ (INTEGER)	◀ AUTO SET
	 *   ├ WORK_TITLE (STRING)	◀ CONVERT FROM BASEDATA
	 *   ├ WORK_PART (STRING)	◀ GET FROM BASEDATA
	 *   ├ WORK_REST (INTEGER)	◀ GET FROM WORK_VALUE
	 *   ├ WORK_OPTION (STRING)	
	 *   └ WORK_SET (ARRAY)
	 *     ├ SET_SEQ (INTEGER)	◀ AUTO SET
	 *     ├ SET_WEIGHT (STRING)
	 *     └ SET_REPS (INTEGER)
	 * 
	 * 
	 * 
	 * < Caution >
	 * - 공백으로 횟수(reps) 구분 이외에 다른 경우에도 공백 존재 가능
	 * - ex) 55 - 12 12 10 << '55'와 '-' 사이의 공백
	 * - 따라서 공백 앞 뒤의 인접자가 숫자인지 아닌지 판별하는 로직 필요
	 * - 
	 * - 무게(weight)에 소숫점이 존재 가능
	 * - WORK_SEQ, SET_SEQ는 1부터 시작, 1씩 증가
	 * - 컴파운드, 슈퍼 세트 어떻게 저장할 지 고민
	 * - 
	 * 
	 * */
	
	public static JSONObject getWorkoutJSON(String inputData) {
		JSONObject baseJSON = getBaseJSON();

        String finDate = "";
        // 리스트 1개의 단위 = 운동 1셋트 (컴파운드의 경우 묶어서 ; 로 구분)
        ArrayList<String> finTitle = new ArrayList<String>();
        ArrayList<String> finPart = new ArrayList<String>();
        ArrayList<String> finRest = new ArrayList<String>();
        ArrayList<String> finOption = new ArrayList<String>();
        ArrayList<String> finReps = new ArrayList<String>();
        ArrayList<String> finWeight = new ArrayList<String>();
        
        int cntName = 0;
        int cntValue = 0;
        Pattern patternName = Pattern.compile(REG_NAME_PATTERN);
        
        try {
        	Path path = Paths.get(PATH_IN_TXT);
            List<String> lines = Files.readAllLines(path);

            // 반복 : 문장에서 줄
            for (String line : lines) {
            	// 0. 데이터 전처리
            	line = line.trim(); 						// line 양 옆의 공백 제거
            	line = line.replaceAll("\\s*-\\s*", "-"); 	// '-' 양 옆의 공백 제거
            	if ("".equals(line)) { continue; } 			// 공백 ▶ PASS
            	System.out.println("● INPUT :: " + line);
            	
            	// 1. DATE
            	if ("".equals(finDate)) {
            		System.out.println("▶▶▶ 1. DATE");
            		if (line.matches(REG_DATE8)) {
            			finDate = line;
            			System.out.println("▶▶▶ DATE ... OK\n");
            		}
            		else if (line.matches(REG_DATE4)) {
            			finDate = VAR_CURRENT_YEAR + line;
            			System.out.println("▶▶▶ DATE ... OK\n");
            		}
            		else {
            			System.out.println("### 경고 :: 날짜 형식에 맞지 않음.\n");
            		}
            		continue;
            	}

            	// 2. WORK NAME
            	if (patternName.matcher(line).find()) {
            		System.out.println("▶▶▶ 2. NAME");
            		
            		String[] arrName = line.split(" ");
            		String tmpTitle = "";
            		String strTitle = "";
            		String strOption = "";
            		String strPart = "";
            		
            		// 2.1. 추출
            		for (String str : arrName) {
            			str = str.trim();
            			if(str.equals("")) { continue; }
            			if(tmpTitle.equals("")) { tmpTitle = str; }
            			else { strOption += str.replace("*", ""); }
            		}
            		if(strOption.equals("")) { strOption = "[없음]"; }
            		
            		if(tmpTitle.matches(REG_TITLE)) {
            			for(String base : WorkoutBaseEditor.LIST_BASE_PART) {
            				JSONArray tempJSONArray = (JSONArray)baseJSON.get(base);
            				for(int i = 0; i < tempJSONArray.size(); i++) {
            					JSONObject tempJSONObject = (JSONObject)tempJSONArray.get(i);
            					Iterator iter =  tempJSONObject.keySet().iterator();
    							String strKey = (String)iter.next();
    							if(strKey.equals(tmpTitle)) {
    								System.out.println("### 성공");
    								strTitle = tempJSONObject.get(strKey).toString();
    								strPart = base;
    								break;
    							}
            				}
            				if(!strTitle.equals("")) { break; }
            			}
            			if(strTitle.equals("")) {
            				System.out.println("### 경고 : 초성이지만 매핑되지 않음");
            				strTitle = tmpTitle;
            				strPart = "[초성O 매핑X]";
            			}
            		}
            		else {
            			System.out.println("### 경고 : 초성아님");
            			strTitle = tmpTitle;
        				strPart = "[초성X]";
            		}
            		
            		
            		/*
            		 * 1. 매핑된 경우 (성공)
            		 * 2. 매핑되지 않은 초성 (실패)
            		 * 3. 초성이 아닌 경우 (실패)
            		 * */
            		
            		System.out.println("[1] TITLE  :: " + strTitle);
            		System.out.println("[2] OPTION :: " + strOption);
            		System.out.println("[3] PART   :: " + strPart);
            		
            		// 리스트 적재
            		if(cntName > cntValue) {
            			int lastIndex = finTitle.size() - 1;
            			String lastTitle = finTitle.get(lastIndex);
            			String lastOption = finOption.get(lastIndex);
            			String lastPart = finPart.get(lastIndex);
            			finTitle.set(lastIndex, lastTitle + ";" + strTitle);
            			finOption.set(lastIndex, lastOption + ";" + strOption);
            			finPart.set(lastIndex, lastPart + ";" + strPart);
            		}
            		else {
            			finTitle.add(strTitle);
            			finOption.add(strOption);
            			finPart.add(strPart);
            		}

            		cntName++;
            		System.out.println("");
            	}
            	// 4. WORK VALUE 처리
            	else {
            		// 4.0. NAME보다 VALUE가 먼저 발견될 경우 ▶ PASS
            		if(cntName <= cntValue) {
            			System.out.println("### 경고 :: NAME 형식에 맞지 않음.\n");
            			continue;
            		}
            		System.out.println("▶▶▶ 3 VALUE");
            		
            		line = line.replace("-", " - ");
            		line = line.replace("/", " / ");
            		line = line.replace("*", " * ");
            		line = line.replace("(", " ( ");
            		line = line.replace(")", " ) ");
            		
            		String[] arrValue = line.split(REG_VALUE);
            		
            		String strRest = "";
            		String strReps = "";
            		String strWeight = "";
            		
            		boolean isRest = false;
            		boolean isInBracket = false;
            		
            		String tmpPrev = "";
            		ArrayList<String> tmpWeight = new ArrayList<String>();
            		ArrayList<String> tmpReps = new ArrayList<String>();
            		
            		
            		for (String str : arrValue) {
            			System.out.println("VALUE :: " + str); 
            			
            			if(str.matches(REG_WARM_AND_DROP)) {
            				String frontValue = str.substring(0, 1);
            				String backValue  = str.substring(1);
            				if(backValue.equals("")) { backValue = "1"; }
            				try {
            					int numBackValue = Integer.parseInt(backValue);
            					for(int i = 0; i < numBackValue; i++) {
            						strReps += frontValue + " ";
            						strWeight += frontValue + " ";
            					}
            				}
            				catch (NumberFormatException ex) {
            					ex.printStackTrace();
            				}
            			}
            			
            			if(str.equals("-")) {
            				if (tmpPrev.matches("[0-9.가-힣]+")) {
            					strWeight = tmpPrev;
            					System.out.println("[5] WEIGHT :: " + strWeight);
            				}
            				else {
            					strWeight = "[숫자X]";
            					System.out.println("### 경고 : 중량이 숫자가 아님");
            				}
            				continue;
            			}
            			
            			if(str.equals("/")) {
            				isRest = true;
            				continue;
            			}
            			
            			if(isRest) {
            				//...숫자인지확인
            				if (str.matches("[0-9]+")) {
            					strRest = str;
            					System.out.println("[4] REST   :: " + strRest);
            				}
            				else {
            					strRest = "[숫자X]";
            					System.out.println("### 경고 : 휴식시간이 숫자가 아님");
            				}
            				isRest = false;
            				continue;
            			}
            			
            			if(str.equals("*")) {
            				if (tmpPrev.matches("[0-9]+")) {
            					// strPrev int 변환 후 for 반복으로 배열 삽입
            					strWeight = tmpPrev;
            					System.out.println("[5] WEIGHT :: " + strWeight);
            				}
            				else if (tmpPrev.matches(")")) {
            					
            				}
            				else {
            					strWeight = "[숫자X]";
            					System.out.println("### 경고 : 중량이 숫자가 아님");
            				}
            				continue;
            			}
            			
            			if(str.equals("(")) {
            				isInBracket = true;
            				tmpWeight.clear();
            				tmpReps.clear();
            				continue;
            			}
            			
            			if(str.equals(")")) {
            				isInBracket = false;
                			continue;
            			}
            			
            			
            			//****************main
            			

            			tmpPrev = str;
            		}
            		
            		// fin 리스트에 추가
            		finRest.add(strRest);

            		// 3.4. 운동NAME-휴식 ▶ 배열 적재 (없을 경우 c/s 고려)
            		/*
            		
            		- "/" 이 없을 경우
            		1) c/s ▶ 배열 적재 "운동명;운동명"
            		2) 카운트
            		3) 0초 휴식

            		1) 정규식으로 나눠 split 배열 저장
            		2) 숫자/예약어 등 분기 설정
            		w일 경우
            		wn일 경우
            		d일 경우
            		dn일 경우
            		f일 경우
            		- , / , * , ( , ) , 
            		
            		1) rest --- 0/null
            		=> name/value count 비교
            		==> c/s판단
            		==> 경고 후에 0초

            		2) weight --- 0/null
            		=> 경고 후에 0kg

            		3) reps --- null
            		=> 경고 후에 0회
            		*/
            		
            		cntValue++;
            		System.out.println("");
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("### NAME :: " + cntName + " / VALUE :: " + cntValue);

		//JSONObject jsonTempSet = new JSONObject();
		//JSONObject jsonTempWork = new JSONObject();
        
        
        JSONObject jsonFinal = new JSONObject();
        
        int works = 5; // 변경
        int sets = 3; // 변경

        JSONArray jsonArrTemp1 = new JSONArray();
        jsonFinal.put("DATE", finDate);
        for(int i = 0; i < works; i++) {
        	JSONObject jsonTemp1 = new JSONObject();
        	JSONArray jsonArrTemp2 = new JSONArray();
        	jsonTemp1.put("WORK_SEQ", i + 1);
        	jsonTemp1.put("WORK_TITLE", "운동이름");
        	jsonTemp1.put("WORK_PART", "부위");
        	jsonTemp1.put("WORK_OPTION", "옵션");
        	jsonTemp1.put("WORK_REST", "휴식시간");
        	for(int j = 0; j < sets; j++) {
        		JSONObject jsonTemp2 = new JSONObject();
            	jsonTemp2.put("SET_SEQ", j + 1);
            	jsonTemp2.put("SET_WEIGHT", "무게");
            	jsonTemp2.put("SET_REPS", "횟수");
            	jsonArrTemp2.add(jsonTemp2);
            }
        	jsonTemp1.put("WORK_SET", jsonArrTemp2);
        	jsonTemp1.put("WORK_SET", "TT");
        	jsonArrTemp1.add(jsonTemp1);
        }
        jsonFinal.put("WORK", jsonArrTemp1);
        
        return jsonFinal;
	}
	
	public static String getWorkoutView(String inputData) {
		
		return "";
	}
	
	public static JSONObject getBaseJSON() {
		JSONObject resultJSON = new JSONObject();
		JSONParser parser = new JSONParser();

		try {
			FileReader reader = new FileReader(PATH_BASE_JSON);
			Object obj = parser.parse(reader);
			resultJSON = (JSONObject) obj;
			reader.close();
		} 
		catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return resultJSON;
	}
}
