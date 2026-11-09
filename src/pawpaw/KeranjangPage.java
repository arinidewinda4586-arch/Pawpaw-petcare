package pawpaw;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Halaman Keranjang: ubah jumlah, hapus item, lalu lanjut ke Checkout. Datanya dari {@link DataStore#keranjang}. */
public final class KeranjangPage extends HalamanDasar {
    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel itemsCard = MainApp.roundedPanel(MainApp.SKY);
    private final JPanel itemsHost = new JPanel();
    private final JPanel summaryCard = MainApp.roundedPanel(MainApp.SKY);
    private final JLabel itemCount = MainApp.label(" ", 14, MainApp.MUTED, false);

    public KeranjangPage(MainApp app) {
        super(app, "Toko", "KERANJANG", "Periksa kembali produk sebelum melanjutkan pesanan.");

        JButton back = MainApp.button("←  Lanjut belanja", Color.WHITE, MainApp.NAVY, 14, 9, MainApp.NAVY);
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.setMaximumSize(back.getPreferredSize());
        back.addActionListener(e -> app.showToko());
        content.add(back);
        content.add(Box.createVerticalStrut(22));

        itemsCard.setLayout(new BorderLayout(0, 14));
        itemsCard.setBorder(new EmptyBorder(20, 22, 20, 22));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(MainApp.label("Produk di keranjang", 20, MainApp.NAVY, true), BorderLayout.WEST);
        top.add(itemCount, BorderLayout.EAST);
        itemsCard.add(top, BorderLayout.NORTH);
        itemsHost.setOpaque(false);
        itemsHost.setLayout(new BoxLayout(itemsHost, BoxLayout.Y_AXIS));
        itemsCard.add(itemsHost, BorderLayout.CENTER);

        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBorder(new EmptyBorder(20, 22, 20, 22));

        body.setOpaque(false);
        body.setAlignmentX(LEFT_ALIGNMENT);
        content.add(body);
        refresh();
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

    private void arrange(int width) {
        boolean wide = width >= 860;
        body.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        if (wide) {
            c.weightx = .68;
            c.insets = new Insets(0, 0, 0, 22);
            body.add(itemsCard, c);
            c.gridx = 1;
            c.weightx = .32;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(summaryCard, c);
        } else {
            c.weightx = 1;
            c.insets = new Insets(0, 0, 22, 0);
            body.add(itemsCard, c);
            c.gridy = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(summaryCard, c);
        }
        body.revalidate();
        body.repaint();
    }

    /** Membangun ulang daftar item dan ringkasan setelah ada perubahan. */
    private void refresh() {
        int n = DataStore.jumlahItemKeranjang();
        itemCount.setText(n + " item");
        itemsHost.removeAll();
        if (DataStore.keranjang.isEmpty()) {
            JLabel title = MainApp.label("Keranjang masih kosong", 18, MainApp.NAVY, true);
            title.setAlignmentX(LEFT_ALIGNMENT);
            JLabel hint = MainApp.label("Tambahkan makanan, mainan, atau perlengkapan dari Toko.", 14, MainApp.MUTED,
                    false);
            hint.setAlignmentX(LEFT_ALIGNMENT);
            JButton toShop = MainApp.button("Belanja di Toko", MainApp.BLUE, Color.WHITE, 15, 10, null);
            toShop.setAlignmentX(LEFT_ALIGNMENT);
            toShop.addActionListener(e -> app.showToko());
            itemsHost.add(Box.createVerticalStrut(8));
            itemsHost.add(title);
            itemsHost.add(Box.createVerticalStrut(6));
            itemsHost.add(hint);
            itemsHost.add(Box.createVerticalStrut(16));
            itemsHost.add(toShop);
        } else {
            for (ItemKeranjang item : DataStore.keranjang) {
                itemsHost.add(row(item));
                itemsHost.add(Box.createVerticalStrut(12));
            }
        }
        buildSummary();
        itemsHost.revalidate();
        itemsHost.repaint();
        summaryCard.revalidate();
        summaryCard.repaint();
        revalidate();
    }

    private JPanel row(ItemKeranjang item) {
        JPanel row = MainApp.roundedPanel(Color.WHITE);
        row.setLayout(new BorderLayout(14, 0));
        row.setBorder(new EmptyBorder(12, 12, 12, 16));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        row.setPreferredSize(new Dimension(300, 104));
        row.add(new MainApp.PhotoView("shop/" + item.produk.gambar, 80, 80, 16), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(MainApp.label(item.produk.nama, 17, MainApp.INK, true));
        text.add(Box.createVerticalStrut(3));
        text.add(MainApp.label(item.produk.label + " • " + item.produk.untuk, 13, MainApp.MUTED, false));
        text.add(Box.createVerticalStrut(5));
        text.add(MainApp.label(DataStore.formatRupiah(item.produk.harga) + " / item", 14, MainApp.INK, false));
        text.add(Box.createVerticalGlue());
        row.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JPanel qty = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        qty.setOpaque(false);
        JButton minus = MainApp.button("−", MainApp.PIL, MainApp.NAVY, 16, 2, null);
        JButton plus = MainApp.button("+", MainApp.PIL, MainApp.NAVY, 16, 2, null);
        for (JButton b : new JButton[] { minus, plus })
            b.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        minus.getAccessibleContext().setAccessibleName("Kurangi " + item.produk.nama);
        plus.getAccessibleContext().setAccessibleName("Tambah " + item.produk.nama);
        minus.setEnabled(item.jumlah > 1);
        minus.addActionListener(e -> {
            if (item.jumlah > 1)
                item.jumlah--;
            refresh();
        });
        plus.addActionListener(e -> {
            item.jumlah++;
            refresh();
        });
        qty.add(minus);
        qty.add(MainApp.label(String.valueOf(item.jumlah), 16, MainApp.INK, true));
        qty.add(plus);
        qty.setAlignmentX(RIGHT_ALIGNMENT);
        JButton remove = MainApp.button("Hapus", Color.WHITE, MainApp.DANGER, 13, 4, null);
        remove.getAccessibleContext().setAccessibleName("Hapus " + item.produk.nama + " dari keranjang");
        remove.setAlignmentX(RIGHT_ALIGNMENT);
        remove.addActionListener(e -> {
            DataStore.keranjang.remove(item);
            refresh();
        });
        JLabel total = MainApp.label(DataStore.formatRupiah(item.totalHarga()), 16, MainApp.NAVY, true);
        total.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(qty);
        right.add(Box.createVerticalGlue());
        right.add(total);
        right.add(remove);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void buildSummary() {
        summaryCard.removeAll();
        JLabel title = MainApp.label("Ringkasan pesanan", 20, MainApp.NAVY, true);
        title.setAlignmentX(LEFT_ALIGNMENT);
        summaryCard.add(title);
        summaryCard.add(Box.createVerticalStrut(16));

        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setAlignmentX(LEFT_ALIGNMENT);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        line.add(MainApp.label("Subtotal", 15, MainApp.INK, false), BorderLayout.WEST);
        line.add(MainApp.label(DataStore.formatRupiah(DataStore.subtotalKeranjang()), 16, MainApp.INK, true),
                BorderLayout.EAST);
        summaryCard.add(line);
        summaryCard.add(Box.createVerticalStrut(8));
        JLabel hint = MainApp.label("Ongkir dihitung saat checkout.", 13, MainApp.MUTED, false);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        summaryCard.add(hint);
        summaryCard.add(Box.createVerticalStrut(20));

        JButton checkout = MainApp.button("Lanjut ke Checkout  ›", MainApp.BLUE, Color.WHITE, 16, 12, null);
        checkout.setAlignmentX(LEFT_ALIGNMENT);
        checkout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        checkout.setEnabled(!DataStore.keranjang.isEmpty());
        checkout.addActionListener(e -> app.showCheckout());
        summaryCard.add(checkout);
    }
}
