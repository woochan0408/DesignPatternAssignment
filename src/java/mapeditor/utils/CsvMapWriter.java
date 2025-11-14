package mapeditor.utils;

import mapeditor.model.EntityType;
import mapeditor.model.MapData;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * CsvMapWriter - CSV 파일 저장 유틸리티
 * 맵 데이터를 CSV 형식으로 저장하고 벽 배경 이미지 생성
 *
 * 기존 CsvReader와 호환되는 형식으로 저장
 * 구분자: 세미콜론 (;)
 */
public class CsvMapWriter {

    private static final String LEVEL_FOLDER = "src/resources/level/";
    private static final String IMG_FOLDER = "src/resources/img/";
    private static final String FILE_PREFIX = "custom_map_";
    private static final String FILE_EXTENSION = ".csv";
    private static final String IMG_EXTENSION = ".png";

    // 벽 렌더링 색상
    private static final Color WALL_COLOR = new Color(0, 0, 139); // 남색
    private static final Color WALL_BORDER = Color.BLACK;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final int CELL_SIZE = 8; // 픽셀 단위

    /**
     * 맵 데이터를 CSV 파일과 배경 이미지로 저장
     * @param mapData 저장할 맵 데이터 (논리적 28×31 그리드)
     * @param filePath 저장 경로 (null이면 자동 생성)
     * @return 저장된 파일 경로
     * @throws IOException 파일 저장 실패
     */
    public static String saveMap(EntityType[][] mapData, String filePath) throws IOException {
        if (filePath == null) {
            filePath = generateFilePath();
        }

        // CSV 파일 저장
        File csvFile = new File(filePath);
        File parentDir = csvFile.getParentFile();

        // 디렉토리가 없으면 생성
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 논리적 그리드를 CSV 크기로 확장 (56×62)
        EntityType[][] expandedData = expandMapData(mapData);

        // CSV 파일 작성
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writeMapData(writer, expandedData);
        }

        // 배경 이미지 생성 및 저장
        String imagePath = generateImagePath(filePath);
        saveBackgroundImage(expandedData, imagePath);

