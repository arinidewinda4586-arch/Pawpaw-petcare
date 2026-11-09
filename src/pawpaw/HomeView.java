package pawpaw;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Beranda: kartu layanan (terhubung ke halaman masing-masing), testimoni, dan footer. */
public final class HomeView extends HalamanDasar {
    private static final int SERVICE_HEIGHT = 124;
    private static final int TESTIMONIAL_HEIGHT = 174;

    private final JPanel services = new JPanel();
    private final JPanel testimonials = new JPanel();
    private final JPanel footer = new JPanel();

    public HomeView(MainApp app) {
        super(app, "Beranda", "PAWPAW PET CARE",
                "Pet Care No. 1 Se-Panam\nSahabat Terbaik Hewan Peliharaan Anda\n"
                        + "Rawat, adopsi, dan jaga kesehatan hewan kesayangan dengan PawPaw!");

        JLabel servicesTitle = MainApp.label("LAYANAN KAMI", 29, MainApp.INK, false);
        servicesTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(servicesTitle);
        content.add(Box.createVerticalStrut(22));

        services.setOpaque(false);
        services.setAlignmentX(LEFT_ALIGNMENT);
        String[] titles = { "ADOPSI HEWAN", "TOKO ONLINE", "CEK KESEHATAN", "JADWALKAN VAKSIN" };
        String[] files = { "meng.png", "food.png", "vet.png", "vaksin.png" };
        Runnable[] actions = { app::showAdopsi, app::showToko, app::showKesehatan, app::showKesehatan };
        Color[] colors = { new Color(255, 214, 214), new Color(214, 232, 255), new Color(216, 245, 208),
                new Color(232, 217, 255) };
        for (int i = 0; i < titles.length; i++) {
            String name = titles[i];
            Runnable action = actions[i];
            JButton card = MainApp.button("<html>" + name.replaceFirst(" ", "<br>") + "</html>", colors[i], MainApp.INK);
            card.setFont(MainApp.font(23, false));
            card.setIcon(MainApp.image(files[i], 54, 68));
            card.setIconTextGap(14);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.setPreferredSize(new Dimension(220, SERVICE_HEIGHT));
            card.getAccessibleContext().setAccessibleName(name);
            card.addActionListener(e -> action.run());
            services.add(card);
        }
        content.add(services);
        content.add(Box.createVerticalStrut(24));

        JLabel testimonialTitle = MainApp.label("APA KATA MEREKA?", 23, MainApp.INK, false);
        testimonialTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(testimonialTitle);
        content.add(Box.createVerticalStrut(20));
        testimonials.setOpaque(false);
        testimonials.setAlignmentX(LEFT_ALIGNMENT);
        testimonials.add(testimonial("jesika.png", "JESIKA", "Pas lagi ga selera makan, aku selalu beli wishkas disini!"));
        testimonials.add(testimonial("puncak-lukisem.png", "ARIN", "Kambing disini ganteng-ganteng."));
        testimonials.add(testimonial("farras.png", "FARRAS", ":3"));
        testimonials.add(testimonial("fakhry.png", "FAKHRY", "Penyelamat pas anabulku tiba-tiba aslam."));
        content.add(testimonials);

        // Footer di area navy di bawah konten putih.
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(28, 30, 10, 30));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        footer.add(footerColumn("PawPaw Pet Care",
                "Sahabat terbaik hewan peliharaan Anda. Adopsi, rawat, dan jaga kesehatan si bulu dengan mudah."));
        footer.add(footerColumn("Kontak",
                "Jl. Panam Raya No. 12, Pekanbaru\nTel. 0812-3456-7890\nhello@pawpawpetcare.id"));
        footer.add(footerColumn("Jam Operasional",
                "Senin - Sabtu: 08.00 - 20.00\nMinggu: 09.00 - 17.00\nLayanan darurat: 24 Jam"));
        JPanel socials = footerColumn("Ikuti Kami", "");
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        links.setOpaque(false);
        for (String name : new String[] { "Instagram", "Facebook", "WhatsApp" }) {
            JButton link = MainApp.button(name.substring(0, 1), MainApp.BLUE, Color.WHITE);
            link.setToolTipText(name);
            link.addActionListener(e -> app.unavailable(name));
            links.add(link);
        }
        socials.add(links, BorderLayout.CENTER);
        footer.add(socials);
        below.add(footer);

        onWidth(1000);
    }

    @Override
    protected void onWidth(int width) {
        int cols = width >= 1100 ? 4 : width >= 600 ? 2 : 1;
        int rows = (4 + cols - 1) / cols;
        services.setLayout(new GridLayout(0, cols, 16, 16));
        services.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * SERVICE_HEIGHT + (rows - 1) * 16));
        testimonials.setLayout(new GridLayout(0, cols, 18, 18));
        testimonials.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * TESTIMONIAL_HEIGHT + (rows - 1) * 18));
        int fc = width >= 1000 ? 4 : width >= 600 ? 2 : 1;
        footer.setLayout(new GridLayout(0, fc, 24, 24));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, ((4 + fc - 1) / fc) * 150 + ((4 + fc - 1) / fc - 1) * 24 + 38));
        services.revalidate();
        testimonials.revalidate();
        footer.revalidate();
    }

    private JPanel testimonial(String file, String name, String quote) {
        JPanel card = MainApp.roundedPanel(new Color(253, 246, 216));
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(new JLabel(MainApp.image(file, 42, 42)));
        top.add(MainApp.label("★★★★★", 17, new Color(245, 179, 1), false));
        card.add(top, BorderLayout.NORTH);
        JPanel words = new JPanel(new BorderLayout(0, 8));
        words.setOpaque(false);
        words.add(MainApp.label(name, 14, MainApp.INK, true), BorderLayout.NORTH);
        words.add(MainApp.paragraph(quote, 14, new Color(34, 34, 34)), BorderLayout.CENTER);
        card.add(words, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(220, TESTIMONIAL_HEIGHT));
        return card;
    }

    private JPanel footerColumn(String title, String value) {
        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setOpaque(false);
        column.add(MainApp.label(title, 16, MainApp.GOLD, true), BorderLayout.NORTH);
        column.add(MainApp.paragraph(value, 13, new Color(215, 226, 240)), BorderLayout.CENTER);
        column.setPreferredSize(new Dimension(210, 150));
        return column;
    }
}
