package fr.devsylone.fallenkingdom.commands.teams.teamscommands;

import com.google.common.collect.ImmutableList;
import fr.devsylone.fallenkingdom.commands.abstraction.CommandResult;
import fr.devsylone.fallenkingdom.commands.abstraction.CommandRole;
import fr.devsylone.fallenkingdom.commands.abstraction.FkCommand;
import fr.devsylone.fallenkingdom.commands.abstraction.FkParentCommand;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomCapture;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomEnabled;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomHealth;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomOffset;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomRegen;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomRemove;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomShow;
import fr.devsylone.fallenkingdom.commands.teams.teamscommands.chestsroom.ChestRoomSpawn;
import fr.devsylone.fallenkingdom.utils.Messages;
import fr.devsylone.fallenkingdom.version.component.FkBook;
import fr.devsylone.fallenkingdom.version.component.FkComponent;
import fr.devsylone.fkpi.FkPI;
import fr.devsylone.fkpi.managers.ChestsRoomsManager;
import fr.devsylone.fkpi.teams.Base;
import fr.devsylone.fkpi.teams.CrystalCore;
import fr.devsylone.fkpi.teams.Nexus;
import fr.devsylone.fkpi.teams.Team;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.devsylone.fallenkingdom.Fk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.devsylone.fallenkingdom.version.component.FkComponent.join;
import static fr.devsylone.fallenkingdom.version.component.FkComponent.newline;
import static fr.devsylone.fallenkingdom.version.component.FkComponent.space;
import static fr.devsylone.fallenkingdom.version.component.FkComponent.text;

public class ChestsRoom extends FkParentCommand
{
	private static final String BOOK_PERMISSION = "fallenkingdom.commands.team.chestsRoom.book";

	public ChestsRoom()
	{
		super("chestsRoom", ImmutableList.<FkCommand>builder()
				.add(new ChestRoomCapture())
				.add(new ChestRoomEnabled())
				.add(new ChestRoomHealth())
				.add(new ChestRoomOffset())
				.add(new ChestRoomRegen())
				.add(new ChestRoomRemove())
				.add(new ChestRoomShow())
				.add(new ChestRoomSpawn())
				.build()
		, Messages.CMD_MAP_CHEST_ROOM, CommandRole.PLAYER);
	}

	@Override
	public CommandResult execute(Fk plugin, CommandSender sender, List<String> args, String label)
	{
		if (args.isEmpty()) {
			this.displayStatus(sender, plugin.getFkPI());
		} else {
			super.execute(plugin, sender, args, label);
		}
		if (sender instanceof Player && plugin.getCommandManager().hasPermission(sender, BOOK_PERMISSION)) {
			final Base base = plugin.getFkPI().getTeamManager().getBase(((Player) sender).getLocation()).orElse(null);
			this.book(base).open((Player) sender);
		}
		return CommandResult.SUCCESS;
	}

	public void displayStatus(@NotNull CommandSender sender, @NotNull FkPI fkPI) {
		final ChestsRoomsManager manager = fkPI.getChestsRoomsManager();
		final boolean enabled = manager.isEnabled();
		sender.sendMessage(
				Messages.CMD_TEAM_CHEST_ROOM_STATUS.getMessage()
						.replace("%state%", (
								enabled ? Messages.CMD_RULES_ACTIVATED : Messages.CMD_RULES_DEACTIVATED
						).getMessage())
		);
		if (enabled) {
			final List<fr.devsylone.fkpi.teams.ChestsRoom> chestsRooms = new ArrayList<>();
			final List<CrystalCore> crystals = new ArrayList<>();
			for (Team team : fkPI.getTeamManager().getTeams()) {
				final Base base = team.getBase();
				if (base == null) continue;
				final Nexus nexus = base.getNexus();
				if (nexus instanceof fr.devsylone.fkpi.teams.ChestsRoom) {
					chestsRooms.add((fr.devsylone.fkpi.teams.ChestsRoom) nexus);
				} else if (nexus instanceof CrystalCore) {
					crystals.add((CrystalCore) nexus);
				}
			}
			if (!chestsRooms.isEmpty()) {
				sender.sendMessage(
						Messages.CMD_TEAM_CHEST_ROOM_STATUS_CHEST.getMessage() + ": "
								+ chestsRooms.stream().map(nexus -> nexus.getBase().getTeam().toString()).collect(Collectors.joining(", "))
				);
				sender.sendMessage(
						Messages.CMD_TEAM_CHEST_ROOM_STATUS_CHEST_PARAMS.getMessage()
								.replace("%offset%", String.valueOf(manager.getOffset()))
								.replace("%offsetunit%", Messages.Unit.BLOCKS.tl(manager.getOffset()))
								.replace("%time%", String.valueOf(manager.getCaptureTime()))
								.replace("%timeunit%", Messages.Unit.SECONDS.tl(manager.getCaptureTime()))
				);
			}
			if (!crystals.isEmpty()) {
				sender.sendMessage(
						Messages.CMD_TEAM_CHEST_ROOM_STATUS_CRYSTAL.getMessage() + ": "
								+ crystals.stream().map(nexus -> nexus.getBase().getTeam().toString()).collect(Collectors.joining(", "))
				);
				sender.sendMessage(
						Messages.CMD_TEAM_CHEST_ROOM_STATUS_CRYSTAL_PARAMS.getMessage()
								.replace("%health%", String.valueOf(manager.getCoreHealth()))
								.replace("%regen%", String.valueOf(manager.getRegenerationPerMinute()))
								.replace("%allyBonus%", String.valueOf(manager.getRegenerationPerMinutePerAlly()))
				);
			}
		}
	}

