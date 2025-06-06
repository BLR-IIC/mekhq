/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import megamek.Version;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ILocationExposureStatus;
import megamek.common.LandAirMek;
import megamek.common.Mek;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class MekLocationTest {
    @Test
    void deserializationCtorTest() {
        MekLocation loc = new MekLocation();
        assertNotNull(loc);
    }

    @Test
    void ctorTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LLEG;
        int tonnage = 70;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        assertEquals(location, mekLocation.getLoc());
        assertEquals(tonnage, mekLocation.getUnitTonnage());
        assertEquals(hasTSM, mekLocation.isTsm());
        assertEquals(structureType, mekLocation.getStructureType());
        assertEquals(isClan, mekLocation.isClan());
        assertEquals(isQuad, mekLocation.forQuad());
        assertEquals(hasSensors, mekLocation.hasSensors());
        assertEquals(hasLifeSupport, mekLocation.hasLifeSupport());
        assertNotNull(mekLocation.getName());
    }

    @Test
    void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LLEG;
        int tonnage = 65;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        MekLocation clone = mekLocation.clone();

        assertEquals(mekLocation.getLoc(), clone.getLoc());
        assertEquals(mekLocation.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(mekLocation.isTsm(), clone.isTsm());
        assertEquals(mekLocation.getStructureType(), clone.getStructureType());
        assertEquals(mekLocation.isClan(), clone.isClan());
        assertEquals(mekLocation.forQuad(), clone.forQuad());
        assertEquals(mekLocation.hasSensors(), clone.hasSensors());
        assertEquals(mekLocation.hasLifeSupport(), clone.hasLifeSupport());
        assertEquals(mekLocation.getName(), clone.getName());
        assertEquals(mekLocation.getPercent(), clone.getPercent(), 0.001);
        assertEquals(mekLocation.isBlownOff(), clone.isBlownOff());
        assertEquals(mekLocation.isBreached(), clone.isBreached());
    }

    @Test
    void getMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LT;
        int tonnage = 65;
        int structureType = EquipmentType.T_STRUCTURE_REINFORCED;
        boolean isClan = true;
        boolean hasTSM = false;
        boolean isQuad = true;
        boolean hasSensors = false;
        boolean hasLifeSupport = false;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        MissingMekLocation missing = mekLocation.getMissingPart();

        assertEquals(mekLocation.getLoc(), missing.getLocation());
        assertEquals(mekLocation.getUnitTonnage(), missing.getUnitTonnage());
        assertEquals(mekLocation.isTsm(), missing.isTsm());
        assertEquals(mekLocation.getStructureType(), missing.getStructureType());
        assertEquals(mekLocation.isClan(), missing.isClan());
        assertEquals(mekLocation.forQuad(), missing.forQuad());
        assertEquals(mekLocation.getName(), missing.getName());
    }

    @Test
    void cannotScrapCT() {
        Campaign mockCampaign = mock(Campaign.class);

        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);

        assertNotNull(centerTorso.checkScrappable());
    }

    @Test
    void cannotSalvageCT() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(true);
        Mek entity = mock(Mek.class);
        when(entity.getWeight()).thenReturn(65.0);
        when(unit.getEntity()).thenReturn(entity);

        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 100, 0, false, false, false, false, false, mockCampaign);
        centerTorso.setUnit(unit);

        assertFalse(centerTorso.isSalvaging());

        MekLocation otherLocation = new MekLocation(Mek.LOC_HEAD, 100, 0, false, false, false, false, false,
                mockCampaign);
        otherLocation.setUnit(unit);

        assertTrue(otherLocation.isSalvaging());
    }

    @Test
    void onBadHipOrShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(entity.getWeight()).thenReturn(65.0);
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_RT;
        MekLocation torso = new MekLocation(location, 100, 0, false, false, false, false, false, mockCampaign);

        // Can't be on a bad hip or shoulder if off a unit
        assertFalse(torso.onBadHipOrShoulder());

        torso.setUnit(unit);

        // Not on a bad hip or shoulder if the unit doesn't say so
        assertFalse(torso.onBadHipOrShoulder());

        doReturn(true).when(unit).hasBadHipOrShoulder(location);

        // Now we're on a bad hip or shoulder
        assertTrue(torso.onBadHipOrShoulder());
    }

    @Test
    void isSamePartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LLEG, otherLocation = Mek.LOC_HEAD;
        int tonnage = 70;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL,
                otherStructureType = EquipmentType.T_STRUCTURE_REINFORCED;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        assertTrue(mekLocation.isSamePartType(mekLocation));

        // Same as our clone
        Part other = mekLocation.clone();
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Same if structure type is not Endo Steel and we're clan vs not clan
        mekLocation = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_INDUSTRIAL, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_INDUSTRIAL, !isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Clan and IS Endo Steel differ
        mekLocation = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_ENDO_STEEL, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_ENDO_STEEL, !isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Restore the original setup
        mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        // Different locations
        other = new MekLocation(otherLocation, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different tonnage
        other = new MekLocation(location, tonnage + 10, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different structure
        other = new MekLocation(location, tonnage, otherStructureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different TSM
        other = new MekLocation(location, tonnage, structureType, isClan,
                !hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Arms for quads must match on quad status, but others do not
        mekLocation = new MekLocation(Mek.LOC_RARM, tonnage, structureType, isClan,
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mek.LOC_LARM, tonnage, structureType, isClan,
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(Mek.LOC_LARM, tonnage, structureType, isClan,
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mek.LOC_LARM, tonnage, structureType, isClan,
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(Mek.LOC_LARM, tonnage, structureType, isClan,
                hasTSM, false, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mek.LOC_LARM, tonnage, structureType, isClan,
                hasTSM, false, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, !isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Restore the original setup
        mekLocation = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        // Different Sensors (off unit)
        other = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, !hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different Life Support (off unit)
        other = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, !hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Put the location on a unit
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn((double) tonnage);
        when(unit.getEntity()).thenReturn(entity);
        mekLocation.setUnit(unit);

        // Different Sensors (on unit)
        other = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, !hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Different Life Support (on unit)
        other = new MekLocation(location, tonnage, structureType, isClan,
                hasTSM, isQuad, hasSensors, !hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));
    }

    @Test
    void mekLocationWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        Campaign mockCampaign = mock(Campaign.class);
        MekLocation mekLocation = new MekLocation(Mek.LOC_CT, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setId(25);

        // Write the MekLocation XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        mekLocation.writeToXML(pw, 0);

        // Get the MekLocation XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the MekLocation
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(MekLocation.class, deserializedPart);

        MekLocation deserialized = (MekLocation) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(mekLocation.getId(), deserialized.getId());
        assertEquals(mekLocation.getName(), deserialized.getName());
        assertEquals(mekLocation.getLoc(), deserialized.getLoc());
        assertEquals(mekLocation.getUnitTonnage(), deserialized.getUnitTonnage());
        assertEquals(mekLocation.getStructureType(), deserialized.getStructureType());
        assertEquals(mekLocation.isClan(), deserialized.isClan());
        assertEquals(mekLocation.isTsm(), deserialized.isTsm());
        assertEquals(mekLocation.forQuad(), deserialized.forQuad());
        assertEquals(mekLocation.hasSensors(), deserialized.hasSensors());
        assertEquals(mekLocation.hasLifeSupport(), deserialized.hasLifeSupport());
    }

    @Test
    void updateConditionFromEntityTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        doReturn(1).when(entity).getInternalForReal(anyInt());
        doReturn(1).when(entity).getOInternal(anyInt());
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);

        assertFalse(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // No unit is a no-op
        mekLocation.updateConditionFromEntity(false);
        assertFalse(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertFalse(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Add the location to a unit
        mekLocation.setUnit(unit);

        // Blow everything off but our location
        doReturn(true).when(entity).isLocationBlownOff(anyInt());
        doReturn(false).when(entity).isLocationBlownOff(location);

        mekLocation.updateConditionFromEntity(false);
        assertFalse(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertFalse(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Blow off our location
        doReturn(true).when(entity).isLocationBlownOff(location);

        mekLocation.updateConditionFromEntity(false);
        assertTrue(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertTrue(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Breach everything but our location
        doReturn(true).when(unit).isLocationBreached(anyInt());
        doReturn(false).when(unit).isLocationBreached(location);

        mekLocation.updateConditionFromEntity(false);
        assertTrue(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertTrue(mekLocation.isBlownOff());
        assertFalse(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Breach our location
        doReturn(true).when(unit).isLocationBreached(location);

        mekLocation.updateConditionFromEntity(false);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Destroy every location but ours
        doReturn(0).when(entity).getInternalForReal(anyInt());
        doReturn(1).when(entity).getInternalForReal(location);

        mekLocation.updateConditionFromEntity(false);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // Damage our location
        doReturn(1).when(entity).getInternalForReal(location);
        doReturn(2).when(entity).getOInternal(location);

        mekLocation.updateConditionFromEntity(false);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1 / 2.0, mekLocation.getPercent(), 0.001);

        mekLocation.updateConditionFromEntity(true);
        assertTrue(mekLocation.isBlownOff());
        assertTrue(mekLocation.isBreached());
        assertEquals(1 / 2.0, mekLocation.getPercent(), 0.001);
    }

    @Test
    void updateConditionFromPartUpdatesEntityArmorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        int totalArmor = 100;
        doReturn(totalArmor).when(entity).getOInternal(anyInt());
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);

        // not on unit
        mekLocation.updateConditionFromPart();

        // assign to unit
        mekLocation.setUnit(unit);

        // 100% armor
        mekLocation.updateConditionFromPart();

        verify(entity, times(1)).getOInternal(location);
        verify(entity, times(1)).setInternal(totalArmor, location);

        // 50% armor
        mekLocation.setPercent(0.5);
        mekLocation.updateConditionFromPart();
        verify(entity, times(1)).setInternal(totalArmor / 2, location);

        // 1% armor
        mekLocation.setPercent(0.01);
        mekLocation.updateConditionFromPart();
        verify(entity, times(1)).setInternal(totalArmor / 100, location);
    }

    @Test
    void updateConditionFromPartRestoresNotHittableCriticalSlotsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        int totalArmor = 100;
        doReturn(totalArmor).when(entity).getOInternal(anyInt());
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);

        doReturn(3).when(entity).getNumberOfCriticals(location);
        CriticalSlot hittable = mock(CriticalSlot.class);
        when(hittable.isEverHittable()).thenReturn(true);
        doReturn(hittable).when(entity).getCritical(location, 0);
        CriticalSlot notHittable = mock(CriticalSlot.class);
        doReturn(notHittable).when(entity).getCritical(location, 1);
        Mounted mount = mock(Mounted.class);
        when(notHittable.getMount()).thenReturn(mount);
        doReturn(null).when(entity).getCritical(location, 2);

        mekLocation.updateConditionFromPart();

        verify(notHittable, times(1)).setDestroyed(false);
        verify(notHittable, times(1)).setHit(false);
        verify(notHittable, times(1)).setRepairable(true);
        verify(notHittable, times(1)).setMissing(false);
        verify(mount, times(1)).setHit(false);
        verify(mount, times(1)).setDestroyed(false);
        verify(mount, times(1)).setMissing(false);
        verify(mount, times(1)).setRepairable(true);
    }

    @Test
    void needsFixingTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_RT;
        MekLocation torso = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);

        // Not on a unit
        assertFalse(torso.needsFixing());

        // On a unit which is fine
        torso.setUnit(unit);
        assertFalse(torso.needsFixing());

        // Bad hip or shoulder
        doReturn(true).when(unit).hasBadHipOrShoulder(location);
        assertTrue(torso.needsFixing());

        // restore the hip/shoulder
        doReturn(false).when(unit).hasBadHipOrShoulder(location);
        assertFalse(torso.needsFixing());

        // Less than 100% armor
        torso.setPercent(0.99);
        assertTrue(torso.needsFixing());

        // restore the armor
        torso.setPercent(1.0);
        assertFalse(torso.needsFixing());

        // Breached
        torso.setBreached(true);
        assertTrue(torso.needsFixing());

        // Not breached
        torso.setBreached(false);
        assertFalse(torso.needsFixing());

        // Blown off
        torso.setBlownOff(true);
        assertTrue(torso.needsFixing());

        // Not blown off
        torso.setBlownOff(false);
        assertFalse(torso.needsFixing());
    }

    @Test
    void checkFixableNoUnitTest() {
        Campaign mockCampaign = mock(Campaign.class);
        MekLocation torso = new MekLocation(Mek.LOC_RT, 30, 0, false, false, false, false, false, mockCampaign);
        assertNull(torso.checkFixable());
    }

    @Test
    void checkFixableBlownOffTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        when(unit.getEntity()).thenReturn(entity);

        // Everything but the CT is busted
        doReturn(true).when(unit).isLocationDestroyed(anyInt());
        doReturn(false).when(unit).isLocationDestroyed(Mek.LOC_CT);

        // Destroyed leg can be repaired even if everything else is gone
        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNull(mekLocation.checkFixable());
        location = Mek.LOC_RLEG;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNull(mekLocation.checkFixable());

        // Destroyed head can be repaired even if everything else is gone
        location = Mek.LOC_HEAD;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNull(mekLocation.checkFixable());

        // Destroyed torsos can be repaired
        location = Mek.LOC_RT;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNull(mekLocation.checkFixable());
        location = Mek.LOC_LT;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNull(mekLocation.checkFixable());

        // Arms cannot without their respective torsos
        location = Mek.LOC_RARM;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNotNull(mekLocation.checkFixable());

        // Fix the RT ...
        doReturn(false).when(unit).isLocationDestroyed(Mek.LOC_RT);

        // ... now the RARM can be fixed.
        assertNull(mekLocation.checkFixable());

        location = Mek.LOC_LARM;
        mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertNotNull(mekLocation.checkFixable());

        // Fix the LT ...
        doReturn(false).when(unit).isLocationDestroyed(Mek.LOC_LT);

        // ... now the LARM can be fixed.
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkFixableBustedHipOrShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mek.LOC_RARM;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        doReturn(true).when(unit).hasBadHipOrShoulder(anyInt());
        doReturn(false).when(unit).hasBadHipOrShoulder(location);

        // Shoulder is fine
        assertNull(mekLocation.checkFixable());

        // Shoulder is not fine
        doReturn(true).when(unit).hasBadHipOrShoulder(location);
        assertNotNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableNotSalvagingTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        MekLocation mekLocation = new MekLocation(Mek.LOC_RARM, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        assertNull(mekLocation.checkSalvageable());
    }

    @Test
    void checkSalvageableBadHipShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_RARM;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Must scrap a limb with a bad hip or shoulder
        doReturn(true).when(unit).hasBadHipOrShoulder(location);
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        doReturn(false).when(unit).hasBadHipOrShoulder(location);
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableTorsoWithArmsIntactTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_RT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad(Mek.LOC_RARM);
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        doReturn(true).when(entity).isLocationBad(Mek.LOC_RARM);
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());

        location = Mek.LOC_LT;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad(Mek.LOC_LARM);
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        doReturn(true).when(entity).isLocationBad(Mek.LOC_LARM);
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableTorsoWithArmsIntactQuadTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_RT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, /* forQuad: */true, false, false,
                mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());

        location = Mek.LOC_LT;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableArmorStillPresentTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // No armor
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());

        // Some armor, for real.
        doReturn(1).when(entity).getArmorForReal(eq(location), anyBoolean());
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        // Some rear armor
        doReturn(0).when(entity).getArmorForReal((location), (false));
        doReturn(true).when(entity).hasRearArmor((location));
        doReturn(1).when(entity).getArmorForReal((location), (true));
        assertNotNull(mekLocation.checkSalvageable());
        assertNotNull(mekLocation.checkFixable());

        // No rear armor
        doReturn(0).when(entity).getArmorForReal((location), (false));
        doReturn(true).when(entity).hasRearArmor((location));
        doReturn(0).when(entity).getArmorForReal((location), (true));
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableOnlyIgnorableSystemsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        int[] systems = new int[] { Mek.ACTUATOR_HIP, Mek.ACTUATOR_SHOULDER,
                Mek.SYSTEM_LIFE_SUPPORT, Mek.SYSTEM_SENSORS
        };
        doReturn(systems.length + 1).when(entity).getNumberOfCriticals((location));
        CriticalSlot notHittable = mock(CriticalSlot.class);
        doReturn(notHittable).when(entity).getCritical((location), (0));

        for (int ii = 0; ii < systems.length; ++ii) {
            CriticalSlot mockIgnoredSystem = mock(CriticalSlot.class);
            when(mockIgnoredSystem.isEverHittable()).thenReturn(true);
            when(mockIgnoredSystem.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
            when(mockIgnoredSystem.getIndex()).thenReturn(systems[ii]);
            doReturn(mockIgnoredSystem).when(entity).getCritical((location), (ii + 1));
        }

        // No hittable or repairable systems
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());
    }

    @Test
    void checkSalvageableRepairableSystemsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        doReturn(1).when(entity).getNumberOfCriticals((location));
        CriticalSlot repairable = mock(CriticalSlot.class);
        when(repairable.isEverHittable()).thenReturn(true);
        when(repairable.getType()).thenReturn(CriticalSlot.TYPE_EQUIPMENT);
        doReturn(repairable).when(entity).getCritical((location), (0));

        // No repairable systems
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());

        when(repairable.isRepairable()).thenReturn(true);

        // A repairable system remains
        String message = mekLocation.checkSalvageable();
        assertNotNull(message);
        assertTrue(message.contains("Repairable Part"));

        message = mekLocation.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains("Repairable Part"));
    }

    @Test
    void checkSalvageableRepairableNamedSystemsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        doReturn(1).when(entity).getNumberOfCriticals((location));
        CriticalSlot repairable = mock(CriticalSlot.class);
        when(repairable.isEverHittable()).thenReturn(true);
        when(repairable.getType()).thenReturn(CriticalSlot.TYPE_EQUIPMENT);
        doReturn(repairable).when(entity).getCritical((location), (0));
        Mounted mounted = mock(Mounted.class);
        when(repairable.getMount()).thenReturn(mounted);
        when(mounted.getType()).thenReturn(mock(EquipmentType.class));
        doReturn(1).when(entity).getEquipmentNum(mounted);
        String partName = "Test Part";
        EquipmentPart part = mock(EquipmentPart.class);
        when(part.getName()).thenReturn(partName);
        when(part.getEquipmentNum()).thenReturn(1);
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            return predicate.test(part) ? part : null;
        }).when(unit).findPart(any());

        // No repairable systems
        assertNull(mekLocation.checkSalvageable());
        assertNull(mekLocation.checkFixable());

        when(repairable.isRepairable()).thenReturn(true);

        // A repairable system remains
        String message = mekLocation.checkSalvageable();
        assertNotNull(message);
        assertTrue(message.contains(partName));

        message = mekLocation.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains(partName));
    }

    @Test
    void checkScrappableCannotScrapCenterTorsoTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        MekLocation mekLocation = new MekLocation(Mek.LOC_CT, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        assertNotNull(mekLocation.checkScrappable());
    }

    @Test
    void checkScrappableTorsoWithArmsIntactTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_RT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNotNull(mekLocation.checkScrappable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNull(mekLocation.checkScrappable());

        location = Mek.LOC_LT;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNotNull(mekLocation.checkScrappable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNull(mekLocation.checkScrappable());
    }

    @Test
    void checkScrappableTorsoWithArmsIntactQuadTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_RT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, /* forQuad: */true, false, false,
                mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNotNull(mekLocation.checkScrappable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_RARM));
        assertNull(mekLocation.checkScrappable());

        location = Mek.LOC_LT;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Cannot salvage a torso if the attached arm is Okay
        doReturn(false).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNotNull(mekLocation.checkScrappable());

        doReturn(true).when(entity).isLocationBad((Mek.LOC_LARM));
        assertNull(mekLocation.checkScrappable());
    }

    @Test
    void checkScrappableArmorStillPresentTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // No armor
        assertNull(mekLocation.checkScrappable());

        // Some armor, for real.
        doReturn(1).when(entity).getArmorForReal(eq(location), anyBoolean());
        assertNotNull(mekLocation.checkScrappable());

        // Some rear armor
        doReturn(0).when(entity).getArmorForReal((location), (false));
        doReturn(true).when(entity).hasRearArmor((location));
        doReturn(1).when(entity).getArmorForReal((location), (true));
        assertNotNull(mekLocation.checkScrappable());

        // No rear armor
        doReturn(0).when(entity).getArmorForReal((location), (false));
        doReturn(true).when(entity).hasRearArmor((location));
        doReturn(0).when(entity).getArmorForReal((location), (true));
        assertNull(mekLocation.checkScrappable());
    }

    @Test
    void checkScrappableOnlyIgnorableSystemsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        int[] systems = new int[] { Mek.SYSTEM_COCKPIT, Mek.ACTUATOR_HIP, Mek.ACTUATOR_SHOULDER };
        doReturn(systems.length + 1).when(entity).getNumberOfCriticals((location));
        CriticalSlot notHittable = mock(CriticalSlot.class);
        doReturn(notHittable).when(entity).getCritical((location), (0));

        for (int ii = 0; ii < systems.length; ++ii) {
            CriticalSlot mockIgnoredSystem = mock(CriticalSlot.class);
            when(mockIgnoredSystem.isEverHittable()).thenReturn(true);
            when(mockIgnoredSystem.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
            when(mockIgnoredSystem.getIndex()).thenReturn(systems[ii]);
            doReturn(mockIgnoredSystem).when(entity).getCritical(location, ii + 1);
        }

        // No hittable or repairable systems
        assertNull(mekLocation.checkScrappable());
    }

    @Test
    void checkScrappableRepairableSystemsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());
        when(unit.isSalvage()).thenReturn(true);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        doReturn(1).when(entity).getNumberOfCriticals((location));
        CriticalSlot repairable = mock(CriticalSlot.class);
        when(repairable.isEverHittable()).thenReturn(true);
        when(repairable.getType()).thenReturn(CriticalSlot.TYPE_EQUIPMENT);
        doReturn(repairable).when(entity).getCritical((location), (0));

        // No repairable systems
        assertNull(mekLocation.checkScrappable());

        when(repairable.isRepairable()).thenReturn(true);

        // A repairable systems remains
        assertNotNull(mekLocation.checkScrappable());
    }

    @Test
    void lamTorsoRemovableOnlyWithMissingAvionicsAndLandingGear() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMek entity = mock(LandAirMek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mek.LOC_RT;
        MekLocation torso = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        torso.setUnit(unit);

        // Mark that we're salvaging the part
        when(unit.isSalvage()).thenReturn(true);

        // Blow off the right arm
        doReturn(true).when(entity).isLocationBad(Mek.LOC_RARM);

        // 2 criticals
        doReturn(2).when(entity).getNumberOfCriticals((location));
        CriticalSlot mockLandingGear = mock(CriticalSlot.class);
        when(mockLandingGear.isEverHittable()).thenReturn(true);
        when(mockLandingGear.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockLandingGear.getIndex()).thenReturn(LandAirMek.LAM_LANDING_GEAR);
        doReturn(mockLandingGear).when(entity).getCritical((location), (0));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMek.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical((location), (1));

        // No missing parts
        doAnswer(inv -> null).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvageable());
        assertNotNull(torso.checkScrappable());

        // Only missing landing gear, avionics are still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            return predicate.test(missingLandingGear) ? missingLandingGear : null;
        }).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvageable());
        assertNotNull(torso.checkScrappable());

        // Only missing avionics, landing gear is still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvageable());
        assertNotNull(torso.checkScrappable());

        // Missing both Landing Gear and Avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN remove this torso
        assertNull(torso.checkFixable());
        assertNull(torso.checkSalvageable());
        assertNull(torso.checkScrappable());
    }

    @Test
    void lamHeadRemovableOnlyWithMissingAvionics() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMek entity = mock(LandAirMek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mek.LOC_HEAD;
        MekLocation head = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        head.setUnit(unit);

        // Mark that we're salvaging the part
        when(unit.isSalvage()).thenReturn(true);

        // 1 critical
        doReturn(1).when(entity).getNumberOfCriticals((location));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMek.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical((location), (0));

        // No missing parts
        doAnswer(inv -> null).when(unit).findPart(any());

        // We cannot remove this head
        assertNotNull(head.checkFixable());
        assertNotNull(head.checkSalvageable());
        assertNotNull(head.checkScrappable());

        // Missing avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN remove this head
        assertNull(head.checkFixable());
        assertNull(head.checkSalvageable());
        assertNull(head.checkScrappable());
    }

    @Test
    void doMaintenanceDamageTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);

        // not on unit
        mekLocation.doMaintenanceDamage(100);

        // No change.
        assertEquals(1.0, mekLocation.getPercent(), 0.001);

        // On unit
        mekLocation.setUnit(unit);

        // Setup getInternalForReal to return the correct calculation
        doAnswer(inv -> {
            int armor = inv.getArgument(0);
            doReturn(armor).when(entity).getInternalForReal((location));
            return null;
        }).when(entity).setInternal(anyInt(), eq(location));
        int startingArmor = 10;
        doReturn(startingArmor).when(entity).getOInternal((location));

        // No damage
        mekLocation.doMaintenanceDamage(0);
        verify(entity, times(0)).setInternal(anyInt(), eq(location));

        // Some damage
        doReturn(startingArmor).when(entity).getInternal((location));
        int damage = 3;

        mekLocation.doMaintenanceDamage(damage);

        verify(entity, times(1)).setInternal((startingArmor - damage), (location));

        // More than enough damage (but will never destroy the location)
        damage = startingArmor;

        mekLocation.doMaintenanceDamage(damage);

        verify(entity, times(1)).setInternal((1), (location));
    }

    @Test
    void removeRestoresBlownOffTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);

        // Removal
        mekLocation.setBlownOff(true);
        mekLocation.remove(false);
        assertFalse(mekLocation.isBlownOff());

        // Salvage
        mekLocation.setBlownOff(true);
        mekLocation.remove(true);
        assertFalse(mekLocation.isBlownOff());
    }

    @Test
    void removeRestoresBreachedTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);

        // Removal
        mekLocation.setBreached(true);
        mekLocation.remove(false);
        assertFalse(mekLocation.isBreached());

        // Salvage
        mekLocation.setBreached(true);
        mekLocation.remove(true);
        assertFalse(mekLocation.isBreached());
    }

    @Test
    void removeSimpleTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        mekLocation.remove(false);

        assertFalse(mekLocation.getId() > 0);
        assertNull(mekLocation.getUnit());
        assertFalse(warehouse.getParts().contains(mekLocation));

        // Only one part!
        MissingMekLocation missingPart = null;
        for (Part part : warehouse.getParts()) {
            assertInstanceOf(MissingMekLocation.class, part);
            assertNull(missingPart);
            missingPart = (MissingMekLocation) part;
        }

        assertNotNull(missingPart);
        assertEquals(location, missingPart.getLocation());
    }

    @Test
    void removeHeadWithoutComponentsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_HEAD;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        mekLocation.remove(false);

        // No head components, so they don't get these
        assertFalse(mekLocation.hasSensors());
        assertFalse(mekLocation.hasLifeSupport());
    }

    @Test
    void removeHeadWithSensorComponentTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_HEAD;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        // Unit has sensors in the head
        MekSensor sensors = mock(MekSensor.class);
        when(unit.getParts()).thenReturn(Collections.singletonList(sensors));

        mekLocation.remove(false);

        // Has sensors but no life support
        assertTrue(mekLocation.hasSensors());
        assertFalse(mekLocation.hasLifeSupport());

        verify(sensors, times(1)).remove(false);
    }

    @Test
    void removeHeadWithLifeSupportComponentTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_HEAD;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        // Unit has life support in the head
        MekLifeSupport lifeSupport = mock(MekLifeSupport.class);
        when(unit.getParts()).thenReturn(Collections.singletonList(lifeSupport));

        mekLocation.remove(false);

        // Has life support but no sensors
        assertFalse(mekLocation.hasSensors());
        assertTrue(mekLocation.hasLifeSupport());

        verify(lifeSupport, times(1)).remove((false));
    }

    @Test
    void removeHeadWithComponentsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_HEAD;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        // Unit has components in the head
        MekSensor sensors = mock(MekSensor.class);
        MekLifeSupport lifeSupport = mock(MekLifeSupport.class);
        when(unit.getParts()).thenReturn(Arrays.asList(sensors, lifeSupport));

        mekLocation.remove(false);

        // Has both sensors and life support
        assertTrue(mekLocation.hasSensors());
        assertTrue(mekLocation.hasLifeSupport());

        verify(sensors, times(1)).remove((false));
        verify(lifeSupport, times(1)).remove((false));
    }

    @Test
    void removeCenterTorsoDoesntAddMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_CT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        mekLocation.remove(false);

        assertFalse(mekLocation.getId() > 0);
        assertNull(mekLocation.getUnit());
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    void updateConditionFromEntityNoInternalsRemovesLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster mockQuartermaster = mock(Quartermaster.class);
        when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(100.0);
        when(unit.getEntity()).thenReturn(entity);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL,
                true, true, true, true, true, mockCampaign);

        // Add the location to a unit
        mekLocation.setUnit(unit);

        // Destroy the limb
        doReturn(0).when(entity).getInternalForReal(anyInt());
        doReturn(1).when(entity).getOInternal(anyInt());

        mekLocation.updateConditionFromEntity(false);

        // We should have removed the limb
        verify(mockWarehouse, times(1)).removePart((mekLocation));

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockQuartermaster, times(1)).addPart(partCaptor.capture(), eq(0));

        Part part = partCaptor.getValue();
        assertInstanceOf(MissingMekLocation.class, part);
        assertEquals(location, part.getLocation());
    }

    @Test
    void salvageSimpleTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        mekLocation.remove(true);

        assertTrue(mekLocation.getId() > 0);
        assertNull(mekLocation.getUnit());
        assertTrue(mekLocation.isSpare());
        assertTrue(warehouse.getParts().contains(mekLocation));

        // Two parts
        MissingMekLocation missingPart = null;
        for (Part part : warehouse.getParts()) {
            if (part instanceof MissingMekLocation) {
                assertNull(missingPart);
                missingPart = (MissingMekLocation) part;
            } else {
                assertEquals(mekLocation, part);
            }
        }

        assertNotNull(missingPart);
        assertEquals(location, missingPart.getLocation());
    }

    @Test
    void salvageCenterTorsoDoesntAddMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_CT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setId(25);
        mekLocation.setUnit(unit);

        warehouse.addPart(mekLocation);

        mekLocation.remove(true);

        assertTrue(mekLocation.getId() > 0);
        assertNull(mekLocation.getUnit());
        assertTrue(mekLocation.isSpare());
        assertTrue(warehouse.getParts().contains(mekLocation));

        // No missing parts in the warehouse if a CT
        for (Part part : warehouse.getParts()) {
            assertFalse(part instanceof MissingMekLocation);
        }
    }

    @Test
    void fixSimpleTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mek.LOC_CT;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setPercent(0.5);

        // Fix the part in the warehouse
        mekLocation.fix();

        assertEquals(1.0, mekLocation.getPercent(), 0.001);
        assertFalse(mekLocation.needsFixing());

        // Place the location on a unit
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        int originalInternal = 10;
        doReturn(originalInternal).when(entity).getOInternal((location));
        when(entity.getWeight()).thenReturn(30.0);

        mekLocation.setUnit(unit);
        mekLocation.setPercent(0.01);

        assertTrue(mekLocation.needsFixing());

        // Fix the location on the unit
        mekLocation.fix();

        assertEquals(1.0, mekLocation.getPercent(), 0.001);
        assertFalse(mekLocation.needsFixing());

        verify(entity, times(1)).setInternal((originalInternal), (location));
    }

    @Test
    void fixBlownOffTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setBlownOff(true);
        mekLocation.setUnit(unit);

        // Setup the critical slots
        doReturn(3).when(entity).getNumberOfCriticals((location));
        doReturn(null).when(entity).getCritical((location), (0)); // empty
        CriticalSlot mockSlot = mock(CriticalSlot.class);
        Mounted mounted = mock(Mounted.class);
        when(mockSlot.getMount()).thenReturn(mounted);
        doReturn(mockSlot).when(entity).getCritical((location), (1));
        CriticalSlot mockSlotNoMount = mock(CriticalSlot.class);
        doReturn(mockSlotNoMount).when(entity).getCritical((location), (2)); // no mount

        assertTrue(mekLocation.needsFixing());
        assertTrue(mekLocation.isBlownOff());

        // Reattach the blown off location on the unit
        mekLocation.fix();

        assertFalse(mekLocation.needsFixing());
        assertFalse(mekLocation.isBlownOff());

        verify(entity, times(1)).setLocationBlownOff((location), (false));
        verify(mockSlot, times(1)).setMissing((false));
        verify(mounted, times(1)).setMissing((false));
    }

    @Test
    void fixBreachedTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setBreached(true);
        mekLocation.setUnit(unit);

        // Setup the critical slots
        doReturn(3).when(entity).getNumberOfCriticals((location));
        doReturn(null).when(entity).getCritical((location), (0)); // empty
        CriticalSlot mockSlot = mock(CriticalSlot.class);
        Mounted mounted = mock(Mounted.class);
        when(mockSlot.getMount()).thenReturn(mounted);
        doReturn(mockSlot).when(entity).getCritical((location), (1));
        CriticalSlot mockSlotNoMount = mock(CriticalSlot.class);
        doReturn(mockSlotNoMount).when(entity).getCritical((location), (2)); // no mount

        assertTrue(mekLocation.needsFixing());
        assertTrue(mekLocation.isBreached());

        // Reattach the blown off location on the unit
        mekLocation.fix();

        assertFalse(mekLocation.needsFixing());
        assertFalse(mekLocation.isBreached());

        verify(entity, times(1)).setLocationStatus((location), (ILocationExposureStatus.NORMAL), (true));
        verify(mockSlot, times(1)).setBreached((false));
        verify(mounted, times(1)).setBreached((false));
    }

    @Test
    void getDifficultyTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        // Blown off non-head
        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertEquals(+1, mekLocation.getDifficulty());

        // Blown off head
        location = Mek.LOC_HEAD;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertEquals(+2, mekLocation.getDifficulty());

        // Salvaging the blown off location
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setBlownOff(true);
        when(unit.isSalvage()).thenReturn(true);
        mekLocation.setUnit(unit);
        assertEquals(0, mekLocation.getDifficulty());

        when(unit.isSalvage()).thenReturn(false);

        // Breached location
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBreached(true);
        assertEquals(0, mekLocation.getDifficulty());

        // Otherwise we're by percent for both repair and salvage
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setPercent(0.01);
        assertEquals(+2, mekLocation.getDifficulty());
        mekLocation.setPercent(0.25);
        assertEquals(+1, mekLocation.getDifficulty());
        mekLocation.setPercent(0.5);
        assertEquals(0, mekLocation.getDifficulty());
        mekLocation.setPercent(0.75);
        assertEquals(-1, mekLocation.getDifficulty());

        // Assign to a salvaging unit
        when(unit.isSalvage()).thenReturn(true);
        mekLocation.setUnit(unit);
        mekLocation.setPercent(0.01);
        assertEquals(+2, mekLocation.getDifficulty());
        mekLocation.setPercent(0.25);
        assertEquals(+1, mekLocation.getDifficulty());
        mekLocation.setPercent(0.5);
        assertEquals(0, mekLocation.getDifficulty());
        mekLocation.setPercent(0.75);
        assertEquals(-1, mekLocation.getDifficulty());
    }

    @Test
    void getBaseTimeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        // Blown off non-head
        int location = Mek.LOC_LLEG;
        MekLocation mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertEquals(180, mekLocation.getBaseTime());

        // Blown off head
        location = Mek.LOC_HEAD;
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBlownOff(true);
        assertEquals(200, mekLocation.getBaseTime());

        // Salvaging the blown off location
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setBlownOff(true);
        when(unit.isSalvage()).thenReturn(true);
        mekLocation.setUnit(unit);
        assertEquals(0, mekLocation.getBaseTime());

        when(unit.isSalvage()).thenReturn(false);

        // Breached location
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setBreached(true);
        assertEquals(60, mekLocation.getBaseTime());

        // Otherwise we're by percent for both repair and salvage
        mekLocation = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setPercent(0.01);
        assertEquals(270, mekLocation.getBaseTime());
        mekLocation.setPercent(0.25);
        assertEquals(180, mekLocation.getBaseTime());
        mekLocation.setPercent(0.5);
        assertEquals(135, mekLocation.getBaseTime());
        mekLocation.setPercent(0.75);
        assertEquals(90, mekLocation.getBaseTime());

        // Assign to a salvaging unit
        when(unit.isSalvage()).thenReturn(true);
        mekLocation.setUnit(unit);
        mekLocation.setPercent(0.01);
        assertEquals(270, mekLocation.getBaseTime());
        mekLocation.setPercent(0.25);
        assertEquals(180, mekLocation.getBaseTime());
        mekLocation.setPercent(0.5);
        assertEquals(135, mekLocation.getBaseTime());
        mekLocation.setPercent(0.75);
        assertEquals(90, mekLocation.getBaseTime());
    }

    @Test
    void isRightTechTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);

        assertTrue(centerTorso.isRightTechType(SkillType.S_TECH_MEK));
        assertFalse(centerTorso.isRightTechType(SkillType.S_TECH_MECHANIC));
    }

    @Test
    void getTechAdvancementTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 25, structureType, true, false, false, false, false,
                mockCampaign);
        assertNotNull(centerTorso.getTechAdvancement());

        centerTorso = new MekLocation(Mek.LOC_CT, 25, structureType, false, false, false, false, false, mockCampaign);
        assertNotNull(centerTorso.getTechAdvancement());
    }

    @Test
    void getMRMSOptionTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);
        assertEquals(PartRepairType.GENERAL_LOCATION, centerTorso.getMRMSOptionType());
    }

    @Test
    void getRepairPartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        MekLocation centerTorso = new MekLocation(Mek.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);
        assertEquals(PartRepairType.MEK_LOCATION, centerTorso.getRepairPartType());
    }

    @Test
    void getDetailsSpareTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        MekLocation mekLocation = new MekLocation(Mek.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);

        assertNotNull(mekLocation.getDetails());
        assertTrue(mekLocation.getDetails().startsWith("25 tons"));
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("25 tons", mekLocation.getDetails(false));

        mekLocation.setPercent(0.01);

        assertNotNull(mekLocation.getDetails());
        assertTrue(mekLocation.getDetails().contains("(1%)"));
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("25 tons", mekLocation.getDetails(false));

        mekLocation = new MekLocation(Mek.LOC_HEAD, 25, 0, false, false, false, false, false, mockCampaign);

        assertNotNull(mekLocation.getDetails());
        assertTrue(mekLocation.getDetails().startsWith("25 tons"));
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("25 tons", mekLocation.getDetails(false));

        mekLocation.setSensors(true);

        assertNotNull(mekLocation.getDetails());
        assertTrue(mekLocation.getDetails().startsWith("25 tons"));
        assertTrue(mekLocation.getDetails().contains("[Sensors]"));
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("25 tons [Sensors]", mekLocation.getDetails(false));

        mekLocation.setLifeSupport(true);

        assertNotNull(mekLocation.getDetails());
        assertTrue(mekLocation.getDetails().startsWith("25 tons"));
        assertTrue(mekLocation.getDetails().contains("[Sensors, Life Support]"));
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("25 tons [Sensors, Life Support]", mekLocation.getDetails(false));
    }

    @Test
    void getDetailsOnUnitTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_RARM;
        doReturn("Right Arm").when(entity).getLocationName((location));

        MekLocation mekLocation = new MekLocation(location, 25, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        assertNotNull(mekLocation.getDetails());
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails());
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails(false));

        mekLocation.setPercent(0.1);

        assertNotNull(mekLocation.getDetails());
        assertEquals("Right Arm, 30 tons (10%)", mekLocation.getDetails());
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails(false));

        mekLocation.setBlownOff(true);

        assertNotNull(mekLocation.getDetails());
        assertEquals("Right Arm, 30 tons (Blown Off)", mekLocation.getDetails());
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails(false));

        mekLocation.setBlownOff(false);
        mekLocation.setBreached(true);

        assertNotNull(mekLocation.getDetails());
        assertEquals("Right Arm, 30 tons (Breached)", mekLocation.getDetails());
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails(false));

        mekLocation.setBreached(false);
        doReturn(true).when(unit).hasBadHipOrShoulder((mekLocation.getLoc()));

        assertNotNull(mekLocation.getDetails());
        assertEquals("Right Arm, 30 tons (Bad Hip/Shoulder)", mekLocation.getDetails());
        assertNotNull(mekLocation.getDetails(false));
        assertEquals("Right Arm, 30 tons", mekLocation.getDetails(false));
    }

    @Test
    void getAllModsBreachedTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_RARM;
        doReturn("Right Arm").when(entity).getLocationName((location));

        MekLocation mekLocation = new MekLocation(location, 25, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Breached but not salvaging
        mekLocation.setBreached(true);

        TargetRoll roll = mekLocation.getAllMods(mock(Person.class));
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
    }

    @Test
    void getAllModsBlownOffTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isSalvage()).thenReturn(true);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_RARM;
        doReturn("Right Arm").when(entity).getLocationName((location));

        MekLocation mekLocation = new MekLocation(location, 25, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);

        // Blown Off and salvaging
        mekLocation.setBlownOff(true);

        TargetRoll roll = mekLocation.getAllMods(mock(Person.class));
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
    }

    @Test
    void getAllModsSimpleTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isSalvage()).thenReturn(true);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_RARM;
        doReturn("Right Arm").when(entity).getLocationName((location));

        MekLocation mekLocation = new MekLocation(location, 25, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setPercent(0.5);

        TargetRoll siteMod = new TargetRoll(1, "site mod");
        when(unit.getSiteMod()).thenReturn(siteMod);

        Person tech = mock(Person.class);
        PersonnelOptions mockOptions = mock(PersonnelOptions.class);
        when(tech.getOptions()).thenReturn(mockOptions);
        TargetRoll roll = mekLocation.getAllMods(tech);

        assertNotNull(roll);
        // 1 (difficulty) + 1 (site mod) + 0 (D)
        assertTrue(roll.getValue() >= siteMod.getValue());
    }

    @Test
    void getDescSimpleTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Unit unit = mock(Unit.class);
        Mek entity = mock(Mek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);

        int location = Mek.LOC_RARM;
        doReturn("Right Arm").when(entity).getLocationName((location));

        MekLocation mekLocation = new MekLocation(location, 25, 0, false, false, false, false, false, mockCampaign);
        mekLocation.setUnit(unit);
        mekLocation.setPercent(0.75);

        // default
        assertNotNull(mekLocation.getDesc());
        assertTrue(mekLocation.getDesc().contains("Repair"));
        assertTrue(mekLocation.getDesc().contains("90 minutes"));

        // default, salvage
        when(unit.isSalvage()).thenReturn(true);
        assertNotNull(mekLocation.getDesc());
        assertTrue(mekLocation.getDesc().contains("Salvage"));

        when(unit.isSalvage()).thenReturn(false);

        mekLocation.setBlownOff(true);
        assertNotNull(mekLocation.getDesc());
        assertTrue(mekLocation.getDesc().contains("Re-attach "));
        assertTrue(mekLocation.getDesc().contains("180 minutes"));

        mekLocation.setBlownOff(false);
        mekLocation.setBreached(true);
        assertNotNull(mekLocation.getDesc());
        assertTrue(mekLocation.getDesc().contains("Seal "));
        assertTrue(mekLocation.getDesc().contains("60 minutes"));

        mekLocation.setTech(mock(Person.class));
        assertNotNull(mekLocation.getDesc());
        assertTrue(mekLocation.getDesc().contains("(scheduled)"));

        mekLocation.setBlownOff(true);
        mekLocation.setBreached(false);
        mekLocation.setMode(WorkTime.EXTRA_2);
        assertTrue(mekLocation.getDesc().contains(mekLocation.getCurrentModeName()));

        // Breached, but too hard to handle
        mekLocation.setSkillMin(SkillType.EXP_ELITE + 1);
        assertTrue(mekLocation.getDesc().contains("Impossible"));
    }
}
