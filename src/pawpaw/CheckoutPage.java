package pawpaw;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Halaman Checkout: data pemesan di kiri, ringkasan pesanan di kanan (ditumpuk bila jendela sempit). */
public final class CheckoutPage extends HalamanDasar {
    private int ongkir = 0;

    private final JTextField usernameField = MainApp.textField(null);
    private final JTextField namaField = MainApp.textField("Nama lengkap");
    private final JTextField teleponField = MainApp.textField("08xx-xxxx-xxxx");
    private final JTextField alamatField = MainApp.textField("Alamat lengkap pengantaran");
    private final JComboBox<String> pengirimanCombo = new JComboBox<>(
            new String[] { "Ambil di toko (Rp0)", "Pengantaran (simulasi Rp15.000)" });
    private JLabel alamatCaption;
    private final JLabel error = MainApp.label(" ", 13, MainApp.DANGER, false);
    private JButton pesanButton;

    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel formCard = MainApp.roundedPanel(MainApp.SKY);
    private final JPanel summaryCard = MainApp.roundedPanel(MainApp.SKY);
    private final JTextArea note = MainApp.paragraph(
            "Pesanan akan disimpan di aplikasi ini untuk kebutuhan demonstrasi. "
                    + "Tidak ada pembayaran sungguhan yang diproses.", 14, new Color(52, 68, 84));
    private final JPanel noteCard = MainApp.roundedPanel(new Color(224, 242, 248));
    private JLabel subtotalLabel, ongkirLabel, totalLabel;
    private Timer timerSukses;

