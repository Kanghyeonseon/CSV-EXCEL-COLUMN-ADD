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

public class addColumn {
	public static void main(String[] args) throws Exception {
		String sggNm = args[1];											// 시군구 이름
		String filePath = args[0];										// filePath
		String ext = args[0].substring(args[0].lastIndexOf(".") + 1);	// 확장자
		String columnName = "code";										// 컬럼이름
		
		// 확장자에 따라 실행 함수가 다르다.
		if(ext.equals("csv")) {
			addColumnToCSV(filePath, columnName, getSggCode(sggNm));
			
		} else if(ext.equals("xlsx")) {
			addColumnToExcel(filePath, columnName, getSggCode(sggNm));
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
		System.out.println("readCSVFile ::::::::::::::::::::");

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
				
				System.out.println();
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

			// 자원 반환
			if(bufferedReader != null) bufferedReader.close();
			if(bufferedWriter != null) {
				bufferedWriter.flush();
				bufferedWriter.close();
			}
			
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
		System.out.println("===== CSV파일 읽기시작 =====");
		try {
			FileInputStream in = new FileInputStream(new File(filePath));

			// 엑셀 파일로 Workbook instance를 생성
			XSSFWorkbook workbook = new XSSFWorkbook(in);

			// workbook의 첫번째 sheet를 가저온다.
			XSSFSheet sheet = workbook.getSheetAt(0);

			// 지자체코드 칼럼 존재 시 삭제 할 칼럼 인덱스
			int delColNum = -1;
			
			// 모든 행(row)들을 조회한다.
			for (Row row : sheet) {
				// 각각의 행에 존재하는 모든 열(cell)을 순회한다.
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();

					// cell의 타입에따라 값을 가져온다.
					switch (cell.getCellType()) {
						case NUMERIC:
							// System.out.print((int) cell.getNumericCellValue() + "\t");
							break;
						case STRING:
							// 지자체 코드 칼럼 존재 시 첫 row 셀삭제
							if(cell.getStringCellValue().equals(columnName) && row.getRowNum() == 0) {
								delColNum = cell.getColumnIndex();
								row.removeCell(cell); 
							}
							
							// 지자체 코드 칼럼 존재 시 나머지 row 셀삭제
							if(row.getRowNum() != 0 && cell.getColumnIndex() == delColNum) {
								row.removeCell(cell);
							}
							// System.out.print(cell.getStringCellValue() + "\t");
							break;
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
	private static String getSggCode(String sggNm) throws SQLException {
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
				// 첫번째 값에 시군구 코드가 출력된다.
				// 나머지 값들은 지금은 필요없어서 내버려뒀음
				sggCd = rs.getString(1);
				System.out.println(sggNm + "의 시군구 코드 : " + rs.getString(1));
			}
		
		} catch (SQLException e) {
			throw new SQLException("SQL Exception...");
		
		// 자원 반환
		} finally {
			try {
				if (rs != null) rs.close();
				if (st != null) st.close();
				if (con != null) con.close();
			} catch (SQLException ex) {
				throw new SQLException("SQL Exception...");
			}
		}
		
		return sggCd;
	}
	
}
