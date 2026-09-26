package com.ash.tiktokregion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LanguageModel implements Serializable {
    private final String mCode;
    private final String mName;
    private final String mNativeName;

    public LanguageModel(String code, String name, String nativeName) {
        this.mCode = code != null ? code.trim().toLowerCase(Locale.ROOT) : "";
        this.mName = name;
        this.mNativeName = nativeName;
    }

    public String getCode() {
        return mCode;
    }

    public String getName() {
        return mName;
    }

    public String getNativeName() {
        return mNativeName;
    }

    public String getDisplayName() {
        if (mNativeName != null && !mNativeName.equalsIgnoreCase(mName)) {
            return mName + " (" + mNativeName + ")";
        }
        return mName;
    }

    private static final List<LanguageModel> ALL_LANGUAGES = new ArrayList<>();

    static {
        ALL_LANGUAGES.add(new LanguageModel("en", "English", "English"));
        ALL_LANGUAGES.add(new LanguageModel("id", "Indonesian", "Bahasa Indonesia"));
        ALL_LANGUAGES.add(new LanguageModel("ms", "Malay", "Bahasa Melayu"));
        ALL_LANGUAGES.add(new LanguageModel("ja", "Japanese", "日本語"));
        ALL_LANGUAGES.add(new LanguageModel("ko", "Korean", "한국어"));
        ALL_LANGUAGES.add(new LanguageModel("es", "Spanish", "Español"));
        ALL_LANGUAGES.add(new LanguageModel("pt", "Portuguese", "Português"));
        ALL_LANGUAGES.add(new LanguageModel("fr", "French", "Français"));
        ALL_LANGUAGES.add(new LanguageModel("de", "German", "Deutsch"));
        ALL_LANGUAGES.add(new LanguageModel("it", "Italian", "Italiano"));
        ALL_LANGUAGES.add(new LanguageModel("ru", "Russian", "Русский"));
        ALL_LANGUAGES.add(new LanguageModel("ar", "Arabic", "العربية"));
        ALL_LANGUAGES.add(new LanguageModel("tr", "Turkish", "Türkçe"));
        ALL_LANGUAGES.add(new LanguageModel("vi", "Vietnamese", "Tiếng Việt"));
        ALL_LANGUAGES.add(new LanguageModel("th", "Thai", "ไทย"));
        ALL_LANGUAGES.add(new LanguageModel("tl", "Filipino / Tagalog", "Filipino"));
        ALL_LANGUAGES.add(new LanguageModel("hi", "Hindi", "हिन्दी"));
        ALL_LANGUAGES.add(new LanguageModel("bn", "Bengali", "বাংলা"));
        ALL_LANGUAGES.add(new LanguageModel("ur", "Urdu", "اردو"));
        ALL_LANGUAGES.add(new LanguageModel("zh", "Chinese", "中文"));
        ALL_LANGUAGES.add(new LanguageModel("zh-Hant", "Traditional Chinese", "繁體中文"));
        ALL_LANGUAGES.add(new LanguageModel("nl", "Dutch", "Nederlands"));
        ALL_LANGUAGES.add(new LanguageModel("pl", "Polish", "Polski"));
        ALL_LANGUAGES.add(new LanguageModel("uk", "Ukrainian", "Українська"));
        ALL_LANGUAGES.add(new LanguageModel("sv", "Swedish", "Svenska"));
        ALL_LANGUAGES.add(new LanguageModel("cs", "Czech", "Čeština"));
        ALL_LANGUAGES.add(new LanguageModel("el", "Greek", "Ελληνικά"));
        ALL_LANGUAGES.add(new LanguageModel("ro", "Romanian", "Română"));
        ALL_LANGUAGES.add(new LanguageModel("hu", "Hungarian", "Magyar"));
        ALL_LANGUAGES.add(new LanguageModel("da", "Danish", "Dansk"));
        ALL_LANGUAGES.add(new LanguageModel("fi", "Finnish", "Suomi"));
        ALL_LANGUAGES.add(new LanguageModel("no", "Norwegian", "Norsk"));
        ALL_LANGUAGES.add(new LanguageModel("he", "Hebrew", "עברית"));
        ALL_LANGUAGES.add(new LanguageModel("fa", "Persian", "فارسی"));
        ALL_LANGUAGES.add(new LanguageModel("kk", "Kazakh", "Қазақ тілі"));
    }

    public static List<LanguageModel> getAll() {
        return Collections.unmodifiableList(ALL_LANGUAGES);
    }

    public static LanguageModel findByCode(String code) {
        if (code == null) return null;
        String clean = code.trim().toLowerCase(Locale.ROOT);
        for (LanguageModel lang : ALL_LANGUAGES) {
            if (lang.getCode().equalsIgnoreCase(clean)) {
                return lang;
            }
        }
        return null;
    }
}
