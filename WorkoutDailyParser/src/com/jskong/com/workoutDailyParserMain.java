package com.jskong.com;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class workoutDailyParserMain {
	public static final String CURRENT_YEAR = "2023";
	public static final String REG_DATE8 = "(19|20)([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_DATE4 = "(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])";
	public static final String REG_IS_WORKNAME = "([가-힣ㄱ-ㅎ]+)(?![가-힣ㄱ-ㅎ]*-)";
	public static final String REG_WORKTITLE = "[^ㄱ-ㅎ]+";
	public static final String REG_WORKOPT   = "[ㄱ-ㅎ]+";
	public static final String REG_WORKVALUE = "[^wdf0-9.가-힣ㄱ-ㅎ-\\/\\*\\(\\)]+";
	public static final String REG_WORK_WARM_DROP = "w[0-9]*|d[0-9]*";
	
	//public static final String REG_SIMPLE_KR = "[ㄱ-ㅎ]+";
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		System.out.println(getJsonData("").toJSONString());
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
	 * ㅅㅋㅌ 바벨			◀ WORK_TITLE + WORK_OPT = WORK_NAME
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
	 * opt :	["" , ""]
	 * reps :	["100", "w w 10 10 10 10 10;15 15 15 15 15"]
	 * weight :	["3", "0 0 20 26 26 26 26;10 10 10 10 10"]
	 * 
	 * 
	 * < Json Structure >
	 * ├ DATE (DATE)
	 * └ WORK (ARRAY)
	 *   ├ WORK_SEQ (INTEGER)	◀ AUTO SET
	 *   ├ WORK_TITLE (STRING)	◀ CONVERT FROM META
	 *   ├ WORK_PART (STRING)	◀ GET FROM META
	 *   ├ WORK_REST (INTEGER)	◀ GET FROM WORK_VALUE
	 *   ├ WORK_OPT (STRING)	
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
	public static JSONObject getJsonData(String inputData) {
        String file_path = System.getProperty("user.dir") + "\\textfile.txt";
        Path path = Paths.get(file_path);
        
        String work_date = "";
        ArrayList<String> work_title = new ArrayList<String>();
        ArrayList<String> work_part = new ArrayList<String>();
        ArrayList<String> work_rest = new ArrayList<String>();
        ArrayList<String> work_opt = new ArrayList<String>();
        ArrayList<String> work_reps = new ArrayList<String>();
        ArrayList<String> work_weight = new ArrayList<String>();
        
        int count_name = 0;
        int count_value = 0;
        Pattern pattern_is_workname = Pattern.compile(REG_IS_WORKNAME);
        
        try {
            List<String> lines = Files.readAllLines(path);

            for (String line : lines) {
            	// 0. 데이터 전처리
            	line = line.trim(); 						// line 양 옆의 공백 제거
            	line = line.replaceAll("\\s*-\\s*", "-"); 	// '-' 양 옆의 공백 제거
            	if ("".equals(line)) { continue; } 			// 공백 ▶ PASS
            	System.out.println("● INPUT :: " + line);
            	
            	// 1. DATE
            	if ("".equals(work_date)) {
            		System.out.println("▶▶▶ 1. DATE");
            		if (line.matches(REG_DATE8)) {
            			work_date = line;
            			System.out.println("▶▶▶ DATE ... OK\n");
            		}
            		else if (line.matches(REG_DATE4)) {
            			work_date = CURRENT_YEAR + line;
            			System.out.println("▶▶▶ DATE ... OK\n");
            		}
            		else {
            			System.out.println("### ERROR :: 날짜 형식에 맞지 않음.\n");
            			//break;
            		}
            		continue;
            	}

            	// 2. WORK NAME
            	if (pattern_is_workname.matcher(line).find()) {
            		System.out.println("▶▶▶ 2. NAME");
            		
            		String[] __arr_title = line.split(REG_WORKTITLE);
            		String[] __arr_opt = line.split(REG_WORKOPT);
            		String __title = "";
            		String __opt = "";
            		String __part = "";
            		
            		/* title, opt 구분 로직 변경 예정
            		* - ' ' 로 split 후
            		* - 0번째 = title
            		* - 1번째 이상 = opt
            		* - ▶ 0번째가 초성인 경우 ▶ 메타에서 매핑
            		* - ▶▶ 
            		* - ▶ 아닌경우 ▶ 그대로 저장 후 [경고]
            		* 
            		* 
            		*/
            		
            		
            		// 2.1. 추출
            		// 2.1.1. TITLE
            		for (int i=0 ; i<__arr_title.length; i++) {
            			__arr_title[i] = __arr_title[i].trim();
            			if("".equals(__arr_title[i])) { continue; }
            			__title = __arr_title[i];
            			break;
            		}
            		System.out.println("[1] TITLE :: " + __title);
            		
            		// 2.1.2. OPT
            		for (int i=0 ; i<__arr_opt.length; i++) {
            			__arr_opt[i] = __arr_opt[i].trim();
            			if("".equals(__arr_opt[i])) { continue; }
            			__opt += __arr_opt[i];
            		}
            		__opt = __opt.replace("*", "");
            		System.out.println("[2] OPT   :: " + __opt);
            		
            		// 2.1.3. PART
            		System.out.println("[3] PART  :: " + __part);
            		__part = "임시";
            		
            		
            		
            		// 2.2. 리스트 적재 - C/S인 경우 ▶ ';' 구분자 추가
            		// 2.2.1. TITLE
            		if(count_name > count_value) {
            			int __last_index = work_title.size() - 1;
            			String  __last_title = work_title.get(__last_index);
            			work_title.set(__last_index, __last_title + ";" + __title);
            		}
            		else {
            			work_title.add(__title);
            		}
            		
            		// 2.2.2. OPT
            		work_opt.add(__opt);
            		if(count_name > count_value) {
            			int __last_index = work_opt.size() - 1;
            			String  __last_opt = work_opt.get(__last_index);
            			work_opt.set(__last_index, __last_opt + ";" + __title);
            		}
            		else {
            			work_opt.add(__title);
            		}
            		
            		// 2.2.3. PART
            		// ...blabla...
            		work_part.add(__part);
            		
            		count_name++;
            		System.out.println("");
            	}
            	// 4. WORK VALUE 처리
            	else {
            		// 4.0. NAME보다 VALUE가 먼저 발견될 경우 ▶ PASS
            		if(count_name <= count_value) {
            			System.out.println("### ERROR :: NAME 형식에 맞지 않음.\n");
            			continue;
            		}
            		System.out.println("▶▶▶ 3 VALUE");
            		
            		line = line.replace("-", " - ");
            		line = line.replace("/", " / ");
            		line = line.replace("*", " * ");
            		line = line.replace("(", " ( ");
            		line = line.replace(")", " ) ");
            		
            		String[] __arr_value = line.split(REG_WORKVALUE);
            		
            		String __rest = "";
            		String __reps = "";
            		String __weight = "";
            		
            		for (int i=0 ; i<__arr_value.length; i++) {
            			// w & d 처리
            			if(__arr_value[i].matches(REG_WORK_WARM_DROP)) {
            				String __first = __arr_value[i].substring(0, 1);
            				String __last  = __arr_value[i].substring(1);
            				if("".equals("")) { __last = "1"; }
            				try {
            					int __val = Integer.parseInt(__last);
            					for(int j=0; j<__val; j++) {
            						__reps += __first + " ";
            						__weight += __first + " ";
            					}
            				}
            				catch (NumberFormatException ex) {
            					ex.printStackTrace();
            				}
            				
            			}
            			
            			
            			System.out.println("VALUE(" + Integer.toString(i) + ") :: " + __arr_value[i]); 
            		}

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
            		
            		count_value++;
            		System.out.println("");
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
        
        int works = 5; // 변경
        int sets = 3; // 변경

        jsonFinal.put("DATE", work_date);
        for(int i = 0; i < works; i++) {
        	jsonTempWork.put("WORK_SEQ", i + 1);
        	jsonTempWork.put("WORK_TITLE", "운동이름");
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
