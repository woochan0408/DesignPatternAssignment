package mapeditor.states;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import mapeditor.model.EntityType;
import mapeditor.model.MapData;

/**
 * PlacementState - 엔티티 배치 상태
 * 선택된 엔티티를 맵에 배치하는 상태
 *
 * State Pattern 구현체:
 * - 마우스를 따라다니는 반투명 미리보기 제공
 * - 배치 가능 여부에 따라 초록/빨강 하이라이트
 * - 클릭 시 실제 배치 수행
 */
public class PlacementState implements EditorState {
    private EditorStateContext context;
    private MapData mapData;
    private EntityType selectedEntityType;
    private Point currentGridPosition;
    private boolean canPlaceAtCurrentPosition;

    // 색상 상수
    private static final Color CAN_PLACE_COLOR = new Color(0, 255, 0, 100);
    private static final Color CANNOT_PLACE_COLOR = new Color(255, 0, 0, 100);
    private static final float PREVIEW_ALPHA = 0.5f;

    public PlacementState(EditorStateContext context, MapData mapData) {
        this.context = context;
        this.mapData = mapData;
    }

    public void setEntityType(EntityType entityType) {
        this.selectedEntityType = entityType;
    }

    @Override
    public void handleMouseClick(int gridX, int gridY, int button) {
        // selectedEntityType이 null인 경우 early return
        if (selectedEntityType == null) {
            context.setIdleState();
            return;
        }

        if (button == MouseEvent.BUTTON1) { // 좌클릭
            if (canPlaceAtCurrentPosition) {
                // Command Pattern을 위한 준비
                // 현재는 직접 배치, 나중에 Command로 래핑 예정
                boolean placed = mapData.placeEntity(gridX, gridY, selectedEntityType);

                if (placed && selectedEntityType.isRequired() &&
                    mapData.getEntityCount(selectedEntityType) >= selectedEntityType.getMaxCount()) {
                    // 필수 엔티티가 최대 개수에 도달하면 Idle 상태로 전환
                    context.setIdleState();
                }
            }
        } else if (button == MouseEvent.BUTTON3) { // 우클릭
            // 우클릭으로 배치 모드 취소
            context.setIdleState();
        }
    }

    @Override
    public void handleMouseMove(int gridX, int gridY) {
        currentGridPosition = new Point(gridX, gridY);

        // selectedEntityType이 null인 경우 early return
        if (selectedEntityType == null) {
            canPlaceAtCurrentPosition = false;
            return;
        }

        // 편집 불가능한 영역 체크 (고스트 집)
        if (!mapData.isEditable(gridX, gridY)) {
            canPlaceAtCurrentPosition = false;
            return;
        }

        // 현재 위치에 배치 가능한지 확인
        EntityType currentEntity = mapData.getEntityAt(gridX, gridY);
        canPlaceAtCurrentPosition = (currentEntity == EntityType.EMPTY);

        // 필수 엔티티인 경우 개수 제한 확인
        if (selectedEntityType.isRequired() && selectedEntityType.getMaxCount() > 0) {
            int currentCount = mapData.getEntityCount(selectedEntityType);
            if (currentCount >= selectedEntityType.getMaxCount()) {
                // 이미 다른 위치에 배치된 경우, 덮어쓰기는 가능
                EntityType existingEntity = mapData.getEntityAt(gridX, gridY);
                canPlaceAtCurrentPosition = (existingEntity == selectedEntityType);
            }
        }
    }

    @Override
    public void handleMouseExit() {
        currentGridPosition = null;
        canPlaceAtCurrentPosition = false;
    }

    @Override
    public void render(Graphics2D g, int cellWidth, int cellHeight, Point mousePosition) {
        if (currentGridPosition == null || selectedEntityType == null) return;

        int x = currentGridPosition.x * cellWidth;
        int y = currentGridPosition.y * cellHeight;

        // 배치 가능 여부에 따른 색상 하이라이트
        g.setColor(canPlaceAtCurrentPosition ? CAN_PLACE_COLOR : CANNOT_PLACE_COLOR);
        g.fillRect(x, y, cellWidth, cellHeight);

        // 반투명 엔티티 미리보기 렌더링
        renderEntityPreview(g, x, y, cellWidth, cellHeight);
    }

