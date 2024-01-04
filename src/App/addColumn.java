package App;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STSourceType;

public class addColumn {
	
	/**
	 * @since 2023-01-04
	 * @author 강현선
	 * @param args[0] 파일 경로 및 이름
	 * @param args[1] 시군구이름
	 * 
	 * csv 또는 excel 파일에 시군구코드 컬럼 추가
	 * 
	 */
	public static void main(String[] args) throws Exception {
		String filePath = args[0];										// 파일 경로 및 이름
		String ext = args[0].substring(args[0].lastIndexOf(".") + 1);	// 확장자
		String sggNm = args[1];											// 시군구 이름
		String columnName = "code";										// 컬럼이름
		String sggCd = getSggCode(sggNm);
		
		// 시군구 이름 잘못입력 시 예외처리
		if(sggCd == null || sggCd.equals("")) throw new Exception("시군구 이름을 정확하게 입력해주세요.");
		
		// 확장자에 따라 실행 함수가 다르다.
		if(ext.equals("csv")) {
			addColumnToCSV(filePath, columnName, sggCd);
			
		} else if(ext.equals("xlsx")) {
			addColumnToExcel(filePath, columnName, sggCd);
		}
		
	}

	/**
	 * @since 2024-01-03
	 * @author 강현선
	 * 
	 * @param filePath		csv파일 경로 및 이름
	 * @param columnName	첫 행에 추가 할 칼럼 이름
	 * @param sggCd		나머지 행에 추가 할 지자체 코드
	 * 
	 * CSV 파일에 지자체 코드 칼럼 추가
	 */
	public static void addColumnToCSV(String filePath, String columnName, String sggCd) {
		System.out.println("========== CSV 파일 읽기 시작 ==========");

		List<List<String>> list = new ArrayList<List<String>>();
		BufferedReader bufferedReader = null;
		
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
				// 임시 리스트 생성 후 list에 넣기
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
			
			// 파일 쓰기, UTF-8 인코딩
			FileOutputStream fos = new FileOutputStream(filePath);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			BufferedWriter bufferedWriter = new BufferedWriter(osw);
			
			for(int i=0; i<list.size(); i++) {
				
				// 첫번째 행에는 칼럼이름
				if(i == 0) {
					list.get(i).add(columnName);
					
				// 나머지 행에는 코드 넣기
				} else {
					list.get(i).add(sggCd);
				}
				
				// 한 줄 씩 쓰기
				bufferedWriter.write(String.join(",", list.get(i)));
				bufferedWriter.newLine();
				
			}
			
			// 확인
			System.out.println(list.toString());

			// 자원 반환
			if(bufferedReader != null) bufferedReader.close();
			if(bufferedWriter != null) {
				bufferedWriter.flush();
				bufferedWriter.close();
			}
			
			System.out.println("========== CSV 파일 읽기 종료 ==========");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @since 2024-01-04
	 * @author 강현선
	 * 
	 * @param filePath		xlsx파일 경로 및 이름
	 * @param columnName	첫 행에 추가 할 칼럼 이름
	 * @param sggCd		나머지 행에 추가 할 지자체 코드
	 * 
	 * xlsx 파일에 지자체 코드 칼럼 추가
	 */
	public static void addColumnToExcel(String filePath, String columnName, String sggCd) {
		System.out.println("========== Excel 파일 읽기 시작 ==========");
		try {
			FileInputStream in = new FileInputStream(new File(filePath));

			// 엑셀파일로 Workbook 인스턴스 생성
			XSSFWorkbook workbook = new XSSFWorkbook(in);

			// 첫번째 sheet를 가저온다.
			XSSFSheet sheet = workbook.getSheetAt(0);

			// 지자체코드 칼럼 존재 시 삭제 할 컬럼의 인덱스
			int delColNum = -1;
			
			// 모든 행(row)들을 조회한다.
			for (Row row : sheet) {
				
				// 이터레이터 생성 및 순회
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					switch (cell.getCellType()) {
						case NUMERIC:
							System.out.print((int) cell.getNumericCellValue() + "\t");
							break;
						case STRING:
							System.out.print(cell.getStringCellValue() + "\t");
							
							// 지자체 코드 이미 존재시 삭제
							if(row.getRowNum() == 0 && cell.getStringCellValue().equals(columnName)) {
								delColNum = cell.getColumnIndex();
								row.removeCell(cell); 
							} else if(row.getRowNum() != 0 && cell.getColumnIndex() == delColNum) {
								row.removeCell(cell);
							}
							break;
						default: break;
					}
				}
				System.out.println();
				
				// 셀에 지자체 코드 추가
				if(row.getRowNum() == 0) {
					row.createCell(row.getLastCellNum()).setCellValue(columnName);
				} else {
					row.createCell(row.getLastCellNum()).setCellValue(sggCd);
				}
				
			}
			
			// 파일 쓰기
			FileOutputStream out = new FileOutputStream(filePath);
			workbook.write(out);

			// 자원 반환
			if(out != null) out.close();
			if(in != null) in.close();
			if(workbook != null) workbook.close();
			
			System.out.println("========== Excel 파일 읽기 종료 ==========");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * @since 2024-01-04
	 * @author 강현선
	 * @param sggNm 시군구이름
	 * 
	 * 시군구 이름으로 시군구 코드 가져오기
	 * @throws SQLException 
	 */
	private static String getSggCode(String sggNm) throws Exception {
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;

		// 연결 정보
		String url = "jdbc:postgresql://112.220.90.92:5432/gyeonggi";
		String user = "postgres";
		String password = "sp160114!";
		String sql = "SELECT substring(h_code, 1, 4) AS h_code , sido_nm , sgg_nm FROM mart.b_h_code_map"
					+ " WHERE h_code LIKE concat('41', '%', '0000') AND sgg_nm IS NOT NULL AND sgg_nm = ?";
		
		// 시군구 코드 결과
		String sggCd = null;
		
		// 연결
		try {
			con = DriverManager.getConnection(url, user, password);
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, sggNm);
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				// 첫번째 값에 시군구 코드가 출력된다. 나머지 값들은 지금은 필요없어서 내버려뒀음
				sggCd = rs.getString(1);
				System.out.println(sggNm + "의 시군구 코드 : " + rs.getString(1));
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		
		// 자원 반환
		} finally {
			try {
				if (rs != null) rs.close();
				if (st != null) st.close();
				if (con != null) con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return sggCd;
	}
	
}
