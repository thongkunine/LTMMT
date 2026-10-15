package server;

import client.LoginForm;
import common.Message;
import common.MessageType;
import common.Protocol;
import database.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Giao diện Quản Trị Máy Chủ (Chat Server Manager GUI).
 * Thiết kế giao diện hiện đại, trực quan, hỗ trợ theo dõi người dùng, phòng chat,
 * phát thông báo toàn hệ thống, kiểm soát luồng, xem nhật ký thời gian thực và kiểm tra cơ sở dữ liệu.
 */
public class ServerGUI extends JFrame implements ServerListener {

    // Theme Colors
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);       // Royal Blue #2563EB
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216); // #1D4ED8
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);      // Emerald #10B981
    private static final Color COLOR_SUCCESS_HOVER = new Color(5, 150, 105);
    private static final Color COLOR_DANGER = new Color(239, 68, 68);        // Red #EF4444
    private static final Color COLOR_DANGER_HOVER = new Color(220, 38, 38);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);      // Amber #F59E0B
    private static final Color COLOR_PURPLE = new Color(139, 92, 246);       // Violet #8B5CF6
    private static final Color COLOR_BG_HEADER = new Color(15, 23, 42);      // Slate 900 #0F172A
    private static final Color COLOR_BG_PANEL = new Color(241, 245, 249);    // Slate 100 #F1F5F9
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_CARD_BORDER = new Color(226, 232, 240); // Slate 200 #E2E8F0
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_CONSOLE_BG = new Color(15, 23, 42);

    // Fonts
    private static final Font FONT_HEADER_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_HEADER_SUB = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_CARD_VAL = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_CARD_SUB = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BOLD_13 = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_REGULAR_13 = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_CONSOLE = new Font("Consolas", Font.PLAIN, 12);

    // Server Core
    private final ChatServer server;
    private LocalDateTime serverStartTime;
    private Timer uptimeTimer;
    private Timer memoryTimer;

    // Header Components
    private JLabel lblStatusBadge;
    private JLabel lblUptime;
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnRestart;
    private JButton btnLaunchClient;

    // Stat Card Values
    private JLabel lblValUsers;
    private JLabel lblValRooms;
    private JLabel lblValThreads;
    private JLabel lblValMemory;
    private JProgressBar barMemory;

    // Online Users Table
    private DefaultTableModel usersTableModel;
    private JTable tblUsers;
    private JTextField txtSearchUser;

    // Rooms Panel
    private DefaultListModel<String> roomListModel;
    private JList<String> lstRooms;
    private DefaultTableModel roomMembersModel;
    private JTable tblRoomMembers;
    private JLabel lblSelectedRoomTitle;
    private JTextField txtRoomMsg;

    // Broadcast Panel
    private JComboBox<String> cmbBroadcastTarget;
    private JTextArea txtBroadcastMessage;

    // Logs Console
    private JTextPane txtLogConsole;
    private StyledDocument logDocument;
    private JComboBox<String> cmbLogFilter;
    private JTextField txtSearchLog;
    private JCheckBox chkAutoScroll;
    private final List<LogEntry> allLogs = new ArrayList<>();

    // Settings Panel
    private JTextField txtPortTcp;
    private JTextField txtPortVoice;
    private JTextField txtPortVideo;
    private JLabel lblIpDetected;
    private JLabel lblDbStatus;

    // Footer
    private JLabel lblFooterInfo;

    public ServerGUI() {
        this(new ChatServer());
    }

    public ServerGUI(ChatServer server) {
        this.server = server;
        this.server.addListener(this);

        initWindow();
        initComponents();
        setupTimers();

        // Tự động khởi động Server khi mở giao diện
        SwingUtilities.invokeLater(this::startServerAction);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new ServerGUI().setVisible(true);
        });
    }

    private void initWindow() {
        setTitle("Quản Trị Hệ Thống Máy Chủ Chat (Chat Server Manager)");
        setSize(1080, 740);
        setMinimumSize(new Dimension(940, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
    }

    private void confirmExit() {
        if (server.isRunning()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Máy chủ đang hoạt động. Bạn có chắc chắn muốn dừng máy chủ và đóng ứng dụng?",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                server.stopServer();
                dispose();
                System.exit(0);
            }
        } else {
            dispose();
            System.exit(0);
        }
    }

    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 8));
        rootPanel.setBackground(COLOR_BG_PANEL);

        // 1. Header Bar
        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Middle Area: Metrics Cards + Tabs
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 12, 0, 12));

        centerPanel.add(createMetricCardsPanel(), BorderLayout.NORTH);
        centerPanel.add(createTabbedPanel(), BorderLayout.CENTER);

        rootPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. Footer Bar
        rootPanel.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    // =========================================================================
    // 1. HEADER PANEL
    // =========================================================================
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG_HEADER);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel(ServerIcons.getServerIcon(34, COLOR_PRIMARY));
        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("HỆ THỐNG MÁY CHỦ CHAT (SERVER MANAGER)");
        lblTitle.setFont(FONT_HEADER_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Giao thức TCP Socket (Chat, File) & UDP (Voice, Video Call) | Cổng TCP: " + Protocol.PORT);
        lblSub.setFont(FONT_HEADER_SUB);
        lblSub.setForeground(new Color(148, 163, 184)); // Slate 400

        titleGroup.add(lblTitle);
        titleGroup.add(Box.createVerticalStrut(2));
        titleGroup.add(lblSub);

        brandPanel.add(lblLogo);
        brandPanel.add(titleGroup);

        // Right Controls & Status
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        lblStatusBadge = new JLabel("○ ĐÃ DỪNG", ServerIcons.getStatusDotIcon(14, COLOR_DANGER), SwingConstants.LEFT);
        lblStatusBadge.setFont(FONT_BOLD_13);
        lblStatusBadge.setForeground(COLOR_DANGER);
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(239, 68, 68, 90), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        lblUptime = new JLabel("⏱ 00:00:00");
        lblUptime.setFont(FONT_BOLD_13);
        lblUptime.setForeground(new Color(203, 213, 225)); // Slate 300

        btnStart = createStyledButton("Khởi Động", COLOR_SUCCESS, COLOR_SUCCESS_HOVER, ServerIcons.getPlayIcon(14, Color.WHITE));
        btnStart.addActionListener(e -> startServerAction());

        btnStop = createStyledButton("Dừng Server", COLOR_DANGER, COLOR_DANGER_HOVER, ServerIcons.getStopIcon(14, Color.WHITE));
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopServerAction());

        btnRestart = createStyledButton("Khởi Động Lại", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, ServerIcons.getRestartIcon(14, Color.WHITE));
        btnRestart.addActionListener(e -> restartServerAction());

        btnLaunchClient = createStyledButton("Mở Client Test", COLOR_PURPLE, new Color(124, 58, 237), ServerIcons.getRocketIcon(14, Color.WHITE));
        btnLaunchClient.setToolTipText("Mở cửa sổ Đăng nhập Client để thử nghiệm chat và gọi điện ngay lập tức");
        btnLaunchClient.addActionListener(e -> launchTestClient());

        rightPanel.add(lblStatusBadge);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(lblUptime);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(btnStart);
        rightPanel.add(btnStop);
        rightPanel.add(btnRestart);
        rightPanel.add(btnLaunchClient);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    // 2. METRIC CARDS PANEL
    // =========================================================================
    private JPanel createMetricCardsPanel() {
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        cardsGrid.setOpaque(false);

        // Card 1: Users Online
        lblValUsers = new JLabel("0");
        lblValUsers.setFont(FONT_CARD_VAL);
        lblValUsers.setForeground(COLOR_PRIMARY);
        cardsGrid.add(createCard("NGƯỜI DÙNG ONLINE", lblValUsers, "Tối đa 20 luồng đồng thời", ServerIcons.getUsersIcon(28, COLOR_PRIMARY)));

        // Card 2: Active Rooms
        lblValRooms = new JLabel("1");
        lblValRooms.setFont(FONT_CARD_VAL);
        lblValRooms.setForeground(COLOR_PURPLE);
        cardsGrid.add(createCard("PHÒNG CHAT ĐANG MỞ", lblValRooms, "Mặc định: Lobby", ServerIcons.getRoomsIcon(28, COLOR_PURPLE)));

        // Card 3: Thread Pool
        lblValThreads = new JLabel("0 / 20");
        lblValThreads.setFont(FONT_CARD_VAL);
        lblValThreads.setForeground(COLOR_WARNING);
        cardsGrid.add(createCard("LUỒNG HOẠT ĐỘNG (POOL)", lblValThreads, "Hàng đợi queue: 0 / 50", ServerIcons.getThreadIcon(28, COLOR_WARNING)));

        // Card 4: Memory Usage
        lblValMemory = new JLabel("0 MB");
        lblValMemory.setFont(FONT_CARD_VAL);
        lblValMemory.setForeground(COLOR_SUCCESS);

        barMemory = new JProgressBar(0, 100);
        barMemory.setStringPainted(true);
        barMemory.setFont(new Font("Segoe UI", Font.BOLD, 10));
        barMemory.setForeground(COLOR_SUCCESS);
        barMemory.setPreferredSize(new Dimension(100, 14));

        JPanel memCard = createCard("BỘ NHỚ JVM (RAM)", lblValMemory, null, ServerIcons.getMemoryIcon(28, COLOR_SUCCESS));
        memCard.add(barMemory);
        cardsGrid.add(memCard);

        return cardsGrid;
    }

    private JPanel createCard(String title, JLabel lblValue, String subtitle, javax.swing.Icon icon) {
        RoundPanel card = new RoundPanel(10);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_CARD_TITLE);
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JLabel lblIcon = new JLabel(icon);
        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(lblIcon, BorderLayout.EAST);

        card.add(topRow);
        card.add(Box.createVerticalStrut(6));
        card.add(lblValue);

        if (subtitle != null) {
            card.add(Box.createVerticalStrut(4));
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(FONT_CARD_SUB);
            lblSub.setForeground(COLOR_TEXT_MUTED);
            card.add(lblSub);
        }

        return card;
    }

    // =========================================================================
    // 3. TABS CONTAINER
    // =========================================================================
    private JTabbedPane createTabbedPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_BOLD_13);
        tabs.setBackground(Color.WHITE);

        tabs.addTab("  Người Dùng Online  ", ServerIcons.getUsersIcon(16, COLOR_PRIMARY), createUsersTab());
        tabs.addTab("  Quản Lý Phòng  ", ServerIcons.getRoomsIcon(16, COLOR_PURPLE), createRoomsTab());
        tabs.addTab("  Phát Thông Báo  ", ServerIcons.getBroadcastIcon(16, COLOR_DANGER), createBroadcastTab());
        tabs.addTab("  Nhật Ký Hoạt Động  ", ServerIcons.getLogIcon(16, COLOR_WARNING), createLogsTab());
        tabs.addTab("  Cấu Hình & Database  ", ServerIcons.getSettingsIcon(16, COLOR_TEXT_MUTED), createSettingsTab());

        return tabs;
    }

    // -------------------------------------------------------------------------
    // TAB 1: USERS TAB
    // -------------------------------------------------------------------------
    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 8, 8));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        txtSearchUser = new JTextField(16);
        txtSearchUser.setFont(FONT_REGULAR_13);
        txtSearchUser.putClientProperty("JTextField.placeholderText", "Tìm người dùng...");
        txtSearchUser.addActionListener(e -> filterUserTable());

        JButton btnSearch = createStyledButton("Tìm", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, null);
        btnSearch.addActionListener(e -> filterUserTable());

        JButton btnKick = createStyledButton("Kick Người Dùng", COLOR_DANGER, COLOR_DANGER_HOVER, ServerIcons.getKickIcon(14, Color.WHITE));
        btnKick.addActionListener(e -> kickSelectedUser());

        JButton btnAlert = createStyledButton("Gửi Cảnh Báo", COLOR_WARNING, new Color(217, 119, 6), null);
        btnAlert.addActionListener(e -> sendAlertToSelectedUser());

        JButton btnRefresh = createStyledButton("Làm Mới", new Color(100, 116, 139), new Color(71, 85, 105), ServerIcons.getRestartIcon(14, Color.WHITE));
        btnRefresh.addActionListener(e -> refreshUserTable());

        JButton btnKickAll = createStyledButton("Ngắt Tất Cả", COLOR_DANGER, COLOR_DANGER_HOVER, null);
        btnKickAll.addActionListener(e -> disconnectAllUsers());

        toolbar.add(new JLabel("Tìm kiếm:"));
        toolbar.add(txtSearchUser);
        toolbar.add(btnSearch);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(btnKick);
        toolbar.add(btnAlert);
        toolbar.add(btnRefresh);
        toolbar.add(btnKickAll);

        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"STT", "Tên Tài Khoản", "Địa Chỉ IP & Cổng", "Phòng Hiện Tại", "Thời Gian Đăng Nhập", "Trạng Thái"};
        usersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblUsers = new JTable(usersTableModel);
        styleTable(tblUsers);

        // Context Menu on Right Click
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemKick = new JMenuItem("Ngắt kết nối (Kick)");
        itemKick.addActionListener(e -> kickSelectedUser());
        JMenuItem itemAlert = new JMenuItem("Gửi cảnh báo cá nhân");
        itemAlert.addActionListener(e -> sendAlertToSelectedUser());
        popupMenu.add(itemKick);
        popupMenu.add(itemAlert);

        tblUsers.setComponentPopupMenu(popupMenu);
        tblUsers.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int r = tblUsers.rowAtPoint(e.getPoint());
                if (r >= 0 && r < tblUsers.getRowCount()) {
                    tblUsers.setRowSelectionInterval(r, r);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblUsers);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------------------
    // TAB 2: ROOMS TAB
    // -------------------------------------------------------------------------
    private JPanel createRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 8, 8));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // Left: Room List
        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setOpaque(false);

        JLabel lblListTitle = new JLabel("DANH SÁCH PHÒNG CHAT");
        lblListTitle.setFont(FONT_BOLD_13);
        leftPanel.add(lblListTitle, BorderLayout.NORTH);

        roomListModel = new DefaultListModel<>();
        roomListModel.addElement(Protocol.DEFAULT_ROOM);

        lstRooms = new JList<>(roomListModel);
        lstRooms.setFont(FONT_REGULAR_13);
        lstRooms.setFixedCellHeight(30);
        lstRooms.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedRoomDetails();
            }
        });

        JScrollPane scrollRooms = new JScrollPane(lstRooms);
        scrollRooms.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        leftPanel.add(scrollRooms, BorderLayout.CENTER);

        // Room action buttons
        JPanel roomActionPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        roomActionPanel.setOpaque(false);
        JButton btnCreateRoom = createStyledButton("Tạo Phòng", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, null);
        btnCreateRoom.addActionListener(e -> createRoomDialog());
        JButton btnDeleteEmpty = createStyledButton("Dọn Phòng Trống", new Color(100, 116, 139), new Color(71, 85, 105), null);
        btnDeleteEmpty.addActionListener(e -> cleanupEmptyRooms());
        roomActionPanel.add(btnCreateRoom);
        roomActionPanel.add(btnDeleteEmpty);
        leftPanel.add(roomActionPanel, BorderLayout.SOUTH);

        // Right: Room Details & Members
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setOpaque(false);

        lblSelectedRoomTitle = new JLabel("Chi Tiết Phòng: " + Protocol.DEFAULT_ROOM);
        lblSelectedRoomTitle.setFont(FONT_BOLD_13);
        rightPanel.add(lblSelectedRoomTitle, BorderLayout.NORTH);

        String[] mCols = {"STT", "Tên Người Dùng", "Địa Chỉ Kết Nối", "Thời Gian Vào"};
        roomMembersModel = new DefaultTableModel(mCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRoomMembers = new JTable(roomMembersModel);
        styleTable(tblRoomMembers);

        JScrollPane scrollMembers = new JScrollPane(tblRoomMembers);
        scrollMembers.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        rightPanel.add(scrollMembers, BorderLayout.CENTER);

        // Room message bar
        JPanel roomMsgPanel = new JPanel(new BorderLayout(6, 0));
        roomMsgPanel.setOpaque(false);
        txtRoomMsg = new JTextField();
        txtRoomMsg.setFont(FONT_REGULAR_13);
        txtRoomMsg.putClientProperty("JTextField.placeholderText", "Gửi thông báo quản trị viên tới phòng này...");
        txtRoomMsg.addActionListener(e -> sendRoomNotification());

        JButton btnSendRoomMsg = createStyledButton("Gửi Vào Phòng", COLOR_PURPLE, new Color(124, 58, 237), null);
        btnSendRoomMsg.addActionListener(e -> sendRoomNotification());

        roomMsgPanel.add(txtRoomMsg, BorderLayout.CENTER);
        roomMsgPanel.add(btnSendRoomMsg, BorderLayout.EAST);
        rightPanel.add(roomMsgPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------------------
    // TAB 3: BROADCAST TAB
    // -------------------------------------------------------------------------
    private JPanel createBroadcastTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        RoundPanel card = new RoundPanel(10);
        card.setLayout(new BorderLayout(0, 12));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Form Top
        JPanel formTop = new JPanel(new GridLayout(2, 1, 0, 8));
        formTop.setOpaque(false);

        JPanel targetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        targetRow.setOpaque(false);
        targetRow.add(new JLabel("Phạm vi phát thông báo:"));

        cmbBroadcastTarget = new JComboBox<>(new String[]{"Toàn bộ hệ thống (Tất cả người dùng & phòng)", "Phòng hiện tại: " + Protocol.DEFAULT_ROOM});
        cmbBroadcastTarget.setFont(FONT_REGULAR_13);
        cmbBroadcastTarget.setPreferredSize(new Dimension(340, 30));
        targetRow.add(cmbBroadcastTarget);

        // Preset buttons row
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        presetRow.setOpaque(false);
        presetRow.add(new JLabel("Mẫu thông báo nhanh:"));

        JButton p1 = createMiniButton("🔔 Bảo trì sau 5 phút", e -> setBroadcastText("Hệ thống máy chủ sẽ tạm dừng bảo trì sau 5 phút. Vui lòng hoàn tất cuộc gọi và tin nhắn."));
        JButton p2 = createMiniButton("🎉 Chào mừng", e -> setBroadcastText("Chào mừng tất cả người dùng tham gia ứng dụng Chat, Voice & Video Call!"));
        JButton p3 = createMiniButton("⚠️ Cảnh báo spam", e -> setBroadcastText("Nghiêm cấm gửi tin nhắn rác hoặc từ ngữ vi phạm quy định trò chuyện."));
        JButton p4 = createMiniButton("🔄 Khởi động lại", e -> setBroadcastText("Máy chủ đang chuẩn bị khởi động lại để tối ưu hóa hiệu năng hệ thống."));

        presetRow.add(p1);
        presetRow.add(p2);
        presetRow.add(p3);
        presetRow.add(p4);

        formTop.add(targetRow);
        formTop.add(presetRow);

        card.add(formTop, BorderLayout.NORTH);

        // Center: Text Area
        txtBroadcastMessage = new JTextArea();
        txtBroadcastMessage.setFont(FONT_REGULAR_13);
        txtBroadcastMessage.setLineWrap(true);
        txtBroadcastMessage.setWrapStyleWord(true);
        txtBroadcastMessage.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollMsg = new JScrollPane(txtBroadcastMessage);
        scrollMsg.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER), "Nội dung thông báo phát"));
        card.add(scrollMsg, BorderLayout.CENTER);

        // Bottom: Action Buttons
        JPanel bottomAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomAction.setOpaque(false);

        JButton btnClearText = createStyledButton("Xóa Nhập", new Color(100, 116, 139), new Color(71, 85, 105), ServerIcons.getClearIcon(14, Color.WHITE));
        btnClearText.addActionListener(e -> txtBroadcastMessage.setText(""));

        JButton btnSendNow = createStyledButton("Phát Thông Báo Ngay", COLOR_DANGER, COLOR_DANGER_HOVER, ServerIcons.getBroadcastIcon(16, Color.WHITE));
        btnSendNow.setPreferredSize(new Dimension(200, 36));
        btnSendNow.addActionListener(e -> executeBroadcast());

        bottomAction.add(btnClearText);
        bottomAction.add(btnSendNow);
        card.add(bottomAction, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void setBroadcastText(String text) {
        txtBroadcastMessage.setText(text);
        txtBroadcastMessage.requestFocus();
    }

    // -------------------------------------------------------------------------
    // TAB 4: LOGS TAB
    // -------------------------------------------------------------------------
    private JPanel createLogsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 8, 8));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);

        cmbLogFilter = new JComboBox<>(new String[]{"Tất cả sự kiện", "INFO", "AUTH", "CHAT", "VOICE", "VIDEO", "ROOM", "WARN", "ERROR", "SYSTEM"});
        cmbLogFilter.setFont(FONT_REGULAR_13);
        cmbLogFilter.addActionListener(e -> applyLogFilter());

        txtSearchLog = new JTextField(14);
        txtSearchLog.setFont(FONT_REGULAR_13);
        txtSearchLog.putClientProperty("JTextField.placeholderText", "Tìm trong log...");
        txtSearchLog.addActionListener(e -> applyLogFilter());

        JButton btnSearchLog = createStyledButton("Lọc", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, null);
        btnSearchLog.addActionListener(e -> applyLogFilter());

        chkAutoScroll = new JCheckBox("Tự động cuộn", true);
        chkAutoScroll.setOpaque(false);
        chkAutoScroll.setFont(FONT_REGULAR_13);

        JButton btnClear = createStyledButton("Xóa Console", new Color(100, 116, 139), new Color(71, 85, 105), ServerIcons.getClearIcon(14, Color.WHITE));
        btnClear.addActionListener(e -> clearLogs());

        JButton btnExport = createStyledButton("Xuất File .log", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, ServerIcons.getExportIcon(14, Color.WHITE));
        btnExport.addActionListener(e -> exportLogsToFile());

        filterBar.add(new JLabel("Bộ lọc:"));
        filterBar.add(cmbLogFilter);
        filterBar.add(txtSearchLog);
        filterBar.add(btnSearchLog);
        filterBar.add(chkAutoScroll);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(btnClear);
        filterBar.add(btnExport);

        panel.add(filterBar, BorderLayout.NORTH);

        // Log Console
        txtLogConsole = new JTextPane();
        txtLogConsole.setFont(FONT_CONSOLE);
        txtLogConsole.setBackground(COLOR_CONSOLE_BG);
        txtLogConsole.setEditable(false);
        txtLogConsole.setBorder(new EmptyBorder(8, 10, 8, 10));
        logDocument = txtLogConsole.getStyledDocument();

        JScrollPane scrollConsole = new JScrollPane(txtLogConsole);
        scrollConsole.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        panel.add(scrollConsole, BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------------------
    // TAB 5: SETTINGS & DATABASE TAB
    // -------------------------------------------------------------------------
    private JPanel createSettingsTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: Network & Port Settings
        RoundPanel cardPort = new RoundPanel(10);
        cardPort.setLayout(new GridBagLayout());
        cardPort.setBackground(COLOR_CARD_BG);
        cardPort.setBorder(new EmptyBorder(16, 18, 16, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Header
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblPortTitle = new JLabel("CẤU HÌNH CỔNG MÁY CHỦ (PORTS)");
        lblPortTitle.setFont(FONT_BOLD_13);
        cardPort.add(lblPortTitle, gbc);

        // IP Display
        gbc.gridy = 1; gbc.gridwidth = 1;
        cardPort.add(new JLabel("Địa chỉ IP máy chủ:"), gbc);
        gbc.gridx = 1;
        lblIpDetected = new JLabel(detectLocalIP() + " (Localhost: 127.0.0.1)");
        lblIpDetected.setFont(FONT_BOLD_13);
        lblIpDetected.setForeground(COLOR_PRIMARY);
        cardPort.add(lblIpDetected, gbc);

        // TCP Chat Port
        gbc.gridx = 0; gbc.gridy = 2;
        cardPort.add(new JLabel("Cổng TCP Chat:"), gbc);
        gbc.gridx = 1;
        txtPortTcp = new JTextField(String.valueOf(Protocol.PORT), 8);
        cardPort.add(txtPortTcp, gbc);

        // UDP Voice Port
        gbc.gridx = 0; gbc.gridy = 3;
        cardPort.add(new JLabel("Cổng UDP Voice Call:"), gbc);
        gbc.gridx = 1;
        txtPortVoice = new JTextField(String.valueOf(Protocol.VOICE_PORT), 8);
        cardPort.add(txtPortVoice, gbc);

        // UDP Video Port
        gbc.gridx = 0; gbc.gridy = 4;
        cardPort.add(new JLabel("Cổng UDP Video Call:"), gbc);
        gbc.gridx = 1;
        txtPortVideo = new JTextField(String.valueOf(Protocol.VIDEO_PORT), 8);
        cardPort.add(txtPortVideo, gbc);

        // Thread Pool Limit
        gbc.gridx = 0; gbc.gridy = 5;
        cardPort.add(new JLabel("Giới hạn Thread Pool:"), gbc);
        gbc.gridx = 1;
        JLabel lblThreadInfo = new JLabel(ChatServer.MAX_THREADS + " luồng (Queue: " + ChatServer.MAX_QUEUE + ")");
        cardPort.add(lblThreadInfo, gbc);

        // Save & Restart button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 6, 6);
        JButton btnApplyPorts = createStyledButton("Lưu & Áp Dụng Cổng Mới", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, null);
        btnApplyPorts.addActionListener(e -> applyNewPorts());
        cardPort.add(btnApplyPorts, gbc);

        // Right: Database Connection Test
        RoundPanel cardDb = new RoundPanel(10);
        cardDb.setLayout(new GridBagLayout());
        cardDb.setBackground(COLOR_CARD_BG);
        cardDb.setBorder(new EmptyBorder(16, 18, 16, 18));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblDbTitle = new JLabel("KẾT NỐI CƠ SỞ DỮ LIỆU SQL SERVER");
        lblDbTitle.setFont(FONT_BOLD_13);
        cardDb.add(lblDbTitle, gbc);

        gbc.gridy = 1; gbc.gridwidth = 2;
        JLabel lblDbDesc = new JLabel("<html>Database: <b>chatapp</b> (Cổng 1433)<br>Lưu trữ tài khoản người dùng và lịch sử tin nhắn.</html>");
        lblDbDesc.setFont(FONT_REGULAR_13);
        cardDb.add(lblDbDesc, gbc);

        gbc.gridy = 2; gbc.gridwidth = 2;
        lblDbStatus = new JLabel("Trạng thái: Chưa kiểm tra");
        lblDbStatus.setFont(FONT_BOLD_13);
        lblDbStatus.setForeground(COLOR_TEXT_MUTED);
        cardDb.add(lblDbStatus, gbc);

        gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 6, 6);
        JButton btnCheckDb = createStyledButton("Kiểm Tra Kết Nối SQL Server", COLOR_SUCCESS, COLOR_SUCCESS_HOVER, ServerIcons.getDatabaseIcon(16, Color.WHITE));
        btnCheckDb.addActionListener(e -> testDatabaseConnection());
        cardDb.add(btnCheckDb, gbc);

        panel.add(cardPort);
        panel.add(cardDb);
        return panel;
    }

    // =========================================================================
    // 4. FOOTER STATUS BAR
    // =========================================================================
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(6, 16, 6, 16));

        lblFooterInfo = new JLabel("Máy chủ sẵn sàng | IP: " + detectLocalIP() + " | TCP: " + server.getTcpPort() + " | UDP Voice: " + server.getVoicePort() + " | UDP Video: " + server.getVideoPort());
        lblFooterInfo.setFont(FONT_REGULAR_13);
        lblFooterInfo.setForeground(COLOR_TEXT_MUTED);

        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightFooter.setOpaque(false);

        JButton btnGc = createMiniButton("🧹 Giải phóng RAM (GC)", e -> {
            System.gc();
            updateMemoryStats();
            JOptionPane.showMessageDialog(this, "Đã gọi Garbage Collector giải phóng bộ nhớ JVM!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        rightFooter.add(btnGc);

        footer.add(lblFooterInfo, BorderLayout.WEST);
        footer.add(rightFooter, BorderLayout.EAST);
        return footer;
    }

    // =========================================================================
    // SERVER CONTROL ACTIONS
    // =========================================================================
    private void startServerAction() {
        int tcp = parsePort(txtPortTcp != null ? txtPortTcp.getText() : String.valueOf(Protocol.PORT), Protocol.PORT);
        int voice = parsePort(txtPortVoice != null ? txtPortVoice.getText() : String.valueOf(Protocol.VOICE_PORT), Protocol.VOICE_PORT);
        int video = parsePort(txtPortVideo != null ? txtPortVideo.getText() : String.valueOf(Protocol.VIDEO_PORT), Protocol.VIDEO_PORT);

        btnStart.setEnabled(false);
        new Thread(() -> {
            boolean ok = server.startServer(tcp, voice, video);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    serverStartTime = LocalDateTime.now();
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    updateStatusUI(true);
                } else {
                    btnStart.setEnabled(true);
                    btnStop.setEnabled(false);
                    updateStatusUI(false);
                }
            });
        }, "ServerStart-Worker").start();
    }

    private void stopServerAction() {
        int choice = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn dừng toàn bộ hoạt động của máy chủ?", "Xác nhận dừng", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        btnStop.setEnabled(false);
        new Thread(() -> {
            server.stopServer();
            SwingUtilities.invokeLater(() -> {
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                updateStatusUI(false);
                refreshUserTable();
            });
        }, "ServerStop-Worker").start();
    }

    private void restartServerAction() {
        int choice = JOptionPane.showConfirmDialog(this, "Khởi động lại máy chủ sẽ ngắt toàn bộ kết nối hiện tại. Bạn có muốn tiếp tục?", "Khởi động lại", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        int tcp = parsePort(txtPortTcp != null ? txtPortTcp.getText() : String.valueOf(Protocol.PORT), Protocol.PORT);
        int voice = parsePort(txtPortVoice != null ? txtPortVoice.getText() : String.valueOf(Protocol.VOICE_PORT), Protocol.VOICE_PORT);
        int video = parsePort(txtPortVideo != null ? txtPortVideo.getText() : String.valueOf(Protocol.VIDEO_PORT), Protocol.VIDEO_PORT);

        btnRestart.setEnabled(false);
        new Thread(() -> {
            server.restartServer(tcp, voice, video);
            SwingUtilities.invokeLater(() -> {
                serverStartTime = LocalDateTime.now();
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                btnRestart.setEnabled(true);
                updateStatusUI(true);
                refreshUserTable();
            });
        }, "ServerRestart-Worker").start();
    }

    private void updateStatusUI(boolean running) {
        if (running) {
            lblStatusBadge.setText("● ĐANG CHẠY (PORT " + server.getTcpPort() + ")");
            lblStatusBadge.setIcon(ServerIcons.getStatusDotIcon(14, COLOR_SUCCESS));
            lblStatusBadge.setForeground(COLOR_SUCCESS);
            lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(16, 185, 129, 120), 1, true),
                    new EmptyBorder(4, 10, 4, 10)
            ));
        } else {
            lblStatusBadge.setText("○ ĐÃ DỪNG");
            lblStatusBadge.setIcon(ServerIcons.getStatusDotIcon(14, COLOR_DANGER));
            lblStatusBadge.setForeground(COLOR_DANGER);
            lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(239, 68, 68, 120), 1, true),
                    new EmptyBorder(4, 10, 4, 10)
            ));
            lblUptime.setText("⏱ 00:00:00");
        }
        if (lblFooterInfo != null) {
            lblFooterInfo.setText("Trạng thái: " + (running ? "Đang chạy" : "Đã dừng") + " | IP: " + detectLocalIP() + " | TCP: " + server.getTcpPort() + " | UDP Voice: " + server.getVoicePort() + " | UDP Video: " + server.getVideoPort());
        }
    }

    private void applyNewPorts() {
        if (server.isRunning()) {
            int c = JOptionPane.showConfirmDialog(this, "Máy chủ cần khởi động lại để áp dụng cổng mới. Khởi động lại ngay?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                restartServerAction();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Cổng mới đã được lưu. Hãy bấm 'Khởi Động' để chạy máy chủ.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void launchTestClient() {
        SwingUtilities.invokeLater(() -> {
            try {
                LoginForm form = new LoginForm();
                form.setVisible(true);
                logLocal("INFO", "Đã mở cửa sổ Client kiểm thử (LoginForm).");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở Client: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void testDatabaseConnection() {
        lblDbStatus.setText("Đang kiểm tra kết nối SQL Server...");
        lblDbStatus.setForeground(COLOR_WARNING);

        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    SwingUtilities.invokeLater(() -> {
                        lblDbStatus.setText("✅ Kết nối SQL Server thành công (Database: chatapp)!");
                        lblDbStatus.setForeground(COLOR_SUCCESS);
                        logLocal("INFO", "Kiểm tra kết nối Database: Thành công!");
                    });
                } else {
                    throw new Exception("Connection returned null or closed");
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblDbStatus.setText("❌ Không thể kết nối SQL Server (" + ex.getMessage() + ")");
                    lblDbStatus.setForeground(COLOR_DANGER);
                    logLocal("ERROR", "Kiểm tra kết nối Database thất bại: " + ex.getMessage());
                });
            }
        }, "DbTest-Worker").start();
    }

    // =========================================================================
    // USER MANAGEMENT ACTIONS
    // =========================================================================
    private void refreshUserTable() {
        usersTableModel.setRowCount(0);
        int stt = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");

        for (ClientHandler client : server.getUserManager().getClients()) {
            String uname = client.getUsername();
            String addr = client.getRemoteAddress();
            String room = client.getCurrentRoomName();
            String time = client.getConnectedTime().format(dtf);
            String status = "Online";

            usersTableModel.addRow(new Object[]{stt++, uname, addr, room, time, status});
        }
        lblValUsers.setText(String.valueOf(server.getUserManager().getOnlineCount()));
    }

    private void filterUserTable() {
        String query = txtSearchUser.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            refreshUserTable();
            return;
        }
        usersTableModel.setRowCount(0);
        int stt = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");

        for (ClientHandler client : server.getUserManager().getClients()) {
            if (client.getUsername().toLowerCase().contains(query) || client.getRemoteAddress().contains(query)) {
                usersTableModel.addRow(new Object[]{
                    stt++, client.getUsername(), client.getRemoteAddress(), client.getCurrentRoomName(),
                    client.getConnectedTime().format(dtf), "Online"
                });
            }
        }
    }

    private void kickSelectedUser() {
        int row = tblUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng để ngắt kết nối!", "Chưa chọn người dùng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tblUsers.getValueAt(row, 1);
        String reason = JOptionPane.showInputDialog(this, "Nhập lý do ngắt kết nối người dùng '" + username + "':", "Lý do kick", JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }

        server.kickUser(username, reason);
        refreshUserTable();
    }

    private void sendAlertToSelectedUser() {
        int row = tblUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng cần gửi cảnh báo!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tblUsers.getValueAt(row, 1);
        String alertMsg = JOptionPane.showInputDialog(this, "Nhập thông báo gửi riêng tới '" + username + "':", "Cảnh báo quản trị viên", JOptionPane.WARNING_MESSAGE);
        if (alertMsg != null && !alertMsg.isBlank()) {
            ClientHandler client = server.getUserManager().getClient(username);
            if (client != null) {
                client.send(new Message(MessageType.SYSTEM, "ADMIN", "[CẢNH BÁO TỪ QUẢN TRỊ VIÊN]: " + alertMsg.trim(), client.getCurrentRoomName()));
                logLocal("SYSTEM", "Đã gửi cảnh báo tới '" + username + "': " + alertMsg.trim());
            }
        }
    }

    private void disconnectAllUsers() {
        int c = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn ngắt kết nối toàn bộ người dùng online?", "Ngắt tất cả", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            server.disconnectAll("Quản trị viên đã ngắt toàn bộ kết nối máy chủ.");
            refreshUserTable();
        }
    }

    // =========================================================================
    // ROOM MANAGEMENT ACTIONS
    // =========================================================================
    private void updateRoomListUI() {
        String currentSelection = lstRooms.getSelectedValue();
        roomListModel.clear();

        for (String rName : server.getRoomManager().getRoomNames()) {
            roomListModel.addElement(rName);
        }

        if (currentSelection != null && roomListModel.contains(currentSelection)) {
            lstRooms.setSelectedValue(currentSelection, true);
        } else if (!roomListModel.isEmpty()) {
            lstRooms.setSelectedIndex(0);
        }

        // Cập nhật thẻ thống kê
        lblValRooms.setText(String.valueOf(server.getRoomManager().getRoomNames().size()));

        // Cập nhật dropdown bên tab Broadcast
        updateBroadcastRoomTarget();
    }

    private void updateSelectedRoomDetails() {
        String rName = lstRooms.getSelectedValue();
        if (rName == null) {
            lblSelectedRoomTitle.setText("Chưa chọn phòng");
            roomMembersModel.setRowCount(0);
            return;
        }

        lblSelectedRoomTitle.setText("Chi Tiết Phòng: " + rName);
        roomMembersModel.setRowCount(0);

        Room room = server.getRoomManager().get(rName);
        if (room != null) {
            int stt = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (ClientHandler m : room.getMembers()) {
                roomMembersModel.addRow(new Object[]{
                    stt++, m.getUsername(), m.getRemoteAddress(), m.getConnectedTime().format(dtf)
                });
            }
        }
    }

    private void createRoomDialog() {
        String name = JOptionPane.showInputDialog(this, "Nhập tên phòng mới muốn tạo:", "Tạo phòng chat", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.isBlank()) {
            name = name.trim();
            server.getRoomManager().getOrCreate(name);
            updateRoomListUI();
            logLocal("ROOM", "Quản trị viên đã tạo phòng chat mới: " + name);
        }
    }

    private void cleanupEmptyRooms() {
        for (String name : server.getRoomManager().getRoomNames()) {
            if (!Protocol.DEFAULT_ROOM.equals(name)) {
                server.getRoomManager().removeIfEmpty(name);
            }
        }
        updateRoomListUI();
        logLocal("ROOM", "Đã dọn dẹp các phòng chat rỗng.");
    }

    private void sendRoomNotification() {
        String rName = lstRooms.getSelectedValue();
        if (rName == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phòng để gửi thông báo!", "Chưa chọn phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String msg = txtRoomMsg.getText().trim();
        if (msg.isEmpty()) {
            return;
        }

        Message m = new Message(MessageType.SYSTEM, "ADMIN", "[THÔNG BÁO QUẢN TRỊ]: " + msg, rName);
        server.broadcastToRoom(rName, m);
        txtRoomMsg.setText("");
        logLocal("SYSTEM", "[Phòng " + rName + "] ADMIN: " + msg);
    }

    private void updateBroadcastRoomTarget() {
        if (cmbBroadcastTarget == null) return;
        String cur = (String) cmbBroadcastTarget.getSelectedItem();
        cmbBroadcastTarget.removeAllItems();
        cmbBroadcastTarget.addItem("Toàn bộ hệ thống (Tất cả người dùng & phòng)");
        for (String r : server.getRoomManager().getRoomNames()) {
            cmbBroadcastTarget.addItem("Chỉ phòng: " + r);
        }
        if (cur != null) {
            cmbBroadcastTarget.setSelectedItem(cur);
        }
    }

    private void executeBroadcast() {
        String content = txtBroadcastMessage.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung thông báo trước khi phát!", "Nội dung trống", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int targetIndex = cmbBroadcastTarget.getSelectedIndex();
        if (targetIndex <= 0) {
            // Toàn hệ thống
            Message broadcastMsg = new Message(MessageType.SYSTEM, "ADMIN", "📢 [THÔNG BÁO TOÀN HỆ THỐNG]: " + content, Protocol.DEFAULT_ROOM);
            server.broadcast(broadcastMsg);
            JOptionPane.showMessageDialog(this, "Đã phát thông báo tới toàn bộ người dùng đang online!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String selectedItem = (String) cmbBroadcastTarget.getSelectedItem();
            String roomName = selectedItem.replace("Chỉ phòng: ", "").trim();
            Message roomMsg = new Message(MessageType.SYSTEM, "ADMIN", "📢 [THÔNG BÁO PHÒNG]: " + content, roomName);
            server.broadcastToRoom(roomName, roomMsg);
            JOptionPane.showMessageDialog(this, "Đã phát thông báo tới phòng '" + roomName + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =========================================================================
    // LOGS CONSOLE ACTIONS
    // =========================================================================
    private void clearLogs() {
        allLogs.clear();
        txtLogConsole.setText("");
    }

    private void exportLogsToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("ChatServer_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(f)) {
                for (LogEntry entry : allLogs) {
                    fw.write("[" + entry.time + "] [" + entry.level + "] " + entry.message + "\n");
                }
                JOptionPane.showMessageDialog(this, "Đã xuất nhật ký thành công: " + f.getAbsolutePath(), "Xuất thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi ghi file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyLogFilter() {
        String filter = (String) cmbLogFilter.getSelectedItem();
        String search = txtSearchLog.getText().trim().toLowerCase();

        txtLogConsole.setText("");
        for (LogEntry entry : allLogs) {
            if (matchesFilter(entry, filter, search)) {
                appendLogStyled(entry.time, entry.level, entry.message);
            }
        }
    }

    private boolean matchesFilter(LogEntry entry, String filter, String search) {
        if (!"Tất cả sự kiện".equals(filter) && !entry.level.equalsIgnoreCase(filter)) {
            return false;
        }
        if (!search.isEmpty() && !entry.message.toLowerCase().contains(search) && !entry.level.toLowerCase().contains(search)) {
            return false;
        }
        return true;
    }

    private void logLocal(String level, String msg) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        server.log(level, msg);
    }

    private void appendLogStyled(String time, String level, String message) {
        SimpleAttributeSet timeAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(timeAttr, new Color(148, 163, 184)); // Slate 400

        SimpleAttributeSet levelAttr = new SimpleAttributeSet();
        StyleConstants.setBold(levelAttr, true);
        Color levelColor = switch (level.toUpperCase()) {
            case "INFO" -> new Color(56, 189, 248);       // Sky 400
            case "AUTH" -> new Color(52, 211, 153);       // Emerald 400
            case "CHAT" -> new Color(103, 232, 249);      // Cyan 300
            case "ROOM" -> new Color(167, 139, 250);      // Purple 400
            case "VOICE", "VIDEO" -> new Color(244, 114, 182); // Pink 400
            case "WARN" -> new Color(251, 191, 36);       // Amber 400
            case "ERROR" -> new Color(248, 113, 113);     // Red 400
            case "SYSTEM" -> new Color(253, 224, 71);     // Yellow 300
            default -> Color.WHITE;
        };
        StyleConstants.setForeground(levelAttr, levelColor);

        SimpleAttributeSet msgAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(msgAttr, new Color(241, 245, 249));

        try {
            logDocument.insertString(logDocument.getLength(), "[" + time + "] ", timeAttr);
            logDocument.insertString(logDocument.getLength(), "[" + level + "] ", levelAttr);
            logDocument.insertString(logDocument.getLength(), message + "\n", msgAttr);

            if (chkAutoScroll != null && chkAutoScroll.isSelected()) {
                txtLogConsole.setCaretPosition(logDocument.getLength());
            }
        } catch (BadLocationException ignored) {
        }
    }

    // =========================================================================
    // SERVER LISTENER IMPLEMENTATION
    // =========================================================================
    @Override
    public void onLog(String timestamp, String level, String message) {
        SwingUtilities.invokeLater(() -> {
            LogEntry entry = new LogEntry(timestamp, level, message);
            allLogs.add(entry);

            String filter = (String) cmbLogFilter.getSelectedItem();
            String search = txtSearchLog.getText().trim().toLowerCase();
            if (matchesFilter(entry, filter, search)) {
                appendLogStyled(timestamp, level, message);
            }
        });
    }

    @Override
    public void onStatusChanged(boolean running, String message) {
        SwingUtilities.invokeLater(() -> {
            updateStatusUI(running);
            btnStart.setEnabled(!running);
            btnStop.setEnabled(running);
        });
    }

    @Override
    public void onClientConnected(String address) {
        // Log đã tự động phát, cập nhật stats
        SwingUtilities.invokeLater(this::refreshUserTable);
    }

    @Override
    public void onClientLoggedIn(ClientHandler client) {
        SwingUtilities.invokeLater(() -> {
            refreshUserTable();
            updateSelectedRoomDetails();
        });
    }

    @Override
    public void onClientDisconnected(String username, String address) {
        SwingUtilities.invokeLater(() -> {
            refreshUserTable();
            updateSelectedRoomDetails();
        });
    }

    @Override
    public void onRoomUpdated() {
        SwingUtilities.invokeLater(() -> {
            updateRoomListUI();
            updateSelectedRoomDetails();
        });
    }

    @Override
    public void onStatsUpdated(int onlineUsers, int roomCount, int activeThreads, int queueSize) {
        SwingUtilities.invokeLater(() -> {
            lblValUsers.setText(String.valueOf(onlineUsers));
            lblValRooms.setText(String.valueOf(roomCount));
            lblValThreads.setText(activeThreads + " / " + ChatServer.MAX_THREADS);
        });
    }

    // =========================================================================
    // TIMERS & METRICS
    // =========================================================================
    private void setupTimers() {
        // Uptime Timer (mỗi 1 giây)
        uptimeTimer = new Timer(1000, e -> {
            if (server.isRunning() && serverStartTime != null) {
                Duration d = Duration.between(serverStartTime, LocalDateTime.now());
                long h = d.toHours();
                long m = d.toMinutesPart();
                long s = d.toSecondsPart();
                lblUptime.setText(String.format("⏱ %02d:%02d:%02d", h, m, s));
            }
        });
        uptimeTimer.start();

        // Memory Timer (mỗi 2 giây)
        memoryTimer = new Timer(2000, e -> updateMemoryStats());
        memoryTimer.start();
    }

    private void updateMemoryStats() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        long max = rt.maxMemory();

        long usedMb = used / (1024 * 1024);
        long maxMb = max / (1024 * 1024);
        int percent = (int) ((used * 100) / max);

        lblValMemory.setText(usedMb + " MB / " + maxMb + " MB");
        barMemory.setValue(percent);
        barMemory.setString(percent + "% (" + usedMb + "MB)");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private static int parsePort(String text, int defaultPort) {
        try {
            int p = Integer.parseInt(text.trim());
            return (p > 1024 && p <= 65535) ? p : defaultPort;
        } catch (Exception e) {
            return defaultPort;
        }
    }

    private static String detectLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static JButton createStyledButton(String text, Color bg, Color hoverBg, javax.swing.Icon icon) {
        JButton btn = new JButton(text, icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled()) {
                    g2.setColor(getModel().isRollover() ? hoverBg : bg);
                } else {
                    g2.setColor(new Color(203, 213, 225));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD_13);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private static JButton createMiniButton(String text, java.awt.event.ActionListener l) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(COLOR_TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        btn.addActionListener(l);
        return btn;
    }

    private static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR_13);
        table.setRowHeight(28);
        table.setGridColor(new Color(241, 245, 249));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(219, 234, 254)); // Blue 100
        table.setSelectionForeground(new Color(30, 58, 138));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD_13);
        header.setBackground(COLOR_BG_HEADER);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(10, 32));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
    }

    /**
     * Lớp hỗ trợ panel bo góc hiện đại
     */
    private static class RoundPanel extends JPanel {
        private final int radius;

        public RoundPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(COLOR_CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class LogEntry {
        final String time;
        final String level;
        final String message;

        LogEntry(String time, String level, String message) {
            this.time = time;
            this.level = level;
            this.message = message;
        }
    }
}
