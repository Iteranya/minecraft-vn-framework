package com.artesparadox.vn.vnEngine.controller;

import com.artesparadox.vn.vnEngine.dataclass.DialogueState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class VNEngine {

    public AtomicBoolean shutdown = new AtomicBoolean(false);
    public List<Map<String, Object>> gameData;
    public List<Map<String, Object>> globalSave;
    public List<Map<String, Object>> localSave;
    public AtomicLong currentState = new AtomicLong(0);
    public Map<String, Object> localVariables = new HashMap<>();

    public Map<String, Object> globalVariables = new HashMap<>();

    public DialogueState state;
    public AtomicBoolean isEngineRunning = new AtomicBoolean(false);

    public StringBuffer entityType = new StringBuffer();

    public StringBuffer entityName = new StringBuffer();

    public StringBuffer uid = new StringBuffer();

    public AtomicBoolean isDay = new AtomicBoolean(true);

    public List<Map<String, Integer>> inventoryHandler; // Contains inventory, list of item id and number


    public VNEngine(
            List<Map<String, Object>> gameData,
            String entityType,
            String entityName,
            String uid,
            boolean day,
            List<Map<String, Integer>> inventory,
            List<Map<String, Object>> globalSave,
            List<Map<String,Object>> localSave
    ) {
        this.uid.setLength(0);
        this.uid.append(uid);

        this.gameData = gameData;
        this.globalSave = globalSave;
        this.localSave = localSave;
        this.state = new DialogueState(null,null,null);
        this.entityName.setLength(0);
        this.entityName.append(entityName);
        this.entityType.setLength(0);
        this.entityType.append(entityType);
        this.isDay.set(day);
        this.inventoryHandler = inventory;
        SaveHandler.loadProgress(this);
    }

    // Look, for the sake of my own sanity, I have to refactor this thing...

    @SuppressWarnings("unchecked")
    private void processAction(Map<String, Object> action) {
        String actionType = (String) action.get("type");

        switch (actionType) {
            case "show_sprite":
                updateSprite(action, this);
                return;
            case "remove_sprite":
                removeSprite((String) action.get("sprite"), this);
                return;
            case "dialogue":
                String sound = (String) action.get("voice");
                updateDialogue(
                        (String) action.get("label"),
                        (String) action.get("content"),
                        (String) action.get("voice"),
                        this);
                return;
            case "modify_variable":
                modifyVariable(
                        (String) action.get("var"),
                        (String) action.get("action"),
                        action.get("value"),
                        localVariables,
                        currentState
                );
                break;
            case "modify_global":
                modifyVariable(
                        (String) action.get("var"),
                        (String) action.get("action"),
                        action.get("value"),
                        globalVariables,
                        currentState
                );
                break;
            case "give_item":
                inventoryHandler.giveItemToPlayer((String) action.get("item"), (int) (long) action.get("amount"));
                break;
            case "conditional":
                processConditional(action, this);
                break;
            case "conditional_global":
                processGlobalConditional(action, this);
                break;
            case "transition":
                if ("jump".equals(action.get("action"))) {
                    processJump(action,this);
                }
                break;
            case "choice":
                System.out.println("Try to yoink choice" + action);
                updateChoices(
                        (List<Map<String, Object>>) action.get("choice"), this
                );
                break;
            case "command":
                updateCommand(action, state);
                this.currentState.incrementAndGet();
                break;
            case "label":
                this.currentState.incrementAndGet();
                break;
            case "modify_background":
                updateBackground((String) action.get("background"),this);
                break;
            case "clear_background":
                state.clearBackground();
                this.currentState.incrementAndGet();
                break;
            case "night_choice":
                if (!isDay.get()) {
                    updateChoices((List<Map<String, Object>>) action.get("choice"), this);
                } else {
                    this.currentState.incrementAndGet();
                }
                break;
            case "unlock_dialogues":
                Set<String> events = new HashSet<>((List<String>) this.localVariables.getOrDefault("unlocked_events", new ArrayList<>()));
                events.addAll((List<String>) action.get("events"));
                this.localVariables.put("unlocked_events", new ArrayList<>(events));
                this.currentState.incrementAndGet();
                break;
            case "play_sound":
                updateSound(this, (String) action.get("sound"));
            case "play_music":
                if(action.get("music")!=null){
                    updateMusic(this, (String) action.get("music"));
                }else{
                    stopMusic(this);
                }

            case "next":
                processNext(action,this);
                this.currentState.incrementAndGet();
                break;
            case "idle_chat":
                processIdleChat(this);
                break;
            case "finish_dialogue":
                processFinishing(this);
            case "check_inventory":
            default:
                this.currentState.incrementAndGet();
                break;
        }
    }



    public void runEngine() {
        while (isEngineRunning.get()) { // Infinite loop
            // Check if engine is running
            System.out.println(this.currentState);
            Map<String, Object> action = getDictById(this.currentState.get(),gameData);
            if(action == null){
                shutdown.set(true);
                isEngineRunning.set(false);
                return;
            }
            if ("meta".equals(((Map<?, ?>) action).get("type"))) {
                processMeta(action,this);
            } else {
                processAction(action);
            }

        }
    }

    public DialogueState getNext() {
        return this.state;
    }

    public void buttonPress(String choice) {
        changeStateByLabel(choice,currentState,gameData);
        this.state.setChoices(new ArrayList<>());
    }
}