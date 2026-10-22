package dev.sh3lby.antiillegalenchants.config;

import org.bukkit.enchantments.Enchantment;

public record BannedCombination(Enchantment first, Enchantment second) {
    public boolean matches(Enchantment left, Enchantment right) {
        return (first.equals(left) && second.equals(right)) || (first.equals(right) && second.equals(left));
    }
}
