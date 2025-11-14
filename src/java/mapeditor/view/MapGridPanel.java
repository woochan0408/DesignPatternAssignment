package mapeditor.view;

import mapeditor.controller.MapEditorManager;
import mapeditor.model.EntityType;
import mapeditor.model.MapData;
import mapeditor.observers.MapObserver;
import mapeditor.states.PlacementState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * MapGridPanel - 맵 그리드 에디터 패널
 * 56x63 그리드를 표시하고 사용자 인터랙션 처리
 *
 * Observer Pattern 구현:
 * - MapObserver를 구현하여 맵 데이터 변경 시 자동 렌더링
 * - State Pattern과 연동하여 마우스 이벤트 처리
 */
public class MapGridPanel extends JPanel implements MapObserver {
    public static final int CELL_SIZE = 24; // 더 큰 셀 크기로 시각성 향상
    private static final Color GRID_COLOR = new Color(80, 80, 80, 150);  // 더 부드러운 그리드
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    private MapEditorManager manager;
    private EntityType[][] gridData;
    private Map<EntityType, BufferedImage> entityImages;
    private Point currentMouseGridPosition;

    public MapGridPanel() {
        this.manager = MapEditorManager.getInstance();
        this.gridData = new EntityType[MapData.HEIGHT][MapData.WIDTH];
        this.entityImages = new HashMap<>();
        this.currentMouseGridPosition = null;

        initializePanel();
        loadEntityImages();
        setupEventListeners();

        // Observer 등록
        manager.addObserver(this);

        // 초기 데이터 로드
        updateGridData();
    }

