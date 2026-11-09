package pawpaw;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Halaman Toko: pencarian, filter kategori, dan kartu produk yang menambah ke keranjang. */
public final class TokoPage extends HalamanDasar {
    private static final int CARD_HEIGHT = 372;
    private static final int GAP = 18;

    private final String[] kategori = { "Semua", "Makanan", "Mainan", "Perawatan" };
    private final JButton[] pills = new JButton[kategori.length];
    private final JPanel toolbar = new JPanel(new GridBagLayout());
    private final JPanel pillRow = new JPanel();
    private final JTextField search = MainApp.textField("Cari produk, kategori, atau kebutuhan...");
    private final JButton cartButton = MainApp.button("", MainApp.BLUE, Color.WHITE, 15, 10, null);
    private final JButton historyButton = MainApp.button("Riwayat Pesanan", Color.WHITE, MainApp.NAVY, 15, 10,
            MainApp.NAVY);
    private final JLabel count = MainApp.label(" ", 15, MainApp.MUTED, false);
    private final JPanel grid = new JPanel();
    private String dipilih = "Semua";
    private int cards;
    private boolean empty;

    public TokoPage(MainApp app) {
        super(app, "Toko", "PAWPAW SHOP",
                "Temukan makanan, mainan, dan kebutuhan perawatan untuk hewan peliharaan.");

        toolbar.setOpaque(false);
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        search.getAccessibleContext().setAccessibleName("Cari produk");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuild();
            }
        });
        historyButton.addActionListener(e -> app.unavailable("Riwayat Pesanan"));
        cartButton.addActionListener(e -> app.showKeranjang());
        updateCartButton();
        content.add(toolbar);
        content.add(Box.createVerticalStrut(16));

        pillRow.setOpaque(false);
        pillRow.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < kategori.length; i++) {
            final String name = kategori[i];
            JButton pill = MainApp.button(name, MainApp.PIL, MainApp.INK, 15, 9, null);
            pill.getAccessibleContext().setAccessibleName("Kategori " + name);
            pill.addActionListener(e -> {
                dipilih = name;
                rebuild();
            });
            pills[i] = pill;
            pillRow.add(pill);
        }
        content.add(pillRow);
        content.add(Box.createVerticalStrut(16));
        count.setAlignmentX(LEFT_ALIGNMENT);
        content.add(count);
        content.add(Box.createVerticalStrut(16));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        content.add(grid);
        rebuild();
        layoutAll(1000);
    }

    @Override
    protected void onWidth(int width) {
        layoutAll(width);
    }

    private void layoutAll(int width) {
        // Bar pencarian: sejajar bila lebar, ditumpuk bila sempit.
        boolean wide = width >= 760;
        toolbar.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.weightx = 1;
        c.gridx = 0;
        c.insets = new Insets(0, 0, wide ? 0 : 10, wide ? 12 : 0);
        c.gridwidth = wide ? 1 : 2;
        toolbar.add(search, c);
        c.weightx = 0;
        c.gridwidth = 1;
        c.gridy = wide ? 0 : 1;
        c.gridx = wide ? 1 : 0;
        c.insets = new Insets(0, 0, 0, 8);
        toolbar.add(historyButton, c);
        c.gridx = wide ? 2 : 1;
        c.insets = new Insets(0, 0, 0, 0);
        toolbar.add(cartButton, c);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, wide ? 50 : 108));

        boolean pillsWide = width >= 640;
        pillRow.removeAll();
        pillRow.setLayout(pillsWide ? new BoxLayout(pillRow, BoxLayout.X_AXIS) : new GridLayout(0, 2, 8, 8));
        for (JButton pill : pills) {
            pill.setBorder(BorderFactory.createEmptyBorder(10, pillsWide ? 24 : 10, 10, pillsWide ? 24 : 10));
            pill.setPreferredSize(null);
            pillRow.add(pill);
            if (pillsWide)
                pillRow.add(Box.createHorizontalStrut(8));
        }
        if (pillsWide)
            pillRow.add(Box.createHorizontalGlue());
        int pillRows = pillsWide ? 1 : 2;
        pillRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, pillRows * 44 + (pillRows - 1) * 8));

        int cols = width >= 980 ? 3 : width >= 560 ? 2 : 1;
        if (empty) {
            grid.setLayout(new BorderLayout());
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        } else {
            grid.setLayout(new GridLayout(0, cols, GAP, GAP));
            int rows = Math.max(1, (cards + cols - 1) / cols);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * CARD_HEIGHT + (rows - 1) * GAP));
        }
        toolbar.revalidate();
        pillRow.revalidate();
        grid.revalidate();
    }

    private void updateCartButton() {
        int n = DataStore.jumlahItemKeranjang();
        cartButton.setText(n == 0 ? "Keranjang  →" : "Keranjang (" + n + ")  →");
        cartButton.getAccessibleContext().setAccessibleName("Buka keranjang, " + n + " item");
    }

    private void rebuild() {
        for (int i = 0; i < kategori.length; i++) {
            boolean aktif = kategori[i].equals(dipilih);
            pills[i].setBackground(aktif ? MainApp.NAVY : MainApp.PIL);
            pills[i].setForeground(aktif ? Color.WHITE : MainApp.INK);
        }
        String q = search.getText().trim().toLowerCase();
        grid.removeAll();
        cards = 0;
        for (Produk p : DataStore.daftarProduk) {
            if (!dipilih.equals("Semua") && !p.kategori.equals(dipilih))
                continue;
            String haystack = (p.nama + " " + p.label + " " + p.untuk + " " + p.kategori).toLowerCase();
            if (!q.isEmpty() && !haystack.contains(q))
                continue;
            grid.add(card(p));
            cards++;
        }
        empty = cards == 0;
        if (empty) {
            grid.add(MainApp.label("Produk tidak ditemukan. Coba kata kunci atau kategori lain.", 15, MainApp.MUTED,
                    false), BorderLayout.NORTH);
            count.setText("0 produk ditemukan");
        } else {
            count.setText(cards + " produk ditemukan" + (dipilih.equals("Semua") ? "" : " • " + dipilih));
        }
        layoutAll(viewportWidth());
        grid.repaint();
    }

    private JPanel card(Produk p) {
        JPanel card = MainApp.roundedPanel(MainApp.SKY);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(260, CARD_HEIGHT));

        JPanel photoBox = MainApp.roundedPanel(Color.WHITE);
        photoBox.setLayout(new BorderLayout());
        photoBox.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        photoBox.add(new JLabel(MainApp.image("shop/" + p.gambar, 210, 150), SwingConstants.CENTER),
                BorderLayout.CENTER);
        card.add(photoBox, BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = MainApp.label(p.nama, 19, MainApp.INK, true);
        name.setAlignmentX(LEFT_ALIGNMENT);
        info.add(name);
        info.add(Box.createVerticalStrut(6));
        info.add(MainApp.chip(p.label, MainApp.PIL, MainApp.NAVY));
        info.add(Box.createVerticalStrut(8));
        JLabel untuk = MainApp.label(p.untuk, 14, new Color(52, 68, 84), false);
        untuk.setAlignmentX(LEFT_ALIGNMENT);
        info.add(untuk);
        card.add(info, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(MainApp.label(DataStore.formatRupiah(p.harga), 18, MainApp.NAVY, true), BorderLayout.WEST);
        JButton add = MainApp.button("+ Tambah", MainApp.BLUE, Color.WHITE, 14, 9, null);
        add.getAccessibleContext().setAccessibleName("Tambah " + p.nama + " ke keranjang");
        add.addActionListener(e -> {
            DataStore.tambahKeKeranjang(p);
            updateCartButton();
            add.setText("✓ Ditambahkan");
            Timer t = new Timer(1200, ev -> add.setText("+ Tambah"));
            t.setRepeats(false);
            t.start();
        });
        bottom.add(add, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }
}
