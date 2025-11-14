package mapeditor.view;

import mapeditor.controller.MapEditorManager;
import mapeditor.model.EntityType;
import mapeditor.observers.MapObserver;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityCounterPanel - 엔티티 카운터 패널
 * 오른쪽에 배치되어 필수 엔티티 개수 표시
 *
 * Observer Pattern 구현:
 * - MapObserver를 구현하여 엔티티 개수 변경 시 자동 업데이트
 * - 검증 상태에 따라 시각적 피드백 제공
 */
public class EntityCounterPanel extends JPanel implements MapObserver {
    private MapEditorManager manager;
    private Map<EntityType, JLabel> countLabels;
    private Map<EntityType, JLabel> statusIcons;
    private JButton saveButton;
    private JButton resetButton;
    private JTextArea validationMessage;

    public EntityCounterPanel() {
        this.manager = MapEditorManager.getInstance();
        this.countLabels = new HashMap<>();
        this.statusIcons = new HashMap<>();

        initializePanel();
        createComponents();

        // Observer 등록
        manager.addObserver(this);

        // 초기 상태 업데이트
        updateAllCounts();
    }

    /**
     * 패널 초기화
     */
    private void initializePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(200, 0));
        setBorder(BorderFactory.createTitledBorder("필수 엔티티 현황"));
        setBackground(new Color(245, 245, 245));
    }

    /**
     * 컴포넌트 생성
     */
    private void createComponents() {
        // 필수 엔티티 카운터
        JPanel countersPanel = new JPanel();
        countersPanel.setLayout(new GridLayout(0, 1, 5, 5));
        countersPanel.setBackground(new Color(245, 245, 245));

        for (EntityType type : EntityType.values()) {
            if (type.isRequired() && type.getMaxCount() > 0) {
                JPanel counterRow = createCounterRow(type);
                countersPanel.add(counterRow);
            }
        }

        add(countersPanel);
        add(Box.createVerticalStrut(10));

        // 구분선
        JSeparator separator = new JSeparator();
        add(separator);
        add(Box.createVerticalStrut(10));

        // 검증 메시지 영역
        validationMessage = new JTextArea(3, 15);
        validationMessage.setEditable(false);
        validationMessage.setWrapStyleWord(true);
        validationMessage.setLineWrap(true);
        validationMessage.setFont(new Font("Arial", Font.PLAIN, 11));
        validationMessage.setBackground(new Color(255, 255, 230));
        validationMessage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(validationMessage);
        scrollPane.setPreferredSize(new Dimension(180, 60));
        add(scrollPane);
        add(Box.createVerticalStrut(10));

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBackground(new Color(245, 245, 245));

        // 저장 버튼
        saveButton = new JButton("💾 저장");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setBackground(new Color(100, 200, 100));
        saveButton.setForeground(Color.WHITE);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> handleSave());
        buttonPanel.add(saveButton);

        // 초기화 버튼
        resetButton = new JButton("🗑️ 초기화");
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        resetButton.setBackground(new Color(200, 100, 100));
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(e -> handleReset());
        buttonPanel.add(resetButton);

        add(buttonPanel);
        add(Box.createVerticalGlue());
    }

    /**
     * 엔티티 카운터 행 생성
     */
    private JPanel createCounterRow(EntityType type) {
        JPanel row = new JPanel();
        row.setLayout(new BorderLayout(5, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 엔티티 이름
        JLabel nameLabel = new JLabel(type.getDisplayName() + ":");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        row.add(nameLabel, BorderLayout.WEST);

        // 카운트 레이블
        JLabel countLabel = new JLabel("0/" + type.getMaxCount());
        countLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        countLabels.put(type, countLabel);
        row.add(countLabel, BorderLayout.CENTER);

        // 상태 아이콘
        JLabel statusIcon = new JLabel("⚠️");
        statusIcon.setFont(new Font("Arial", Font.PLAIN, 16));
        statusIcon.setForeground(Color.RED);
        statusIcons.put(type, statusIcon);
        row.add(statusIcon, BorderLayout.EAST);

        return row;
    }

    /**
     * 저장 버튼 핸들러
     */
    private void handleSave() {
        if (!manager.validateMap()) {
            JOptionPane.showMessageDialog(this,
                manager.getValidationErrorMessage(),
                "맵 저장 실패",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // PacGum 자동 채우기
        manager.fillEmptySpacesWithPacGum();

        // CSV 파일과 배경 이미지로 저장
        try {
            EntityType[][] mapData = manager.getMapDataCopy(); // 논리적 28×31 그리드
            String csvPath = mapeditor.utils.CsvMapWriter.saveMap(mapData, null);

            // 이미지 경로 계산
            String fileName = new java.io.File(csvPath).getName();
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String imgPath = "src/resources/img/" + nameWithoutExt + "_bg.png";

            JOptionPane.showMessageDialog(this,
                "맵이 성공적으로 저장되었습니다!\n\n" +
                "CSV 파일: " + csvPath + "\n" +
                "배경 이미지: " + imgPath + "\n\n" +
                "게임에서 이 맵을 사용하려면 Game.java에서\n" +
                "\"level/" + fileName + "\"로 변경하세요.",
                "저장 완료",
                JOptionPane.INFORMATION_MESSAGE);

            manager.setLastSavedFilePath(csvPath);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "맵 저장 중 오류가 발생했습니다:\n" + e.getMessage(),
                "저장 실패",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 초기화 버튼 핸들러
     */
    private void handleReset() {
        int result = JOptionPane.showConfirmDialog(this,
            "정말 맵을 초기화하시겠습니까?\n모든 배치된 엔티티가 삭제됩니다.",
            "초기화 확인",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            manager.resetMap();
        }
    }

    /**
     * 모든 카운트 업데이트
     */
    private void updateAllCounts() {
        for (EntityType type : EntityType.values()) {
            if (type.isRequired() && type.getMaxCount() > 0) {
                updateEntityCount(type);
            }
        }
        updateValidationState();
    }

    /**
     * 개별 엔티티 카운트 업데이트
     */
    private void updateEntityCount(EntityType type) {
        int count = manager.getEntityCount(type);
        int max = type.getMaxCount();

        JLabel countLabel = countLabels.get(type);
        if (countLabel != null) {
            countLabel.setText(count + "/" + max);

            // 색상 변경
            if (count == max) {
                countLabel.setForeground(new Color(0, 150, 0));
            } else {
                countLabel.setForeground(Color.RED);
            }
        }

        JLabel statusIcon = statusIcons.get(type);
        if (statusIcon != null) {
            if (count == max) {
                statusIcon.setText("✓");
                statusIcon.setForeground(new Color(0, 150, 0));
            } else {
                statusIcon.setText("⚠️");
                statusIcon.setForeground(Color.RED);
            }
        }
    }

    /**
     * 검증 상태 업데이트
     */
    private void updateValidationState() {
        boolean isValid = manager.validateMap();
        saveButton.setEnabled(isValid);

        if (isValid) {
            validationMessage.setText("✓ 맵을 저장할 준비가 완료되었습니다.");
            validationMessage.setForeground(new Color(0, 150, 0));
        } else {
            validationMessage.setText(manager.getValidationErrorMessage());
            validationMessage.setForeground(Color.RED);
        }
    }

    // ========== MapObserver 인터페이스 구현 ==========

    @Override
    public void onEntityPlaced(int x, int y, EntityType entityType) {
        updateEntityCount(entityType);
        updateValidationState();
    }

    @Override
    public void onEntityRemoved(int x, int y) {
        // 모든 필수 엔티티 카운트 업데이트
        updateAllCounts();
    }

    @Override
    public void onMapReset() {
        updateAllCounts();
    }

    @Override
    public void onEntityCountChanged(EntityType entityType, int count) {
        if (entityType.isRequired() && entityType.getMaxCount() > 0) {
            updateEntityCount(entityType);
            updateValidationState();
        }
    }

    @Override
    public void onValidationStateChanged(boolean isValid) {
        saveButton.setEnabled(isValid);
        updateValidationState();
    }
}