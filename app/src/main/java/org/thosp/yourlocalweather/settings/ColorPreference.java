package org.thosp.yourlocalweather.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.thosp.yourlocalweather.R;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ColorPreference extends Preference {

    private int mValue = Color.BLACK;
    private boolean mSupportsAlpha = false;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWidgetLayoutResource(R.layout.pref_color_widget);
        mSupportsAlpha = true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ImageView colorView = (ImageView) holder.findViewById(R.id.color_preview);

        if (colorView != null) {
            android.graphics.drawable.Drawable background = colorView.getBackground();

            if (background instanceof GradientDrawable) {
                GradientDrawable shape = (GradientDrawable) background.mutate();
                shape.setColor(mValue);
            }
        }
    }

    @Override
    protected void onClick() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), mValue, mSupportsAlpha, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                if (callChangeListener(color)) {
                    mValue = color;
                    persistInt(mValue);
                    notifyChanged();
                }
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }
        });
        dialog.show();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if (a.peekValue(index) != null && a.peekValue(index).type == android.util.TypedValue.TYPE_STRING) {
            return Color.parseColor(a.getString(index));
        }
        return a.getInteger(index, Color.BLACK);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue != null) {
            mValue = getPersistedInt((Integer) defaultValue);
        } else {
            mValue = getPersistedInt(Color.BLACK);
        }
    }
}
