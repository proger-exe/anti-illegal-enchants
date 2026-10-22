package dev.sh3lby.antiillegalenchants;

import dev.sh3lby.antiillegalenchants.action.IllegalItemAction;
import dev.sh3lby.antiillegalenchants.action.IllegalItemActionService;
import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import dev.sh3lby.antiillegalenchants.action.ScanContext;
import dev.sh3lby.antiillegalenchants.check.CheckResult;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import dev.sh3lby.antiillegalenchants.check.ViolationType;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantCheckServiceTest {
    private AntiIllegalEnchantsPlugin plugin;
    private EnchantCheckService checkService;
    private IllegalItemActionService actionService;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(AntiIllegalEnchantsPlugin.class);
        checkService = plugin.checkService();
        actionService = new IllegalItemActionService(checkService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void rejectsSharpnessAboveConfiguredMax() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);

        CheckResult result = checkService.check(sword);

        assertTrue(result.illegalItem());
        assertTrue(hasViolation(result, ViolationType.MAX_LEVEL_EXCEEDED));
    }

    @Test
    void rejectsConflictingVanillaEnchantments() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        sword.addUnsafeEnchantment(Enchantment.SMITE, 5);

        CheckResult result = checkService.check(sword);

        assertTrue(result.illegalItem());
        assertTrue(hasViolation(result, ViolationType.CONFLICTING_ENCHANTS));
    }

    @Test
    void rejectsConfiguredBannedCombination() {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.MENDING, 1);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);

        CheckResult result = checkService.check(bow);

        assertTrue(result.illegalItem());
        assertTrue(hasViolation(result, ViolationType.BANNED_COMBINATION));
    }

    @Test
    void rejectsEnchantNotSupportedByItem() {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);

        CheckResult result = checkService.check(bow);

        assertTrue(result.illegalItem());
        assertTrue(hasViolation(result, ViolationType.INVALID_FOR_ITEM));
    }

    @Test
    void materialWhitelistSkipsOwnEnchantments() {
        ItemStack stick = new ItemStack(Material.STICK);
        stick.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);

        assertTrue(checkService.check(stick).legalItem());
    }

    @Test
    void elytraIsNeverChecked() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        elytra.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);

        assertTrue(checkService.check(elytra).legalItem());
    }

    @Test
    void vanishingCurseIsLegalByDefault() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);

        assertTrue(checkService.check(sword).legalItem());
    }

    @Test
    void acceptsLegalVanillaEnchantments() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);

        assertTrue(checkService.check(sword).legalItem());
    }

    @Test
    void anvilResultIgnoresApplicabilityFalsePositives() {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);

        assertTrue(checkService.checkAnvilResult(bow).legalItem());
    }

    @Test
    void anvilResultStillRejectsOverleveledEnchantments() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);

        CheckResult result = checkService.checkAnvilResult(sword);

        assertTrue(result.illegalItem());
        assertTrue(hasViolation(result, ViolationType.MAX_LEVEL_EXCEEDED));
    }

    @Test
    void anvilPreviewStripsIllegalEnchantInsteadOfDeletingResult() {
        ItemScanService scanService = plugin.scanService();
        AtomicReference<ItemStack> preview = new AtomicReference<>();
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);

        boolean changed = scanService.scanAnvilResult(sword, preview::set, new ScanContext(null, "Anvil", true));

        assertTrue(changed);
        assertFalse(checkService.isEmpty(preview.get()));
        assertFalse(preview.get().containsEnchantment(Enchantment.SHARPNESS));
    }

    @Test
    void deleteActionRemovesIllegalItem() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);
        CheckResult result = checkService.check(sword);

        assertNull(actionService.apply(sword, result, IllegalItemAction.DELETE));
    }

    @Test
    void stripActionRemovesOnlyOffendingEnchantments() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);
        sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        CheckResult result = checkService.check(sword);

        ItemStack stripped = actionService.apply(sword, result, IllegalItemAction.STRIP);

        assertFalse(stripped.containsEnchantment(Enchantment.SHARPNESS));
        assertEquals(3, stripped.getEnchantmentLevel(Enchantment.UNBREAKING));
    }

    @Test
    void stripActionKeepsVanishingCurseWhenOtherEnchantIsIllegal() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);
        sword.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        CheckResult result = checkService.check(sword);

        ItemStack stripped = actionService.apply(sword, result, IllegalItemAction.STRIP);

        assertFalse(stripped.containsEnchantment(Enchantment.SHARPNESS));
        assertTrue(stripped.containsEnchantment(Enchantment.VANISHING_CURSE));
    }

    @Test
    void legalSlotScanDoesNotCallSetter() {
        ItemScanService scanService = plugin.scanService();
        AtomicBoolean setterCalled = new AtomicBoolean(false);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);

        boolean changed = scanService.scanSlot(sword, item -> setterCalled.set(true), new ScanContext(null, "Test", false));

        assertFalse(changed);
        assertFalse(setterCalled.get());
    }

    @Test
    void nestedShulkerItemsAreProcessed() {
        ItemStack shulkerItem = new ItemStack(Material.SHULKER_BOX);
        ItemMeta rawMeta = shulkerItem.getItemMeta();
        assertTrue(rawMeta instanceof BlockStateMeta);
        BlockStateMeta meta = (BlockStateMeta) rawMeta;
        assertTrue(meta.getBlockState() instanceof ShulkerBox);
        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);
        shulkerBox.getInventory().setItem(0, sword);
        meta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(meta);

        actionService.processNested(shulkerItem, IllegalItemAction.DELETE);

        BlockStateMeta updatedMeta = (BlockStateMeta) shulkerItem.getItemMeta();
        ShulkerBox updatedBox = (ShulkerBox) updatedMeta.getBlockState();
        assertNull(updatedBox.getInventory().getItem(0));
    }

    private boolean hasViolation(CheckResult result, ViolationType type) {
        return result.violations().stream().anyMatch(violation -> violation.type() == type);
    }
}
