package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.PlaySoundEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import pub.astralis.client.module.core.Category;

public class Sounds extends Module {
    private static RandomSource random;
    private long lastHurtSoundTime;
    private final BooleanSetting attackKnockback;
    private final BooleanSetting attackNoDamage;
    private final BooleanSetting attackStrong;
    private final BooleanSetting attackSweep;
    private final BooleanSetting attackWeak;
    private final BooleanSetting customHurt;
    private final NumberSetting volume;
    private final ModeSetting hurtSound;

    public Sounds() {

        super(MODULE_ID, "", "", Category.Misc);

        this.attackKnockback = new BooleanSetting(ATTACK_KNOCKBACK_SETTING_ID, false);

        this.attackNoDamage = new BooleanSetting(ATTACK_NO_DAMAGE_SETTING_ID, true);

        this.attackStrong = new BooleanSetting(ATTACK_STRONG_SETTING_ID, false);

        this.attackSweep = new BooleanSetting(ATTACK_SWEEP_SETTING_ID, true);

        this.attackWeak = new BooleanSetting(ATTACK_WEAK_SETTING_ID, true);

        this.customHurt = new BooleanSetting(CUSTOM_HURT_SETTING_ID, false);

        this.volume = new NumberSetting(VOLUME_SETTING_ID, 50.0F, 0.0F, 100.0F, 1.0F);

        ModeSetting modeSetting = new ModeSetting(HURT_SOUND_SETTING_ID);
        String[] hurtSoundModes = new String[4];

        hurtSoundModes[0] = "Hit";

        hurtSoundModes[1] = "Hit 2";

        hurtSoundModes[2] = "Never Lose";

        hurtSoundModes[3] = "Skeet";
        ModeSetting modeSetting2 = modeSetting.addModes(hurtSoundModes);
        this.hurtSound = modeSetting2.setDefault("Hit");
        SettingGroup settingGroup =
                new SettingGroup(REMOVALS_GROUP_ID)
                        .addValues(
                                this.attackKnockback,
                                this.attackNoDamage,
                                this.attackStrong,
                                this.attackSweep,
                                this.attackWeak);
        SettingGroup settingGroup2 =
                new SettingGroup(HURT_SOUNDS_GROUP_ID).addValues(this.hurtSound, this.volume);
        settingGroup2.hideIf(
                () -> {
                    return !this.customHurt.getValue();
                });
        this.addSettings(settingGroup, this.customHurt, settingGroup2);
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (this.customHurt.getValue() && attackEvent.getStage() == EventStage.POST) {
            this.playHurtSound();
        }
    }

    private void playHurtSound() {

        long now = System.currentTimeMillis();
        if (now - this.lastHurtSoundTime >= 100L) {
            this.lastHurtSoundTime = now;

            Identifier identifier =
                    Identifier.fromNamespaceAndPath("astralis-recode", this.getHurtSoundPath());
            if (mc.getSoundManager().getSoundEvent(identifier) != null) {
                float soundVolume = this.volume.getValue() / 100.0F;
                mc.getSoundManager()
                        .play(
                                new SimpleSoundInstance(
                                        identifier,
                                        SoundSource.MASTER,
                                        soundVolume,
                                        1.0F,
                                        random,
                                        false,
                                        0,
                                        Attenuation.NONE,
                                        0.0,
                                        0.0,
                                        0.0,
                                        true));
            }
        }
    }

    private String getHurtSoundPath() {
        String soundName = this.hurtSound.getValue().getName();
        String path;
        switch (soundName) {
            case "Hit":
                path = "hit";
                break;
            case "Hit 2":
                path = "hit2";
                break;
            case "Never Lose":
                path = "neverlose";
                break;
            case "Skeet":
                path = "skeet";
                break;
            default:
                String unexpectedValue = String.valueOf(this.hurtSound.getValue());
                throw new IllegalStateException("Unexpected value: " + unexpectedValue);
        }

        return path;
    }

    @EventTarget
    public void onPlaySound(PlaySoundEvent event) {
        String path = event.getSound().getIdentifier().getPath();
        switch (path) {
            case "entity.player.hurt":
                event.setCancelled(this.customHurt.getValue());
                break;
            case "entity.player.attack.knockback":
                event.setCancelled(this.attackKnockback.getValue());
                break;
            case "entity.player.attack.nodamage":
                event.setCancelled(this.attackNoDamage.getValue());
                break;
            case "entity.player.attack.strong":
                event.setCancelled(this.attackStrong.getValue());
                break;
            case "entity.player.attack.sweep":
                event.setCancelled(this.attackSweep.getValue());
                break;
            case "entity.player.attack.weak":
                event.setCancelled(this.attackWeak.getValue());
        }
    }

    static {
        random = RandomSource.create();
    }

    private static final String MODULE_ID = "6c28a0629689811ac1ea88a722efda7d";
    private static final String ATTACK_KNOCKBACK_SETTING_ID = "485d7a700657a8d9b51cc98f24d7f470";
    private static final String ATTACK_NO_DAMAGE_SETTING_ID = "424492e942d526ebb28901554286a6e6";
    private static final String ATTACK_STRONG_SETTING_ID = "69dd03d394841057ac94414fbea2bd50";
    private static final String ATTACK_SWEEP_SETTING_ID = "cee53a933cf811db6de441c368c8c51a";
    private static final String ATTACK_WEAK_SETTING_ID = "551ac07308607cb80d6d75a02381fc27";
    private static final String CUSTOM_HURT_SETTING_ID = "053b7836d14ecc948b5f3e0e2f4301e1";
    private static final String VOLUME_SETTING_ID = "c77d7df007e94e0e53c6453618bf8a04";
    private static final String HURT_SOUND_SETTING_ID = "5cdfaaab4ddd9d11de8102ddaa23d02a";
    private static final String REMOVALS_GROUP_ID = "4b80150529ed698c128b0f1ad255698e";
    private static final String HURT_SOUNDS_GROUP_ID = "0a621acb6ead9d66b382377dd7db58fc";
}
