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

package mekhq.campaign.autoresolve;

import static megamek.common.force.Force.NO_FORCE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.sentry.Sentry;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.ProtoMek;
import megamek.common.UnitType;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.converter.EntityAsUnit;
import megamek.common.autoresolve.converter.ForceConsolidation;
import megamek.common.autoresolve.converter.SetupForces;
import megamek.common.force.Forces;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

/**
 * @author Luana Coppio
 */
public class AtBSetupForces extends SetupForces {
    private static final MMLogger logger = MMLogger.create(AtBSetupForces.class);

    private final Campaign campaign;
    private final List<Unit> units;
    private final AtBScenario scenario;
    private final ForceConsolidation forceConsolidationMethod;
    private final Set<Integer> teamIds = new HashSet<>();
    private final OrderFactory orderFactory;
    private final Game dummyGame;

    public AtBSetupForces(Campaign campaign, List<Unit> units, AtBScenario scenario,
            ForceConsolidation forceConsolidationMethod) {
        this(campaign, units, scenario, forceConsolidationMethod, new OrderFactory(campaign, scenario));
    }

    public AtBSetupForces(Campaign campaign, List<Unit> units, AtBScenario scenario,
            ForceConsolidation forceConsolidationMethod, OrderFactory orderFactory) {
        this.campaign = campaign;
        this.dummyGame = campaign.getGame();
        this.units = units;
        this.scenario = scenario;
        this.forceConsolidationMethod = forceConsolidationMethod;
        this.orderFactory = orderFactory;
        setupTeamIds();
    }

    private void setupTeamIds() {
        if (!units.isEmpty()) {
            teamIds.add(1);
        }
        for (int i = 0; i < scenario.getNumBots(); i++) {
            BotForce bf = scenario.getBotForce(i);
            teamIds.add(bf.getTeam());
        }
    }

    /**
     * Create the forces for the game object, using the campaign, units and scenario
     *
     * @param game The game object to setup the forces in
     */
    public void createForcesOnSimulation(SimulationContext game) {
        setupPlayer(game);
        setupBots(game);
        forceConsolidationMethod.consolidateForces(game);
        convertForcesIntoFormations(game);
    }

    @Override
    public void addOrdersToForces(SimulationContext context) {
        var orders = orderFactory.getOrders();
        context.getOrders().clear();
        context.getOrders().addAll(orders);
        context.getOrders().resetOrders();
    }

    @Override
    public boolean isTeamPresent(int teamId) {
        return teamIds.contains(teamId);
    }

    private static class FailedToConvertForceToFormationException extends RuntimeException {
        public FailedToConvertForceToFormationException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Convert the forces in the game to formations, this is the most important step
     * in the setup of the game,
     * it converts every top level force into a single formation, and those
     * formations are then added to the game
     * and used in the auto resolve in place of the original entities
     *
     * @param game The game object to convert the forces in
     */
    private void convertForcesIntoFormations(SimulationContext game) {
        for (var force : game.getForces().getTopLevelForces()) {
            try {
                var formation = new EntityAsUnit(force, game).convert();
                formation.setTargetFormationId(Entity.NONE);
                formation.setOwnerId(force.getOwnerId());
                game.addUnit(formation);
                game.getForces().addEntity(formation, force.getId());
            } catch (Exception e) {
                Sentry.captureException(e);
                var entities = game.getForces().getFullEntities(force).stream().filter(Entity.class::isInstance)
                        .map(Entity.class::cast).toList();
                logger.error("Error converting force to formation {} - {}", force, entities, e);
                throw new FailedToConvertForceToFormationException(e);
            }
        }
    }

    /**
     * Setup the player, its forces and entities in the game, it also sets the
     * player skill level.
     *
     * @param game The game object to setup the player in
     */
    private void setupPlayer(SimulationContext game) {
        var player = getCleanPlayer();
        game.addPlayer(player.getId(), player);
        var entities = setupPlayerForces(player);
        var playerSkill = campaign.getReputation().getAverageSkillLevel();
        game.setPlayerSkillLevel(player.getId(), playerSkill);
        sendEntities(entities, game);
    }

