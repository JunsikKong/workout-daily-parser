package com.jskong.com;

public class workoutDailyParserMain {

	public static void main(String[] args) {
		System.out.println("hi");
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
	 * [ Json Structure]
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
	 * 예외상황
	 * */
	public static String parseWorkoutJson(String inputData) {

		return "";
	}
	
	public static String parseWorkoutData(String inputData) {

		return "";
	}

}
