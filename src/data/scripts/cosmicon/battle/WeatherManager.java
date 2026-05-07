package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import data.scripts.cosmicon.util.CharacterIds;

public class WeatherManager {
    
    private static final List<Integer> WEATHER_TURN_SCHEDULE = Arrays.asList(2, 4, 6, 8);
    
    private WeatherType currentWeather;
    private int currentTurn;
    private int halfTurnCount;
    private boolean fullTurnBoundary;
    private final List<WeatherType> weatherSchedule;
    private final boolean isStoryBattle;
    private final boolean isNpcDuel;
    private final String storyBattleId;
    private Map<Integer, WeatherType> forcedWeatherSchedule;
    private boolean weatherDisabled;
    
    private static final Map<String, List<WeatherType>> STORY_BATTLE_WEATHERS = new HashMap<>();
    
    static {
        STORY_BATTLE_WEATHERS.put("svarog", Arrays.asList(
            WeatherType.DUST, WeatherType.FROG_RAIN, WeatherType.BLIZZARD, WeatherType.DRY_THUNDERSTORM));
        STORY_BATTLE_WEATHERS.put("clara", Arrays.asList(
            WeatherType.HIGH_TEMPERATURE, WeatherType.TEMPORAL_STORM, WeatherType.SEA_OF_CLOUDS, WeatherType.SUNNY));
        STORY_BATTLE_WEATHERS.put("serval", Arrays.asList(
            WeatherType.CYCLONIC_SWARM, WeatherType.BLIZZARD, WeatherType.MODERATE_SNOW, WeatherType.DRY_THUNDERSTORM));
        STORY_BATTLE_WEATHERS.put("landau", Arrays.asList(
            WeatherType.FISH_RAIN, WeatherType.STORM, WeatherType.FROST, WeatherType.TOXIC_FOG));
        STORY_BATTLE_WEATHERS.put("julian", Arrays.asList(
            WeatherType.SOLAR_ECLIPSE, WeatherType.FROST, WeatherType.DROUGHT, WeatherType.SUNNY));
        STORY_BATTLE_WEATHERS.put("hook", Arrays.asList(
            WeatherType.RAINBOW, WeatherType.SEA_OF_CLOUDS, WeatherType.LUNISOLAR_LUMINANCE, WeatherType.DRY_THUNDERSTORM));
        STORY_BATTLE_WEATHERS.put("hook_true", Arrays.asList(
            WeatherType.RAINBOW, WeatherType.SEA_OF_CLOUDS, WeatherType.LUNISOLAR_LUMINANCE, WeatherType.DRY_THUNDERSTORM));
        STORY_BATTLE_WEATHERS.put(CharacterIds.SPARXIE, Arrays.asList(
            WeatherType.ACID_RAIN, WeatherType.VENOCLLOUD, WeatherType.DRIZZLE, WeatherType.TOXIC_FOG));
        STORY_BATTLE_WEATHERS.put("rappa", Arrays.asList(
            WeatherType.SCORCHING_SUN, WeatherType.RAINBOW, WeatherType.DROUGHT, WeatherType.DRY_THUNDERSTORM));
        STORY_BATTLE_WEATHERS.put(CharacterIds.YAO_GUANG, Arrays.asList(
            WeatherType.PARHELION, WeatherType.SEA_OF_CLOUDS, WeatherType.SUNNY));
        STORY_BATTLE_WEATHERS.put("seele", Arrays.asList(
            WeatherType.FROG_RAIN, WeatherType.HIGH_TEMPERATURE, WeatherType.CREPUSCULAR_RAYS, WeatherType.SUNNY));
    }
    
    public WeatherManager() {
        this(false, null, false);
    }
    
    public WeatherManager(boolean isStoryBattle, String storyBattleId) {
        this(isStoryBattle, storyBattleId, false);
    }

    public WeatherManager(boolean isStoryBattle, String storyBattleId, boolean isNpcDuel) {
        this.currentTurn = 1;
        this.currentWeather = null;
        this.halfTurnCount = 0;
        this.fullTurnBoundary = true;
        this.isStoryBattle = isStoryBattle;
        this.isNpcDuel = isNpcDuel;
        this.storyBattleId = storyBattleId;
        this.weatherSchedule = new ArrayList<>();
        this.forcedWeatherSchedule = null;
        
        initializeWeatherSchedule();
    }
    
