package server;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Trình tạo Icon Vector 2D thuần túy cho Server GUI.
 * Đảm bảo hiển thị sắc nét 100% trên mọi tỷ lệ DPI màn hình, không phụ thuộc font/ảnh ngoài.
 */
public class ServerIcons {

    public static Icon getPlayIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                Path2D path = new Path2D.Double();
                path.moveTo(x + size * 0.25, y + size * 0.15);
                path.lineTo(x + size * 0.85, y + size * 0.5);
                path.lineTo(x + size * 0.25, y + size * 0.85);
                path.closePath();
                g2.fill(path);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getStopIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int pad = (int) (size * 0.2);
                g2.fillRoundRect(x + pad, y + pad, size - pad * 2, size - pad * 2, 4, 4);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getRestartIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float) (size * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = (int) (size * 0.18);
                g2.drawArc(x + pad, y + pad, size - pad * 2, size - pad * 2, 45, 270);
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(x + size * 0.7, y + size * 0.15);
                arrow.lineTo(x + size * 0.88, y + size * 0.32);
                arrow.lineTo(x + size * 0.65, y + size * 0.45);
                arrow.closePath();
                g2.fill(arrow);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getServerIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float) (size * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // 2 ngăn server rack
                int w = (int) (size * 0.84);
                int h1 = (int) (size * 0.32);
                int rx = x + (int) (size * 0.08);

                g2.drawRoundRect(rx, y + (int) (size * 0.12), w, h1, 6, 6);
                g2.drawRoundRect(rx, y + (int) (size * 0.54), w, h1, 6, 6);

                // LED lights
                g2.fillOval(rx + (int) (w * 0.15), y + (int) (size * 0.24), (int) (size * 0.09), (int) (size * 0.09));
                g2.fillOval(rx + (int) (w * 0.32), y + (int) (size * 0.24), (int) (size * 0.09), (int) (size * 0.09));

                g2.fillOval(rx + (int) (w * 0.15), y + (int) (size * 0.66), (int) (size * 0.09), (int) (size * 0.09));
                g2.fillOval(rx + (int) (w * 0.32), y + (int) (size * 0.66), (int) (size * 0.09), (int) (size * 0.09));

                // Slot bars
                g2.drawLine(rx + (int) (w * 0.55), y + (int) (size * 0.28), rx + (int) (w * 0.85), y + (int) (size * 0.28));
                g2.drawLine(rx + (int) (w * 0.55), y + (int) (size * 0.70), rx + (int) (w * 0.85), y + (int) (size * 0.70));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getUsersIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                // Người bên trái (chính)
                int headR = (int) (size * 0.22);
                g2.fillOval(x + (int) (size * 0.25), y + (int) (size * 0.12), headR, headR);

                Path2D body = new Path2D.Double();
                body.moveTo(x + size * 0.15, y + size * 0.85);
                body.curveTo(x + size * 0.15, y + size * 0.52, x + size * 0.57, y + size * 0.52, x + size * 0.57, y + size * 0.85);
                body.closePath();
                g2.fill(body);

                // Người bên phải (phụ)
                g2.fillOval(x + (int) (size * 0.58), y + (int) (size * 0.20), (int) (headR * 0.8), (int) (headR * 0.8));
                Path2D body2 = new Path2D.Double();
                body2.moveTo(x + size * 0.50, y + size * 0.85);
                body2.curveTo(x + size * 0.50, y + size * 0.58, x + size * 0.88, y + size * 0.58, x + size * 0.88, y + size * 0.85);
                body2.closePath();
                g2.fill(body2);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getRoomsIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                // Chat bubble
                Path2D bubble = new Path2D.Double();
                bubble.moveTo(x + size * 0.15, y + size * 0.2);
                bubble.lineTo(x + size * 0.85, y + size * 0.2);
                bubble.curveTo(x + size * 0.92, y + size * 0.2, x + size * 0.92, y + size * 0.65, x + size * 0.85, y + size * 0.65);
                bubble.lineTo(x + size * 0.45, y + size * 0.65);
                bubble.lineTo(x + size * 0.25, y + size * 0.88);
                bubble.lineTo(x + size * 0.25, y + size * 0.65);
                bubble.lineTo(x + size * 0.15, y + size * 0.65);
                bubble.curveTo(x + size * 0.08, y + size * 0.65, x + size * 0.08, y + size * 0.2, x + size * 0.15, y + size * 0.2);
                bubble.closePath();
                g2.fill(bubble);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getThreadIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                // Tia sét (Lightning)
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(x + size * 0.55, y + size * 0.1);
                bolt.lineTo(x + size * 0.2, y + size * 0.52);
                bolt.lineTo(x + size * 0.48, y + size * 0.52);
                bolt.lineTo(x + size * 0.42, y + size * 0.9);
                bolt.lineTo(x + size * 0.82, y + size * 0.45);
                bolt.lineTo(x + size * 0.54, y + size * 0.45);
                bolt.closePath();
                g2.fill(bolt);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getMemoryIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float) (size * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Microchip body
                int pad = (int) (size * 0.25);
                g2.drawRoundRect(x + pad, y + pad, size - pad * 2, size - pad * 2, 4, 4);

                // Pins
                int pinLen = (int) (size * 0.14);
                // Top & Bottom pins
                g2.drawLine(x + (int)(size * 0.38), y + pad, x + (int)(size * 0.38), y + pad - pinLen);
                g2.drawLine(x + (int)(size * 0.62), y + pad, x + (int)(size * 0.62), y + pad - pinLen);
                g2.drawLine(x + (int)(size * 0.38), y + size - pad, x + (int)(size * 0.38), y + size - pad + pinLen);
                g2.drawLine(x + (int)(size * 0.62), y + size - pad, x + (int)(size * 0.62), y + size - pad + pinLen);

                // Left & Right pins
                g2.drawLine(x + pad, y + (int)(size * 0.38), x + pad - pinLen, y + (int)(size * 0.38));
                g2.drawLine(x + pad, y + (int)(size * 0.62), x + pad - pinLen, y + (int)(size * 0.62));
                g2.drawLine(x + size - pad, y + (int)(size * 0.38), x + size - pad + pinLen, y + (int)(size * 0.38));
                g2.drawLine(x + size - pad, y + (int)(size * 0.62), x + size - pad + pinLen, y + (int)(size * 0.62));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getBroadcastIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                // Megaphone
                Path2D horn = new Path2D.Double();
                horn.moveTo(x + size * 0.2, y + size * 0.35);
                horn.lineTo(x + size * 0.45, y + size * 0.35);
                horn.lineTo(x + size * 0.75, y + size * 0.15);
                horn.lineTo(x + size * 0.75, y + size * 0.75);
                horn.lineTo(x + size * 0.45, y + size * 0.55);
                horn.lineTo(x + size * 0.2, y + size * 0.55);
                horn.closePath();
                g2.fill(horn);

                // Handle
                g2.setStroke(new BasicStroke((float)(size * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + (int)(size * 0.35), y + (int)(size * 0.55), x + (int)(size * 0.28), y + (int)(size * 0.85));

                // Sound waves
                g2.drawArc(x + (int)(size * 0.7), y + (int)(size * 0.25), (int)(size * 0.2), (int)(size * 0.4), -40, 80);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getLogIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Console / Terminal window
                g2.drawRoundRect(x + (int)(size * 0.12), y + (int)(size * 0.15), (int)(size * 0.76), (int)(size * 0.7), 6, 6);
                // Prompt > _
                g2.drawLine(x + (int)(size * 0.25), y + (int)(size * 0.38), x + (int)(size * 0.38), y + (int)(size * 0.5));
                g2.drawLine(x + (int)(size * 0.38), y + (int)(size * 0.5), x + (int)(size * 0.25), y + (int)(size * 0.62));
                g2.drawLine(x + (int)(size * 0.45), y + (int)(size * 0.62), x + (int)(size * 0.65), y + (int)(size * 0.62));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getSettingsIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = x + size / 2;
                int cy = y + size / 2;
                int r = (int)(size * 0.26);
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // 8 cogs
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4.0;
                    int x1 = (int)(cx + Math.cos(angle) * (r * 0.9));
                    int y1 = (int)(cy + Math.sin(angle) * (r * 0.9));
                    int x2 = (int)(cx + Math.cos(angle) * (r * 1.45));
                    int y2 = (int)(cy + Math.sin(angle) * (r * 1.45));
                    g2.drawLine(x1, y1, x2, y2);
                }

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getKickIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Circle with slash (ban sign)
                int pad = (int)(size * 0.15);
                int d = size - pad * 2;
                g2.drawOval(x + pad, y + pad, d, d);
                g2.drawLine(x + (int)(size * 0.27), y + (int)(size * 0.27), x + (int)(size * 0.73), y + (int)(size * 0.73));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getClearIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Trash bin lid
                g2.drawLine(x + (int)(size * 0.2), y + (int)(size * 0.28), x + (int)(size * 0.8), y + (int)(size * 0.28));
                g2.drawArc(x + (int)(size * 0.4), y + (int)(size * 0.16), (int)(size * 0.2), (int)(size * 0.2), 0, 180);

                // Body
                Path2D body = new Path2D.Double();
                body.moveTo(x + size * 0.28, y + size * 0.28);
                body.lineTo(x + size * 0.32, y + size * 0.82);
                body.lineTo(x + size * 0.68, y + size * 0.82);
                body.lineTo(x + size * 0.72, y + size * 0.28);
                g2.draw(body);

                // Vertical lines
                g2.drawLine(x + (int)(size * 0.43), y + (int)(size * 0.38), x + (int)(size * 0.43), y + (int)(size * 0.72));
                g2.drawLine(x + (int)(size * 0.57), y + (int)(size * 0.38), x + (int)(size * 0.57), y + (int)(size * 0.72));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getExportIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Down arrow
                g2.drawLine(x + size / 2, y + (int)(size * 0.15), x + size / 2, y + (int)(size * 0.62));
                Path2D head = new Path2D.Double();
                head.moveTo(x + size * 0.32, y + size * 0.5);
                head.lineTo(x + size * 0.5, y + size * 0.68);
                head.lineTo(x + size * 0.68, y + size * 0.5);
                g2.draw(head);

                // Base tray
                Path2D tray = new Path2D.Double();
                tray.moveTo(x + size * 0.2, y + size * 0.7);
                tray.lineTo(x + size * 0.2, y + size * 0.85);
                tray.lineTo(x + size * 0.8, y + size * 0.85);
                tray.lineTo(x + size * 0.8, y + size * 0.7);
                g2.draw(tray);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getDatabaseIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke((float)(size * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int rx = x + (int)(size * 0.2);
                int rw = (int)(size * 0.6);
                int rh = (int)(size * 0.2);

                // Top ellipse
                g2.drawOval(rx, y + (int)(size * 0.15), rw, rh);

                // Middle layer
                g2.drawArc(rx, y + (int)(size * 0.42), rw, rh, 180, 180);

                // Bottom layer
                g2.drawArc(rx, y + (int)(size * 0.68), rw, rh, 180, 180);

                // Left and right side lines
                g2.drawLine(rx, y + (int)(size * 0.25), rx, y + (int)(size * 0.78));
                g2.drawLine(rx + rw, y + (int)(size * 0.25), rx + rw, y + (int)(size * 0.78));

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getRocketIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                // Rocket diagonal
                Path2D rocket = new Path2D.Double();
                rocket.moveTo(x + size * 0.85, y + size * 0.15);
                rocket.curveTo(x + size * 0.55, y + size * 0.2, x + size * 0.35, y + size * 0.4, x + size * 0.3, y + size * 0.65);
                rocket.lineTo(x + size * 0.42, y + size * 0.7);
                rocket.curveTo(x + size * 0.6, y + size * 0.65, x + size * 0.8, y + size * 0.45, x + size * 0.85, y + size * 0.15);
                rocket.closePath();
                g2.fill(rocket);

                // Fin
                Path2D fin = new Path2D.Double();
                fin.moveTo(x + size * 0.3, y + size * 0.65);
                fin.lineTo(x + size * 0.15, y + size * 0.7);
                fin.lineTo(x + size * 0.25, y + size * 0.52);
                fin.closePath();
                g2.fill(fin);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon getStatusDotIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Halo glow ring
                Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
                g2.setColor(glow);
                g2.fillOval(x, y, size, size);

                // Inner bright dot
                g2.setColor(color);
                int pad = size / 4;
                g2.fillOval(x + pad, y + pad, size - pad * 2, size - pad * 2);

                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}
