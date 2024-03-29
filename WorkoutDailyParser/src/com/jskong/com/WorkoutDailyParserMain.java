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
	/* 상수 */
	public static final String CONST_CURRENT_YEAR = "2023";
	public static final String CONST_NULL = "xxx";
	
	/* 경로 */
	public static final String PATH_IN_TXT = System.getProperty("user.dir") + "\\textfile.txt";
	public static final String PATH_BASE_JSON = System.getProperty("user.dir") + "\\jsonfile.json";
	public static final String PATH_OUT_JSON = System.getProperty("user.dir") + "\\out.json";
	
	/* 구분자 */
	public static final String DELIMITER_WORKOUT = "─";
	public static final String DELIMITER_BRACKET = "│";
	public static final String DELIMITER_WORKSET = "┌";
	
	/* 정규식 */
	public static final String REG_DATE8 = "(19|20)([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_DATE4 = "(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_NAME_PATTERN = "([가-힣ㄱ-ㅎ]+)(?![가-힣ㄱ-ㅎ]*-)";
	public static final String REG_TITLE = "[ㄱ-ㅎ]+";
	public static final String REG_VALUE = "[^wdf0-9.가-힣ㄱ-ㅎ-\\/\\*\\(\\)]+";
	public static final String REG_WARM_AND_DROP = "w[0-9]*|d[0-9]*";
	
	/* 최종값 */
	private static String            FIN_DATE   = "";
	private static ArrayList<String> FIN_TITLE  = new ArrayList<String>();
	private static ArrayList<String> FIN_PART   = new ArrayList<String>();
	private static ArrayList<String> FIN_REST   = new ArrayList<String>();
	private static ArrayList<String> FIN_OPTION = new ArrayList<String>();
	private static ArrayList<String> FIN_REPS   = new ArrayList<String>();
	private static ArrayList<String> FIN_WEIGHT = new ArrayList<String>();
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		System.out.println(getJSONWorkout("").toJSONString());
	}
	
	/* [ Delimiter ]	[ Description ]
	 * [   ]			set delimiter
	 * 
	 * [ - ]			weight
	 * [ / ]			rest
	 * [ * ]			set loop
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
	 * ㅂㄹㄹ 누워서
	 * (6-20 10-15 14-10)*4/30
	 *
	 * <OUT>
	 * title :	["ㅅㄹㄹ", "ㅁㅍ;ㅅㄹㄹ", "ㅂㄹㄹ"]
	 * part :	["어깨", "어깨;어깨", "어깨"]
	 * rest :	["0", "60", "30"]
	 * option :	["", "", "누워서"]
	 * reps :	["100", "w2 10 10 10 10 10;15 15 15 15 15", "6~10~14 6~10~14 6~10~14 6~10~14"]
	 * weight :	["3", "w2 20 26 26 26 26;10 10 10 10 10"  , "20~15~10 20~15~10 20~15~10 20~15~10"]
	 * 
	 * 
	 * < Json Structure >
	 * ├ DATE (DATE)
	 * └ WORK (ARRAY)
	 *   ├ WORK_SEQ (INTEGER)	◀ *AUTO SET
	 *   ├ WORK_TITLE (STRING)	◀ CONVERT FROM BASEDATA
	 *   ├ WORK_PART (STRING)	◀ GET FROM BASEDATA
	 *   ├ WORK_REST (INTEGER)	◀ GET FROM WORK_VALUE
	 *   ├ WORK_OPTION (STRING)	
	 *   └ WORK_SET (ARRAY)
	 *     ├ SET_SEQ (INTEGER)	◀ *AUTO SET
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
	
	public static JSONObject getJSONWorkout(String inputData) {
		JSONObject baseJSON = getJSONBase();

        int cntName = 0;
        int cntValue = 0;

        try {
        	Path path = Paths.get(PATH_IN_TXT);
            List<String> lines = Files.readAllLines(path);

            for (String line : lines) {
            	// 0. 데이터 전처리
            	line = line.trim();
            	if (line.equals("")) { continue; }
            	Log.debug("▶line :: " + line);
            	
            	/* 1. DATE *****************************************************************************************************/
            	if (FIN_DATE.equals("")) {
            		Log.debug("▶date 시작");
            		if (line.matches(REG_DATE8)) {
            			FIN_DATE = line;
            			Log.info("▶▶date 성공 8자리");
            		}
            		else if (line.matches(REG_DATE4)) {
            			FIN_DATE = CONST_CURRENT_YEAR + line;
            			Log.info("▶▶date 성공 4자리");
            		}
            		else {
            			Log.error("▶▶date 실패, 날짜형식 아님");
            		}
            		Log.debug("▶date 종료");
            		continue;
            	}

            	/* 2. NAME *****************************************************************************************************/
            	if (Pattern.compile(REG_NAME_PATTERN).matcher(line).find()) {
            		Log.debug("▶name 시작");
            		String[] arrName = line.split(" ");
            		String tmpTitle = "";
            		String strTitle = "";
            		String strOption = "";
            		String strPart = "";
            		
            		// 2.1. 추출
            		for (String str : arrName) {
            			str = str.trim();
            			if(str.equals("")) { continue; } // pass
            			if(tmpTitle.equals("")) { tmpTitle = str; }
            			else { strOption += str.replace("*", ""); }
            		}
            		
            		if(tmpTitle.matches(REG_TITLE)) {
            			for(String base : WorkoutBaseEditor.LIST_BASE_PART) {
            				JSONArray tempJSONArray = (JSONArray)baseJSON.get(base);
            				for(int i = 0; i < tempJSONArray.size(); i++) {
            					JSONObject tempJSONObject = (JSONObject)tempJSONArray.get(i);
            					Iterator iter =  tempJSONObject.keySet().iterator();
    							String strKey = (String)iter.next();
    							if(strKey.equals(tmpTitle)) {
    								Log.info("▶▶name 성공");
    								strTitle = tempJSONObject.get(strKey).toString();
    								strPart = base;
    								break;
    							}
            				}
            				if(!strTitle.equals("")) { break; }
            			}
            			if(strTitle.equals("")) {
            				Log.error("▶▶name 실패, 초성이지만 매핑되지 않음");
            				strTitle = tmpTitle;
            				strPart = CONST_NULL;
            			}
            		}
            		else {
            			for(String base : WorkoutBaseEditor.LIST_BASE_PART) {
            				JSONArray tempJSONArray = (JSONArray)baseJSON.get(base);
            				for(int i = 0; i < tempJSONArray.size(); i++) {
            					JSONObject tempJSONObject = (JSONObject)tempJSONArray.get(i);
            					Iterator iter =  tempJSONObject.values().iterator();
    							String strValue = (String)iter.next();
    							if(strValue.equals(tmpTitle)) {
    								Log.error("▶▶name 실패, 초성이 아닌데 매핑됨");
    								strTitle = tmpTitle;
    								strPart = base;
    								break;
    							}
            				}
            				if(!strTitle.equals("")) { break; }
            			}
            			if(strTitle.equals("")) {
            				Log.error("▶▶name 실패, 초성도 아닌데 매핑되지도 않음");
            				strTitle = tmpTitle;
            				strPart = CONST_NULL;
            			}
            		}
            		
            		Log.info("▶▶name.title  :: " + strTitle);
            		Log.info("▶▶name.option :: " + strOption);
            		Log.info("▶▶name.part  :: " + strPart);
            		
            		// 리스트 적재
            		if(cntName > cntValue) {
            			int lastIndex = FIN_TITLE.size() - 1;
            			FIN_TITLE.set(lastIndex, FIN_TITLE.get(lastIndex) + DELIMITER_WORKOUT + strTitle);
            			FIN_OPTION.set(lastIndex, FIN_OPTION.get(lastIndex) + DELIMITER_WORKOUT + strOption);
            			FIN_PART.set(lastIndex, FIN_PART.get(lastIndex) + DELIMITER_WORKOUT + strPart);
            		}
            		else {
            			FIN_TITLE.add(strTitle);
            			FIN_OPTION.add(strOption);
            			FIN_PART.add(strPart);
            		}

            		cntName++;
            		Log.debug("▶name 종료");
            	}
            	/* 3. VALUE *****************************************************************************************************/
            	else {
            		if(cntName <= cntValue) {
            			Log.error("▶▶value 실패, name 형식에 맞지 않음");
            			continue;
            		}
            		
            		Log.debug("▶value 시작");
            		line = line.replace("-", " - ");
            		line = line.replace("/", " / ");
            		line = line.replace("*", " * ");
            		line = line.replace("(", " ( ");
            		line = line.replace(")", " ) ");
            		
            		String[] arr = line.split(REG_VALUE);
            		
            		// ********* 데이터 검증
            		/*
            		 * 1) begin
            		 * 2) end
            		 * 3) number
            		 * 4) string
            		 * 5) -
            		 * 6) /
            		 * 7) *
            		 * 8) (
            		 * 9) )
            		 * 10) wn
            		 * 11) dn
            		 * 12) f
            		 * */
            		for (int i = 0; i < arr.length; i++) {
            			if (i == 0) { // begin
            				if(i == arr.length - 1) {
            					
            				}
            			}
            			else if (i == arr.length - 1) { // end
            				
            			}
            		}
            		
            		
            		
            		
            		// *******************
            		
            		// 임시로 저장될 변수
            		String tmpRest = "";
            		String tmpReps = "";
            		String tmpWeight = "";
            		String prevValue = "";
            		
            		// 1개 단위 : 1셋, 괄호 안의 구간 반복값(어센딩/디센딩) 
            		String tmpBracketWeight = "";
            		String tmpBracketReps = "";
            		
            		// fin list에 저장 될 최종 변수
            		String strRest = "";
            		String strReps = "";
            		String strWeight = "";
            		
            		boolean isRest = false; // true 일 경우 다음 값은 휴식 값
            		boolean isLoop = false; // true 일 경우 다음 값은 휴식 값
            		boolean isInBracket = false; // true 일 경우 tmpBracket 리스트에도 값 추가
            		
            		for (String str : arr) {
            			Log.info("▶▶value  :: " + str);
            			
            			// [ w d ] : 워밍업 / 드랍세트
            			if(str.matches(REG_WARM_AND_DROP)) {
            				String frontValue = str.substring(0, 1);
            				String otherValue  = str.substring(1);
            				if(otherValue.equals("")) { otherValue = "1"; }
            				try {
            					int numBackValue = Integer.parseInt(otherValue);
            					for(int i = 0; i < numBackValue; i++) {
            						tmpReps += DELIMITER_BRACKET + frontValue;
            						tmpWeight += DELIMITER_BRACKET + frontValue;
            					}
            					tmpReps = tmpReps.substring(1);
            					tmpWeight = tmpWeight.substring(1); 
            				}
            				catch (NumberFormatException ex) {
            					ex.printStackTrace();
            				}
            			}
            			
            			// [ 숫자 ] : 횟수
            			if(str.matches("[0-9]+")) {
            				if(prevValue.equals("")) { // 이전값이 없을 때 ▶ 처음일 때
            					continue;
            				}
            				if(prevValue.matches("[0-9]+")) { 
            					
            				}
            				
            			}
            			
            			
            			// [ / ] : 휴식
            			if(str.equals("/")) {
            				isRest = true;
            				continue;
            			}
            			if(isRest) {
            				//...숫자인지확인
            				if (str.matches("[0-9]+")) {
            					tmpRest += DELIMITER_WORKOUT + str;
            					Log.info("▶▶name.rest  :: " + tmpRest);
            				}
            				else {
            					tmpRest = "[숫자X]";
            					Log.error("▶▶value 실패, 휴식시간이 숫자가 아님");
            				}
            				isRest = false;
            				continue;
            			}
            			
            			// [ * ] : 구간반복
            			if(str.equals("*")) {
            				isLoop = true;
            				continue;
            			}
            			if(isLoop) {
            				if (str.matches("[0-9]+")) {
            					if(prevValue.matches("[0-9]+")) {
            						try {
                    					int __num = Integer.parseInt(prevValue);
                    					for(int i = 0; i < __num; i++) {
                    						tmpReps += DELIMITER_WORKSET + tmpReps;
                    						tmpWeight += DELIMITER_WORKSET + tmpWeight;
                    					}
                    				}
                    				catch (NumberFormatException ex) {
                    					ex.printStackTrace();
                    				}
            					}
            					else if(prevValue.equals(")")) {
            						
            					}
            					else {
            						Log.error("▶▶value 실패, 반복하려는 대상이 없음");
            					}
            				}
            				else {
            					Log.error("▶▶value 실패, 반복 다음 숫자가 아님");
            				}

            				isLoop = false;
            				continue;
            			}
            			
            			
            			// [ - ] : 중량
            			if(str.equals("-")) {
            				if (prevValue.matches("[0-9.가-힣]+")) {
            					tmpWeight = prevValue;
            					Log.info("▶▶value.weight :: " + tmpWeight);
            				}
            				else {
            					tmpWeight = "[숫자X]";
            					Log.error("▶▶value 실패, 중량이 숫자가 아님");
            				}
            				continue;
            			}
            			
            			// [ ( ] : 어센딩/디센딩 시작
            			if(str.equals("(")) {
            				isInBracket = true;
            				tmpBracketWeight = "";
            				tmpBracketReps = "";
            				continue;
            			}
            			
            			// [ ) ] : 어센딩/디센딩 종료
            			if(str.equals(")")) {
            				isInBracket = false;
                			continue;
            			}
            			
            			
            			//****************main
            			tmpReps = "";
            			tmpWeight = "";

            			prevValue = str;
            		}
            		
            		// fin 리스트에 추가
            		FIN_REST.add(tmpRest);

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
            		Log.debug("▶value 종료");
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Log.info("NAME :: " + cntName + " / VALUE :: " + cntValue);

		//JSONObject jsonTempSet = new JSONObject();
		//JSONObject jsonTempWork = new JSONObject();
        
        
        JSONObject jsonFinal = new JSONObject();
        
        int works = 5; // 변경
        int sets = 3; // 변경

        JSONArray jsonArrTemp1 = new JSONArray();
        jsonFinal.put("DATE", FIN_DATE);
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
	
	public static JSONObject getJSONBase() {
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
