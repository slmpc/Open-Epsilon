package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public final class AdminFrame extends JFrame {
    private final IRCServer server;
    private final JLabel statusLeft = new JLabel();
    private final JLabel statusRight = new JLabel();
    private final Instant startedAt;

    public AdminFrame(IRCServer server) {
        super("Lumin Verification Server");
        this.server = server;
        this.startedAt = Instant.ofEpochMilli(server.getStartedAt());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 760));
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("总览", new DashboardPanel(server));
        tabs.addTab("用户", new UsersPanel(server));
        tabs.addTab("卡密", new CardsPanel(server));
        tabs.addTab("云配置", new CloudConfigPanel(server));
        tabs.addTab("会话", new SessionsPanel(server));
        tabs.addTab("Mod管理", new ModsPanel(server));
        tabs.addTab("控制台", new ConsolePanel(server));
        add(tabs, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        Timer timer = new Timer(1000, e -> refreshStatus());
        timer.setRepeats(true);
        timer.start();
        refreshStatus();
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu serverMenu = new JMenu("服务器");
        JMenuItem shutdown = new JMenuItem("关闭服务器");
        shutdown.addActionListener(e -> {
            server.shutdown();
            SwingUtilities.invokeLater(() -> dispose());
        });
        serverMenu.add(shutdown);
        bar.add(serverMenu);

        JMenu fileMenu = new JMenu("文件");
        JMenuItem exit = new JMenuItem("退出");
        exit.addActionListener(e -> {
            server.shutdown();
            System.exit(0);
        });
        fileMenu.add(exit);
        bar.add(fileMenu);

        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, p.getBackground().darker()),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        p.add(statusLeft, BorderLayout.WEST);
        p.add(statusRight, BorderLayout.EAST);
        return p;
    }

    private void refreshStatus() {
        Duration up = Duration.between(startedAt, Instant.now());
        long hours = up.toHours();
        long minutes = up.toMinutesPart();
        long seconds = up.toSecondsPart();
        statusLeft.setText("端口: " + server.getPort() + "    连接数: " + server.getConnectionCount());
        statusRight.setText("运行时间: " + hours + "h " + minutes + "m " + seconds + "s");
    }
}

