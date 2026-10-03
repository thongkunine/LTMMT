package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/**
 * Trình vẽ Icon Vector 2D cho ứng dụng Chat (Đảm bảo 100% hiển thị sắc nét trên mọi màn hình/HĐH, không bị lỗi ô vuông Unicode)
 */
public class ClientIcons {

    /**
     * Icon sấm sét ngang (Horizontal Lightning Bolt)
     */
    public static Icon getLightningIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                Path2D path = new Path2D.Double();
                double w = size;
                double h = size;

                path.moveTo(x + w * 0.1, y + h * 0.45);
                path.lineTo(x + w * 0.55, y + h * 0.15);
                path.lineTo(x + w * 0.45, y + h * 0.45);
                path.lineTo(x + w * 0.9, y + h * 0.45);
                path.lineTo(x + w * 0.4, y + h * 0.85);
                path.lineTo(x + w * 0.5, y + h * 0.55);
                path.closePath();

                g2.fill(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon gọi thoại (Telephone Handset)
     */
    public static Icon getPhoneIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                Path2D path = new Path2D.Double();
                double w = size;
                double h = size;

                path.moveTo(x + w * 0.25, y + h * 0.2);
                path.curveTo(x + w * 0.4, y + h * 0.2, x + w * 0.6, y + h * 0.4, x + w * 0.8, y + h * 0.7);
                path.curveTo(x + w * 0.85, y + h * 0.8, x + w * 0.75, y + h * 0.9, x + w * 0.6, y + h * 0.85);
                path.curveTo(x + w * 0.45, y + h * 0.75, x + w * 0.25, y + h * 0.55, x + w * 0.15, y + h * 0.4);
                path.curveTo(x + w * 0.1, y + h * 0.25, x + w * 0.2, y + h * 0.15, x + w * 0.25, y + h * 0.2);
                path.closePath();

                g2.fill(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon gọi video (Camera Icon)
     */
    public static Icon getVideoIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double w = size;
                double h = size;

                // Thân camera
                g2.fillRoundRect((int) (x + w * 0.1), (int) (y + h * 0.25), (int) (w * 0.55), (int) (h * 0.5), 4, 4);

                // Ống kính tam giác
                Path2D path = new Path2D.Double();
                path.moveTo(x + w * 0.68, y + h * 0.35);
                path.lineTo(x + w * 0.92, y + h * 0.22);
                path.lineTo(x + w * 0.92, y + h * 0.78);
                path.lineTo(x + w * 0.68, y + h * 0.65);
                path.closePath();

                g2.fill(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon hệ thống chat (Speech Bubble)
     */
    public static Icon getChatAppIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double w = size;
                double h = size;

                // Bong bóng chat chính
                g2.fillRoundRect((int) (x + w * 0.1), (int) (y + h * 0.15), (int) (w * 0.8), (int) (h * 0.55), 8, 8);

                // Đuôi chỉ bong bóng chat
                Path2D path = new Path2D.Double();
                path.moveTo(x + w * 0.25, y + h * 0.7);
                path.lineTo(x + w * 0.15, y + h * 0.9);
                path.lineTo(x + w * 0.45, y + h * 0.7);
                path.closePath();

                g2.fill(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon danh sách phòng (Folder/Pin Icon)
     */
    public static Icon getRoomListIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double w = size;
                double h = size;

                // Hình ghim / danh sách
                g2.fillRoundRect((int) (x + w * 0.15), (int) (y + h * 0.2), (int) (w * 0.7), (int) (h * 0.65), 4, 4);
                g2.setColor(Color.WHITE);
                g2.fillRect((int) (x + w * 0.25), (int) (y + h * 0.35), (int) (w * 0.5), 2);
                g2.fillRect((int) (x + w * 0.25), (int) (y + h * 0.5), (int) (w * 0.5), 2);
                g2.fillRect((int) (x + w * 0.25), (int) (y + h * 0.65), (int) (w * 0.3), 2);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon người dùng online (Green Glowing Dot)
     */
    public static Icon getOnlineUserIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                double w = size;
                double h = size;

                g2.setColor(color);
                g2.fillOval((int) (x + w * 0.1), (int) (y + h * 0.1), (int) (w * 0.8), (int) (h * 0.8));

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon gửi file (File Attachment)
     */
    public static Icon getFileIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double w = size;
                double h = size;

                g2.fillRoundRect((int) (x + w * 0.2), (int) (y + h * 0.15), (int) (w * 0.6), (int) (h * 0.75), 4, 4);
                g2.setColor(Color.WHITE);
                g2.fillRect((int) (x + w * 0.35), (int) (y + h * 0.35), (int) (w * 0.3), 2);
                g2.fillRect((int) (x + w * 0.35), (int) (y + h * 0.5), (int) (w * 0.3), 2);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon tạo phòng (Plus Icon)
     */
    public static Icon getAddIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = x + size / 2;
                int cy = y + size / 2;
                int r = size / 3;

                g2.drawLine(cx - r, cy, cx + r, cy);
                g2.drawLine(cx, cy - r, cx, cy + r);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon rời phòng (Leave / Exit Icon)
     */
    public static Icon getLeaveIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                double w = size;
                double h = size;

                // Khung cửa
                g2.drawRect((int) (x + w * 0.15), (int) (y + h * 0.15), (int) (w * 0.4), (int) (h * 0.7));

                // Mũi tên chỉ ra ngoài
                int arrowY = (int) (y + h * 0.5);
                g2.drawLine((int) (x + w * 0.4), arrowY, (int) (x + w * 0.85), arrowY);
                Path2D head = new Path2D.Double();
                head.moveTo(x + w * 0.7, y + h * 0.35);
                head.lineTo(x + w * 0.88, y + h * 0.5);
                head.lineTo(x + w * 0.7, y + h * 0.65);
                g2.draw(head);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon Microphone (Mic On / Muted)
     */
    public static Icon getMicIcon(int size, Color color, boolean muted) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                double w = size;
                double h = size;

                // Thân mic
                g2.fillRoundRect((int) (x + w * 0.35), (int) (y + h * 0.15), (int) (w * 0.3), (int) (h * 0.45), 6, 6);
                // Vòng giữ mic
                g2.drawArc((int) (x + w * 0.25), (int) (y + h * 0.3), (int) (w * 0.5), (int) (h * 0.4), 180, 180);
                // Chân mic
                g2.drawLine((int) (x + w * 0.5), (int) (y + h * 0.7), (int) (x + w * 0.5), (int) (y + h * 0.85));

                if (muted) {
                    g2.setColor(new Color(220, 38, 38));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) (x + w * 0.15), (int) (y + h * 0.15), (int) (x + w * 0.85), (int) (y + h * 0.85));
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon Loa ngoài / Speaker (Speaker On / Muted)
     */
    public static Icon getSpeakerIcon(int size, Color color, boolean muted) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                double w = size;
                double h = size;

                // Thân loa
                Path2D speaker = new Path2D.Double();
                speaker.moveTo(x + w * 0.2, y + h * 0.35);
                speaker.lineTo(x + w * 0.4, y + h * 0.35);
                speaker.lineTo(x + w * 0.65, y + h * 0.15);
                speaker.lineTo(x + w * 0.65, y + h * 0.85);
                speaker.lineTo(x + w * 0.4, y + h * 0.65);
                speaker.lineTo(x + w * 0.2, y + h * 0.65);
                speaker.closePath();
                g2.fill(speaker);

                if (muted) {
                    g2.setColor(new Color(220, 38, 38));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) (x + w * 0.15), (int) (y + h * 0.15), (int) (x + w * 0.85), (int) (y + h * 0.85));
                } else {
                    // Sóng âm thanh
                    g2.drawArc((int) (x + w * 0.5), (int) (y + h * 0.3), (int) (w * 0.35), (int) (h * 0.4), -60, 120);
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Icon Camera / Cam (Cam On / Muted)
     */
    public static Icon getCamOffIcon(int size, Color color, boolean off) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double w = size;
                double h = size;

                g2.fillRoundRect((int) (x + w * 0.1), (int) (y + h * 0.25), (int) (w * 0.55), (int) (h * 0.5), 4, 4);
                Path2D path = new Path2D.Double();
                path.moveTo(x + w * 0.68, y + h * 0.35);
                path.lineTo(x + w * 0.92, y + h * 0.22);
                path.lineTo(x + w * 0.92, y + h * 0.78);
                path.lineTo(x + w * 0.68, y + h * 0.65);
                path.closePath();
                g2.fill(path);

                if (off) {
                    g2.setColor(new Color(220, 38, 38));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) (x + w * 0.1), (int) (y + h * 0.15), (int) (x + w * 0.9), (int) (y + h * 0.85));
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }
}
