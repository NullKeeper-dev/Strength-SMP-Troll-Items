package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class RavagerProtectionListenerTest {
    private PlayerMock shooter;
    private PlayerMock target;
    private PlayerMock outsider;
    private Ravager ravager;
    private Ravager ordinary;
    private RavagerProtectionListener listener;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        shooter = server.addPlayer("Shooter");
        target = server.addPlayer("Target");
        outsider = server.addPlayer("Outsider");
        ravager = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);
        ordinary = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);
        RavagerMetadataStore metadata = new RavagerMetadataStore(new PersistentKeys(plugin));
        metadata.write(ravager, new RavagerAssignment(
                shooter.getUniqueId(),
                target.getUniqueId()));
        listener = new RavagerProtectionListener(metadata, new RavagerAccessPolicy());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onlyParticipantsMayDamageOrInteractWithPrivateRavager() {
        EntityDamageByEntityEvent outsiderDamage = damage(outsider, ravager);
        EntityDamageByEntityEvent shooterDamage = damage(shooter, ravager);
        EntityDamageByEntityEvent targetDamage = damage(target, ravager);
        listener.onDamage(outsiderDamage);
        listener.onDamage(shooterDamage);
        listener.onDamage(targetDamage);

        assertTrue(outsiderDamage.isCancelled());
        assertFalse(shooterDamage.isCancelled());
        assertFalse(targetDamage.isCancelled());

        PlayerInteractEntityEvent outsiderInteraction = new PlayerInteractEntityEvent(
                outsider,
                ravager,
                EquipmentSlot.HAND);
        PlayerInteractEntityEvent targetInteraction = new PlayerInteractEntityEvent(
                target,
                ravager,
                EquipmentSlot.HAND);
        listener.onInteract(outsiderInteraction);
        listener.onInteract(targetInteraction);

        assertTrue(outsiderInteraction.isCancelled());
        assertFalse(targetInteraction.isCancelled());
    }

    @Test
    void privateRavagerMayDamageAndTargetOnlyAssignedPlayer() {
        EntityDamageByEntityEvent shooterDamage = damage(ravager, shooter);
        EntityDamageByEntityEvent targetDamage = damage(ravager, target);
        EntityDamageByEntityEvent outsiderDamage = damage(ravager, outsider);
        listener.onDamage(shooterDamage);
        listener.onDamage(targetDamage);
        listener.onDamage(outsiderDamage);

        assertTrue(shooterDamage.isCancelled());
        assertFalse(targetDamage.isCancelled());
        assertTrue(outsiderDamage.isCancelled());

        EntityTargetLivingEntityEvent wrongTarget = new EntityTargetLivingEntityEvent(
                ravager,
                outsider,
                TargetReason.CLOSEST_PLAYER);
        EntityTargetLivingEntityEvent assignedTarget = new EntityTargetLivingEntityEvent(
                ravager,
                target,
                TargetReason.CLOSEST_PLAYER);
        listener.onTarget(wrongTarget);
        listener.onTarget(assignedTarget);

        assertTrue(wrongTarget.isCancelled());
        assertFalse(assignedTarget.isCancelled());
    }

    @Test
    void unrelatedRavagersRemainVanilla() {
        EntityDamageByEntityEvent damage = damage(outsider, ordinary);
        PlayerInteractEntityEvent interaction = new PlayerInteractEntityEvent(
                outsider,
                ordinary,
                EquipmentSlot.HAND);
        listener.onDamage(damage);
        listener.onInteract(interaction);

        assertFalse(damage.isCancelled());
        assertFalse(interaction.isCancelled());
    }

    private static EntityDamageByEntityEvent damage(
            org.bukkit.entity.Entity damager,
            org.bukkit.entity.Entity victim) {
        return new EntityDamageByEntityEvent(damager, victim, DamageCause.ENTITY_ATTACK, 1.0);
    }
}