    /**
     * 패널 초기화
     */
    private void initializePanel() {
        setPreferredSize(new Dimension(
            MapData.WIDTH * CELL_SIZE,
            MapData.HEIGHT * CELL_SIZE
        ));
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 255), 3),
            BorderFactory.createLineBorder(new Color(50, 50, 50), 2)
        ));
        setFocusable(true);
    }

    /**
     * 엔티티 이미지 로드 (스프라이트 시트에서 첫 번째 프레임만 추출)
     */
    private void loadEntityImages() {
        String basePath = "src/resources/img/";

        // 팩맨: 512x32 스프라이트 시트에서 첫 32x32만 사용
        loadSpriteImage(EntityType.PACMAN, basePath + "pacman.png", 32, 32, 512, 32);

        // 유령들: 256x32 스프라이트 시트에서 첫 32x32만 사용
        loadSpriteImage(EntityType.BLINKY, basePath + "blinky.png", 32, 32, 256, 32);
        loadSpriteImage(EntityType.PINKY, basePath + "pinky.png", 32, 32, 256, 32);
        loadSpriteImage(EntityType.INKY, basePath + "inky.png", 32, 32, 256, 32);
        loadSpriteImage(EntityType.CLYDE, basePath + "clyde.png", 32, 32, 256, 32);

        // 다른 이미지들은 그대로 로드
        loadImage(EntityType.WALL, basePath + "wall.png");
        loadImage(EntityType.SUPER_PAC_GUM, basePath + "superpacgum.png");
        loadImage(EntityType.PAC_GUM, basePath + "pacgum.png");
    }

    /**
     * 스프라이트 시트에서 특정 프레임 추출
     */
    private void loadSpriteImage(EntityType type, String path, int frameWidth, int frameHeight, int sheetWidth, int sheetHeight) {
        try {
            File file = new File(path);
            if (file.exists()) {
                BufferedImage sheet = ImageIO.read(file);
                // 첫 번째 프레임(맨 왼쪽) 추출
                BufferedImage firstFrame = sheet.getSubimage(0, 0, frameWidth, frameHeight);
                entityImages.put(type, firstFrame);
            }
        } catch (Exception e) {
            System.err.println("스프라이트 이미지 로드 실패: " + path);
        }
    }

    /**
     * 개별 이미지 로드
     */
    private void loadImage(EntityType type, String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                BufferedImage image = ImageIO.read(file);
                entityImages.put(type, image);
            }
        } catch (Exception e) {
            // 이미지 로드 실패 시 기본 렌더링 사용
            System.err.println("이미지 로드 실패: " + path);
        }
    }

    /**
     * 이벤트 리스너 설정
     */
    private void setupEventListeners() {
        // 마우스 클릭 리스너
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                currentMouseGridPosition = null;
                manager.getStateContext().handleMouseExit();
                repaint();
            }
        });

        // 마우스 이동 리스너
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // 드래그를 통한 연속 배치 (선택적 구현)
                // 현재는 클릭 방식만 지원
            }
        });
    }

    /**
     * 마우스 클릭 처리
     */
    private void handleMouseClick(MouseEvent e) {
        Point gridPos = screenToGrid(e.getPoint());
        if (isValidGridPosition(gridPos)) {
            manager.getStateContext().handleMouseClick(
                gridPos.x, gridPos.y, e.getButton()
            );
        }
    }

    /**
     * 마우스 이동 처리
     */
    private void handleMouseMove(MouseEvent e) {
        Point gridPos = screenToGrid(e.getPoint());
        if (isValidGridPosition(gridPos)) {
            currentMouseGridPosition = gridPos;
            manager.getStateContext().handleMouseMove(gridPos.x, gridPos.y);
        } else {
            currentMouseGridPosition = null;
            manager.getStateContext().handleMouseExit();
        }
        repaint();
    }

    /**
     * 화면 좌표를 그리드 좌표로 변환
     */
    private Point screenToGrid(Point screenPos) {
        int gridX = screenPos.x / CELL_SIZE;
        int gridY = screenPos.y / CELL_SIZE;
        return new Point(gridX, gridY);
    }

    /**
     * 그리드 좌표를 화면 좌표로 변환
     */
    private Point gridToScreen(int gridX, int gridY) {
        return new Point(gridX * CELL_SIZE, gridY * CELL_SIZE);
    }

    /**
     * 유효한 그리드 위치인지 확인
     */
    private boolean isValidGridPosition(Point gridPos) {
        return gridPos != null &&
               gridPos.x >= 0 && gridPos.x < MapData.WIDTH &&
               gridPos.y >= 0 && gridPos.y < MapData.HEIGHT;
    }

    /**
     * 그리드 데이터 업데이트
     */
    private void updateGridData() {
        gridData = manager.getMapDataCopy();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // 그리드 그리기
        drawGrid(g2d);

        // 엔티티들 그리기
        drawEntities(g2d);

        // 현재 상태에 따른 추가 렌더링 (미리보기, 하이라이트 등)
        if (currentMouseGridPosition != null) {
            manager.getStateContext().render(
                g2d, CELL_SIZE, CELL_SIZE, currentMouseGridPosition
            );
        }
    }

    /**
     * 그리드 라인 그리기
     */
    private void drawGrid(Graphics2D g) {
        g.setColor(GRID_COLOR);
        g.setStroke(new BasicStroke(0.5f));

        // 세로선
        for (int x = 0; x <= MapData.WIDTH; x++) {
            g.drawLine(x * CELL_SIZE, 0,
                      x * CELL_SIZE, MapData.HEIGHT * CELL_SIZE);
        }

        // 가로선
        for (int y = 0; y <= MapData.HEIGHT; y++) {
            g.drawLine(0, y * CELL_SIZE,
                      MapData.WIDTH * CELL_SIZE, y * CELL_SIZE);
        }

        // 고스트 집 영역 하이라이트 (편집 불가능 영역)
        drawGhostHouseHighlight(g);
    }

    /**
     * 고스트 집 영역 하이라이트
     */
    private void drawGhostHouseHighlight(Graphics2D g) {
        // 편집 불가능한 영역을 약간 어둡게 표시
        g.setColor(new Color(150, 50, 200, 25)); // 약간 보라색 투명 오버레이

        for (int y = 0; y < MapData.HEIGHT; y++) {
            for (int x = 0; x < MapData.WIDTH; x++) {
                if (!manager.getMapData().isEditable(x, y)) {
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // 고스트 집 영역 테두리 강조
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5.0f, 3.0f}, 0.0f));
        g.setColor(new Color(200, 100, 255));

        // 고스트 집 전체 영역 테두리 (하드코딩된 위치 사용)
        int ghostHouseX = 11;
        int ghostHouseY = 14;
        int ghostHouseWidth = 5;
        int ghostHouseHeight = 3;

        g.drawRect(
            ghostHouseX * CELL_SIZE + 2,
            ghostHouseY * CELL_SIZE + 2,
            ghostHouseWidth * CELL_SIZE - 4,
            ghostHouseHeight * CELL_SIZE - 4
        );

        // "GHOST HOUSE" 텍스트
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(new Color(200, 100, 255, 200));
        String text = "GHOST HOUSE";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = ghostHouseX * CELL_SIZE + (ghostHouseWidth * CELL_SIZE - textWidth) / 2;
        int textY = ghostHouseY * CELL_SIZE - 5;
        g.drawString(text, textX, textY);
    }

    /**
     * 엔티티들 그리기
     */
    private void drawEntities(Graphics2D g) {
        for (int y = 0; y < MapData.HEIGHT; y++) {
            for (int x = 0; x < MapData.WIDTH; x++) {
                EntityType entity = gridData[y][x];
                if (entity != null && entity != EntityType.EMPTY) {
                    drawEntity(g, entity, x, y);
                }
            }
        }
    }

    /**
     * 개별 엔티티 그리기
     */
    private void drawEntity(Graphics2D g, EntityType type, int gridX, int gridY) {
        int x = gridX * CELL_SIZE;
        int y = gridY * CELL_SIZE;

        // 이미지가 있으면 이미지로 그리기
        BufferedImage image = entityImages.get(type);
        if (image != null) {
            g.drawImage(image, x, y, CELL_SIZE, CELL_SIZE, null);
        } else {
            // 이미지가 없으면 기본 도형으로 그리기
            drawEntityShape(g, type, x, y);
        }
    }

    /**
     * 엔티티를 기본 도형으로 그리기
     */
    private void drawEntityShape(Graphics2D g, EntityType type, int x, int y) {
        switch (type) {
            case PACMAN:
                g.setColor(Color.YELLOW);
                g.fillOval(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                break;
            case BLINKY:
                g.setColor(Color.RED);
                drawGhostShape(g, x, y);
                break;
            case PINKY:
                g.setColor(Color.PINK);
                drawGhostShape(g, x, y);
                break;
            case INKY:
                g.setColor(Color.CYAN);
                drawGhostShape(g, x, y);
                break;
            case CLYDE:
                g.setColor(Color.ORANGE);
                drawGhostShape(g, x, y);
                break;
            case WALL:
                g.setColor(Color.BLUE);
                g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                break;
            case GHOST_HOUSE_WALL:
                g.setColor(new Color(100, 100, 255));
                g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.WHITE);
                g.drawLine(x, y + CELL_SIZE/2, x + CELL_SIZE, y + CELL_SIZE/2);
                break;
            case SUPER_PAC_GUM:
                g.setColor(Color.WHITE);
                g.fillOval(x + CELL_SIZE/4, y + CELL_SIZE/4,
                          CELL_SIZE/2, CELL_SIZE/2);
                break;
            case PAC_GUM:
                g.setColor(Color.WHITE);
                g.fillOval(x + CELL_SIZE/3, y + CELL_SIZE/3,
                          CELL_SIZE/3, CELL_SIZE/3);
                break;
            default:
                break;
        }
    }

    /**
     * 유령 모양 그리기
     */
    private void drawGhostShape(Graphics2D g, int x, int y) {
        g.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 3);
        g.fillOval(x + 1, y, CELL_SIZE - 2, CELL_SIZE/2);
    }

    // ========== MapObserver 인터페이스 구현 ==========

    @Override
    public void onEntityPlaced(int x, int y, EntityType entityType) {
        gridData[y][x] = entityType;
        repaint();
    }

    @Override
    public void onEntityRemoved(int x, int y) {
        gridData[y][x] = EntityType.EMPTY;
        repaint();
    }

    @Override
    public void onMapReset() {
        updateGridData();
    }

    @Override
    public void onEntityCountChanged(EntityType entityType, int count) {
        // 그리드 패널에서는 특별한 처리 불필요
    }

    @Override
    public void onValidationStateChanged(boolean isValid) {
        // 유효성에 따라 테두리 색상 변경 등 가능
        if (isValid) {
            setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
        } else {
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        }
    }
}