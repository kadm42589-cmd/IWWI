package info.iwwi.addon.modules;

import info.iwwi.addon.IWWIAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

public class AutoTPAModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> nick = sgGeneral.add(new StringSetting.Builder()
        .name("nick")
        .description("Player nickname for TPA/TPAHere.")
        .defaultValue("")
        .build()
    );

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay-seconds")
        .description("Delay between commands (seconds).")
        .defaultValue(0.1)
        .min(0.05)
        .sliderMin(0.05)
        .sliderMax(2)
        .build()
    );

    private final Setting<CommandType> commandType = sgGeneral.add(new EnumSetting.Builder<CommandType>()
        .name("command-type")
        .description("Command type: /tpa or /tpahere.")
        .defaultValue(CommandType.TPAHERE)
        .build()
    );

    private int sentCount = 0;
    private long lastSendTime = 0;
    private boolean inPause = false;
    private long pauseStartTime = 0;
    private static final int MESSAGES_PER_CYCLE = 15;
    private static final long PAUSE_MIN_MS = 2000;
    private static final long PAUSE_MAX_MS = 6000;
    private long pauseDuration = 0;

    public AutoTPAModule() {
        super(IWWIAddon.CATEGORY, "AutoTPA/TPAHere", "Spams /tpa or /tpahere to the selected player.");
    }

    @Override
    public void onActivate() {
        sentCount = 0;
        lastSendTime = 0;
        inPause = false;
        pauseStartTime = 0;
        pauseDuration = 0;
    }

    @Override
    public void onDeactivate() {
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        if (inPause) {
            if (now - pauseStartTime >= pauseDuration) {
                sentCount = 0;
                inPause = false;
                lastSendTime = now;
            }
            return;
        }

        if (sentCount < MESSAGES_PER_CYCLE) {
            if (now - lastSendTime >= (long) (delay.get() * 1000)) {
                String cmd = commandType.get().getCommand() + " " + nick.get();
                ChatUtils.sendPlayerMsg(cmd);
                sentCount++;
                lastSendTime = now;
            }
        } else {
            inPause = true;
            pauseStartTime = now;
            pauseDuration = PAUSE_MIN_MS + (long) (Math.random() * (PAUSE_MAX_MS - PAUSE_MIN_MS));
        }
    }

    public enum CommandType {
        TPA("/tpa"),
        TPAHERE("/tpahere");
        private final String command;
        CommandType(String command) { this.command = command; }
        public String getCommand() { return command; }
    }
} 