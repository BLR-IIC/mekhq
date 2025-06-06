/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui.dialog.resupplyAndCaches;

import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.force.ForceType.CONVOY;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerDescription;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * This class provides utility methods to display dialogs related to the beginning of a contract. It generates
 * user-friendly messages summarizing cargo requirements, player convoy capabilities, and mission details.
 */
public class DialogContractStart extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Displays a dialog at the start of a contract, providing summarized details about the mission and player convoy
     * capabilities. The content is dynamically generated based on the given {@link Campaign} and {@link AtBContract}.
     * <p>
     * This method: - Generates a message summarizing the player's convoy capabilities and cargo capacity. - Fetches
     * localized text from the resource bundle based on the contract type and command rights. - Displays a dialog with
     * visuals (e.g., faction icon) and a confirmation button to proceed.
     *
     * @param campaign the current {@link Campaign}.
     * @param contract the active contract.
     */
    public DialogContractStart(Campaign campaign, AtBContract contract) {
        setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "incomingTransmission.title"));

        // Main Panel to hold both boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        // Left box for speaker details
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Get speaker details
        Person speaker = campaign.getSeniorAdminPerson(AdministratorSpecialization.LOGISTICS);

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            speakerName = campaign.getName();
        }

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIcon(speakerIcon, 100, true);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s</div></html>",
              LEFT_WIDTH,
              speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the leftBox
        leftBox.add(imageLabel);
        leftBox.add(Box.createRigidArea(new Dimension(0, INSERT_SIZE)));
        leftBox.add(leftDescription);

        // Add leftBox to mainPanel
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        mainPanel.add(leftBox, constraints);

        // Right box: Just a message
        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setBorder(BorderFactory.createEtchedBorder());

        String message = generateContractStartMessage(campaign, contract);

        JLabel rightDescription = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s</div></html>",
              RIGHT_WIDTH,
              message));
        rightBox.add(rightDescription);

        // Add rightBox to mainPanel
        constraints.gridx = 1;
        constraints.weightx = 1; // Allow horizontal stretching
        mainPanel.add(rightBox, constraints);

        add(mainPanel, BorderLayout.CENTER);

        // Create a container panel to hold both the button panel and the new panel
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS)); // Stack vertically

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "convoyConfirm.text"));
        confirmButton.addActionListener(e -> dispose());
        buttonPanel.add(confirmButton);

        // Add the button panel to the container
        containerPanel.add(buttonPanel);

        // New panel (to be added below the button panel)
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel lblInfo = new JLabel(String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
              RIGHT_WIDTH + LEFT_WIDTH,
              getFormattedTextAt(RESOURCE_BUNDLE, "documentation.prompt")));
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblInfo, BorderLayout.CENTER);
        infoPanel.setBorder(BorderFactory.createEtchedBorder());

        // Add the new panel to the container (below the button panel)
        containerPanel.add(infoPanel);

        // Add the container panel to the dialog (at the bottom of the layout)
        add(containerPanel, BorderLayout.SOUTH);

        // Dialog settings
        pack();
        setModal(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Generates an HTML-formatted message to display in the start-of-contract dialog. The message includes details such
     * as - Total player convoy cargo capacity. - Number of operational player convoys. - Cargo requirements for the
     * contract.
     * <p>
     * The message format adapts based on the contract type (e.g., guerrilla warfare vs. general contract) and the
     * player's command rights (e.g., independent command).
     * <p>
     * This method: - Iterates through all player forces to calculate total convoy cargo capacity. - Checks for convoy
     * readiness, excluding units that are damaged, uncrewed, or prohibited. - Formats the message using localized
     * templates from the resource bundle.
     *
     * @param campaign the current {@link Campaign}.
     * @param contract the current {@link AtBContract}.
     *
     * @return an HTML-formatted string message summarizing the player's readiness and convoy details in the context of
     *       the contract.
     */
    private static String generateContractStartMessage(Campaign campaign, AtBContract contract) {
        int playerConvoys = 0;
        double totalPlayerCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.isForceType(CONVOY)) {
                continue;
            }

            if (force.getParentForce() != null && force.getParentForce().isForceType(CONVOY)) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            boolean hasCargo = false;
            for (UUID unitId : force.getAllUnits(false)) {
                try {
                    Unit unit = campaign.getUnit(unitId);
                    Entity entity = unit.getEntity();

                    if (unit.isDamaged() || !unit.isFullyCrewed() || isProhibitedUnitType(entity, true, true)) {
                        continue;
                    }

                    double individualCargo = unit.getCargoCapacity();

                    if (individualCargo > 0) {
                        hasCargo = true;
                    }

                    cargoCapacitySubTotal += individualCargo;
                } catch (Exception ignored) {
                    // If we run into an exception, it's because we failed to get Unit or Entity.
                    // In either case, we just ignore that unit.
                }
            }

            if (hasCargo) {
                if (cargoCapacitySubTotal > 0) {
                    totalPlayerCargoCapacity += cargoCapacitySubTotal;
                    playerConvoys++;
                }
            }
        }

        String convoyMessage;
        String commanderTitle = campaign.getCommanderAddress(false);

        if (contract.getContractType().isGuerrillaWarfare()) {
            String convoyMessageTemplate = "contractStartMessageGuerrilla.text";
            convoyMessage = getFormattedTextAt(RESOURCE_BUNDLE, convoyMessageTemplate, commanderTitle);
        } else {
            String convoyMessageTemplate = "contractStartMessageGeneric.text";
            if (contract.getCommandRights().isIndependent()) {
                convoyMessageTemplate = "contractStartMessageIndependent.text";
            }

            convoyMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  convoyMessageTemplate,
                  commanderTitle,
                  estimateCargoRequirements(campaign, contract),
                  totalPlayerCargoCapacity,
                  playerConvoys,
                  playerConvoys != 1 ? "s" : "");
        }

        int width = UIUtil.scaleForGUI(500);
        return String.format("<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
              width,
              convoyMessage);
    }
}
