package me.maker56.survivalgames.chat;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.maker56.survivalgames.user.SpectatorUser;
import me.maker56.survivalgames.user.User;
import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JSONMessage
{
  private final List<MessagePart> messageParts;
  private String jsonString;
  private boolean dirty;
  private Class<?> nmsChatSerializer = ReflectionUtil.getNMSClass("ChatSerializer");
  private Class<?> nmsTagCompound = ReflectionUtil.getNMSClass("NBTTagCompound");
  private Class<?> nmsPacketPlayOutChat = ReflectionUtil.getNMSClass("PacketPlayOutChat");
  private Class<?> nmsAchievement = ReflectionUtil.getNMSClass("Achievement");
  private Class<?> nmsStatistic = ReflectionUtil.getNMSClass("Statistic");
  private Class<?> nmsItemStack = ReflectionUtil.getNMSClass("ItemStack");

  private Class<?> obcStatistic = ReflectionUtil.getOBCClass("CraftStatistic");
  private Class<?> obcItemStack = ReflectionUtil.getOBCClass("inventory.CraftItemStack");

  public JSONMessage(String firstPartText) {
    this.messageParts = new ArrayList();
    this.messageParts.add(new MessagePart(firstPartText));
    this.jsonString = null;
    this.dirty = false;
  }

  public JSONMessage() {
    this.messageParts = new ArrayList();
    this.messageParts.add(new MessagePart());
    this.jsonString = null;
    this.dirty = false;
  }

  public JSONMessage text(String text) {
    MessagePart latest = latest();
    if (latest.hasText()) {
      throw new IllegalStateException("text for this message part is already set");
    }
    latest.text = text;
    this.dirty = true;
    return this;
  }

  public JSONMessage color(ChatColor color) {
    if (!color.isColor()) {
      throw new IllegalArgumentException(color.name() + " is not a color");
    }
    latest().color = color;
    this.dirty = true;
    return this;
  }

  public JSONMessage style(ChatColor[] styles) {
    for (ChatColor style : styles) {
      if (!style.isFormat()) {
        throw new IllegalArgumentException(style.name() + " is not a style");
      }
    }
    latest().styles.addAll(Arrays.asList(styles));
    this.dirty = true;
    return this;
  }

  public JSONMessage file(String path) {
    onClick("open_file", path);
    return this;
  }

  public JSONMessage link(String url) {
    onClick("open_url", url);
    return this;
  }

  public JSONMessage suggest(String command) {
    onClick("suggest_command", command);
    return this;
  }

  public JSONMessage command(String command) {
    onClick("run_command", command);
    return this;
  }

  public JSONMessage achievementTooltip(String name) {
    onHover("show_achievement", "achievement." + name);
    return this;
  }

  public JSONMessage achievementTooltip(Achievement which) {
    try {
      Object achievement = ReflectionUtil.getMethod(this.obcStatistic, "getNMSAchievement", new Class[0]).invoke(null, new Object[] { which });
      return achievementTooltip((String)ReflectionUtil.getField(this.nmsAchievement, "name").get(achievement));
    } catch (Exception e) {
      e.printStackTrace();
    }return this;
  }

  public JSONMessage statisticTooltip(Statistic which)
  {
    Statistic.Type type = which.getType();
    if (type != Statistic.Type.UNTYPED)
      throw new IllegalArgumentException("That statistic requires an additional " + type + " parameter!");
    try
    {
      Object statistic = ReflectionUtil.getMethod(this.obcStatistic, "getNMSStatistic", new Class[0]).invoke(null, new Object[] { which });
      return achievementTooltip((String)ReflectionUtil.getField(this.nmsStatistic, "name").get(statistic));
    } catch (Exception e) {
      e.printStackTrace();
    }return this;
  }

  public JSONMessage statisticTooltip(Statistic which, Material item)
  {
    Statistic.Type type = which.getType();
    if (type == Statistic.Type.UNTYPED) {
      throw new IllegalArgumentException("That statistic needs no additional parameter!");
    }
    if (((type == Statistic.Type.BLOCK) && (item.isBlock())) || (type == Statistic.Type.ENTITY))
      throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + type + "!");
    try
    {
      Object statistic = ReflectionUtil.getMethod(this.obcStatistic, "getMaterialStatistic", new Class[0]).invoke(null, new Object[] { which, item });
      return achievementTooltip((String)ReflectionUtil.getField(this.nmsStatistic, "name").get(statistic));
    } catch (Exception e) {
      e.printStackTrace();
    }return this;
  }

  public JSONMessage statisticTooltip(Statistic which, EntityType entity)
  {
    Statistic.Type type = which.getType();
    if (type == Statistic.Type.UNTYPED) {
      throw new IllegalArgumentException("That statistic needs no additional parameter!");
    }
    if (type != Statistic.Type.ENTITY)
      throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + type + "!");
    try
    {
      Object statistic = ReflectionUtil.getMethod(this.obcStatistic, "getEntityStatistic", new Class[0]).invoke(null, new Object[] { which, entity });
      return achievementTooltip((String)ReflectionUtil.getField(this.nmsStatistic, "name").get(statistic));
    } catch (Exception e) {
      e.printStackTrace();
    }return this;
  }

  public JSONMessage itemTooltip(String itemJSON)
  {
    onHover("show_item", itemJSON);
    return this;
  }

  public JSONMessage itemTooltip(ItemStack itemStack) {
    try {
      Object nmsItem = ReflectionUtil.getMethod(this.obcItemStack, "asNMSCopy", new Class[] { ItemStack.class }).invoke(null, new Object[] { itemStack });
      return itemTooltip(ReflectionUtil.getMethod(this.nmsItemStack, "save", new Class[0]).invoke(nmsItem, new Object[] { this.nmsTagCompound.newInstance() }).toString());
    } catch (Exception e) {
      e.printStackTrace();
    }return this;
  }

  public JSONMessage tooltip(String text)
  {
    return tooltip(text.split("\\n"));
  }

  public JSONMessage tooltip(List<String> lines) {
    return tooltip((String[])lines.toArray());
  }

  public JSONMessage tooltip(String[] lines) {
    if (lines.length == 1)
      onHover("show_text", lines[0]);
    else {
      itemTooltip(makeMultilineTooltip(lines));
    }
    return this;
  }

  public JSONMessage then(Object obj) {
    if (!latest().hasText()) {
      throw new IllegalStateException("previous message part has no text");
    }
    this.messageParts.add(new MessagePart(obj.toString()));
    this.dirty = true;
    return this;
  }

  public JSONMessage then() {
    if (!latest().hasText()) {
      throw new IllegalStateException("previous message part has no text");
    }
    this.messageParts.add(new MessagePart());
    this.dirty = true;
    return this;
  }

  public String toJSONString() {
    if ((!this.dirty) && (this.jsonString != null)) {
      return this.jsonString;
    }
    StringWriter string = new StringWriter();
    JsonWriter json = new JsonWriter(string);
    try {
      if (this.messageParts.size() == 1) {
        latest().writeJson(json);
      } else {
        json.beginObject().name("text").value("").name("extra").beginArray();
        for (MessagePart part : this.messageParts) {
          part.writeJson(json);
        }
        json.endArray().endObject();
        json.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("invalid message");
    }
    this.jsonString = string.toString();
    this.dirty = false;
    return this.jsonString;
  }

  public void send(Player player) {
    try {
      Object handle = ReflectionUtil.getHandle(player);
      Object connection = ReflectionUtil.getField(handle.getClass(), "playerConnection").get(handle);
      Object serialized = ReflectionUtil.getMethod(this.nmsChatSerializer, "a", new Class[] { String.class }).invoke(null, new Object[] { toJSONString() });
      Object packet = this.nmsPacketPlayOutChat.getConstructor(new Class[] { ReflectionUtil.getNMSClass("IChatBaseComponent") }).newInstance(new Object[] { serialized });
      ReflectionUtil.getMethod(connection.getClass(), "sendPacket", new Class[0]).invoke(connection, new Object[] { packet });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void send(CommandSender sender) {
    if ((sender instanceof Player))
      send((Player)sender);
    else
      sender.sendMessage(toOldMessageFormat());
  }

  public void send(Iterable<? extends CommandSender> senders)
  {
    for (CommandSender sender : senders)
      send(sender);
  }

  public void sendToSpectators(List<SpectatorUser> users)
  {
    for (SpectatorUser su : users)
      send(su.getPlayer());
  }

  public void send(List<User> users)
  {
    for (User u : users)
      send(u.getPlayer());
  }

  public String toOldMessageFormat()
  {
    StringBuilder result = new StringBuilder();
    for (MessagePart part : this.messageParts) {
      result.append(part.color).append(part.text);
    }
    return result.toString();
  }

  private MessagePart latest() {
    return (MessagePart)this.messageParts.get(this.messageParts.size() - 1);
  }

  private String makeMultilineTooltip(String[] lines) {
    StringWriter string = new StringWriter();
    JsonWriter json = new JsonWriter(string);
    try {
      json.beginObject().name("id").value(1L);
      json.name("tag").beginObject().name("display").beginObject();
      json.name("Name").value("\\u00A7f" + lines[0].replace("\"", "\\\""));
      json.name("Lore").beginArray();
      for (int i = 1; i < lines.length; i++) {
        String line = lines[i];
        json.value(line.isEmpty() ? " " : line.replace("\"", "\\\""));
      }
      json.endArray().endObject().endObject().endObject();
      json.close();
    } catch (Exception e) {
      throw new RuntimeException("invalid tooltip");
    }
    return string.toString();
  }

  private void onClick(String name, String data) {
    MessagePart latest = latest();
    latest.clickActionName = name;
    latest.clickActionData = data;
    this.dirty = true;
  }

  private void onHover(String name, String data) {
    MessagePart latest = latest();
    latest.hoverActionName = name;
    latest.hoverActionData = data;
    this.dirty = true;
  }
}