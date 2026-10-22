package dev.sh3lby.antiillegalenchants.check;

import org.bukkit.enchantments.Enchantment;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class EnchantViolation {
    private final ViolationType type;
    private final Set<Enchantment> enchantments;
    private final String detail;

    public EnchantViolation(ViolationType type, Set<Enchantment> enchantments, String detail) {
        this.type = Objects.requireNonNull(type, "type");
        this.enchantments = Collections.unmodifiableSet(new LinkedHashSet<>(enchantments));
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public ViolationType type() {
        return type;
    }

    public Set<Enchantment> enchantments() {
        return enchantments;
    }

    public String detail() {
        return detail;
    }

    public String readable() {
        return type + ": " + detail;
    }

    public String enchantmentKeys() {
        return enchantments.stream()
            .map(enchantment -> enchantment.getKey().getKey())
            .collect(Collectors.joining(","));
    }
}