	public @NotNull FkBook book(@Nullable Base base) {
		FkComponent title = text("ChestsRoom - Help", ChatColor.GREEN);
		FkComponent author = text("Devsylone", ChatColor.DARK_BLUE);
		FkComponent[] pages = new FkComponent[]{
				join(
						text(" Reconnaissance auto\n     de la salle des\n         coffres\n\n ", ChatColor.DARK_BLUE),
						text("[Activer]", ChatColor.DARK_GREEN).command("/fk team chestsRoom enabled true").hover("Clique pour activer"),
						space(),
						text("[Désactiver]", ChatColor.DARK_RED).command("/fk team chestsRoom enabled false").hover("Clique pour désactiver"),
						text("\n   "),
						base == null ? space() : text("[Placer un cristal]", ChatColor.DARK_PURPLE).command("/fk team chestsRoom spawn " + base.getTeam().getName()).hover("Clique pour placer un cœur de base"),
						text("\n\n   "),
						text("> En savoir plus <", ChatColor.UNDERLINE).changePage(5).hover("Clique pour plus d'infos")
				),
				join(
						text(" Temps de la capture\n     de la salle des\n         coffres\n\n", ChatColor.DARK_BLUE),
						text("> 2 minutes", ChatColor.GREEN).command("/fk team chestsRoom captureTime 120"),
						newline(),
						text("> 1 minute", ChatColor.GREEN).command("/fk team chestsRoom captureTime 60"),
						newline(),
						text("> 50 secondes", ChatColor.GREEN).command("/fk team chestsRoom captureTime 50"),
						newline(),
						text("> 40 secondes", ChatColor.GREEN).command("/fk team chestsRoom captureTime 40"),
						newline(),
						text("> 30 secondes", ChatColor.GREEN).command("/fk team chestsRoom captureTime 30"),
						text("\n\n/fk team chestsRoom captureTime")
				),
				join(
						text("Marge de la salle des coffres\n", ChatColor.DARK_BLUE),
						text("> 1 bloc", ChatColor.GREEN).command("/fk team chestsRoom offset 1"),
						newline(),
						text("> 2 blocs (Conseillé)", ChatColor.GREEN).command("/fk team chestsRoom offset 2"),
						newline(),
						text("> 3 blocs", ChatColor.GREEN).command("/fk team chestsRoom offset 3"),
						newline(),
						text("> 4 blocs", ChatColor.GREEN).command("/fk team chestsRoom offset 4"),
						newline(),
						text("> 5 blocs", ChatColor.GREEN).command("/fk team chestsRoom offset 5"),
						text("\n\n   "),
						text("> En savoir plus <", ChatColor.UNDERLINE).changePage(8).hover("Clique pour plus d'infos")
				),
				text("Explications\npages suivantes"),
				text("↪ La gestion de la salle des coffres permet d'automatiser l'élimination d'une équipe lorsque le plugin détecte qu'une équipe l'a capturée.\n\nDeux options sont disponibles :\n\n\n\n--->"),
				text("1. Les coffres posés par les joueurs sont détectés et une salle des coffres est générée en fonction de leur position.\n\n2. L'administrateur de la partie place un cristal que les ennemis doivent frapper pour capturer la base.\n--->"),
				join(
						text("En optant pour la première option, chaque joueur peut visualiser à tout moment de la partie sa salle des coffres grâce à la commande"),
						text(" /fk team chestsRoom show", ChatColor.DARK_PURPLE),
						text("\nVous êtes le seul à voir votre salle des coffres.", ChatColor.RED)
				),
				text("↪ À chaque fois qu'un joueur pose un coffre dans sa base, la salle des coffres évolue pour former une zone rectangulaire englobant tous les coffres posés (seulement en survie).\n\n--->"),
				join(
						text("↪ La "),
						text("marge", ChatColor.AQUA),
						text(" ou "),
						text("offset", ChatColor.AQUA),
						text(" est le nombre de blocs qui se trouvent entre les coffres les plus proches de la limite de la salle des coffres et la limite de la salle elle-même.\n\n"),
						text("Exemple page suivante --->", ChatColor.GOLD)
				),
				join(
						text("Exemple", ChatColor.UNDERLINE, ChatColor.BOLD),
						text(" : Avec une marge de 2 blocs, lorsque vous allez poser un seul coffre, les dimensions de la salle seront de 5x5x5.\n\n"),
						text("Screens\n\n", ChatColor.UNDERLINE, ChatColor.BOLD),
						text("     "),
						text("> En Vanilla <", ChatColor.UNDERLINE, ChatColor.LIGHT_PURPLE).interact(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://imgur.com/kRpAMkA")),
						text("\n\n   "),
						text("> Marge 2 blocs <", ChatColor.UNDERLINE, ChatColor.DARK_GREEN).interact(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://imgur.com/q1A1BfO")),
						text("\n\n   "),
						text("> Marge 4 blocs <", ChatColor.UNDERLINE, ChatColor.DARK_GREEN).interact(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://imgur.com/8flOtrC"))
				)
		};
		return FkBook.book(title, author, pages);
	}

	@Override
	public @NotNull Map<String, CommandRole> getPermissions() {
		Map<String, CommandRole> permissions = new HashMap<>(2);
		permissions.put(this.permission, this.role);
		permissions.put(BOOK_PERMISSION, CommandRole.ADMIN);
		return permissions;
	}
}
