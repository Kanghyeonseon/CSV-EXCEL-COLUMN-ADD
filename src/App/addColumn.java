package App;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class addColumn {
	public static void main(String[] args) {
		addColumnToCSV(args[0], "지자체코드", "cd_000005");
	}

	/**
	 * @since 2024-01-03
	 * @author 강현선
	 * 
	 * @param filePath		csv파일 경로 및 이름
	 * @param columnName	첫 행에 추가 할 칼럼 이름
	 * @param admiCd		나머지 행에 추가 할 지자체 코드
	 * 
	 * CSV 파일에 지자체 코드 칼럼 추가
	 */
	public static void addColumnToCSV(String filePath, String columnName, String admiCd) {
		System.out.println("readCSVFile ::::::::::::::::::::");

		List<List<String>> list = new ArrayList<List<String>>();
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		
		// 지자체 코드 칼럼 인덱스
		int cdIndex = -1;

		try {
			// 파일 읽기
			bufferedReader = Files.newBufferedReader(Paths.get(filePath));
			String line = "";

			while ((line = bufferedReader.readLine()) != null) {
				// 한 줄 결과(최종)
				List<String> stringList = new ArrayList<>();
				
				// 한 줄 결과(임시)
				String stringArray[] = line.split(",");
				
				// Arrays.asList로 변환한 List에 바로 add 메서드를 쓸 수 없어서
				//임시 리스트 생성 후 list에 넣기
				List<String> temp = Arrays.asList(stringArray);
				stringList.addAll(temp);
				
				// 지자체코드 칼럼 존재시 칼럼 삭제
				if(stringList.contains(columnName)) {
					cdIndex = stringList.indexOf(columnName);
				}
				if(cdIndex != -1) {
					stringList.remove(cdIndex);
				}
				
				// 최종 결과 리스트에 추가
				list.add(stringList);
			}
			
			// 파일 쓰기
			bufferedWriter = new BufferedWriter(new FileWriter(filePath));
			for(int i=0; i<list.size(); i++) {
				
				// 첫번째 행에는 칼럼이름
				if(i == 0) {
					list.get(i).add(columnName);
					
				// 나머지 행에는 코드 넣기
				} else {
					list.get(i).add(admiCd);
				}
				
				// 한 줄 씩 쓰기
				bufferedWriter.write(String.join(",", list.get(i)));
				bufferedWriter.newLine();
			}

			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				// 객체 닫기
				assert bufferedReader != null;
				bufferedReader.close();
				
				assert bufferedWriter != null;
				bufferedWriter.flush();
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
