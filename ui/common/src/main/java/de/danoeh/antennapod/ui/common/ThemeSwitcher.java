package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.StyleRes;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class ThemeSwitcher {
    @StyleRes
    public static int getNoTitleTheme(Context context) {
        boolean dynamic = UserPreferences.getIsThemeColorTinted();
        UserPreferences.ThemePalette palette = UserPreferences.getThemePalette();
        switch (readThemeValue(context)) {
            case DARK:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_Dark_NoTitle;
                }
                return getDarkNoTitleTheme(palette);
            case BLACK:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_TrueBlack_NoTitle;
                }
                return getTrueBlackNoTitleTheme(palette);
            case LIGHT: // fall-through
            default:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_Light_NoTitle;
                }
                return getLightNoTitleTheme(palette);
        }
    }

    @StyleRes
    public static int getTranslucentTheme(Context context) {
        boolean dynamic = UserPreferences.getIsThemeColorTinted();
        UserPreferences.ThemePalette palette = UserPreferences.getThemePalette();
        switch (readThemeValue(context)) {
            case DARK:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_Dark_Translucent;
                }
                return getDarkTranslucentTheme(palette);
            case BLACK:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_TrueBlack_Translucent;
                }
                return getTrueBlackTranslucentTheme(palette);
            case LIGHT: // fall-through
            default:
                if (dynamic) {
                    return R.style.Theme_AntennaPod_Dynamic_Light_Translucent;
                }
                return getLightTranslucentTheme(palette);
        }
    }

    @StyleRes
    private static int getLightNoTitleTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_Light_NoTitle;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_Light_NoTitle;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_Light_NoTitle;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_Light_NoTitle;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_Light_NoTitle;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_Light_NoTitle;
        }
    }

    @StyleRes
    private static int getDarkNoTitleTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_Dark_NoTitle;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_Dark_NoTitle;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_Dark_NoTitle;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_Dark_NoTitle;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_Dark_NoTitle;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_Dark_NoTitle;
        }
    }

    @StyleRes
    private static int getTrueBlackNoTitleTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_TrueBlack_NoTitle;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_TrueBlack_NoTitle;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_TrueBlack_NoTitle;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_TrueBlack_NoTitle;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_TrueBlack_NoTitle;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_TrueBlack_NoTitle;
        }
    }

    @StyleRes
    private static int getLightTranslucentTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_Light_Translucent;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_Light_Translucent;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_Light_Translucent;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_Light_Translucent;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_Light_Translucent;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_Light_Translucent;
        }
    }

    @StyleRes
    private static int getDarkTranslucentTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_Dark_Translucent;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_Dark_Translucent;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_Dark_Translucent;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_Dark_Translucent;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_Dark_Translucent;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_Dark_Translucent;
        }
    }

    @StyleRes
    private static int getTrueBlackTranslucentTheme(UserPreferences.ThemePalette palette) {
        switch (palette) {
            case CORAL:
                return R.style.Theme_AntennaPod_Coral_TrueBlack_Translucent;
            case EMERALD:
                return R.style.Theme_AntennaPod_Emerald_TrueBlack_Translucent;
            case CYAN:
                return R.style.Theme_AntennaPod_Cyan_TrueBlack_Translucent;
            case AMBER:
                return R.style.Theme_AntennaPod_Amber_TrueBlack_Translucent;
            case PURPLE:
                return R.style.Theme_AntennaPod_Purple_TrueBlack_Translucent;
            case INDIGO:
            default:
                return R.style.Theme_AntennaPod_TrueBlack_Translucent;
        }
    }

    private static UserPreferences.ThemePreference readThemeValue(Context context) {
        UserPreferences.ThemePreference theme = UserPreferences.getTheme();
        if (theme == UserPreferences.ThemePreference.SYSTEM) {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                theme = UserPreferences.ThemePreference.DARK;
            } else {
                theme = UserPreferences.ThemePreference.LIGHT;

            }
        }
        if (theme == UserPreferences.ThemePreference.DARK && UserPreferences.getIsBlackTheme()) {
            theme = UserPreferences.ThemePreference.BLACK;
        }
        return theme;
    }
}
