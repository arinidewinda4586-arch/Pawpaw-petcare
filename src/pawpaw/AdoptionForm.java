package pawpaw;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Jendela form "Ajukan Adopsi" (dipakai oleh halaman Adopsi dan Detail Adopsi). */
public final class AdoptionForm {
    private AdoptionForm() {
    }

    /** Membuka form untuk satu hewan. onSent dijalankan setelah pengajuan berhasil disimpan. */
    public static void show(MainApp app, String[] hewan, Runnable onSent) {
        JDialog dialog = new JDialog(app, "Ajukan Adopsi", true);
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 30, 24, 30));

        JLabel title = MainApp.label("Ajukan Adopsi", 22, MainApp.NAVY, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        JLabel subtitle = MainApp.label("Mengajukan adopsi untuk " + hewan[0], 15, MainApp.MUTED, false);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(14));

        JLabel error = MainApp.label(" ", 13, MainApp.DANGER, false);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(error);
        card.add(Box.createVerticalStrut(6));

        JTextField nama = MainApp.textField("Nama sesuai identitas");
        JTextField email = MainApp.textField("nama@email.com");
        JTextField telepon = MainApp.textField("08xx-xxxx-xxxx");
        JTextField alamat = MainApp.textField("Alamat lengkap");
        JTextField alasan = MainApp.textField("Ceritakan alasan kamu ingin mengadopsi");
        MainApp.addField(card, "Nama Lengkap", nama);
        MainApp.addField(card, "Email", email);
        MainApp.addField(card, "Nomor Telepon", telepon);
        MainApp.addField(card, "Alamat", alamat);
        MainApp.addField(card, "Alasan Adopsi", alasan);
        card.add(Box.createVerticalStrut(6));

        JButton kirim = MainApp.button("Kirim Pengajuan", MainApp.BLUE, Color.WHITE);
        kirim.setAlignmentX(Component.LEFT_ALIGNMENT);
        kirim.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        kirim.addActionListener(e -> {
            JTextField[] semua = { nama, email, telepon, alamat, alasan };
            for (JTextField f : semua) {
                if (f.getText().isBlank()) {
                    error.setText("Semua kolom wajib diisi.");
                    f.requestFocusInWindow();
                    return;
                }
            }
            if (!email.getText().contains("@")) {
                error.setText("Format email tidak valid.");
                email.requestFocusInWindow();
                return;
            }
            String tanggal = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")));
            DataStore.riwayatAdopsi.add(new String[] { hewan[0], hewan[2], hewan[3], hewan[4], tanggal,
                    "Menunggu Verifikasi", hewan[6] });
            dialog.dispose();
            JOptionPane.showMessageDialog(app, "Pengajuan adopsi untuk " + hewan[0]
                    + " berhasil dikirim! Tim PawPaw akan memverifikasinya.", "Berhasil Dikirim!",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onSent != null)
                onSent.run();
        });
        card.add(kirim);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(kirim);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(460, dialog.getHeight());
        dialog.setLocationRelativeTo(app);
        dialog.setVisible(true);
    }
}
