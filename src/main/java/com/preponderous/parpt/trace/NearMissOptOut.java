package com.preponderous.parpt.trace;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Locale;

/**
 * Finds an attempt to turn usage reporting off that is not written as the real switch,
 * {@code usage-reporting.enabled}. Spring binds only that name (and its relaxed forms, such as
 * {@code USAGE_REPORTING_ENABLED}), so anything else used to be ignored and reporting stayed on. An operator
 * who wrote any of these did not ask for reporting, so each counts as off:
 * <ul>
 *   <li>a property that spells "enabled" another way under any spelling of "usage reporting"
 *       ({@code usage_reporting.enable}, {@code usageReporting.Enabled}, ...) whose value is not
 *       clearly on ({@link TraceClient#allowsReporting(Object)});</li>
 *   <li>one that spells "disabled" ({@code usage-reporting.disabled}, {@code USAGE_REPORTING_DISABLE},
 *       ...) whose value is on;</li>
 *   <li>a value given to "usage reporting" itself ({@code usage-reporting=false},
 *       {@code USAGE_REPORTING=off}, ...) that is not clearly on.</li>
 * </ul>
 * {@code TRACE_USAGE_REPORTING} is left to the client, which reads it itself.
 */
final class NearMissOptOut {

    private NearMissOptOut() {
    }

    /** The near-miss property that turns reporting off, or null when there is none. */
    static String find(Environment environment, String realSwitch) {
        if (!(environment instanceof ConfigurableEnvironment)) {
            return null;
        }
        ConfigurationPropertyName switchName = ConfigurationPropertyName.of(realSwitch);
        for (ConfigurationPropertySource source
                : ConfigurationPropertySources.from(((ConfigurableEnvironment) environment).getPropertySources())) {
            if (!(source instanceof IterableConfigurationPropertySource)) {
                continue;
            }
            for (ConfigurationPropertyName name : (IterableConfigurationPropertySource) source) {
                if (name.equals(switchName)) {
                    continue; // the switch itself, which Spring resolves
                }
                String key = normalise(name.toString());
                if (!key.contains("usagereport") || key.equals("traceusagereporting")) {
                    continue;
                }
                ConfigurationProperty property = source.getConfigurationProperty(name);
                Object value = property == null ? null : property.getValue();
                if (isOptOut(key, value)) {
                    return name.toString();
                }
            }
        }
        return null;
    }

    static boolean isOptOut(String normalisedKey, Object value) {
        if (normalisedKey.contains("disabl")) {
            return TraceClient.allowsReporting(value);
        }
        if (normalisedKey.contains("enabl") || normalisedKey.endsWith("usagereporting")) {
            return !TraceClient.allowsReporting(value);
        }
        return false;
    }

    /** The key in lower case with everything but letters and digits dropped. */
    static String normalise(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
