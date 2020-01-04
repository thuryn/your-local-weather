package org.thosp.yourlocalweather.model;

import android.location.Address;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.Locale;

public class VoiceSettingParameter implements Parcelable {

    private long id;
    private long voiceSettingId;
    private int paramTypeId;
    private Boolean paramBooleanValue;
    private Long paramLongValue;
    private String paramStringValue;

    public VoiceSettingParameter(long id,
                                 long voiceSettingId,
                                 int paramTypeId,
                                 Boolean paramBooleanValue,
                                 Long paramLongValue,
                                 String paramStringValue) {
        this.id = id;
        this.voiceSettingId = voiceSettingId;
        this.paramTypeId = paramTypeId;
        this.paramBooleanValue = paramBooleanValue;
        this.paramLongValue =paramLongValue;
        this.paramStringValue = paramStringValue;
    }

    public Long getId() {
        return id;
    }

    public long getVoiceSettingId() {
        return voiceSettingId;
    }

    public void setVoiceSettingId(long voiceSettingId) {
        this.voiceSettingId = voiceSettingId;
    }

    public int getParamTypeId() {
        return paramTypeId;
    }

    public void setParamTypeId(int paramTypeId) {
        this.paramTypeId = paramTypeId;
    }

    public Boolean getParamBooleanValue() {
        return paramBooleanValue;
    }

    public void setParamBooleanValue(Boolean paramBooleanValue) {
        this.paramBooleanValue = paramBooleanValue;
    }

    public Long getParamLongValue() {
        return paramLongValue;
    }

    public void setParamLongValue(Long paramLongValue) {
        this.paramLongValue = paramLongValue;
    }

    public String getParamStringValue() {
        return paramStringValue;
    }

    public void setParamStringValue(String paramStringValue) {
        this.paramStringValue = paramStringValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(voiceSettingId);
        parcel.writeInt(paramTypeId);
        parcel.writeInt(mapBooleanToInt(paramBooleanValue));
        parcel.writeLong(paramLongValue);
        parcel.writeString(paramStringValue);
    }

    public static final Creator<VoiceSettingParameter> CREATOR
            = new Creator<VoiceSettingParameter>() {
        public VoiceSettingParameter createFromParcel(Parcel in) {
            return new VoiceSettingParameter(in);
        }

        public VoiceSettingParameter[] newArray(int size) {
            return new VoiceSettingParameter[size];
        }
    };

    private VoiceSettingParameter(Parcel in) {
        id = in.readLong();
        voiceSettingId = in.readLong();
        paramTypeId = in.readInt();
        paramBooleanValue = mapIntToBoolean(in.readInt());
        paramLongValue = in.readLong();
        paramStringValue = in.readString();
    }

    public VoiceSettingParameter(PersistableBundle persistentBundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            id = persistentBundle.getLong("id");
            voiceSettingId = persistentBundle.getLong("voiceSettingId");
            paramTypeId = persistentBundle.getInt("paramTypeId");
            paramBooleanValue = mapIntToBoolean(persistentBundle.getInt("paramBooleanValue"));
            paramLongValue = persistentBundle.getLong("paramLongValue");
            paramStringValue = persistentBundle.getString("paramStringValue");
        }
    }

    public PersistableBundle getPersistableBundle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putLong("id", id);
            persistableBundle.putLong("voiceSettingId", voiceSettingId);
            persistableBundle.putInt("paramTypeId", paramTypeId);
            persistableBundle.putInt("paramBooleanValue", mapBooleanToInt(paramBooleanValue));
            persistableBundle.putLong("paramLongValue", paramLongValue);
            persistableBundle.putString("paramStringValue", paramStringValue);
            return persistableBundle;
        } else {
            return null;
        }
    }

    private int mapBooleanToInt(Boolean booleanValue) {
        if (booleanValue == null) {
            return 0;
        } else if (booleanValue) {
            return 1;
        } else {
            return 2;
        }
    }

    private Boolean mapIntToBoolean(int intValue) {
        if (intValue == 0) {
            return null;
        } else if (intValue == 1) {
            return true;
        } else {
            return false;
        }
    }
}
