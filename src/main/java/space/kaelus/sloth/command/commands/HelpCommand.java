package space.kaelus.sloth.command.commands;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class HelpCommand implements SlothCommand {

    @Override
    public void register(CommandManager<Sender> manager) {
        final var builder = manager.commandBuilder("sloth", "slothac")
                .permission("sloth.help");

        manager.command(builder.handler(this::help));

        manager.command(builder.literal("help").handler(this::help));
    }

    private void help(CommandContext<Sender> context) {
        final Sender sender = context.sender();
        MessageUtil.sendMessageList(sender.getNativeSender(), Message.HELP_MESSAGE, "command", "sloth");
    }
}