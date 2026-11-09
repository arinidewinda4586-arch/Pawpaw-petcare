package pawpaw;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Halaman Kesehatan: pilih layanan, isi form booking, dan lihat daftar appointment. */
public final class KesehatanPage extends HalamanDasar {
    private static final String[] LAYANAN = { "Booking Jadwal Periksa", "Jadwalkan Vaksin",
            "Panggil Dokter Ke Rumah" };
    private static final String[] DESKRIPSI = { "Pilih dokter & waktu untuk konsultasi klinik",
            "Atur jadwal vaksinasi rutin untuk hewan peliharaan",
            "Dokter kami datang ke rumah anda untuk Home Visit" };
    private static final Color[] WARNA = { new Color(216, 245, 208), new Color(214, 232, 255),
            new Color(232, 217, 255) };

    private final JTextField pemilik = MainApp.textField("Nama pemilik hewan");
    private final JTextField hewan = MainApp.textField("Nama hewan");
    private final JComboBox<String> jenis = new JComboBox<>(new String[] { "Kucing", "Anjing", "Kelinci", "Lainnya" });
    private final JComboBox<String> layanan = new JComboBox<>(new String[] { "Booking Jadwal Periksa (Rp 100.000)",
            "Jadwalkan Vaksin", "Panggil Dokter Ke Rumah" });
    private final JTextField tanggal = MainApp.textField("YYYY-MM-DD, contoh 2026-11-15");
    private final JLabel error = MainApp.label(" ", 13, MainApp.DANGER, false);

    private final JPanel serviceRow = new JPanel();
    private final JPanel formGrid = new JPanel();
    private final JPanel appointments = new JPanel();
    private final JPanel[] fields = new JPanel[5];

    public KesehatanPage(MainApp app) {
        super(app, "Kesehatan", "LAYANAN KESEHATAN",
                "Booking pemeriksaan, jadwalkan vaksin, atau panggil dokter ke rumah.");

        JLabel serviceTitle = MainApp.label("Layanan Utama", 22, MainApp.NAVY, true);
        serviceTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(serviceTitle);
        content.add(Box.createVerticalStrut(14));
        serviceRow.setOpaque(false);
        serviceRow.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < LAYANAN.length; i++) {
            final int index = i;
            JButton card = MainApp.button("<html><div style='width:210px'><b>" + LAYANAN[i] + "</b><br>"
                    + "<span style='font-weight:normal;font-size:12px'>" + DESKRIPSI[i] + "</span></div></html>",
                    WARNA[i], MainApp.INK, 15, 14, null);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.getAccessibleContext().setAccessibleName("Pilih layanan " + LAYANAN[i]);
            card.addActionListener(e -> {
                layanan.setSelectedIndex(index);
                tanggal.requestFocusInWindow();
            });
            serviceRow.add(card);
        }
        content.add(serviceRow);
        content.add(Box.createVerticalStrut(28));

        JLabel formTitle = MainApp.label("Form Booking Layanan", 22, MainApp.NAVY, true);
        formTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(formTitle);
        content.add(Box.createVerticalStrut(14));

