/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.extras.constraints.net;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * <p><b>Domain name</b> validation routines.</p>
 *
 * <p>
 * This validator provides methods for validating Internet domain names
 * and top-level domains.
 * </p>
 *
 * <p>Domain names are evaluated according
 * to the standards <a href="http://www.ietf.org/rfc/rfc1034.txt">RFC1034</a>,
 * section 3, and <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC1123</a>,
 * section 2.1. No accomodation is provided for the specialized needs of
 * other applications; if the domain name has been URL-encoded, for example,
 * validation will fail even though the equivalent plaintext version of the
 * same name would have passed.
 * </p>
 *
 * <p>
 * Validation is also provided for top-level domains (TLDs) as defined and
 * maintained by the Internet Assigned Numbers Authority (IANA):
 * </p>
 *
 *   <ul>
 *     <li>{@link #isValidInfrastructureTld} - validates infrastructure TLDs
 *         (<code>.arpa</code>, etc.)</li>
 *     <li>{@link #isValidGenericTld} - validates generic TLDs
 *         (<code>.com, .org</code>, etc.)</li>
 *     <li>{@link #isValidCountryCodeTld} - validates country code TLDs
 *         (<code>.us, .uk, .cn</code>, etc.)</li>
 *   </ul>
 *
 * <p>
 * (<b>NOTE</b>: This class does not provide IP address lookup for domain names or
 * methods to ensure that a given domain name matches a specific IP; see
 * {@link java.net.InetAddress} for that functionality.)
 * </p>
 */
public class DomainValidator implements ConstraintValidator<Domain, String> {

    private boolean allowLocal;

    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)
    private static final Pattern DOMAIN_LABEL = Pattern.compile("\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*");

    private static final Pattern DOMAIN_NAME_REGEX =
        Pattern.compile("^(?:" + DOMAIN_LABEL.pattern() + "\\.)+(\\p{Alpha}{2,})$");

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String domain, ConstraintValidatorContext context) {
        Matcher matcher = DOMAIN_NAME_REGEX.matcher(domain);
        if (matcher.matches()) {
            domain = matcher.group(1);
            return isValidTld(domain);
        } else if (allowLocal && DOMAIN_LABEL.matcher(domain).matches()) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined top-level domain. Leading dots are ignored if present.
     * The search is case-sensitive.
     *
     * @param tld the parameter to check for TLD status
     * @return true if the parameter is a TLD
     */
    boolean isValidTld(String tld) {
        if (allowLocal && isValidLocalTld(tld)) {
            return true;
        }

        tld = chompLeadingDot(tld).toLowerCase();
        return isValidInfrastructureTld(tld) || isValidGenericTld(tld) || isValidCountryCodeTld(tld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined infrastructure top-level domain. Leading dots are
     * ignored if present. The search is case-sensitive.
     *
     * @param iTld the parameter to check for infrastructure TLD status
     * @return true if the parameter is an infrastructure TLD
     */
    static boolean isValidInfrastructureTld(String iTld) {
        return INFRASTRUCTURE_TLDS.contains(iTld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined generic top-level domain. Leading dots are ignored
     * if present. The search is case-sensitive.
     *
     * @param gTld the parameter to check for generic TLD status
     * @return true if the parameter is a generic TLD
     */
    static boolean isValidGenericTld(String gTld) {
        return GENERIC_TLDS.contains(gTld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined country code top-level domain. Leading dots are
     * ignored if present. The search is case-sensitive.
     *
     * @param ccTld the parameter to check for country code TLD status
     * @return true if the parameter is a country code TLD
     */
    static boolean isValidCountryCodeTld(String ccTld) {
        return COUNTRY_CODE_TLDS.contains(ccTld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * widely used "local" domains (localhost or localdomain). Leading dots are
     * ignored if present. The search is case-sensitive.
     *
     * @param iTld the parameter to check for local TLD status
     * @return true if the parameter is an local TLD
     */
    static boolean isValidLocalTld(String iTld) {
        return LOCAL_TLDS.contains(iTld);
    }

    private static String chompLeadingDot(String str) {
        if (str.startsWith(".")) {
            return str.substring(1);
        }
        return str;
    }

    // ---------------------------------------------
    // ----- TLDs defined by IANA
    // ----- Authoritative and comprehensive list at:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    private static final List<String> INFRASTRUCTURE_TLDS = asList("arpa", // internet infrastructure
        "root" // diagnostic marker for non-truncated root zone
    );

    private static final List<String> GENERIC_TLDS = asList("aero", // air transport industry
        "asia", // Pan-Asia/Asia Pacific
        "biz", // businesses
        "cat", // Catalan linguistic/cultural community
        "com", // commercial enterprises
        "coop", // cooperative associations
        "info", // informational sites
        "jobs", // Human Resource managers
        "mobi", // mobile products and services
        "museum", // museums, surprisingly enough
        "name", // individuals' sites
        "net", // internet support infrastructure/business
        "org", // noncommercial organizations
        "pro", // credentialed professionals and entities
        "tel", // contact data for businesses and individuals
        "travel", // entities in the travel industry
        "gov", // United States Government
        "edu", // accredited postsecondary US education entities
        "mil", // United States Military
        "int" // organizations established by international treaty
    );

    private static final List<String> COUNTRY_CODE_TLDS = asList("ac", // Ascension Island
        "ad", // Andorra
        "ae", // United Arab Emirates
        "af", // Afghanistan
        "ag", // Antigua and Barbuda
        "ai", // Anguilla
        "al", // Albania
        "am", // Armenia
        "an", // Netherlands Antilles
        "ao", // Angola
        "aq", // Antarctica
        "ar", // Argentina
        "as", // American Samoa
        "at", // Austria
        "au", // Australia (includes Ashmore and Cartier Islands and Coral Sea Islands)
        "aw", // Aruba
        "ax", // Åland
        "az", // Azerbaijan
        "ba", // Bosnia and Herzegovina
        "bb", // Barbados
        "bd", // Bangladesh
        "be", // Belgium
        "bf", // Burkina Faso
        "bg", // Bulgaria
        "bh", // Bahrain
        "bi", // Burundi
        "bj", // Benin
        "bm", // Bermuda
        "bn", // Brunei Darussalam
        "bo", // Bolivia
        "br", // Brazil
        "bs", // Bahamas
        "bt", // Bhutan
        "bv", // Bouvet Island
        "bw", // Botswana
        "by", // Belarus
        "bz", // Belize
        "ca", // Canada
        "cc", // Cocos (Keeling) Islands
        "cd", // Democratic Republic of the Congo (formerly Zaire)
        "cf", // Central African Republic
        "cg", // Republic of the Congo
        "ch", // Switzerland
        "ci", // Côte d'Ivoire
        "ck", // Cook Islands
        "cl", // Chile
        "cm", // Cameroon
        "cn", // China, mainland
        "co", // Colombia
        "cr", // Costa Rica
        "cu", // Cuba
        "cv", // Cape Verde
        "cx", // Christmas Island
        "cy", // Cyprus
        "cz", // Czech Republic
        "de", // Germany
        "dj", // Djibouti
        "dk", // Denmark
        "dm", // Dominica
        "do", // Dominican Republic
        "dz", // Algeria
        "ec", // Ecuador
        "ee", // Estonia
        "eg", // Egypt
        "er", // Eritrea
        "es", // Spain
        "et", // Ethiopia
        "eu", // European Union
        "fi", // Finland
        "fj", // Fiji
        "fk", // Falkland Islands
        "fm", // Federated States of Micronesia
        "fo", // Faroe Islands
        "fr", // France
        "ga", // Gabon
        "gb", // Great Britain (United Kingdom)
        "gd", // Grenada
        "ge", // Georgia
        "gf", // French Guiana
        "gg", // Guernsey
        "gh", // Ghana
        "gi", // Gibraltar
        "gl", // Greenland
        "gm", // The Gambia
        "gn", // Guinea
        "gp", // Guadeloupe
        "gq", // Equatorial Guinea
        "gr", // Greece
        "gs", // South Georgia and the South Sandwich Islands
        "gt", // Guatemala
        "gu", // Guam
        "gw", // Guinea-Bissau
        "gy", // Guyana
        "hk", // Hong Kong
        "hm", // Heard Island and McDonald Islands
        "hn", // Honduras
        "hr", // Croatia (Hrvatska)
        "ht", // Haiti
        "hu", // Hungary
        "id", // Indonesia
        "ie", // Ireland (Éire)
        "il", // Israel
        "im", // Isle of Man
        "in", // India
        "io", // British Indian Ocean Territory
        "iq", // Iraq
        "ir", // Iran
        "is", // Iceland
        "it", // Italy
        "je", // Jersey
        "jm", // Jamaica
        "jo", // Jordan
        "jp", // Japan
        "ke", // Kenya
        "kg", // Kyrgyzstan
        "kh", // Cambodia (Khmer)
        "ki", // Kiribati
        "km", // Comoros
        "kn", // Saint Kitts and Nevis
        "kp", // North Korea
        "kr", // South Korea
        "kw", // Kuwait
        "ky", // Cayman Islands
        "kz", // Kazakhstan
        "la", // Laos (currently being marketed as the official domain for Los Angeles)
        "lb", // Lebanon
        "lc", // Saint Lucia
        "li", // Liechtenstein
        "lk", // Sri Lanka
        "lr", // Liberia
        "ls", // Lesotho
        "lt", // Lithuania
        "lu", // Luxembourg
        "lv", // Latvia
        "ly", // Libya
        "ma", // Morocco
        "mc", // Monaco
        "md", // Moldova
        "me", // Montenegro
        "mg", // Madagascar
        "mh", // Marshall Islands
        "mk", // Republic of Macedonia
        "ml", // Mali
        "mm", // Myanmar
        "mn", // Mongolia
        "mo", // Macau
        "mp", // Northern Mariana Islands
        "mq", // Martinique
        "mr", // Mauritania
        "ms", // Montserrat
        "mt", // Malta
        "mu", // Mauritius
        "mv", // Maldives
        "mw", // Malawi
        "mx", // Mexico
        "my", // Malaysia
        "mz", // Mozambique
        "na", // Namibia
        "nc", // New Caledonia
        "ne", // Niger
        "nf", // Norfolk Island
        "ng", // Nigeria
        "ni", // Nicaragua
        "nl", // Netherlands
        "no", // Norway
        "np", // Nepal
        "nr", // Nauru
        "nu", // Niue
        "nz", // New Zealand
        "om", // Oman
        "pa", // Panama
        "pe", // Peru
        "pf", // French Polynesia With Clipperton Island
        "pg", // Papua New Guinea
        "ph", // Philippines
        "pk", // Pakistan
        "pl", // Poland
        "pm", // Saint-Pierre and Miquelon
        "pn", // Pitcairn Islands
        "pr", // Puerto Rico
        "ps", // Palestinian territories (PA-controlled West Bank and Gaza Strip)
        "pt", // Portugal
        "pw", // Palau
        "py", // Paraguay
        "qa", // Qatar
        "re", // Réunion
        "ro", // Romania
        "rs", // Serbia
        "ru", // Russia
        "rw", // Rwanda
        "sa", // Saudi Arabia
        "sb", // Solomon Islands
        "sc", // Seychelles
        "sd", // Sudan
        "se", // Sweden
        "sg", // Singapore
        "sh", // Saint Helena
        "si", // Slovenia
        "sj", // Svalbard and Jan Mayen Islands Not in use (Norwegian dependencies; see .no)
        "sk", // Slovakia
        "sl", // Sierra Leone
        "sm", // San Marino
        "sn", // Senegal
        "so", // Somalia
        "sr", // Suriname
        "st", // São Tomé and Príncipe
        "su", // Soviet Union (deprecated)
        "sv", // El Salvador
        "sy", // Syria
        "sz", // Swaziland
        "tc", // Turks and Caicos Islands
        "td", // Chad
        "tf", // French Southern and Antarctic Lands
        "tg", // Togo
        "th", // Thailand
        "tj", // Tajikistan
        "tk", // Tokelau
        "tl", // East Timor (deprecated old code)
        "tm", // Turkmenistan
        "tn", // Tunisia
        "to", // Tonga
        "tp", // East Timor
        "tr", // Turkey
        "tt", // Trinidad and Tobago
        "tv", // Tuvalu
        "tw", // Taiwan, Republic of China
        "tz", // Tanzania
        "ua", // Ukraine
        "ug", // Uganda
        "uk", // United Kingdom
        "um", // United States Minor Outlying Islands
        "us", // United States of America
        "uy", // Uruguay
        "uz", // Uzbekistan
        "va", // Vatican City State
        "vc", // Saint Vincent and the Grenadines
        "ve", // Venezuela
        "vg", // British Virgin Islands
        "vi", // U.S. Virgin Islands
        "vn", // Vietnam
        "vu", // Vanuatu
        "wf", // Wallis and Futuna
        "ws", // Samoa (formerly Western Samoa)
        "ye", // Yemen
        "yt", // Mayotte
        "yu", // Serbia and Montenegro (originally Yugoslavia)
        "za", // South Africa
        "zm", // Zambia
        "zw" // Zimbabwe
    );

    private static final List<String> LOCAL_TLDS = asList("localhost", // RFC2606 defined
        "localdomain" // Also widely used as localhost.localdomain
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Domain domain) {
        allowLocal = domain.allowLocal();
    }

}
