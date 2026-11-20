package pawpaw.gui.common;

import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Width-tracking {@link Scrollable} card grid that reflows columns cleanly based on
 * viewport width, equalizes card heights per row, and renders a friendly empty-state card.
 */
public final class ResponsiveCardGrid extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    private final int minCardWidth;
    private final int hgap;
    private final int vgap;
    private final transient List<JComponent> cards = new ArrayList<>();

    private final JPanel emptyStateCard;
    private final JLabel emptyTitleLabel;
    private final JLabel emptySubtitleLabel;

    private int lastColumnCount = 1;
    private int lastLaidOutHeight = -1;

    public ResponsiveCardGrid(int minCardWidth, int hgap, int vgap) {
        this.minCardWidth = Math.max(80, minCardWidth);
        this.hgap = Math.max(0, hgap);
        this.vgap = Math.max(0, vgap);

        setOpaque(false);
        setLayout(new ResponsiveGridLayout());

        emptyTitleLabel = UiKit.label("Belum ada data", Theme.heading(), Theme.INK);
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        emptySubtitleLabel = UiKit.label("Coba ubah filter atau kata kunci pencarian.", Theme.small(), Theme.MUTED);
        emptySubtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptySubtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        emptyStateCard = buildEmptyStateCard();
        add(emptyStateCard);
    }

    private JPanel buildEmptyStateCard() {
        JPanel card = UiKit.cardPanel(new BorderLayout());
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16));

        JLabel iconLabel = new JLabel(VectorIcons.of(VectorIcons.Kind.PAW, 30, Theme.MUTED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(iconLabel);
        inner.add(Box.createVerticalStrut(Theme.SPACE_8));
        inner.add(emptyTitleLabel);
        inner.add(Box.createVerticalStrut(Theme.SPACE_4));
        inner.add(emptySubtitleLabel);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    public void setCards(List<? extends JComponent> newCards) {
        cards.clear();
        removeAll();
        if (newCards != null) {
            for (JComponent card : newCards) {
                if (card != null) {
                    cards.add(card);
                    add(card);
                }
            }
        }
        if (cards.isEmpty()) {
            add(emptyStateCard);
        }
        lastLaidOutHeight = -1;
        revalidate();
        repaint();
    }

    public void setEmptyState(String title, String subtitle) {
        emptyTitleLabel.setText(title != null && !title.isBlank() ? title : "Belum ada data");
        emptySubtitleLabel.setText(subtitle != null ? subtitle : "");
        if (cards.isEmpty()) {
            lastLaidOutHeight = -1;
            revalidate();
            repaint();
        }
    }

    public int getCardCount() {
        return cards.size();
    }

    public int getColumnCount() {
        JViewport vp = findAncestorViewport(this);
        if (getWidth() > 0 || (vp != null && vp.getWidth() > 0)) {
            return computeColumnCount(resolveAvailableWidth(this));
        }
        return lastColumnCount;
    }

    private int resolveAvailableWidth(Container target) {
        Insets in = target.getInsets();
        int width = target.getWidth();
        if (width <= 0) {
            JViewport vp = findAncestorViewport(target);
            if (vp != null && vp.getWidth() > 0) {
                width = vp.getWidth();
            }
        }
        if (width <= 0) {
            width = minCardWidth * 2 + hgap + in.left + in.right;
        }
        return Math.max(1, width - in.left - in.right);
    }

    private static JViewport findAncestorViewport(Component comp) {
        Container vp = SwingUtilities.getAncestorOfClass(JViewport.class, comp);
        return vp instanceof JViewport viewport ? viewport : null;
    }

    private void updateLaidOutHeight(Container parent, int actualHeight) {
        if (lastLaidOutHeight != actualHeight) {
            lastLaidOutHeight = actualHeight;
            if (findAncestorViewport(parent) != null) {
                SwingUtilities.invokeLater(parent::revalidate);
            }
        }
    }

    private int computeColumnCount(int availW) {
        return Math.max(1, (availW + hgap) / (minCardWidth + hgap));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect != null ? Math.max(16, visibleRect.height - 16) : 64;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private final class ResponsiveGridLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int availW = resolveAvailableWidth(parent);
            if (cards.isEmpty()) {
                int emptyH = parent.getComponentCount() > 0
                    ? Math.max(140, parent.getComponent(0).getPreferredSize().height)
                    : 140;
                return new Dimension(minCardWidth + in.left + in.right, emptyH + in.top + in.bottom);
            }

            int cols = computeColumnCount(availW);
            int totalH = in.top + in.bottom;
            int n = cards.size();
            for (int rowStart = 0; rowStart < n; rowStart += cols) {
                int rowEnd = Math.min(n, rowStart + cols);
                int rowH = 0;
                for (int idx = rowStart; idx < rowEnd; idx++) {
                    rowH = Math.max(rowH, Math.max(40, cards.get(idx).getPreferredSize().height));
                }
                totalH += rowH;
                if (rowEnd < n) {
                    totalH += vgap;
                }
            }
            return new Dimension(availW + in.left + in.right, Math.max(40, totalH));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            return new Dimension(minCardWidth + in.left + in.right, 100 + in.top + in.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int availW = resolveAvailableWidth(parent);
            if (cards.isEmpty()) {
                lastColumnCount = computeColumnCount(availW);
                int emptyH = 140;
                if (parent.getComponentCount() > 0) {
                    Component empty = parent.getComponent(0);
                    empty.setSize(availW, Math.max(140, empty.getPreferredSize().height));
                    empty.validate();
                    emptyH = Math.max(140, empty.getPreferredSize().height);
                    empty.setBounds(in.left, in.top, availW, emptyH);
                    empty.validate();
                }
                updateLaidOutHeight(parent, emptyH + in.top + in.bottom);
                return;
            }

            int cols = computeColumnCount(availW);
            lastColumnCount = cols;

            int totalGaps = Math.max(0, cols - 1) * hgap;
            int usableW = Math.max(cols, availW - totalGaps);
            int baseColW = usableW / cols;
            int remainder = usableW % cols;

            int y = in.top;
            int n = cards.size();
            for (int rowStart = 0; rowStart < n; rowStart += cols) {
                int rowEnd = Math.min(n, rowStart + cols);
                int rowH = 0;
                for (int idx = rowStart; idx < rowEnd; idx++) {
                    int col = idx - rowStart;
                    int colW = baseColW + (col < remainder ? 1 : 0);
                    JComponent card = cards.get(idx);
                    if (card.getWidth() != colW) {
                        card.setSize(colW, Math.max(1, card.getPreferredSize().height));
                        card.validate();
                    }
                    rowH = Math.max(rowH, Math.max(40, card.getPreferredSize().height));
                }

                int x = in.left;
                for (int idx = rowStart; idx < rowEnd; idx++) {
                    int col = idx - rowStart;
                    int colW = baseColW + (col < remainder ? 1 : 0);
                    JComponent card = cards.get(idx);
                    card.setBounds(x, y, colW, rowH);
                    card.validate();
                    x += colW + hgap;
                }
                y += rowH;
                if (rowEnd < n) {
                    y += vgap;
                }
            }

            updateLaidOutHeight(parent, y + in.bottom);
        }
    }
}
