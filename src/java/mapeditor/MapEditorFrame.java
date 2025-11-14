package mapeditor;

import mapeditor.controller.MapEditorManager;
import mapeditor.view.EntityPalettePanel;
import mapeditor.view.MapGridPanel;
import mapeditor.view.EntityCounterPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * MapEditorFrame - 맵 에디터 메인 프레임
 * 모든 UI 컴포넌트를 통합하는 메인 윈도우
 *
 * 디자인 패턴 통합:
 * - 모든 패턴들이 조화롭게 작동하는 통합점
 * - MVC 패턴의 View 역할
 */
public class MapEditorFrame extends JFrame {
    private MapEditorManager manager;
    private EntityPalettePanel palettePanel;
    private MapGridPanel gridPanel;
    private EntityCounterPanel counterPanel;

    public MapEditorFrame() {
        this.manager = MapEditorManager.getInstance();
        initializeFrame();
        createComponents();
        setupLayout();
        setupKeyboardShortcuts();
        addWindowListeners();
    }

    /**
     * 프레임 초기화
     */
    private void initializeFrame() {
        setTitle("Pacman Map Editor - 28×31 Grid (CSV: 56×62)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // 아이콘 설정 (있는 경우)
        try {
            Image icon = Toolkit.getDefaultToolkit().getImage("src/resources/img/pacman.png");
            setIconImage(icon);
        } catch (Exception e) {
            // 아이콘 로드 실패 시 무시
        }
    }

    /**
     * 컴포넌트 생성
     */
    private void createComponents() {
        // 상단 팔레트 패널
        palettePanel = new EntityPalettePanel();

        // 중앙 그리드 패널
        gridPanel = new MapGridPanel();
        JScrollPane gridScrollPane = new JScrollPane(gridPanel);
        gridScrollPane.setPreferredSize(new Dimension(
            MapGridPanel.CELL_SIZE * mapeditor.model.MapData.WIDTH + 20,
            MapGridPanel.CELL_SIZE * mapeditor.model.MapData.HEIGHT + 20
        ));

        // 오른쪽 카운터 패널
        counterPanel = new EntityCounterPanel();
    }

    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 상단 팔레트
        add(palettePanel, BorderLayout.NORTH);

        // 중앙 영역 (그리드 + 카운터)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        centerPanel.add(counterPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null); // 화면 중앙에 배치
    }

    /**
     * 키보드 단축키 설정
     */
    private void setupKeyboardShortcuts() {
        // 메뉴바 생성
        JMenuBar menuBar = new JMenuBar();

        // 파일 메뉴
        JMenu fileMenu = new JMenu("파일");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newMapItem = new JMenuItem("새 맵");
        newMapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newMapItem.addActionListener(e -> handleNewMap());
        fileMenu.add(newMapItem);

        JMenuItem saveItem = new JMenuItem("저장");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> handleSave());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("종료");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> handleExit());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // 편집 메뉴
        JMenu editMenu = new JMenu("편집");
        editMenu.setMnemonic(KeyEvent.VK_E);

        JMenuItem undoItem = new JMenuItem("실행 취소");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> {
            if (manager.canUndo()) {
                manager.undo();
                palettePanel.updateButtonStates();
            }
        });
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("다시 실행");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> {
            if (manager.canRedo()) {
                manager.redo();
                palettePanel.updateButtonStates();
            }
        });
        editMenu.add(redoItem);

        editMenu.addSeparator();

        JMenuItem clearItem = new JMenuItem("모두 지우기");
        clearItem.addActionListener(e -> handleClearAll());
        editMenu.add(clearItem);

        menuBar.add(editMenu);

        // 도움말 메뉴
        JMenu helpMenu = new JMenu("도움말");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("정보");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        JMenuItem patternsItem = new JMenuItem("적용된 디자인 패턴");
        patternsItem.addActionListener(e -> showDesignPatternsInfo());
        helpMenu.add(patternsItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * 윈도우 리스너 추가
     */
    private void addWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
    }

    /**
     * 새 맵 생성
     */
    private void handleNewMap() {
        int result = JOptionPane.showConfirmDialog(this,
            "새 맵을 생성하시겠습니까?\n현재 작업 내용이 저장되지 않았을 수 있습니다.",
            "새 맵",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            manager.createNewMap();
            palettePanel.clearSelection();
        }
    }

    /**
     * 저장 처리
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
            mapeditor.model.EntityType[][] mapData = manager.getMapDataCopy();
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
     * 모두 지우기
     */
    private void handleClearAll() {
        int result = JOptionPane.showConfirmDialog(this,
            "맵의 모든 엔티티를 지우시겠습니까?",
            "모두 지우기",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            manager.resetMap();
            palettePanel.clearSelection();
        }
    }

    /**
     * 종료 처리
     */
    private void handleExit() {
        int result = JOptionPane.showConfirmDialog(this,
            "맵 에디터를 종료하시겠습니까?\n저장되지 않은 변경사항이 있을 수 있습니다.",
            "종료 확인",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /**
     * About 다이얼로그
     */
    private void showAboutDialog() {
        String message = "Pacman Map Editor\n\n" +
                        "버전: 1.0.0\n" +
                        "개발: 디자인 패턴 수업 과제\n\n" +
                        "56×63 그리드의 Pacman 맵을 생성하는 에디터입니다.\n" +
                        "다양한 디자인 패턴이 적용되었습니다.";

        JOptionPane.showMessageDialog(this, message, "정보",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 디자인 패턴 정보 다이얼로그
     */
    private void showDesignPatternsInfo() {
        String patterns = "적용된 디자인 패턴:\n\n" +
            "1. Observer Pattern\n" +
            "   - MapData (Subject) ↔ UI Components (Observers)\n" +
            "   - 맵 데이터 변경 시 자동 UI 업데이트\n\n" +
            "2. State Pattern\n" +
            "   - EditorState 인터페이스와 구현체들\n" +
            "   - IdleState, PlacementState, EraseState\n" +
            "   - 상태에 따른 마우스 이벤트 처리 변경\n\n" +
            "3. Command Pattern\n" +
            "   - PlaceEntityCommand, RemoveEntityCommand\n" +
            "   - Undo/Redo 기능 구현\n" +
            "   - CommandManager로 히스토리 관리\n\n" +
            "4. Singleton Pattern\n" +
            "   - MapEditorManager\n" +
            "   - 전역 에디터 상태 관리\n\n" +
            "5. MVC Pattern\n" +
            "   - Model: MapData, EntityType\n" +
            "   - View: UI Components (Panels)\n" +
            "   - Controller: MapEditorManager, StateContext\n\n" +
            "각 패턴이 서로 조화롭게 작동하여\n" +
            "유지보수가 쉽고 확장 가능한 구조를 제공합니다.";

        JTextArea textArea = new JTextArea(patterns);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
            "적용된 디자인 패턴", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 메인 메서드
     */
    public static void main(String[] args) {
        // Look and Feel 설정
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 기본 Look and Feel 사용
        }

        // EDT에서 실행
        SwingUtilities.invokeLater(() -> {
            MapEditorFrame frame = new MapEditorFrame();
            frame.setVisible(true);
        });
    }
}