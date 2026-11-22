package astralis.setting;

public interface SettingListener<T> {
    void onChange(T previousValue, T newValue);
}
