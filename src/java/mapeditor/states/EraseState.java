package mapeditor.states;

import java.awt.*;
import java.awt.event.MouseEvent;
import mapeditor.model.EntityType;
import mapeditor.model.MapData;

/**
 * EraseState - 엔티티 삭제 상태
 * 맵에서 엔티티를 삭제하는 상태
 *
 * State Pattern 구현체:
 * - 지우개 모드에서 클릭한 위치의 엔티티 제거
 * - 빨간색 하이라이트로 삭제 가능 위치 표시
 */
public class EraseState implements EditorState {
    private EditorStateContext context;
    private MapData mapData;
    private Point currentGridPosition;
    private boolean canEraseAtCurrentPosition;

    // 색상 상수
    private static final Color ERASE_HIGHLIGHT_COLOR = new Color(255, 100, 100, 100);
    private static final Color EMPTY_HIGHLIGHT_COLOR = new Color(200, 200, 200, 50);

    public EraseState(EditorStateContext context, MapData mapData) {
        this.context = context;
        this.mapData = mapData;
    }

    @Override
    public void handleMouseClick(int gridX, int gridY, int button) {
        if (button == MouseEvent.BUTTON1) { // 좌클릭
            if (canEraseAtCurrentPosition) {
                // Command Pattern을 위한 준비
                // 현재는 직접 삭제, 나중에 Command로 래핑 예정
                mapData.removeEntity(gridX, gridY);
            }
        } else if (button == MouseEvent.BUTTON3) { // 우클릭
            // 우클릭으로 지우개 모드 취소
            context.setIdleState();
        }
    }

    @Override
    public void handleMouseMove(int gridX, int gridY) {
        currentGridPosition = new Point(gridX, gridY);

        // 편집 불가능한 영역 체크 (고스트 집)
        if (!mapData.isEditable(gridX, gridY)) {
            canEraseAtCurrentPosition = false;
            return;
        }

        // 현재 위치에 삭제할 엔티티가 있는지 확인
        EntityType currentEntity = mapData.getEntityAt(gridX, gridY);
        canEraseAtCurrentPosition = (currentEntity != null && currentEntity != EntityType.EMPTY);
    }

    @Override
    public void handleMouseExit() {
        currentGridPosition = null;
        canEraseAtCurrentPosition = false;
    }

    @Override
    public void render(Graphics2D g, int cellWidth, int cellHeight, Point mousePosition) {
        if (currentGridPosition == null) return;

        int x = currentGridPosition.x * cellWidth;
        int y = currentGridPosition.y * cellHeight;

        // 삭제 가능 여부에 따른 색상 하이라이트
        if (canEraseAtCurrentPosition) {
            g.setColor(ERASE_HIGHLIGHT_COLOR);
        } else {
            g.setColor(EMPTY_HIGHLIGHT_COLOR);
        }
        g.fillRect(x, y, cellWidth, cellHeight);

        // 지우개 아이콘 렌더링
        renderEraserIcon(g, x, y, cellWidth, cellHeight);
    }

    /**
     * 지우개 아이콘 렌더링
     */
    private void renderEraserIcon(Graphics2D g, int x, int y, int width, int height) {
        // 알파 컴포지트 설정 (반투명)
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

        // 지우개 모양 그리기 (간단한 사각형과 X 표시)
        g.setColor(Color.WHITE);
        g.fillRect(x + width/4, y + height/4, width/2, height/2);

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + width/4, y + height/4, x + 3*width/4, y + 3*height/4);
        g.drawLine(x + 3*width/4, y + height/4, x + width/4, y + 3*height/4);

        // 원래 컴포지트 복원
        g.setComposite(originalComposite);
    }

    @Override
    public String getStateName() {
        return "지우개";
    }

    @Override
    public void enter() {
        currentGridPosition = null;
        canEraseAtCurrentPosition = false;
    }

    @Override
    public void exit() {
        currentGridPosition = null;
    }

    @Override
    public EntityType getSelectedEntityType() {
        return null;
    }
}