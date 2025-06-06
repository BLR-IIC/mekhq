/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.common.AmmoType;
import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This will be a special type of part that will only exist as spares
 * It will determine the amount of ammo of a particular type that
 * is available
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class AmmoStorage extends EquipmentPart implements IAcquisitionWork {
    private static final MMLogger logger = MMLogger.create(AmmoStorage.class);

    protected int shots;

    public AmmoStorage() {
        this(0, null, 0, null);
    }

    public AmmoStorage(int tonnage, @Nullable AmmoType et, int shots, @Nullable Campaign c) {
        super(tonnage, et, -1, 1.0, c);
        this.shots = shots;
    }

    @Override
    public AmmoStorage clone() {
        AmmoStorage storage = new AmmoStorage(0, getType(), shots, campaign);
        storage.copyBaseData(this);
        return storage;
    }

    @Override
    public AmmoType getType() {
        return (AmmoType) super.getType();
    }

    @Override
    public double getTonnage() {
        if (getType().getKgPerShot() > 0) {
            return getType().getKgPerShot() * (shots / 1000.0);
        }
        return ((double) shots / getType().getShots());
    }

    @Override
    public Money getStickerPrice() {
        // CAW: previously we went thru AmmoType::getCost, which
        // for AmmoType was the default implementation
        // that simply returned 'cost'. Avoid the hassle for
        // now and just return the raw cost as our sticker price.
        //
        // We should revisit this if you can ever have ammo that
        // should be sold for more based on the unit which carries
        // it, or which location the ammo is stored in, or if
        // the unit which carries the ammo does so in an armored
        // location... but I don't think that's likely.
        return Money.of(getType().getRawCost());
    }

    @Override
    public Money getBuyCost() {
        return getActualValue();
    }

    @Override
    public Money getActualValue() {
        if (getType().getShots() <= 0) {
            return Money.zero();
        }

        return adjustCostsForCampaignOptions(
                getStickerPrice().multipliedBy(shots).dividedBy(getType().getShots()));
    }

    public int getShots() {
        return shots;
    }

    @Override
    public int getSellableQuantity() {
        return getShots();
    }

    @Override
    public boolean isSamePartType(@Nullable Part part) {
        return (getClass() == part.getClass())
                && Objects.equals(getType(), ((AmmoStorage) part).getType());
    }

    /**
     * Gets a value indicating whether or an {@code AmmoType} is
     * the same as this instance's ammo.
     *
     * @param otherAmmoType The other {@code AmmoType}.
     */
    public boolean isSameAmmoType(AmmoType otherAmmoType) {
        return getType().equalsAmmoTypeOnly(otherAmmoType)
                && (getType().getMunitionType().equals(otherAmmoType.getMunitionType()))
                && (getType().getRackSize() == otherAmmoType.getRackSize());
    }

    /**
     * Gets a value indicating whether or not an {@code AmmoType}
     * is compatible with this instance's ammo.
     *
     * @param otherAmmoType The other {@code AmmoType}.
     * @return False if the ammo does not support "compatibility" or is not compatible, true if the ammo type
     * supports compatibility and is compatible
     */
    public boolean isCompatibleAmmo(AmmoType otherAmmoType) {
        return getType().isCompatibleWith(otherAmmoType);
    }

    public void changeShots(int s) {
        shots = Math.max(0, shots + s);
    }

    public void setShots(int s) {
        shots = Math.max(0, s);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", getType().getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shots", shots);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                    typeName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("shots")) {
                    shots = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }

        restore();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return getType().getTechAdvancement();
    }

    @Override
    public void fix() {
        // nothing to fix
    }

    @Override
    public MissingEquipmentPart getMissingPart() {
        // nothing to do here
        return null;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return getNewPart();
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        // nothing to do here
        return null;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // nothing to do here
    }

    @Override
    public void updateConditionFromPart() {
        // nothing to do here
    }

    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public String getDesc() {
        String toReturn = "<html><font";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Reload " + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        toReturn += "" + getTimeLeft() + " minutes" + scheduled;
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        return shots + " shots";
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public String find(int transitDays) {
        AmmoStorage newPart = getNewPart();
        newPart.setBrandNew(true);
        if (campaign.getQuartermaster().buyPart(newPart, transitDays)) {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor()
                    + "'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                    + "'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public AmmoStorage getNewEquipment() {
        return getNewPart();
    }

    @Override
    public String failToFind() {
        return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                + "'><b> part not found</b>.</font>";
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font";

        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        toReturn += getAcquisitionExtraDesc() + "<br/>";
        PartInventory inventories = getCampaign().getPartInventory(getAcquisitionPart());
        toReturn += inventories.getTransitOrderedDetails() + "<br/>";
        toReturn += getActualValue().toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return getType().getDesc();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return getType().getShots() + " shots (1 ton)";
    }

    @Override
    public String getAcquisitionName() {
        return getType().getDesc();
    }

    @Override
    public String getAcquisitionBonus() {
        String bonus = getAllAcquisitionMods().getValueAsString();
        if (getAllAcquisitionMods().getValue() > -1) {
            bonus = "+" + bonus;
        }
        return "(" + bonus + ")";
    }

    @Override
    public AmmoStorage getAcquisitionPart() {
        return getNewPart();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if (isClanTechBase() && (campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0)) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        // availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
        return target;
    }

    public AmmoStorage getNewPart() {
        if (getType().getKgPerShot() > 0) {
            return new AmmoStorage(1, getType(), (int) Math.ceil(1000 / getType().getKgPerShot()), campaign);
        }
        return new AmmoStorage(1, getType(), getType().getShots(), campaign);
    }

    @Override
    public String getQuantityName(int quan) {
        int totalShots = quan * getShots();
        String report = "" + totalShots + " shots of " + getName();
        if (totalShots == 1) {
            report = "" + totalShots + " shot of " + getName();
        }
        return report;
    }

    @Override
    public String getArrivalReport() {
        int totalShots = quantity * getShots();
        String report = getQuantityName(quantity);
        if (totalShots == 1) {
            report += " has arrived";
        } else {
            report += " have arrived";
        }
        return report;
    }

    @Override
    public boolean needsMaintenance() {
        return true;
    }

    @Override
    public boolean isPriceAdjustedForAmount() {
        return true;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
