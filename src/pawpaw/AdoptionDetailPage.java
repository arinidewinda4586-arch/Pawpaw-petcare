package pawpaw;

import java.awt.*;
import javax.swing.*;

/** Halaman Detail Adopsi: foto besar, data hewan, dan tombol Ajukan Adopsi. */
public final class AdoptionDetailPage extends HalamanDasar {
    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel photoCard;
    private final JPanel right = new JPanel();

    public AdoptionDetailPage(MainApp app, String[] hewan) {
        super(app, "Adopsi", "DETAIL ADOPSI", "Kenali " + hewan[0] + " sebelum memutuskan untuk mengadopsi.");
        boolean tersedia = hewan[5].equals("Tersedia");

        JButton back = MainApp.button("←  Kembali ke daftar adopsi", Color.WHITE, MainApp.NAVY, 14, 9,
                MainApp.NAVY);
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.setMaximumSize(back.getPreferredSize());
        back.addActionListener(e -> app.showAdopsi());
        content.add(back);
        content.add(Box.createVerticalStrut(22));

        // ----- Kiri: foto -----
        photoCard = MainApp.roundedPanel(DataStore.warnaJenis(hewan[1]));
        photoCard.setLayout(new BorderLayout());
        photoCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        photoCard.add(new MainApp.PhotoView(hewan[6], 360, 340, 20), BorderLayout.CENTER);

        // ----- Kanan: informasi -----
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel name = MainApp.label(hewan[0], 34, MainApp.INK, true);
        name.setAlignmentX(LEFT_ALIGNMENT);
        right.add(name);
        right.add(Box.createVerticalStrut(8));
        right.add(tersedia ? MainApp.chip("Tersedia", MainApp.SUCCESS_BG, MainApp.SUCCESS)
                : MainApp.chip("Sudah Diadopsi", MainApp.DANGER_BG, MainApp.DANGER));
        right.add(Box.createVerticalStrut(18));

        JPanel info = MainApp.roundedPanel(MainApp.SKY);
        info.setLayout(new GridLayout(0, 2, 12, 12));
        info.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        String[][] rows = { { "Jenis", hewan[1] }, { "Ras", hewan[2] }, { "Gender", hewan[3] },
                { "Umur", hewan[4] }, { "Status", hewan[5] } };
        for (String[] row : rows) {
            info.add(MainApp.label(row[0], 15, MainApp.MUTED, false));
            info.add(MainApp.label(row[1], 16, MainApp.INK, true));
        }
        info.setAlignmentX(LEFT_ALIGNMENT);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, info.getPreferredSize().height));
        right.add(info);
        right.add(Box.createVerticalStrut(16));

        JLabel note = MainApp.label("<html><div style='width:300px'>"
                + (tersedia ? "Pengajuan adopsi akan diverifikasi tim PawPaw sebelum " + hewan[0]
                        + " bisa dibawa pulang."
                        : hewan[0] + " sudah menemukan rumah barunya. Lihat hewan lain yang masih tersedia.")
                + "</div></html>", 14, MainApp.MUTED, false);
        note.setAlignmentX(LEFT_ALIGNMENT);
        right.add(note);
        right.add(Box.createVerticalStrut(20));

        JButton ajukan = MainApp.button(tersedia ? "Ajukan Adopsi" : "Tidak Tersedia", MainApp.BLUE, Color.WHITE,
                17, 13, null);
        ajukan.setEnabled(tersedia);
        ajukan.setAlignmentX(LEFT_ALIGNMENT);
        ajukan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        if (tersedia)
            ajukan.addActionListener(e -> AdoptionForm.show(app, hewan, null));
        right.add(ajukan);

        body.setOpaque(false);
        body.setAlignmentX(LEFT_ALIGNMENT);
        content.add(body);
        arrange(1000);
    }

    @Override
    protected boolean isRoot() {
        return false;
    }

    @Override
    protected void onWidth(int width) {
        arrange(width);
    }

    // Lebar: foto di kiri dan info di kanan. Sempit: ditumpuk ke bawah.
    private void arrange(int width) {
        boolean wide = width >= 860;
        body.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        if (wide) {
            c.insets = new Insets(0, 0, 0, 28);
            body.add(photoCard, c);
            c.gridx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(right, c);
        } else {
            c.insets = new Insets(0, 0, 22, 0);
            body.add(photoCard, c);
            c.gridy = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(right, c);
        }
        body.revalidate();
        body.repaint();
    }
}
