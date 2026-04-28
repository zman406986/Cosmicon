package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.util.CharacterIds;

public class WeatherManager {
    
    private static final List<Integer> WEATHER_TURN_SCHEDULE = Arrays.asList(2, 4, 6, 8);
    
    private WeatherType currentWeather;
    private int currentTurn;
    private final List<WeatherType> weatherSchedule;
    private final boolean isStoryBattle;
    private final String storyBattleId;
    
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
            WeatherType.PARHELION, WeatherType.THUNDERSTORM, WeatherType.SEA_OF_CLOUDS, WeatherType.SUNNY));
        STORY_BATTLE_WEATHERS.put("seele", Arrays.asList(
            WeatherType.FROG_RAIN, WeatherType.HIGH_TEMPERATURE, WeatherType.CREPUSCULAR_RAYS, WeatherType.SUNNY));
    }
    
    public WeatherManager() {
        this(false, null);
    }
    
    public WeatherManager(boolean isStoryBattle, String storyBattleId) {
        this.currentTurn = 1;
        this.currentWeather = null;
        this.isStoryBattle = isStoryBattle;
        this.storyBattleId = storyBattleId;
        this.weatherSchedule = new ArrayList<>();
        
        initializeWeatherSchedule();
    }
    
    private void initializeWeatherSchedule() {
        if (isStoryBattle && storyBattleId != null) {
            List<WeatherType> storyWeather = STORY_BATTLE_WEATHERS.get(storyBattleId.toLowerCase());
            if (storyWeather != null) {
                weatherSchedule.addAll(storyWeather);
            } else {
                generateRandomWeatherSchedule();
            }
        } else if (isNpcDuel()) {
            weatherSchedule.add(WeatherType.THE_DECISIVE_MOMENT);
        } else {
            generateRandomWeatherSchedule();
        }
    }
    
    private void generateRandomWeatherSchedule() {
        List<WeatherType> turn2Weathers = getWeathersForTurn(2);
        List<WeatherType> turn4Weathers = getWeathersForTurn(4);
        List<WeatherType> turn6Weathers = getWeathersForTurn(6);
        List<WeatherType> turn8Weathers = getWeathersForTurn(8);
        
        Collections.shuffle(turn2Weathers);
        Collections.shuffle(turn4Weathers);
        Collections.shuffle(turn6Weathers);
        Collections.shuffle(turn8Weathers);
        
        weatherSchedule.add(turn2Weathers.get(0));
        weatherSchedule.add(turn4Weathers.get(0));
        weatherSchedule.add(turn6Weathers.get(0));
        weatherSchedule.add(turn8Weathers.get(0));
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
    
    public void advanceTurn() {
        currentTurn++;
        
        int scheduleIndex = WEATHER_TURN_SCHEDULE.indexOf(currentTurn);
        if (scheduleIndex >= 0 && scheduleIndex < weatherSchedule.size()) {
            WeatherType newWeather = weatherSchedule.get(scheduleIndex);
            setCurrentWeather(newWeather);
        }
    }
    
    public void reset() {
        currentTurn = 1;
        currentWeather = null;
        weatherSchedule.clear();
        initializeWeatherSchedule();
    }
    
    public WeatherType getCurrentWeather() {
        return currentWeather;
    }
    
    public void setCurrentWeather(WeatherType weather) {
        this.currentWeather = weather;
    }
    
    
    
    public boolean isNpcDuel() {
        return false;
    }
    
}