        return filePath;
    }

    /**
     * 논리적 그리드(28×31)를 CSV 그리드(56×62)로 확장
     */
    private static EntityType[][] expandMapData(EntityType[][] logicalGrid) {
        int logicalHeight = logicalGrid.length;
        int logicalWidth = logicalHeight > 0 ? logicalGrid[0].length : 0;

        EntityType[][] expanded = new EntityType[MapData.CSV_HEIGHT][MapData.CSV_WIDTH];

        // 전체를 EMPTY로 초기화
        for (int y = 0; y < MapData.CSV_HEIGHT; y++) {
            for (int x = 0; x < MapData.CSV_WIDTH; x++) {
                expanded[y][x] = EntityType.EMPTY;
            }
        }

        // 각 논리적 칸을 2×2로 확장
        for (int logicalY = 0; logicalY < logicalHeight && logicalY < MapData.HEIGHT; logicalY++) {
            for (int logicalX = 0; logicalX < logicalWidth && logicalX < MapData.WIDTH; logicalX++) {
                EntityType entity = logicalGrid[logicalY][logicalX];

                int csvX = logicalX * 2;
                int csvY = logicalY * 2;

                if (csvY < MapData.CSV_HEIGHT && csvX < MapData.CSV_WIDTH) {
                    // 벽은 2×2 전체를 채움
                    if (entity == EntityType.WALL || entity == EntityType.GHOST_HOUSE_WALL) {
                        for (int dy = 0; dy < 2 && csvY + dy < MapData.CSV_HEIGHT; dy++) {
                            for (int dx = 0; dx < 2 && csvX + dx < MapData.CSV_WIDTH; dx++) {
                                expanded[csvY + dy][csvX + dx] = entity;
                            }
                        }
                    }
                    // 다른 엔티티는 좌측 상단에만 배치
                    else if (entity != EntityType.EMPTY) {
                        expanded[csvY][csvX] = entity;
                    }
                }
            }
        }

        return expanded;
    }

    /**
     * 배경 이미지 생성 및 저장
     */
    private static void saveBackgroundImage(EntityType[][] mapData, String imagePath) throws IOException {
        int width = MapData.CSV_WIDTH * CELL_SIZE;
        int height = MapData.CSV_HEIGHT * CELL_SIZE;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 안티앨리어싱 설정
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경을 검은색으로 채움
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);

        // 벽 그리기
        for (int y = 0; y < MapData.CSV_HEIGHT; y++) {
            for (int x = 0; x < MapData.CSV_WIDTH; x++) {
                EntityType entity = mapData[y][x];

                if (entity == EntityType.WALL) {
                    drawWall(g, x, y, mapData);
                } else if (entity == EntityType.GHOST_HOUSE_WALL) {
                    drawGhostHouseWall(g, x, y);
                }
            }
        }

        g.dispose();

        // 이미지 파일로 저장
        File imgFile = new File(imagePath);
        File imgDir = imgFile.getParentFile();
        if (imgDir != null && !imgDir.exists()) {
            imgDir.mkdirs();
        }

        ImageIO.write(image, "PNG", imgFile);
    }

    /**
     * 일반 벽 그리기
     */
    private static void drawWall(Graphics2D g, int gridX, int gridY, EntityType[][] mapData) {
        int x = gridX * CELL_SIZE;
        int y = gridY * CELL_SIZE;

        // 벽 채우기
        g.setColor(WALL_COLOR);
        g.fillRect(x, y, CELL_SIZE, CELL_SIZE);

        // 인접한 벽 확인
        boolean hasTop = gridY > 0 && mapData[gridY - 1][gridX] == EntityType.WALL;
        boolean hasBottom = gridY < MapData.CSV_HEIGHT - 1 && mapData[gridY + 1][gridX] == EntityType.WALL;
        boolean hasLeft = gridX > 0 && mapData[gridY][gridX - 1] == EntityType.WALL;
        boolean hasRight = gridX < MapData.CSV_WIDTH - 1 && mapData[gridY][gridX + 1] == EntityType.WALL;

        // 테두리 그리기 (인접한 벽이 없는 면만)
        g.setColor(WALL_BORDER);
        g.setStroke(new BasicStroke(1));

        if (!hasTop) {
            g.drawLine(x, y, x + CELL_SIZE, y);
        }
        if (!hasBottom) {
            g.drawLine(x, y + CELL_SIZE, x + CELL_SIZE, y + CELL_SIZE);
        }
        if (!hasLeft) {
            g.drawLine(x, y, x, y + CELL_SIZE);
        }
        if (!hasRight) {
            g.drawLine(x + CELL_SIZE, y, x + CELL_SIZE, y + CELL_SIZE);
        }
    }

    /**
     * 유령 집 벽 그리기 (특별한 표시)
     */
    private static void drawGhostHouseWall(Graphics2D g, int gridX, int gridY) {
        int x = gridX * CELL_SIZE;
        int y = gridY * CELL_SIZE;

        // 더 밝은 파란색으로 채우기
        g.setColor(new Color(100, 100, 255));
        g.fillRect(x, y, CELL_SIZE, CELL_SIZE);

        // 점선 테두리 또는 특별한 표시
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f}, 0.0f));
        g.drawRect(x, y, CELL_SIZE, CELL_SIZE);
    }

    /**
     * 이미지 파일 경로 생성
     */
    private static String generateImagePath(String csvPath) {
        // CSV 경로에서 이미지 경로 생성
        String fileName = new File(csvPath).getName();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return IMG_FOLDER + nameWithoutExt + "_bg" + IMG_EXTENSION;
    }

    /**
     * 자동으로 파일 경로 생성
     * @return 생성된 파일 경로
     */
    private static String generateFilePath() {
        File levelFolder = new File(LEVEL_FOLDER);
        if (!levelFolder.exists()) {
            levelFolder.mkdirs();
        }

        // 사용되지 않은 번호 찾기
        int fileNumber = 1;
        File file;
        do {
            String fileName = String.format("%s%03d%s", FILE_PREFIX, fileNumber, FILE_EXTENSION);
            file = new File(levelFolder, fileName);
            fileNumber++;
        } while (file.exists() && fileNumber < 1000);

        return file.getAbsolutePath();
    }

    /**
     * 맵 데이터를 BufferedWriter에 작성
     */
    private static void writeMapData(BufferedWriter writer, EntityType[][] mapData) throws IOException {
        int height = mapData.length;
        int width = height > 0 ? mapData[0].length : 0;

        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < width; x++) {
                EntityType entity = mapData[y][x];
                char symbol = entity != null ? entity.getSymbol() : ' ';

                // 심볼 추가
                line.append(symbol);

                // 마지막 열이 아니면 구분자 추가
                if (x < width - 1) {
                    line.append(';');
                }
            }

            // 라인 작성
            writer.write(line.toString());

            // 마지막 행이 아니면 줄바꿈 추가
            if (y < height - 1) {
                writer.newLine();
            }
        }
    }

    /**
     * 맵 데이터 검증 (저장 전 확인용)
     * @param mapData 검증할 맵 데이터
     * @return 검증 통과 여부
     */
    public static boolean validateBeforeSave(EntityType[][] mapData) {
        if (mapData == null || mapData.length == 0) {
            return false;
        }

        // 필수 엔티티 개수 확인
        int pacmanCount = 0;
        int blinkyCount = 0;
        int pinkyCount = 0;
        int inkyCount = 0;
        int clydeCount = 0;

        for (EntityType[] row : mapData) {
            for (EntityType entity : row) {
                if (entity == null) continue;

                switch (entity) {
                    case PACMAN:
                        pacmanCount++;
                        break;
                    case BLINKY:
                        blinkyCount++;
                        break;
                    case PINKY:
                        pinkyCount++;
                        break;
                    case INKY:
                        inkyCount++;
                        break;
                    case CLYDE:
                        clydeCount++;
                        break;
                    default:
                        break;
                }
            }
        }

        // 각 필수 엔티티가 정확히 1개씩 있는지 확인
        return pacmanCount == 1 && blinkyCount == 1 && pinkyCount == 1 &&
               inkyCount == 1 && clydeCount == 1;
    }

    /**
     * 파일 이름에서 맵 번호 추출
     * @param fileName 파일 이름
     * @return 맵 번호 (추출 실패 시 -1)
     */
    public static int extractMapNumber(String fileName) {
        if (fileName == null || !fileName.startsWith(FILE_PREFIX)) {
            return -1;
        }

        String numberPart = fileName.replace(FILE_PREFIX, "").replace(FILE_EXTENSION, "");
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}