    /**
     * 엔티티 미리보기 렌더링
     */
    private void renderEntityPreview(Graphics2D g, int x, int y, int width, int height) {
        // 알파 컴포지트 설정 (반투명)
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PREVIEW_ALPHA));

        // 엔티티 타입에 따른 렌더링
        renderEntity(g, selectedEntityType, x, y, width, height);

        // 원래 컴포지트 복원
        g.setComposite(originalComposite);
    }

    /**
     * 엔티티 렌더링 (실제 이미지 또는 심볼)
     */
    private void renderEntity(Graphics2D g, EntityType type, int x, int y, int width, int height) {
        // 이미지 파일이 있는지 확인하고 렌더링
        String imagePath = getImagePathForEntity(type);
        File imageFile = new File(imagePath);

        if (imageFile.exists()) {
            try {
                BufferedImage image = ImageIO.read(imageFile);

                // 스프라이트 시트인 경우 첫 번째 프레임만 추출
                BufferedImage frameToUse;
                if (type == EntityType.PACMAN && image.getWidth() == 512 && image.getHeight() == 32) {
                    // Pacman 스프라이트 시트: 512x32에서 첫 32x32 추출
                    frameToUse = image.getSubimage(0, 0, 32, 32);
                } else if ((type == EntityType.BLINKY || type == EntityType.PINKY ||
                           type == EntityType.INKY || type == EntityType.CLYDE) &&
                          image.getWidth() == 256 && image.getHeight() == 32) {
                    // Ghost 스프라이트 시트: 256x32에서 첫 32x32 추출
                    frameToUse = image.getSubimage(0, 0, 32, 32);
                } else {
                    // 일반 이미지
                    frameToUse = image;
                }

                g.drawImage(frameToUse, x, y, width, height, null);
            } catch (Exception e) {
                // 이미지 로드 실패 시 심볼로 대체
                drawEntitySymbol(g, type, x, y, width, height);
            }
        } else {
            // 이미지가 없으면 심볼로 렌더링
            drawEntitySymbol(g, type, x, y, width, height);
        }
    }

    /**
     * 엔티티 심볼 그리기
     */
    private void drawEntitySymbol(Graphics2D g, EntityType type, int x, int y, int width, int height) {
        // 배경색 설정
        switch (type) {
            case PACMAN:
                g.setColor(Color.YELLOW);
                g.fillOval(x + 2, y + 2, width - 4, height - 4);
                break;
            case BLINKY:
                g.setColor(Color.RED);
                g.fillRect(x + 2, y + 2, width - 4, height - 4);
                break;
            case PINKY:
                g.setColor(Color.PINK);
                g.fillRect(x + 2, y + 2, width - 4, height - 4);
                break;
            case INKY:
                g.setColor(Color.CYAN);
                g.fillRect(x + 2, y + 2, width - 4, height - 4);
                break;
            case CLYDE:
                g.setColor(Color.ORANGE);
                g.fillRect(x + 2, y + 2, width - 4, height - 4);
                break;
            case WALL:
                g.setColor(Color.BLUE);
                g.fillRect(x, y, width, height);
                break;
            case GHOST_HOUSE_WALL:
                g.setColor(new Color(100, 100, 255));
                g.fillRect(x, y, width, height);
                break;
            case SUPER_PAC_GUM:
                g.setColor(Color.WHITE);
                g.fillOval(x + width/4, y + height/4, width/2, height/2);
                break;
            case PAC_GUM:
                g.setColor(Color.WHITE);
                g.fillOval(x + width/3, y + height/3, width/3, height/3);
                break;
            default:
                break;
        }

        // 심볼 텍스트 그리기
        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        String symbol = String.valueOf(type.getSymbol());
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(symbol)) / 2;
        int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(symbol, textX, textY);
    }

    /**
     * 엔티티 타입에 따른 이미지 경로 반환
     */
    private String getImagePathForEntity(EntityType type) {
        String basePath = "src/resources/img/";
        switch (type) {
            case PACMAN:
                return basePath + "pacman.png";
            case BLINKY:
                return basePath + "blinky.png";
            case PINKY:
                return basePath + "pinky.png";
            case INKY:
                return basePath + "inky.png";
            case CLYDE:
                return basePath + "clyde.png";
            case WALL:
                return basePath + "wall.png";
            case SUPER_PAC_GUM:
                return basePath + "superpacgum.png";
            case PAC_GUM:
                return basePath + "pacgum.png";
            default:
                return "";
        }
    }

    @Override
    public String getStateName() {
        return "배치: " + (selectedEntityType != null ? selectedEntityType.getDisplayName() : "없음");
    }

    @Override
    public void enter() {
        currentGridPosition = null;
        canPlaceAtCurrentPosition = false;
    }

    @Override
    public void exit() {
        currentGridPosition = null;
        canPlaceAtCurrentPosition = false;
        // selectedEntityType은 나중에 재사용될 수 있으므로 null로 설정하지 않음
        // 다음 setEntityType 호출시 새로운 값으로 설정됨
    }

    @Override
    public EntityType getSelectedEntityType() {
        return selectedEntityType;
    }
}