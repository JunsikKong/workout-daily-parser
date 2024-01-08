package com.jskong.com;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WorkoutMetaEditor {
	public static final List<String> BASE_PART = new ArrayList<>(Arrays.asList("하체", "등", "가슴", "어깨", "이두", "삼두", "코어", "복근"));
	
	public static void main(String[] args) {
		String csvfile_path = System.getProperty("user.dir") + "\\csvfile.csv";
		String jsonfile_path = System.getProperty("user.dir") + "\\jsonfile.json";
		
		JSONObject jsonFinal = csvToJson(readCSV(csvfile_path));
		writeJSON(jsonFinal, jsonfile_path);
		
	}
	
	public static List<List<String>> readCSV(String file_path) {
		List<List<String>> csvList = new ArrayList<List<String>>();
		File csv = new File(file_path);
		BufferedReader br = null;
		String line = "";
		
		try {
			br = new BufferedReader(new FileReader(csv));
			br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> aLine = new ArrayList<String>();
				String[] lineArr = line.split(",");
				
				System.out.print("입력 : " + Arrays.toString(lineArr));
				if(BASE_PART.contains(lineArr[0])) {
					aLine = Arrays.asList(lineArr);
					csvList.add(aLine);
					System.out.println(" ... 성공");
				}
				else {
					System.out.println(" ... 실패");
					continue;
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(br != null) {
					br.close();
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		return csvList;
	}

	public static JSONObject csvToJson(List<List<String>> csvList) {
		JSONObject jsonFinal = new JSONObject();
		List<JSONArray> jsonArrList = new ArrayList<JSONArray>();
		
		for(String base : BASE_PART) {
			jsonArrList.add(new JSONArray());
			jsonFinal.put(base, jsonArrList.get(jsonArrList.size() - 1));
		}
		
		for(List<String> ls : csvList) {
			System.out.println(" ... 출력 : " + ls.get(0));
			int __idx = BASE_PART.indexOf(ls.get(0));
			JSONObject jsonTemp = new JSONObject();
			JSONArray jsonArrTemp = new JSONArray();
			
			
			jsonTemp.put(ls.get(1), ls.get(2));
			
			jsonArrTemp = jsonArrList.get(__idx);

			jsonArrTemp.add(jsonTemp);
			
		}
		
		for(int i = 0; i < BASE_PART.size(); i ++) {
			jsonFinal.put(BASE_PART.get(i), jsonArrList.get(i));
		}
		
		System.out.println(jsonFinal.toJSONString());
		
		return jsonFinal;
	}
	
	public static boolean writeJSON(JSONObject jsonFinal, String file_path) {
		try {
			FileWriter file = new FileWriter(file_path);
			file.write(jsonFinal.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
