package pawpaw;

import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Kalender booking bulanan PawPaw: kuota 3 booking/hari, jam 08.00-16.00. */
public final class KesehatanPage extends HalamanDasar {
    private static final int KUOTA_HARIAN = 3;
    private static final LocalTime JAM_BUKA = LocalTime.of(8, 0);
    private static final LocalTime JAM_TUTUP = LocalTime.of(16, 0);
    private static final String[] LAYANAN = {
            "Booking Jadwal Periksa", "Jadwalkan Vaksin", "Panggil Dokter Ke Rumah"
    };
    private static final String[] DESKRIPSI = {
            "Pilih dokter & waktu untuk konsultasi klinik",
            "Atur jadwal vaksinasi rutin untuk hewan peliharaan",
            "Dokter kami datang ke rumah anda untuk Home Visit"
    };
    private static final Color[] WARNA = {
            new Color(216, 245, 208), new Color(214, 232, 255), new Color(232, 217, 255)
    };
    private static final DateTimeFormatter TANGGAL_DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TANGGAL_TAMPIL = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID"));
    private static final DateTimeFormatter BULAN_TAMPIL = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    private final JTextField pemilik = MainApp.textField("Nama pemilik hewan");
    private final JTextField hewan = MainApp.textField("Nama hewan");
    private final JComboBox<String> jenis = new JComboBox<>(new String[] { "Kucing", "Anjing", "Kelinci", "Lainnya" });
    private final JComboBox<String> layanan = new JComboBox<>(LAYANAN);
    private final JComboBox<String> durasi = new JComboBox<>(new String[] { "30 menit", "60 menit" });
    private final JComboBox<String> jam = new JComboBox<>();
    private final JLabel error = MainApp.label(" ", 13, MainApp.DANGER, false);
    private final JLabel bulanLabel = MainApp.label("", 19, MainApp.NAVY, true);
    private final JLabel tanggalDipilihLabel = MainApp.label("Pilih tanggal pada kalender.", 14, MainApp.MUTED, true);
    private final JLabel kuotaLabel = MainApp.label("", 13, MainApp.MUTED, false);

