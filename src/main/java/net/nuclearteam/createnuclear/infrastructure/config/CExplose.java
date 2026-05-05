package net.nuclearteam.createnuclear.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;

public class CExplose extends ConfigBase {
    public final ConfigInt size = i(500, 0, 500, "Size of the reactor explosion", Comments.size);
    public final ConfigInt type = i(1, 0, 2, "Type of explosion", Comments.type);
    public final ConfigInt time = i(600, 100, 1200, "Duration before explosion", Comments.explosionTime, Comments.hintExplosion);
    public final ConfigInt alarmWarningTime = i(200, 20, 600, "Alarm warning time", Comments.alarmWarningTime, Comments.hintAlarm);
    public final ConfigInt flashRadius = i(100, 1, 512, "Flash radius", Comments.flashRadius);

    @Override
    public String getName() {
        return "Explosion Reactor";
    }

    private static class Comments {
        static String size = "Reactor explosion radius in blocks. Warning: high values can heavily lag the server.";
        static String explosionTime = "Create Nuclear Explosion Time";
        static String hintExplosion = "300 ticks = 15 seconds";
        static String alarmWarningTime = "How many ticks before the explosion the alarm starts.";
        static String hintAlarm = "200 ticks = 10 seconds";
        static String flashRadius = "Players within this radius receive the white flash effect.";
        static String type = "Explanation: 0 = no explosion, 1 = current explosion, 2 = new explosion.";
    }
}
