package com.jskong.com;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WorkoutMetaEditor {
	public static final String[] META_PART = {"하체", "등", "가슴", "어깨", "이두", "삼두", "코어", "복근"};
	
	public static void main(String[] args) {
		JSONObject obj = new JSONObject();
		obj.put("name", "mine-it-record");
		obj.put("mine", "GN");
		obj.put("year", 2021);

		try {
			FileWriter file = new FileWriter("c:/mine_data/mine.json");
			file.write(obj.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		//JSONObject jsonTempSet = new JSONObject();
		//JSONObject jsonTempWork = new JSONObject();
        JSONArray jsonArrSet = new JSONArray();
        JSONArray jsonArrWork = new JSONArray();
        JSONObject jsonFinal = new JSONObject();
        
        int works = 5; // 변경
        int sets = 3; // 변경

        
        
        
        for(String part : META_PART) {
        }
        jsonFinal.put("DATE", work_date);
        
        
        for(int i = 0; i < works; i++) {
        	JSONObject jsonTempWork = new JSONObject();
        	jsonTempWork.put("WORK_SEQ", i + 1);
        	jsonTempWork.put("WORK_TITLE", "운동이름");
        	jsonTempWork.put("WORK_PART", "부위");
        	jsonTempWork.put("WORK_OPT", "옵션");
        	jsonTempWork.put("WORK_REST", "휴식시간");
        	for(int j = 0; j < sets; j++) {
        		JSONObject jsonTempSet = new JSONObject();
            	jsonTempSet.put("SET_SEQ", j + 1);
            	jsonTempSet.put("SET_WEIGHT", "무게");
            	jsonTempSet.put("SET_REPS", "횟수");
            	jsonArrSet.add(jsonTempSet);
            }
        	jsonTempWork.put("WORK_SET", jsonArrSet);
        	jsonArrSet = new JSONArray();
        	
        	jsonArrWork.add(jsonTempWork);
        }
        jsonFinal.put("WORK", jsonArrWork);
        jsonArrWork = new JSONArray();
        
        
		System.out.print(obj);
	}

}