    private final JPanel serviceRow = new JPanel();
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 6, 6));
    private final JPanel calendarCard = MainApp.roundedPanel(MainApp.SKY);
    private final JPanel formGrid = new JPanel();
    private final JPanel appointments = new JPanel();
    private final JPanel[] fields = new JPanel[6];
    private YearMonth bulanAktif = YearMonth.now();
    private LocalDate tanggalDipilih;
    private Map<LocalDate, Integer> jumlahPerHari = new HashMap<>();

    public KesehatanPage(MainApp app) {
        super(app, "Kesehatan", "LAYANAN KESEHATAN",
                "Booking pemeriksaan, jadwalkan vaksin, atau panggil dokter ke rumah.");

        JLabel serviceTitle = MainApp.label("Layanan Utama", 22, MainApp.NAVY, true);
        serviceTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(serviceTitle);
        content.add(Box.createVerticalStrut(12));
        serviceRow.setOpaque(false);
        serviceRow.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < LAYANAN.length; i++) {
            final int index = i;
            JButton card = MainApp.button("<html><div style='width:210px'><b>" + LAYANAN[i] + "</b><br>"
                    + "<span style='font-weight:normal;font-size:12px'>" + DESKRIPSI[i] + "</span></div></html>",
                    WARNA[i], MainApp.INK, 15, 14, null);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.addActionListener(e -> {
                layanan.setSelectedIndex(index);
                refreshJamTersedia();
            });
            serviceRow.add(card);
        }
        content.add(serviceRow);
        content.add(Box.createVerticalStrut(24));

        JLabel calendarTitle = MainApp.label("Kalender Ketersediaan Booking", 22, MainApp.NAVY, true);
        calendarTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(calendarTitle);
        content.add(Box.createVerticalStrut(10));
        calendarCard.setLayout(new BorderLayout(10, 12));
        calendarCard.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel monthNav = new JPanel(new BorderLayout(10, 0));
        monthNav.setOpaque(false);
        JButton prev = MainApp.button("‹", MainApp.BLUE, Color.WHITE, 20, 5, null);
        JButton next = MainApp.button("›", MainApp.BLUE, Color.WHITE, 20, 5, null);
        prev.addActionListener(e -> ubahBulan(-1));
        next.addActionListener(e -> ubahBulan(1));
        monthNav.add(prev, BorderLayout.WEST);
        monthNav.add(bulanLabel, BorderLayout.CENTER);
        bulanLabel.setHorizontalAlignment(SwingConstants.CENTER);
        monthNav.add(next, BorderLayout.EAST);
        calendarCard.add(monthNav, BorderLayout.NORTH);
        calendarGrid.setOpaque(false);
        calendarCard.add(calendarGrid, BorderLayout.CENTER);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legend.setOpaque(false);
        legend.add(MainApp.label("● Tersedia (0–2 booking)", 12, MainApp.SUCCESS, true));
        legend.add(MainApp.label("● Penuh (3 booking)", 12, MainApp.DANGER, true));
        legend.add(MainApp.label("● Lewat", 12, MainApp.MUTED, true));
        calendarCard.add(legend, BorderLayout.SOUTH);
        calendarCard.setAlignmentX(LEFT_ALIGNMENT);
        calendarCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));
        content.add(calendarCard);
        content.add(Box.createVerticalStrut(12));
        tanggalDipilihLabel.setAlignmentX(LEFT_ALIGNMENT);
        kuotaLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(tanggalDipilihLabel);
        content.add(kuotaLabel);
        content.add(Box.createVerticalStrut(22));

        JLabel formTitle = MainApp.label("Form Booking Layanan", 22, MainApp.NAVY, true);
        formTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(formTitle);
        content.add(Box.createVerticalStrut(12));
        JPanel formCard = MainApp.roundedPanel(MainApp.SKY);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(20, 22, 20, 22));
        formCard.setAlignmentX(LEFT_ALIGNMENT);
        error.setAlignmentX(LEFT_ALIGNMENT);
        formCard.add(error);
        formCard.add(Box.createVerticalStrut(8));
        MainApp.styleInput(jenis);
        MainApp.styleInput(layanan);
        MainApp.styleInput(durasi);
        MainApp.styleInput(jam);
        fields[0] = field("Nama Pemilik", pemilik);
        fields[1] = field("Nama Hewan", hewan);
        fields[2] = field("Jenis Hewan", jenis);
        fields[3] = field("Pilih Layanan", layanan);
        fields[4] = field("Durasi Pemeriksaan", durasi);
        fields[5] = field("Jam Tersedia (08.00–16.00)", jam);
        formGrid.setOpaque(false);
        formGrid.setAlignmentX(LEFT_ALIGNMENT);
        for (JPanel f : fields) formGrid.add(f);
        formCard.add(formGrid);
        JButton kirim = MainApp.button("Kirim Booking", MainApp.BLUE, Color.WHITE);
        kirim.setAlignmentX(LEFT_ALIGNMENT);
        kirim.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        kirim.addActionListener(e -> kirimBooking());
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(kirim);
        content.add(formCard);
        content.add(Box.createVerticalStrut(24));

        JLabel listTitle = MainApp.label("Detail Appointment", 22, MainApp.NAVY, true);
        listTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(listTitle);
        content.add(Box.createVerticalStrut(4));
        JLabel listSub = MainApp.label("Menampilkan riwayat booking dari akun Anda", 14, MainApp.MUTED, false);
        listSub.setAlignmentX(LEFT_ALIGNMENT);
        content.add(listSub);
        content.add(Box.createVerticalStrut(12));
        appointments.setOpaque(false);
        appointments.setLayout(new BoxLayout(appointments, BoxLayout.Y_AXIS));
        appointments.setAlignmentX(LEFT_ALIGNMENT);
        content.add(appointments);

        durasi.addActionListener(e -> refreshJamTersedia());
        layanan.addActionListener(e -> refreshJamTersedia());
        refreshKalender();
        refreshAppointments();
        layoutAll(1000);
    }

    @Override
    protected void onWidth(int width) { layoutAll(width); }

    private void layoutAll(int width) {
        int cols = width >= 980 ? 3 : 1;
        serviceRow.setLayout(new GridLayout(0, cols, 14, 14));
        int rows = (3 + cols - 1) / cols;
        serviceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 96 + (rows - 1) * 14));
        int formCols = width >= 760 ? 2 : 1;
        formGrid.setLayout(new GridLayout(0, formCols, 16, 12));
        int formRows = (fields.length + formCols - 1) / formCols;
        formGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, formRows * 78));
        serviceRow.revalidate();
        formGrid.revalidate();
    }

    private JPanel field(String caption, JComponent input) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        MainApp.addField(p, caption, input);
        return p;
    }

    private void ubahBulan(int selisih) {
        bulanAktif = bulanAktif.plusMonths(selisih);
        tanggalDipilih = null;
        tanggalDipilihLabel.setText("Pilih tanggal pada kalender.");
        kuotaLabel.setText(" ");
        refreshJamTersedia();
        refreshKalender();
    }

    /** Ambil jumlah booking aktif per tanggal untuk bulan yang sedang ditampilkan. */
    private void refreshKalender() {
        try {
            jumlahPerHari = loadJumlahBookingBulan(bulanAktif);
            gambarKalender();
            if (tanggalDipilih != null) refreshInfoTanggal();
        } catch (SQLException ex) {
            error.setText("Gagal membaca kalender dari database: " + ex.getMessage());
        }
    }

    private Map<LocalDate, Integer> loadJumlahBookingBulan(YearMonth bulan) throws SQLException {
        Map<LocalDate, Integer> hasil = new HashMap<>();
        String sql = "SELECT DATE(booking_date) AS hari, COUNT(*) AS jumlah FROM health_booking "
                + "WHERE booking_date >= ? AND booking_date < ? "
                + "AND status NOT IN ('cancelled','rejected') GROUP BY DATE(booking_date)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(bulan.atDay(1).atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(bulan.plusMonths(1).atDay(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) hasil.put(rs.getDate("hari").toLocalDate(), rs.getInt("jumlah"));
            }
        }
        return hasil;
    }

    private void gambarKalender() {
        calendarGrid.removeAll();
        bulanLabel.setText(bulanAktif.format(BULAN_TAMPIL));
        String[] namaHari = { "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min" };
        for (String h : namaHari) {
            JLabel label = MainApp.label(h, 12, MainApp.NAVY, true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            calendarGrid.add(label);
        }
        LocalDate pertama = bulanAktif.atDay(1);
        int offset = pertama.getDayOfWeek().getValue() - 1;
        int jumlahHari = bulanAktif.lengthOfMonth();
        for (int i = 0; i < offset; i++) calendarGrid.add(new JLabel(""));
        for (int hari = 1; hari <= jumlahHari; hari++) {
            LocalDate tanggal = bulanAktif.atDay(hari);
            int jumlah = jumlahPerHari.getOrDefault(tanggal, 0);
            boolean lewat = tanggal.isBefore(LocalDate.now());
            boolean penuh = jumlah >= KUOTA_HARIAN;
            String status = lewat ? "Lewat" : penuh ? "Penuh" : jumlah + "/" + KUOTA_HARIAN;
            Color bg = lewat ? new Color(235, 237, 240) : penuh ? MainApp.DANGER_BG : MainApp.SUCCESS_BG;
            Color fg = lewat ? MainApp.MUTED : penuh ? MainApp.DANGER : MainApp.SUCCESS;
            JButton day = new JButton("<html><center><b>" + hari + "</b><br><span style='font-size:10px'>" + status + "</span></center></html>");
            day.setFont(MainApp.font(13, true));
            day.setForeground(fg);
            day.setBackground(bg);
            day.setOpaque(true);
            day.setFocusPainted(false);
            day.setBorder(BorderFactory.createLineBorder(tanggal.equals(tanggalDipilih) ? MainApp.BLUE : Color.WHITE,
                    tanggal.equals(tanggalDipilih) ? 2 : 1));
            day.setEnabled(!lewat && !penuh);
            day.setToolTipText(tanggal.format(DateTimeFormatter.ISO_LOCAL_DATE) + " — " + jumlah + "/" + KUOTA_HARIAN + " booking");
            day.addActionListener(e -> pilihTanggal(tanggal));
            calendarGrid.add(day);
        }
        int sisa = (7 - (calendarGrid.getComponentCount() % 7)) % 7;
        for (int i = 0; i < sisa; i++) calendarGrid.add(new JLabel(""));
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void pilihTanggal(LocalDate tanggal) {
        tanggalDipilih = tanggal;
        refreshInfoTanggal();
        gambarKalender();
        refreshJamTersedia();
    }

    private void refreshInfoTanggal() {
        int jumlah = jumlahPerHari.getOrDefault(tanggalDipilih, 0);
        tanggalDipilihLabel.setText("Tanggal dipilih: " + tanggalDipilih.format(TANGGAL_TAMPIL));
        kuotaLabel.setText("Kuota harian: " + jumlah + "/" + KUOTA_HARIAN + " booking — "
                + (jumlah >= KUOTA_HARIAN ? "PENUH" : "tersedia " + (KUOTA_HARIAN - jumlah) + " kuota"));
        kuotaLabel.setForeground(jumlah >= KUOTA_HARIAN ? MainApp.DANGER : MainApp.SUCCESS);
    }

    /** Isi pilihan jam yang tidak bertabrakan dengan booking lain. */
    private void refreshJamTersedia() {
        jam.removeAllItems();
        if (tanggalDipilih == null) {
            jam.addItem("Pilih tanggal pada kalender terlebih dahulu");
            return;
        }
        int jumlah = jumlahPerHari.getOrDefault(tanggalDipilih, 0);
        if (jumlah >= KUOTA_HARIAN) {
            jam.addItem("Kuota tanggal ini sudah penuh");
            return;
        }
        int menit = durasi.getSelectedIndex() == 0 ? 30 : 60;
        try {
            List<LocalTime> tersedia = loadJamTersedia(tanggalDipilih, menit);
            for (LocalTime waktu : tersedia) jam.addItem(waktu.toString());
            if (tersedia.isEmpty()) jam.addItem("Tidak ada jam tersedia");
        } catch (SQLException ex) {
            jam.addItem("Gagal memuat jam");
            error.setText("Gagal memuat jam booking: " + ex.getMessage());
        }
    }

    private List<LocalTime> loadJamTersedia(LocalDate tanggal, int durasiMenit) throws SQLException {
        List<LocalTime> hasil = new ArrayList<>();
        String sql = "SELECT booking_date, duration_minutes FROM health_booking "
                + "WHERE DATE(booking_date) = ? AND status NOT IN ('cancelled','rejected')";
        List<LocalDateTime[]> terisi = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime mulai = rs.getTimestamp("booking_date").toLocalDateTime();
                    int lama = rs.getInt("duration_minutes");
                    terisi.add(new LocalDateTime[] { mulai, mulai.plusMinutes(lama) });
                }
            }
        }
        for (LocalTime mulai = JAM_BUKA; !mulai.plusMinutes(durasiMenit).isAfter(JAM_TUTUP); mulai = mulai.plusMinutes(30)) {
            LocalDateTime awalBaru = tanggal.atTime(mulai);
            LocalDateTime akhirBaru = awalBaru.plusMinutes(durasiMenit);
            boolean bentrok = false;
            for (LocalDateTime[] rentang : terisi) {
                if (awalBaru.isBefore(rentang[1]) && akhirBaru.isAfter(rentang[0])) {
                    bentrok = true;
                    break;
                }
            }
            if (!bentrok) hasil.add(mulai);
        }
        return hasil;
    }

    private void kirimBooking() {
        String namaPemilik = pemilik.getText().trim();
        String namaHewan = hewan.getText().trim();
        if (namaPemilik.isEmpty() || namaHewan.isEmpty()) {
            error.setText("Nama pemilik dan nama hewan wajib diisi.");
            (namaPemilik.isEmpty() ? pemilik : hewan).requestFocusInWindow();
            return;
        }
        if (tanggalDipilih == null) {
            error.setText("Pilih tanggal booking pada kalender terlebih dahulu.");
            return;
        }
        if (jam.getSelectedItem() == null || !jam.getSelectedItem().toString().matches("\\d{2}:\\d{2}")) {
            error.setText("Pilih jam yang masih tersedia.");
            return;
        }
        int userId = app.session().getUserId();
        if (userId <= 0) {
            error.setText("Sesi pengguna tidak ditemukan. Silakan login kembali.");
            return;
        }
        int durasiMenit = durasi.getSelectedIndex() == 0 ? 30 : 60;
        LocalTime waktu = LocalTime.parse(jam.getSelectedItem().toString());
        LocalDateTime mulai = tanggalDipilih.atTime(waktu);
        if (mulai.isBefore(LocalDateTime.now())) {
            error.setText("Jam booking sudah lewat. Pilih jam lain.");
            refreshJamTersedia();
            return;
        }
        try {
            int serviceId = getServiceId(LAYANAN[layanan.getSelectedIndex()]);
            simpanBooking(userId, serviceId, namaPemilik, namaHewan,
                    jenis.getSelectedItem().toString(), mulai, durasiMenit);
            error.setText(" ");
            pemilik.setText("");
            hewan.setText("");
            refreshKalender();
            refreshAppointments();
            JOptionPane.showMessageDialog(app,
                    "Booking berhasil disimpan.\nTanggal: " + mulai.format(TANGGAL_TAMPIL)
                    + "\nJam: " + waktu + "\nDurasi: " + durasiMenit + " menit",
                    "Booking berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLIntegrityConstraintViolationException ex) {
            error.setText("Data booking tidak cocok dengan relasi database: " + ex.getMessage());
        } catch (SQLException ex) {
            error.setText("Booking gagal: " + ex.getMessage());
            JOptionPane.showMessageDialog(app, "Booking gagal disimpan.\n" + ex.getMessage(),
                    "Kesalahan Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getServiceId(String namaLayanan) throws SQLException {
        String sql = "SELECT service_id FROM health_service WHERE service_name = ? LIMIT 1";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Layanan '" + namaLayanan + "' belum ada di tabel health_service.");
                return rs.getInt(1);
            }
        }
    }

    /** Lock per tanggal menjaga kuota 3 booking/hari walau ada dua pengguna booking bersamaan. */
    private void simpanBooking(int userId, int serviceId, String owner, String pet, String species,
            LocalDateTime mulai, int durasiMenit) throws SQLException {
        String lockSql = "INSERT IGNORE INTO health_booking_day_lock (booking_date) VALUES (?)";
        String countSql = "SELECT COUNT(*) FROM health_booking WHERE DATE(booking_date) = ? "
                + "AND status NOT IN ('cancelled','rejected')";
        String overlapSql = "SELECT COUNT(*) FROM health_booking WHERE DATE(booking_date) = ? "
                + "AND status NOT IN ('cancelled','rejected') "
                + "AND booking_date < ? AND DATE_ADD(booking_date, INTERVAL duration_minutes MINUTE) > ?";
        String insertSql = "INSERT INTO health_booking (user_id, service_id, owner_name, pet_name, pet_species, "
                + "booking_date, duration_minutes, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(lockSql)) {
                    ps.setDate(1, java.sql.Date.valueOf(mulai.toLocalDate()));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT booking_date FROM health_booking_day_lock WHERE booking_date = ? FOR UPDATE")) {
                    ps.setDate(1, java.sql.Date.valueOf(mulai.toLocalDate()));
                    ps.executeQuery().close();
                }
                try (PreparedStatement ps = c.prepareStatement(countSql)) {
                    ps.setDate(1, java.sql.Date.valueOf(mulai.toLocalDate()));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) >= KUOTA_HARIAN)
                            throw new SQLException("Kuota harian sudah penuh (maksimal 3 booking per hari).");
                    }
                }
                LocalDateTime akhir = mulai.plusMinutes(durasiMenit);
                try (PreparedStatement ps = c.prepareStatement(overlapSql)) {
                    ps.setDate(1, java.sql.Date.valueOf(mulai.toLocalDate()));
                    ps.setTimestamp(2, Timestamp.valueOf(akhir));
                    ps.setTimestamp(3, Timestamp.valueOf(mulai));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) throw new SQLException("Jam tersebut sudah bertabrakan dengan booking lain. Pilih jam berbeda.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, serviceId);
                    ps.setString(3, owner);
                    ps.setString(4, pet);
                    ps.setString(5, species);
                    ps.setTimestamp(6, Timestamp.valueOf(mulai));
                    ps.setInt(7, durasiMenit);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void refreshAppointments() {
        appointments.removeAll();
        String sql = "SELECT b.owner_name, b.pet_name, b.pet_species, b.booking_date, b.duration_minutes, "
                + "b.status, s.service_name FROM health_booking b JOIN health_service s ON s.service_id=b.service_id "
                + "WHERE b.user_id = ? ORDER BY b.booking_date DESC";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, app.session().getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                boolean ada = false;
                while (rs.next()) {
                    ada = true;
                    LocalDateTime waktu = rs.getTimestamp("booking_date").toLocalDateTime();
                    String statusDb = rs.getString("status");
                    String status = statusDb.equalsIgnoreCase("approved") ? "Disetujui"
                            : statusDb.equalsIgnoreCase("completed") ? "Selesai"
                            : statusDb.equalsIgnoreCase("rejected") ? "Ditolak"
                            : statusDb.equalsIgnoreCase("cancelled") ? "Dibatalkan" : "Menunggu";
                    Color bg = statusDb.equalsIgnoreCase("rejected") || statusDb.equalsIgnoreCase("cancelled")
                            ? MainApp.DANGER_BG : statusDb.equalsIgnoreCase("completed") ? MainApp.PIL : MainApp.SUCCESS_BG;
                    Color fg = statusDb.equalsIgnoreCase("rejected") || statusDb.equalsIgnoreCase("cancelled")
                            ? MainApp.DANGER : MainApp.SUCCESS;
                    JPanel row = MainApp.roundedPanel(new Color(226, 239, 218));
                    row.setLayout(new BorderLayout(12, 0));
                    row.setBorder(new EmptyBorder(14, 18, 14, 18));
                    JPanel details = new JPanel();
                    details.setOpaque(false);
                    details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                    details.add(MainApp.label("Nama hewan: " + rs.getString("pet_name") + " (" + rs.getString("pet_species") + ")", 15, MainApp.INK, true));
                    details.add(Box.createVerticalStrut(4));
                    details.add(MainApp.label("Pemilik: " + rs.getString("owner_name"), 13, MainApp.INK, false));
                    details.add(MainApp.label("Layanan: " + rs.getString("service_name"), 13, MainApp.INK, false));
                    details.add(MainApp.label("Jadwal: " + waktu.format(TANGGAL_TAMPIL) + " pukul "
                            + waktu.format(DateTimeFormatter.ofPattern("HH:mm")) + " (" + rs.getInt("duration_minutes") + " menit)", 13, MainApp.INK, false));
                    details.add(Box.createVerticalStrut(6));
                    details.add(MainApp.chip(status, bg, fg));
                    row.add(details, BorderLayout.CENTER);
                    row.setAlignmentX(LEFT_ALIGNMENT);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                    appointments.add(row);
                    appointments.add(Box.createVerticalStrut(9));
                }
                if (!ada) {
                    JLabel kosong = MainApp.label("Belum ada booking kesehatan.", 14, MainApp.MUTED, false);
                    kosong.setAlignmentX(LEFT_ALIGNMENT);
                    appointments.add(kosong);
                }
            }
        } catch (SQLException ex) {
            JLabel gagal = MainApp.label("Riwayat booking gagal dimuat: " + ex.getMessage(), 13, MainApp.DANGER, false);
            gagal.setAlignmentX(LEFT_ALIGNMENT);
            appointments.add(gagal);
        }
        appointments.revalidate();
        appointments.repaint();
    }
}
