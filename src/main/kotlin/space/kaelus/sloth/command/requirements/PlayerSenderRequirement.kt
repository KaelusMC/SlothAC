package space.kaelus.sloth.command.requirements

import net.kyori.adventure.text.Component
import org.incendo.cloud.context.CommandContext
import space.kaelus.sloth.command.SenderRequirement
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

object PlayerSenderRequirement : SenderRequirement {
  override fun errorMessage(sender: Sender): Component {
    return MessageUtil.getMessage(Message.RUN_AS_PLAYER)
  }

  override fun evaluateRequirement(commandContext: CommandContext<Sender>): Boolean {
    return commandContext.sender().isPlayer
  }
}
