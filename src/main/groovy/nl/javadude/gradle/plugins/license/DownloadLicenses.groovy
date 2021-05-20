/*
 * Copyright (C)2011 - Jeroen van Erp <jeroen@javadude.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.javadude.gradle.plugins.license

import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*

import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends ConventionTask {

    /**
     * Custom license mapping that overrides existent if needed.
     */
    @Input Map<Object, Object> licenses

    /**
     * Aliases for licences that has different names spelling.
     */
    @Input Map<Object, List<Object>> aliases

    /**
     * Generate report for each dependency.
     */
    @Input boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    @Input boolean reportByLicenseType

    /**
     * Include project dependencies in reports.
     */
    @Input boolean includeProjectDependencies

    /**
     * Ignore fatal errors when parsing POMs of transitive dependencies.
     */
    @Input boolean ignoreFatalParseErrors

    /**
     * List of dependencies that will be omitted in the report.
     */
    @Input List<String> excludeDependencies

    /**
     * Output directory for xml reports.
     */
    @OutputDirectory File xmlDestination

    /**
     * Output directory for html reports.
     */
    @OutputDirectory File htmlDestination

    /**
     * Output directory for json reports.
     */
    @OutputDirectory File jsonDestination

    /**
     * File name for reports by dependency.
     */
    @Input String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    @Input String reportByLicenseFileName

    /**
     * Is xml reports are enabled.
     */
    @Input boolean xml

    /**
     * Is html reports are enabled.
     */
    @Input private boolean html

    /**
     * Are json reports enabled.
     */

    @Input boolean json

    /**
     * The dependency configuration to report on.
     */
    @Input String dependencyConfiguration

    /*
    The following getters and setters are provided as Gradle 7 throws validation errors like:

    Some problems were found with the configuration of task ':downloadLicenses' (type 'DownloadLicenses').
  - Type 'nl.javadude.gradle.plugins.license.DownloadLicenses' property 'html' has redundant getters: 'getHtml()' and 'isHtml()'.

    Reason: Boolean property 'html' has both an `is` and a `get` getter.

    Possible solutions:
      1. Remove one of the getters.
      2. Annotate one of the getters with @Internal.

    Please refer to https://docs.gradle.org/7.0.2/userguide/validation_problems.html#redundant_getters for more details about this problem.
    ...
    
     */

    boolean isHtml() { return html }

    @Internal
    boolean getHtml() { return html }


    boolean isIgnoreFatalParseErrors() { return ignoreFatalParseErrors }

    @Internal
    boolean getIgnoreFatalParseErrors() { return ignoreFatalParseErrors }


    boolean isIncludeProjectDependencies() { return includeProjectDependencies }

    @Internal
    boolean getIncludeProjectDependencies() { return includeProjectDependencies }


    boolean isJson() { return json }

    @Internal
    boolean getJson() { return json }


    boolean isReportByDependency() { return reportByDependency }

    @Internal
    boolean getReportByDependency() { return reportByDependency }

    @TaskAction
    def downloadLicenses() {
        if (!enabled || (!isReportByDependency() && !isReportByLicenseType())
                || (!isXml() && !isHtml() && !isJson())) {
            didWork = false;
            return;
        }

        // Lazy dependency resolving
        def dependencyLicensesSet = {
            def licenseResolver = new LicenseResolver(project: project,
                    includeProjectDependencies: getIncludeProjectDependencies(),
                    ignoreFatalParseErrors: getIgnoreFatalParseErrors(),
                    aliases: aliases.collectEntries {
                        new MapEntry(resolveAliasKey(it.key), it.value)
                    },
                    licenses: getLicenses(),
                    dependenciesToIgnore: excludeDependencies,
                    dependencyConfiguration: dependencyConfiguration)
            licenseResolver.provideLicenseMap4Dependencies()
        }.memoize()

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(xmlOutputDir: getXmlDestination(), htmlOutputDir: getHtmlDestination(), jsonOutputDir: getJsonDestination()) }

        // Generate report that groups dependencies
        if (isReportByDependency()) {
            if(isHtml()) {
                reporter().generateHTMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".html")
            }
            if(isXml()) {
                reporter().generateXMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".xml")
            }
            if(isJson()) {
                reporter().generateJSONReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".json")
            }
        }

        // Generate report that groups licenses
        if (isReportByLicenseType()) {
            if(isHtml()) {
                reporter().generateHTMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".html")
            }
            if( isXml()) {
                reporter().generateXMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".xml")
            }
            if(isJson()) {
                reporter().generateJSONReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".json")
            }
        }
    }

    LicenseMetadata resolveAliasKey(key) {
        if(key instanceof String) {
            license(key)
        } else {
            key
        }
    }
}
