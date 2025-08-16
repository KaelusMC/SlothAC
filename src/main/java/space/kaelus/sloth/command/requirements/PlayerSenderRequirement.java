package space.kaelus.sloth.command.requirements;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.command.SenderRequirement;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public final class PlayerSenderRequirement implements SenderRequirement {

    public static final PlayerSenderRequirement PLAYER_SENDER_REQUIREMENT = new PlayerSenderRequirement();

    @Override
    public @NonNull Component errorMessage(Sender sender) {
        return MessageUtil.getMessage(Message.RUN_AS_PLAYER);
    }

    @Override
    public boolean evaluateRequirement(@NonNull CommandContext<Sender> commandContext) {
        return commandContext.sender().isPlayer();
    }
}