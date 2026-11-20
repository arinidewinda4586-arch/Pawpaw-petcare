package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin verification workspace for reviewing adoption requests, inspecting animal/applicant
 * details, adding admin notes, and approving/rejecting/reopening non-terminal requests.
 */
public final class AdminAdoptionsPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] STATUS_FILTERS = {
        "Semua", "Menunggu", "Disetujui", "Ditolak", "Dibatalkan"
    };

    private final transient AppWindow app;

    private final JLabel inlineFeedbackLabel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JLabel countLabel;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel adoptionModel;
    private final JTable adoptionTable;

    private final PhotoPanel animalPhoto;
    private final JLabel inspectorTitleLabel;
    private final JLabel inspectorMetaLabel;
    private final AdminWidgets.StatusPill animalStatusPill;
    private final AdminWidgets.StatusPill requestStatusPill;
    private final JLabel applicantValueLabel;
    private final JLabel emailValueLabel;
    private final JLabel phoneValueLabel;
    private final JLabel addressValueLabel;
    private final JTextArea reasonArea;
    private final JTextField adminNoteField;
    private final JLabel terminalNoteLabel;
    private JPanel terminalNoteRow;
    private final JButton approveButton;
    private final JButton rejectButton;
    private final JButton pendingButton;

    private transient List<AdoptionRequest> allRequests = new ArrayList<>();
    private transient List<AdoptionRequest> filteredRequests = new ArrayList<>();
    private transient AdoptionRequest selectedRequest;
    private String statusSummary = "Verifikasi adopsi siap.";

    public AdminAdoptionsPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineFeedbackLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari pemohon, email, atau nama hewan...");
        searchField.setPreferredSize(new Dimension(225, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        statusFilterCombo = AdminWidgets.adminComboBox(STATUS_FILTERS);
        statusFilterCombo.addActionListener(e -> applyFilter());

        countLabel = UiKit.label("0 pengajuan", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel toolbar = AdminWidgets.buildTransactionToolbar(
            searchField, statusFilterCombo, countLabel, refreshButton
        );
        JPanel northStack = AdminWidgets.buildNorthStack(inlineFeedbackLabel, toolbar);

        adoptionModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Tanggal", "Hewan", "Jenis / Ras", "Pemohon", "Telepon", "Status" },
            new Class<?>[] {
                Integer.class, String.class, String.class, String.class,
                String.class, String.class, String.class
            }
        );
        adoptionTable = UiKit.adminTable(adoptionModel);
        adoptionTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        adoptionTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        configureColumns();
        AdminWidgets.wireTableSelection(adoptionTable, this::onSelectionChanged);

        // --- Bottom Inspector ---
        animalPhoto = new PhotoPanel(68, 68, Theme.Admin.BUTTON_RADIUS);
        inspectorTitleLabel = UiKit.label("Pilih pengajuan adopsi pada tabel", Theme.heading(), Theme.Admin.TEXT);
        inspectorMetaLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        animalStatusPill = new AdminWidgets.StatusPill();
        requestStatusPill = new AdminWidgets.StatusPill();

        applicantValueLabel = UiKit.label("-", Theme.bodyBold(), Theme.Admin.TEXT);
        emailValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        phoneValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        addressValueLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT);

        reasonArea = AdminWidgets.createReadOnlyTextArea(3);
        adminNoteField = UiKit.textField("Catatan admin untuk keputusan ini (opsional)...");
        adminNoteField.setEnabled(false);

        terminalNoteLabel = UiKit.label("Status final terkunci", Theme.smallBold(), Theme.Admin.TEXT_SOFT);
        terminalNoteLabel.setVisible(false);

        approveButton = UiKit.adminPrimaryButton("Setujui", VectorIcons.Kind.CHECK);
        approveButton.setEnabled(false);
        approveButton.addActionListener(e -> updateSelectedRequest("approved"));

        rejectButton = UiKit.adminDeleteButton("Tolak", VectorIcons.Kind.CLOSE);
        rejectButton.setEnabled(false);
        rejectButton.addActionListener(e -> updateSelectedRequest("rejected"));

        pendingButton = UiKit.adminLightButton("Pending", VectorIcons.Kind.REFRESH);
        pendingButton.setEnabled(false);
        pendingButton.addActionListener(e -> updateSelectedRequest("pending"));

        JPanel inspectorBody = buildInspectorContent();
        JSplitPane splitPane = AdminWidgets.buildMasterDetailSplit(adoptionTable, inspectorBody);

        add(northStack, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    protected Color defaultBackground() {
        return Theme.Admin.BG;
    }

    private JPanel buildInspectorContent() {
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(Theme.Admin.SURFACE);
        stack.setOpaque(true);
        stack.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
        ));

        JPanel headerRow = new AdminWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_12, 0));
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        inspectorTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        inspectorMetaLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel animalStatusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        animalStatusRow.setOpaque(false);
        animalStatusRow.setAlignmentX(LEFT_ALIGNMENT);
        animalStatusRow.add(UiKit.label("Status Hewan:", Theme.smallBold(), Theme.Admin.TEXT_SOFT));
        animalStatusRow.add(animalStatusPill);

        textCol.add(inspectorTitleLabel);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(inspectorMetaLabel);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(animalStatusRow);

        JPanel reqPillWrap = new JPanel(new GridBagLayout());
        reqPillWrap.setOpaque(false);
        reqPillWrap.add(requestStatusPill);

        headerRow.add(animalPhoto, BorderLayout.WEST);
        headerRow.add(textCol, BorderLayout.CENTER);
        headerRow.add(reqPillWrap, BorderLayout.EAST);

        JPanel detailGrid = new AdminWidgets.CompactSectionPanel(new GridLayout(1, 2, Theme.SPACE_12, 0));

        JPanel applicantCard = AdminWidgets.createInspectorSubCard(new GridBagLayout());
        int aRow = 0;
        AdminWidgets.addAttrRow(applicantCard, aRow++, "Pemohon", applicantValueLabel);
        AdminWidgets.addAttrRow(applicantCard, aRow++, "Email", emailValueLabel);
        AdminWidgets.addAttrRow(applicantCard, aRow++, "Telepon", phoneValueLabel);
        AdminWidgets.addAttrRow(applicantCard, aRow, "Alamat", addressValueLabel);

        JPanel reasonCard = AdminWidgets.createInspectorSubCard(new BorderLayout(0, 6));
        reasonCard.add(UiKit.label("Alasan Pengajuan Adopsi", Theme.smallBold(), Theme.Admin.TEXT_SOFT), BorderLayout.NORTH);
        JScrollPane reasonScroll = AdminWidgets.adminScrollPane(reasonArea);
        reasonScroll.setPreferredSize(new Dimension(240, 82));
        reasonCard.add(reasonScroll, BorderLayout.CENTER);

        detailGrid.add(applicantCard);
        detailGrid.add(reasonCard);

        JPanel noteRow = new AdminWidgets.CompactSectionPanel(new BorderLayout(0, 4));
        noteRow.add(AdminWidgets.adminFormLabel("Catatan Admin"), BorderLayout.NORTH);
        noteRow.add(adminNoteField, BorderLayout.CENTER);

        terminalNoteRow = AdminWidgets.createNoteRow(terminalNoteLabel);

        JPanel actionBar = new AdminWidgets.CompactSectionPanel(new BorderLayout());
        JPanel decisionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        decisionButtons.setOpaque(false);
        decisionButtons.add(pendingButton);
        decisionButtons.add(rejectButton);
        decisionButtons.add(approveButton);
        actionBar.add(decisionButtons, BorderLayout.EAST);

        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        detailGrid.setAlignmentX(LEFT_ALIGNMENT);
        noteRow.setAlignmentX(LEFT_ALIGNMENT);
        terminalNoteRow.setAlignmentX(LEFT_ALIGNMENT);
        actionBar.setAlignmentX(LEFT_ALIGNMENT);

        stack.add(headerRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(detailGrid);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(noteRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(terminalNoteRow);
        stack.add(actionBar);
        return stack;
    }

    private void configureColumns() {
        adoptionTable.getColumnModel().getColumn(0).setPreferredWidth(58);
        adoptionTable.getColumnModel().getColumn(1).setPreferredWidth(135);
        adoptionTable.getColumnModel().getColumn(2).setPreferredWidth(115);
        adoptionTable.getColumnModel().getColumn(3).setPreferredWidth(145);
        adoptionTable.getColumnModel().getColumn(4).setPreferredWidth(155);
        adoptionTable.getColumnModel().getColumn(5).setPreferredWidth(115);
        adoptionTable.getColumnModel().getColumn(6).setPreferredWidth(115);
    }

    @Override
    public String getPageKey() {
        return "admin.adoptions";
    }

    @Override
    public String getPageTitle() {
        return "Verifikasi Adopsi";
    }

    @Override
    public String getPageSubtitle() {
        return "Tinjau pengajuan adopsi masuk dan setujui atau tolak dengan catatan";
    }

    @Override
    public void refreshData() {
        int prevId = selectedRequest != null ? selectedRequest.getAdoptionRequestId() : -1;
        AdminWidgets.hideBanner(inlineFeedbackLabel);

        if (!isAdminActive()) {
            allRequests = new ArrayList<>();
            filteredRequests = new ArrayList<>();
            adoptionModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 pengajuan");
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        try {
            allRequests = new ArrayList<>(app.getAdoptionService().listAllRequests());
            applyFilter(prevId);
            statusSummary = "Pengajuan adopsi dimuat: " + allRequests.size() + " permohonan.";
        } catch (RuntimeException ex) {
            allRequests = new ArrayList<>();
            filteredRequests = new ArrayList<>();
            adoptionModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 pengajuan");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memuat pengajuan adopsi: " + msg, true);
            statusSummary = "Gagal memuat pengajuan adopsi: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private boolean isAdminActive() {
        User user = app.getSession().getUser();
        return user != null && user.isAdmin();
    }

    private void applyFilter() {
        int currentId = selectedRequest != null ? selectedRequest.getAdoptionRequestId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredRequestId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object statusObj = statusFilterCombo.getSelectedItem();

        filteredRequests = new ArrayList<>();
        adoptionModel.setRowCount(0);

        for (AdoptionRequest req : allRequests) {
            if (!AdminWidgets.matchesStatusFilter(req.getStatus(), statusObj) || !matchesQuery(req, query)) {
                continue;
            }
            filteredRequests.add(req);

            String animalName = formatAnimalName(req);
            String species = AdminWidgets.capitalizeWords(req.getAnimalSpecies());
            String breed = AdminWidgets.safeText(req.getAnimalBreed());
            String speciesBreed = "-".equals(breed) ? species : species + " - " + breed;
            String applicant = (req.getApplicantName() != null && !req.getApplicantName().isBlank())
                ? req.getApplicantName()
                : AdminWidgets.safeText(req.getUserName());

            adoptionModel.addRow(new Object[] {
                Integer.valueOf(req.getAdoptionRequestId()),
                FormatUtil.dateTimeId(req.getRequestDate()),
                animalName,
                speciesBreed,
                applicant,
                AdminWidgets.safeText(req.getPhone()),
                AdminWidgets.safeText(req.getStatus())
            });
        }

        countLabel.setText(filteredRequests.size() + " / " + allRequests.size() + " pengajuan");
        AdminWidgets.restoreTableSelection(
            adoptionTable,
            filteredRequests,
            AdoptionRequest::getAdoptionRequestId,
            preferredRequestId,
            this::onSelectionChanged,
            this::clearSelection
        );
    }

    private void onSelectionChanged() {
        AdoptionRequest req = AdminWidgets.resolveSelectedItem(adoptionTable, filteredRequests);
        if (req == null) {
            clearSelection();
            return;
        }
        selectedRequest = req;

        animalPhoto.setImagePath(req.getAnimalImage());
        String animalName = formatAnimalName(req);
        inspectorTitleLabel.setText("Pengajuan #" + req.getAdoptionRequestId() + " — " + animalName);
        inspectorMetaLabel.setText(
            AdminWidgets.capitalizeWords(req.getAnimalSpecies())
                + "  |  Ras: " + AdminWidgets.safeText(req.getAnimalBreed())
                + "  |  Tanggal: " + FormatUtil.dateTimeId(req.getRequestDate())
        );
        animalStatusPill.setStatus(req.getAnimalStatus());
        requestStatusPill.setStatus(req.getStatus());

        applicantValueLabel.setText(AdminWidgets.safeText(req.getApplicantName()));
        emailValueLabel.setText(AdminWidgets.safeText(req.getEmail()));
        phoneValueLabel.setText(AdminWidgets.safeText(req.getPhone()));
        String address = AdminWidgets.safeText(req.getAddress());
        addressValueLabel.setText("<html>" + AdminWidgets.escapeHtml(address) + "</html>");
        addressValueLabel.setToolTipText(address);

        reasonArea.setText(AdminWidgets.safeText(req.getReason()));
        reasonArea.setCaretPosition(0);

        adminNoteField.setText(AdminWidgets.nonNull(req.getAdminNote()));

        boolean adminActive = isAdminActive();
        boolean terminal = isTerminalRequestStatus(req.getStatus());
        boolean animalAvailable = "available".equalsIgnoreCase(req.getAnimalStatus());

        adminNoteField.setEnabled(adminActive && !terminal);
        approveButton.setEnabled(adminActive && !terminal && animalAvailable);
        rejectButton.setEnabled(adminActive && !terminal);
        pendingButton.setEnabled(adminActive && !terminal);

        if (terminal) {
            AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, true, "Status final terkunci");
        } else if (!animalAvailable) {
            AdminWidgets.setNoteRowVisible(
                terminalNoteRow,
                terminalNoteLabel,
                true,
                "Hewan berstatus " + FormatUtil.statusLabel(req.getAnimalStatus()) + " — tidak dapat disetujui"
            );
        } else {
            AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, false, null);
        }
    }

    private void clearSelection() {
        selectedRequest = null;
        animalPhoto.setImagePath(null);
        inspectorTitleLabel.setText("Pilih pengajuan adopsi pada tabel");
        inspectorMetaLabel.setText("-");
        animalStatusPill.setStatus("");
        requestStatusPill.setStatus("");
        applicantValueLabel.setText("-");
        emailValueLabel.setText("-");
        phoneValueLabel.setText("-");
        addressValueLabel.setText("-");
        addressValueLabel.setToolTipText(null);
        reasonArea.setText("");
        adminNoteField.setText("");
        adminNoteField.setEnabled(false);
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        pendingButton.setEnabled(false);
        AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, false, null);
    }

    private void updateSelectedRequest(String targetStatus) {
        AdminWidgets.hideBanner(inlineFeedbackLabel);
        AdoptionRequest target = AdminWidgets.resolveSelectedItem(adoptionTable, filteredRequests);
        if (!isAdminActive() || target == null) {
            return;
        }
        if (isTerminalRequestStatus(target.getStatus())) {
            AdminWidgets.showBanner(
                inlineFeedbackLabel,
                "Pengajuan berstatus final (" + FormatUtil.statusLabel(target.getStatus()) + ") sudah terkunci.",
                true
            );
            return;
        }

        int requestId = target.getAdoptionRequestId();
        String animalName = formatAnimalName(target);
        if ("approved".equalsIgnoreCase(targetStatus)) {
            boolean confirmed = UiKit.confirm(
                this,
                "Setujui Pengajuan Adopsi",
                "Setujui pengajuan #" + requestId + " untuk mengadopsi \"" + animalName + "\"?\n"
                    + "Status hewan akan berubah menjadi Sudah Diadopsi (adopted) dan semua pengajuan "
                    + "pending lainnya untuk hewan ini akan ditolak otomatis."
            );
            if (!confirmed) {
                return;
            }
        }

        String note = adminNoteField.getText();
        try {
            app.getAdoptionService().updateRequestStatus(requestId, targetStatus, note);
            refreshData();
            AdminWidgets.restoreTableSelection(
                adoptionTable,
                filteredRequests,
                AdoptionRequest::getAdoptionRequestId,
                requestId,
                this::onSelectionChanged,
                this::clearSelection
            );
            String msg = "Pengajuan #" + requestId + " diperbarui ke status "
                + FormatUtil.statusLabel(targetStatus) + ".";
            AdminWidgets.showBanner(inlineFeedbackLabel, msg, false);
            statusSummary = msg;
            app.updateStatus(msg);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memperbarui pengajuan adopsi: " + msg, true);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal memperbarui adopsi: " + msg);
        }
    }

    private static boolean isTerminalRequestStatus(String status) {
        if (status == null) {
            return false;
        }
        String clean = status.trim().toLowerCase(Locale.ROOT);
        return "approved".equals(clean) || "cancelled".equals(clean);
    }

    private static String formatAnimalName(AdoptionRequest req) {
        return (req.getAnimalNama() != null && !req.getAnimalNama().isBlank())
            ? req.getAnimalNama()
            : "Hewan #" + req.getAnimalId();
    }

    private static boolean matchesQuery(AdoptionRequest req, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return AdminWidgets.containsIgnoreCase(req.getApplicantName(), query)
            || AdminWidgets.containsIgnoreCase(req.getUserName(), query)
            || AdminWidgets.containsIgnoreCase(req.getEmail(), query)
            || AdminWidgets.containsIgnoreCase(req.getPhone(), query)
            || AdminWidgets.containsIgnoreCase(req.getAnimalNama(), query)
            || AdminWidgets.containsIgnoreCase(req.getAnimalSpecies(), query)
            || AdminWidgets.containsIgnoreCase(req.getAnimalBreed(), query);
    }
}
