package dev.sh3lby.antiillegalenchants.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class CheckResult {
    private static final CheckResult LEGAL = new CheckResult(List.of());

    private final List<EnchantViolation> violations;

    private CheckResult(List<EnchantViolation> violations) {
        this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
    }

    public static CheckResult legal() {
        return LEGAL;
    }

    public static CheckResult illegal(List<EnchantViolation> violations) {
        if (violations.isEmpty()) {
            return legal();
        }
        return new CheckResult(violations);
    }

    public boolean legalItem() {
        return violations.isEmpty();
    }

    public boolean illegalItem() {
        return !legalItem();
    }

    public List<EnchantViolation> violations() {
        return violations;
    }

    public String summary() {
        if (violations.isEmpty()) {
            return "LEGAL";
        }
        return violations.stream()
            .map(EnchantViolation::readable)
            .collect(Collectors.joining("; "));
    }
}