    public void setForcedWeatherSchedule(Map<Integer, WeatherType> schedule) {
        this.forcedWeatherSchedule = schedule;
    }
    
    public void setWeatherDisabled(boolean disabled) {
        this.weatherDisabled = disabled;
    }
    
    private void initializeWeatherSchedule() {
        if (isStoryBattle && storyBattleId != null) {
            List<WeatherType> storyWeather = STORY_BATTLE_WEATHERS.get(storyBattleId.toLowerCase());
            if (storyWeather != null) {
                weatherSchedule.addAll(storyWeather);
            } else {
                generateRandomWeatherSchedule();
            }
        } else if (isNpcDuel) {
            weatherSchedule.add(WeatherType.THE_DECISIVE_MOMENT);
        } else {
            generateRandomWeatherSchedule();
        }
    }
    
    private void generateRandomWeatherSchedule() {
        for (int turn : WEATHER_TURN_SCHEDULE) {
            List<WeatherType> candidates = getWeathersForTurn(turn);
            if (!candidates.isEmpty()) {
                Collections.shuffle(candidates, ThreadLocalRandom.current());
                weatherSchedule.add(candidates.get(0));
            }
        }
    }
    
    private List<WeatherType> getWeathersForTurn(int turn) {
        List<WeatherType> result = new ArrayList<>();
        for (WeatherType weather : WeatherType.values()) {
            if (weather.getDefaultTurn() == turn) {
                result.add(weather);
            }
        }
        return result;
    }
    
    private List<WeatherType> getWeathersForTurn(int turn, boolean allowSafeguard, boolean allowAttack) {
        List<WeatherType> result = new ArrayList<>();
        for (WeatherType weather : WeatherType.values()) {
            if (weather.getDefaultTurn() != turn) continue;
            WeatherCategory cat = weather.getCategory();
            if (cat == WeatherCategory.SAFEGUARD && !allowSafeguard) continue;
            if (cat == WeatherCategory.ATTACK && !allowAttack) continue;
            result.add(weather);
        }
        return result;
    }
    
    public void advanceTurn(boolean allowSafeguard, boolean allowAttack) {
        if (weatherDisabled) return;
        
        halfTurnCount++;
        fullTurnBoundary = (halfTurnCount % 2 == 0);
        
        if (!fullTurnBoundary) return;
        
        currentTurn++;
        
        if (forcedWeatherSchedule != null && forcedWeatherSchedule.containsKey(currentTurn)) {
            setCurrentWeather(forcedWeatherSchedule.get(currentTurn));
            return;
        }
        
        int scheduleIndex = WEATHER_TURN_SCHEDULE.indexOf(currentTurn);
        if (scheduleIndex < 0 || scheduleIndex >= weatherSchedule.size()) return;
        
        if (isStoryBattle || isNpcDuel) {
            setCurrentWeather(weatherSchedule.get(scheduleIndex));
            return;
        }
        
        WeatherType scheduled = weatherSchedule.get(scheduleIndex);
        WeatherCategory cat = scheduled.getCategory();
        boolean scheduledAllowed = !((cat == WeatherCategory.SAFEGUARD && !allowSafeguard) ||
                                     (cat == WeatherCategory.ATTACK && !allowAttack));
        
        if (scheduledAllowed) {
            setCurrentWeather(scheduled);
            return;
        }
        
        List<WeatherType> candidates = getWeathersForTurn(currentTurn, allowSafeguard, allowAttack);
        if (candidates.isEmpty()) {
            candidates = getWeathersForTurn(currentTurn);
        }
        if (!candidates.isEmpty()) {
            Collections.shuffle(candidates, ThreadLocalRandom.current());
            setCurrentWeather(candidates.get(0));
        }
    }
    
    public boolean isFullTurnBoundary() {
        return fullTurnBoundary;
    }
    
    public void reset() {
        currentTurn = 1;
        currentWeather = null;
        halfTurnCount = 0;
        fullTurnBoundary = true;
        weatherSchedule.clear();
        initializeWeatherSchedule();
    }
    
    public WeatherType getCurrentWeather() {
        return currentWeather;
    }
    
    public void setCurrentWeather(WeatherType weather) {
        this.currentWeather = weather;
    }
    
}
