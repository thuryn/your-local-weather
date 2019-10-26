package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class VoiceSettingParameterContract {

    private VoiceSettingParameterContract() {}

    protected static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME + " (" +
                    VoiceSettingParameterContract.VoiceSettingParameters._ID + " INTEGER PRIMARY KEY," +
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + " integer," +
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + " integer," +
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE + " integer," +
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE + " text)";

    protected static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME;

    public static class VoiceSettingParameters implements BaseColumns {
        public static final String TABLE_NAME = "voice_setting_parameters";
        public static final String COLUMN_NAME_VOICE_SETTING_ID = "voiceSettingId";
        public static final String COLUMN_NAME_PARAM_TYPE_ID = "paramTypeId";
        public static final String COLUMN_NAME_PARAM_LONG_VALUE = "paramLongValue";
        public static final String COLUMN_NAME_PARAM_STRING_VALUE = "paramStringValue";
    }
}
