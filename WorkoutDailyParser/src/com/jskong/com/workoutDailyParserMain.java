package com.jskong.com;

public class workoutDailyParserMain {

	public static void main(String[] args) {
		System.out.println("workout");
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
	 * ├ date (DATE)
	 * └ work (ARRAY)
	 *   ├ work_index (INTEGER)
	 *   ├ work_name (STRING)
	 *   ├ work_part (STRING)
	 *   ├ work_rest (INTEGER)
	 *   ├ work_opt (STRING)
	 *   └ work_set (ARRAY)
	 *     ├ set_index (INTEGER)
	 *     ├ set_weight (INTEGER)
	 *     └ set_reps (INTEGER)
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
	 * - work_index, set_index는 1부터 시작, 1씩 증가
	 * - 
	 * - 
	 * 
	 * */
	public static String getJsonData(String inputData) {
		
		return "";
	}
	
	public static String getViewData(String inputData) {
		
		return "";
	}

}
