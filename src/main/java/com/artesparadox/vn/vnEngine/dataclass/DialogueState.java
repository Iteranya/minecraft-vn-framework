package com.artesparadox.vn.vnEngine.dataclass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogueState {
    // This class will represent EVERYTHING visible on screen
    // I mean this in the most literal way possible
    // Everything on screen will be decided by this class
    // Let's *Fucking Do This*
    private String label;
    private String content;
    private String background;
    private String command;
    private String music;
    private String sound;
    private List <SpriteState> sprites = new ArrayList<>();
    private List<Map<String, Object>> choices;

    public DialogueState(String label, String content, List<Map<String, Object>> choices) {
        this.label = label;
        this.content = content;

    }

    // Getters and setters
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getContent() { return content; }
    public void setContent(String content) {
        if (content != null && !content.isEmpty()) {
            this.content = content;
        }

    }
//    public SpriteState getSprite() { return sprite; }
//    public void setSprite(SpriteState sprite) { this.sprite = sprite; }

    public List<SpriteState> getSprites() { return sprites; }
    public void addSprite(SpriteState sprite) { this.sprites.add(sprite); }

    public List<Map<String, Object>> getChoices() {
        return choices;
    }
    public void setChoices(List<Map<String, Object>> choices) { this.choices = choices; }

    public void setBackground(String background) {
        this.background = background;
    }

    public void clearBackground() {
        this.background = null;
    }

    public String getBackground() {
        return this.background;
    }

    public  String getCommand(){
        return this.command;
    }

    public void setCommand(String action) {
        this.command = action;
    }

    public String getMusic(){return this.music;}

    public void setMusic(String music) {
        this.music = music;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }
}