package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.UiKit;
import pawpaw.model.Pet;
import pawpaw.model.User;
import pawpaw.service.ProfileService;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Customer profile workspace featuring a website-matched {@code #D6E8FF} hero header with
 * circular {@code #FFD6D6}-ring avatar and live stat chips, account biodata editor,
 * personal pet CRUD management, and password security controls.
 */
public final class ProfileWorkspacePanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] PET_SPECIES_OPTIONS = {
        "Kucing", "Anjing", "Kelinci", "Burung", "Hamster", "Lainnya"
    };
    private static final String[] PET_GENDER_OPTIONS = { "Jantan", "Betina" };

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JPanel errorBannerSlot;
    private final JTabbedPane tabs;

    // Hero header widgets
    private final PhotoPanel heroAvatar;
    private final JLabel heroNameLabel;
    private final JLabel heroRoleBadge;
    private final JLabel heroMetaLabel;
    private final StatChip hewanStatChip;
    private final StatChip pesananStatChip;
    private final StatChip adopsiBookingStatChip;

    // Tab 1: Data Profil
    private final PhotoPanel avatarPreview;
    private final JLabel avatarPathLabel;
    private final JTextField nameField;
    private final JTextField usernameField;
    private final JTextField emailField;
    private final JTextField phoneField;
    private final JTextArea addressArea;
    private final JTextArea bioArea;
    private final JLabel profileFeedbackLabel;
    private String pendingAvatarPath;

    // Tab 2: Hewan Peliharaan Saya
    private final JLabel petsCountLabel;
    private final JButton editPetButton;
    private final JButton deletePetButton;
    private final WorkspaceWidgets.ReadOnlyTableModel petsModel;
    private final JTable petsTable;

    // Tab 3: Keamanan / Sandi
    private final JPasswordField currentPasswordField;
    private final JPasswordField newPasswordField;
    private final JPasswordField confirmPasswordField;
    private final JLabel passwordFeedbackLabel;

    private transient List<Pet> ownedPets = new ArrayList<>();
    private String statusSummary = "Profil dan data hewan siap.";

    public ProfileWorkspacePanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();
        errorBannerSlot = WorkspaceWidgets.createBannerSlot(inlineErrorLabel, Theme.SPACE_12);

        // --- Top Blue Hero Header Card (#D6E8FF, radius 22px) ---
        heroAvatar = new PhotoPanel(76, 76, 38);
        heroAvatar.setCircular(true);
        heroAvatar.setRingBorder(Theme.PASTEL_PINK, 4);
        Dimension heroAvatarSize = new Dimension(76, 76);
        heroAvatar.setMinimumSize(heroAvatarSize);
        heroAvatar.setMaximumSize(heroAvatarSize);

        heroNameLabel = UiKit.label("Pengguna", Theme.title(), Theme.INK);
        heroRoleBadge = UiKit.pillLabel("Pelanggan", Theme.PASTEL_GREEN, Theme.INK);
        heroMetaLabel = UiKit.label("@- \u2022 -", Theme.small(), Theme.MUTED);

        hewanStatChip = new StatChip("Hewan", Theme.PASTEL_LIGHT_PINK);
        pesananStatChip = new StatChip("Pesanan", Theme.SURFACE);
        adopsiBookingStatChip = new StatChip("Adopsi & Booking", Theme.PASTEL_LAVENDER);

        JPanel heroCard = buildHeroHeaderCard();

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        errorBannerSlot.setAlignmentX(LEFT_ALIGNMENT);
        heroCard.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(errorBannerSlot);
        northStack.add(heroCard);
        add(northStack, BorderLayout.NORTH);

        // --- Tab 1: Data Profil ---
        avatarPreview = new PhotoPanel(156, 156, Theme.CARD_RADIUS);
        avatarPathLabel = UiKit.label("Belum ada foto unggahan", Theme.small(), Theme.MUTED);
        JButton chooseAvatarBtn = UiKit.secondaryButton("Pilih Foto", VectorIcons.Kind.IMAGE);
        chooseAvatarBtn.addActionListener(e -> chooseAvatarImage());

        JPanel avatarCard = UiKit.cardPanel();
        avatarCard.setPreferredSize(new Dimension(240, 290));
        JPanel avatarStack = new JPanel();
        avatarStack.setLayout(new BoxLayout(avatarStack, BoxLayout.Y_AXIS));
        avatarStack.setOpaque(false);
        JLabel avatarTitle = UiKit.label("Foto Profil", Theme.heading(), Theme.INK);
        avatarTitle.setAlignmentX(CENTER_ALIGNMENT);
        avatarPreview.setAlignmentX(CENTER_ALIGNMENT);
        avatarPathLabel.setAlignmentX(CENTER_ALIGNMENT);
        chooseAvatarBtn.setAlignmentX(CENTER_ALIGNMENT);
        avatarStack.add(avatarTitle);
        avatarStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        avatarStack.add(avatarPreview);
        avatarStack.add(Box.createVerticalStrut(Theme.SPACE_8));
        avatarStack.add(avatarPathLabel);
        avatarStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        avatarStack.add(chooseAvatarBtn);
        avatarCard.add(avatarStack, BorderLayout.NORTH);

        nameField = UiKit.textField("Nama lengkap");
        usernameField = UiKit.textField("username_anda");
        emailField = UiKit.textField("nama@email.com");
        phoneField = UiKit.textField("08xxxxxxxxxx");
        addressArea = WorkspaceWidgets.createEditableTextArea(3);
        bioArea = WorkspaceWidgets.createEditableTextArea(3);
        profileFeedbackLabel = WorkspaceWidgets.createFeedbackBanner();

        JButton saveProfileBtn = UiKit.primaryButton("Simpan Profil", VectorIcons.Kind.CHECK);
        saveProfileBtn.addActionListener(e -> saveProfile());

        JPanel profileFormCard = buildProfileFormCard(saveProfileBtn);

        JPanel profileContent = new JPanel(new BorderLayout(Theme.SPACE_16, 0));
        profileContent.setOpaque(false);
        profileContent.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        profileContent.add(avatarCard, BorderLayout.WEST);
        profileContent.add(profileFormCard, BorderLayout.CENTER);

        JScrollPane profileTabScroll = WorkspaceWidgets.verticalScrollPane(profileContent, Theme.BG);

        // --- Tab 2: Hewan Peliharaan Saya ---
        petsCountLabel = UiKit.label("0 hewan peliharaan", Theme.smallBold(), Theme.MUTED);
        JButton addPetButton = UiKit.primaryButton("+ Tambah Hewan", VectorIcons.Kind.PLUS);
        addPetButton.addActionListener(e -> openPetDialog(null));

        editPetButton = UiKit.secondaryButton("Edit", VectorIcons.Kind.EDIT);
        editPetButton.setEnabled(false);
        editPetButton.addActionListener(e -> editSelectedPet());

        deletePetButton = UiKit.dangerButton("Hapus", VectorIcons.Kind.TRASH);
        deletePetButton.setEnabled(false);
        deletePetButton.addActionListener(e -> deleteSelectedPet());

        JPanel petActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        petActions.setOpaque(false);
        petActions.add(addPetButton);
        petActions.add(editPetButton);
        petActions.add(deletePetButton);

        JPanel petToolbar = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        petToolbar.add(petsCountLabel, BorderLayout.WEST);
        petToolbar.add(petActions, BorderLayout.EAST);

        petsModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "Nama", "Jenis", "Ras", "Gender", "Umur", "Status Kesehatan" }
        );
        petsTable = UiKit.table(petsModel);
        configurePetColumns();
        petsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePetSelectionButtons();
            }
        });

        JPanel petsTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        petsTab.setOpaque(false);
        petsTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        petsTab.add(petToolbar, BorderLayout.NORTH);
        petsTab.add(UiKit.scrollPane(petsTable), BorderLayout.CENTER);

        // --- Tab 3: Keamanan / Sandi ---
        currentPasswordField = UiKit.passwordField("Masukkan kata sandi saat ini");
        newPasswordField = UiKit.passwordField("Minimal 6 karakter");
        confirmPasswordField = UiKit.passwordField("Ulangi kata sandi baru");
        passwordFeedbackLabel = WorkspaceWidgets.createFeedbackBanner();
        JButton changePasswordBtn = UiKit.primaryButton("Ubah Kata Sandi", VectorIcons.Kind.SHIELD);
        changePasswordBtn.addActionListener(e -> submitPasswordChange());

        JScrollPane securityTabScroll = buildSecurityTab(changePasswordBtn);

        tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyBold());
        tabs.setBackground(Theme.SURFACE);
        tabs.setForeground(Theme.INK);
        tabs.addTab("Data Profil", profileTabScroll);
        tabs.addTab("Hewan Peliharaan Saya", petsTab);
        tabs.addTab("Keamanan / Sandi", securityTabScroll);

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeroHeaderCard() {
        JPanel heroCard = UiKit.pastelCardPanel(
            new BorderLayout(Theme.SPACE_16, Theme.SPACE_12),
            Theme.PASTEL_BLUE,
            new Color(0xBB, 0xD7, 0xFA),
            22
        );
        heroCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        JPanel avatarHost = new JPanel(new GridBagLayout());
        avatarHost.setOpaque(false);
        avatarHost.add(heroAvatar);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        nameRow.setOpaque(false);
        nameRow.add(heroNameLabel);
        nameRow.add(heroRoleBadge);

        JPanel identityStack = new JPanel();
        identityStack.setLayout(new BoxLayout(identityStack, BoxLayout.Y_AXIS));
        identityStack.setOpaque(false);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        heroMetaLabel.setAlignmentX(LEFT_ALIGNMENT);
        identityStack.add(nameRow);
        identityStack.add(Box.createVerticalStrut(4));
        identityStack.add(heroMetaLabel);

        JPanel identityCenter = new JPanel(new GridBagLayout());
        identityCenter.setOpaque(false);
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.gridx = 0;
        igbc.gridy = 0;
        igbc.weightx = 1.0;
        igbc.fill = GridBagConstraints.HORIZONTAL;
        igbc.anchor = GridBagConstraints.WEST;
        identityCenter.add(identityStack, igbc);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        statsRow.setOpaque(false);
        statsRow.add(hewanStatChip);
        statsRow.add(pesananStatChip);
        statsRow.add(adopsiBookingStatChip);

        JPanel statsCenter = new JPanel(new GridBagLayout());
        statsCenter.setOpaque(false);
        statsCenter.add(statsRow);

        heroCard.add(avatarHost, BorderLayout.WEST);
        heroCard.add(identityCenter, BorderLayout.CENTER);
        heroCard.add(statsCenter, BorderLayout.EAST);
        return heroCard;
    }

    private JPanel buildProfileFormCard(JButton saveProfileBtn) {
        JPanel card = UiKit.cardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Nama Lengkap", nameField);
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Username", usernameField);
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Email", emailField);
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Telepon", phoneField);
        addScrollGroup(card, gbc, row++, "Alamat", addressArea, 72);
        addScrollGroup(card, gbc, row++, "Bio", bioArea, 72);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 8, 0);
        card.add(profileFeedbackLabel, gbc);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(4, 0, 0, 0);
        card.add(saveProfileBtn, gbc);

        return card;
    }

    private JScrollPane buildSecurityTab(JButton changePasswordBtn) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));

        JPanel card = UiKit.cardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(UiKit.label("Ubah Kata Sandi", Theme.heading(), Theme.INK));
        header.add(Box.createVerticalStrut(4));
        header.add(UiKit.label(
            "Gunakan minimal 6 karakter yang unik untuk menjaga keamanan akun PawPaw Anda.",
            Theme.small(),
            Theme.MUTED
        ));

        int row = 0;
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, Theme.SPACE_16, 0);
        card.add(header, gbc);

        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Kata Sandi Saat Ini", currentPasswordField);
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Kata Sandi Baru", newPasswordField);
        WorkspaceWidgets.addFieldGroup(card, gbc, row++, "Konfirmasi Kata Sandi Baru", confirmPasswordField);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 8, 0);
        card.add(passwordFeedbackLabel, gbc);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 0, 0, 0);
        card.add(changePasswordBtn, gbc);

        wrapper.add(card, BorderLayout.NORTH);
        return WorkspaceWidgets.verticalScrollPane(wrapper, Theme.BG);
    }

    private void configurePetColumns() {
        petsTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        petsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        petsTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        petsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        petsTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        petsTable.getColumnModel().getColumn(5).setPreferredWidth(180);
    }

    @Override
    public String getPageKey() {
        return "user.profile";
    }

    @Override
    public String getPageTitle() {
        return "Profil & Hewan Saya";
    }

    @Override
    public String getPageSubtitle() {
        return "Perbarui biodata akun, kelola hewan peliharaan, dan ganti kata sandi";
    }

    @Override
    public void refreshData() {
        WorkspaceWidgets.hideBannerInSlot(errorBannerSlot, inlineErrorLabel);
        User sessionUser = app.getSession().getUser();
        if (sessionUser == null) {
            updateHero(null, 0, 0, 0);
            populateProfileForm(null);
            ownedPets = new ArrayList<>();
            petsModel.setRowCount(0);
            petsCountLabel.setText("0 hewan peliharaan");
            updatePetSelectionButtons();
            statusSummary = "Belum ada sesi pengguna aktif.";
            return;
        }

        try {
            ProfileService.ProfileSummary summary = app.getProfileService().loadSummary(sessionUser.getUserId());
            User freshUser = summary.user() != null ? summary.user() : sessionUser;
            populateProfileForm(freshUser);

            ownedPets = new ArrayList<>(summary.pets());
            petsModel.setRowCount(0);
            for (Pet pet : ownedPets) {
                petsModel.addRow(new Object[] {
                    WorkspaceWidgets.safeText(pet.getPetName()),
                    WorkspaceWidgets.safeText(pet.getSpecies()),
                    WorkspaceWidgets.safeText(pet.getBreed()),
                    WorkspaceWidgets.safeText(pet.getGender()),
                    WorkspaceWidgets.safeText(pet.getAge()),
                    WorkspaceWidgets.safeText(pet.getHealthStatus())
                });
            }
            petsCountLabel.setText(ownedPets.size() + " hewan peliharaan terdaftar");
            updatePetSelectionButtons();

            updateHero(
                freshUser,
                ownedPets.size(),
                summary.ordersCount(),
                summary.adoptionsCount() + summary.bookingsCount()
            );

            statusSummary = "Profil dimuat: " + WorkspaceWidgets.safeText(freshUser.getName())
                + " (" + ownedPets.size() + " hewan peliharaan).";
        } catch (RuntimeException ex) {
            updateHero(sessionUser, 0, 0, 0);
            populateProfileForm(sessionUser);
            ownedPets = new ArrayList<>();
            petsModel.setRowCount(0);
            petsCountLabel.setText("0 hewan peliharaan");
            updatePetSelectionButtons();
            String msg = WorkspaceWidgets.safeErrorMessage(ex);
            WorkspaceWidgets.showBannerInSlot(
                errorBannerSlot,
                inlineErrorLabel,
                "Gagal memuat data profil: " + msg,
                true
            );
            statusSummary = "Gagal memuat profil: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void updateHero(User user, int petsCount, int ordersCount, int activityCount) {
        updateHeroIdentity(user);
        hewanStatChip.setValue(String.valueOf(Math.max(0, petsCount)));
        pesananStatChip.setValue(String.valueOf(Math.max(0, ordersCount)));
        adopsiBookingStatChip.setValue(String.valueOf(Math.max(0, activityCount)));
    }

    private void updateHeroIdentity(User user) {
        if (user == null) {
            heroAvatar.setImagePath(null);
            heroNameLabel.setText("Pengguna");
            heroRoleBadge.setText("Pelanggan");
            heroMetaLabel.setText("@- \u2022 -");
            return;
        }
        heroAvatar.setImagePath(user.getAvatar());
        heroNameLabel.setText(WorkspaceWidgets.safeText(user.getName()));
        heroRoleBadge.setText(user.isAdmin() ? "Administrator" : "Pelanggan");
        heroMetaLabel.setText(
            "@" + WorkspaceWidgets.safeText(user.getUsername())
                + " \u2022 " + WorkspaceWidgets.safeText(user.getEmail())
        );
    }

    private void populateProfileForm(User user) {
        if (user == null) {
            pendingAvatarPath = null;
            avatarPreview.setImagePath(null);
            avatarPathLabel.setText("Belum ada foto unggahan");
            nameField.setText("");
            usernameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            addressArea.setText("");
            bioArea.setText("");
            return;
        }
        pendingAvatarPath = user.getAvatar();
        avatarPreview.setImagePath(pendingAvatarPath);
        avatarPathLabel.setText(formatAvatarHint(pendingAvatarPath));
        nameField.setText(WorkspaceWidgets.nonNull(user.getName()));
        usernameField.setText(WorkspaceWidgets.nonNull(user.getUsername()));
        emailField.setText(WorkspaceWidgets.nonNull(user.getEmail()));
        phoneField.setText(WorkspaceWidgets.nonNull(user.getPhone()));
        addressArea.setText(WorkspaceWidgets.nonNull(user.getAddress()));
        bioArea.setText(WorkspaceWidgets.nonNull(user.getBio()));
    }

    private void chooseAvatarImage() {
        User user = app.getSession().getUser();
        if (user == null) {
            UiKit.showError(this, "Silakan masuk terlebih dahulu.");
            return;
        }
        String chosen = UiKit.chooseAndCopyImage(this, "avatars", "avatar-" + user.getUserId());
        if (chosen != null) {
            pendingAvatarPath = chosen;
            avatarPreview.setImagePath(chosen);
            avatarPathLabel.setText(formatAvatarHint(chosen));
        }
    }

    private void saveProfile() {
        WorkspaceWidgets.hideBanner(profileFeedbackLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            setFeedback(profileFeedbackLabel, "Sesi pengguna tidak aktif. Silakan masuk kembali.", true);
            return;
        }

        try {
            User updated = app.getProfileService().updateProfile(
                user.getUserId(),
                nameField.getText(),
                usernameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                addressArea.getText(),
                bioArea.getText(),
                pendingAvatarPath
            );
            app.getSession().setUser(updated);
            app.refreshSessionHeader();
            updateHeroIdentity(updated);
            populateProfileForm(updated);
            setFeedback(profileFeedbackLabel, "Profil akun berhasil disimpan.", false);
            statusSummary = "Profil diperbarui: " + WorkspaceWidgets.safeText(updated.getName()) + ".";
            app.updateStatus(statusSummary);
        } catch (RuntimeException ex) {
            setFeedback(profileFeedbackLabel, WorkspaceWidgets.safeErrorMessage(ex), true);
        }
    }

    private void openPetDialog(Pet existing) {
        User user = app.getSession().getUser();
        if (user == null) {
            UiKit.showError(this, "Silakan masuk terlebih dahulu.");
            return;
        }
        if (PetFormDialog.show(app, existing)) {
            refreshData();
            app.updateStatus(getStatusSummary());
        }
    }

    private void editSelectedPet() {
        Pet selected = getSelectedPet();
        if (selected != null) {
            openPetDialog(selected);
        }
    }

    private void deleteSelectedPet() {
        User user = app.getSession().getUser();
        if (user == null) {
            UiKit.showError(this, "Silakan masuk terlebih dahulu.");
            return;
        }
        Pet selected = getSelectedPet();
        if (selected == null) {
            return;
        }
        boolean confirmed = UiKit.confirm(
            this,
            "Hapus Hewan Peliharaan",
            "Yakin ingin menghapus data hewan \"" + WorkspaceWidgets.safeText(selected.getPetName()) + "\"?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getProfileService().deletePet(user.getUserId(), selected.getPetId());
            refreshData();
            app.updateStatus(getStatusSummary());
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private Pet getSelectedPet() {
        int viewRow = petsTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= petsTable.getRowCount()) {
            return null;
        }
        int modelRow = petsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= ownedPets.size()) {
            return null;
        }
        return ownedPets.get(modelRow);
    }

    private void updatePetSelectionButtons() {
        boolean hasSelection = getSelectedPet() != null;
        editPetButton.setEnabled(hasSelection);
        deletePetButton.setEnabled(hasSelection);
    }

    private void submitPasswordChange() {
        WorkspaceWidgets.hideBanner(passwordFeedbackLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            setFeedback(passwordFeedbackLabel, "Sesi pengguna tidak aktif. Silakan masuk kembali.", true);
            return;
        }

        String currentPw = new String(currentPasswordField.getPassword());
        String newPw = new String(newPasswordField.getPassword());
        String confirmPw = new String(confirmPasswordField.getPassword());

        try {
            app.getProfileService().changePassword(user.getUserId(), currentPw, newPw, confirmPw);
            currentPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            setFeedback(passwordFeedbackLabel, "Kata sandi berhasil diperbarui.", false);
            app.updateStatus("Kata sandi akun berhasil diperbarui.");
        } catch (RuntimeException ex) {
            setFeedback(passwordFeedbackLabel, WorkspaceWidgets.safeErrorMessage(ex), true);
        }
    }

    private static void addScrollGroup(
            JPanel container,
            GridBagConstraints gbc,
            int rowIndex,
            String labelText,
            JTextArea area,
            int preferredHeight) {
        JScrollPane scroll = UiKit.scrollPane(area);
        scroll.setPreferredSize(new Dimension(360, preferredHeight));
        WorkspaceWidgets.addFieldGroup(container, gbc, rowIndex, labelText, scroll);
    }

    private void setFeedback(JLabel target, String message, boolean error) {
        WorkspaceWidgets.showBanner(target, message, error);
        revalidate();
        repaint();
    }

    private static String formatAvatarHint(String path) {
        if (path == null || path.isBlank()) {
            return "Belum ada foto unggahan";
        }
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    /**
     * Compact pastel stat chip displayed inside the profile hero header.
     */
    private static final class StatChip extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel valueLabel;

        StatChip(String title, Color fill) {
            super(new BorderLayout(0, 2));
            setOpaque(false);

            JPanel card = UiKit.pastelCardPanel(
                new BorderLayout(0, 2),
                fill,
                Theme.tint(Theme.NAVY, 24),
                14
            );
            card.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 14));

            valueLabel = UiKit.label("0", Theme.heading(), Theme.INK);
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel titleLabel = UiKit.label(title, Theme.badge(), Theme.MUTED);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            card.add(valueLabel, BorderLayout.CENTER);
            card.add(titleLabel, BorderLayout.SOUTH);
            add(card, BorderLayout.CENTER);
        }

        void setValue(String value) {
            valueLabel.setText(value != null ? value : "0");
        }
    }

    private static final class PetFormDialog extends JDialog {
        private static final long serialVersionUID = 1L;

        private final transient AppWindow app;
        private final transient Pet existingPet;

        private final JTextField petNameField;
        private final JComboBox<String> speciesCombo;
        private final JTextField breedField;
        private final JComboBox<String> genderCombo;
        private final JTextField ageField;
        private final JTextField healthStatusField;
        private final JLabel errorLabel;

        private boolean saved;

        private PetFormDialog(AppWindow app, Pet existingPet) {
            super(
                app,
                existingPet == null ? "Tambah Hewan Peliharaan" : "Edit Hewan Peliharaan",
                ModalityType.APPLICATION_MODAL
            );
            this.app = app;
            this.existingPet = existingPet;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            petNameField = UiKit.textField("Nama hewan peliharaan");
            speciesCombo = UiKit.comboBox(PET_SPECIES_OPTIONS);
            speciesCombo.setEditable(true);
            breedField = UiKit.textField("Campuran");
            genderCombo = UiKit.comboBox(PET_GENDER_OPTIONS);
            ageField = UiKit.textField("1 Tahun");
            healthStatusField = UiKit.textField("Sehat");
            errorLabel = WorkspaceWidgets.createFeedbackBanner();

            if (existingPet != null) {
                petNameField.setText(WorkspaceWidgets.nonNull(existingPet.getPetName()));
                speciesCombo.setSelectedItem(WorkspaceWidgets.nonNull(existingPet.getSpecies()));
                breedField.setText(WorkspaceWidgets.nonNull(existingPet.getBreed()));
                genderCombo.setSelectedItem(WorkspaceWidgets.nonNull(existingPet.getGender()));
                ageField.setText(WorkspaceWidgets.nonNull(existingPet.getAge()));
                healthStatusField.setText(WorkspaceWidgets.nonNull(existingPet.getHealthStatus()));
            } else {
                speciesCombo.setSelectedItem("Kucing");
                breedField.setText("Campuran");
                genderCombo.setSelectedItem("Jantan");
                ageField.setText("1 Tahun");
                healthStatusField.setText("Sehat");
            }

            JPanel formCard = UiKit.cardPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;

            int row = 0;
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Nama Hewan", petNameField);
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Jenis Hewan", speciesCombo);
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Ras", breedField);
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Gender", genderCombo);
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Umur", ageField);
            WorkspaceWidgets.addFieldGroup(formCard, gbc, row++, "Status Kesehatan", healthStatusField);

            gbc.gridy = row;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(4, 0, 0, 0);
            formCard.add(errorLabel, gbc);

            JButton cancelBtn = UiKit.secondaryButton("Batal", VectorIcons.Kind.CLOSE);
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = UiKit.primaryButton("Simpan", VectorIcons.Kind.CHECK);
            saveBtn.addActionListener(e -> submitPet());

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
            footer.setOpaque(false);
            footer.add(cancelBtn);
            footer.add(saveBtn);

            JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_16));
            root.setBackground(Theme.BG);
            root.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
            ));
            root.add(WorkspaceWidgets.verticalScrollPane(formCard, Theme.BG), BorderLayout.CENTER);
            root.add(footer, BorderLayout.SOUTH);

            setContentPane(root);
            getRootPane().setDefaultButton(saveBtn);
            WorkspaceWidgets.registerEscapeClose(this);

            setMinimumSize(new Dimension(440, 480));
            setPreferredSize(new Dimension(460, 520));
            pack();
            setLocationRelativeTo(app);
        }

        static boolean show(AppWindow app, Pet existingPet) {
            PetFormDialog dialog = new PetFormDialog(app, existingPet);
            dialog.setVisible(true);
            return dialog.saved;
        }

        private void submitPet() {
            WorkspaceWidgets.hideBanner(errorLabel);
            User user = app.getSession().getUser();
            if (user == null) {
                showInlineError("Sesi pengguna tidak aktif.");
                return;
            }

            Object rawSpecies = speciesCombo.isEditable()
                ? speciesCombo.getEditor().getItem()
                : speciesCombo.getSelectedItem();
            Object rawGender = genderCombo.getSelectedItem();

            Pet target = new Pet();
            if (existingPet != null) {
                target.setPetId(existingPet.getPetId());
            }
            target.setPetName(petNameField.getText());
            target.setSpecies(rawSpecies == null ? "" : rawSpecies.toString());
            target.setBreed(breedField.getText());
            target.setGender(rawGender == null ? "Jantan" : rawGender.toString());
            target.setAge(ageField.getText());
            target.setHealthStatus(healthStatusField.getText());

            try {
                app.getProfileService().savePet(user.getUserId(), target);
                saved = true;
                dispose();
            } catch (RuntimeException ex) {
                showInlineError(WorkspaceWidgets.safeErrorMessage(ex));
            }
        }

        private void showInlineError(String message) {
            WorkspaceWidgets.showBanner(errorLabel, message, true);
            revalidate();
            repaint();
        }
    }
}
