package com.jskong.com;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class workoutDailyParserMain {
	public static final String CURRENT_YEAR = "2023";
	public static final String REG_DATE8 = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$";
	public static final String REG_DATE4 = "^(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$";
	public static final String REG_SIMPLE_KR = "[ㄱ-ㅎ]+";
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		System.out.println(getJsonData("2").toJSONString());
	}
	
	/* [ Delimiter ]	[ Description ]
	 * [ - ]			weight
	 * [ / ]			rest
	 * [ * ]			set count
	 * [   ]			set delimiter
	 * [( )]			ascending/descending
	 * [ w ]			warming up
	 * [ p ]			pyramid set
	 * [ d ]			drop set
	 * [ c ]			compound set
	 * [ s ]			super set
	 * [ f ]			failure point
	 * 
	 * [ ) ]			pyramid set(not used)
	 * [ ( ]			drop set(not used)
	 * 
	 * 
	 * 
	 * < Json Structure >
	 * ├ DATE (DATE)
	 * └ WORK (ARRAY)
	 *   ├ WORK_SEQ (INTEGER)
	 *   ├ WORK_NAME (STRING)
	 *   ├ WORK_PART (STRING)
	 *   ├ WORK_REST (INTEGER)
	 *   ├ WORK_OPT (STRING)
	 *   └ WORK_SET (ARRAY)
	 *     ├ SET_SEQ (INTEGER)
	 *     ├ SET_WEIGHT (STRING)
	 *     └ SET_REPS (INTEGER)
	 * 
	 * 
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
	public static JSONObject getJsonData(String inputData) {
        String filePath = System.getProperty("user.dir") + "\\textfile.txt";
        Path path = Paths.get(filePath);
        
        String strDate = ""; 

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
            	System.out.println(line);
            	
            	line = line.trim();
            	
            	// 1. 빈값 pass
            	if ("".equals(line)) { continue; }
            	
            	// 2. 날짜 처리 후 pass
            	if ("".equals(strDate)) { // 날짜 설정
            		if (line.matches(REG_DATE4)) {
            			strDate = CURRENT_YEAR + line;
            		}
            		else if (line.matches(REG_DATE8)) {
            			strDate = line;
            		}
            		else {
            			System.out.println("### ERROR :: 날짜 형식 에러");
            			break;
            		}
            		continue;
            	}
            	
            	// 3. 운동데이터 처리 후 pass
            	if (line.matches(REG_SIMPLE_KR)) {
            		
            	}
            	
            	
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

		
		JSONObject jsonTempSet = new JSONObject();
		JSONObject jsonTempWork = new JSONObject();
        JSONArray jsonArrSet = new JSONArray();
        JSONArray jsonArrWork = new JSONArray();
        JSONObject jsonFinal = new JSONObject();
        
        int works = 5;
        int sets = 3;

        jsonFinal.put("DATE", strDate);
        for(int i = 0; i < works; i++) {
        	jsonTempWork.put("WORK_SEQ", i + 1);
        	jsonTempWork.put("WORK_NAME", "운동");
        	jsonTempWork.put("WORK_PART", "부위");
        	jsonTempWork.put("WORK_REST", "휴식시간");
        	jsonTempWork.put("WORK_OPT", "옵션");
        	for(int j = 0; j < sets; j++) {
            	jsonTempSet.put("SET_SEQ", j + 1);
            	jsonTempSet.put("SET_WEIGHT", "무게");
            	jsonTempSet.put("SET_REPS", "횟수");
            	jsonArrSet.add(jsonTempSet);
                jsonTempSet = new JSONObject();
            }
        	jsonTempWork.put("WORK_SET", jsonArrSet);
        	jsonArrSet = new JSONArray();
        	
        	jsonArrWork.add(jsonTempWork);
            jsonTempWork = new JSONObject();
        }
        jsonFinal.put("WORK", jsonArrWork);
        jsonArrWork = new JSONArray();
        
        
        
        return jsonFinal;
	}
	
	public static String getViewData(String inputData) {
		
		return "";
	}
}
