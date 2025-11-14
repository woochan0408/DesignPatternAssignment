package mapeditor.view;

import mapeditor.controller.MapEditorManager;
import mapeditor.model.EntityType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityPalettePanel - 엔티티 선택 팔레트
 * 상단에 배치되는 엔티티 선택 버튼들
 *
 * Observer Pattern의 일부:
 * - 선택 상태에 따라 버튼 하이라이트 업데이트
 * - MapEditorManager와 연동하여 상태 변경
 */
public class EntityPalettePanel extends JPanel {
    private MapEditorManager manager;
    private Map<EntityType, JToggleButton> entityButtons;
    private ButtonGroup buttonGroup;
    private JToggleButton eraseButton;
    private JButton cancelButton;

    public EntityPalettePanel() {
        this.manager = MapEditorManager.getInstance();
        this.entityButtons = new HashMap<>();
        this.buttonGroup = new ButtonGroup();

        initializePanel();
        createButtons();
    }

    /**
     * 패널 초기화
     */
    private void initializePanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 2),
                "■ 엔티티 팔레트",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                Color.WHITE
            )
        ));
        setPreferredSize(new Dimension(0, 110));
        setBackground(new Color(45, 45, 45));
    }

    /**
     * 버튼들 생성 및 배치
     */
    private void createButtons() {
        // 필수 엔티티 그룹
        JPanel requiredPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        requiredPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 200, 100), 1),
            "★ 필수 엔티티",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12),
            Color.WHITE
        ));
        requiredPanel.setBackground(new Color(55, 55, 55));

        addEntityButton(requiredPanel, EntityType.PACMAN, Color.YELLOW);
        addEntityButton(requiredPanel, EntityType.BLINKY, Color.RED);
        addEntityButton(requiredPanel, EntityType.PINKY, Color.PINK);
        addEntityButton(requiredPanel, EntityType.INKY, Color.CYAN);
        addEntityButton(requiredPanel, EntityType.CLYDE, Color.ORANGE);

        add(requiredPanel);

        // 자유 배치 엔티티 그룹
        JPanel freePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        freePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 200, 255), 1),
            "◆ 자유 배치",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12),
            Color.WHITE
        ));
        freePanel.setBackground(new Color(55, 55, 55));

        addEntityButton(freePanel, EntityType.WALL, Color.BLUE);
        addEntityButton(freePanel, EntityType.SUPER_PAC_GUM, Color.WHITE);

        add(freePanel);

        // 도구 그룹
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        toolPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 100, 100), 1),
            "▶ 도구",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12),
            new Color(255, 100, 100)
        ));
        toolPanel.setBackground(new Color(55, 55, 55));

        // 지우개 버튼
        eraseButton = new JToggleButton("지우개") {
            private Color baseColor = new Color(200, 200, 200);

            @Override
            public void setSelected(boolean selected) {
                super.setSelected(selected);
                // 선택 상태에 따라 테두리와 배경색 변경
                if (selected) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 215, 0), 4), // 금색 테두리
                        BorderFactory.createLineBorder(Color.WHITE, 2)
                    ));
                    setBackground(baseColor.brighter());
                } else {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                    setBackground(baseColor);
                }
            }
        };
        eraseButton.setPreferredSize(new Dimension(80, 40));
        eraseButton.setBackground(new Color(200, 200, 200));
        eraseButton.setForeground(Color.BLACK);
        eraseButton.setFont(new Font("Arial", Font.BOLD, 12));
        eraseButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        eraseButton.setFocusPainted(false);
        eraseButton.addActionListener(e -> {
            if (eraseButton.isSelected()) {
                manager.setEraseMode();
                clearOtherSelections(eraseButton);
            }
        });
        buttonGroup.add(eraseButton);
        toolPanel.add(eraseButton);

        // 취소 버튼
        cancelButton = new JButton("X (취소)");
        cancelButton.setPreferredSize(new Dimension(80, 40));
        cancelButton.setBackground(new Color(200, 200, 200));
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 12));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> {
            buttonGroup.clearSelection();
            manager.cancelSelection();
            updateButtonStates();
        });
        toolPanel.add(cancelButton);

        add(toolPanel);

        // Undo/Redo 버튼 그룹
        JPanel undoRedoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        undoRedoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 100, 255), 1),
            "◀ 편집",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12),
            Color.WHITE
        ));
        undoRedoPanel.setBackground(new Color(55, 55, 55));

        JButton undoButton = new JButton("↶ Undo");
        undoButton.setPreferredSize(new Dimension(85, 45));
        undoButton.setBackground(new Color(80, 80, 80));
        undoButton.setForeground(Color.WHITE);
        undoButton.setFocusPainted(false);
        undoButton.setBorder(BorderFactory.createRaisedBevelBorder());
        undoButton.addActionListener(e -> {
            if (manager.canUndo()) {
                manager.undo();
                updateButtonStates();
            }
        });
        undoRedoPanel.add(undoButton);

        JButton redoButton = new JButton("↷ Redo");
        redoButton.setPreferredSize(new Dimension(85, 45));
        redoButton.setBackground(new Color(80, 80, 80));
        redoButton.setForeground(Color.WHITE);
        redoButton.setFocusPainted(false);
        redoButton.setBorder(BorderFactory.createRaisedBevelBorder());
        redoButton.addActionListener(e -> {
            if (manager.canRedo()) {
                manager.redo();
                updateButtonStates();
            }
        });
        undoRedoPanel.add(redoButton);

        add(undoRedoPanel);
    }

    /**
     * 엔티티 버튼 추가
     */
    private void addEntityButton(JPanel panel, EntityType entityType, Color color) {
        JToggleButton button = createEntityButton(entityType, color);
        entityButtons.put(entityType, button);
        buttonGroup.add(button);
        panel.add(button);
    }

    /**
     * 엔티티 버튼 생성
     */
    private JToggleButton createEntityButton(EntityType entityType, Color originalColor) {
        JToggleButton button = new JToggleButton() {
            private Color baseColor = originalColor;

            @Override
            public void setSelected(boolean selected) {
                super.setSelected(selected);
                // 선택 상태에 따라 테두리와 배경색 변경
                if (selected) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 215, 0), 4), // 금색 테두리
                        BorderFactory.createLineBorder(Color.WHITE, 2)
                    ));
                    // 약간 밝은 배경색으로 변경
                    Color brighterColor = baseColor.brighter();
                    setBackground(brighterColor);
                } else {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                    setBackground(baseColor);
                }
            }
        };
        button.setPreferredSize(new Dimension(75, 50));
        button.setBackground(originalColor);
        button.setToolTipText(entityType.getDisplayName());
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setFocusPainted(false);

        // 버튼 텍스트 설정 (심볼 없이 이름만)
        String buttonText = entityType.getDisplayName().split(" ")[0];
        button.setText(buttonText);

        // 모든 텍스트를 어두운 색으로 일관성 있게 설정
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Arial", Font.BOLD, 12));

        // 액션 리스너 추가
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (button.isSelected()) {
                    manager.selectEntity(entityType);
                    clearOtherSelections(button);
                } else {
                    manager.cancelSelection();
                }
                updateButtonStates();
            }
        });

        return button;
    }

    /**
     * 다른 버튼들의 선택 해제
     */
    private void clearOtherSelections(JToggleButton selectedButton) {
        for (JToggleButton button : entityButtons.values()) {
            if (button != selectedButton) {
                button.setSelected(false);
            }
        }
        if (selectedButton != eraseButton) {
            eraseButton.setSelected(false);
        }
    }

    /**
     * 버튼 상태 업데이트 (필수 엔티티 최대 개수 도달 시 비활성화)
     */
    public void updateButtonStates() {
        for (Map.Entry<EntityType, JToggleButton> entry : entityButtons.entrySet()) {
            EntityType type = entry.getKey();
            JToggleButton button = entry.getValue();

            if (type.isRequired() && type.getMaxCount() > 0) {
                int currentCount = manager.getEntityCount(type);
                boolean canPlaceMore = currentCount < type.getMaxCount();

                // 이미 최대 개수에 도달했으면 버튼 비활성화
                button.setEnabled(canPlaceMore || button.isSelected());

                // 툴팁 업데이트
                button.setToolTipText(String.format("%s (%d/%d)",
                    type.getDisplayName(), currentCount, type.getMaxCount()));
            }
        }

        // Undo/Redo 버튼 상태도 업데이트할 수 있음
        // (별도의 메서드로 분리하는 것이 좋음)
    }

    /**
     * 선택 해제
     */
    public void clearSelection() {
        buttonGroup.clearSelection();
        updateButtonStates();
    }
}