package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class L10N {
    public static final Language language = Language.RU;
    
    enum Language {
	EN("en"),
	RU("ru");
	
	public final String name;
	
	Language(String name) {this.name = name;}
    }
    
    enum Bundle {
	BUTTON("button"),
	LABEL("label", true);
	
	public final String name;
	public final boolean useMatch;
	
	Bundle(String name, boolean useMatch) {
	    this.name = name;
	    this.useMatch = useMatch;
	}
	
	Bundle(String name) {this(name, false);}
    }
    
    private final static Map<Bundle, Map<String, String>> simple = new HashMap<>();
    private final static Map<Bundle, Map<Pattern, String>> match = new HashMap<>();
    
    private static final boolean DBG = true;
    private final static Gson GSON_OUT = new GsonBuilder().setPrettyPrinting().create();
    private final static Map<Bundle, Map<String, String>> MISSING = new HashMap<>();
    
    static {
	for (Bundle bundle : Bundle.values()) {
	    if(bundle.useMatch) {
		match.put(bundle, loadMatch(bundle));
	    } else {
		simple.put(bundle, loadSimple(bundle));
	    }
	    MISSING.put(bundle, new HashMap<>());
	}
    }
    
    public static String button(String text) {
	return process(Bundle.BUTTON, text);
    }
    
    public static String label(String text) {
	return process(Bundle.LABEL, text);
    }
    
    public static String process(Bundle bundle, Resource key, String def) {
	Map<String, String> map = simple.get(bundle);
	if(map == null || key == null)
	    return def;
	String ll = map.get(key.name);
	return ll != null ? ll : def;
    }
    
    private static final Pattern tmp = Pattern.compile("^\\d+$");
    
    public static String process(Bundle bundle, String key) {
	String result = null;
	if(key == null || key.isEmpty() || language == Language.EN) {
	    return key;
	}
	if(bundle.useMatch) {
	    Map<Pattern, String> patterns = match.get(bundle);
	    Matcher m = patterns.keySet().stream().map(p -> p.matcher(key)).filter(Matcher::find).findFirst().orElse(null);
	    if(m != null) {
		String format = patterns.get(m.pattern());
		int k = m.groupCount();
		Object[] values = new Object[k];
		for (int i = 0; i < k; i++) {
		    values[i] = m.group(i + 1);
		}
		result = String.format(format, values);
		if(DBG) System.out.printf("[%s] '%s' => '%s'%n", bundle, key, result);
	    }
	} else {
	    Map<String, String> map = simple.get(bundle);
	    if(map == null) {
		return key;
	    }
	    result = map.get(key);
	}
	if(DBG && result == null) {
	    MISSING.get(bundle).put(key, key);
	    Config.saveFile("MISSING_TRANSLATIONS.json", GSON_OUT.toJson(MISSING));
	}
	return result != null ? result : key;
    }
    
    private static Map<String, String> loadSimple(Bundle bundle) {
	if(language == Language.EN) { return new HashMap<>(); }
	
	String json = Config.loadFile(String.format("i10n/%s_%s.json", bundle.name, language.name));
	
	Map<String, String> map = null;
	if(json != null) {
	    try {
		Gson gson = new GsonBuilder().create();
		map = gson.fromJson(json, new TypeToken<Map<String, String>>() {
		}.getType());
		
	    } catch (JsonSyntaxException ignored) {}
	}
	
	return map == null ? new HashMap<>() : map;
    }
    
    private static Map<Pattern, String> loadMatch(Bundle bundle) {
	Map<String, String> tmp = loadSimple(bundle);
	HashMap<Pattern, String> map = new HashMap<>();
	for (Map.Entry<String, String> e : tmp.entrySet()) {
	    map.put(Pattern.compile(String.format("^%s$", e.getKey())), e.getValue());
	}
	return map;
    }
}