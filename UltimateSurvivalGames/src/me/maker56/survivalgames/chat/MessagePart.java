package me.maker56.survivalgames.chat;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;

class MessagePart
{
  ChatColor color = ChatColor.WHITE;
  ArrayList<ChatColor> styles = new ArrayList();
  String clickActionName = null; String clickActionData = null;
  String hoverActionName = null; String hoverActionData = null;
  String text = null;

  MessagePart(String text) {
    this.text = text;
  }
  MessagePart() {
  }

  boolean hasText() {
    return this.text != null;
  }

  JsonWriter writeJson(JsonWriter json) {
    try {
      json.beginObject().name("text").value(this.text);
      json.name("color").value(this.color.name().toLowerCase());
      for (ChatColor style : this.styles)
      {
        String styleName;
        switch (style) {
        case RED:
          styleName = "obfuscated"; break;
        case UNDERLINE:
          styleName = "underlined"; break;
        case RESET:
        case STRIKETHROUGH:
        default:
          styleName = style.name().toLowerCase();
        }
        json.name(styleName).value(true);
      }
      if ((this.clickActionName != null) && (this.clickActionData != null)) {
        json.name("clickEvent")
          .beginObject()
          .name("action").value(this.clickActionName)
          .name("value").value(this.clickActionData)
          .endObject();
      }
      if ((this.hoverActionName != null) && (this.hoverActionData != null)) {
        json.name("hoverEvent")
          .beginObject()
          .name("action").value(this.hoverActionName)
          .name("value").value(this.hoverActionData)
          .endObject();
      }
      return json.endObject();
    } catch (Exception e) {
      e.printStackTrace();
    }return json;
  }
}