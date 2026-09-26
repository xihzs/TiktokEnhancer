package com.ash.tiktokregion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CountryPreset {

    private final String id;
    private final String flag;
    private final String countryName;
    private final String countryIso;
    private final String mcc;
    private final String mnc;
    private final String operatorMccMnc;
    private final String operatorName;
    private final String localeLanguage;
    private final String localeCountry;

    public CountryPreset(String id, String flag, String countryName, String countryIso,
                         String mcc, String mnc, String operatorMccMnc, String operatorName,
                         String localeLanguage, String localeCountry) {
        this.id = id;
        this.flag = flag;
        this.countryName = countryName;
        this.countryIso = countryIso.toLowerCase(Locale.ROOT);
        this.mcc = mcc;
        this.mnc = mnc;
        this.operatorMccMnc = operatorMccMnc;
        this.operatorName = operatorName;
        this.localeLanguage = localeLanguage.toLowerCase(Locale.ROOT);
        this.localeCountry = localeCountry.toUpperCase(Locale.ROOT);
    }

    public String getId() { return id; }
    public String getFlag() { return flag; }
    public String getCountryName() { return countryName; }
    public String getCountryIso() { return countryIso; }
    public String getMcc() { return mcc; }
    public String getMnc() { return mnc; }
    public String getOperatorMccMnc() { return operatorMccMnc; }
    public String getOperatorName() { return operatorName; }
    public String getLocaleLanguage() { return localeLanguage; }
    public String getLocaleCountry() { return localeCountry; }

    public String getDisplayText() {
        return flag + " " + countryName + " (" + operatorName + " · " + countryIso.toUpperCase(Locale.ROOT) + ")";
    }

    public static final List<CountryPreset> PRESETS;

    static {
        List<CountryPreset> list = new ArrayList<>();

        list.add(new CountryPreset("US_TMOBILE", "🇺🇸", "United States", "us", "310", "260", "310260", "T-Mobile", "en", "US"));
        list.add(new CountryPreset("US_VERIZON", "🇺🇸", "United States", "us", "311", "480", "311480", "Verizon", "en", "US"));
        list.add(new CountryPreset("US_ATT", "🇺🇸", "United States", "us", "310", "410", "310410", "AT&T", "en", "US"));
        list.add(new CountryPreset("CA_ROGERS", "🇨🇦", "Canada", "ca", "302", "720", "302720", "Rogers", "en", "CA"));
        list.add(new CountryPreset("CA_BELL", "🇨🇦", "Canada", "ca", "302", "610", "302610", "Bell", "en", "CA"));
        list.add(new CountryPreset("CA_TELUS", "🇨🇦", "Canada", "ca", "302", "220", "302220", "Telus", "en", "CA"));
        list.add(new CountryPreset("MX_TELCEL", "🇲🇽", "Mexico", "mx", "334", "020", "334020", "Telcel", "es", "MX"));

        list.add(new CountryPreset("MY_MAXIS", "🇲🇾", "Malaysia", "my", "502", "12", "50212", "Maxis", "ms", "MY"));
        list.add(new CountryPreset("MY_CELCOM", "🇲🇾", "Malaysia", "my", "502", "19", "50219", "Celcom", "ms", "MY"));
        list.add(new CountryPreset("MY_DIGI", "🇲🇾", "Malaysia", "my", "502", "16", "50216", "Digi", "ms", "MY"));
        list.add(new CountryPreset("MY_UMOBILE", "🇲🇾", "Malaysia", "my", "502", "18", "50218", "U Mobile", "ms", "MY"));
        list.add(new CountryPreset("SG_SINGTEL", "🇸🇬", "Singapore", "sg", "525", "01", "52501", "Singtel", "en", "SG"));
        list.add(new CountryPreset("SG_STARHUB", "🇸🇬", "Singapore", "sg", "525", "05", "52505", "StarHub", "en", "SG"));
        list.add(new CountryPreset("ID_TELKOMSEL", "🇮🇩", "Indonesia", "id", "510", "10", "51010", "Telkomsel", "in", "ID"));
        list.add(new CountryPreset("ID_INDOSAT", "🇮🇩", "Indonesia", "id", "510", "01", "51001", "Indosat Ooredoo", "in", "ID"));
        list.add(new CountryPreset("ID_XL", "🇮🇩", "Indonesia", "id", "510", "11", "51011", "XL Axiata", "in", "ID"));
        list.add(new CountryPreset("TH_AIS", "🇹🇭", "Thailand", "th", "520", "01", "52001", "AIS", "th", "TH"));
        list.add(new CountryPreset("TH_TRUEMOVE", "🇹🇭", "Thailand", "th", "520", "00", "52000", "TrueMove", "th", "TH"));
        list.add(new CountryPreset("PH_GLOBE", "🇵🇭", "Philippines", "ph", "515", "02", "51502", "Globe", "en", "PH"));
        list.add(new CountryPreset("PH_SMART", "🇵🇭", "Philippines", "ph", "515", "03", "51503", "Smart", "en", "PH"));
        list.add(new CountryPreset("VN_VIETTEL", "🇻🇳", "Vietnam", "vn", "452", "04", "45204", "Viettel", "vi", "VN"));
        list.add(new CountryPreset("VN_VINAPHONE", "🇻🇳", "Vietnam", "vn", "452", "02", "45202", "Vinaphone", "vi", "VN"));

        list.add(new CountryPreset("JP_DOCOMO", "🇯🇵", "Japan", "jp", "440", "10", "44010", "NTT DOCOMO", "ja", "JP"));
        list.add(new CountryPreset("JP_SOFTBANK", "🇯🇵", "Japan", "jp", "440", "20", "44020", "SoftBank", "ja", "JP"));
        list.add(new CountryPreset("JP_KDDI", "🇯🇵", "Japan", "jp", "440", "50", "44050", "au (KDDI)", "ja", "JP"));
        list.add(new CountryPreset("KR_SKT", "🇰🇷", "South Korea", "kr", "450", "05", "45005", "SK Telecom", "ko", "KR"));
        list.add(new CountryPreset("KR_KT", "🇰🇷", "South Korea", "kr", "450", "08", "45008", "KT", "ko", "KR"));
        list.add(new CountryPreset("KR_LGU", "🇰🇷", "South Korea", "kr", "450", "06", "45006", "LG U+", "ko", "KR"));
        list.add(new CountryPreset("TW_CHUNGHWA", "🇹🇼", "Taiwan", "tw", "466", "92", "46692", "Chunghwa", "zh", "TW"));
        list.add(new CountryPreset("TW_TAIWANMO", "🇹🇼", "Taiwan", "tw", "466", "97", "46697", "Taiwan Mobile", "zh", "TW"));

        list.add(new CountryPreset("PK_JAZZ", "🇵🇰", "Pakistan", "pk", "410", "01", "41001", "Jazz", "ur", "PK"));
        list.add(new CountryPreset("BD_GRAMEEN", "🇧🇩", "Bangladesh", "bd", "470", "01", "47001", "Grameenphone", "bn", "BD"));

        list.add(new CountryPreset("GB_EE", "🇬🇧", "United Kingdom", "gb", "234", "30", "23430", "EE", "en", "GB"));
        list.add(new CountryPreset("GB_VODAFONE", "🇬🇧", "United Kingdom", "gb", "234", "15", "23415", "Vodafone", "en", "GB"));
        list.add(new CountryPreset("GB_O2", "🇬🇧", "United Kingdom", "gb", "234", "10", "23410", "O2", "en", "GB"));
        list.add(new CountryPreset("DE_TELEKOM", "🇩🇪", "Germany", "de", "262", "01", "26201", "Telekom", "de", "DE"));
        list.add(new CountryPreset("DE_VODAFONE", "🇩🇪", "Germany", "de", "262", "02", "26202", "Vodafone", "de", "DE"));
        list.add(new CountryPreset("FR_ORANGE", "🇫🇷", "France", "fr", "208", "01", "20801", "Orange", "fr", "FR"));
        list.add(new CountryPreset("FR_SFR", "🇫🇷", "France", "fr", "208", "10", "20810", "SFR", "fr", "FR"));
        list.add(new CountryPreset("NL_KPN", "🇳🇱", "Netherlands", "nl", "204", "08", "20408", "KPN", "nl", "NL"));
        list.add(new CountryPreset("NL_VODAFONE", "🇳🇱", "Netherlands", "nl", "204", "04", "20404", "Vodafone", "nl", "NL"));
        list.add(new CountryPreset("BE_PROXIMUS", "🇧🇪", "Belgium", "be", "206", "01", "20601", "Proximus", "nl", "BE"));
        list.add(new CountryPreset("CH_SWISSCOM", "🇨🇭", "Switzerland", "ch", "228", "01", "22801", "Swisscom", "de", "CH"));
        list.add(new CountryPreset("AT_A1", "🇦🇹", "Austria", "at", "232", "01", "23201", "A1", "de", "AT"));
        list.add(new CountryPreset("IE_VODAFONE", "🇮🇪", "Ireland", "ie", "272", "01", "27201", "Vodafone", "en", "IE"));
        list.add(new CountryPreset("SE_TELIA", "🇸🇪", "Sweden", "se", "240", "01", "24001", "Telia", "sv", "SE"));
        list.add(new CountryPreset("NO_TELENOR", "🇳🇴", "Norway", "no", "242", "01", "24201", "Telenor", "no", "NO"));
        list.add(new CountryPreset("DK_TDC", "🇩🇰", "Denmark", "dk", "238", "01", "23801", "TDC", "da", "DK"));
        list.add(new CountryPreset("FI_ELISA", "🇫🇮", "Finland", "fi", "244", "05", "24405", "Elisa", "fi", "FI"));

        list.add(new CountryPreset("IT_TIM", "🇮🇹", "Italy", "it", "222", "01", "22201", "TIM", "it", "IT"));
        list.add(new CountryPreset("IT_VODAFONE", "🇮🇹", "Italy", "it", "222", "10", "22210", "Vodafone", "it", "IT"));
        list.add(new CountryPreset("ES_MOVISTAR", "🇪🇸", "Spain", "es", "214", "07", "21407", "Movistar", "es", "ES"));
        list.add(new CountryPreset("ES_VODAFONE", "🇪🇸", "Spain", "es", "214", "01", "21401", "Vodafone", "es", "ES"));
        list.add(new CountryPreset("PT_MEO", "🇵🇹", "Portugal", "pt", "268", "06", "26806", "MEO", "pt", "PT"));
        list.add(new CountryPreset("GR_COSMOTE", "🇬🇷", "Greece", "gr", "202", "01", "20201", "Cosmote", "el", "GR"));
        list.add(new CountryPreset("PL_ORANGE", "🇵🇱", "Poland", "pl", "260", "03", "26003", "Orange", "pl", "PL"));
        list.add(new CountryPreset("PL_PLAY", "🇵🇱", "Poland", "pl", "260", "06", "26006", "Play", "pl", "PL"));
        list.add(new CountryPreset("CZ_TMOBILE", "🇨🇿", "Czech Republic", "cz", "230", "01", "23001", "T-Mobile", "cs", "CZ"));
        list.add(new CountryPreset("RO_ORANGE", "🇷🇴", "Romania", "ro", "226", "10", "22610", "Orange", "ro", "RO"));
        list.add(new CountryPreset("HU_TELEKOM", "🇭🇺", "Hungary", "hu", "216", "30", "21630", "Telekom", "hu", "HU"));
        list.add(new CountryPreset("UA_KYIVSTAR", "🇺🇦", "Ukraine", "ua", "255", "03", "25503", "Kyivstar", "uk", "UA"));

        list.add(new CountryPreset("AE_ETISALAT", "🇦🇪", "United Arab Emirates", "ae", "424", "02", "42402", "Etisalat", "ar", "AE"));
        list.add(new CountryPreset("AE_DU", "🇦🇪", "United Arab Emirates", "ae", "424", "03", "42403", "du", "ar", "AE"));
        list.add(new CountryPreset("SA_STC", "🇸🇦", "Saudi Arabia", "sa", "420", "01", "42001", "STC", "ar", "SA"));
        list.add(new CountryPreset("SA_MOBILY", "🇸🇦", "Saudi Arabia", "sa", "420", "03", "42003", "Mobily", "ar", "SA"));
        list.add(new CountryPreset("TR_TURKCELL", "🇹🇷", "Turkey", "tr", "286", "01", "28601", "Turkcell", "tr", "TR"));
        list.add(new CountryPreset("TR_VODAFONE", "🇹🇷", "Turkey", "tr", "286", "02", "28602", "Vodafone", "tr", "TR"));
        list.add(new CountryPreset("IL_CELLCOM", "🇮🇱", "Israel", "il", "425", "02", "42502", "Cellcom", "he", "IL"));
        list.add(new CountryPreset("QA_OOREDOO", "🇶🇦", "Qatar", "qa", "427", "01", "42701", "Ooredoo", "ar", "QA"));
        list.add(new CountryPreset("KW_ZAIN", "🇰🇼", "Kuwait", "kw", "419", "02", "41902", "Zain", "ar", "KW"));
        list.add(new CountryPreset("EG_VODAFONE", "🇪🇬", "Egypt", "eg", "602", "02", "60202", "Vodafone", "ar", "EG"));
        list.add(new CountryPreset("EG_ORANGE", "🇪🇬", "Egypt", "eg", "602", "01", "60201", "Orange", "ar", "EG"));
        list.add(new CountryPreset("MA_MAROC", "🇲🇦", "Morocco", "ma", "604", "01", "60401", "Maroc Telecom", "ar", "MA"));

        list.add(new CountryPreset("BR_VIVO", "🇧🇷", "Brazil", "br", "724", "06", "72406", "Vivo", "pt", "BR"));
        list.add(new CountryPreset("BR_CLARO", "🇧🇷", "Brazil", "br", "724", "05", "72405", "Claro", "pt", "BR"));
        list.add(new CountryPreset("AR_CLARO", "🇦🇷", "Argentina", "ar", "722", "310", "722310", "Claro", "es", "AR"));
        list.add(new CountryPreset("CO_CLARO", "🇨🇴", "Colombia", "co", "732", "101", "732101", "Claro", "es", "CO"));
        list.add(new CountryPreset("CL_ENTEL", "🇨🇱", "Chile", "cl", "730", "01", "73001", "Entel", "es", "CL"));
        list.add(new CountryPreset("PE_CLARO", "🇵🇪", "Peru", "pe", "716", "10", "71610", "Claro", "es", "PE"));

        list.add(new CountryPreset("AU_TELSTRA", "🇦🇺", "Australia", "au", "505", "01", "50501", "Telstra", "en", "AU"));
        list.add(new CountryPreset("AU_OPTUS", "🇦🇺", "Australia", "au", "505", "02", "50502", "Optus", "en", "AU"));
        list.add(new CountryPreset("NZ_SPARK", "🇳🇿", "New Zealand", "nz", "530", "05", "53005", "Spark", "en", "NZ"));
        list.add(new CountryPreset("ZA_VODACOM", "🇿🇦", "South Africa", "za", "655", "01", "65501", "Vodacom", "en", "ZA"));
        list.add(new CountryPreset("ZA_MTN", "🇿🇦", "South Africa", "za", "655", "10", "65510", "MTN", "en", "ZA"));
        list.add(new CountryPreset("NG_MTN", "🇳🇬", "Nigeria", "ng", "621", "30", "62130", "MTN", "en", "NG"));
        list.add(new CountryPreset("KE_SAFARICOM", "🇰🇪", "Kenya", "ke", "639", "02", "63902", "Safaricom", "en", "KE"));

        PRESETS = Collections.unmodifiableList(list);
    }

    public static CountryPreset getDefault() {
        return PRESETS.get(0);
    }

    public static CountryPreset getById(String id) {
        if (id != null) {
            for (CountryPreset p : PRESETS) {
                if (p.getId().equalsIgnoreCase(id)) {
                    return p;
                }
            }
        }
        return getDefault();
    }

    public static CountryPreset findByIso(String iso) {
        if (iso != null) {
            for (CountryPreset p : PRESETS) {
                if (p.getCountryIso().equalsIgnoreCase(iso.trim())) {
                    return p;
                }
            }
        }
        return null;
    }

    public static List<CountryPreset> filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            return PRESETS;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<CountryPreset> filtered = new ArrayList<>();
        for (CountryPreset p : PRESETS) {
            if (p.getCountryName().toLowerCase(Locale.ROOT).contains(q)
                    || p.getCountryIso().contains(q)
                    || p.getOperatorName().toLowerCase(Locale.ROOT).contains(q)
                    || p.getOperatorMccMnc().contains(q)) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    public static class CountryItem implements Comparable<CountryItem> {
        private final String iso;
        private final String name;
        private final String flag;

        public CountryItem(String iso, String name, String flag) {
            this.iso = iso != null ? iso.trim().toLowerCase(Locale.ROOT) : "";
            this.name = name != null ? name.trim() : "";
            this.flag = flag != null ? flag.trim() : "";
        }

        public String getIso() { return iso; }
        public String getName() { return name; }
        public String getFlag() { return flag; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CountryItem)) return false;
            CountryItem that = (CountryItem) o;
            return iso.equalsIgnoreCase(that.iso);
        }

        @Override
        public int hashCode() {
            return iso.toLowerCase(Locale.ROOT).hashCode();
        }

        @Override
        public int compareTo(CountryItem o) {
            return this.name.compareToIgnoreCase(o.name);
        }
    }

    public static String isoToFlagEmoji(String countryIso) {
        if (countryIso == null || countryIso.trim().length() != 2) return "🌐";
        String code = countryIso.trim().toUpperCase(Locale.ROOT);
        int firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6;
        int secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6;
        if (!Character.isBmpCodePoint(firstChar) || !Character.isBmpCodePoint(secondChar)) {
            return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
        }
        return "🌐";
    }

    public static CountryItem createCountryItem(String iso) {
        if (iso == null || iso.trim().length() != 2) return null;
        String cleanIso = iso.trim().toLowerCase(Locale.ROOT);

        CountryPreset preset = findByIso(cleanIso);
        if (preset != null) {
            return new CountryItem(cleanIso, preset.getCountryName(), preset.getFlag());
        }

        String name = new Locale("", cleanIso.toUpperCase(Locale.ROOT)).getDisplayCountry(Locale.ENGLISH);
        if (name.isEmpty()) {
            name = cleanIso.toUpperCase(Locale.ROOT);
        }
        String flag = isoToFlagEmoji(cleanIso);
        return new CountryItem(cleanIso, name, flag);
    }

    public static List<CountryItem> getUniqueCountries() {
        java.util.Map<String, CountryItem> uniqueMap = new java.util.LinkedHashMap<>();

        for (CountryPreset p : PRESETS) {
            String iso = p.getCountryIso().toLowerCase(Locale.ROOT);
            if (!uniqueMap.containsKey(iso)) {
                uniqueMap.put(iso, new CountryItem(iso, p.getCountryName(), p.getFlag()));
            }
        }

        String[][] additional = {
                {"in", "India", "🇮🇳"},
                {"hk", "Hong Kong", "🇭🇰"},
                {"ru", "Russia", "🇷🇺"},
                {"cn", "China", "🇨🇳"},
                {"ir", "Iran", "🇮🇷"},
                {"by", "Belarus", "🇧🇾"},
                {"kz", "Kazakhstan", "🇰🇿"},
                {"uz", "Uzbekistan", "🇺🇿"}
        };
        for (String[] item : additional) {
            String iso = item[0].toLowerCase(Locale.ROOT);
            if (!uniqueMap.containsKey(iso)) {
                uniqueMap.put(iso, new CountryItem(iso, item[1], item[2]));
            }
        }

        List<CountryItem> result = new ArrayList<>(uniqueMap.values());
        Collections.sort(result);
        return result;
    }

    public static List<CountryItem> filterCountries(List<CountryItem> list, String query) {
        if (query == null || query.trim().isEmpty()) {
            return list;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<CountryItem> filtered = new ArrayList<>();
        for (CountryItem item : list) {
            if (item.getName().toLowerCase(Locale.ROOT).contains(q)
                    || item.getIso().contains(q)) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}

