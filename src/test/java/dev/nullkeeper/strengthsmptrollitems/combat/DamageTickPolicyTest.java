package dev.nullkeeper.strengthsmptrollitems.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static dev.nullkeeper.strengthsmptrollitems.combat.DamageEventFixtures.zeroFinalDamage;

@SuppressWarnings({"deprecation", "removal"})
class DamageTickPolicyTest {
    private ServerMock server;
    private PlayerMock attacker;
    private PlayerMock target;
    private DamageTickPolicy policy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        attacker = server.addPlayer("Attacker");
        target = server.addPlayer("Target");
        policy = new DamageTickPolicy();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void freshPositiveRawDamageQualifies() {
        assertTrue(policy.qualifies(damage(target, 1.0), target));
    }

    @Test
    void positiveRawDamageQualifiesWhenFinalDamageIsZero() {
        EntityDamageByEntityEvent event = zeroFinalDamage(
                attacker,
                target,
                DamageCause.ENTITY_ATTACK);

        assertTrue(policy.qualifies(event, target));
    }

    @Test
    void livingNonPlayerTargetQualifies() {
        LivingEntity cow = (LivingEntity) target.getWorld().spawnEntity(
                target.getLocation(),
                EntityType.COW);

        assertTrue(policy.qualifies(damage(cow, 1.0), cow));
    }

    @Test
    void cancelledDamageDoesNotQualify() {
        EntityDamageByEntityEvent event = damage(target, 1.0);
        event.setCancelled(true);

        assertFalse(policy.qualifies(event, target));
    }

    @Test
    void zeroRawDamageDoesNotQualify() {
        assertFalse(policy.qualifies(damage(target, 0.0), target));
    }

    @Test
    void creativeOrSpectatorTargetDoesNotQualify() {
        target.setGameMode(GameMode.CREATIVE);
        assertFalse(policy.qualifies(damage(target, 1.0), target));

        target.setGameMode(GameMode.SPECTATOR);
        assertFalse(policy.qualifies(damage(target, 1.0), target));
    }

    @Test
    void explicitlyInvulnerableTargetDoesNotQualify() {
        target.setInvulnerable(true);

        assertFalse(policy.qualifies(damage(target, 1.0), target));
    }

    @Test
    void positiveDamageEventQualifiesWhileNoDamageTicksIsPositive() {
        target.setNoDamageTicks(5);

        assertTrue(policy.qualifies(damage(target, 1.0), target));
    }

    private EntityDamageByEntityEvent damage(LivingEntity victim, double amount) {
        return new EntityDamageByEntityEvent(
                attacker,
                victim,
                DamageCause.ENTITY_ATTACK,
                amount);
    }
}