        JPanel formCard = MainApp.roundedPanel(MainApp.SKY);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(22, 24, 20, 24));
        formCard.setAlignmentX(LEFT_ALIGNMENT);
        error.setAlignmentX(LEFT_ALIGNMENT);
        formCard.add(error);
        formCard.add(Box.createVerticalStrut(8));
        MainApp.styleInput(jenis);
        MainApp.styleInput(layanan);
        fields[0] = field("Nama Pemilik", pemilik);
        fields[1] = field("Nama Hewan", hewan);
        fields[2] = field("Jenis Hewan", jenis);
        fields[3] = field("Pilih Layanan", layanan);
        fields[4] = field("Tanggal (YYYY-MM-DD)", tanggal);
        formGrid.setOpaque(false);
        formGrid.setAlignmentX(LEFT_ALIGNMENT);
        for (JPanel f : fields)
            formGrid.add(f);
        formCard.add(formGrid);
        JButton kirim = MainApp.button("Kirim Booking", MainApp.BLUE, Color.WHITE);
        kirim.setAlignmentX(LEFT_ALIGNMENT);
        kirim.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        kirim.addActionListener(e -> kirimBooking());
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(kirim);
        content.add(formCard);
        content.add(Box.createVerticalStrut(28));

        JLabel listTitle = MainApp.label("Detail Appointment", 22, MainApp.NAVY, true);
        listTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(listTitle);
        content.add(Box.createVerticalStrut(4));
        JLabel listSub = MainApp.label("Menampilkan riwayat dan jadwal Anda", 14, MainApp.MUTED, false);
        listSub.setAlignmentX(LEFT_ALIGNMENT);
        content.add(listSub);
        content.add(Box.createVerticalStrut(14));
        appointments.setOpaque(false);
        appointments.setLayout(new BoxLayout(appointments, BoxLayout.Y_AXIS));
        appointments.setAlignmentX(LEFT_ALIGNMENT);
        content.add(appointments);
        refreshAppointments();
        layoutAll(1000);
    }

    @Override
    protected void onWidth(int width) {
        layoutAll(width);
    }

    private void layoutAll(int width) {
        int cols = width >= 980 ? 3 : 1;
        serviceRow.setLayout(new GridLayout(0, cols, 16, 16));
        int rows = (3 + cols - 1) / cols;
        serviceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 96 + (rows - 1) * 16));
        int formCols = width >= 760 ? 2 : 1;
        formGrid.setLayout(new GridLayout(0, formCols, 18, 0));
        int formRows = (fields.length + formCols - 1) / formCols;
        formGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, formRows * 83));
        serviceRow.revalidate();
        formGrid.revalidate();
    }

    /** Satu blok "caption + isian" untuk grid form. */
    private JPanel field(String caption, JComponent input) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        MainApp.addField(p, caption, input);
        return p;
    }

    private void kirimBooking() {
        String namaPemilik = pemilik.getText().trim();
        String namaHewan = hewan.getText().trim();
        String teksTanggal = tanggal.getText().trim();
        if (namaPemilik.isEmpty() || namaHewan.isEmpty() || teksTanggal.isEmpty()) {
            error.setText("Nama pemilik, nama hewan, dan tanggal wajib diisi.");
            (namaPemilik.isEmpty() ? pemilik : namaHewan.isEmpty() ? hewan : tanggal).requestFocusInWindow();
            return;
        }
        LocalDate tgl;
        try {
            tgl = LocalDate.parse(teksTanggal);
        } catch (DateTimeParseException ex) {
            error.setText("Format tanggal harus YYYY-MM-DD, contoh 2026-11-15.");
            tanggal.requestFocusInWindow();
            return;
        }
        if (tgl.isBefore(LocalDate.now())) {
            error.setText("Tanggal booking tidak boleh di masa lalu.");
            tanggal.requestFocusInWindow();
            return;
        }
        String layananDipilih = LAYANAN[layanan.getSelectedIndex()];
        String tampil = tgl.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")));
        DataStore.daftarAppointment.add(0, new String[] { namaPemilik, namaHewan, (String) jenis.getSelectedItem(),
                layananDipilih, tampil, "Disetujui" });
        error.setText(" ");
        pemilik.setText("");
        hewan.setText("");
        tanggal.setText("");
        refreshAppointments();
        JOptionPane.showMessageDialog(app, "Booking " + layananDipilih + " untuk " + namaHewan + " berhasil ditambahkan.",
                "Booking berhasil", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAppointments() {
        appointments.removeAll();
        for (String[] a : DataStore.daftarAppointment) {
            boolean selesai = a[5].equals("Selesai");
            JPanel row = MainApp.roundedPanel(new Color(226, 239, 218));
            row.setLayout(new BorderLayout(18, 0));
            row.setBorder(new EmptyBorder(16, 20, 16, 20));
            row.setAlignmentX(LEFT_ALIGNMENT);

            JPanel details = new JPanel();
            details.setOpaque(false);
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            details.add(MainApp.label("Nama hewan: " + a[1] + " (" + a[2] + ")", 15, MainApp.INK, true));
            details.add(Box.createVerticalStrut(4));
            details.add(MainApp.label("Jenis Appointment: " + a[3], 14, MainApp.INK, false));
            details.add(Box.createVerticalStrut(4));
            details.add(MainApp.label("Tanggal & waktu: " + a[4], 14, MainApp.INK, false));
            details.add(Box.createVerticalStrut(8));
            JPanel chip = selesai ? MainApp.chip(a[5], MainApp.PIL, MainApp.NAVY)
                    : MainApp.chip(a[5], MainApp.SUCCESS_BG, MainApp.SUCCESS);
            details.add(chip);
            row.add(details, BorderLayout.CENTER);

            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            appointments.add(row);
            appointments.add(Box.createVerticalStrut(10));
        }
        appointments.revalidate();
        appointments.repaint();
    }
}