    public CheckoutPage(MainApp app) {
        super(app, "Toko", "CHECKOUT", "Lengkapi data pemesan lalu buat pesananmu.");

        JButton back = MainApp.button("←  Kembali ke Keranjang", Color.WHITE, MainApp.NAVY, 14, 9, MainApp.NAVY);
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.setMaximumSize(back.getPreferredSize());
        back.addActionListener(e -> app.showKeranjang());
        content.add(back);
        content.add(Box.createVerticalStrut(20));

        noteCard.setLayout(new BorderLayout());
        noteCard.setBorder(new EmptyBorder(14, 18, 14, 18));
        noteCard.add(note, BorderLayout.CENTER);
        noteCard.setAlignmentX(LEFT_ALIGNMENT);
        content.add(noteCard);
        content.add(Box.createVerticalStrut(22));

        buildForm();
        buildSummary();
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
        MainApp.fit(note, width - 60 - 36);
        noteCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, note.getPreferredSize().height + 28));
        arrange(width);
    }

    private void arrange(int width) {
        boolean wide = width >= 860;
        body.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        if (wide) {
            c.weightx = 1.35;
            c.insets = new Insets(0, 0, 0, 22);
            body.add(formCard, c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(summaryCard, c);
        } else {
            c.weightx = 1;
            c.insets = new Insets(0, 0, 22, 0);
            body.add(formCard, c);
            c.gridy = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(summaryCard, c);
        }
        body.revalidate();
        body.repaint();
    }

    // ===================== Kartu kiri: data pemesan =====================

    private void buildForm() {
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(24, 24, 22, 24));

        JLabel title = MainApp.label("Data pemesan", 20, MainApp.NAVY, true);
        title.setAlignmentX(LEFT_ALIGNMENT);
        formCard.add(title);
        formCard.add(Box.createVerticalStrut(16));

        // Username diambil dari akun login dan tidak bisa diubah
        usernameField.setText(app.session().getEmail());
        usernameField.setEditable(false);
        usernameField.setBackground(new Color(240, 245, 251));
        MainApp.addField(formCard, "Username akun", usernameField);
        MainApp.addField(formCard, "Nama", namaField);
        MainApp.addField(formCard, "Nomor telepon", teleponField);

        MainApp.styleInput(pengirimanCombo);
        MainApp.addField(formCard, "Cara menerima barang", pengirimanCombo);

        alamatCaption = MainApp.addField(formCard, "Alamat pengantaran", alamatField);
        alamatCaption.setVisible(false);
        alamatField.setVisible(false);

        formCard.add(Box.createVerticalStrut(4));
        error.setAlignmentX(LEFT_ALIGNMENT);
        formCard.add(error);
        formCard.add(Box.createVerticalStrut(8));

        pesanButton = MainApp.button("Buat Pesanan", MainApp.BLUE, Color.WHITE);
        pesanButton.setAlignmentX(LEFT_ALIGNMENT);
        pesanButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        pesanButton.addActionListener(e -> buatPesanan());
        formCard.add(pesanButton);

        pengirimanCombo.addActionListener(e -> {
            boolean antar = pengirimanCombo.getSelectedIndex() == 1;
            alamatCaption.setVisible(antar);
            alamatField.setVisible(antar);
            ongkir = antar ? 15000 : 0;
            hitungTotal();
            formCard.revalidate();
            body.revalidate();
        });
    }

    // ===================== Kartu kanan: ringkasan pesanan =====================

    private void buildSummary() {
        summaryCard.removeAll();
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBorder(new EmptyBorder(24, 24, 22, 24));

        JLabel title = MainApp.label("Ringkasan pesanan", 20, MainApp.NAVY, true);
        title.setAlignmentX(LEFT_ALIGNMENT);
        summaryCard.add(title);
        summaryCard.add(Box.createVerticalStrut(16));

        if (DataStore.keranjang.isEmpty()) {
            JLabel icon = new JLabel(new BagIcon(52, new Color(120, 152, 192)));
            icon.setAlignmentX(CENTER_ALIGNMENT);
            summaryCard.add(icon);
            summaryCard.add(Box.createVerticalStrut(10));
            JLabel empty = MainApp.label("Keranjang masih kosong", 16, MainApp.INK, true);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            summaryCard.add(empty);
            summaryCard.add(Box.createVerticalStrut(4));
            JLabel hint = MainApp.label("Belum ada produk yang bisa dipesan.", 13, MainApp.MUTED, false);
            hint.setAlignmentX(CENTER_ALIGNMENT);
            summaryCard.add(hint);
            summaryCard.add(Box.createVerticalStrut(16));
            JButton keToko = MainApp.button("Belanja di Toko", MainApp.BLUE, Color.WHITE, 15, 10, null);
            keToko.setAlignmentX(CENTER_ALIGNMENT);
            keToko.addActionListener(e -> app.showToko());
            summaryCard.add(keToko);
            summaryCard.add(Box.createVerticalStrut(18));
        } else {
            for (ItemKeranjang item : DataStore.keranjang) {
                JPanel row = new JPanel(new BorderLayout(12, 0));
                row.setOpaque(false);
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
                row.setPreferredSize(new Dimension(200, 58));
                row.add(new MainApp.PhotoView("shop/" + item.produk.gambar, 54, 54, 14), BorderLayout.WEST);
                JPanel text = new JPanel(new GridLayout(2, 1));
                text.setOpaque(false);
                text.add(MainApp.label(item.produk.nama, 14, MainApp.INK, true));
                text.add(MainApp.label(item.jumlah + " x " + DataStore.formatRupiah(item.produk.harga), 13,
                        MainApp.MUTED, false));
                row.add(text, BorderLayout.CENTER);
                row.add(MainApp.label(DataStore.formatRupiah(item.totalHarga()), 14, MainApp.INK, true),
                        BorderLayout.EAST);
                summaryCard.add(row);
                summaryCard.add(Box.createVerticalStrut(10));
            }
            summaryCard.add(Box.createVerticalStrut(2));
        }

        summaryCard.add(separator());
        summaryCard.add(Box.createVerticalStrut(12));
        subtotalLabel = priceRow("Subtotal", false);
        ongkirLabel = priceRow("Pengantaran", false);
        summaryCard.add(Box.createVerticalStrut(2));
        summaryCard.add(separator());
        summaryCard.add(Box.createVerticalStrut(12));
        totalLabel = priceRow("Total", true);

        if (pesanButton != null)
            pesanButton.setEnabled(!DataStore.keranjang.isEmpty());
        hitungTotal();
        summaryCard.revalidate();
        summaryCard.repaint();
    }

    private JComponent separator() {
        JSeparator line = new JSeparator();
        line.setForeground(MainApp.LINE);
        line.setAlignmentX(LEFT_ALIGNMENT);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        return line;
    }

    private JLabel priceRow(String text, boolean bold) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        int size = bold ? 17 : 14;
        row.add(MainApp.label(text, size, MainApp.INK, bold), BorderLayout.WEST);
        JLabel value = MainApp.label("", size, MainApp.INK, bold);
        row.add(value, BorderLayout.EAST);
        summaryCard.add(row);
        summaryCard.add(Box.createVerticalStrut(8));
        return value;
    }

    private void hitungTotal() {
        int subtotal = DataStore.subtotalKeranjang();
        subtotalLabel.setText(DataStore.formatRupiah(subtotal));
        ongkirLabel.setText(DataStore.formatRupiah(ongkir));
        totalLabel.setText(DataStore.formatRupiah(subtotal + ongkir));
    }

    // ===================== Membuat pesanan =====================

    private void buatPesanan() {
        String nama = namaField.getText().trim();
        String telepon = teleponField.getText().trim();
        String alamat = alamatField.getText().trim();
        boolean antar = pengirimanCombo.getSelectedIndex() == 1;

        if (DataStore.keranjang.isEmpty()) {
            error.setText("Keranjang masih kosong.");
            return;
        }
        if (nama.isEmpty() || telepon.isEmpty()) {
            error.setText("Nama dan nomor telepon wajib diisi.");
            (nama.isEmpty() ? namaField : teleponField).requestFocusInWindow();
            return;
        }
        if (!telepon.matches("[0-9+ ()-]{8,20}")) {
            error.setText("Nomor telepon tidak valid.");
            teleponField.requestFocusInWindow();
            return;
        }
        if (antar && alamat.isEmpty()) {
            error.setText("Alamat pengantaran wajib diisi.");
            alamatField.requestFocusInWindow();
            return;
        }

        String kodeTanggal = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String idPesanan = "ORD-" + kodeTanggal + "-" + (100000 + new Random().nextInt(900000));
        int total = DataStore.subtotalKeranjang() + ongkir;

        StringBuilder daftarItem = new StringBuilder();
        for (ItemKeranjang item : DataStore.keranjang) {
            if (daftarItem.length() > 0)
                daftarItem.append(", ");
            daftarItem.append(item.produk.nama).append(" x").append(item.jumlah);
        }
        String tanggal = LocalDate.now().format(
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")));
        DataStore.daftarPesanan.add(new String[] { idPesanan, app.session().getEmail(), nama, telepon,
                antar ? "Pengantaran" : "Ambil di toko", DataStore.formatRupiah(total), tanggal,
                daftarItem.toString() });

        // Kosongkan keranjang dan form
        DataStore.keranjang.clear();
        error.setText(" ");
        namaField.setText("");
        teleponField.setText("");
        alamatField.setText("");
        pengirimanCombo.setSelectedIndex(0);
        buildSummary();

        tampilkanDialogSukses(idPesanan, total);
    }

    private void tampilkanDialogSukses(String idPesanan, int total) {
        JDialog dialog = new JDialog(app, "Checkout", true);
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 30, 26, 30));

        JPanel tick = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainApp.SUCCESS_BG);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(MainApp.SUCCESS);
                g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[] { 20, 29, 45 }, new int[] { 34, 43, 24 }, 3);
                g2.dispose();
            }
        };
        tick.setOpaque(false);
        tick.setPreferredSize(new Dimension(64, 64));
        tick.setMaximumSize(new Dimension(64, 64));
        tick.setAlignmentX(CENTER_ALIGNMENT);
        card.add(tick);
        card.add(Box.createVerticalStrut(14));

        JLabel title = MainApp.label("Pesanan berhasil dibuat", 20, MainApp.NAVY, true);
        title.setAlignmentX(CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        JLabel msg = new JLabel("<html><div style='width:280px;text-align:center'>Pesanan " + idPesanan
                + " atas username " + app.session().getEmail() + " berhasil disimpan dengan total "
                + DataStore.formatRupiah(total) + ".</div></html>");
        msg.setFont(MainApp.font(14, false));
        msg.setForeground(MainApp.MUTED);
        msg.setAlignmentX(CENTER_ALIGNMENT);
        card.add(msg);
        card.add(Box.createVerticalStrut(8));

        JLabel auto = new JLabel("<html><div style='width:280px;text-align:center'>Jendela ini tertutup otomatis "
                + "dan kamu akan kembali ke halaman Toko.</div></html>");
        auto.setFont(MainApp.font(13, false));
        auto.setForeground(MainApp.MUTED);
        auto.setAlignmentX(CENTER_ALIGNMENT);
        card.add(auto);
        card.add(Box.createVerticalStrut(20));

        JButton go = MainApp.button("Kembali ke Toko Sekarang", MainApp.BLUE, Color.WHITE);
        go.setAlignmentX(CENTER_ALIGNMENT);
        go.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        go.addActionListener(e -> selesai(dialog));
        card.add(go);

        dialog.setContentPane(card);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                selesai(dialog);
            }
        });
        timerSukses = new Timer(5000, e -> selesai(dialog));
        timerSukses.setRepeats(false);
        timerSukses.start();

        dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(Math.max(400, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(app);
        dialog.setVisible(true);
    }

    private void selesai(JDialog dialog) {
        if (timerSukses != null)
            timerSukses.stop();
        if (!dialog.isDisplayable())
            return;
        dialog.dispose();
        app.showToko();
    }

    /** Ikon tas belanja untuk keadaan keranjang kosong (digambar lokal). */
    private static final class BagIcon implements Icon {
        private final int size;
        private final Color color;

        BagIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D q = (Graphics2D) g.create();
            q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            q.translate(x, y);
            q.scale(size / 24.0, size / 24.0);
            q.setColor(color);
            q.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            q.drawRoundRect(4, 8, 16, 14, 2, 2);
            q.drawArc(8, 2, 8, 12, 0, 180);
            q.drawLine(8, 8, 8, 11);
            q.drawLine(16, 8, 16, 11);
            q.dispose();
        }
    }
}
