package com.jskong.com;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class workoutDailyParserMain {
	public static final String CURRENT_YEAR = "2023";
	public static final String REG_DATE8 = "(19|20)([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_DATE4 = "(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_IS_WORKNAME = "([가-힣ㄱ-ㅎ]+)(?![가-힣ㄱ-ㅎ]*-)";
	public static final String REG_WORKNAME = "[ㄱ-ㅎ]+";
	public static final String REG_WORKVALUE = "[가-힣]+|[0-9]+\\.[0-9]+|[0-9]+|\\/|\\*|-|\\)|\\(|d[0-9]|w[0-9]+|d[0-9]+|w|d|f";
	
	//public static final String REG_SIMPLE_KR = "[ㄱ-ㅎ]+";
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		System.out.println(getJsonData("").toJSONString());
	}
	
	/* [ Delimiter ]	[ Description ]
	 * [ - ]			weight
	 * [ / ]			rest
	 * [ * ]			set count
	 * [   ]			set delimiter
	 * [( )]			ascending/descending
	 * [ w ]			warming up
	 * [ wn ]			warming up (n set)
	 * [ d ]			drop set
	 * [ dn ]			drop set (n set)
	 * [ f ]			failure point
	 * 
	 * [ c ]			compound set
	 * [ s ]			super set
	 *  
	 * [ ) ]			pyramid set(not used)
	 * [ ( ]			drop set(not used)
	 * 
	 * 
	 * ㅅㅋㅌ				◀ workName + workOpt
	 * 60-10 10 10/90	◀ workValue
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
        String file_path = System.getProperty("user.dir") + "\\textfile.txt";
        Path path = Paths.get(file_path);
        
        String var_date = "";
        int count_name = 0;
        int count_value = 0;
        Pattern pattern_is_workname = Pattern.compile(REG_IS_WORKNAME);
        
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
            	System.out.println(line);
            	
            	line = line.trim();
            	
            	// 1. 빈 값이 발견될 경우 ▶ PASS
            	if ("".equals(line)) { continue; }
            	
            	// 2. 날짜 데이터 처리
            	if ("".equals(var_date)) {
            		if (line.matches(REG_DATE4)) {
            			var_date = CURRENT_YEAR + line;
            			System.out.println("### SET DATE\n");
            		}
            		else if (line.matches(REG_DATE8)) {
            			var_date = line;
            			System.out.println("### SET DATE\n");
            		}
            		else {
            			System.out.println("### ERROR :: 날짜 형식 에러\n");
            			//break;
            		}
            		continue;
            	}

            	// 3. 운동NAME 데이터 처리
            	if (pattern_is_workname.matcher(line).find()) {
            		// 운동 NAME
            		// 3.1. 운동NAME-이름 ▶ 배열 적재 (meta에서 초성을 통한 매칭)
            		// 3.2. 운동NAME-부위 ▶ 배열 적재 (meta에서 이름과 부위 매칭)
            		// 3.3. 운동NAME-옵션 ▶ 배열 적재 (그대로 서술, * 제거)
            		
            		
            		
            		System.out.println("### NAME\n");
            		count_name++;
            	}
            	// 4. 운동VALUE 데이터 처리
            	else {
            		// 운동 VALUE
            		// 4.0. 운동 NAME보다 운동 VALUE이 먼저 발견될 경우 ▶ PASS
            		if(count_name <= count_value) {
            			System.out.println("### PASS\n");
            			continue;
            		}
            		// 3.4. 운동NAME-휴식 ▶ 배열 적재 (없을 경우 c/s 고려)

            		
            		
            		System.out.println("### VALUE\n");
            		count_value++;
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("### NAME :: " + count_name + " / VALUE :: " + count_value);

		JSONObject jsonTempSet = new JSONObject();
		JSONObject jsonTempWork = new JSONObject();
        JSONArray jsonArrSet = new JSONArray();
        JSONArray jsonArrWork = new JSONArray();
        JSONObject jsonFinal = new JSONObject();
        
        int works = 5;
        int sets = 3;

        jsonFinal.put("DATE", var_date);
        for(int i = 0; i < works; i++) {
        	jsonTempWork.put("WORK_SEQ", i + 1);
        	jsonTempWork.put("WORK_NAME", "운동이름");
        	jsonTempWork.put("WORK_PART", "부위");
        	jsonTempWork.put("WORK_OPT", "옵션");
        	jsonTempWork.put("WORK_REST", "휴식시간");
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
