package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.UiKit;
import pawpaw.model.Animal;
import pawpaw.model.User;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

/**
 * Modal adoption request form prefilled from the active session user with inline validation,
 * {@code #EEF6FD} canvas, {@code 20px} rounded cards, and {@code Poppins} typography.
 */
public final class AdoptionFormDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient AppWindow app;
    private final transient Animal animal;

    private final JTextField nameField;
    private final JTextField emailField;
    private final JTextField phoneField;
    private final JTextArea addressArea;
    private final JTextArea reasonArea;
    private final JLabel inlineErrorLabel;
    private final JButton submitButton;

    private boolean submitted;

    public AdoptionFormDialog(AppWindow app, Animal animal) {
        super(app, "Form Pengajuan Adopsi", ModalityType.APPLICATION_MODAL);
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");
        this.animal = Objects.requireNonNull(animal, "Animal tidak boleh null.");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16));

        JPanel summaryCard = buildAnimalSummaryCard(animal);

        nameField = UiKit.textField("Nama lengkap pemohon");
        emailField = UiKit.textField("nama@email.com");
        phoneField = UiKit.textField("08xxxxxxxxxx");
        addressArea = WorkspaceWidgets.createEditableTextArea(3);
        reasonArea = WorkspaceWidgets.createEditableTextArea(4);

        prefillFromSession();

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();

        JPanel formCard = UiKit.cardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Nama Pemohon", nameField);
        WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Email", emailField);
        WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Nomor Telepon", phoneField);
        addScrollAreaGroup(formCard, gbc, row++, "Alamat Lengkap", addressArea, 72);
        addScrollAreaGroup(formCard, gbc, row++, "Alasan Adopsi", reasonArea, 88);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(4, 0, 0, 0);
        formCard.add(inlineErrorLabel, gbc);

        JPanel bodyStack = new JPanel();
        bodyStack.setLayout(new BoxLayout(bodyStack, BoxLayout.Y_AXIS));
        bodyStack.setOpaque(false);
        summaryCard.setAlignmentX(LEFT_ALIGNMENT);
        formCard.setAlignmentX(LEFT_ALIGNMENT);
        bodyStack.add(summaryCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(formCard);

        JScrollPane bodyScroll = WorkspaceWidgets.verticalScrollPane(bodyStack, Theme.BG);

        JButton cancelButton = UiKit.secondaryButton("Batal", VectorIcons.Kind.CLOSE);
        cancelButton.addActionListener(e -> dispose());

        submitButton = UiKit.primaryButton("Kirim Pengajuan", VectorIcons.Kind.CHECK);
        submitButton.addActionListener(e -> submitForm());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(submitButton);

        root.add(bodyScroll, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(submitButton);
        WorkspaceWidgets.registerEscapeClose(this);

        setMinimumSize(new Dimension(520, 560));
        setPreferredSize(new Dimension(540, 620));
        pack();
        setLocationRelativeTo(app);
    }

    public static boolean show(AppWindow app, Animal animal) {
        if (app == null || animal == null || app.getSession().getUser() == null) {
            return false;
        }
        AdoptionFormDialog dialog = new AdoptionFormDialog(app, animal);
        dialog.setVisible(true);
        return dialog.isSubmitted();
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public String getInlineErrorText() {
        return inlineErrorLabel.isVisible() ? inlineErrorLabel.getText() : "";
    }

    private void prefillFromSession() {
        User user = app.getSession().getUser();
        if (user == null) {
            return;
        }
        nameField.setText(WorkspaceWidgets.nonNull(user.getName()));
        emailField.setText(WorkspaceWidgets.nonNull(user.getEmail()));
        phoneField.setText(WorkspaceWidgets.nonNull(user.getPhone()));
        addressArea.setText(WorkspaceWidgets.nonNull(user.getAddress()));
    }

    private void submitForm() {
        WorkspaceWidgets.hideBanner(inlineErrorLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            setInlineError("Sesi pengguna tidak aktif. Silakan masuk kembali.");
            return;
        }

        try {
            app.getAdoptionService().submitRequest(
                user.getUserId(),
                animal.getAnimalId(),
                nameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                addressArea.getText(),
                reasonArea.getText()
            );
            submitted = true;
            dispose();
        } catch (RuntimeException ex) {
            setInlineError(WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private void setInlineError(String message) {
        WorkspaceWidgets.showBanner(inlineErrorLabel, message, true);
        revalidate();
        repaint();
    }

    private static JPanel buildAnimalSummaryCard(Animal animal) {
        JPanel card = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        PhotoPanel thumb = new PhotoPanel(64, 64, Theme.RADIUS);
        thumb.setImagePath(animal.getImage());

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel title = UiKit.label(
            "Mengajukan Adopsi: " + WorkspaceWidgets.safeText(animal.getNama()),
            Theme.heading(),
            Theme.INK
        );
        String species = WorkspaceWidgets.capitalizeWords(animal.getSpecies());
        String meta = species
            + " - " + WorkspaceWidgets.safeText(animal.getBreed())
            + " | " + WorkspaceWidgets.safeText(animal.getGender())
            + " | " + WorkspaceWidgets.safeText(animal.getAge());
        JLabel subtitle = UiKit.label(meta, Theme.small(), Theme.MUTED);

        textStack.add(title);
        textStack.add(Box.createVerticalStrut(4));
        textStack.add(subtitle);

        card.add(thumb, BorderLayout.WEST);
        card.add(textStack, BorderLayout.CENTER);
        return card;
    }

    private static void addScrollAreaGroup(
            JPanel container,
            GridBagConstraints gbc,
            int rowIndex,
            String labelText,
            JTextArea area,
            int preferredHeight) {
        JScrollPane scroll = UiKit.scrollPane(area);
        scroll.setPreferredSize(new Dimension(420, preferredHeight));
        WorkspaceWidgets.addFieldGroup(container, gbc, rowIndex, labelText, scroll);
    }
}
