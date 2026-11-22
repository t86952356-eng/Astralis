package astralis.setting;

import astralis.module.Module;

import imgui.type.ImString;

import java.util.List;

public class TextSetting extends Setting<String> {
    private final ImString inputBuffer;

    public TextSetting(String id, Module module2, String initialValue) {
        super(id, module2, initialValue);
        String initialText = initialValue == null ? "" : initialValue;

        this.inputBuffer = new ImString(initialText, 512);
    }

    public TextSetting(String id, String initialValue) {
        super(id, initialValue);
        String initialText = initialValue == null ? "" : initialValue;

        this.inputBuffer = new ImString(initialText, 512);
    }

    public ImString getInputBuffer() {
        return this.inputBuffer;
    }

    @Override
    public void setValue(String newValue) {
        super.setValue(newValue);
        this.syncInputBuffer();
    }

    @Override
    public void setValueObject(Object newValue) {
        super.setValueObject(newValue);
        this.syncInputBuffer();
    }

    @Override
    public void setValueRaw(String newValue) {
        super.setValueRaw(newValue);
        this.syncInputBuffer();
    }

    /**
     * Keeps the ImGui input buffer showing the current value, including values loaded from a
     * config. The buffer is still null while the superclass constructor sets the initial value.
     */
    private void syncInputBuffer() {
        Object value = this.getValue();
        String text = value == null ? "" : value.toString();
        if (this.inputBuffer != null && !this.inputBuffer.get().equals(text)) {
            this.inputBuffer.set(text);
        }
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }
}