    /**
     * Move the entity by copying it, this is used to break references to the original instance
     * @param entity The entity to copy
     * @return The copied entity
     */
    private Entity moveByCopy(Entity entity) {
        try {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                    // Serialize the entities
                    objectOutputStream.writeObject(entity);
                    objectOutputStream.flush();
                    byte[] serializedData = byteArrayOutputStream.toByteArray();

                    // Deserialize to create new instances
                    try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData)) {
                        try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                            return (Entity) objectInputStream.readObject();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e, "Failed to break references for entity {}", entity);
            return null;
        }
    }

    /**
     * Setup the bots, their forces and entities in the game, it also sets the
     * player skill level.
     *
     * @param game The game object to setup the bots in
     */
    private void setupBots(SimulationContext game) {
        var forbiddenColor = game.getPlayer(0).getColour();
        var enemySkill = (scenario.getContract(campaign)).getEnemySkill();
        var allySkill = (scenario.getContract(campaign)).getAllySkill();
        var localBots = new HashMap<String, Player>();
        for (int i = 0; i < scenario.getNumBots(); i++) {
            BotForce botForce = scenario.getBotForce(i);
            String name = botForce.getName();
            if (localBots.containsKey(name)) {
                int append = 2;
                while (localBots.containsKey(name + append)) {
                    append++;
                }
                name += append;
            }
            var highestPlayerId = game.getPlayersList().stream().mapToInt(Player::getId).max().orElse(0);
            Player bot = new Player(highestPlayerId + 1, name);
            bot.setTeam(botForce.getTeam());

            localBots.put(name, bot);
            configureBot(bot, botForce, forbiddenColor);
            game.addPlayer(bot.getId(), bot);
            if (bot.isEnemyOf(campaign.getPlayer())) {
                game.setPlayerSkillLevel(bot.getId(), enemySkill);
            } else {
                game.setPlayerSkillLevel(bot.getId(), allySkill);
            }
            botForce.generateRandomForces(units, campaign);
            var botEntities = setupBotEntities(bot, botForce.getFullEntityList(campaign), botForce.getDeployRound());
            sendEntities(botEntities, game);
        }
    }

    /**
     * Create a player object from the campaign and scenario wichi doesnt have a
     * reference to the original player
     *
     * @return The clean player object
     */
    private Player getCleanPlayer() {
        var campaignPlayer = campaign.getPlayer();
        var player = new Player(campaignPlayer.getId(), campaign.getName());
        player.setCamouflage(campaign.getCamouflage().clone());
        player.setColour(campaign.getColour());
        player.setStartingPos(scenario.getStartingPos());
        player.setStartOffset(scenario.getStartOffset());
        player.setStartWidth(scenario.getStartWidth());
        player.setStartingAnyNWx(scenario.getStartingAnyNWx());
        player.setStartingAnyNWy(scenario.getStartingAnyNWy());
        player.setStartingAnySEx(scenario.getStartingAnySEx());
        player.setStartingAnySEy(scenario.getStartingAnySEy());
        player.setTeam(1);
        player.setNbrMFActive(scenario.getNumPlayerMinefields(Minefield.TYPE_ACTIVE));
        player.setNbrMFConventional(scenario.getNumPlayerMinefields(Minefield.TYPE_CONVENTIONAL));
        player.setNbrMFInferno(scenario.getNumPlayerMinefields(Minefield.TYPE_INFERNO));
        player.setNbrMFVibra(scenario.getNumPlayerMinefields(Minefield.TYPE_VIBRABOMB));
        player.getTurnInitBonus();
        return player;
    }

    /**
     * Setup the player forces and entities for the game
     *
     * @param player The player object to setup the forces for
     * @return A list of entities for the player
     */
    private List<Entity> setupPlayerForces(Player player) {
        boolean useDropship = isUsingDropship();
        List<Entity> entities = new ArrayList<>();
        entities.addAll(getCopyOfEntities(player, useDropship, new UnitEntitySource()));
        entities.addAll(getCopyOfEntities(player, useDropship, new AllyEntitySource()));
        return entities;
    }

    private List<Entity> getCopyOfEntities(Player player, boolean useDropship, EntitySource entitySource) {
        List<Entity> entities = new ArrayList<>();
        for (Object source : entitySource.getSources()) {
            Entity entity = entitySource.setupEntity(player, source, useDropship);
            if (entity == null) {
                continue;
            }
            entities.add(entity);
        }
        return entities;
    }

    private interface EntitySource {
        Iterable<?> getSources();
        Entity setupEntity(Player player, Object source, boolean useDropship);
    }

    private class UnitEntitySource implements EntitySource {
        @Override
        public Iterable<?> getSources() {
            return units;
        }

        @Override
        public Entity setupEntity(Player player, Object source, boolean useDropship) {
            return setupPlayerEntityFromUnit(player, (Unit) source, useDropship);
        }
    }

    private class AllyEntitySource implements EntitySource {
        @Override
        public Iterable<?> getSources() {
            return scenario.getAlliesPlayer();
        }

        @Override
        public Entity setupEntity(Player player, Object source, boolean useDropship) {
            return setupPlayerAllyEntity(player, (Entity) source, useDropship);
        }
    }

    private Entity setupPlayerAllyEntity(Player player, Entity originalAllyEntity, boolean useDropship) {
        var entity = moveByCopy(originalAllyEntity);
        if (Objects.isNull(entity)) {
            logger.error("Could not setup ally entity {}", originalAllyEntity);
            return null;
        }

        entity.setOwner(player);

        int deploymentRound = entity.getDeployRound();
        if (!(scenario instanceof AtBDynamicScenario)) {
            int speed = entity.getWalkMP();
            if (entity.getAnyTypeMaxJumpMP() > 0) {
                if (entity instanceof Infantry) {
                    speed = entity.getJumpMP();
                } else {
                    speed++;
                }
            }
            deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
            if (!useDropship
                    && scenario.getCombatRole().isPatrol()
                    && (scenario.getCombatTeamById(campaign) != null)
                    && (scenario.getCombatTeamById(campaign).getForceId() == scenario.getCombatTeamId())) {
                deploymentRound = Math.max(deploymentRound, 6 - speed);
            }
        }

        entity.setDeployRound(deploymentRound);
        return entity;
    }

    private Entity setupPlayerEntityFromUnit(Player player, Unit unit, boolean useDropship) {
        var entity = moveByCopy(unit.getEntity());
        if (Objects.isNull(entity)) {
            logger.error("Could not setup unit {} for player {}", unit, player);
            return null;
        }
        entity.setOwner(player);

        // Set the TempID for auto reporting
        entity.setExternalIdAsString(unit.getId().toString());

        // If this unit is a spacecraft, set the crew size and marine size values
        if (entity.isLargeCraft() || (entity.getUnitType() == UnitType.SMALL_CRAFT)) {
            entity.setNCrew(unit.getActiveCrew().size());
            entity.setNMarines(unit.getMarineCount());
        }
        // Calculate deployment round
        int deploymentRound = entity.getDeployRound();
        if (!(scenario instanceof AtBDynamicScenario)) {
            int speed = entity.getWalkMP();
            if (entity.getAnyTypeMaxJumpMP() > 0) {
                if (entity instanceof Infantry) {
                    speed = entity.getJumpMP();
                } else {
                    speed++;
                }
            }
            // Set scenario type-specific delay
            deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
            // Lances deployed in scout roles always deploy units in 6-walking speed turns
            if (scenario.getCombatRole().isPatrol()
                    && (scenario.getCombatTeamById(campaign) != null)
                    && (scenario.getCombatTeamById(campaign).getForceId() == scenario.getCombatTeamId())
                    && !useDropship) {
                deploymentRound = Math.max(deploymentRound, 6 - speed);
            }
        }
        entity.setDeployRound(deploymentRound);
        var force = campaign.getForceFor(unit);
        if (force != null) {
            entity.setForceString(force.getFullMMName());
        } else if (!unit.getEntity().getForceString().isBlank()) {
            // this was added mostly to make it easier to run tests
            entity.setForceString(unit.getEntity().getForceString());
        }
        return entity;
    }

    /**
     * Check if using dropships for patrol scenario
     * @return True if using dropships under specific conditions, false otherwise
     */
    private boolean isUsingDropship() {
        if (scenario.getCombatRole().isPatrol()) {
            for (Entity en : scenario.getAlliesPlayer()) {
                if (en.getUnitType() == UnitType.DROPSHIP) {
                    return true;
                }
            }
            for (Unit unit : units) {
                if (unit.getEntity().getUnitType() == UnitType.DROPSHIP) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Setup the map settings for the game, not relevant at the moment, as the map
     * settings are not used in the autoresolve currently
     *
     * @return The map settings object
     */
    private MapSettings setupMapSettings() {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
        mapSettings.setMapSize(1, 1);
        mapSettings.getBoardsSelectedVector().clear();

        // if the scenario is taking place in space, do space settings instead
        if (scenario.getBoardType() == Scenario.T_SPACE
                || scenario.getTerrainType().equals("Space")) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        } else if (scenario.isUsingFixedMap()) {
            String board = scenario.getMap().replace(".board", "");
            board = board.replace("\\", "/");
            mapSettings.getBoardsSelectedVector().add(board);

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }
        } else {
            File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml");
            try (InputStream is = new FileInputStream(mapgenFile)) {
                mapSettings = MapSettings.getInstance(is);
            } catch (IOException ex) {
                Sentry.captureException(ex);
                logger.error(
                        String.format("Could not load map file data/mapgen/%s.xml", scenario.getMap()),
                        ex);
            }

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }

            // duplicate code, but getting a new instance of map settings resets the size
            // parameters
            mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
            mapSettings.setMapSize(1, 1);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        }
        return mapSettings;
    }

    public PlayerColour getNextColor(PlayerColour playerColour) {
        PlayerColour[] playerColours = PlayerColour.values();
        int index = (playerColour.ordinal() + 1) % playerColours.length;
        return playerColours[index];
    }

    /**
     * Configure the bot player object with the bot force data
     *
     * @param bot      The bot player object
     * @param botForce The bot force data
     */
    private void configureBot(Player bot, BotForce botForce, PlayerColour forbiddenColor) {
        // set camo
        bot.setCamouflage(botForce.getCamouflage().clone());
        boolean isSameColorAsForbidden = botForce.getColour().equals(forbiddenColor);
        var color = isSameColorAsForbidden ? getNextColor(botForce.getColour()) : botForce.getColour();
        bot.setColour(color);
        bot.setTeam(botForce.getTeam());

        // set deployment
        bot.setStartingPos(botForce.getStartingPos());
        bot.setStartOffset(botForce.getStartOffset());
        bot.setStartWidth(botForce.getStartWidth());
        bot.setStartingAnyNWx(botForce.getStartingAnyNWx());
        bot.setStartingAnyNWy(botForce.getStartingAnyNWy());
        bot.setStartingAnySEx(botForce.getStartingAnySEx());
        bot.setStartingAnySEy(botForce.getStartingAnySEy());

    }

    /**
     * Setup the bot entities for the game
     *
     * @param bot              The bot player object
     * @param originalEntities The original entities for the bot
     * @param deployRound      The deployment round for the bot
     * @return A list of entities for the bot
     */
    private List<Entity> setupBotEntities(Player bot, List<Entity> originalEntities, int deployRound) {
        String forceName = bot.getName() + "|1";
        var entities = new ArrayList<Entity>();

        for (Entity originalBotEntity : originalEntities) {
            var entity = moveByCopy(originalBotEntity);

            if (entity == null) {
                logger.warn("Could not convert entity for bot {} - {}", bot.getName(), originalBotEntity);
                continue;
            }

            entity.setOwner(bot);
            entity.setForceString(forceName);
            entity.setId(originalBotEntity.getId());

            if (entity.getDeployRound() == 0) {
                entity.setDeployRound(deployRound);
            }
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Get the planetary conditions for the game, not used at the moment in the auto
     * resolve, but planed for the future
     *
     * @return The planetary conditions object
     */
    private PlanetaryConditions getPlanetaryConditions() {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        if (campaign.getCampaignOptions().isUseLightConditions()) {
            planetaryConditions.setLight(scenario.getLight());
        }
        if (campaign.getCampaignOptions().isUseWeatherConditions()) {
            planetaryConditions.setWeather(scenario.getWeather());
            planetaryConditions.setWind(scenario.getWind());
            planetaryConditions.setFog(scenario.getFog());
            planetaryConditions.setEMI(scenario.getEMI());
            planetaryConditions.setBlowingSand(scenario.getBlowingSand());
            planetaryConditions.setTemperature(scenario.getModifiedTemperature());
        }
        if (campaign.getCampaignOptions().isUsePlanetaryConditions()) {
            planetaryConditions.setAtmosphere(scenario.getAtmosphere());
            planetaryConditions.setGravity(scenario.getGravity());
        }
        return planetaryConditions;
    }

    /**
     * Send the entities to the game object
     *
     * @param entities The entities to send
     * @param game     the game object to send the entities to
     */
    private void sendEntities(List<Entity> entities, SimulationContext game) {
        Map<Integer, Integer> forceMapping = new HashMap<>();
        for (final Entity entity : entities) {
            lastTouchesBeforeSendingEntity(game, entity);
            game.getPlayer(entity.getOwnerId()).changeInitialEntityCount(1);

            // Restore forces from MULs or other external sources from the forceString, if
            // any
            if (!entity.getForceString().isBlank()) {
                List<megamek.common.force.Force> forceList = Forces.parseForceString(entity);
                int realId = NO_FORCE;
                boolean topLevel = true;

                for (megamek.common.force.Force force : forceList) {
                    if (!forceMapping.containsKey(force.getId())) {
                        if (topLevel) {
                            realId = game.getForces().addTopLevelForce(force, entity.getOwner());
                        } else {
                            megamek.common.force.Force parent = game.getForces().getForce(realId);
                            realId = game.getForces().addSubForce(force, parent);
                        }
                        forceMapping.put(force.getId(), realId);
                    } else {
                        realId = forceMapping.get(force.getId());
                    }
                    topLevel = false;
                }
                entity.setForceString("");
                entity.setGame(dummyGame);
                game.addEntity(entity);
                game.getForces().addEntity(entity, realId);
            }
        }
    }

    private static void lastTouchesBeforeSendingEntity(SimulationContext game, Entity entity) {
        if (entity instanceof ProtoMek) {
            int numPlayerProtos = game.getSelectedEntityCount(new EntitySelector() {
                private final int ownerId = entity.getOwnerId();

                @Override
                public boolean accept(Entity entity) {
                    return (entity instanceof ProtoMek) && (ownerId == entity.getOwnerId());
                }
            });

            entity.setUnitNumber((short) (numPlayerProtos / 5));
        }

        if (Entity.NONE == entity.getId()) {
            entity.setId(game.getNextEntityId());
        }

        // Give the unit a spotlight, if it has the spotlight quirk
        entity.setExternalSearchlight(entity.hasExternalSearchlight()
                || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
    }
}
