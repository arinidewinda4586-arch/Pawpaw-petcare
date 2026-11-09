package pawpaw;

import java.awt.*;
import javax.swing.*;

/** Halaman Adopsi Hewan: filter kategori dan kartu hewan yang menyesuaikan lebar jendela. */
public final class AdoptionPage extends HalamanDasar {
    private static final int CARD_HEIGHT = 392;
    private static final int GAP = 18;

    private final String[] kategori = { "Semua", "Kucing", "Anjing", "Kelinci", "Lainnya" };
    private final JButton[] pills = new JButton[kategori.length];
    private final JPanel pillRow = new JPanel();
    private final JLabel count = MainApp.label(" ", 15, MainApp.MUTED, false);
    private final JPanel grid = new JPanel();
    private String dipilih = "Semua";
    private int cols = 3;
    private int cards;
    private boolean empty;

    public AdoptionPage(MainApp app) {
        super(app, "Adopsi", "ADOPSI HEWAN", "Temukan sahabat baru yang membutuhkan rumah penuh kasih.");

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
        layoutGrid(1000);
    }

    @Override
    protected void onWidth(int width) {
        layoutGrid(width);
    }

    private void layoutGrid(int width) {
        cols = width >= 980 ? 3 : width >= 560 ? 2 : 1;
        if (empty) {
            grid.setLayout(new BorderLayout());
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        } else {
            grid.setLayout(new GridLayout(0, cols, GAP, GAP));
            int rows = Math.max(1, (cards + cols - 1) / cols);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * CARD_HEIGHT + (rows - 1) * GAP));
        }

        boolean wide = width >= 640;
        pillRow.removeAll();
        pillRow.setLayout(wide ? new BoxLayout(pillRow, BoxLayout.X_AXIS) : new GridLayout(0, 3, 8, 8));
        for (JButton pill : pills) {
            // Border dipasang lebih dulu supaya lebar pill dihitung dengan benar (teks tidak terpotong).
            pill.setBorder(BorderFactory.createEmptyBorder(10, wide ? 24 : 10, 10, wide ? 24 : 10));
            pill.setPreferredSize(null); // lebar mengikuti teks, jadi tidak terpotong
            pillRow.add(pill);
            if (wide)
                pillRow.add(Box.createHorizontalStrut(8));
        }
        if (wide)
            pillRow.add(Box.createHorizontalGlue());
        int pillRows = wide ? 1 : 2;
        pillRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, pillRows * 44 + (pillRows - 1) * 8));
        grid.revalidate();
        pillRow.revalidate();
    }

    // Menampilkan kartu sesuai kategori terpilih
    private void rebuild() {
        for (int i = 0; i < kategori.length; i++) {
            boolean aktif = kategori[i].equals(dipilih);
            pills[i].setBackground(aktif ? MainApp.NAVY : MainApp.PIL);
            pills[i].setForeground(aktif ? Color.WHITE : MainApp.INK);
        }
        grid.removeAll();
        cards = 0;
        for (String[] hewan : DataStore.daftarHewan) {
            if (!dipilih.equals("Semua") && !hewan[1].equals(dipilih))
                continue;
            grid.add(card(hewan));
            cards++;
        }
        empty = cards == 0;
        if (empty) {
            grid.add(MainApp.label("Belum ada hewan pada kategori ini.", 15, MainApp.MUTED, false),
                    BorderLayout.NORTH);
            count.setText("0 hewan ditemukan");
        } else {
            count.setText(cards + " hewan ditemukan" + (dipilih.equals("Semua") ? "" : " • " + dipilih));
        }
        layoutGrid(viewportWidth());
        grid.revalidate();
        grid.repaint();
    }

    private JPanel card(String[] hewan) {
        boolean tersedia = hewan[5].equals("Tersedia");
        JPanel card = MainApp.roundedPanel(DataStore.warnaJenis(hewan[1]));
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(260, CARD_HEIGHT));

        card.add(new MainApp.PhotoView(hewan[6], 240, 150, 18), BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = MainApp.label(hewan[0], 21, MainApp.INK, true);
        name.setAlignmentX(LEFT_ALIGNMENT);
        info.add(name);
        info.add(Box.createVerticalStrut(6));
        info.add(tersedia ? MainApp.chip("Tersedia", MainApp.SUCCESS_BG, MainApp.SUCCESS)
                : MainApp.chip("Sudah Diadopsi", MainApp.DANGER_BG, MainApp.DANGER));
        info.add(Box.createVerticalStrut(10));
        String[] rows = { "Ras: " + hewan[2], "Gender: " + hewan[3], "Umur: " + hewan[4] };
        for (String row : rows) {
            JLabel l = MainApp.label(row, 14, new Color(52, 68, 84), false);
            l.setAlignmentX(LEFT_ALIGNMENT);
            info.add(l);
            info.add(Box.createVerticalStrut(3));
        }
        card.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        JButton detail = MainApp.button("Lihat Detail", Color.WHITE, MainApp.NAVY, 13, 10, MainApp.NAVY);
        detail.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        detail.getAccessibleContext().setAccessibleName("Lihat detail " + hewan[0]);
        detail.addActionListener(e -> app.showDetail(hewan));
        JButton ajukan = MainApp.button(tersedia ? "Ajukan Adopsi" : "Tidak Tersedia", MainApp.BLUE, Color.WHITE,
                13, 10, null);
        ajukan.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        ajukan.setEnabled(tersedia);
        ajukan.getAccessibleContext().setAccessibleName("Ajukan adopsi " + hewan[0]);
        if (tersedia)
            ajukan.addActionListener(e -> AdoptionForm.show(app, hewan, null));
        buttons.add(detail);
        buttons.add(ajukan);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }
}
